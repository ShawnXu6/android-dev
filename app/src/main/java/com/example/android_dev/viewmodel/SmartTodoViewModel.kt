package com.example.android_dev.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_dev.ai.AiBreakdownRequest
import com.example.android_dev.ai.AiChatClient
import com.example.android_dev.ai.AiTaskPlannerRepository
import com.example.android_dev.data.LocalSmartTodoRepository
import com.example.android_dev.domain.AchievementBadge
import com.example.android_dev.domain.AiBreakdownResult
import com.example.android_dev.domain.ChatMessage
import com.example.android_dev.domain.ChatRole
import com.example.android_dev.domain.ExtractedTask
import com.example.android_dev.domain.CognitiveLoadRecord
import com.example.android_dev.domain.CognitiveSnapshot
import com.example.android_dev.domain.HeatmapData
import com.example.android_dev.domain.ProductivityInsight
import com.example.android_dev.domain.ScheduleSlot
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskRecommendation
import com.example.android_dev.domain.TaskStatus
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.domain.WeeklyReport
import com.example.android_dev.engine.AchievementEngine
import com.example.android_dev.engine.CognitiveLoadEngine
import com.example.android_dev.engine.SmartTaskEngine
import com.example.android_dev.engine.StatisticsEngine
import com.example.android_dev.engine.TaskCompletionEngine
import com.example.android_dev.reminder.ReminderScheduler
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 主界面状态功能：集中承载页面渲染所需的原始数据和派生数据。
data class SmartTodoUiState(
    val tasks: List<SmartTask>,
    val signal: UserCognitiveSignal,
    val snapshot: CognitiveSnapshot,
    val schedule: List<ScheduleSlot>,
    val insights: List<ProductivityInsight>,
    val nextTask: SmartTask?,
    val nextRecommendation: TaskRecommendation?,
    val weeklyReport: WeeklyReport,
    val heatmapData: HeatmapData,
    val loadCurve: List<CognitiveLoadRecord>,
    val predictedLoadCurve: List<CognitiveLoadRecord>,
    val achievements: List<AchievementBadge>
)

// AI 对话状态功能：承载消息历史、发送中标记和错误提示。
data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val error: String? = null
)

