@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskStatus
import com.example.android_dev.ui.components.StatusBadge
import com.example.android_dev.ui.components.tint

// 看板视图功能：三列（待处理 / 进行中 / 已完成）展示任务，可在列间左右流转。
@Composable
fun KanbanScreen(
    tasks: List<SmartTask>,
    onMoveStatus: (SmartTask, TaskStatus) -> Unit,
    onEditTask: (SmartTask) -> Unit,
    onDeleteTask: (SmartTask) -> Unit
) {
    // 习惯任务不进看板（它们按日打卡，状态语义不同）。
    val boardTasks = tasks.filterNot { it.isHabit }
    val columns = listOf(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE)

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "拖动按钮把任务在三列之间流转",
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(columns, key = { it.name }) { status ->
                KanbanColumn(
                    status = status,
                    tasks = boardTasks.filter { taskStatusOf(it) == status },
                    onMoveStatus = onMoveStatus,
                    onEditTask = onEditTask,
                    onDeleteTask = onDeleteTask
                )
            }
        }
    }
}

// 状态归一功能：把「completedAt 已设置但 status 仍为旧值」的任务也正确归入已完成列。
private fun taskStatusOf(task: SmartTask): TaskStatus =
    if (task.isCompleted) TaskStatus.DONE else task.status

@Composable
private fun KanbanColumn(
    status: TaskStatus,
    tasks: List<SmartTask>,
    onMoveStatus: (SmartTask, TaskStatus) -> Unit,
    onEditTask: (SmartTask) -> Unit,
    onDeleteTask: (SmartTask) -> Unit
) {
    Surface(
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(status.tint())
                )
                Text(status.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${tasks.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (tasks.isEmpty()) {
                Text(
                    "暂无任务",
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(tasks, key = { it.id }) { task ->
                        KanbanCard(
                            task = task,
                            status = status,
                            onMoveStatus = onMoveStatus,
                            onEditTask = onEditTask,
                            onDeleteTask = onDeleteTask
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KanbanCard(
    task: SmartTask,
    status: TaskStatus,
    onMoveStatus: (SmartTask, TaskStatus) -> Unit,
    onEditTask: (SmartTask) -> Unit,
    onDeleteTask: (SmartTask) -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                task.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(text = "优先级 ${task.priority.label}", color = task.priority.tint())
                task.dueDate?.let { StatusBadge(text = "截止 $it", color = if (task.isOverdue) task.priority.tint() else MaterialTheme.colorScheme.outline) }
            }
            if (task.subtasks.isNotEmpty()) {
                Text(
                    "子任务 ${task.subtasks.count { it.done }}/${task.subtasks.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onMoveStatus(task, status.previous()) },
                    enabled = status != TaskStatus.TODO
                ) { Text("← 回退") }
                TextButton(onClick = { onEditTask(task) }) { Text("编辑") }
                TextButton(
                    onClick = { onMoveStatus(task, status.next()) },
                    enabled = status != TaskStatus.DONE
                ) { Text("推进 →") }
            }
            TextButton(
                onClick = { onDeleteTask(task) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("删除", color = MaterialTheme.colorScheme.error) }
        }
    }
}
