package com.example.android_dev.ai

import com.example.android_dev.domain.AiBreakdownResult
import com.example.android_dev.domain.AiSource
import com.example.android_dev.domain.Subtask
import com.example.android_dev.domain.TaskCategory
import com.example.android_dev.engine.SmartTaskEngine
import java.time.LocalDate

// 本地启发式任务拆解功能：无令牌 / 断网 / 远程失败时的兜底实现。
// 根据任务类别套用通用步骤模板，并把子任务沿截止日期倒排成时间线。
object LocalAiTaskPlanner : AiTaskPlanner {

    override suspend fun breakdown(request: AiBreakdownRequest): AiBreakdownResult {
        val goal = request.goal.trim().ifBlank { "未命名目标" }
        val dueDate = request.dueDate ?: DueDateParser.parse(goal + " " + request.note)
        val category = SmartTaskEngine.classify(goal, request.note)

        val steps = templateFor(category, goal)
        val timeline = distributeAcross(steps.size, dueDate)

        val subtasks = steps.mapIndexed { index, step ->
            Subtask(
                title = step.title,
                estimatedMinutes = step.minutes,
                plannedDate = timeline.getOrNull(index)
            )
        }

        val summary = buildString {
            append("已为「")
            append(goal.take(18))
            append("」拆解为 ${subtasks.size} 个步骤")
            if (dueDate != null) append("，并按截止日 $dueDate 倒排时间线")
            append("。")
        }

        return AiBreakdownResult(subtasks = subtasks, summary = summary, source = AiSource.LOCAL)
    }

    private data class Step(val title: String, val minutes: Int)

    // 步骤模板功能：按任务类别给出常见的分解步骤。
    private fun templateFor(category: TaskCategory, goal: String): List<Step> {
        val core = goal.take(12)
        return when (category) {
            TaskCategory.STUDY -> listOf(
                Step("文献/资料调研：$core", 45),
                Step("梳理大纲与框架", 40),
                Step("撰写初稿", 90),
                Step("修改完善与查证", 60),
                Step("定稿与排版检查", 30)
            )
            TaskCategory.WORK -> listOf(
                Step("明确目标与拆解范围", 25),
                Step("收集所需信息与依赖", 40),
                Step("完成核心产出", 90),
                Step("自查与同步反馈", 30),
                Step("交付与收尾", 20)
            )
            TaskCategory.CREATIVE -> listOf(
                Step("收集灵感与参考", 30),
                Step("草拟概念与原型", 50),
                Step("制作主体内容", 90),
                Step("打磨细节", 45),
                Step("成品输出", 25)
            )
            TaskCategory.HEALTH -> listOf(
                Step("制定计划与目标", 15),
                Step("准备所需条件", 15),
                Step("执行主要行动", 40),
                Step("记录与复盘", 10)
            )
            TaskCategory.SOCIAL -> listOf(
                Step("梳理沟通要点", 15),
                Step("准备材料/约定时间", 20),
                Step("进行沟通协作", 40),
                Step("跟进与确认结论", 15)
            )
            TaskCategory.LIFE -> listOf(
                Step("列出待办清单", 15),
                Step("准备与采购", 30),
                Step("执行主要事项", 40),
                Step("收尾与整理", 15)
            )
        }
    }

    // 时间线分配功能：把步骤数均匀铺到「今天 .. 截止日」之间，无截止日则从今天起逐日递推。
    private fun distributeAcross(count: Int, dueDate: LocalDate?): List<LocalDate> {
        if (count == 0) return emptyList()
        val start = LocalDate.now()
        val end = dueDate ?: start.plusDays((count - 1).toLong())
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(start, end).coerceAtLeast(0)

        return (0 until count).map { index ->
            val offset = if (count == 1) {
                totalDays
            } else {
                Math.round(totalDays.toDouble() * index / (count - 1))
            }
            start.plusDays(offset)
        }
    }
}
