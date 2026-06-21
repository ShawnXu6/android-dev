package com.example.android_dev.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.reminder.CountdownReminderReceiver
import com.example.android_dev.domain.Countdown
import java.time.ZoneId

// 提醒调度功能：用 AlarmManager 在任务的 reminderAt 时间触发本地通知。
class ReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // 调度功能：为带有未来提醒时间且未完成的任务设置精确闹钟。
    fun schedule(task: SmartTask) {
        val triggerAt = task.reminderAt ?: return
        if (task.isCompleted || triggerAt <= System.currentTimeMillis()) return

        val pendingIntent = buildPendingIntent(task) ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // 无精确闹钟权限时退化为非精确闹钟，保证仍会提醒。
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    // 取消功能：任务删除或完成时撤销已设置的提醒。
    fun cancel(task: SmartTask) {
        val pendingIntent = buildPendingIntent(task, mutableFlag = true) ?: return
        runCatching { alarmManager.cancel(pendingIntent) }
    }

    fun scheduleCountdownReminder(countdown: Countdown) {
        val targetDate = countdown.targetDate
        val triggerAt = targetDate.atStartOfDay(ZoneId.systemDefault())
            .plusHours(9)
            .toInstant()
            .toEpochMilli()
        if (triggerAt <= System.currentTimeMillis()) return

        val intent = Intent(appContext, CountdownReminderReceiver::class.java).apply {
            putExtra(CountdownReminderReceiver.EXTRA_COUNTDOWN_ID, countdown.id)
            putExtra(CountdownReminderReceiver.EXTRA_TITLE, countdown.title)
            putExtra(CountdownReminderReceiver.EXTRA_NOTE, countdown.note)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            countdown.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    fun cancelCountdownReminder(countdownId: String) {
        val intent = Intent(appContext, CountdownReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            countdownId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching { alarmManager.cancel(pendingIntent) }
    }

    // 全量重排功能：应用启动或用户切换时，按当前任务列表重建所有提醒。
    fun rescheduleAll(tasks: List<SmartTask>) {
        tasks.forEach { task ->
            if (task.reminderAt != null && !task.isCompleted) schedule(task) else cancel(task)
        }
    }

    private fun buildPendingIntent(task: SmartTask, mutableFlag: Boolean = false): PendingIntent? {
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, "任务提醒：${task.title}")
            putExtra(
                ReminderReceiver.EXTRA_BODY,
                task.description.ifBlank { "记得处理「${task.title}」" }
            )
        }
        val flags = if (mutableFlag) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(appContext, task.id.hashCode(), intent, flags)
    }

}
