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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.Countdown
import com.example.android_dev.ui.components.CountdownDialog
import com.example.android_dev.ui.components.StatusBadge
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun CountdownScreen(
    countdowns: List<Countdown>,
    onAdd: (Countdown) -> Unit,
    onEdit: (Countdown) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCountdown by remember { mutableStateOf<Countdown?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("未来倒计时", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onClose) { Text("← 返回") }
                },
                actions = {
                    TextButton(onClick = { showAddDialog = true }) { Text("＋ 添加") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        val sorted = sortCountdowns(countdowns)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 72.dp)
        ) {
            if (sorted.isEmpty()) {
                item {
                    Text(
                        "还没有倒计时，点击右上角「＋ 添加」创建。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            } else {
                items(sorted, key = { it.id }) { countdown ->
                    CountdownCard(
                        countdown = countdown,
                        onEdit = { editingCountdown = countdown },
                        onDelete = { onDelete(countdown.id) }
                    )
                }
            }
        }
    }

    // 添加弹窗
    if (showAddDialog) {
        CountdownDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newCountdown ->
                onAdd(newCountdown)
                showAddDialog = false
            }
        )
    }

    // 编辑弹窗
    editingCountdown?.let { countdown ->
        CountdownDialog(
            initialCountdown = countdown,
            onDismiss = { editingCountdown = null },
            onConfirm = { updated ->
                onEdit(updated)
                editingCountdown = null
            }
        )
    }
}

@Composable
private fun CountdownCard(
    countdown: Countdown,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val today = LocalDate.now()
    val daysDiff = ChronoUnit.DAYS.between(today, countdown.targetDate).toInt()
    val isExpired = daysDiff < 0
    val daysDisplay = if (isExpired) "已过去 ${-daysDiff} 天" else "还剩 $daysDiff 天"

    val startDate = Instant.ofEpochMilli(countdown.createdAt)
        .atZone(ZoneId.systemDefault()).toLocalDate()
    val totalDays = ChronoUnit.DAYS.between(startDate, countdown.targetDate).toInt()
    val elapsedDays = ChronoUnit.DAYS.between(startDate, today).toInt()
    val progressText = if (!isExpired && totalDays > 0) {
        "已过 ${elapsedDays.coerceIn(0, totalDays)} / $totalDays 天"
    } else null

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        countdown.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (countdown.note.isNotBlank()) {
                        Text(
                            countdown.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusBadge(
                    daysDisplay,
                    if (isExpired) Color(0xFFB13E4B) else Color(0xFF2F6F63)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "目标日期: ${countdown.targetDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                progressText?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

private fun sortCountdowns(countdowns: List<Countdown>): List<Countdown> {
    val today = LocalDate.now()
    val (active, expired) = countdowns.partition { !it.targetDate.isBefore(today) }
    val sortedActive = active.sortedBy { ChronoUnit.DAYS.between(today, it.targetDate) }
    val sortedExpired = expired.sortedBy { ChronoUnit.DAYS.between(it.targetDate, today) }
    return sortedActive + sortedExpired
}