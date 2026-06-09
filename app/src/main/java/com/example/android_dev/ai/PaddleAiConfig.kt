package com.example.android_dev.ai

// 飞桨 AIStudio 配置功能：集中存放大模型访问地址、模型名与访问令牌。
//
// 使用说明：
//   1. 登录百度飞桨 AIStudio（https://aistudio.baidu.com/），在「个人中心 -> 访问令牌」生成 Access Token。
//   2. 把令牌字符串填入下方 API_TOKEN（保持引号），例如 const val API_TOKEN = "ak-xxxxxxxx"。
//   3. 若使用的不是 ERNIE 系列，可同时调整 MODEL。
//
// 留空时（API_TOKEN 为空字符串），应用会自动降级到本地启发式拆解（LocalAiTaskPlanner），
// 保证「AI 智能拆解」功能在无网络/无令牌时依旧可用。
object PaddleAiConfig {

    // 飞桨 AIStudio 兼容 OpenAI 的对话补全接口地址。
    const val BASE_URL = "https://aistudio.baidu.com/llm/lmapi/v3/chat/completions"

    // 默认调用的大模型名称（飞桨 AIStudio 上的 ERNIE 模型标识，可按需替换）。
    const val MODEL = "ernie-4.5-turbo-128k"

    // TODO: 在此填入你的飞桨 AIStudio 访问令牌（Access Token）。留空则使用本地兜底拆解。
    const val API_TOKEN = "cd8cc85ba5bf76fefc7d4d9d61afff5cabaccc64"

    // 连接超时（秒）：连不上就快速失败。
    const val CONNECT_TIMEOUT_SECONDS = 15L

    // 读/写/整体超时（秒）：大模型生成较慢，给足时间，避免长任务被误判超时。
    const val CALL_TIMEOUT_SECONDS = 90L

    // 兼容旧引用的单次请求超时（秒）。
    const val TIMEOUT_SECONDS = CALL_TIMEOUT_SECONDS

    // 是否已配置令牌：用于决定走远程还是本地。
    val hasToken: Boolean
        get() = API_TOKEN.isNotBlank()
}
