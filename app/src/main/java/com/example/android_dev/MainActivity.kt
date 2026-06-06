package com.example.android_dev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.android_dev.data.LocalSmartTodoRepository
import com.example.android_dev.engine.ReminderManager
import com.example.android_dev.ui.SmartTodoApp
import com.example.android_dev.ui.theme.AndroiddevTheme
import com.example.android_dev.viewmodel.SmartTodoViewModel

// 应用入口功能：配置边到边显示、创建 ViewModel，并挂载 Compose 根组件。
class MainActivity : ComponentActivity() {
    private val smartTodoViewModel: SmartTodoViewModel by lazy {
        ViewModelProvider(
            owner = this,
            factory = SmartTodoViewModelFactory(
                repository = LocalSmartTodoRepository(applicationContext)
            )
        )[SmartTodoViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by smartTodoViewModel.uiState.collectAsState()
                        
            ReminderManager.rescheduleReminders(applicationContext, uiState.tasks)

            AndroiddevTheme(dynamicColor = false) {
                SmartTodoApp(
                    uiState = uiState,
                    onSignalChange = smartTodoViewModel::updateSignal,
                    onQuickAddTask = { title, note ->
                        smartTodoViewModel.quickAddTask(title, note)
                        ReminderManager.rescheduleReminders(applicationContext, smartTodoViewModel.uiState.value.tasks)
                    },
                    onCreateTask = { task ->
                        smartTodoViewModel.addTask(task)
                        ReminderManager.rescheduleReminders(applicationContext, smartTodoViewModel.uiState.value.tasks)
                    },
                    onUpdateTask = { task ->
                        smartTodoViewModel.updateTask(task)
                        ReminderManager.rescheduleReminders(applicationContext, smartTodoViewModel.uiState.value.tasks)
                    },
                    onToggleTask = { task ->
                        smartTodoViewModel.toggleTaskCompletion(task)
                        ReminderManager.rescheduleReminders(applicationContext, smartTodoViewModel.uiState.value.tasks)
                    },
                    onDeleteTask = { task ->
                        smartTodoViewModel.deleteTask(task)
                        ReminderManager.rescheduleReminders(applicationContext, smartTodoViewModel.uiState.value.tasks)
                    }
                )
            }
        }
    }
}

// ViewModel 工厂功能：把本地仓库注入 SmartTodoViewModel。
private class SmartTodoViewModelFactory(
    private val repository: LocalSmartTodoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SmartTodoViewModel::class.java)) {
            return SmartTodoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
