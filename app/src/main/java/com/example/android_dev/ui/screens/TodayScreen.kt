@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.CognitiveLoadLevel
import com.example.android_dev.domain.CognitiveSnapshot
import com.example.android_dev.domain.ScheduleSlot
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskRecommendation
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.engine.SmartTaskEngine
import com.example.android_dev.ui.components.CognitiveControls
import com.example.android_dev.ui.components.CognitiveStatusPanel
import com.example.android_dev.ui.components.MinimalFocusPanel
import com.example.android_dev.ui.components.SchedulePanel
import com.example.android_dev.ui.components.StatusBadge
import com.example.android_dev.ui.components.percent
import com.example.android_dev.ui.components.tint
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// 今日页功能：以「列表为中心」的首页布局（对标 Todoist / TickTick / Things）——
// 顶部进度头 + 当前最该做的一件 → 快速捕捉 → 按「逾期/今天/稍后」分组的任务清单 → 可折叠的「状态与排程」抽屉。
@Composable
fun TodayScreen(
    tasks: List<SmartTask>,
    signal: UserCognitiveSignal,
    snapshot: CognitiveSnapshot,
    schedule: List<ScheduleSlot>,
    nextTask: SmartTask?,
    nextRecommendation: TaskRecommendation?,
    onSignalChange: (UserCognitiveSignal) -> Unit,
    onQuickAddTask: (String, String) -> Unit,
    onToggleTask: (SmartTask) -> Unit
) {
    val today = remember { LocalDate.now() }
    val activeTasks = tasks.filterNot { it.isCompleted }
    val completedCount = tasks.size - activeTasks.size

    // 三组分桶：逾期（截止日早于今天）、今天（截止今天或无截止日）、稍后（截止日在今天之后）。
    val overdue = activeTasks.filter { it.dueDate != null && it.dueDate.isBefore(today) }
    val todayTasks = activeTasks.filter { it.dueDate == null || it.dueDate.isEqual(today) }
    val later = activeTasks.filter { it.dueDate != null && it.dueDate.isAfter(today) }

    // 自适应触发：开启自适应模式后，负荷高/压力大/任务多时把「状态与排程」抽屉默认换成极简专注视图。
    val shouldSimplify = signal.adaptiveMode && (
        snapshot.level >= CognitiveLoadLevel.HIGH ||
            signal.stress > 0.7f ||
            activeTasks.size >= 7
        )
    var forceFullSchedule by rememberSaveable { mutableStateOf(false) }
    val simplified = shouldSimplify && !forceFullSchedule

    // 折叠态记忆：各分组与底部抽屉的展开状态。
    var overdueOpen by rememberSaveable { mutableStateOf(true) }
    var todayOpen by rememberSaveable { mutableStateOf(true) }
    var laterOpen by rememberSaveable { mutableStateOf(false) }
    var advancedOpen by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 顶部进度头：日期 + 完成进度环 + 当前最该做的一件。
        item(key = "header") {
            TodayHeaderCard(
                today = today,
                total = tasks.size,
                completed = completedCount,
                snapshot = snapshot,
                nextTask = nextTask,
                nextRecommendation = nextRecommendation,
                signal = signal,
                onToggleNext = { nextTask?.let(onToggleTask) }
            )
        }

        // 快速捕捉：单行输入 + 加号；输入后展开备注。
        item(key = "quick") {
            QuickCapturePanel(onQuickAddTask = onQuickAddTask)
        }

        // 逾期分组。
        if (overdue.isNotEmpty()) {
            sectionHeader(
                key = "h-overdue",
                title = "逾期",
                count = overdue.size,
                open = overdueOpen,
                tint = { MaterialTheme.colorScheme.error },
                onToggle = { overdueOpen = !overdueOpen }
            )
            if (overdueOpen) {
                items(overdue, key = { "o-${it.id}" }) { task ->
                    TodayTaskRow(task = task, signal = signal, today = today, onToggle = { onToggleTask(task) })
                }
            }
        }

        // 今天分组。
        sectionHeader(
            key = "h-today",
            title = "今天",
            count = todayTasks.size,
            open = todayOpen,
            tint = { MaterialTheme.colorScheme.primary },
            onToggle = { todayOpen = !todayOpen }
        )
        if (todayOpen) {
            if (todayTasks.isEmpty()) {
                item(key = "today-empty") { EmptyHint("今天没有待办，享受片刻轻松 🎉") }
            } else {
                items(todayTasks, key = { "t-${it.id}" }) { task ->
                    TodayTaskRow(task = task, signal = signal, today = today, onToggle = { onToggleTask(task) })
                }
            }
        }

        // 稍后分组。
        if (later.isNotEmpty()) {
            sectionHeader(
                key = "h-later",
                title = "稍后",
                count = later.size,
                open = laterOpen,
                tint = { MaterialTheme.colorScheme.onSurfaceVariant },
                onToggle = { laterOpen = !laterOpen }
            )
            if (laterOpen) {
                items(later, key = { "l-${it.id}" }) { task ->
                    TodayTaskRow(task = task, signal = signal, today = today, onToggle = { onToggleTask(task) })
                }
            }
        }

        // 底部「状态与排程」抽屉：默认折叠，保留认知负荷、状态调节、智能排程/专注视图等全部特色功能。
        item(key = "advanced") {
            AdvancedDrawer(
                open = advancedOpen,
                onToggle = { advancedOpen = !advancedOpen },
                levelLabel = snapshot.level.label,
                overall = snapshot.overall,
                tint = snapshot.level.tint()
            ) {
                CognitiveStatusPanel(snapshot = snapshot, signal = signal)
                CognitiveControls(signal = signal, onSignalChange = onSignalChange)
                if (simplified) {
                    MinimalFocusPanel(
                        tasks = tasks,
                        signal = signal,
                        onToggleTask = onToggleTask,
                        onShowFullSchedule = { forceFullSchedule = true }
                    )
                } else {
                    if (shouldSimplify && forceFullSchedule) {
                        TextButton(
                            onClick = { forceFullSchedule = false },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("收起，回到专注模式") }
                    }
                    SchedulePanel(schedule = schedule)
                }
            }
        }
    }
}

