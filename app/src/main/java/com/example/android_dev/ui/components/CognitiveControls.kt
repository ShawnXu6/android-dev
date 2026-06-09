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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.EmotionalTone
import com.example.android_dev.domain.EnvironmentContext
import com.example.android_dev.domain.UserCognitiveSignal

// 状态调节功能：允许用户手动调整专注、精力、压力、情绪、环境和自适应模式。
@Composable
fun CognitiveControls(signal: UserCognitiveSignal, onSignalChange: (UserCognitiveSignal) -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text("状态感知", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "告诉 App 你现在的状态，它会据此调整任务推荐、耗时预估和界面繁简。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 自适应模式开关：开启后，任务过多/状态偏差时今日页会自动切换到极简专注视图。
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("自适应模式", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (signal.adaptiveMode) {
                            "已开启：忙碌时自动精简界面，只留最该做的事。"
                        } else {
                            "已关闭：始终显示完整排程视图。"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = signal.adaptiveMode,
                    onCheckedChange = { onSignalChange(signal.copy(adaptiveMode = it)) }
                )
            }
            SignalSlider("专注", signal.focus) { onSignalChange(signal.copy(focus = it)) }
            SignalSlider("精力", signal.energy) { onSignalChange(signal.copy(energy = it)) }
            SignalSlider("压力", signal.stress) { onSignalChange(signal.copy(stress = it)) }
            EnumChips(
                title = "情绪",
                values = EmotionalTone.entries,
                selected = signal.mood,
                label = { it.label },
                onSelect = { onSignalChange(signal.copy(mood = it)) }
            )
            EnumChips(
                title = "环境",
                values = EnvironmentContext.entries,
                selected = signal.environment,
                label = { it.label },
                onSelect = { onSignalChange(signal.copy(environment = it)) }
            )
        }
    }
}
