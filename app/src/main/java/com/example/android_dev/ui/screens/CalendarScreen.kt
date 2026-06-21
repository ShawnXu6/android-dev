@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.ui.components.StatusBadge
import com.example.android_dev.ui.components.tint
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import androidx.compose.ui.graphics.Color
import com.example.android_dev.ui.components.StatusBadge
import com.example.android_dev.domain.Countdown
import com.example.android_dev.ui.components.StatusBadge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.example.android_dev.ui.components.CountdownDialog

// 日历视图功能：按截止日期把任务铺到月历格子，点选某天查看当天任务。
@Composable
fun CalendarScreen(
    tasks: List<SmartTask>,
    onToggleTask: (SmartTask) -> Unit,
    onEditTask: (SmartTask) -> Unit,
    countdowns: List<Countdown> = emptyList(), // 新增
    onEditCountdown: (Countdown) -> Unit,   // 新增
    onDeleteCountdown: (String) -> Unit     // 新增
) {
    val today = remember { LocalDate.now() }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf(today) }
    var editingCountdown by remember { mutableStateOf<Countdown?>(null) }

    // 按截止日期分组任务，用于在格子上显示数量圆点。
    val tasksByDate = remember(tasks) {
        tasks.filter { it.dueDate != null }.groupBy { it.dueDate!! }
    }
    val countdownsByDate = remember(countdowns) {
        countdowns.groupBy { it.targetDate }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 月份切换头部。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) { Text("‹ 上月") }
            Text(
                "${visibleMonth.year} 年 ${visibleMonth.monthValue} 月",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) { Text("下月 ›") }
        }

        // 星期表头。
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                Text(
                    day,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 月历网格。
        MonthGrid(
            month = visibleMonth,
            today = today,
            selectedDate = selectedDate,
            tasksByDate = tasksByDate,
            countdownsByDate = countdownsByDate,  // 新增
            onSelect = { selectedDate = it }
        )

        Text(
            "$selectedDate 的任务",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        val dayTasks = tasksByDate[selectedDate].orEmpty()
        val dayCountdowns = remember(selectedDate, countdowns) {
            countdowns.filter { it.targetDate == selectedDate }
        }

        if (dayCountdowns.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("倒计时", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                dayCountdowns.forEach { countdown ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // 第一行：标题 + 状态徽章（右侧）
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(countdown.title, fontWeight = FontWeight.SemiBold)
                                    if (countdown.note.isNotBlank()) {
                                        Text(
                                            countdown.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                val days = ChronoUnit.DAYS.between(LocalDate.now(), countdown.targetDate).toInt()
                                StatusBadge(
                                    if (days < 0) "已过 ${-days} 天" else "还剩 $days 天",
                                    if (days < 0) Color(0xFFB13E4B) else Color(0xFF2F6F63)
                                )
                            }
                            // 第二行：编辑和删除按钮（右对齐）
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { editingCountdown = countdown }) {
                                    Text("编辑")
                                }
                                TextButton(onClick = { onDeleteCountdown(countdown.id) }) {
                                    Text("删除", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (dayTasks.isEmpty()) {
            Text("这一天没有截止任务。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dayTasks, key = { it.id }) { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    task.title,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                StatusBadge(text = if (task.isCompleted) "已完成" else "优先级 ${task.priority.label}", color = task.priority.tint())
                            }
                            TextButton(onClick = { onEditTask(task) }) { Text("编辑") }
                            TextButton(onClick = { onToggleTask(task) }) {
                                Text(if (task.isCompleted) "撤销" else "完成")
                            }
                        }
                    }
                }
            }
        }
    }
    editingCountdown?.let { countdown ->
        CountdownDialog(
            initialCountdown = countdown,
            onDismiss = { editingCountdown = null },
            onConfirm = { updated ->
                onEditCountdown(updated)
                editingCountdown = null
            }
        )
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    tasksByDate: Map<LocalDate, List<SmartTask>>,
    countdownsByDate: Map<LocalDate, List<Countdown>>,  // 新增
    onSelect: (LocalDate) -> Unit
) {
    val firstDay = month.atDay(1)
    // 周一为第一列：DayOfWeek MONDAY=1 → 偏移 0。
    val leadingBlanks = firstDay.dayOfWeek.value - 1
    val daysInMonth = month.lengthOfMonth()
    val totalCells = leadingBlanks + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - leadingBlanks + 1
                    Box(modifier = Modifier.weight(1f)) {
                        if (dayNumber in 1..daysInMonth) {
                            val date = month.atDay(dayNumber)
                            DayCell(
                                date = date,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                taskCount = tasksByDate[date]?.size ?: 0,
                                hasOverdue = tasksByDate[date]?.any { it.isOverdue } == true,
                                hasCountdown = countdownsByDate[date].isNullOrEmpty() == false,
                                isCountdownFuture = !date.isBefore(today),  // 新增：当天或未来为 true// 新增
                                onClick = { onSelect(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    taskCount: Int,
    hasOverdue: Boolean,
    hasCountdown: Boolean,  // 新增
    isCountdownFuture: Boolean,   // 新增
    onClick: () -> Unit
) {
    val container = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isToday -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .border(
                width = if (isToday) 1.dp else 0.dp,
                color = if (isToday) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (taskCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                )
            }
            // 倒计时点（新增颜色区分）
            if (hasCountdown) {
                val dotColor = if (isCountdownFuture) {
                    Color(0xFF9C27B0)  // 紫色
                } else {
                    Color(0xFFFFC107)  // 黄色（琥珀色）
                }
                Box(
                    modifier = Modifier
                        .padding(top = if (taskCount > 0) 10.dp else 2.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}
