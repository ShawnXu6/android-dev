package com.example.android_dev.engine

import com.example.android_dev.domain.CognitiveLoadLevel
import com.example.android_dev.domain.CognitiveSnapshot
import com.example.android_dev.domain.EmotionalTone
import com.example.android_dev.domain.EnvironmentContext
import com.example.android_dev.domain.HabitPlan
import com.example.android_dev.domain.InputModality
import com.example.android_dev.domain.InsightSeverity
import com.example.android_dev.domain.ProductivityInsight
import com.example.android_dev.domain.ScheduleSlot
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskCategory
import com.example.android_dev.domain.TimePrediction
import com.example.android_dev.domain.UserCognitiveSignal
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

object SmartTaskEngine {
    private val workKeywords = listOf("项目", "会议", "汇报", "文档", "客户", "review", "sync", "report")
    private val studyKeywords = listOf("学习", "阅读", "论文", "课程", "考试", "research", "paper", "study")
    private val healthKeywords = listOf("运动", "散步", "睡眠", "体检", "喝水", "冥想", "run", "sleep", "walk")
    private val socialKeywords = listOf("团队", "朋友", "家人", "沟通", "同步", "share", "call", "message")
    private val creativeKeywords = listOf("设计", "写作", "创作", "原型", "拍摄", "draft", "design", "write")

    fun createTaskFromInput(
        rawTitle: String,
        note: String,
        signal: UserCognitiveSignal,
        nowHour: Int = LocalTime.now().hour
    ): SmartTask {
        val title = rawTitle.trim().ifBlank { "未命名任务" }
        val category = classify(title, note)
        val complexity = inferComplexity(title, note)
        val importance = inferImportance(title, note)
        val targetHour = inferTargetHour(title, nowHour)
        val isHabit = listOf("每天", "每日", "习惯", "打卡", "复盘", "喝水").any { title.contains(it) }

        return SmartTask(
            title = title,
            description = note.trim(),
            category = category,
            estimatedMinutes = estimateMinutes(title, note, category, complexity, signal),
            importance = importance,
            complexity = complexity,
            targetHour = targetHour,
            isHabit = isHabit,
            modality = InputModality.TEXT
        )
    }

    fun classify(title: String, note: String = ""): TaskCategory {
        val text = "$title $note".lowercase()
        return when {
            workKeywords.any { text.contains(it) } -> TaskCategory.WORK
            studyKeywords.any { text.contains(it) } -> TaskCategory.STUDY
            healthKeywords.any { text.contains(it) } -> TaskCategory.HEALTH
            socialKeywords.any { text.contains(it) } -> TaskCategory.SOCIAL
            creativeKeywords.any { text.contains(it) } -> TaskCategory.CREATIVE
            else -> TaskCategory.LIFE
        }
    }

    fun inferComplexity(title: String, note: String = ""): Int {
        val text = "$title $note"
        var score = 2
        if (text.length > 28) score += 1
        if (text.length > 58) score += 1
        if (listOf("设计", "分析", "架构", "论文", "研究", "优化", "复杂", "方案").any { text.contains(it) }) score += 1
        if (listOf("打卡", "喝水", "散步", "整理", "回复").any { text.contains(it) }) score -= 1
        return score.coerceIn(1, 5)
    }

    fun inferImportance(title: String, note: String = ""): Int {
        val text = "$title $note"
        var score = 3
        if (listOf("紧急", "今天", "截止", "deadline", "必须", "重要").any { text.contains(it, ignoreCase = true) }) score += 1
        if (listOf("可选", "有空", "以后", "低优先级").any { text.contains(it, ignoreCase = true) }) score -= 1
        return score.coerceIn(1, 5)
    }

