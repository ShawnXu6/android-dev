@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.engine.SmartTaskEngine

// 极简焦点功能：忙碌/高压时只聚焦最重要的几件未完成任务，可直接勾选完成，并能展开完整排程。
@Composable
fun MinimalFocusPanel(
    tasks: List<SmartTask>,
    signal: UserCognitiveSignal,
    onToggleTask: (SmartTask) -> Unit,
    onShowFullSchedule: () -> Unit
) {
    val activeTasks = tasks.filterNot { it.isCompleted }
    // 取优先级评分最高的前 3 件作为「现在专注做这些」。
    val focusTasks = remember(tasks, signal) {
        activeTasks
            .sortedByDescending { SmartTaskEngine.explainPriorityScore(it, signal).totalScore }
            .take(3)
    }

    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("专注模式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "任务较多，先集中做这几件最重要的。",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                StatusBadge(text = "待办 ${activeTasks.size}", color = MaterialTheme.colorScheme.primary)
            }

            if (focusTasks.isEmpty()) {
                Text(
                    "🎉 全部完成，今天可以休息一下了。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            } else {
                focusTasks.forEachIndexed { index, task ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f))
                    }
                    FocusTaskRow(
                        order = index + 1,
                        task = task,
                        signal = signal,
                        onToggle = { onToggleTask(task) }
                    )
                }
                if (activeTasks.size > focusTasks.size) {
                    Text(
                        "还有 ${activeTasks.size - focusTasks.size} 项已折叠，做完这几件再看。",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            TextButton(onClick = onShowFullSchedule, modifier = Modifier.fillMaxWidth()) {
                Text("展开完整排程")
            }
        }
    }
}

// 焦点任务行功能：序号 + 勾选框 + 标题 + 优先级/截止/预计耗时的关键信息。
@Composable
private fun FocusTaskRow(
    order: Int,
    task: SmartTask,
    signal: UserCognitiveSignal,
    onToggle: () -> Unit
) {
    val prediction = remember(task, signal) { SmartTaskEngine.predictTime(task, signal) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$order",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(
                task.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append("优先级${task.priority.label} · 约 ${prediction.minutes} 分")
                    task.dueDate?.let { append(if (task.isOverdue) " · 已逾期 $it" else " · 截止 $it") }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}
