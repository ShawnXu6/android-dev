package com.example.android_dev.ai

import com.example.android_dev.domain.ChatMessage
import com.example.android_dev.domain.ExtractedTask
import com.example.android_dev.domain.TaskPriority
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

// AI 对话客户端功能：调用百度飞桨 AIStudio 的对话补全接口，支持多轮上下文。
class AiChatClient(
    private val client: OkHttpClient = defaultClient()
) {
    // 默认系统提示：把模型定位成本应用的待办/效率助手。
    private val systemPrompt =
        "你是「智能 TodoLife」应用内的 AI 助手，擅长任务规划、时间管理、学习与效率建议。" +
            "回答简洁、有条理，必要时用要点列出。请用中文回复。"

    // 发送功能：把系统提示 + 历史对话发给模型，返回 AI 回复文本。
    suspend fun send(history: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        require(PaddleAiConfig.hasToken) { "未配置飞桨 AIStudio 访问令牌" }

        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt))
            history.forEach { msg ->
                put(JSONObject().put("role", msg.role.apiName).put("content", msg.content))
            }
        }
        val body = JSONObject()
            .put("model", PaddleAiConfig.MODEL)
            .put("messages", messages)
            .put("temperature", 0.7)

        val httpRequest = Request.Builder()
            .url(PaddleAiConfig.BASE_URL)
            .addHeader("Authorization", "Bearer ${PaddleAiConfig.API_TOKEN}")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(httpRequest).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("飞桨接口返回 ${response.code}: ${raw.take(200)}")
            }
            JSONObject(raw)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        }
    }

    // 任务提取功能：把一段 AI 计划文本解析成结构化任务列表，便于一键加入计划。
    suspend fun extractTasks(planText: String): List<ExtractedTask> = withContext(Dispatchers.IO) {
        require(PaddleAiConfig.hasToken) { "未配置飞桨 AIStudio 访问令牌" }

        val prompt = buildString {
            append("下面是一段计划/建议文本。请从中提取出具体、可执行的待办任务。\n")
            append("今天是 ${LocalDate.now()}。如果文本里有「周一/明天/第一天」等相对时间，请换算成具体日期(YYYY-MM-DD)；没有就留空。\n")
            append("为每个任务估计合理的预计分钟数，并按重要程度给出优先级 high/medium/low。\n")
            append("最多提取 12 个任务。只返回 JSON，不要任何额外文字，格式：\n")
            append("""{"tasks":[{"title":"任务标题","date":"YYYY-MM-DD","minutes":30,"priority":"medium"}]}""")
            append("\n\n文本：\n")
            append(planText.take(2000))
        }

        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", "你是任务提取助手，只输出 JSON。"))
            .put(JSONObject().put("role", "user").put("content", prompt))
        val body = JSONObject()
            .put("model", PaddleAiConfig.MODEL)
            .put("messages", messages)
            .put("temperature", 0.2)
            .put("max_tokens", 1024)

        val httpRequest = Request.Builder()
            .url(PaddleAiConfig.BASE_URL)
            .addHeader("Authorization", "Bearer ${PaddleAiConfig.API_TOKEN}")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(httpRequest).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("飞桨接口返回 ${response.code}: ${raw.take(200)}")
            }
            parseExtractedTasks(raw)
        }
    }

    private fun parseExtractedTasks(raw: String): List<ExtractedTask> {
        val content = JSONObject(raw)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")

        val obj = JSONObject(extractJsonObject(content))
        val array = obj.optJSONArray("tasks") ?: JSONArray()
        val result = List(array.length()) { index ->
            val item = array.getJSONObject(index)
            ExtractedTask(
                title = item.optString("title").trim(),
                plannedDate = item.optString("date").takeIf { it.isNotBlank() }
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                estimatedMinutes = item.optInt("minutes", 30).coerceIn(5, 240),
                priority = when (item.optString("priority").lowercase()) {
                    "high" -> TaskPriority.HIGH
                    "low" -> TaskPriority.LOW
                    else -> TaskPriority.MEDIUM
                }
            )
        }.filter { it.title.isNotBlank() }

        if (result.isEmpty()) throw IllegalStateException("未能从文本中提取到任务")
        return result
    }

    // JSON 抽取功能：模型可能包裹 ```json 代码块，截取第一个完整的大括号对象。
    private fun extractJsonObject(content: String): String {
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        return if (start >= 0 && end > start) content.substring(start, end + 1) else content
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(PaddleAiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(PaddleAiConfig.CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(PaddleAiConfig.CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(PaddleAiConfig.CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
}
