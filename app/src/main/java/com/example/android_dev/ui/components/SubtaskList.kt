@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.Subtask
import kotlin.math.roundToInt

// 子任务展开视图功能：在父卡片下方展开，支持勾选完成、内联编辑标题/分钟、删除和新增子任务。
@Composable
fun SubtaskList(
    subtasks: List<Subtask>,
    onSubtasksChange: (List<Subtask>) -> Unit
) {
    var addingNew by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "子任务",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            if (subtasks.isEmpty() && !addingNew) {
                Text(
                    "还没有子任务，可点下方添加，或在编辑里用 ✨ AI 智能拆解。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            subtasks.forEachIndexed { index, subtask ->
                SubtaskRow(
                    subtask = subtask,
                    onToggle = { checked ->
                        onSubtasksChange(subtasks.replaceAt(index) { it.copy(done = checked) })
                    },
                    onSave = { title, minutes ->
                        onSubtasksChange(
                            subtasks.replaceAt(index) { it.copy(title = title, estimatedMinutes = minutes) }
                        )
                    },
                    onDelete = {
                        onSubtasksChange(subtasks.filterIndexed { i, _ -> i != index })
                    }
                )
            }

            if (addingNew) {
                SubtaskEditor(
                    initialTitle = "",
                    initialMinutes = 20,
                    onConfirm = { title, minutes ->
                        onSubtasksChange(subtasks + Subtask(title = title, estimatedMinutes = minutes))
                        addingNew = false
                    },
                    onCancel = { addingNew = false }
                )
            } else {
                TextButton(onClick = { addingNew = true }) { Text("+ 添加子任务") }
            }
        }
    }
}

// 单个子任务行功能：默认展示态可勾选/编辑/删除，点「编辑」切换为内联编辑态。
@Composable
private fun SubtaskRow(
    subtask: Subtask,
    onToggle: (Boolean) -> Unit,
    onSave: (String, Int) -> Unit,
    onDelete: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }

    if (editing) {
        SubtaskEditor(
            initialTitle = subtask.title,
            initialMinutes = subtask.estimatedMinutes,
            onConfirm = { title, minutes ->
                onSave(title, minutes)
                editing = false
            },
            onCancel = { editing = false }
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = subtask.done, onCheckedChange = onToggle)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subtask.title,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (subtask.done) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (subtask.done) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = buildString {
                        append("${subtask.estimatedMinutes} 分")
                        subtask.plannedDate?.let { append(" · 计划 $it") }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { editing = true }) { Text("编辑") }
            TextButton(onClick = onDelete) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// 子任务内联编辑器功能：编辑标题与预计分钟，确认/取消。
@Composable
private fun SubtaskEditor(
    initialTitle: String,
    initialMinutes: Int,
    onConfirm: (String, Int) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var minutesText by remember { mutableStateOf(initialMinutes.toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("子任务标题") },
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = minutesText,
                onValueChange = { input -> minutesText = input.filter { it.isDigit() }.take(3) },
                modifier = Modifier.width(120.dp),
                label = { Text("预计分钟") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val minutes = minutesText.toIntOrNull()?.coerceIn(5, 240) ?: 20
                    onConfirm(title.trim(), minutes)
                }
            ) { Text("保存") }
            TextButton(onClick = onCancel) { Text("取消") }
        }
    }
}

// 列表替换辅助功能：返回把第 index 项替换为 transform 结果后的新列表。
private inline fun <T> List<T>.replaceAt(index: Int, transform: (T) -> T): List<T> =
    mapIndexed { i, item -> if (i == index) transform(item) else item }
