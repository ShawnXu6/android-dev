package com.example.android_dev.domain

import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
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
    val habitId: String? = null,
    val lastCompletedDate: String? = null,
    val completionHistory: List<String> = emptyList(),
    val modality: InputModality = InputModality.TEXT,
    val reminderTime: Int? = null
) {
    val isCompleted: Boolean
        get() {
            if (!isHabit) return completedAt != null

            val today = LocalDate.now().toString()
            val completedAtDate = completedAt
                ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString() }
            return lastCompletedDate == today || completionHistory.contains(today) || completedAtDate == today
        }
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

// 推荐权重配置功能：集中定义任务优先级算法的可调权重。
data class PriorityWeights(
    val importanceWeight: Float = 0.36f,
    val urgencyWeight: Float = 0.30f,
    val complexityWeight: Float = 0.14f,
    val capabilityFitWeight: Float = 0.20f,
    val habitWeight: Float = 0.08f
)

// 推荐分项功能：描述单个评分因子的原始分、权重、贡献值和解释。
data class PriorityFactorScore(
    val label: String,
    val score: Float,
    val weight: Float,
    val contribution: Float,
    val reason: String
)

// 推荐评分拆解功能：保存总分、分项贡献和面向用户的推荐解释。
data class PriorityScoreBreakdown(
    val totalScore: Float,
    val factors: List<PriorityFactorScore>,
    val explanation: String
)

// 推荐结果功能：把被推荐任务和对应的可解释优先级评分绑定在一起。
data class TaskRecommendation(
    val task: SmartTask,
    val priority: PriorityScoreBreakdown
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

// ========== 统计与可视化相关模型 ==========

data class DailyStats(
    val date: String, // yyyy-MM-dd
    val totalTasks: Int,
    val completedTasks: Int,
    val totalMinutes: Int,
    val categoryMinutes: Map<TaskCategory, Int>,
    val completedCategoryMinutes: Map<TaskCategory, Int> = emptyMap(),
    val pendingCategoryMinutes: Map<TaskCategory, Int> = emptyMap(),
    val avgCognitiveLoad: Float,
    val peakLoadHour: Int?,
    val habitCompleted: Int
) {
    val completionRate: Float
        get() = if (totalTasks == 0) 0f else completedTasks.toFloat() / totalTasks
}

data class WeeklyReport(
    val weekStart: String, // yyyy-MM-dd
    val dailyStats: List<DailyStats>,
    val totalCompleted: Int,
    val totalTasks: Int,
    val avgCompletionRate: Float,
    val categoryDistribution: Map<TaskCategory, Float>,
    val completedCategoryDistribution: Map<TaskCategory, Float> = emptyMap(),
    val pendingCategoryDistribution: Map<TaskCategory, Float> = emptyMap(),
    val trend: CompletionTrend,
    val totalHabitStreak: Int
)

enum class CompletionTrend {
    IMPROVING,
    STABLE,
    DECLINING
}

data class CognitiveLoadRecord(
    val timestamp: Long,
    val hour: Int,
    val overall: Float,
    val visualLoad: Float,
    val memoryLoad: Float,
    val temporalPressure: Float,
    val decisionFatigue: Float
)

data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val category: AchievementCategory,
    val unlockedAt: Long?,
    val progress: Float, // 0-1
    val requirement: String
)

enum class AchievementCategory(val label: String) {
    COMPLETION("完成"),
    HABIT("习惯"),
    EFFICIENCY("效率"),
    COGNITIVE("认知"),
    MILESTONE("里程碑")
}

data class HeatmapData(
    val dates: List<String>,
    val values: Map<String, Int>, // date -> streak count
    val maxStreak: Int
)
