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
 *
 * v2.5 修复"设了不提醒"：
 * 1) 有精确闹钟权限时用 setExactAndAllowWhileIdle，避免低电耗下被大幅延后；
 * 2) 时间已过直接补发通知，不再静默丢弃；
 * 3) 提供 rescheduleAll，App 启动与开机后重建全部闹钟（系统可能已清除）。
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
                ).apply {
                    description = "记录到点提醒"
                    enableVibration(true)
                    setSound(
                        android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                }
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
        // 已过期：直接补发一条通知，避免用户"设了却不响"
        if (reminderAt <= System.currentTimeMillis()) {
            ReminderReceiver.showNotification(context, noteId, text)
            return
        }
        runCatching {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarm.canScheduleExactAlarms() ->
                    alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pi)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pi)
                else -> alarm.set(AlarmManager.RTC_WAKEUP, reminderAt, pi)
            }
        }
    }

    /**
     * 按本机记录重建所有未到期的提醒。
     * 系统升级、应用更新、用户清理后台都可能让已注册的闹钟丢失，
     * 只要记录里还留着 reminderAt，这里就能补回来。
     */
    fun rescheduleAll(context: Context, notes: List<Note>) {
        ensureChannel(context)
        val now = System.currentTimeMillis()
        notes.filter { !it.isTrashed && it.hasReminder && it.reminderAt > now }
            .forEach { schedule(context, it.id, it.text, it.reminderAt) }
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
        val noteId = intent.getStringExtra(EXTRA_ID).orEmpty()
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty().ifBlank { "该看看这条记录了" }
        showNotification(context, noteId, text)
    }

    companion object {
        private const val EXTRA_ID = "note_id"
        private const val EXTRA_TEXT = "note_text"

        fun showNotification(context: Context, noteId: String, text: String) {
            ReminderManager.ensureChannel(context)

            val open = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val flag = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val pi = PendingIntent.getActivity(context, noteId.hashCode(), open, flag)

            val notification = NotificationCompat.Builder(context, ReminderManager.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("随手说 · 提醒")
                .setContentText(text.take(80))
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build()

            runCatching {
                @Suppress("MissingPermission")
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .notify(noteId.hashCode() + 1000, notification)
            }
        }
    }
}

/** 开机后重建提醒：重启会清掉所有已注册的 AlarmManager 闹钟。 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action.orEmpty()
        if (action != Intent.ACTION_BOOT_COMPLETED && action != "android.intent.action.QUICKBOOT_POWERON") return
        runCatching {
            val appContext = context.applicationContext
            ReminderManager.rescheduleAll(appContext, NoteStore(appContext).all())
        }
    }
}