// 智能待办 ViewModel 功能：负责读取、保存、更新任务状态，并调用引擎生成界面状态。
class SmartTodoViewModel(
    private val repository: LocalSmartTodoRepository,
    private val reminderScheduler: ReminderScheduler? = null,
    private val aiPlannerRepository: AiTaskPlannerRepository = AiTaskPlannerRepository(),
    private val aiChatClient: AiChatClient = AiChatClient(),
    private val currentHour: Int = LocalTime.now().hour,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : ViewModel() {
    private val _uiState = MutableStateFlow(buildInitialState())
    val uiState: StateFlow<SmartTodoUiState> = _uiState.asStateFlow()

    // AI 对话状态流。
    private val _chatState = MutableStateFlow(AiChatUiState())
    val chatState: StateFlow<AiChatUiState> = _chatState.asStateFlow()

    init {
        // 应用启动时按当前任务重建所有提醒，保证杀进程后提醒依旧生效。
        reminderScheduler?.rescheduleAll(_uiState.value.tasks)
    }

    // 快速新增功能：根据标题和备注调用智能引擎创建任务并持久化。
    fun quickAddTask(title: String, note: String) {
        val task = SmartTaskEngine.createTaskFromInput(
            rawTitle = title,
            note = note,
            signal = _uiState.value.signal,
            nowHour = currentHour
        )
        addTask(task)
    }

    // 手动新增功能：保存从编辑弹窗创建出的完整任务，并按需调度提醒。
    fun addTask(task: SmartTask) {
        reminderScheduler?.schedule(task)
        updateTasks(listOf(task) + _uiState.value.tasks)
    }

    // 编辑保存功能：用同 id 的新任务替换旧任务，并重排提醒。
    fun updateTask(task: SmartTask) {
        reminderScheduler?.cancel(task)
        reminderScheduler?.schedule(task)
        updateTasks(_uiState.value.tasks.map { if (it.id == task.id) task else it })
    }

    // 看板状态切换功能：把任务移动到指定列（待处理/进行中/已完成）。
    fun moveTaskStatus(task: SmartTask, status: TaskStatus) {
        val updated = TaskCompletionEngine.setStatus(task, status)
        if (updated.isCompleted) reminderScheduler?.cancel(updated)
        updateTasks(_uiState.value.tasks.map { if (it.id == updated.id) updated else it })
    }

    // 任务完成切换功能：切换完成时间，并同步更新习惯连续天数。
    fun toggleTaskCompletion(task: SmartTask) {
        updateTasks(_uiState.value.tasks.toggleCompletion(task))
    }

    // 删除任务功能：从任务池移除指定任务、取消提醒并持久化。
    fun deleteTask(task: SmartTask) {
        reminderScheduler?.cancel(task)
        updateTasks(_uiState.value.tasks.filterNot { it.id == task.id })
    }

    // AI 拆解功能：在协程里请求拆解结果，成功/失败分别回调到界面。
    fun requestAiBreakdown(
        request: AiBreakdownRequest,
        onResult: (AiBreakdownResult) -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            runCatching { aiPlannerRepository.breakdown(request) }
                .onSuccess(onResult)
                .onFailure { onError() }
        }
    }

    // AI 对话发送功能：把用户消息追加进历史，调用大模型，再追加 AI 回复。
    fun sendChatMessage(text: String) {
        val content = text.trim()
        if (content.isBlank() || _chatState.value.isSending) return

        val userMessage = ChatMessage(role = ChatRole.USER, content = content)
        val history = _chatState.value.messages + userMessage
        _chatState.value = _chatState.value.copy(messages = history, isSending = true, error = null)

        viewModelScope.launch {
            runCatching { aiChatClient.send(history) }
                .onSuccess { reply ->
                    _chatState.value = _chatState.value.copy(
                        messages = _chatState.value.messages + ChatMessage(role = ChatRole.ASSISTANT, content = reply),
                        isSending = false
                    )
                }
                .onFailure { e ->
                    _chatState.value = _chatState.value.copy(
                        isSending = false,
                        error = friendlyAiError(e)
                    )
                }
        }
    }

    // 清空对话功能：开始一段新会话。
    fun clearChat() {
        _chatState.value = AiChatUiState()
    }

    // 从对话提取任务功能：把一条 AI 回复解析成可加入计划的任务列表。
    fun extractTasksFromMessage(
        text: String,
        onResult: (List<ExtractedTask>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching { aiChatClient.extractTasks(text) }
                .onSuccess(onResult)
                .onFailure { onError(friendlyAiError(it)) }
        }
    }

    // 错误友好化功能：把网络/超时等技术异常翻译成用户能看懂、可操作的提示。
    private fun friendlyAiError(e: Throwable): String {
        val msg = e.message.orEmpty()
        return when {
            e is java.net.SocketTimeoutException || msg.contains("timeout", ignoreCase = true) ->
                "网络较慢，AI 响应超时了，请重试一次。"
            e is java.net.UnknownHostException || msg.contains("Unable to resolve host") ->
                "连不上网络，请检查手机网络后重试。"
            msg.contains("401") || msg.contains("403") ->
                "访问令牌无效或已过期，请检查飞桨令牌配置。"
            else -> msg.ifBlank { "提取失败，请稍后再试。" }
        }
    }

    // 批量加入计划功能：把用户确认的提取任务转成正式任务并持久化。
    fun addExtractedTasks(tasks: List<ExtractedTask>) {
        if (tasks.isEmpty()) return
        // 复用语义引擎补全分类/复杂度/耗时，再覆盖用户已确认的标题、日期、优先级、分钟。
        val newTasks = tasks.map { extracted ->
            val base = SmartTaskEngine.createTaskFromInput(
                rawTitle = extracted.title,
                note = "",
                signal = _uiState.value.signal,
                nowHour = currentHour
            )
            base.copy(
                dueDate = extracted.plannedDate ?: base.dueDate,
                priority = extracted.priority,
                estimatedMinutes = extracted.estimatedMinutes
            )
        }
        newTasks.forEach { reminderScheduler?.schedule(it) }
        updateTasks(newTasks + _uiState.value.tasks)
    }

    // 用户状态更新功能：保存专注、精力、压力、情绪、环境和自适应模式。
    fun updateSignal(signal: UserCognitiveSignal) {
        repository.saveSignal(signal)
        _uiState.value = buildUiState(tasks = _uiState.value.tasks, signal = signal)
    }

    private fun updateTasks(tasks: List<SmartTask>) {
        repository.saveTasks(tasks)
        _uiState.value = buildUiState(tasks = tasks, signal = _uiState.value.signal)
    }

    private fun buildInitialState(): SmartTodoUiState {
        return buildUiState(
            tasks = repository.loadTasks(),
            signal = repository.loadSignal()
        )
    }

    private fun buildUiState(tasks: List<SmartTask>, signal: UserCognitiveSignal): SmartTodoUiState {
        val snapshot = CognitiveLoadEngine.assess(tasks, signal, currentHour)
        val loadRecords = saveAndLoadCurrentRecords(snapshot)
        val nextRecommendation = SmartTaskEngine.recommendNextTaskWithExplanation(tasks, signal, currentHour)

        return SmartTodoUiState(
            tasks = tasks,
            signal = signal,
            snapshot = snapshot,
            schedule = SmartTaskEngine.buildSchedule(tasks, signal, currentHour),
            insights = SmartTaskEngine.generateInsights(tasks, signal),
            nextTask = nextRecommendation?.task,
            nextRecommendation = nextRecommendation,
            weeklyReport = StatisticsEngine.buildWeeklyReport(tasks, loadRecords),
            heatmapData = StatisticsEngine.buildHeatmapData(tasks),
            loadCurve = StatisticsEngine.computeLoadCurve(loadRecords),
            predictedLoadCurve = StatisticsEngine.generateLoadRecords(tasks, signal),
            achievements = AchievementEngine.evaluateAchievements(tasks, loadRecords)
        )
    }

    // 认知负荷记录刷新功能：保存当前快照后立即读取最新记录，保证统计页使用最新数据。
    private fun saveAndLoadCurrentRecords(snapshot: CognitiveSnapshot): List<CognitiveLoadRecord> {
        repository.saveLoadRecord(
            CognitiveLoadRecord(
                timestamp = System.currentTimeMillis(),
                hour = currentHour,
                overall = snapshot.overall,
                visualLoad = snapshot.visualLoad,
                memoryLoad = snapshot.memoryLoad,
                temporalPressure = snapshot.temporalPressure,
                decisionFatigue = snapshot.decisionFatigue
            )
        )
        return repository.loadLoadRecords()
    }

    private fun List<SmartTask>.toggleCompletion(task: SmartTask): List<SmartTask> {
        return map {
            if (it.id == task.id) TaskCompletionEngine.toggleCompletion(it, zoneId = zoneId) else it
        }
    }
}
