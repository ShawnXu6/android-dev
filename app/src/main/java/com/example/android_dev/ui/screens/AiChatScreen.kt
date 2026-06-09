@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.ChatMessage
import com.example.android_dev.domain.ChatRole
import com.example.android_dev.domain.ExtractedTask
import com.example.android_dev.ui.components.ExtractedTasksDialog
import com.example.android_dev.viewmodel.AiChatUiState

// AI 对话页功能：与飞桨 AIStudio 大模型多轮对话，并支持把 AI 回复一键提取为计划任务。
@Composable
fun AiChatScreen(
    state: AiChatUiState,
    onSend: (String) -> Unit,
    onExtractTasks: (String, (List<ExtractedTask>) -> Unit, (String) -> Unit) -> Unit,
    onAddTasks: (List<ExtractedTask>) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // 提取流程状态：正在提取的消息 id、提取出的任务、错误与加入成功提示。
    var extractingId by remember { mutableStateOf<String?>(null) }
    var previewTasks by remember { mutableStateOf<List<ExtractedTask>?>(null) }
    var extractError by remember { mutableStateOf<String?>(null) }
    var addedHint by remember { mutableStateOf<String?>(null) }

    // 新消息到达时自动滚到底部。
    LaunchedEffect(state.messages.size, state.isSending) {
        val target = state.messages.size + if (state.isSending) 1 else 0
        if (target > 0) listState.animateScrollToItem((target - 1).coerceAtLeast(0))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            if (state.messages.isEmpty() && !state.isSending) {
                EmptyChatHint(
                    onPick = { input = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        extracting = extractingId == message.id,
                        onExtract = {
                            extractingId = message.id
                            extractError = null
                            onExtractTasks(
                                message.content,
                                { tasks ->
                                    extractingId = null
                                    previewTasks = tasks
                                },
                                { err ->
                                    extractingId = null
                                    extractError = err
                                }
                            )
                        }
                    )
                }
                if (state.isSending) {
                    item { TypingBubble() }
                }
                state.error?.let { error ->
                    item {
                        Text(
                            "⚠️ $error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                extractError?.let { error ->
                    item {
                        Text(
                            "⚠️ 提取失败：$error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                addedHint?.let { hint ->
                    item {
                        Text(
                            hint,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // 输入栏。
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("问点什么…例如：帮我规划今天的复习") },
                    maxLines = 4
                )
                Button(
                    enabled = input.isNotBlank() && !state.isSending,
                    onClick = {
                        onSend(input)
                        input = ""
                    }
                ) { Text("发送") }
            }
        }
    }

    // 提取任务预览弹窗：确认后批量加入计划。
    previewTasks?.let { tasks ->
        ExtractedTasksDialog(
            initialTasks = tasks,
            onConfirm = { selected ->
                onAddTasks(selected)
                previewTasks = null
                addedHint = "✅ 已加入 ${selected.size} 个任务到计划。"
            },
            onDismiss = { previewTasks = null }
        )
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    extracting: Boolean = false,
    onExtract: (() -> Unit)? = null
) {
    val isUser = message.role == ChatRole.USER
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        // 仅 AI 回复显示「加入计划」入口：把这段内容提取为任务。
        if (!isUser && onExtract != null) {
            if (extracting) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp), strokeWidth = 2.dp)
                    Text("正在识别任务…", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                androidx.compose.material3.TextButton(onClick = onExtract) {
                    Text("＋ 加入计划")
                }
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    Row(horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
                Text("AI 正在思考…", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun EmptyChatHint(onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    val suggestions = listOf(
        "帮我把「期末复习」拆成一周计划",
        "怎么克服拖延，立刻开始一个任务？",
        "今天只有 2 小时，我该先做什么？",
        "给我三个提升专注力的小技巧"
    )
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🤖", style = MaterialTheme.typography.displaySmall)
        Text(
            "我是你的 AI 助手",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "规划任务、整理思路、效率建议，都可以问我。试试：",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )
        suggestions.forEach { s ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    s,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(s) }
                        .padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
