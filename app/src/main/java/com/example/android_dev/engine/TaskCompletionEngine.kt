package com.example.android_dev.engine

import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object TaskCompletionEngine {
    fun toggleCompletion(
        task: SmartTask,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): SmartTask {
        return if (task.isHabit) {
            toggleHabitCompletion(task, nowMillis, zoneId)
        } else {
            toggleOneTimeTaskCompletion(task, nowMillis)
        }
    }

    fun calculateHabitStreak(completionHistory: List<String>, anchorDate: LocalDate): Int {
        val completedDates = completionHistory.mapNotNull { it.toLocalDateOrNull() }.toSet()
        var date = anchorDate
        var streak = 0

        while (completedDates.contains(date)) {
            streak += 1
            date = date.minusDays(1)
        }

        return streak
    }

    private fun toggleOneTimeTaskCompletion(task: SmartTask, nowMillis: Long): SmartTask {
        return if (task.isCompleted) {
            // 取消完成：回到进行中（若曾推进过）或待处理，并清空完成时间。
            val restored = if (task.status == TaskStatus.DONE) TaskStatus.TODO else task.status
            task.copy(completedAt = null, status = restored)
        } else {
            // 标记完成：同步置为已完成状态，保证看板与列表一致。
            task.copy(completedAt = nowMillis, status = TaskStatus.DONE)
        }
    }

    // 看板状态切换功能：直接把任务设置到指定列，并同步完成时间。
    fun setStatus(task: SmartTask, status: TaskStatus, nowMillis: Long = System.currentTimeMillis()): SmartTask {
        if (task.status == status) return task
        return when (status) {
            TaskStatus.DONE -> task.copy(status = TaskStatus.DONE, completedAt = task.completedAt ?: nowMillis)
            else -> task.copy(status = status, completedAt = null)
        }
    }

    private fun toggleHabitCompletion(task: SmartTask, nowMillis: Long, zoneId: ZoneId): SmartTask {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val todayText = today.toString()
        val normalizedHistory = task.normalizedCompletionHistory(zoneId).toMutableSet()

        if (normalizedHistory.contains(todayText)) {
            normalizedHistory.remove(todayText)
            val updatedHistory = normalizedHistory.sorted()
            val latestCompletedDate = updatedHistory.lastOrNull()
            val streakAnchor = latestCompletedDate?.let(LocalDate::parse)

            return task.copy(
                completedAt = null,
                habitId = task.habitId ?: task.id,
                lastCompletedDate = latestCompletedDate,
                completionHistory = updatedHistory,
                streak = streakAnchor?.let { calculateHabitStreak(updatedHistory, it) } ?: 0
            )
        }

        normalizedHistory.add(todayText)
        val updatedHistory = normalizedHistory.sorted()
        return task.copy(
            completedAt = nowMillis,
            habitId = task.habitId ?: task.id,
            lastCompletedDate = todayText,
            completionHistory = updatedHistory,
            streak = calculateHabitStreak(updatedHistory, today)
        )
    }

    private fun SmartTask.normalizedCompletionHistory(zoneId: ZoneId): Set<String> {
        return buildSet {
            completionHistory.filter { it.toLocalDateOrNull() != null }.forEach(::add)
            lastCompletedDate?.takeIf { it.toLocalDateOrNull() != null }?.let(::add)
            completedAt?.let { timestamp ->
                add(Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate().toString())
            }
        }
    }

    private fun String.toLocalDateOrNull(): LocalDate? {
        return runCatching { LocalDate.parse(this) }.getOrNull()
    }
}
