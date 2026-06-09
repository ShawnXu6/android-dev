package com.example.android_dev.ai

import android.util.Log
import com.example.android_dev.domain.AiBreakdownResult

// AI 拆解仓库功能：对外暴露统一的拆解入口，优先走远程大模型，失败或无令牌时自动降级本地兜底。
class AiTaskPlannerRepository(
    private val remotePlanner: AiTaskPlanner = RemoteAiTaskPlanner(),
    private val localPlanner: AiTaskPlanner = LocalAiTaskPlanner
) {
    suspend fun breakdown(request: AiBreakdownRequest): AiBreakdownResult {
        if (PaddleAiConfig.hasToken) {
            runCatching { remotePlanner.breakdown(request) }
                .onSuccess { return it }
                .onFailure { Log.w(TAG, "远程拆解失败，降级本地兜底", it) }
        }
        return localPlanner.breakdown(request)
    }

    private companion object {
        const val TAG = "AiTaskPlanner"
    }
}
