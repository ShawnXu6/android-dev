@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.engine.SmartTaskEngine
import kotlin.math.roundToInt

// 任务卡片功能：展示单个任务的优先级、预测耗时、目标时间和操作按钮（完成、编辑、删除）。
@Composable
fun TaskCard(
    task: SmartTask,
    signal: UserCognitiveSignal,
    onToggleTask: () -> Unit,
    onDeleteTask: () -> Unit,
    onEditTask: (SmartTask) -> Unit = {}
) {
    val priority = SmartTaskEngine.explainPriorityScore(task, signal)
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
                        text = if (task.isCompleted) "完成" else priority.totalScore.roundToInt().toString(),
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
                    TextButton(onClick = { onEditTask(task) }) {
                        Text("编辑")
                    }
                    TextButton(onClick = onDeleteTask) {
                        Text("删除")
                    }
                }
            }
        }
    }
}
