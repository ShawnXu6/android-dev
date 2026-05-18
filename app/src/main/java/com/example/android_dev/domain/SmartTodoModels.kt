package com.example.android_dev.domain

import java.util.UUID

enum class TaskCategory(val label: String) {
    WORK("工作"),
    STUDY("学习"),
    HEALTH("健康"),
    LIFE("生活"),
    SOCIAL("协作"),
    CREATIVE("创作")
}

enum class InputModality(val label: String) {
    TEXT("文本"),
    VOICE("语音"),
    CAMERA("图像"),
    GESTURE("手势")
}

enum class EmotionalTone(val label: String) {
    POSITIVE("积极"),
    NEUTRAL("平稳"),
    NEGATIVE("低落")
}

enum class EnvironmentContext(val label: String) {
    QUIET("安静"),
    COMMUTE("通勤"),
    MEETING("会议"),
    LOW_LIGHT("弱光")
}

enum class CognitiveLoadLevel(val label: String) {
    LOW("轻负荷"),
    BALANCED("平衡"),
    HIGH("高负荷"),
    OVERWHELMING("过载")
}

enum class InsightSeverity {
    GOOD,
    NOTICE,
    WARNING
}

data class SmartTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val category: TaskCategory = TaskCategory.WORK,
    val estimatedMinutes: Int = 25,
    val importance: Int = 3,
    val complexity: Int = 3,
    val targetHour: Int = 10,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isHabit: Boolean = false,
    val streak: Int = 0,
    val modality: InputModality = InputModality.TEXT
) {
    val isCompleted: Boolean
        get() = completedAt != null
}

data class UserCognitiveSignal(
    val focus: Float = 0.68f,
    val energy: Float = 0.62f,
    val stress: Float = 0.34f,
    val mood: EmotionalTone = EmotionalTone.NEUTRAL,
    val environment: EnvironmentContext = EnvironmentContext.QUIET,
    val adaptiveMode: Boolean = true
)

data class CognitiveSnapshot(
    val overall: Float,
    val visualLoad: Float,
    val memoryLoad: Float,
    val temporalPressure: Float,
    val decisionFatigue: Float,
    val level: CognitiveLoadLevel,
    val recommendation: String
)

data class TimePrediction(
    val minutes: Int,
    val confidence: Float,
    val uncertaintyMinutes: Int,
    val rationale: String
)

data class ScheduleSlot(
    val task: SmartTask,
    val start: String,
    val end: String,
    val expectedLoad: Float,
    val rationale: String
)

data class ProductivityInsight(
    val title: String,
    val body: String,
    val severity: InsightSeverity
)

data class HabitPlan(
    val anchor: String,
    val tinyAction: String,
    val reinforcement: String,
    val nextStep: String
)
