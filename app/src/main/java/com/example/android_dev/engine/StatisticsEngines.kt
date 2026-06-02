package com.example.android_dev.engine

import com.example.android_dev.domain.AchievementBadge
import com.example.android_dev.domain.AchievementCategory
import com.example.android_dev.domain.CognitiveLoadRecord
import com.example.android_dev.domain.CognitiveSnapshot
import com.example.android_dev.domain.CompletionTrend
import com.example.android_dev.domain.DailyStats
import com.example.android_dev.domain.HeatmapData
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskCategory
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.domain.WeeklyReport
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

object StatisticsEngine {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun buildDailyStats(
        tasks: List<SmartTask>,
        loadRecords: List<CognitiveLoadRecord>,
        date: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DailyStats {
        val dateStr = date.format(dateFormatter)
        val createdTodayTasks = tasks.filter {
            val taskDate = it.createdAt.toLocalDate(zoneId)
            taskDate == date
        }
        val dayCompletedTasks = tasks.filter { it.wasCompletedOn(date, zoneId) }
        val dayTasks = (createdTodayTasks + dayCompletedTasks).distinctBy { it.id }
        val categoryMinutes = TaskCategory.entries.associateWith { cat ->
            dayTasks.filter { it.category == cat }.sumOf { it.estimatedMinutes }
        }
        val completedCategoryMinutes = TaskCategory.entries.associateWith { cat ->
            dayCompletedTasks.filter { it.category == cat }.sumOf { it.estimatedMinutes }
        }
        val pendingCategoryMinutes = TaskCategory.entries.associateWith { cat ->
            createdTodayTasks.filter { it.category == cat && !it.wasCompletedOn(date, zoneId) }.sumOf { it.estimatedMinutes }
        }
        val dayRecords = loadRecords.filter {
            it.timestamp.toLocalDate(zoneId) == date
        }
        val avgLoad = if (dayRecords.isEmpty()) 0f else dayRecords.map { it.overall }.average().toFloat()
        val peakHour = dayRecords.groupBy { it.hour }
            .mapValues { (_, records) -> records.map { it.overall }.average().toFloat() }
            .maxByOrNull { it.value }?.key

        return DailyStats(
            date = dateStr,
            totalTasks = dayTasks.size,
            completedTasks = dayCompletedTasks.size,
            totalMinutes = dayCompletedTasks.sumOf { it.estimatedMinutes },
            categoryMinutes = categoryMinutes,
            completedCategoryMinutes = completedCategoryMinutes,
            pendingCategoryMinutes = pendingCategoryMinutes,
            avgCognitiveLoad = avgLoad,
            peakLoadHour = peakHour,
            habitCompleted = tasks.count { it.isHabit && it.wasHabitCompletedOn(date) }
        )
    }

    fun buildWeeklyReport(
        tasks: List<SmartTask>,
        loadRecords: List<CognitiveLoadRecord>,
        weekOffset: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): WeeklyReport {
        val today = LocalDate.now(zoneId)
        val weekStart = today.minusDays((today.dayOfWeek.value - 1 + weekOffset * 7).toLong())
        val dailyStats = (0..6).map { dayOffset ->
            buildDailyStats(tasks, loadRecords, weekStart.plusDays(dayOffset.toLong()), zoneId)
        }

        val totalCompleted = dailyStats.sumOf { it.completedTasks }
        val totalTasks = dailyStats.sumOf { it.totalTasks }
        val avgCompletionRate = if (dailyStats.isEmpty()) 0f else dailyStats.map { it.completionRate }.average().toFloat()

        val categoryDistribution = TaskCategory.entries.associateWith { cat ->
            val total = dailyStats.sumOf { it.categoryMinutes[cat] ?: 0 }
            val grandTotal = dailyStats.sumOf { it.categoryMinutes.values.sum() }
            if (grandTotal == 0) 0f else total.toFloat() / grandTotal
        }
        val completedCategoryDistribution = buildCategoryDistribution(dailyStats) { it.completedCategoryMinutes }
        val pendingCategoryDistribution = buildCategoryDistribution(dailyStats) { it.pendingCategoryMinutes }

        val trend = computeTrend(dailyStats)
        val totalHabitStreak = tasks.filter { it.isHabit }.sumOf { it.streak }

        return WeeklyReport(
            weekStart = weekStart.format(dateFormatter),
            dailyStats = dailyStats,
            totalCompleted = totalCompleted,
            totalTasks = totalTasks,
            avgCompletionRate = avgCompletionRate,
            categoryDistribution = categoryDistribution,
            completedCategoryDistribution = completedCategoryDistribution,
            pendingCategoryDistribution = pendingCategoryDistribution,
            trend = trend,
            totalHabitStreak = totalHabitStreak
        )
    }

    fun buildHeatmapData(
        tasks: List<SmartTask>,
        days: Int = 30,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): HeatmapData {
        val today = LocalDate.now(zoneId)
        val dates = (0 until days).map { today.minusDays(it.toLong()).format(dateFormatter) }.reversed()
        val values = dates.associateWith { dateStr ->
            val date = LocalDate.parse(dateStr, dateFormatter)
            tasks.count { it.isHabit && it.wasHabitCompletedOn(date) }
        }
        val maxStreak = values.values.maxOrNull() ?: 0

        return HeatmapData(
            dates = dates,
            values = values,
            maxStreak = maxStreak
        )
    }

    fun computeLoadCurve(
        loadRecords: List<CognitiveLoadRecord>,
        date: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<CognitiveLoadRecord> {
        return loadRecords
            .filter { it.timestamp.toLocalDate(zoneId) == date }
            .sortedBy { it.hour }
    }

    fun generateLoadRecords(
        tasks: List<SmartTask>,
        signal: UserCognitiveSignal,
        hours: IntRange = 8..22,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<CognitiveLoadRecord> {
        val dayStart = LocalDate.now(zoneId).startOfDayMillis(zoneId)

        return hours.map { hour ->
            val simulatedTasks = tasks.filterNot { it.isCompleted }.take((hour - 8).coerceIn(0, 6))
            val simulatedSignal = signal.copy(
                focus = when (hour) {
                    in 9..11 -> signal.focus * 1.15f
                    in 14..16 -> signal.focus * 0.85f
                    in 19..21 -> signal.focus * 0.9f
                    else -> signal.focus
                }.coerceIn(0f, 1f),
                energy = when (hour) {
                    in 8..10 -> signal.energy * 1.1f
                    in 13..15 -> signal.energy * 0.8f
                    in 20..22 -> signal.energy * 0.7f
                    else -> signal.energy
                }.coerceIn(0f, 1f)
            )
            val snapshot = CognitiveLoadEngine.assess(simulatedTasks, simulatedSignal, hour)
            CognitiveLoadRecord(
                timestamp = dayStart + hour * 60 * 60 * 1000,
                hour = hour,
                overall = snapshot.overall,
                visualLoad = snapshot.visualLoad,
                memoryLoad = snapshot.memoryLoad,
                temporalPressure = snapshot.temporalPressure,
                decisionFatigue = snapshot.decisionFatigue
            )
        }
    }

    private fun computeTrend(dailyStats: List<DailyStats>): CompletionTrend {
        val rates = dailyStats.map { it.completionRate }
        if (rates.size < 3) return CompletionTrend.STABLE

        val firstHalf = rates.take(3).average()
        val secondHalf = rates.takeLast(3).average()
        val diff = secondHalf - firstHalf

        return when {
            diff > 0.08 -> CompletionTrend.IMPROVING
            diff < -0.08 -> CompletionTrend.DECLINING
            else -> CompletionTrend.STABLE
        }
    }

    private fun buildCategoryDistribution(
        dailyStats: List<DailyStats>,
        selector: (DailyStats) -> Map<TaskCategory, Int>
    ): Map<TaskCategory, Float> {
        val grandTotal = dailyStats.sumOf { stats -> selector(stats).values.sum() }
        return TaskCategory.entries.associateWith { cat ->
            val total = dailyStats.sumOf { stats -> selector(stats)[cat] ?: 0 }
            if (grandTotal == 0) 0f else total.toFloat() / grandTotal
        }
    }
}

object AchievementEngine {
    private val allBadges = listOf(
        AchievementBadge(
            id = "first_task",
            title = "第一步",
            description = "完成第一个任务",
            icon = "🎯",
            category = AchievementCategory.COMPLETION,
            unlockedAt = null,
            progress = 0f,
            requirement = "完成 1 个任务"
        ),
        AchievementBadge(
            id = "ten_tasks",
            title = "十项全能",
            description = "累计完成 10 个任务",
            icon = "🏆",
            category = AchievementCategory.COMPLETION,
            unlockedAt = null,
            progress = 0f,
            requirement = "完成 10 个任务"
        ),
        AchievementBadge(
            id = "fifty_tasks",
            title = "五十达成",
            description = "累计完成 50 个任务",
            icon = "👑",
            category = AchievementCategory.COMPLETION,
            unlockedAt = null,
            progress = 0f,
            requirement = "完成 50 个任务"
        ),
        AchievementBadge(
            id = "hundred_tasks",
            title = "百炼成钢",
            description = "累计完成 100 个任务",
            icon = "💎",
            category = AchievementCategory.MILESTONE,
            unlockedAt = null,
            progress = 0f,
            requirement = "完成 100 个任务"
        ),
        AchievementBadge(
            id = "habit_3_days",
            title = "三日坚持",
            description = "习惯连续打卡 3 天",
            icon = "🔥",
            category = AchievementCategory.HABIT,
            unlockedAt = null,
            progress = 0f,
            requirement = "习惯连续 3 天"
        ),
        AchievementBadge(
            id = "habit_7_days",
            title = "一周习惯",
            description = "习惯连续打卡 7 天",
            icon = "⭐",
            category = AchievementCategory.HABIT,
            unlockedAt = null,
            progress = 0f,
            requirement = "习惯连续 7 天"
        ),
        AchievementBadge(
            id = "habit_30_days",
            title = "月度习惯",
            description = "习惯连续打卡 30 天",
            icon = "🌟",
            category = AchievementCategory.HABIT,
            unlockedAt = null,
            progress = 0f,
            requirement = "习惯连续 30 天"
        ),
        AchievementBadge(
            id = "perfect_day",
            title = "完美一天",
            description = "一天内完成所有计划任务",
            icon = "✨",
            category = AchievementCategory.EFFICIENCY,
            unlockedAt = null,
            progress = 0f,
            requirement = "日完成率 100%"
        ),
        AchievementBadge(
            id = "low_load_master",
            title = "负荷管理师",
            description = "连续 3 天保持低认知负荷",
            icon = "🧘",
            category = AchievementCategory.COGNITIVE,
            unlockedAt = null,
            progress = 0f,
            requirement = "连续 3 天负荷 < 0.32"
        ),
        AchievementBadge(
            id = "category_explorer",
            title = "全领域探索",
            description = "在全部 6 个分类中都完成过任务",
            icon = "🌈",
            category = AchievementCategory.MILESTONE,
            unlockedAt = null,
            progress = 0f,
            requirement = "完成全部 6 分类任务"
        ),
        AchievementBadge(
            id = "early_bird",
            title = "早起鸟",
            description = "在 9 点前完成一个任务",
            icon = "🌅",
            category = AchievementCategory.EFFICIENCY,
            unlockedAt = null,
            progress = 0f,
            requirement = "9 点前完成任务"
        ),
        AchievementBadge(
            id = "night_owl",
            title = "夜猫子",
            description = "在 22 点后完成一个任务",
            icon = "🦉",
            category = AchievementCategory.EFFICIENCY,
            unlockedAt = null,
            progress = 0f,
            requirement = "22 点后完成任务"
        )
    )

    fun evaluateAchievements(
        tasks: List<SmartTask>,
        loadRecords: List<CognitiveLoadRecord>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<AchievementBadge> {
        val completedTasks = tasks.filter { it.isCompleted }
        val completedCount = completedTasks.size
        val maxHabitStreak = tasks.filter { it.isHabit }.maxOfOrNull { it.streak } ?: 0
        val categoriesWithCompletions = completedTasks.map { it.category }.distinct().size
        val hasPerfectDay = tasks
            .groupBy { it.createdAt.toLocalDate(zoneId) }
            .any { (_, dayTasks) ->
                dayTasks.all { it.isCompleted } && dayTasks.isNotEmpty()
            }
        val hasEarlyTask = completedTasks.any { task ->
            task.completedAt?.toLocalHour(zoneId)?.let { it < 9 } == true
        }
        val hasLateTask = completedTasks.any { task ->
            task.completedAt?.toLocalHour(zoneId)?.let { it >= 22 } == true
        }
        val lowLoadDays = loadRecords
            .groupBy { it.timestamp.toLocalDate(zoneId) }
            .count { (_, records) ->
                records.all { it.overall < 0.32f } && records.isNotEmpty()
            }

        return allBadges.map { badge ->
            val (unlocked, progress) = when (badge.id) {
                "first_task" -> Pair(completedCount >= 1, completedCount / 1f)
                "ten_tasks" -> Pair(completedCount >= 10, completedCount / 10f)
                "fifty_tasks" -> Pair(completedCount >= 50, completedCount / 50f)
                "hundred_tasks" -> Pair(completedCount >= 100, completedCount / 100f)
                "habit_3_days" -> Pair(maxHabitStreak >= 3, maxHabitStreak / 3f)
                "habit_7_days" -> Pair(maxHabitStreak >= 7, maxHabitStreak / 7f)
                "habit_30_days" -> Pair(maxHabitStreak >= 30, maxHabitStreak / 30f)
                "perfect_day" -> Pair(hasPerfectDay, if (hasPerfectDay) 1f else 0f)
                "low_load_master" -> Pair(lowLoadDays >= 3, lowLoadDays / 3f)
                "category_explorer" -> Pair(categoriesWithCompletions >= 6, categoriesWithCompletions / 6f)
                "early_bird" -> Pair(hasEarlyTask, if (hasEarlyTask) 1f else 0f)
                "night_owl" -> Pair(hasLateTask, if (hasLateTask) 1f else 0f)
                else -> Pair(false, 0f)
            }

            badge.copy(
                unlockedAt = if (unlocked && badge.unlockedAt == null) System.currentTimeMillis() else badge.unlockedAt,
                progress = progress.coerceIn(0f, 1f)
            )
        }
    }

    fun getUnlockedCount(badges: List<AchievementBadge>): Int {
        return badges.count { it.unlockedAt != null }
    }

    fun getTotalProgress(badges: List<AchievementBadge>): Float {
        if (badges.isEmpty()) return 0f
        return badges.map { it.progress }.average().toFloat()
    }

}

private fun SmartTask.wasHabitCompletedOn(date: LocalDate): Boolean {
    return completionHistory.contains(date.toString()) || lastCompletedDate == date.toString()
}

private fun SmartTask.wasCompletedOn(date: LocalDate, zoneId: ZoneId): Boolean {
    if (isHabit) return wasHabitCompletedOn(date)
    return completedAt?.toLocalDate(zoneId) == date
}

private fun Long.toLocalDate(zoneId: ZoneId): LocalDate {
    return Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
}

private fun Long.toLocalHour(zoneId: ZoneId): Int {
    return Instant.ofEpochMilli(this).atZone(zoneId).toLocalTime().hour
}

private fun LocalDate.startOfDayMillis(zoneId: ZoneId): Long {
    return atStartOfDay(zoneId).toInstant().toEpochMilli()
}
