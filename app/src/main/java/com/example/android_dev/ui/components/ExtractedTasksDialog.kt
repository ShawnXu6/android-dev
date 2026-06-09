@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.ExtractedTask

// 提取任务预览弹窗功能：展示 AI 从对话里提取的任务，用户可勾选取舍后批量加入计划。
@Composable
fun ExtractedTasksDialog(
    initialTasks: List<ExtractedTask>,
    onConfirm: (List<ExtractedTask>) -> Unit,
    onDismiss: () -> Unit
) {
    var tasks by remember { mutableStateOf(initialTasks) }
    val selectedCount = tasks.count { it.selected }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入计划（已识别 ${tasks.size} 项）") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "勾选要加入的任务，可逐项取舍：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                tasks.forEachIndexed { index, task ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.selected,
                            onCheckedChange = { checked ->
                                tasks = tasks.toMutableList().also { it[index] = task.copy(selected = checked) }
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                buildString {
                                    append("优先级${task.priority.label} · ${task.estimatedMinutes} 分")
                                    task.plannedDate?.let { append(" · $it") }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedCount > 0,
                onClick = { onConfirm(tasks.filter { it.selected }) }
            ) { Text("加入 $selectedCount 项") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