    fun estimateMinutes(
        title: String,
        note: String,
        category: TaskCategory,
        complexity: Int,
        signal: UserCognitiveSignal
    ): Int {
        val text = "$title $note"
        val base = when (category) {
            TaskCategory.HEALTH -> 20
            TaskCategory.SOCIAL -> 18
            TaskCategory.LIFE -> 25
            TaskCategory.STUDY -> 45
            TaskCategory.CREATIVE -> 50
            TaskCategory.WORK -> 40
        }
        val keywordBoost = when {
            listOf("方案", "论文", "架构", "设计", "复盘", "分析").any { text.contains(it) } -> 18
            listOf("回复", "整理", "确认", "打卡").any { text.contains(it) } -> -8
            else -> 0
        }
        val cognitiveBuffer = ((signal.stress - signal.focus + 0.35f) * 18).roundToInt()
        return (base + complexity * 8 + keywordBoost + cognitiveBuffer).coerceIn(8, 180)
    }

    fun predictTime(task: SmartTask, signal: UserCognitiveSignal): TimePrediction {
        val stressBuffer = (task.estimatedMinutes * signal.stress * 0.25f).roundToInt()
        val focusDiscount = (task.estimatedMinutes * signal.focus * 0.12f).roundToInt()
        val minutes = (task.estimatedMinutes + stressBuffer - focusDiscount).coerceAtLeast(5)
        val uncertainty = (minutes * (0.12f + signal.stress * 0.22f + task.complexity * 0.025f)).roundToInt()
        val confidence = (0.88f - signal.stress * 0.22f - (task.complexity - 3).coerceAtLeast(0) * 0.04f)
            .coerceIn(0.52f, 0.94f)

        return TimePrediction(
            minutes = minutes,
            confidence = confidence.round2(),
            uncertaintyMinutes = uncertainty.coerceAtLeast(3),
            rationale = "结合任务复杂度、当前专注度与压力水平生成的端侧估计"
        )
    }

    fun calculatePriorityScore(task: SmartTask, signal: UserCognitiveSignal, nowHour: Int = LocalTime.now().hour): Float {
        if (task.isCompleted) return 0f
        val hoursUntilTarget = (task.targetHour - nowHour).coerceIn(-4, 16)
        val urgency = when {
            hoursUntilTarget < 0 -> 0.96f
            hoursUntilTarget <= 2 -> 0.86f
            else -> (1f - hoursUntilTarget / 16f).coerceIn(0.18f, 0.72f)
        }
        val importance = task.importance / 5f
        val complexity = task.complexity / 5f
        val capabilityFit = (1f - abs(complexity - ((signal.focus + signal.energy) / 2f)) * 0.55f).coerceIn(0.28f, 1f)
        val habitBoost = if (task.isHabit) 0.08f else 0f

        return ((importance * 0.36f + urgency * 0.30f + complexity * 0.14f + capabilityFit * 0.20f + habitBoost) * 100f)
            .coerceIn(0f, 100f)
            .round1()
    }

    fun recommendNextTask(
        tasks: List<SmartTask>,
        signal: UserCognitiveSignal,
        nowHour: Int = LocalTime.now().hour
    ): SmartTask? {
        return tasks
            .filterNot { it.isCompleted }
            .maxByOrNull { calculatePriorityScore(it, signal, nowHour) }
    }

    fun buildSchedule(
        tasks: List<SmartTask>,
        signal: UserCognitiveSignal,
        nowHour: Int = LocalTime.now().hour
    ): List<ScheduleSlot> {
        val active = tasks
            .filterNot { it.isCompleted }
            .sortedByDescending { calculatePriorityScore(it, signal, nowHour) }
            .take(6)

        var cursor = max(nowHour * 60, 8 * 60)
        return active.map { task ->
            val prediction = predictTime(task, signal)
            val preferredStart = preferredStartMinute(task, signal, nowHour)
            cursor = max(cursor, preferredStart)
            val start = cursor
            val end = start + prediction.minutes
            cursor = end + if (task.complexity >= 4) 18 else 10
            ScheduleSlot(
                task = task,
                start = clockLabel(start),
                end = clockLabel(end),
                expectedLoad = (task.complexity / 5f * 0.68f + signal.stress * 0.32f).coerceIn(0f, 1f).round2(),
                rationale = when {
                    task.complexity >= 4 && signal.focus >= 0.55f -> "高复杂任务安排在当前可承载窗口"
                    task.isHabit -> "微习惯优先放入低阻力时段"
                    signal.stress > 0.65f -> "压力偏高，保留缓冲"
                    else -> "按优先级、目标时间和认知负荷平衡排序"
                }
            )
        }
    }

