@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.example.android_dev.domain.TaskPriority
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.ui.components.TaskCard

// 列表排序方式功能：决定每个完成分组内部的排序规则。
private enum class TaskSortMode(val label: String) {
    CREATED("按创建时间"),
    PRIORITY("按优先级")
}

// 列表视图功能：展示、按优先级筛选、排序、完成、编辑和删除任务。
@Composable
fun TaskScreen(
    tasks: List<SmartTask>,
    signal: UserCognitiveSignal,
    onToggleTask: (SmartTask) -> Unit,
    onDeleteTask: (SmartTask) -> Unit,
    onEditTask: (SmartTask) -> Unit,
    onUpdateTask: (SmartTask) -> Unit
) {
    var selectedPriorityName by rememberSaveable { mutableStateOf<String?>(null) }
    var sortModeName by rememberSaveable { mutableStateOf(TaskSortMode.CREATED.name) }
    var showCompleted by rememberSaveable { mutableStateOf(true) }
    val selectedPriority = selectedPriorityName?.let { name ->
        TaskPriority.entries.firstOrNull { it.name == name }
    }
    val sortMode = TaskSortMode.entries.firstOrNull { it.name == sortModeName } ?: TaskSortMode.CREATED

    val filtered = remember(tasks, selectedPriority, showCompleted, sortMode) {
        // 组内按创建时间倒序（新的在上）的比较器。
        val byCreatedDesc = compareByDescending<SmartTask> { it.createdAt }
        // 组内排序：优先级模式先按优先级高→低，同级再按创建时间；创建时间模式直接按创建时间。
        val withinGroup = when (sortMode) {
            TaskSortMode.PRIORITY ->
                compareByDescending<SmartTask> { it.priority.weight }.then(byCreatedDesc)
            TaskSortMode.CREATED -> byCreatedDesc
        }

        tasks
            .filter { selectedPriority == null || it.priority == selectedPriority }
            .filter { showCompleted || !it.isCompleted }
            .sortedWith(
                // 最外层永远是「未完成在上、已完成在下」，再按组内规则排序。
                compareByDescending<SmartTask> { !it.isCompleted }.then(withinGroup)
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
                Text("我的任务", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("显示已完成", style = MaterialTheme.typography.labelMedium)
                    Switch(checked = showCompleted, onCheckedChange = { showCompleted = it })
                }
            }
            // 优先级筛选行。
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedPriority == null,
                        onClick = { selectedPriorityName = null },
                        label = { Text("全部") }
                    )
                }
                items(TaskPriority.entries, key = { it.name }) { priority ->
                    FilterChip(
                        selected = selectedPriority == priority,
                        onClick = { selectedPriorityName = priority.name },
                        label = { Text("优先级 ${priority.label}") }
                    )
                }
            }
            // 排序方式行。
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "排序",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TaskSortMode.entries.forEach { mode ->
                    FilterChip(
                        selected = sortMode == mode,
                        onClick = { sortModeName = mode.name },
                        label = { Text(mode.label) }
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
                        "还没有任务，点右下角 + 新建一个吧。",
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
                    onDeleteTask = { onDeleteTask(task) },
                    onEditTask = { onEditTask(task) },
                    onUpdateTask = onUpdateTask
                )
            }
        }
    }
}
