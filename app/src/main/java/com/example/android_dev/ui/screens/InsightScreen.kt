@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.CognitiveSnapshot
import com.example.android_dev.domain.ProductivityInsight
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.engine.SmartTaskEngine
import com.example.android_dev.ui.components.HabitPlanPanel
import com.example.android_dev.ui.components.InsightCard
import com.example.android_dev.ui.components.ModelSummaryPanel
// 洞察页功能：汇总端侧模型状态、微习惯方案和行为洞察。
@Composable
fun InsightScreen(
    tasks: List<SmartTask>,
    signal: UserCognitiveSignal,
    snapshot: CognitiveSnapshot,
    insights: List<ProductivityInsight>,
    nextTask: SmartTask?
) {
    val habitPlan = remember(nextTask, signal) {
        nextTask?.let { SmartTaskEngine.designHabitPlan(it, signal) }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ModelSummaryPanel(tasks = tasks, signal = signal, snapshot = snapshot)
        habitPlan?.let { HabitPlanPanel(plan = it) }
        Text("行为洞察", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        insights.forEach { insight ->
            InsightCard(insight)
        }
        Spacer(modifier = Modifier.height(72.dp))
    }
}

