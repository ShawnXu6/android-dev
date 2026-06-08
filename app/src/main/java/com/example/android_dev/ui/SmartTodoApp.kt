@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.android_dev.ui.components.SmartTodoBottomBar
import com.example.android_dev.ui.components.SmartTodoTab
import com.example.android_dev.ui.components.TaskEditorDialog
import com.example.android_dev.ui.screens.InsightScreen
import com.example.android_dev.ui.screens.StatisticsScreen
import com.example.android_dev.ui.screens.TaskScreen
import com.example.android_dev.ui.screens.TodayScreen
import com.example.android_dev.viewmodel.SmartTodoUiState

// 应用壳层功能：负责顶部栏、底部导航、悬浮按钮和四个功能页面的路由。
@Composable
fun SmartTodoApp(
    uiState: SmartTodoUiState,
    onSignalChange: (com.example.android_dev.domain.UserCognitiveSignal) -> Unit,
    onQuickAddTask: (String, String) -> Unit,
    onCreateTask: (com.example.android_dev.domain.SmartTask) -> Unit,
    onToggleTask: (com.example.android_dev.domain.SmartTask) -> Unit,
    onDeleteTask: (com.example.android_dev.domain.SmartTask) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(SmartTodoTab.TODAY) }
    var showTaskEditor by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("智能TodoLife", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${uiState.snapshot.level.label} · ${uiState.tasks.count { !it.isCompleted }} 项待办",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            SmartTodoBottomBar(selectedTab = selectedTab, onSelect = { selectedTab = it })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showTaskEditor = true }) {
                Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                SmartTodoTab.TODAY -> TodayScreen(
                    tasks = uiState.tasks,
                    signal = uiState.signal,
                    snapshot = uiState.snapshot,
                    schedule = uiState.schedule,
                    nextTask = uiState.nextTask,
                    nextRecommendation = uiState.nextRecommendation,
                    onSignalChange = onSignalChange,
                    onQuickAddTask = onQuickAddTask,
                    onToggleTask = onToggleTask
                )

                SmartTodoTab.TASKS -> TaskScreen(
                    tasks = uiState.tasks,
                    signal = uiState.signal,
                    onToggleTask = onToggleTask,
                    onDeleteTask = onDeleteTask
                )

                SmartTodoTab.INSIGHTS -> InsightScreen(
                    tasks = uiState.tasks,
                    signal = uiState.signal,
                    snapshot = uiState.snapshot,
                    insights = uiState.insights,
                    nextTask = uiState.nextTask
                )

                SmartTodoTab.STATISTICS -> StatisticsScreen(
                    tasks = uiState.tasks,
                    weeklyReport = uiState.weeklyReport,
                    heatmapData = uiState.heatmapData,
                    loadCurve = uiState.loadCurve,
                    predictedLoadCurve = uiState.predictedLoadCurve,
                    achievements = uiState.achievements
                )
            }
        }
    }

    if (showTaskEditor) {
        TaskEditorDialog(
            signal = uiState.signal,
            onDismiss = { showTaskEditor = false },
            onCreate = { task ->
                onCreateTask(task)
                showTaskEditor = false
            }
        )
    }
}
