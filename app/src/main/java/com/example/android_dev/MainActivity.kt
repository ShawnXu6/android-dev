@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.android_dev.data.LocalSmartTodoRepository
import com.example.android_dev.domain.AchievementBadge
import com.example.android_dev.domain.CognitiveLoadLevel
import com.example.android_dev.domain.CognitiveSnapshot
import com.example.android_dev.domain.EmotionalTone
import com.example.android_dev.domain.EnvironmentContext
import com.example.android_dev.domain.HabitPlan
import com.example.android_dev.domain.InsightSeverity
import com.example.android_dev.domain.ProductivityInsight
import com.example.android_dev.domain.ScheduleSlot
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskCategory
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.engine.AchievementEngine
import com.example.android_dev.engine.CognitiveLoadEngine
import com.example.android_dev.engine.SmartTaskEngine
import com.example.android_dev.engine.StatisticsEngine
import com.example.android_dev.ui.StatisticsScreen
import com.example.android_dev.ui.theme.AndroiddevTheme
import java.time.LocalTime
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroiddevTheme(dynamicColor = false) {
                SmartTodoLifeApp()
            }
        }
    }
}

@Composable
private fun SmartTodoLifeApp() {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { LocalSmartTodoRepository(context) }
    var tasks by remember { mutableStateOf(repository.loadTasks()) }
    var signal by remember { mutableStateOf(repository.loadSignal()) }
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.TODAY) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    val currentHour = remember { LocalTime.now().hour }

    val snapshot = remember(tasks, signal, currentHour) {
        CognitiveLoadEngine.assess(tasks, signal, currentHour)
    }
    val schedule = remember(tasks, signal, currentHour) {
        SmartTaskEngine.buildSchedule(tasks, signal, currentHour)
    }
    val insights = remember(tasks, signal) {
        SmartTaskEngine.generateInsights(tasks, signal)
    }
    val nextTask = remember(tasks, signal, currentHour) {
        SmartTaskEngine.recommendNextTask(tasks, signal, currentHour)
    }
    val loadRecords = remember { repository.loadLoadRecords() }
    val weeklyReport = remember(tasks, loadRecords) {
        StatisticsEngine.buildWeeklyReport(tasks, loadRecords)
    }
    val heatmapData = remember(tasks) { StatisticsEngine.buildHeatmapData(tasks) }
    val loadCurve = remember(tasks, signal) {
        StatisticsEngine.generateLoadRecords(tasks, signal)
    }
    val achievements = remember(tasks, loadRecords) {
        AchievementEngine.evaluateAchievements(tasks, loadRecords)
    }

    // 保存当前负荷记录
    LaunchedEffect(tasks, signal, currentHour) {
        repository.saveLoadRecord(
            com.example.android_dev.domain.CognitiveLoadRecord(
                timestamp = System.currentTimeMillis(),
                hour = currentHour,
                overall = snapshot.overall,
                visualLoad = snapshot.visualLoad,
                memoryLoad = snapshot.memoryLoad,
                temporalPressure = snapshot.temporalPressure,
                decisionFatigue = snapshot.decisionFatigue
            )
        )
    }

    fun persistTasks(updated: List<SmartTask>) {
        tasks = updated
        repository.saveTasks(updated)
    }

    fun persistSignal(updated: UserCognitiveSignal) {
        signal = updated
        repository.saveSignal(updated)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("智能TodoLife", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${snapshot.level.label} · ${tasks.count { !it.isCompleted }} 项待办",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            SmartBottomBar(selectedTab = selectedTab, onSelect = { selectedTab = it })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showEditor = true }) {
                Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.TODAY -> DashboardScreen(
                    tasks = tasks,
                    signal = signal,
                    snapshot = snapshot,
                    schedule = schedule,
                    nextTask = nextTask,
                    onSignalChange = ::persistSignal,
                    onAddTask = { task -> persistTasks(listOf(task) + tasks) },
                    onToggleTask = { task -> persistTasks(tasks.toggleCompletion(task)) }
                )

                AppTab.TASKS -> TaskListScreen(
                    tasks = tasks,
                    signal = signal,
                    onToggleTask = { task -> persistTasks(tasks.toggleCompletion(task)) },
                    onDeleteTask = { task -> persistTasks(tasks.filterNot { it.id == task.id }) }
                )

                AppTab.INSIGHTS -> InsightsScreen(
                    tasks = tasks,
                    signal = signal,
                    snapshot = snapshot,
                    insights = insights,
                    nextTask = nextTask
                )

                AppTab.STATISTICS -> StatisticsScreen(
                    tasks = tasks,
                    weeklyReport = weeklyReport,
                    heatmapData = heatmapData,
                    loadCurve = loadCurve,
                    achievements = achievements
                )
            }
        }
    }

    if (showEditor) {
        TaskEditorDialog(
            signal = signal,
            onDismiss = { showEditor = false },
            onCreate = { task ->
                persistTasks(listOf(task) + tasks)
                showEditor = false
            }
        )
    }
}

