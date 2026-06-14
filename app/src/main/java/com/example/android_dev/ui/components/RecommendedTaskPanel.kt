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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskRecommendation
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.engine.SmartTaskEngine
import kotlin.math.roundToInt

// 下一步推荐功能：展示当前优先级最高任务、推荐解释和耗时预测。
@Composable
fun RecommendedTaskPanel(
    task: SmartTask,
    recommendation: TaskRecommendation?,
    signal: UserCognitiveSignal,
    onToggleTask: () -> Unit
) {
    val prediction = remember(task, signal) { SmartTaskEngine.predictTime(task, signal) }
    ElevatedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("下一步推荐", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusBadge("${prediction.minutes} 分", MaterialTheme.colorScheme.primary)
                // 完成入口缩成图标按钮，省去整行的「标记完成」大按钮。
                FilledIconButton(
                    onClick = onToggleTask,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "标记完成")
                }
            }
            Text(
                text = recommendation?.priority?.explanation
                    ?: "${prediction.rationale}，置信度 ${percent(prediction.confidence)}，不确定性约 ±${prediction.uncertaintyMinutes} 分钟。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                recommendation?.let {
                    CompactChip("优先级 ${it.priority.totalScore.roundToInt()}")
                }
                CompactChip(task.category.label)
                CompactChip("复杂度 ${task.complexity}/5")
                CompactChip(task.targetHour.hourLabel())
            }
            recommendation?.priority?.factors
                ?.sortedByDescending { it.contribution }
                ?.take(2)
                ?.forEach { factor ->
                    RecommendationFactorRow(label = factor.label, value = factor.score, reason = factor.reason)
                }
        }
    }
}

@Composable
private fun RecommendationFactorRow(label: String, value: Float, reason: String) {
    // 紧凑因素行：标签、细进度条、百分比放同一行，理由作为第二行小字。
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            LinearProgressIndicator(
                progress = { value.coerceIn(0f, 1f) },
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            )
            Text(percent(value), style = MaterialTheme.typography.labelSmall)
        }
        Text(
            reason,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// 紧凑芯片功能：比 AssistChip 更小的只读标签胶囊，用于推荐卡的元信息。
@Composable
private fun CompactChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
