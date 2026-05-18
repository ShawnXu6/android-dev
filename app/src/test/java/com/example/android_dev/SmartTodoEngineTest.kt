package com.example.android_dev

import com.example.android_dev.domain.CognitiveLoadLevel
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskCategory
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.engine.CognitiveLoadEngine
import com.example.android_dev.engine.SmartTaskEngine
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
}
