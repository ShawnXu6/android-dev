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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.android_dev.domain.TaskCategory
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.engine.SmartTaskEngine
import com.example.android_dev.ui.components.TaskCard
import com.example.android_dev.ui.components.TaskEditorDialog

// 任务列表功能：展示、搜索、筛选、编辑、完成和删除所有智能任务。
@Composable
fun TaskScreen(
    tasks: List<SmartTask>,
    signal: UserCognitiveSignal,
    onToggleTask: (SmartTask) -> Unit,
    onDeleteTask: (SmartTask) -> Unit,
    onUpdateTask: (SmartTask) -> Unit
) {
    var selectedCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var showCompleted by rememberSaveable { mutableStateOf(true) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingTask by rememberSaveable { mutableStateOf<SmartTask?>(null) }
    
    val selectedCategory = selectedCategoryName?.let { name ->
        TaskCategory.entries.firstOrNull { it.name == name }
    }
    
    val filtered = remember(tasks, selectedCategory, showCompleted, signal, searchQuery) {
        tasks
            .filter { selectedCategory == null || it.category == selectedCategory }
            .filter { showCompleted || !it.isCompleted }
            .filter { 
                searchQuery.isEmpty() || 
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<SmartTask> { !it.isCompleted }
                    .thenByDescending { SmartTaskEngine.explainPriorityScore(it, signal).totalScore }
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
                Text("任务池", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("显示已完成", style = MaterialTheme.typography.labelMedium)
                    Switch(checked = showCompleted, onCheckedChange = { showCompleted = it })
                }
            }
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索任务") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "搜索") },
                placeholder = { Text("输入关键词搜索") }
            )
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategoryName = null },
                        label = { Text("全部") }
                    )
                }
                items(TaskCategory.entries, key = { it.name }) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategoryName = category.name },
                        label = { Text(category.label) }
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
                        "没有匹配任务。",
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
                    onEditTask = { 
                        editingTask = it
                        showEditor = true
                    }
                )
            }
        }
    }
    
    if (showEditor) {
        TaskEditorDialog(
            signal = signal,
            onDismiss = { 
                showEditor = false
                editingTask = null
            },
            onCreate = {},
            onUpdate = onUpdateTask,
            task = editingTask
        )
    }
}

