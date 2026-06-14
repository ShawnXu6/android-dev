@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.example.android_dev.domain.EmotionalTone
import com.example.android_dev.domain.EnvironmentContext
import com.example.android_dev.domain.UserCognitiveSignal

// 状态调节功能：允许用户手动调整专注、精力、压力、情绪、环境和自适应模式。
// 紧凑设计：默认折叠，仅显示标题 + 自适应开关 + 当前状态摘要；点击展开后才显示滑杆和枚举芯片。
@Composable
fun CognitiveControls(signal: UserCognitiveSignal, onSignalChange: (UserCognitiveSignal) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 折叠态头部：标题 + 状态摘要 + 自适应开关 + 展开/收起按钮，整块可点。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("状态感知", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = stateSummary(signal),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = signal.adaptiveMode,
                    onCheckedChange = { onSignalChange(signal.copy(adaptiveMode = it)) }
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (signal.adaptiveMode) "自适应已开启：忙碌时自动精简界面。" else "自适应已关闭：始终显示完整排程。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
    }
}

// 状态摘要功能：把当前专注/精力/压力浓缩成折叠态下的一行文字。
private fun stateSummary(signal: UserCognitiveSignal): String =
    "专注${percent(signal.focus)} · 精力${percent(signal.energy)} · 压力${percent(signal.stress)} · ${signal.mood.label}"
