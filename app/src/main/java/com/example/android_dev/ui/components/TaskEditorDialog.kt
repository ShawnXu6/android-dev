@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.AiBreakdownResult
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.Subtask
import com.example.android_dev.domain.TaskCategory
import com.example.android_dev.domain.TaskPriority
import com.example.android_dev.domain.TaskStatus
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.engine.SmartTaskEngine
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt

// 任务编辑弹窗功能：新建或编辑任务，支持截止日期、高/中/低优先级、自定义标签、提醒、看板状态，并可一键 AI 智能拆解。
@Composable
fun TaskEditorDialog(
    signal: UserCognitiveSignal,
    onDismiss: () -> Unit,
    onCreate: (SmartTask) -> Unit,
    initialTask: SmartTask? = null,
    onRequestAiBreakdown: ((goal: String, dueDate: LocalDate?, onResult: (AiBreakdownResult) -> Unit, onError: () -> Unit) -> Unit)? = null
) {
    val editing = initialTask != null
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var note by remember { mutableStateOf(initialTask?.description ?: "") }
    var categoryName by remember { mutableStateOf((initialTask?.category ?: TaskCategory.WORK).name) }
    var priority by remember { mutableStateOf(initialTask?.priority ?: TaskPriority.MEDIUM) }
    var status by remember { mutableStateOf(initialTask?.status ?: TaskStatus.TODO) }
    var complexity by remember { mutableStateOf((initialTask?.complexity ?: 3).toFloat()) }
    var isHabit by remember { mutableStateOf(initialTask?.isHabit ?: false) }
    var dueDate by remember { mutableStateOf(initialTask?.dueDate) }
    var reminderAt by remember { mutableStateOf(initialTask?.reminderAt) }
    var tags by remember { mutableStateOf(initialTask?.tags ?: emptyList()) }
    var tagInput by remember { mutableStateOf("") }
    var subtasks by remember { mutableStateOf(initialTask?.subtasks ?: emptyList()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var aiLoading by remember { mutableStateOf(false) }
    var aiMessage by remember { mutableStateOf<String?>(null) }

    val category = TaskCategory.entries.first { it.name == categoryName }
    val importance = priority.toImportance()
    val estimate = remember(title, note, category, complexity, signal) {
        SmartTaskEngine.estimateMinutes(title, note, category, complexity.roundToInt(), signal)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing) "编辑任务" else "新建任务") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("任务标题") },
                    placeholder = { Text("例如：下周五前完成期末论文") },
                    maxLines = 2
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("说明（可选）") },
                    maxLines = 3
                )

                // ===== AI 智能拆解 =====
                if (onRequestAiBreakdown != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("✨ AI 智能拆解", fontWeight = FontWeight.SemiBold)
                                if (aiLoading) {
                                    CircularProgressIndicator(modifier = Modifier.padding(end = 4.dp), strokeWidth = 2.dp)
                                } else {
                                    Button(
                                        enabled = title.isNotBlank(),
                                        onClick = {
                                            aiLoading = true
                                            aiMessage = null
                                            onRequestAiBreakdown(
                                                title,
                                                dueDate ?: com.example.android_dev.ai.DueDateParser.parse("$title $note"),
                                                { result ->
                                                    aiLoading = false
                                                    subtasks = result.subtasks
                                                    aiMessage = "${result.source.label}：${result.summary}"
                                                    // 若解析到截止日且当前未设置，自动回填最晚子任务日期为截止日。
                                                    if (dueDate == null) {
                                                        dueDate = result.subtasks.mapNotNull { it.plannedDate }.maxOrNull()
                                                    }
                                                },
                                                {
                                                    aiLoading = false
                                                    aiMessage = "拆解失败，请稍后再试。"
                                                }
                                            )
                                        }
                                    ) { Text("拆解") }
                                }
                            }
                            Text(
                                aiMessage ?: "输入标题后，让 AI 把大目标拆成可执行的小步骤；拆解结果可逐条编辑。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    // 子任务清单：AI 拆解后可逐条编辑标题/分钟、删除或手动新增。
                    SubtaskList(
                        subtasks = subtasks,
                        onSubtasksChange = { subtasks = it }
                    )
                }

                // ===== 截止日期 =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("截止日期")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (dueDate != null) {
                            TextButton(onClick = { dueDate = null }) { Text("清除") }
                        }
                        OutlinedButton(onClick = { showDatePicker = true }) {
                            Text(dueDate?.toString() ?: "选择日期")
                        }
                    }
                }

                // ===== 优先级 高/中/低 =====
                Text("优先级", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.label) }
                        )
                    }
                }

                // ===== 看板状态（编辑时可调） =====
                if (editing && !isHabit) {
                    Text("状态", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskStatus.entries.forEach { s ->
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                label = { Text(s.label) }
                            )
                        }
                    }
                }

                // ===== 分类 =====
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

                // ===== 自定义标签 =====
                Text("标签", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("添加标签") },
                        singleLine = true
                    )
                    TextButton(
                        enabled = tagInput.isNotBlank(),
                        onClick = {
                            val t = tagInput.trim()
                            if (t.isNotBlank() && !tags.contains(t)) tags = tags + t
                            tagInput = ""
                        }
                    ) { Text("添加") }
                }
                if (tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tags.forEach { tag ->
                            InputChip(
                                selected = false,
                                onClick = { tags = tags.filterNot { it == tag } },
                                label = { Text("#$tag ✕") }
                            )
                        }
                    }
                }

                DialogSlider("复杂度", complexity, " ${complexity.roundToInt()}/5") { complexity = it }

                // ===== 提醒时间 =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("提醒")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (reminderAt != null) {
                            TextButton(onClick = { reminderAt = null }) { Text("清除") }
                        }
                        // 快捷提醒：基于截止日 9:00，或今天 1 小时后。
                        OutlinedButton(onClick = {
                            reminderAt = defaultReminderMillis(dueDate)
                        }) {
                            Text(reminderAt?.let { formatReminder(it) } ?: "设置提醒")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("作为微习惯追踪")
                    Switch(checked = isHabit, onCheckedChange = { isHabit = it })
                }

                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        text = "预计 $estimate 分钟 · ${category.label} · 优先级${priority.label}",
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
                    val base = initialTask ?: SmartTask(title = title.trim())
                    onCreate(
                        base.copy(
                            title = title.trim(),
                            description = note.trim(),
                            category = category,
                            estimatedMinutes = estimate,
                            importance = importance,
                            complexity = complexity.roundToInt().coerceIn(1, 5),
                            isHabit = isHabit,
                            dueDate = dueDate,
                            priority = priority,
                            status = if (isHabit) TaskStatus.TODO else status,
                            tags = tags,
                            subtasks = subtasks,
                            reminderAt = reminderAt
                        )
                    )
                }
            ) { Text(if (editing) "保存" else "创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate
                ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        dueDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun TaskPriority.toImportance(): Int = when (this) {
    TaskPriority.HIGH -> 5
    TaskPriority.MEDIUM -> 3
    TaskPriority.LOW -> 2
}

// 默认提醒时间功能：有截止日则取当天 9:00，否则取当前时间 1 小时后。
private fun defaultReminderMillis(dueDate: LocalDate?): Long {
    val zone = ZoneId.systemDefault()
    return if (dueDate != null) {
        dueDate.atTime(LocalTime.of(9, 0)).atZone(zone).toInstant().toEpochMilli()
    } else {
        System.currentTimeMillis() + 60 * 60 * 1000L
    }
}

private fun formatReminder(millis: Long): String {
    val dt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    return "%02d-%02d %02d:%02d".format(dt.monthValue, dt.dayOfMonth, dt.hour, dt.minute)
}
