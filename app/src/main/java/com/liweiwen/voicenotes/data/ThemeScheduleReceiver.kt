package com.liweiwen.voicenotes.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * 深色模式定时切换：到点根据当前小时决定切深色还是浅色。
 * 只写设置值，界面下次进入/回到前台会读取应用（通过 AppBus 事件即时刷新）。
 */
class ThemeScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val settings = SettingsStore(context)
        if (!settings.themeScheduleEnabled) return
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val darkHour = settings.themeDarkHour
        val lightHour = settings.themeLightHour
        val shouldBeDark = isDarkAt(hour, darkHour, lightHour)
        val target = if (shouldBeDark) "dark" else "light"
        if (settings.theme != target) {
            settings.theme = target
            AppBus.emit(AppBus.THEME_REFRESH)
        }
    }

    companion object {
        const val ACTION = "com.liweiwen.voicenotes.THEME_SCHEDULE"
        const val EXTRA_HOUR = "hour"

        /** 判断当前小时是否应处于深色：darkHour 起进入深色，lightHour 起回到浅色，支持跨午夜。 */
        fun isDarkAt(hour: Int, darkHour: Int, lightHour: Int): Boolean {
            return if (darkHour == lightHour) {
                false
            } else if (darkHour < lightHour) {
                hour >= darkHour && hour < lightHour
            } else {
                // 跨午夜：darkHour..23 或 0..lightHour-1
                hour >= darkHour || hour < lightHour
            }
        }
    }
}
