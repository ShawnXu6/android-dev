package com.example.android_dev

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.android_dev.data.AccountRepository
import com.example.android_dev.data.LocalSmartTodoRepository
import com.example.android_dev.data.ThemePreference
import com.example.android_dev.reminder.NotificationHelper
import com.example.android_dev.reminder.ReminderScheduler
import com.example.android_dev.ui.SmartTodoApp
import com.example.android_dev.ui.auth.AuthScreen
import com.example.android_dev.ui.theme.AndroiddevTheme
import com.example.android_dev.ui.theme.AppPalette
import com.example.android_dev.viewmodel.SmartTodoViewModel

// 应用入口功能：配置边到边显示、登录门、通知权限，并按登录用户挂载 Compose 根组件。
class MainActivity : ComponentActivity() {

    private val accountRepository by lazy { AccountRepository(applicationContext) }
    private val themePreference by lazy { ThemePreference(applicationContext) }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* 用户选择即可，无需处理结果 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannel(this)
        requestNotificationPermissionIfNeeded()

        setContent {
            // 主题状态提到顶层：切换后立即重组，并持久化到偏好。
            var palette by remember { mutableStateOf(themePreference.load()) }
            val onChangeTheme: (AppPalette) -> Unit = {
                palette = it
                themePreference.save(it)
            }

            AndroiddevTheme(dynamicColor = false, palette = palette) {
                var currentUser by remember { mutableStateOf(accountRepository.currentUsername()) }

                val user = currentUser
                if (user == null) {
                    AuthScreen(
                        accountRepository = accountRepository,
                        onAuthenticated = { currentUser = it }
                    )
                } else {
                    // 每个登录用户拥有独立的 ViewModel（独立数据分库）。
                    val viewModel = remember(user) { buildViewModel(user) }
                    val uiState by viewModel.uiState.collectAsState()
                    val chatState by viewModel.chatState.collectAsState()

                    SmartTodoApp(
                        uiState = uiState,
                        username = user,
                        palette = palette,
                        onChangeTheme = onChangeTheme,
                        chatState = chatState,
                        onSendChat = viewModel::sendChatMessage,
                        onClearChat = viewModel::clearChat,
                        onExtractTasks = viewModel::extractTasksFromMessage,
                        onAddExtractedTasks = viewModel::addExtractedTasks,
                        onSignalChange = viewModel::updateSignal,
                        onQuickAddTask = viewModel::quickAddTask,
                        onCreateTask = viewModel::addTask,
                        onUpdateTask = viewModel::updateTask,
                        onToggleTask = viewModel::toggleTaskCompletion,
                        onDeleteTask = viewModel::deleteTask,
                        onMoveStatus = viewModel::moveTaskStatus,
                        onRequestAiBreakdown = { request, onResult, onError ->
                            viewModel.requestAiBreakdown(request, onResult, onError)
                        },
                        onLogout = {
                            accountRepository.logout()
                            currentUser = null
                        }
                    )
                }
            }
        }
    }

    // 按用户构造 ViewModel 功能：每个登录用户使用独立数据分库，登出再登录会重建。
    private fun buildViewModel(username: String): SmartTodoViewModel {
        return SmartTodoViewModel(
            repository = LocalSmartTodoRepository(applicationContext, username),
            reminderScheduler = ReminderScheduler(applicationContext)
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
