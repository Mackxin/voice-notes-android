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
 * v2.6 修复"设了还是不提醒"：
 * - v2.5 用 setExact/setAndAllowWhileIdle，在 Android 12+ 没给 SCHEDULE_EXACT_ALARM
 *   权限时会降级到 setAndAllowWhileIdle——锁屏进入 Doze 后会被系统延后甚至吞掉，
 *   实测闹钟到期 13 分钟都没触发。
 * - 改用 setAlarmClock：这是系统给闹钟类应用的专用接口，Doze 下也会准时唤醒，
 *   且【不需要】精确闹钟权限，是最可靠的一条路。状态栏会出现闹钟图标（符合预期）。
 * - 系统升级/重启/清后台会清掉闹钟，App 启动与开机时按本机记录全部重建。
 * - 到点时间已过但闹钟被吞的，启动时补发，不再静默丢弃。
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
        // 时间已过：直接补发一条通知，避免"设了却不响"
        if (reminderAt <= System.currentTimeMillis()) {
            ReminderReceiver.showNotification(context, noteId, text)
            return
        }
        runCatching {
            // 点击状态栏闹钟图标时打开 App 的意图
            val showIntent = PendingIntent.getActivity(
                context,
                noteId.hashCode() + 7777,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                pendingFlags(),
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // setAlarmClock：Doze 下Deliver 精确、无需特殊权限，系统可见的闹钟
                alarm.setAlarmClock(AlarmManager.AlarmClockInfo(reminderAt, showIntent), pi)
            } else {
                alarm.set(AlarmManager.RTC_WAKEUP, reminderAt, pi)
            }
        }
    }

    /**
     * 按本机记录重建所有提醒：未到期的重新注册，已到期但没触发的补发。
     * 系统升级、应用更新、用户清理后台、重启都可能让已注册的闹钟丢失。
     */
    fun rescheduleAll(context: Context, notes: List<Note>) {
        ensureChannel(context)
        val now = System.currentTimeMillis()
        notes.filter { !it.isTrashed && it.hasReminder }
            .forEach { schedule(context, it.id, it.text, it.reminderAt) }
    }

    fun cancel(context: Context, noteId: String, text: String) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pendingIntent(context, noteId, text))
    }

    private fun pendingFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    private fun pendingIntent(context: Context, noteId: String, text: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_NOTE_ID, noteId)
            putExtra(EXTRA_TEXT, text)
        }
        // requestCode 用 id hashCode，保证一条记录只有一个闹钟
        return PendingIntent.getBroadcast(context, noteId.hashCode(), intent, pendingFlags())
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
            // 提醒发出后清掉时间戳：一是气泡上的 ⏰ 徽标消失，二是避免下次启动
            // rescheduleAll 把这条已过期的提醒再补发一次（一次性提醒语义）。
            runCatching {
                if (noteId.isNotBlank()) {
                    NoteStore(context.applicationContext).setReminder(noteId, 0L)
                }
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