@Composable
private fun DashboardScreen(
    tasks: List<SmartTask>,
    signal: UserCognitiveSignal,
    snapshot: CognitiveSnapshot,
    schedule: List<ScheduleSlot>,
    nextTask: SmartTask?,
    onSignalChange: (UserCognitiveSignal) -> Unit,
    onAddTask: (SmartTask) -> Unit,
    onToggleTask: (SmartTask) -> Unit
) {
    val simplified = signal.adaptiveMode && snapshot.level >= CognitiveLoadLevel.HIGH
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CognitiveStatusPanel(snapshot = snapshot, signal = signal)
        QuickCapturePanel(signal = signal, onAddTask = onAddTask)

        nextTask?.let {
            RecommendedTaskPanel(
                task = it,
                signal = signal,
                onToggleTask = { onToggleTask(it) }
            )
        }

        CognitiveControls(signal = signal, onSignalChange = onSignalChange)

        AnimatedVisibility(visible = !simplified) {
            SchedulePanel(schedule = schedule)
        }
        AnimatedVisibility(visible = simplified) {
            MinimalFocusPanel(tasks = tasks, nextTask = nextTask)
        }
    }
}

@Composable
private fun CognitiveStatusPanel(snapshot: CognitiveSnapshot, signal: UserCognitiveSignal) {
    ElevatedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("认知负荷", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = snapshot.level.label,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusBadge(text = percent(snapshot.overall), color = snapshot.level.tint())
            }
            LinearProgressIndicator(
                progress = { snapshot.overall },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = snapshot.level.tint(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = snapshot.recommendation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                MetricPill("视觉", snapshot.visualLoad)
                MetricPill("记忆", snapshot.memoryLoad)
                MetricPill("时间", snapshot.temporalPressure)
                MetricPill("决策", snapshot.decisionFatigue)
                MetricPill("专注", signal.focus)
            }
        }
    }
}

@Composable
private fun QuickCapturePanel(signal: UserCognitiveSignal, onAddTask: (SmartTask) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("快速捕捉", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("任务") },
                placeholder = { Text("例如：今天下午整理项目计划") },
                maxLines = 2
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("补充信息") },
                maxLines = 2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本地语义分类 · 时间估算 · 负荷适配",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    enabled = text.isNotBlank(),
                    onClick = {
                        onAddTask(SmartTaskEngine.createTaskFromInput(text, note, signal))
                        text = ""
                        note = ""
                    }
                ) {
                    Text("加入")
                }
            }
        }
    }
}

