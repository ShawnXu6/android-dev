@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.CognitiveLoadLevel
import com.example.android_dev.domain.CognitiveSnapshot
import com.example.android_dev.domain.ScheduleSlot
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskRecommendation
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.ui.components.CognitiveControls
import com.example.android_dev.ui.components.CognitiveStatusPanel
import com.example.android_dev.ui.components.MinimalFocusPanel
import com.example.android_dev.ui.components.RecommendedTaskPanel
import com.example.android_dev.ui.components.SchedulePanel
// 今日页功能：展示认知负荷、快速捕捉、下一步推荐、状态调节和自适应排程。
@Composable
fun TodayScreen(
    tasks: List<SmartTask>,
    signal: UserCognitiveSignal,
    snapshot: CognitiveSnapshot,
    schedule: List<ScheduleSlot>,
    nextTask: SmartTask?,
    nextRecommendation: TaskRecommendation?,
    onSignalChange: (UserCognitiveSignal) -> Unit,
    onQuickAddTask: (String, String) -> Unit,
    onToggleTask: (SmartTask) -> Unit
) {
    val simplified = signal.adaptiveMode && snapshot.level >= CognitiveLoadLevel.HIGH
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CognitiveStatusPanel(snapshot = snapshot, signal = signal)
        QuickCapturePanel(onQuickAddTask = onQuickAddTask)

        nextTask?.let {
            RecommendedTaskPanel(
                task = it,
                recommendation = nextRecommendation,
                signal = signal,
                onToggleTask = { onToggleTask(it) }
            )
        }

        CognitiveControls(signal = signal, onSignalChange = onSignalChange)

        AnimatedVisibility(visible = !simplified) {
            SchedulePanel(schedule = schedule)
        }
        AnimatedVisibility(visible = simplified) {
            MinimalFocusPanel(tasks = tasks, nextTask = nextTask)
        }
    }
}

// 快速捕捉功能：收集标题和备注，由 ViewModel 创建智能任务。
@Composable
private fun QuickCapturePanel(onQuickAddTask: (String, String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("快速捕捉", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("任务") },
                placeholder = { Text("例如：今天下午整理项目计划") },
                maxLines = 2
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("补充信息") },
                maxLines = 2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本地语义分类 · 时间估算 · 负荷适配",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    enabled = text.isNotBlank(),
                    onClick = {
                        onQuickAddTask(text, note)
                        text = ""
                        note = ""
                    }
                ) {
                    Text("加入")
                }
            }
        }
    }
}


