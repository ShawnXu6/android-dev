@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.screens

import android.graphics.Canvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.AchievementBadge
import com.example.android_dev.domain.AchievementCategory
import com.example.android_dev.domain.CognitiveLoadRecord
import com.example.android_dev.domain.CompletionTrend
import com.example.android_dev.domain.DailyStats
import com.example.android_dev.domain.HeatmapData
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskCategory
import com.example.android_dev.domain.WeeklyReport
import kotlin.math.roundToInt

// ========== 统计主屏幕 ==========

@Composable
fun StatisticsScreen(
    tasks: List<SmartTask>,
    weeklyReport: WeeklyReport,
    heatmapData: HeatmapData,
    loadCurve: List<CognitiveLoadRecord>,
    predictedLoadCurve: List<CognitiveLoadRecord>,
    achievements: List<AchievementBadge>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WeeklyReportSummary(report = weeklyReport)
        CompletionTrendChart(dailyStats = weeklyReport.dailyStats)
        CategoryPieChart(
            completedDistribution = weeklyReport.completedCategoryDistribution,
            pendingDistribution = weeklyReport.pendingCategoryDistribution
        )
        CognitiveLoadCurve(realLoadRecords = loadCurve, predictedLoadRecords = predictedLoadCurve)
        HabitHeatmap(heatmapData = heatmapData)
        AchievementPanel(badges = achievements)
        Spacer(modifier = Modifier.height(72.dp))
    }
}

// ========== 完成率趋势图 ==========

