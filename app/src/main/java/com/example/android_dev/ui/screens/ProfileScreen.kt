@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.FilterChip
import com.example.android_dev.data.AccountRepository
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.ui.theme.AppPalette

// 我的页面功能：显示账户信息、任务概览、主题切换，并提供登出与进入高级洞察/统计的入口。
@Composable
fun ProfileScreen(
    username: String,
    tasks: List<SmartTask>,
    palette: AppPalette,
    onChangeTheme: (AppPalette) -> Unit,
    onOpenInsights: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenCountdown: () -> Unit,  // 新增
    onLogout: () -> Unit
) {
    val isGuest = username == AccountRepository.GUEST
    val displayName = if (isGuest) "游客" else username
    val total = tasks.count { !it.isHabit }
    val done = tasks.count { !it.isHabit && it.isCompleted }
    val habitStreak = tasks.filter { it.isHabit }.sumOf { it.streak }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 账户卡片。
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp)) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column {
                    Text(displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        if (isGuest) "游客模式 · 数据仅存本机" else "已登录账户",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 概览统计。
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OverviewTile("任务总数", total.toString(), Modifier.weight(1f))
            OverviewTile("已完成", done.toString(), Modifier.weight(1f))
            OverviewTile("习惯连续", "${habitStreak}天", Modifier.weight(1f))
        }

        // 外观主题切换。
        Text("外观主题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppPalette.entries.forEach { option ->
                FilterChip(
                    selected = palette == option,
                    onClick = { onChangeTheme(option) },
                    label = { Text(if (option == AppPalette.PINK_BLUE) "🌸 ${option.label}" else "🌿 ${option.label}") }
                )
            }
        }

        Text("高级功能", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedButton(onClick = onOpenInsights, modifier = Modifier.fillMaxWidth()) {
            Text("🧠 智能洞察与任务建议")
        }
        OutlinedButton(onClick = onOpenStatistics, modifier = Modifier.fillMaxWidth()) {
            Text("📊 统计 / 热力图 / 成就")
        }
        OutlinedButton(onClick = onOpenCountdown, modifier = Modifier.fillMaxWidth()) {
            Text("⏳ 未来倒计时")
        }

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text(if (isGuest) "退出游客模式" else "退出登录")
        }
    }
}

@Composable
private fun OverviewTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
