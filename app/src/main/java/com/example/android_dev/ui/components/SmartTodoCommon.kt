@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.CognitiveLoadLevel
import com.example.android_dev.domain.InsightSeverity
import com.example.android_dev.domain.TaskCategory
import com.example.android_dev.domain.TaskPriority
import com.example.android_dev.domain.TaskStatus
import kotlin.math.roundToInt

// 底部导航功能：在今日、任务、洞察和统计四个主页面之间切换。
@Composable
fun SmartTodoBottomBar(selectedTab: SmartTodoTab, onSelect: (SmartTodoTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        SmartTodoTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onSelect(tab) },
                icon = {
                    Surface(
                        shape = CircleShape,
                        color = if (selectedTab == tab) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ) {
                        Text(
                            text = tab.mark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                label = { Text(tab.label) }
            )
        }
    }
}

// 指标胶囊功能：以百分比形式展示认知负荷、完成率等轻量指标。
@Composable
fun MetricPill(label: String, value: Float) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 76.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(percent(value.coerceIn(0f, 1f)), fontWeight = FontWeight.Bold)
        }
    }
}

// 状态徽章功能：用强调色展示优先级、耗时、完成状态等短文本。
@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// 信号滑杆功能：让用户调整专注、精力和压力等 0 到 1 的状态值。
@Composable
fun SignalSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(percent(value), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = { onChange(it.coerceIn(0f, 1f)) },
            valueRange = 0f..1f
        )
    }
}

// 弹窗滑杆功能：让用户在新建任务时调整重要度、复杂度和目标时间。
@Composable
fun DialogSlider(label: String, value: Float, suffix: String, onChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text(suffix, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = if (label == "目标时间") 0f..23f else 1f..5f,
            steps = if (label == "目标时间") 22 else 3
        )
    }
}

// 枚举选择功能：把情绪、环境等枚举值渲染为横向筛选芯片。
@Composable
fun <T> EnumChips(
    title: String,
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            values.forEach { value ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(label(value)) }
                )
            }
        }
    }
}

// 洞察字段功能：以标题和值的形式展示微习惯方案条目。
@Composable
fun InsightLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f))
}

// 主导航标签功能：定义应用底部导航的五个顶层页面（今日 / 看板 / 日历 / 列表 / 我的）。
enum class SmartTodoTab(val label: String, val mark: String) {
    TODAY("今日", "今"),
    BOARD("看板", "板"),
    CALENDAR("日历", "历"),
    TASKS("列表", "单"),
    PROFILE("我的", "我")
}

// 百分比格式化功能：把 0 到 1 的浮点值转换成百分比文本。
fun percent(value: Float): String = "${(value.coerceIn(0f, 1f) * 100).roundToInt()}%"

// 小时格式化功能：把整数小时转换成 24 小时制整点文本。
fun Int.hourLabel(): String = "%02d:00".format(coerceIn(0, 23))

// 任务分类颜色功能：为不同任务分类提供一致的 UI 强调色。
fun TaskCategory.tint(): Color = when (this) {
    TaskCategory.WORK -> Color(0xFF2F6F63)
    TaskCategory.STUDY -> Color(0xFF4D65A8)
    TaskCategory.HEALTH -> Color(0xFF6C8F2E)
    TaskCategory.LIFE -> Color(0xFF9A6A20)
    TaskCategory.SOCIAL -> Color(0xFFB45347)
    TaskCategory.CREATIVE -> Color(0xFF7D5594)
}

// 优先级颜色功能：高/中/低三档对应红/橙/绿，给用户直观语义。
fun TaskPriority.tint(): Color = when (this) {
    TaskPriority.HIGH -> Color(0xFFB13E4B)
    TaskPriority.MEDIUM -> Color(0xFFC06A24)
    TaskPriority.LOW -> Color(0xFF2F7D57)
}

// 看板状态颜色功能：为待处理/进行中/已完成提供一致的强调色。
fun TaskStatus.tint(): Color = when (this) {
    TaskStatus.TODO -> Color(0xFF4D65A8)
    TaskStatus.IN_PROGRESS -> Color(0xFFC06A24)
    TaskStatus.DONE -> Color(0xFF2F7D57)
}

// 认知负荷颜色功能：为不同负荷等级提供一致的 UI 强调色。
fun CognitiveLoadLevel.tint(): Color = when (this) {
    CognitiveLoadLevel.LOW -> Color(0xFF2F7D57)
    CognitiveLoadLevel.BALANCED -> Color(0xFF2E6EA6)
    CognitiveLoadLevel.HIGH -> Color(0xFFC06A24)
    CognitiveLoadLevel.OVERWHELMING -> Color(0xFFB13E4B)
}

// 洞察严重度颜色功能：为不同建议级别提供一致的 UI 强调色。
fun InsightSeverity.tint(): Color = when (this) {
    InsightSeverity.GOOD -> Color(0xFF2F7D57)
    InsightSeverity.NOTICE -> Color(0xFF2E6EA6)
    InsightSeverity.WARNING -> Color(0xFFB13E4B)
}
