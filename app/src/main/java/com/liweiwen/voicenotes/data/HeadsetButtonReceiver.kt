package com.liweiwen.voicenotes.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent

/**
 * 耳机线控：监听耳机/media 按键，按一下开始或停止录音。
 * 动态注册（在 MainActivity 里），只在 App 前台时生效，避免后台误触与权限问题。
 */
class HeadsetButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MEDIA_BUTTON) return
        val settings = SettingsStore(context.applicationContext)
        if (!settings.headsetControl) return
        val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return
        // 只认按下（UE 耳机线控常同时发 down/up），且是播放/暂停或耳机钩键
        if (event.action != KeyEvent.ACTION_DOWN) return
        if (
            event.keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
            event.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        ) {
            AppBus.emit(AppBus.TOGGLE_RECORD)
        }
    }
}
