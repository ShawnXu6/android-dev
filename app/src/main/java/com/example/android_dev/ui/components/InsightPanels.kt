@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.CognitiveSnapshot
import com.example.android_dev.domain.HabitPlan
import com.example.android_dev.domain.ProductivityInsight
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.UserCognitiveSignal

// 模型摘要功能：展示任务、完成率、认知负荷和压力等核心状态。
@Composable
fun ModelSummaryPanel(tasks: List<SmartTask>, signal: UserCognitiveSignal, snapshot: CognitiveSnapshot) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("端侧智能状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                MetricPill("任务", tasks.size / 12f)
                MetricPill("完成", if (tasks.isEmpty()) 0f else tasks.count { it.isCompleted }.toFloat() / tasks.size)
                MetricPill("负荷", snapshot.overall)
                MetricPill("压力", signal.stress)
            }
            Text(
                "当前版本采用可解释启发式模型完成分类、排程、时间估算和界面自适应，所有数据保存在本机。",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 微习惯方案功能：把推荐任务拆成低阻力启动动作。
@Composable
fun HabitPlanPanel(plan: HabitPlan) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("微习惯方案", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            InsightLine("提示", plan.anchor)
            InsightLine("动作", plan.tinyAction)
            InsightLine("强化", plan.reinforcement)
            InsightLine("进阶", plan.nextStep)
        }
    }
}

// 行为洞察卡片功能：展示单条建议及其严重程度。
@Composable
fun InsightCard(insight: ProductivityInsight) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = insight.severity.tint().copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(insight.severity.tint())
                    .padding(top = 4.dp)
            )
            Column {
                Text(insight.title, fontWeight = FontWeight.SemiBold)
                Text(insight.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