    fun generateInsights(tasks: List<SmartTask>, signal: UserCognitiveSignal): List<ProductivityInsight> {
        val completed = tasks.count { it.isCompleted }
        val active = tasks.count { !it.isCompleted }
        val completionRate = if (tasks.isEmpty()) 0f else completed.toFloat() / tasks.size
        val highComplexity = tasks.count { !it.isCompleted && it.complexity >= 4 }
        val habitStreak = tasks.filter { it.isHabit }.sumOf { it.streak }

        return buildList {
            add(
                ProductivityInsight(
                    title = "完成率 ${(completionRate * 100).roundToInt()}%",
                    body = if (completionRate >= 0.55f) {
                        "当前闭环表现稳定，可以增加一个中等挑战任务。"
                    } else {
                        "建议先收敛任务池，把待办压到 5 项以内。"
                    },
                    severity = if (completionRate >= 0.55f) InsightSeverity.GOOD else InsightSeverity.NOTICE
                )
            )
            add(
                ProductivityInsight(
                    title = "认知队列 $active 项",
                    body = if (active <= 6) {
                        "任务数量处在可扫描范围，适合使用详细列表。"
                    } else {
                        "任务数量偏多，建议开启极简视图并隐藏低优先级事项。"
                    },
                    severity = if (active <= 6) InsightSeverity.GOOD else InsightSeverity.WARNING
                )
            )
            add(
                ProductivityInsight(
                    title = "高复杂任务 $highComplexity 项",
                    body = if (highComplexity == 0) {
                        "今天没有高负荷任务，可以安排维护型工作。"
                    } else {
                        "高复杂任务应放在专注度峰值，并拆出 15 分钟启动动作。"
                    },
                    severity = if (highComplexity <= 2) InsightSeverity.NOTICE else InsightSeverity.WARNING
                )
            )
            add(
                ProductivityInsight(
                    title = "习惯连续性 $habitStreak 天",
                    body = if (habitStreak > 0) {
                        "保留低门槛打卡，强化即时反馈，不建议一次叠加多个新习惯。"
                    } else {
                        "可从一个 2 分钟微习惯开始，先建立提示和奖励。"
                    },
                    severity = InsightSeverity.NOTICE
                )
            )
            if (signal.stress > 0.65f) {
                add(
                    ProductivityInsight(
                        title = "压力偏高",
                        body = "系统会降低信息密度，推荐短任务和更长缓冲。高风险任务应延期或拆分。",
                        severity = InsightSeverity.WARNING
                    )
                )
            }
        }
    }

    fun designHabitPlan(task: SmartTask, signal: UserCognitiveSignal): HabitPlan {
        val anchor = when (signal.environment) {
            EnvironmentContext.QUIET -> "坐到桌前后的 30 秒"
            EnvironmentContext.COMMUTE -> "到达目的地后的第一分钟"
            EnvironmentContext.MEETING -> "会议结束后"
            EnvironmentContext.LOW_LIGHT -> "打开屏幕后"
        }
        val tinyAction = if (task.estimatedMinutes > 20) {
            "只做 2 分钟启动动作：${task.title.take(12)}"
        } else {
            "完成一个最小版本：${task.title.take(12)}"
        }
        val reinforcement = when (signal.mood) {
            EmotionalTone.POSITIVE -> "记录一次进度，并允许增加一点挑战"
            EmotionalTone.NEUTRAL -> "完成后立即打勾，保留连续性反馈"
            EmotionalTone.NEGATIVE -> "只要求出现，不要求完美，用低压力反馈收尾"
        }
        return HabitPlan(
            anchor = anchor,
            tinyAction = tinyAction,
            reinforcement = reinforcement,
            nextStep = "连续 3 天后再把时长增加 20%"
        )
    }

