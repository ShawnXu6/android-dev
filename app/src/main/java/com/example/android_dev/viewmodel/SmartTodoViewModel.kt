package com.example.android_dev.viewmodel

import androidx.lifecycle.ViewModel
import com.example.android_dev.data.LocalSmartTodoRepository
import com.example.android_dev.domain.AchievementBadge
import com.example.android_dev.domain.CognitiveLoadRecord
import com.example.android_dev.domain.CognitiveSnapshot
import com.example.android_dev.domain.HeatmapData
import com.example.android_dev.domain.ProductivityInsight
import com.example.android_dev.domain.ScheduleSlot
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskRecommendation
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.domain.WeeklyReport
import com.example.android_dev.engine.AchievementEngine
import com.example.android_dev.engine.CognitiveLoadEngine
import com.example.android_dev.engine.SmartTaskEngine
import com.example.android_dev.engine.StatisticsEngine
import com.example.android_dev.engine.TaskCompletionEngine
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

// 智能待办 ViewModel 功能：负责读取、保存、更新任务状态，并调用引擎生成界面状态。
class SmartTodoViewModel(
    private val repository: LocalSmartTodoRepository,
    private val currentHour: Int = LocalTime.now().hour,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : ViewModel() {
    private val _uiState = MutableStateFlow(buildInitialState())
    val uiState: StateFlow<SmartTodoUiState> = _uiState.asStateFlow()

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

    // 手动新增功能：保存从编辑弹窗创建出的完整任务。
    fun addTask(task: SmartTask) {
        updateTasks(listOf(task) + _uiState.value.tasks)
    }

    // 任务完成切换功能：切换完成时间，并同步更新习惯连续天数。
    fun toggleTaskCompletion(task: SmartTask) {
        updateTasks(_uiState.value.tasks.toggleCompletion(task))
    }

    // 删除任务功能：从任务池移除指定任务并持久化。
    fun deleteTask(task: SmartTask) {
        updateTasks(_uiState.value.tasks.filterNot { it.id == task.id })
    }

    // 更新任务功能：替换已有任务并持久化。
    fun updateTask(updatedTask: SmartTask) {
        updateTasks(_uiState.value.tasks.map {
            if (it.id == updatedTask.id) updatedTask else it
        })
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
