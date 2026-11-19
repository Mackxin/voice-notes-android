package com.liweiwen.voicenotes.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.liweiwen.voicenotes.MainActivity
import com.liweiwen.voicenotes.R

/**
 * 待办提醒：用 AlarmManager 在指定时间发一条通知。
 * 通过 noteId 区分每条记录的提醒，方便后续取消。
 */
object ReminderManager {
    const val CHANNEL_ID = "voice_notes_reminder"
    private const val EXTRA_NOTE_ID = "note_id"
    private const val EXTRA_TEXT = "note_text"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "随手说提醒",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "记录到点提醒" }
                nm.createNotificationChannel(channel)
            }
        }
    }

    /** 设置或取消一条提醒（reminderAt=0 取消）。 */
    fun schedule(context: Context, noteId: String, text: String, reminderAt: Long) {
        ensureChannel(context)
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, noteId, text)
        if (reminderAt <= 0L) {
            alarm.cancel(pi)
            return
        }
        // setAndAllowWhileIdle 在低电耗模式也能触发，不要求精确闹钟特殊权限
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pi)
            } else {
                alarm.set(AlarmManager.RTC_WAKEUP, reminderAt, pi)
            }
        }
    }

    fun cancel(context: Context, noteId: String, text: String) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pendingIntent(context, noteId, text))
    }

    private fun pendingIntent(context: Context, noteId: String, text: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_NOTE_ID, noteId)
            putExtra(EXTRA_TEXT, text)
        }
        val flag = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        // requestCode 用 id hashCode，保证一条记录只有一个闹钟
        return PendingIntent.getBroadcast(context, noteId.hashCode(), intent, flag)
    }
}

/** 到点弹出通知；点击通知打开 App。 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra("note_text").orEmpty().ifBlank { "该看看这条记录了" }
        ReminderManager.ensureChannel(context)

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flag = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pi = PendingIntent.getActivity(context, text.hashCode(), open, flag)

        val notification = NotificationCompat.Builder(context, ReminderManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("随手说 · 提醒")
            .setContentText(text.take(80))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        runCatching {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(text.hashCode() + 1000, notification)
        }
    }
}
