package com.example.android_dev

import com.example.android_dev.domain.CognitiveLoadLevel
import com.example.android_dev.domain.CognitiveLoadRecord
import com.example.android_dev.domain.PriorityWeights
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskCategory
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.engine.AchievementEngine
import com.example.android_dev.engine.CognitiveLoadEngine
import com.example.android_dev.engine.SmartTaskEngine
import com.example.android_dev.engine.StatisticsEngine
import com.example.android_dev.engine.TaskCompletionEngine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartTodoEngineTest {
    @Test
    fun classifyDetectsStudyTasks() {
        val category = SmartTaskEngine.classify("阅读一篇认知负荷论文摘要")

        assertEquals(TaskCategory.STUDY, category)
    }

    @Test
    fun timePredictionAddsUncertaintyForStress() {
        val task = SmartTask(
            title = "设计项目架构方案",
            category = TaskCategory.WORK,
            estimatedMinutes = 60,
            complexity = 5
        )
        val calm = SmartTaskEngine.predictTime(task, UserCognitiveSignal(stress = 0.1f))
        val stressed = SmartTaskEngine.predictTime(task, UserCognitiveSignal(stress = 0.9f))

        assertTrue(stressed.minutes >= calm.minutes)
        assertTrue(stressed.uncertaintyMinutes > calm.uncertaintyMinutes)
    }

    @Test
    fun cognitiveLoadRisesWithManyComplexTasks() {
        val tasks = List(8) { index ->
            SmartTask(
                title = "复杂任务 $index",
                category = TaskCategory.WORK,
                complexity = 5,
                targetHour = 10
            )
        }

        val snapshot = CognitiveLoadEngine.assess(
            tasks = tasks,
            signal = UserCognitiveSignal(focus = 0.35f, energy = 0.3f, stress = 0.85f),
            currentHour = 9
        )

        assertTrue(snapshot.overall > 0.7f)
        assertTrue(snapshot.level >= CognitiveLoadLevel.HIGH)
    }

    @Test
    fun scheduleKeepsOnlyActiveTasks() {
        val completed = SmartTask(title = "已完成", completedAt = 1L)
        val active = SmartTask(title = "整理项目计划", category = TaskCategory.WORK, targetHour = 11)

        val schedule = SmartTaskEngine.buildSchedule(
            tasks = listOf(completed, active),
            signal = UserCognitiveSignal(),
            nowHour = 9
        )

        assertEquals(1, schedule.size)
        assertEquals(active.id, schedule.first().task.id)
    }

    @Test
    fun recommendationWeightsCanBeTuned() {
        val importantLater = SmartTask(title = "重要方案", importance = 5, complexity = 2, targetHour = 18)
        val urgentNow = SmartTask(title = "马上回复", importance = 2, complexity = 2, targetHour = 9)

        val defaultRecommendation = SmartTaskEngine.recommendNextTaskWithExplanation(
            tasks = listOf(importantLater, urgentNow),
            signal = UserCognitiveSignal(),
            nowHour = 9
        )
        val urgencyOnlyRecommendation = SmartTaskEngine.recommendNextTaskWithExplanation(
            tasks = listOf(importantLater, urgentNow),
            signal = UserCognitiveSignal(),
            nowHour = 9,
            weights = PriorityWeights(
                importanceWeight = 0f,
                urgencyWeight = 1f,
                complexityWeight = 0f,
                capabilityFitWeight = 0f,
                habitWeight = 0f
            )
        )

        assertEquals(importantLater.id, defaultRecommendation?.task?.id)
        assertEquals(urgentNow.id, urgencyOnlyRecommendation?.task?.id)
    }

    @Test
    fun priorityBreakdownExplainsDominantFactors() {
        val task = SmartTask(title = "今天完成重要方案", importance = 5, complexity = 4, targetHour = 10)

        val breakdown = SmartTaskEngine.explainPriorityScore(
            task = task,
            signal = UserCognitiveSignal(focus = 0.8f, energy = 0.7f),
            nowHour = 9
        )

        assertTrue(breakdown.totalScore > 0f)
        assertTrue(breakdown.factors.any { it.label == "重要度" && it.weight == 0.36f })
        assertTrue(breakdown.explanation.contains("主要因为"))
    }

    @Test
    fun dailyStatsUsesLocalDateInsteadOfUtcDate() {
        val shanghaiZone = ZoneId.of("Asia/Shanghai")
        val localMorningMillis = Instant.parse("2026-06-01T16:30:00Z").toEpochMilli()
        val task = SmartTask(title = "本地凌晨任务", createdAt = localMorningMillis, completedAt = localMorningMillis)
        val loadRecord = CognitiveLoadRecord(
            timestamp = localMorningMillis,
            hour = 0,
            overall = 0.2f,
            visualLoad = 0.2f,
            memoryLoad = 0.2f,
            temporalPressure = 0.2f,
            decisionFatigue = 0.2f
        )

        val stats = StatisticsEngine.buildDailyStats(
            tasks = listOf(task),
            loadRecords = listOf(loadRecord),
            date = LocalDate.of(2026, 6, 2),
            zoneId = shanghaiZone
        )

        assertEquals(1, stats.totalTasks)
        assertEquals(1, stats.completedTasks)
        assertEquals(0, stats.peakLoadHour)
    }

    @Test
    fun dailyStatsCountsTasksByCompletionDate() {
        val shanghaiZone = ZoneId.of("Asia/Shanghai")
        val yesterday = Instant.parse("2026-06-01T02:00:00Z").toEpochMilli()
        val today = Instant.parse("2026-06-02T02:00:00Z").toEpochMilli()
        val task = SmartTask(
            title = "昨天创建今天完成",
            category = TaskCategory.WORK,
            estimatedMinutes = 30,
            createdAt = yesterday,
            completedAt = today
        )

        val stats = StatisticsEngine.buildDailyStats(
            tasks = listOf(task),
            loadRecords = emptyList(),
            date = LocalDate.of(2026, 6, 2),
            zoneId = shanghaiZone
        )

        assertEquals(1, stats.totalTasks)
        assertEquals(1, stats.completedTasks)
        assertEquals(30, stats.totalMinutes)
        assertEquals(30, stats.completedCategoryMinutes[TaskCategory.WORK])
    }

    @Test
    fun weeklyReportSeparatesCompletedAndPendingCategoryDistribution() {
        val shanghaiZone = ZoneId.of("Asia/Shanghai")
        val today = Instant.parse("2026-06-02T02:00:00Z").toEpochMilli()
        val completed = SmartTask(
            title = "完成工作",
            category = TaskCategory.WORK,
            estimatedMinutes = 40,
            createdAt = today,
            completedAt = today
        )
        val pending = SmartTask(
            title = "未完成学习",
            category = TaskCategory.STUDY,
            estimatedMinutes = 60,
            createdAt = today
        )

        val report = StatisticsEngine.buildWeeklyReport(
            tasks = listOf(completed, pending),
            loadRecords = emptyList(),
            zoneId = shanghaiZone
        )

        assertEquals(1f, report.completedCategoryDistribution[TaskCategory.WORK] ?: 0f, 0.001f)
        assertEquals(1f, report.pendingCategoryDistribution[TaskCategory.STUDY] ?: 0f, 0.001f)
    }

    @Test
    fun achievementHoursUseLocalTimeInsteadOfUtcTime() {
        val shanghaiZone = ZoneId.of("Asia/Shanghai")
        val localEarlyMorningMillis = Instant.parse("2026-06-02T00:30:00Z").toEpochMilli()
        val localLateNightMillis = Instant.parse("2026-06-02T14:30:00Z").toEpochMilli()
        val earlyTask = SmartTask(title = "早间任务", completedAt = localEarlyMorningMillis)
        val lateTask = SmartTask(title = "夜间任务", completedAt = localLateNightMillis)

        val badges = AchievementEngine.evaluateAchievements(
            tasks = listOf(earlyTask, lateTask),
            loadRecords = emptyList(),
            zoneId = shanghaiZone
        )

        assertTrue(badges.first { it.id == "early_bird" }.unlockedAt != null)
        assertTrue(badges.first { it.id == "night_owl" }.unlockedAt != null)
    }

    @Test
    fun habitCompletionDoesNotIncreaseStreakRepeatedlyOnSameDay() {
        val shanghaiZone = ZoneId.of("Asia/Shanghai")
        val todayMorning = Instant.parse("2026-06-02T01:00:00Z").toEpochMilli()
        val todayEvening = Instant.parse("2026-06-02T12:00:00Z").toEpochMilli()
        val habit = SmartTask(title = "喝水", isHabit = true)

        val completed = TaskCompletionEngine.toggleCompletion(habit, todayMorning, shanghaiZone)
        val canceled = TaskCompletionEngine.toggleCompletion(completed, todayEvening, shanghaiZone)
        val completedAgain = TaskCompletionEngine.toggleCompletion(canceled, todayEvening, shanghaiZone)

        assertEquals(1, completed.streak)
        assertEquals(0, canceled.streak)
        assertEquals(1, completedAgain.streak)
        assertEquals(listOf("2026-06-02"), completedAgain.completionHistory)
    }

    @Test
    fun habitCompletionBuildsConsecutiveStreakByLocalDate() {
        val shanghaiZone = ZoneId.of("Asia/Shanghai")
        val dayOne = Instant.parse("2026-06-01T23:30:00Z").toEpochMilli()
        val dayTwo = Instant.parse("2026-06-02T23:30:00Z").toEpochMilli()
        val habit = SmartTask(title = "复盘", isHabit = true)

        val first = TaskCompletionEngine.toggleCompletion(habit, dayOne, shanghaiZone)
        val second = TaskCompletionEngine.toggleCompletion(first, dayTwo, shanghaiZone)

        assertEquals(2, second.streak)
        assertEquals("2026-06-03", second.lastCompletedDate)
        assertEquals(listOf("2026-06-02", "2026-06-03"), second.completionHistory)
    }
}
