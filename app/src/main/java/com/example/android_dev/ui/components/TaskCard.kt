@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.Subtask
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.engine.SmartTaskEngine

// 任务卡片功能：展示标题、优先级（高/中/低）、截止日期、标签、子任务进度与操作按钮。
@Composable
fun TaskCard(
    task: SmartTask,
    signal: UserCognitiveSignal,
    onToggleTask: () -> Unit,
    onDeleteTask: () -> Unit,
    onEditTask: (() -> Unit)? = null,
    onUpdateTask: ((SmartTask) -> Unit)? = null
) {
    val prediction = SmartTaskEngine.predictTime(task, signal)
    // 展开态：点卡片主体在下方展开子任务清单。
    var expanded by remember { mutableStateOf(false) }
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
                    .background(task.priority.tint())
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
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { expanded = !expanded }
                    ) {
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
                        text = if (task.isCompleted) "完成" else task.priority.label,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.outline else task.priority.tint()
                    )
                }

                // 子任务进度条（点击可展开/收起子任务清单）。
                if (task.subtasks.isNotEmpty()) {
                    Column(
                        modifier = Modifier.clickable { expanded = !expanded },
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "子任务 ${task.subtasks.count { it.done }}/${task.subtasks.size} ${if (expanded) "▲ 收起" else "▼ 展开"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(
                            progress = { task.subtaskProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = task.priority.tint(),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(onClick = {}, label = { Text(task.category.label) })
                    AssistChip(onClick = {}, label = { Text("状态 ${task.status.label}") })
                    task.dueDate?.let {
                        AssistChip(
                            onClick = {},
                            label = { Text(if (task.isOverdue) "逾期 $it" else "截止 $it") }
                        )
                    }
                    AssistChip(onClick = {}, label = { Text("${prediction.minutes} 分") })
                    task.tags.forEach { tag -> AssistChip(onClick = {}, label = { Text("#$tag") }) }
                    if (task.isHabit) AssistChip(onClick = {}, label = { Text("连续 ${task.streak} 天") })
                }

                // 子任务展开区：点击卡片后在下方展开，可勾选/编辑/删除/新增子任务。
                if (onUpdateTask != null) {
                    AnimatedVisibility(visible = expanded) {
                        SubtaskList(
                            subtasks = task.subtasks,
                            onSubtasksChange = { updated ->
                                onUpdateTask(task.copy(subtasks = updated))
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 创建时间：显示在底部操作行左侧。
                    Text(
                        text = "创建于 ${formatCreatedAt(task.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        onEditTask?.let {
                            TextButton(onClick = it) { Text("编辑") }
                        }
                        TextButton(onClick = onDeleteTask) { Text("删除") }
                    }
                }
            }
        }
    }
}

// 创建时间格式化功能：把毫秒时间戳转成本地「yyyy-MM-dd HH:mm」文本。
private fun formatCreatedAt(epochMillis: Long): String {
    val dt = java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
    return "%04d-%02d-%02d %02d:%02d".format(
        dt.year, dt.monthValue, dt.dayOfMonth, dt.hour, dt.minute
    )
}