@Composable
private fun RecommendedTaskPanel(
    task: SmartTask,
    signal: UserCognitiveSignal,
    onToggleTask: () -> Unit
) {
    val prediction = remember(task, signal) { SmartTaskEngine.predictTime(task, signal) }
    ElevatedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("下一步推荐", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusBadge("${prediction.minutes} 分", MaterialTheme.colorScheme.primary)
            }
            Text(
                text = "${prediction.rationale}，置信度 ${percent(prediction.confidence)}，不确定性约 ±${prediction.uncertaintyMinutes} 分钟。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(task.category.label) })
                AssistChip(onClick = {}, label = { Text("复杂度 ${task.complexity}/5") })
                AssistChip(onClick = {}, label = { Text(task.targetHour.hourLabel()) })
            }
            Button(onClick = onToggleTask, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("标记完成")
            }
        }
    }
}

@Composable
private fun CognitiveControls(signal: UserCognitiveSignal, onSignalChange: (UserCognitiveSignal) -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("状态感知", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "由用户可控信号驱动，避免隐式采集敏感生理数据。",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = signal.adaptiveMode,
                    onCheckedChange = { onSignalChange(signal.copy(adaptiveMode = it)) }
                )
            }
            SignalSlider("专注", signal.focus) { onSignalChange(signal.copy(focus = it)) }
            SignalSlider("精力", signal.energy) { onSignalChange(signal.copy(energy = it)) }
            SignalSlider("压力", signal.stress) { onSignalChange(signal.copy(stress = it)) }
            EnumChips(
                title = "情绪",
                values = EmotionalTone.entries,
                selected = signal.mood,
                label = { it.label },
                onSelect = { onSignalChange(signal.copy(mood = it)) }
            )
            EnumChips(
                title = "环境",
                values = EnvironmentContext.entries,
                selected = signal.environment,
                label = { it.label },
                onSelect = { onSignalChange(signal.copy(environment = it)) }
            )
        }
    }
}