@Composable
fun CompletionTrendChart(
    dailyStats: List<DailyStats>,
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("完成率趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("近 7 天", style = MaterialTheme.typography.labelMedium, color = onSurfaceVariantColor)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (dailyStats.none { it.totalTasks > 0 || it.completedTasks > 0 }) {
                ChartEmptyState("近 7 天还没有任务完成数据")
                return@Column
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 24.dp.toPx()
                val chartWidth = width - padding * 2
                val chartHeight = height - padding * 2

                (0..4).forEach { i ->
                    val y = padding + (chartHeight / 4) * i
                    drawLine(
                        color = outlineColor,
                        start = Offset(padding, y),
                        end = Offset(width - padding, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (dailyStats.size > 1) {
                    val points = dailyStats.mapIndexed { index, stats ->
                        val x = padding + (chartWidth / (dailyStats.size - 1)) * index
                        val y = padding + chartHeight * (1f - stats.completionRate)
                        Offset(x, y)
                    }

                    val areaPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(points.first().x, padding + chartHeight)
                        points.forEach { point -> lineTo(point.x, point.y) }
                        lineTo(points.last().x, padding + chartHeight)
                        close()
                    }
                    drawPath(path = areaPath, color = primaryColor.copy(alpha = 0.12f))

                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = primaryColor,
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    points.forEach { point ->
                        drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = point)
                        drawCircle(color = surfaceColor, radius = 2.dp.toPx(), center = point)
                    }
                }

                (0..4).forEach { i ->
                    val y = padding + (chartHeight / 4) * i
                    val value = (100 - i * 25)
                    drawContext.canvas.nativeCanvas.drawText(
                        "${value}%",
                        4.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 10.dp.toPx()
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                    )
                }

                dailyStats.forEachIndexed { index, stats ->
                    val x = if (dailyStats.size == 1) padding + chartWidth / 2 else padding + (chartWidth / (dailyStats.size - 1)) * index
                    val shortDate = stats.date.substring(5)
                    drawContext.canvas.nativeCanvas.drawText(
                        shortDate,
                        x,
                        height - 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 10.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}

// ========== 时间分布饼图 ==========

@Composable
fun CategoryPieChart(
    completedDistribution: Map<TaskCategory, Float>,
    pendingDistribution: Map<TaskCategory, Float>,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val hasCompletedData = completedDistribution.any { (_, value) -> value > 0.01f }
    val hasPendingData = pendingDistribution.any { (_, value) -> value > 0.01f }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("时间分布", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            if (!hasCompletedData && !hasPendingData) {
                ChartEmptyState("暂无已完成或未完成任务的时间分布")
                return@Column
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(
                    modifier = Modifier
                        .size(140.dp)
                        .weight(1f)
                ) {
                    val size = minOf(size.width, size.height)
                    val center = Offset(size / 2, size / 2)
                    val radius = size / 2 * 0.85f
                    var startAngle = -90f

                    val distribution = if (hasCompletedData) completedDistribution else pendingDistribution
                    distribution.forEach { (category, percentage) ->
                        if (percentage > 0.01f) {
                            val sweepAngle = percentage * 360f
                            drawArc(
                                color = category.chartTint(),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    drawCircle(
                        color = surfaceColor,
                        radius = radius * 0.55f,
                        center = center
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        if (hasCompletedData) "已完成" else "未完成",
                        style = MaterialTheme.typography.labelLarge,
                        color = onSurfaceVariantColor
                    )
                    CategoryLegend(distribution = if (hasCompletedData) completedDistribution else pendingDistribution)
                    if (hasCompletedData && hasPendingData) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("未完成", style = MaterialTheme.typography.labelLarge, color = onSurfaceVariantColor)
                        CategoryLegend(distribution = pendingDistribution)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryLegend(distribution: Map<TaskCategory, Float>) {
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val sortedEntries = distribution
        .filter { (_, v) -> v > 0.01f }
        .toList()
        .sortedByDescending { (_, v) -> v }

    sortedEntries.forEach { (category, percentage) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(category.chartTint(), RoundedCornerShape(2.dp))
                                )
                                Column {
                                    Text(
                                        category.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${(percentage * 100).roundToInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = onSurfaceVariantColor
                                    )
                                }
                            }
                        }
}

// ========== 习惯连续性热力图 ==========

@Composable
fun HabitHeatmap(
    heatmapData: HeatmapData,
    modifier: Modifier = Modifier
) {
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("习惯热力图", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("近 30 天", style = MaterialTheme.typography.labelMedium, color = onSurfaceVariantColor)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (heatmapData.values.values.all { it == 0 }) {
                ChartEmptyState("近 30 天还没有习惯打卡记录")
                return@Column
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val weeks = heatmapData.dates.chunked(7)
                weeks.forEach { weekDates ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        weekDates.forEach { date ->
                            val value = heatmapData.values[date] ?: 0
                            val intensity = if (heatmapData.maxStreak == 0) 0f else value / heatmapData.maxStreak.toFloat()
                            val bgColor = when {
                                intensity == 0f -> surfaceVariantColor
                                intensity < 0.33f -> Color(0xFFA7D7CA).copy(alpha = 0.4f)
                                intensity < 0.66f -> Color(0xFFA7D7CA).copy(alpha = 0.7f)
                                else -> Color(0xFF2F6F63)
                            }
                            Surface(
                                modifier = Modifier.size(18.dp),
                                shape = RoundedCornerShape(3.dp),
                                color = bgColor
                            ) {}
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("少", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
                listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { intensity ->
                    val bgColor = when {
                        intensity == 0f -> surfaceVariantColor
                        intensity < 0.33f -> Color(0xFFA7D7CA).copy(alpha = 0.4f)
                        intensity < 0.66f -> Color(0xFFA7D7CA).copy(alpha = 0.7f)
                        else -> Color(0xFF2F6F63)
                    }
                    Spacer(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(12.dp)
                            .background(bgColor, RoundedCornerShape(2.dp))
                    )
                }
                Text("多", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
            }
        }
    }
}

// ========== 认知负荷曲线 ==========

@Composable
fun CognitiveLoadCurve(
    realLoadRecords: List<CognitiveLoadRecord>,
    predictedLoadRecords: List<CognitiveLoadRecord>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val predictedColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val hasRealData = realLoadRecords.isNotEmpty()
    val hasPredictedData = predictedLoadRecords.isNotEmpty()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("认知负荷曲线", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("真实 / 预测", style = MaterialTheme.typography.labelMedium, color = onSurfaceVariantColor)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (!hasRealData && !hasPredictedData) {
                ChartEmptyState("暂无认知负荷记录")
                return@Column
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 24.dp.toPx()
                val chartWidth = width - padding * 2
                val chartHeight = height - padding * 2

                val lowLoadY = padding + chartHeight * (1f - 0.32f)
                val balancedLoadY = padding + chartHeight * (1f - 0.58f)
                val highLoadY = padding + chartHeight * (1f - 0.78f)

                drawRect(
                    color = Color(0xFF4CAF50).copy(alpha = 0.08f),
                    topLeft = Offset(padding, lowLoadY),
                    size = Size(chartWidth, balancedLoadY - lowLoadY)
                )
                drawRect(
                    color = Color(0xFFFFC107).copy(alpha = 0.08f),
                    topLeft = Offset(padding, balancedLoadY),
                    size = Size(chartWidth, highLoadY - balancedLoadY)
                )
                drawRect(
                    color = Color(0xFFF44336).copy(alpha = 0.08f),
                    topLeft = Offset(padding, highLoadY),
                    size = Size(chartWidth, padding + chartHeight - highLoadY)
                )

                if (hasPredictedData) {
                    val points = predictedLoadRecords.map { record ->
                        val x = padding + (record.hour - 8) / 14f * chartWidth
                        val y = padding + chartHeight * (1f - record.overall.coerceIn(0f, 1f))
                        Offset(x, y)
                    }
                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = predictedColor.copy(alpha = 0.72f),
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                if (hasRealData) {
                    val points = realLoadRecords.map { record ->
                        val x = padding + (record.hour - 8) / 14f * chartWidth
                        val y = padding + chartHeight * (1f - record.overall.coerceIn(0f, 1f))
                        Offset(x, y)
                    }

                    val areaPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(points.first().x, padding + chartHeight)
                        points.forEach { point -> lineTo(point.x, point.y) }
                        lineTo(points.last().x, padding + chartHeight)
                        close()
                    }
                    drawPath(path = areaPath, color = primaryColor.copy(alpha = 0.15f))

                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = primaryColor,
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    points.forEachIndexed { index, point ->
                        val record = realLoadRecords[index]
                        val pointColor = when {
                            record.overall < 0.32f -> Color(0xFF4CAF50)
                            record.overall < 0.58f -> Color(0xFFFFC107)
                            record.overall < 0.78f -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                        drawCircle(color = pointColor, radius = 4.dp.toPx(), center = point)
                        drawCircle(color = surfaceColor, radius = 2.dp.toPx(), center = point)
                    }

                    val bestHour = realLoadRecords.minByOrNull { it.overall }?.hour
                    bestHour?.let { hour ->
                        val x = padding + (hour - 8) / 14f * chartWidth
                        drawContext.canvas.nativeCanvas.drawText(
                            "最佳",
                            x,
                            padding - 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#2F6F63")
                                textSize = 10.dp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }

                (8..22 step 2).forEach { hour ->
                    val x = padding + (hour - 8) / 14f * chartWidth
                    drawContext.canvas.nativeCanvas.drawText(
                        "${hour}h",
                        x,
                        height - 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 10.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }

                listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { value ->
                    val y = padding + chartHeight * (1f - value)
                    drawContext.canvas.nativeCanvas.drawText(
                        "${(value * 100).roundToInt()}%",
                        4.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 10.dp.toPx()
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Pair(primaryColor, "真实"),
                    Pair(predictedColor, "预测"),
                    Pair(Color(0xFF4CAF50), "低负荷"),
                    Pair(Color(0xFFFFC107), "平衡"),
                    Pair(Color(0xFFFF9800), "高负荷"),
                    Pair(Color(0xFFF44336), "过载")
                ).forEach { (color, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color, RoundedCornerShape(2.dp))
                        )
                        Text(label, style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
                    }
                }
            }
        }
    }
}

// ========== 成就面板 ==========

@Composable
fun AchievementPanel(
    badges: List<AchievementBadge>,
    modifier: Modifier = Modifier
) {
    val unlockedCount = badges.count { it.unlockedAt != null }
    val totalCount = badges.size
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("成就徽章", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "$unlockedCount / $totalCount",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("总进度", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${(badges.map { it.progress }.average() * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { badges.map { it.progress }.average().toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AchievementCategory.entries.forEach { category ->
                val categoryBadges = badges.filter { it.category == category }
                if (categoryBadges.isNotEmpty()) {
                    Text(
                        category.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = onSurfaceVariantColor,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categoryBadges.forEach { badge ->
                            AchievementBadgeItem(badge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementBadgeItem(badge: AchievementBadge) {
    val isUnlocked = badge.unlockedAt != null
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isUnlocked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isUnlocked) badge.icon else "",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    badge.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isUnlocked) FontWeight.Bold else FontWeight.Normal,
                    color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else onSurfaceVariantColor
                )
                Text(
                    badge.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariantColor
                )
                if (!isUnlocked && badge.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { badge.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.outline,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            if (isUnlocked) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        "已解锁",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    "${(badge.progress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = onSurfaceVariantColor
                )
            }
        }
    }
}

// ========== 周/月报摘要卡片 ==========

@Composable
fun WeeklyReportSummary(
    report: WeeklyReport,
    modifier: Modifier = Modifier
) {
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("周报摘要", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(report.weekStart, style = MaterialTheme.typography.labelMedium, color = onSurfaceVariantColor)
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "完成率",
                    value = "${(report.avgCompletionRate * 100).roundToInt()}%",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "已完成",
                    value = "${report.totalCompleted}/${report.totalTasks}",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "趋势",
                    value = when (report.trend) {
                        CompletionTrend.IMPROVING -> "上升 ↑"
                        CompletionTrend.STABLE -> "平稳 →"
                        CompletionTrend.DECLINING -> "下降 ↓"
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("习惯总连续天数", style = MaterialTheme.typography.labelMedium)
                Text(
                    "${report.totalHabitStreak} 天",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ========== 每日统计卡片 ==========

@Composable
fun DailyStatsCard(
    dailyStats: DailyStats,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    dailyStats.date.substring(5),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${dailyStats.completedTasks}/${dailyStats.totalTasks} 任务",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${(dailyStats.completionRate * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${dailyStats.totalMinutes} 分钟",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChartEmptyState(message: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun TaskCategory.chartTint(): Color = when (this) {
    TaskCategory.WORK -> Color(0xFF2F6F63)
    TaskCategory.STUDY -> Color(0xFF4D65A8)
    TaskCategory.HEALTH -> Color(0xFFB45347)
    TaskCategory.LIFE -> Color(0xFF8B5CF6)
    TaskCategory.SOCIAL -> Color(0xFFF59E0B)
    TaskCategory.CREATIVE -> Color(0xFFEC4899)
}
