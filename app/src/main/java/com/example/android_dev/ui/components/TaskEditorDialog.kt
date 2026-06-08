@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskCategory
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.engine.SmartTaskEngine
import com.example.android_dev.ui.components.hourLabel
import kotlin.math.roundToInt

// 任务编辑弹窗功能：支持新建和编辑任务，收集任务参数并根据当前用户状态实时估算耗时。
@Composable
fun TaskEditorDialog(
    signal: UserCognitiveSignal,
    onDismiss: () -> Unit,
    onCreate: (SmartTask) -> Unit,
    onUpdate: ((SmartTask) -> Unit)? = null,
    task: SmartTask? = null
) {
    val isEditMode = task != null
    
    var title by rememberSaveable { mutableStateOf(task?.title ?: "") }
    var note by rememberSaveable { mutableStateOf(task?.description ?: "") }
    var categoryName by rememberSaveable { mutableStateOf(task?.category?.name ?: TaskCategory.WORK.name) }
    var importance by rememberSaveable { mutableStateOf((task?.importance ?: 3).toFloat()) }
    var complexity by rememberSaveable { mutableStateOf((task?.complexity ?: 3).toFloat()) }
    var targetHour by rememberSaveable { mutableStateOf((task?.targetHour ?: 10).toFloat()) }
    var isHabit by rememberSaveable { mutableStateOf(task?.isHabit ?: false) }
    var reminderEnabled by rememberSaveable { mutableStateOf(task?.reminderTime != null) }
    var reminderMinutes by rememberSaveable { mutableStateOf((task?.reminderTime ?: 0).toFloat()) }
    
    val category = TaskCategory.entries.first { it.name == categoryName }
    val estimate = remember(title, note, category, complexity, signal) {
        SmartTaskEngine.estimateMinutes(title, note, category, complexity.roundToInt(), signal)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditMode) "编辑任务" else "新建智能任务") },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("设置提醒")
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
                AnimatedVisibility(visible = reminderEnabled) {
                    DialogSlider("提前提醒分钟数", reminderMinutes, " ${reminderMinutes.roundToInt()} 分钟") { reminderMinutes = it }
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
                    val reminder = if (reminderEnabled) reminderMinutes.roundToInt().coerceIn(0, 1440) else null
                    val taskData = SmartTask(
                        id = task?.id ?: "",
                        title = title.trim(),
                        description = note.trim(),
                        category = category,
                        estimatedMinutes = estimate,
                        importance = importance.roundToInt().coerceIn(1, 5),
                        complexity = complexity.roundToInt().coerceIn(1, 5),
                        targetHour = targetHour.roundToInt().coerceIn(0, 23),
                        isHabit = isHabit,
                        createdAt = task?.createdAt ?: System.currentTimeMillis(),
                        completedAt = task?.completedAt,
                        streak = task?.streak ?: 0,
                        habitId = task?.habitId,
                        lastCompletedDate = task?.lastCompletedDate,
                        completionHistory = task?.completionHistory ?: emptyList(),
                        reminderTime = reminder
                    )
                    if (isEditMode && onUpdate != null) {
                        onUpdate(taskData)
                    } else {
                        onCreate(taskData)
                    }
                }
            ) {
                Text(if (isEditMode) "保存" else "创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
