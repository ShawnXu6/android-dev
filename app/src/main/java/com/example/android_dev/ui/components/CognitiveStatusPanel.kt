@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.CognitiveSnapshot
import com.example.android_dev.domain.UserCognitiveSignal

// 认知负荷状态功能：显示综合负荷等级、分项指标和当前建议。
@Composable
fun CognitiveStatusPanel(snapshot: CognitiveSnapshot, signal: UserCognitiveSignal) {
    ElevatedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("今日忙碌程度", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = snapshot.level.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                // 把负荷进度做成行内细条，和数值徽章并排，省去独立一行的进度条。
                LinearProgressIndicator(
                    progress = { snapshot.overall },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = snapshot.level.tint(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                StatusBadge(text = percent(snapshot.overall), color = snapshot.level.tint())
            }
            Text(
                text = snapshot.recommendation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                MetricChip("任务量", snapshot.visualLoad)
                MetricChip("难度", snapshot.memoryLoad)
                MetricChip("时间紧", snapshot.temporalPressure)
                MetricChip("琐碎度", snapshot.decisionFatigue)
                MetricChip("专注", signal.focus)
            }
        }
    }
}
