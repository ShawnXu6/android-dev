package com.example.android_dev.engine

import com.example.android_dev.domain.SmartTask
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
            task.copy(completedAt = null)
        } else {
            task.copy(completedAt = nowMillis)
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
