package com.example.android_dev.ai

import com.example.android_dev.domain.AiBreakdownResult
import com.example.android_dev.domain.AiSource
import com.example.android_dev.domain.Subtask
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

// 远程任务拆解功能：调用百度飞桨 AIStudio 的 OpenAI 兼容大模型接口完成任务拆解。
// 失败时抛异常，由上层 AiTaskPlannerRepository 兜底为本地拆解。
class RemoteAiTaskPlanner(
    private val client: OkHttpClient = defaultClient()
) : AiTaskPlanner {

    override suspend fun breakdown(request: AiBreakdownRequest): AiBreakdownResult = withContext(Dispatchers.IO) {
        require(PaddleAiConfig.hasToken) { "未配置飞桨 AIStudio 访问令牌" }

        val body = buildRequestBody(request)
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
            parseResponse(raw, request)
        }
    }

    // 请求体构造功能：用 system + user 提示词约束模型输出严格 JSON。
    private fun buildRequestBody(request: AiBreakdownRequest): JSONObject {
        val dueText = request.dueDate?.toString() ?: "未指定"
        val userPrompt = buildString {
            append("请把下面这个目标拆解为有先后顺序的子任务，并为每个子任务安排一个计划日期。\n")
            append("目标：${request.goal}\n")
            if (request.note.isNotBlank()) append("补充说明：${request.note}\n")
            append("截止日期：$dueText（今天是 ${LocalDate.now()}）\n")
            append("要求：3 到 7 个子任务；时间线不得晚于截止日期；")
            append("只返回 JSON，不要任何额外文字，格式为：\n")
            append("""{"summary":"一句话说明","subtasks":[{"title":"子任务标题","minutes":30,"date":"YYYY-MM-DD"}]}""")
        }

        val messages = JSONArray()
            .put(
                JSONObject()
                    .put("role", "system")
                    .put("content", "你是高效的项目规划助手，擅长把目标拆成可执行步骤并合理排期。只输出 JSON。")
            )
            .put(JSONObject().put("role", "user").put("content", userPrompt))

        return JSONObject()
            .put("model", PaddleAiConfig.MODEL)
            .put("messages", messages)
            .put("temperature", 0.3)
    }

    // 响应解析功能：从对话补全结果里取出模型生成内容并解析为子任务列表。
    private fun parseResponse(raw: String, request: AiBreakdownRequest): AiBreakdownResult {
        val root = JSONObject(raw)
        val content = root
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")

        val json = JSONObject(extractJsonObject(content))
        val summary = json.optString("summary", "AI 已为你拆解任务。")
        val array = json.optJSONArray("subtasks") ?: JSONArray()

        val subtasks = List(array.length()) { index ->
            val item = array.getJSONObject(index)
            Subtask(
                title = item.optString("title", "子任务 ${index + 1}").trim(),
                estimatedMinutes = item.optInt("minutes", 30).coerceIn(5, 240),
                plannedDate = item.optString("date").takeIf { it.isNotBlank() }
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            )
        }.filter { it.title.isNotBlank() }

        if (subtasks.isEmpty()) throw IllegalStateException("模型未返回有效子任务")

        return AiBreakdownResult(subtasks = subtasks, summary = summary, source = AiSource.REMOTE)
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
