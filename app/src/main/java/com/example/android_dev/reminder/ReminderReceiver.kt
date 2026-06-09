package com.example.android_dev.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// 提醒广播接收功能：闹钟触发时发送本地通知。
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "任务提醒"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "你有一个任务待处理"
        NotificationHelper.showReminder(
            context = context,
            notificationId = taskId.hashCode(),
            title = title,
            body = body
        )
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
    }
}
