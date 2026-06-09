package com.example.android_dev.ai

import com.example.android_dev.domain.AiBreakdownResult
import java.time.LocalDate

// AI 任务拆解请求功能：描述要拆解的目标、可选截止日期与可选补充说明。
data class AiBreakdownRequest(
    val goal: String,
    val dueDate: LocalDate? = null,
    val note: String = ""
)

// 任务规划器接口功能：把一个目标拆解为带时间线的子任务，远程与本地实现共用此契约。
interface AiTaskPlanner {
    suspend fun breakdown(request: AiBreakdownRequest): AiBreakdownResult
}
