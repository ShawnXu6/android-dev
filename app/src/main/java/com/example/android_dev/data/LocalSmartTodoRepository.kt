package com.example.android_dev.data

import android.content.Context
import com.example.android_dev.domain.CognitiveLoadRecord
import com.example.android_dev.domain.EmotionalTone
import com.example.android_dev.domain.EnvironmentContext
import com.example.android_dev.domain.InputModality
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskCategory
import com.example.android_dev.domain.UserCognitiveSignal
import org.json.JSONArray
import org.json.JSONObject

class LocalSmartTodoRepository(context: Context) {
    private val prefs = context.getSharedPreferences("smart_todo_life", Context.MODE_PRIVATE)

    fun loadTasks(): List<SmartTask> {
        val raw = prefs.getString(KEY_TASKS, null) ?: return seedTasks().also(::saveTasks)
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index -> array.getJSONObject(index).toTask() }
        }.getOrElse {
            seedTasks().also(::saveTasks)
        }
    }

    fun saveTasks(tasks: List<SmartTask>) {
        val array = JSONArray()
        tasks.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_TASKS, array.toString()).apply()
    }

    fun loadSignal(): UserCognitiveSignal {
        val raw = prefs.getString(KEY_SIGNAL, null) ?: return UserCognitiveSignal()
        return runCatching {
            val json = JSONObject(raw)
            UserCognitiveSignal(
                focus = json.optDouble("focus", 0.68).toFloat().coerceIn(0f, 1f),
                energy = json.optDouble("energy", 0.62).toFloat().coerceIn(0f, 1f),
                stress = json.optDouble("stress", 0.34).toFloat().coerceIn(0f, 1f),
                mood = enumOrDefault(json.optString("mood"), EmotionalTone.NEUTRAL),
                environment = enumOrDefault(json.optString("environment"), EnvironmentContext.QUIET),
                adaptiveMode = json.optBoolean("adaptiveMode", true)
            )
        }.getOrDefault(UserCognitiveSignal())
    }

    fun saveSignal(signal: UserCognitiveSignal) {
        prefs.edit().putString(
            KEY_SIGNAL,
            JSONObject()
                .put("focus", signal.focus)
                .put("energy", signal.energy)
                .put("stress", signal.stress)
                .put("mood", signal.mood.name)
                .put("environment", signal.environment.name)
                .put("adaptiveMode", signal.adaptiveMode)
                .toString()
        ).apply()
    }

    fun saveLoadRecord(record: CognitiveLoadRecord) {
        val records = loadLoadRecords().toMutableList()
        records.add(record)
        val last30Days = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        val filtered = records.filter { it.timestamp >= last30Days }
        saveLoadRecords(filtered)
    }

    fun loadLoadRecords(): List<CognitiveLoadRecord> {
        val raw = prefs.getString(KEY_LOAD_RECORDS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val json = array.getJSONObject(index)
                CognitiveLoadRecord(
                    timestamp = json.getLong("timestamp"),
                    hour = json.getInt("hour"),
                    overall = json.getDouble("overall").toFloat(),
                    visualLoad = json.getDouble("visualLoad").toFloat(),
                    memoryLoad = json.getDouble("memoryLoad").toFloat(),
                    temporalPressure = json.getDouble("temporalPressure").toFloat(),
                    decisionFatigue = json.getDouble("decisionFatigue").toFloat()
                )
            }
        }.getOrElse { emptyList() }
    }

    private fun saveLoadRecords(records: List<CognitiveLoadRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("timestamp", record.timestamp)
                    .put("hour", record.hour)
                    .put("overall", record.overall)
                    .put("visualLoad", record.visualLoad)
                    .put("memoryLoad", record.memoryLoad)
                    .put("temporalPressure", record.temporalPressure)
                    .put("decisionFatigue", record.decisionFatigue)
            )
        }
        prefs.edit().putString(KEY_LOAD_RECORDS, array.toString()).apply()
    }

    private fun SmartTask.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("title", title)
            .put("description", description)
            .put("category", category.name)
            .put("estimatedMinutes", estimatedMinutes)
            .put("importance", importance)
            .put("complexity", complexity)
            .put("targetHour", targetHour)
            .put("createdAt", createdAt)
            .put("completedAt", completedAt ?: JSONObject.NULL)
            .put("isHabit", isHabit)
            .put("streak", streak)
            .put("habitId", habitId ?: JSONObject.NULL)
            .put("lastCompletedDate", lastCompletedDate ?: JSONObject.NULL)
            .put("completionHistory", JSONArray(completionHistory))
            .put("modality", modality.name)
    }

    private fun JSONObject.toTask(): SmartTask {
        return SmartTask(
            id = optString("id"),
            title = optString("title", "未命名任务"),
            description = optString("description", ""),
            category = enumOrDefault(optString("category"), TaskCategory.WORK),
            estimatedMinutes = optInt("estimatedMinutes", 25).coerceIn(5, 240),
            importance = optInt("importance", 3).coerceIn(1, 5),
            complexity = optInt("complexity", 3).coerceIn(1, 5),
            targetHour = optInt("targetHour", 10).coerceIn(0, 23),
            createdAt = optLong("createdAt", System.currentTimeMillis()),
            completedAt = if (isNull("completedAt")) null else optLong("completedAt"),
            isHabit = optBoolean("isHabit", false),
            streak = optInt("streak", 0).coerceAtLeast(0),
            habitId = if (isNull("habitId")) null else optString("habitId"),
            lastCompletedDate = if (isNull("lastCompletedDate")) null else optString("lastCompletedDate"),
            completionHistory = optStringArray("completionHistory"),
            modality = enumOrDefault(optString("modality"), InputModality.TEXT)
        )
    }

    private fun JSONObject.optStringArray(key: String): List<String> {
        val array = optJSONArray(key) ?: return emptyList()
        return List(array.length()) { index -> array.optString(index) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, default: T): T {
        return runCatching { enumValueOf<T>(value.orEmpty()) }.getOrDefault(default)
    }

    private fun seedTasks(): List<SmartTask> {
        return listOf(
            SmartTask(
                title = "整理本周项目优先级",
                description = "把待办压缩到三个关键结果，降低决策疲劳。",
                category = TaskCategory.WORK,
                estimatedMinutes = 38,
                importance = 5,
                complexity = 4,
                targetHour = 9
            ),
            SmartTask(
                title = "阅读一篇认知负荷论文摘要",
                description = "提取一个可以用于任务展示密度调整的设计启发。",
                category = TaskCategory.STUDY,
                estimatedMinutes = 45,
                importance = 4,
                complexity = 4,
                targetHour = 11
            ),
            SmartTask(
                title = "20分钟恢复性散步",
                description = "压力偏高时自动推荐的恢复任务。",
                category = TaskCategory.HEALTH,
                estimatedMinutes = 20,
                importance = 3,
                complexity = 1,
                targetHour = 18,
                isHabit = true,
                streak = 2
            ),
            SmartTask(
                title = "给团队同步今日阻塞",
                description = "用简短结构描述问题、影响和需要的支持。",
                category = TaskCategory.SOCIAL,
                estimatedMinutes = 15,
                importance = 4,
                complexity = 2,
                targetHour = 16
            ),
            SmartTask(
                title = "睡前复盘并规划明天三件事",
                description = "微习惯：只写三行，不追求完整计划。",
                category = TaskCategory.LIFE,
                estimatedMinutes = 8,
                importance = 3,
                complexity = 1,
                targetHour = 21,
                isHabit = true,
                streak = 5
            )
        )
    }

    private companion object {
        const val KEY_TASKS = "tasks"
        const val KEY_SIGNAL = "signal"
        const val KEY_LOAD_RECORDS = "load_records"
    }
}