// 顶部进度头功能：左侧问候+日期+「现在做」，右侧完成进度环。
@Composable
private fun TodayHeaderCard(
    today: LocalDate,
    total: Int,
    completed: Int,
    snapshot: CognitiveSnapshot,
    nextTask: SmartTask?,
    nextRecommendation: TaskRecommendation?,
    signal: UserCognitiveSignal,
    onToggleNext: () -> Unit
) {
    val ratio = if (total == 0) 0f else completed.toFloat() / total
    val weekday = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA)
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${today.monthValue} 月 ${today.dayOfMonth} 日 · $weekday",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (total == 0) "今天还没有任务" else "已完成 $completed / $total",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = snapshot.recommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // 完成进度环。
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                    CircularProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 6.dp,
                        color = snapshot.level.tint(),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(percent(ratio), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }

            // 「现在做」：把下一步推荐压缩成一行高亮条，点左侧圆圈即完成。
            nextTask?.let { task ->
                val prediction = remember(task, signal) { SmartTaskEngine.predictTime(task, signal) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                            .clickable { onToggleNext() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "完成",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "现在做",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            task.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    StatusBadge("${prediction.minutes} 分", MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// 分组标题功能：可点击折叠的小标题，左侧色点 + 标题 + 数量徽章 + 展开箭头。
private fun androidx.compose.foundation.lazy.LazyListScope.sectionHeader(
    key: String,
    title: String,
    count: Int,
    open: Boolean,
    tint: @Composable () -> androidx.compose.ui.graphics.Color,
    onToggle: () -> Unit
) {
    item(key = key) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggle() }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(tint())
            )
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "$count",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (open) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 任务行功能：轻量列表行——左侧优先级色条 + 勾选框 + 标题 + 分类/截止/耗时元信息。
@Composable
private fun TodayTaskRow(
    task: SmartTask,
    signal: UserCognitiveSignal,
    today: LocalDate,
    onToggle: () -> Unit
) {
    val prediction = remember(task, signal) { SmartTaskEngine.predictTime(task, signal) }
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .background(task.priority.tint())
            )
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = taskMeta(task, today, prediction.minutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (task.isOverdue) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusBadge(text = task.priority.label, color = task.priority.tint())
            Spacer(modifier = Modifier.width(10.dp))
        }
    }
}

// 元信息拼接功能：分类 · 约X分 · 截止/逾期 · 子任务进度。
private fun taskMeta(task: SmartTask, today: LocalDate, minutes: Int): String = buildString {
    append(task.category.label)
    append(" · 约 $minutes 分")
    task.dueDate?.let {
        val label = when {
            it.isBefore(today) -> "逾期 $it"
            it.isEqual(today) -> "今天截止"
            else -> "截止 $it"
        }
        append(" · $label")
    }
    if (task.subtasks.isNotEmpty()) {
        append(" · 子任务 ${task.subtasks.count { it.done }}/${task.subtasks.size}")
    }
}

// 空态提示功能：分组为空时给一句轻提示。
@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
    )
}

// 高级抽屉功能：折叠收纳认知负荷、状态调节、智能排程/专注视图等特色功能，默认折叠不打扰清单。
@Composable
private fun AdvancedDrawer(
    open: Boolean,
    onToggle: () -> Unit,
    levelLabel: String,
    overall: Float,
    tint: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("状态与排程", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "认知负荷 · 状态调节 · 智能排程",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge("$levelLabel ${percent(overall)}", tint)
                Icon(
                    imageVector = if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (open) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = open) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    content()
                }
            }
        }
    }
}

// 快速捕捉功能：收集标题和备注，由 ViewModel 创建智能任务。
// 紧凑设计：默认只显示单行输入 + 加号按钮；一旦输入内容才展开备注框与说明，平时不占额外高度。
@Composable
private fun QuickCapturePanel(onQuickAddTask: (String, String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val expanded = text.isNotBlank()
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("快速捕捉") },
                    placeholder = { Text("记一件待办…") },
                    singleLine = true
                )
                FilledIconButton(
                    enabled = text.isNotBlank(),
                    onClick = {
                        onQuickAddTask(text, note)
                        text = ""
                        note = ""
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "加入")
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("补充信息（可选）") },
                        maxLines = 2
                    )
                    Text(
                        text = "本地语义分类 · 时间估算 · 负荷适配",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