    private fun inferTargetHour(title: String, nowHour: Int): Int {
        return when {
            title.contains("早") || title.contains("上午") -> 9
            title.contains("中午") -> 12
            title.contains("下午") -> 15
            title.contains("晚上") || title.contains("睡前") -> 21
            title.contains("今天") -> (nowHour + 2).coerceAtMost(22)
            title.contains("明天") -> 10
            else -> (nowHour + 3).coerceIn(9, 21)
        }
    }

    private fun preferredStartMinute(task: SmartTask, signal: UserCognitiveSignal, nowHour: Int): Int {
        val current = nowHour * 60
        val preferredHour = when {
            task.complexity >= 4 && signal.focus >= 0.58f -> 9
            task.category == TaskCategory.HEALTH -> 18
            task.isHabit && task.targetHour >= 20 -> 21
            task.targetHour < nowHour -> nowHour
            else -> task.targetHour
        }
        return max(current, preferredHour * 60)
    }

    private fun clockLabel(totalMinutes: Int): String {
        val normalized = totalMinutes.coerceAtMost(23 * 60 + 59)
        val hour = normalized / 60
        val minute = normalized % 60
        return "%02d:%02d".format(hour, minute)
    }
}

object CognitiveLoadEngine {
    fun assess(
        tasks: List<SmartTask>,
        signal: UserCognitiveSignal,
        currentHour: Int = LocalTime.now().hour
    ): CognitiveSnapshot {
        val activeTasks = tasks.filterNot { it.isCompleted }
        val activeCount = activeTasks.size
        val categorySpread = activeTasks.map { it.category }.distinct().size
        val urgentCount = activeTasks.count { it.targetHour <= currentHour + 2 }
        val highComplexity = activeTasks.count { it.complexity >= 4 }

        val visualLoad = (activeCount / 10f + categorySpread * 0.045f).coerceIn(0f, 1f)
        val memoryLoad = (highComplexity / 5f + activeCount / 16f + (1f - signal.focus) * 0.22f).coerceIn(0f, 1f)
        val temporalPressure = (urgentCount / 5f + signal.stress * 0.48f).coerceIn(0f, 1f)
        val decisionFatigue = (activeCount / 12f + signal.stress * 0.36f - signal.energy * 0.12f).coerceIn(0f, 1f)
        val overall = (
            visualLoad * 0.24f +
                memoryLoad * 0.28f +
                temporalPressure * 0.26f +
                decisionFatigue * 0.22f
            ).coerceIn(0f, 1f).round2()

        val level = when {
            overall < 0.32f -> CognitiveLoadLevel.LOW
            overall < 0.58f -> CognitiveLoadLevel.BALANCED
            overall < 0.78f -> CognitiveLoadLevel.HIGH
            else -> CognitiveLoadLevel.OVERWHELMING
        }

        return CognitiveSnapshot(
            overall = overall,
            visualLoad = visualLoad.round2(),
            memoryLoad = memoryLoad.round2(),
            temporalPressure = temporalPressure.round2(),
            decisionFatigue = decisionFatigue.round2(),
            level = level,
            recommendation = recommendationFor(level, signal)
        )
    }

    private fun recommendationFor(level: CognitiveLoadLevel, signal: UserCognitiveSignal): String {
        return when (level) {
            CognitiveLoadLevel.LOW -> "可以展示完整任务细节，并安排一个创造型或学习型任务。"
            CognitiveLoadLevel.BALANCED -> "保持平衡视图，优先处理一个高价值任务。"
            CognitiveLoadLevel.HIGH -> "建议切换到简化视图，只保留前三个任务和必要提醒。"
            CognitiveLoadLevel.OVERWHELMING -> "先暂停新增任务，拆分最高负荷任务，并加入恢复间隔。"
        } + if (signal.adaptiveMode) " 自适应模式已开启。" else " 自适应模式未开启。"
    }
}

private fun Float.round1(): Float = (this * 10f).roundToInt() / 10f

private fun Float.round2(): Float = (this * 100f).roundToInt() / 100f
