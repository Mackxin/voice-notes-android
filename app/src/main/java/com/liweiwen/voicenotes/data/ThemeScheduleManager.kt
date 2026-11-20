package com.liweiwen.voicenotes.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/** 深色模式定时切换的闹钟调度：每小时整点检查一次是否该切换主题。 */
object ThemeScheduleManager {
    fun reschedule(context: Context, settings: SettingsStore) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(context)
        if (!settings.themeScheduleEnabled) {
            am.cancel(pi)
            return
        }
        val now = Calendar.getInstance()
        val first = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // 每小时重复，整点触发；App 未启动时 receiver 仍能写设置
        am.setRepeating(AlarmManager.RTC, first.timeInMillis, AlarmManager.INTERVAL_HOUR, pi)
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(context))
    }

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, ThemeScheduleReceiver::class.java).apply { action = ThemeScheduleReceiver.ACTION }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, 9101, intent, flags)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun unused(now: Calendar) = Unit
}