@Composable
private fun SchedulePanel(schedule: List<ScheduleSlot>) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("智能排程", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (schedule.isEmpty()) {
                Text("当前没有待排程任务。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                schedule.forEach { slot ->
                    ScheduleRow(slot)
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(slot: ScheduleSlot) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = slot.task.category.tint().copy(alpha = 0.14f)
        ) {
            Text(
                text = "${slot.start}\n${slot.end}",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = slot.task.category.tint(),
                fontWeight = FontWeight.SemiBold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(slot.task.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(slot.rationale, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(
                progress = { slot.expectedLoad },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = slot.task.category.tint(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun MinimalFocusPanel(tasks: List<SmartTask>, nextTask: SmartTask?) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("极简焦点", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = nextTask?.title ?: "当前没有待办任务",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "已隐藏低优先级细节。待办池还有 ${tasks.count { !it.isCompleted }} 项，建议只推进一个最小动作。",
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun TaskListScreen(
    tasks: List<SmartTask>,
    signal: UserCognitiveSignal,
    onToggleTask: (SmartTask) -> Unit,
    onDeleteTask: (SmartTask) -> Unit
) {
    var selectedCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var showCompleted by rememberSaveable { mutableStateOf(true) }
    val selectedCategory = selectedCategoryName?.let { name ->
        TaskCategory.entries.firstOrNull { it.name == name }
    }
    val filtered = remember(tasks, selectedCategory, showCompleted) {
        tasks
            .filter { selectedCategory == null || it.category == selectedCategory }
            .filter { showCompleted || !it.isCompleted }
            .sortedWith(
                compareByDescending<SmartTask> { !it.isCompleted }
                    .thenByDescending { SmartTaskEngine.calculatePriorityScore(it, signal) }
            )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("任务池", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("显示已完成", style = MaterialTheme.typography.labelMedium)
                    Switch(checked = showCompleted, onCheckedChange = { showCompleted = it })
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategoryName = null },
                        label = { Text("全部") }
                    )
                }
                items(TaskCategory.entries, key = { it.name }) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategoryName = category.name },
                        label = { Text(category.label) }
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "没有匹配任务。",
                        modifier = Modifier.padding(top = 24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(filtered, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    signal = signal,
                    onToggleTask = { onToggleTask(task) },
                    onDeleteTask = { onDeleteTask(task) }
                )
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: SmartTask,
    signal: UserCognitiveSignal,
    onToggleTask: () -> Unit,
    onDeleteTask: () -> Unit
) {
    val priority = SmartTaskEngine.calculatePriorityScore(task, signal)
    val prediction = SmartTaskEngine.predictTime(task, signal)
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .heightIn(min = 120.dp)
                    .background(task.category.tint())
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(checked = task.isCompleted, onCheckedChange = { onToggleTask() })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (task.description.isNotBlank()) {
                            Text(
                                task.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    StatusBadge(
                        text = if (task.isCompleted) "完成" else priority.roundToInt().toString(),
                        color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(onClick = {}, label = { Text(task.category.label) })
                    AssistChip(onClick = {}, label = { Text("${prediction.minutes} 分") })
                    AssistChip(onClick = {}, label = { Text("±${prediction.uncertaintyMinutes}") })
                    AssistChip(onClick = {}, label = { Text(task.targetHour.hourLabel()) })
                    if (task.isHabit) AssistChip(onClick = {}, label = { Text("连续 ${task.streak} 天") })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDeleteTask) {
                        Text("删除")
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightsScreen(
    tasks: List<SmartTask>,
    signal: UserCognitiveSignal,
    snapshot: CognitiveSnapshot,
    insights: List<ProductivityInsight>,
    nextTask: SmartTask?
) {
    val habitPlan = remember(nextTask, signal) {
        nextTask?.let { SmartTaskEngine.designHabitPlan(it, signal) }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ModelSummaryPanel(tasks = tasks, signal = signal, snapshot = snapshot)
        habitPlan?.let { HabitPlanPanel(plan = it) }
        Text("行为洞察", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        insights.forEach { insight ->
            InsightCard(insight)
        }
        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
private fun ModelSummaryPanel(tasks: List<SmartTask>, signal: UserCognitiveSignal, snapshot: CognitiveSnapshot) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("端侧智能状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                MetricPill("任务", tasks.size / 12f)
                MetricPill("完成", if (tasks.isEmpty()) 0f else tasks.count { it.isCompleted }.toFloat() / tasks.size)
                MetricPill("负荷", snapshot.overall)
                MetricPill("压力", signal.stress)
            }
            Text(
                "当前版本采用可解释启发式模型完成分类、排程、时间估算和界面自适应，所有数据保存在本机。",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HabitPlanPanel(plan: HabitPlan) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("微习惯方案", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            InsightLine("提示", plan.anchor)
            InsightLine("动作", plan.tinyAction)
            InsightLine("强化", plan.reinforcement)
            InsightLine("进阶", plan.nextStep)
        }
    }
}

@Composable
private fun InsightCard(insight: ProductivityInsight) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = insight.severity.tint().copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(insight.severity.tint())
                    .padding(top = 4.dp)
            )
            Column {
                Text(insight.title, fontWeight = FontWeight.SemiBold)
                Text(insight.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TaskEditorDialog(
    signal: UserCognitiveSignal,
    onDismiss: () -> Unit,
    onCreate: (SmartTask) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf(TaskCategory.WORK.name) }
    var importance by rememberSaveable { mutableStateOf(3f) }
    var complexity by rememberSaveable { mutableStateOf(3f) }
    var targetHour by rememberSaveable { mutableStateOf(10f) }
    var isHabit by rememberSaveable { mutableStateOf(false) }
    val category = TaskCategory.entries.first { it.name == categoryName }
    val estimate = remember(title, note, category, complexity, signal) {
        SmartTaskEngine.estimateMinutes(title, note, category, complexity.roundToInt(), signal)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建智能任务") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("任务") },
                    maxLines = 2
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("说明") },
                    maxLines = 3
                )
                Text("分类", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskCategory.entries.forEach { item ->
                        FilterChip(
                            selected = item == category,
                            onClick = { categoryName = item.name },
                            label = { Text(item.label) }
                        )
                    }
                }
                DialogSlider("重要度", importance, " ${importance.roundToInt()}/5") { importance = it }
                DialogSlider("复杂度", complexity, " ${complexity.roundToInt()}/5") { complexity = it }
                DialogSlider("目标时间", targetHour, " ${targetHour.roundToInt().hourLabel()}") { targetHour = it }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("作为微习惯追踪")
                    Switch(checked = isHabit, onCheckedChange = { isHabit = it })
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "预计 $estimate 分钟 · ${category.label} · ${if (isHabit) "习惯" else "任务"}",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    onCreate(
                        SmartTask(
                            title = title.trim(),
                            description = note.trim(),
                            category = category,
                            estimatedMinutes = estimate,
                            importance = importance.roundToInt().coerceIn(1, 5),
                            complexity = complexity.roundToInt().coerceIn(1, 5),
                            targetHour = targetHour.roundToInt().coerceIn(0, 23),
                            isHabit = isHabit
                        )
                    )
                }
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun SmartBottomBar(selectedTab: AppTab, onSelect: (AppTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        AppTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onSelect(tab) },
                icon = {
                    Surface(
                        shape = CircleShape,
                        color = if (selectedTab == tab) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ) {
                        Text(
                            text = tab.mark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                label = { Text(tab.label) }
            )
        }
    }
}

@Composable
private fun MetricPill(label: String, value: Float) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 76.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(percent(value.coerceIn(0f, 1f)), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SignalSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(percent(value), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = { onChange(it.coerceIn(0f, 1f)) },
            valueRange = 0f..1f
        )
    }
}

@Composable
private fun DialogSlider(label: String, value: Float, suffix: String, onChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text(suffix, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = if (label == "目标时间") 0f..23f else 1f..5f,
            steps = if (label == "目标时间") 22 else 3
        )
    }
}

@Composable
private fun <T> EnumChips(
    title: String,
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            values.forEach { value ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(label(value)) }
                )
            }
        }
    }
}

@Composable
private fun InsightLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f))
}

private enum class AppTab(val label: String, val mark: String) {
    TODAY("今日", "今"),
    TASKS("任务", "待"),
    INSIGHTS("洞察", "智"),
    STATISTICS("统计", "统")
}

private fun List<SmartTask>.toggleCompletion(task: SmartTask): List<SmartTask> {
    return map {
        if (it.id != task.id) {
            it
        } else if (it.isCompleted) {
            it.copy(completedAt = null, streak = if (it.isHabit) max(0, it.streak - 1) else it.streak)
        } else {
            it.copy(
                completedAt = System.currentTimeMillis(),
                streak = if (it.isHabit) it.streak + 1 else it.streak
            )
        }
    }
}

private fun percent(value: Float): String = "${(value.coerceIn(0f, 1f) * 100).roundToInt()}%"

private fun Int.hourLabel(): String = "%02d:00".format(coerceIn(0, 23))

private fun TaskCategory.tint(): Color = when (this) {
    TaskCategory.WORK -> Color(0xFF2F6F63)
    TaskCategory.STUDY -> Color(0xFF4D65A8)
    TaskCategory.HEALTH -> Color(0xFF6C8F2E)
    TaskCategory.LIFE -> Color(0xFF9A6A20)
    TaskCategory.SOCIAL -> Color(0xFFB45347)
    TaskCategory.CREATIVE -> Color(0xFF7D5594)
}

private fun CognitiveLoadLevel.tint(): Color = when (this) {
    CognitiveLoadLevel.LOW -> Color(0xFF2F7D57)
    CognitiveLoadLevel.BALANCED -> Color(0xFF2E6EA6)
    CognitiveLoadLevel.HIGH -> Color(0xFFC06A24)
    CognitiveLoadLevel.OVERWHELMING -> Color(0xFFB13E4B)
}

private fun InsightSeverity.tint(): Color = when (this) {
    InsightSeverity.GOOD -> Color(0xFF2F7D57)
    InsightSeverity.NOTICE -> Color(0xFF2E6EA6)
    InsightSeverity.WARNING -> Color(0xFFB13E4B)
}
