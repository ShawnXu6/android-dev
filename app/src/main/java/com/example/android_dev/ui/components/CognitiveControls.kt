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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("状态感知", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "由用户可控信号驱动，避免隐式采集敏感生理数据。",
                        style = MaterialTheme.typography.labelMedium,
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
