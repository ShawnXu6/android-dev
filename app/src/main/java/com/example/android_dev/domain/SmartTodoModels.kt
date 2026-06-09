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

// 任务优先级功能：用直观的高/中/低三档表达优先级，并提供排序权重和颜色语义。
enum class TaskPriority(val label: String, val weight: Int) {
    HIGH("高", 3),
    MEDIUM("中", 2),
    LOW("低", 1);

    companion object {
        // 兼容旧数据：把历史的 1-5 重要度映射成高/中/低。
        fun fromImportance(importance: Int): TaskPriority = when {
            importance >= 4 -> HIGH
            importance <= 2 -> LOW
            else -> MEDIUM
        }
    }
}

// 任务状态功能：支撑看板三列（待处理 / 进行中 / 已完成）的流转。
enum class TaskStatus(val label: String) {
    TODO("待处理"),
    IN_PROGRESS("进行中"),
    DONE("已完成");

    // 看板向前推进：待处理 → 进行中 → 已完成。
    fun next(): TaskStatus = when (this) {
        TODO -> IN_PROGRESS
        IN_PROGRESS -> DONE
        DONE -> DONE
    }

    // 看板向后回退：已完成 → 进行中 → 待处理。
    fun previous(): TaskStatus = when (this) {
        DONE -> IN_PROGRESS
        IN_PROGRESS -> TODO
        TODO -> TODO
    }
}

// 子任务功能：承载 AI 拆解或手动添加的步骤，含独立计划日期与完成态。
data class Subtask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val estimatedMinutes: Int = 20,
    val done: Boolean = false,
    val plannedDate: LocalDate? = null
)

// AI 拆解来源功能：标注结果是远程大模型生成还是本地启发式兜底。
enum class AiSource(val label: String) {
    REMOTE("AI 生成"),
    LOCAL("本地智能生成")
}

// AI 拆解结果功能：把拆出的子任务、整体说明和来源打包返回给界面。
data class AiBreakdownResult(
    val subtasks: List<Subtask>,
    val summary: String,
    val source: AiSource
)

// 对话角色功能：区分用户消息与 AI 回复。
enum class ChatRole(val apiName: String) {
    USER("user"),
    ASSISTANT("assistant")
}

// 对话消息功能：承载单条聊天消息的角色、内容和时间。
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

// 提取任务功能：从 AI 回复文本里解析出的、可一键加入计划的任务条目（含勾选态供预览）。
data class ExtractedTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val plannedDate: LocalDate? = null,
    val estimatedMinutes: Int = 30,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val selected: Boolean = true
)

// 本地账户功能：保存用户名、加盐后的密码哈希和创建时间。
data class UserAccount(
    val username: String,
    val passwordHash: String,
    val salt: String,
    val createdAt: Long = System.currentTimeMillis()
)

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
    // ===== 新增：截止日期、优先级、看板状态、标签、子任务、提醒 =====
    val dueDate: LocalDate? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.TODO,
    val tags: List<String> = emptyList(),
    val subtasks: List<Subtask> = emptyList(),
    val reminderAt: Long? = null
) {
    // 完成判定功能：习惯任务按当日打卡判定，普通任务按 status==DONE 或 completedAt 判定（兼容旧数据）。
    val isCompleted: Boolean
        get() {
            if (!isHabit) return status == TaskStatus.DONE || completedAt != null

            val today = LocalDate.now().toString()
            val completedAtDate = completedAt
                ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString() }
            return lastCompletedDate == today || completionHistory.contains(today) || completedAtDate == today
        }

    // 子任务进度功能：返回已完成子任务比例，供卡片显示进度条。
    val subtaskProgress: Float
        get() = if (subtasks.isEmpty()) 0f else subtasks.count { it.done }.toFloat() / subtasks.size

    // 逾期判定功能：有截止日期、未完成且截止日早于今天即为逾期。
    val isOverdue: Boolean
        get() = dueDate != null && !isCompleted && dueDate.isBefore(LocalDate.now())
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
