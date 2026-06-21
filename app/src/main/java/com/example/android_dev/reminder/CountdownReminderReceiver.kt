package com.example.android_dev.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CountdownReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_COUNTDOWN_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "倒计时提醒"
        val note = intent.getStringExtra(EXTRA_NOTE) ?: ""
        NotificationHelper.showReminder(
            context = context,
            notificationId = id.hashCode(),
            title = "⏳ $title",
            body = if (note.isNotEmpty()) note else "目标日期就是今天！"
        )
    }

    companion object {
        const val EXTRA_COUNTDOWN_ID = "extra_countdown_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_NOTE = "extra_note"
    }
}