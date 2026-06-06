package com.example.android_dev.engine

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.android_dev.R
import com.example.android_dev.domain.SmartTask
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("taskId")
        val taskTitle = intent.getStringExtra("taskTitle")
        
        if (taskTitle != null) {
            showNotification(context, taskId ?: "", taskTitle)
        }
    }
    
    private fun showNotification(context: Context, taskId: String, taskTitle: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "todo_reminder",
                "任务提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "智能待办任务提醒"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val builder = NotificationCompat.Builder(context, "todo_reminder")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("任务提醒")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        
        notificationManager.notify(taskId.hashCode(), builder.build())
    }
}

object ReminderManager {
    fun setReminder(context: Context, task: SmartTask) {
        val reminderMinutes = task.reminderTime ?: return
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, task.targetHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            add(Calendar.MINUTE, -reminderMinutes)
        }
        
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("taskId", task.id)
            putExtra("taskTitle", task.title)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
    
    fun cancelReminder(context: Context, task: SmartTask) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
    
    fun rescheduleReminders(context: Context, tasks: List<SmartTask>) {
        tasks.forEach { task ->
            if (!task.isCompleted && task.reminderTime != null) {
                setReminder(context, task)
            } else {
                cancelReminder(context, task)
            }
        }
    }
}
