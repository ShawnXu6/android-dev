package com.example.android_dev

import com.example.android_dev.ai.AiBreakdownRequest
import com.example.android_dev.ai.DueDateParser
import com.example.android_dev.ai.LocalAiTaskPlanner
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskPriority
import com.example.android_dev.domain.TaskStatus
import com.example.android_dev.engine.TaskCompletionEngine
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewFeaturesTest {

    private val today = LocalDate.of(2026, 6, 9) // 周二

    @Test
    fun dueDateParserHandlesTomorrow() {
        assertEquals(today.plusDays(1), DueDateParser.parse("明天交作业", today))
    }

    @Test
    fun dueDateParserHandlesNextFriday() {
        val result = DueDateParser.parse("下周五前完成期末论文", today)
        assertNotNull(result)
        assertEquals(DayOfWeek.FRIDAY, result!!.dayOfWeek)
        assertTrue("下周五应晚于本周", result.isAfter(today.plusDays(3)))
    }

    @Test
    fun dueDateParserHandlesExplicitChineseDate() {
        assertEquals(LocalDate.of(2026, 6, 20), DueDateParser.parse("6月20日截止", today))
    }

    @Test
    fun dueDateParserReturnsNullWhenNoDate() {
        assertNull(DueDateParser.parse("随便写点什么", today))
    }

    @Test
    fun localPlannerProducesSubtasksWithinDueDate() = runBlocking {
        val due = today.plusDays(7)
        val result = LocalAiTaskPlanner.breakdown(
            AiBreakdownRequest(goal = "完成期末论文", dueDate = due)
        )
        assertTrue("应拆出子任务", result.subtasks.isNotEmpty())
        // 所有计划日期都不应晚于截止日。
        result.subtasks.mapNotNull { it.plannedDate }.forEach { date ->
            assertFalse("子任务计划日期不应晚于截止日", date.isAfter(due))
        }
    }

    @Test
    fun completedAtMigratesToDoneStatus() {
        // 模拟旧数据：只有 completedAt，没有显式 status。
        val legacy = SmartTask(title = "旧任务", completedAt = 1L)
        assertTrue("有 completedAt 即视为已完成", legacy.isCompleted)
    }

    @Test
    fun importanceMapsToPriority() {
        assertEquals(TaskPriority.HIGH, TaskPriority.fromImportance(5))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromImportance(3))
        assertEquals(TaskPriority.LOW, TaskPriority.fromImportance(1))
    }

    @Test
    fun setStatusToDoneMarksCompleted() {
        val task = SmartTask(title = "写代码", status = TaskStatus.TODO)
        val done = TaskCompletionEngine.setStatus(task, TaskStatus.DONE, nowMillis = 1000L)
        assertEquals(TaskStatus.DONE, done.status)
        assertTrue(done.isCompleted)
        assertEquals(1000L, done.completedAt)
    }

    @Test
    fun setStatusBackToTodoClearsCompletion() {
        val done = SmartTask(title = "写代码", status = TaskStatus.DONE, completedAt = 1000L)
        val todo = TaskCompletionEngine.setStatus(done, TaskStatus.TODO)
        assertEquals(TaskStatus.TODO, todo.status)
        assertFalse(todo.isCompleted)
        assertNull(todo.completedAt)
    }

    @Test
    fun toggleNonHabitSyncsStatusAndCompletedAt() {
        val task = SmartTask(title = "普通任务")
        val completed = TaskCompletionEngine.toggleCompletion(task, nowMillis = 500L)
        assertEquals(TaskStatus.DONE, completed.status)
        assertTrue(completed.isCompleted)

        val reverted = TaskCompletionEngine.toggleCompletion(completed, nowMillis = 600L)
        assertEquals(TaskStatus.TODO, reverted.status)
        assertFalse(reverted.isCompleted)
    }
}
