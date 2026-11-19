package com.liweiwen.voicenotes

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.liweiwen.voicenotes.data.AppBus
import com.liweiwen.voicenotes.data.HeadsetButtonReceiver
import com.liweiwen.voicenotes.data.NoteStore
import com.liweiwen.voicenotes.data.SettingsStore
import com.liweiwen.voicenotes.ui.LockScreen
import com.liweiwen.voicenotes.ui.NoteScreen
import androidx.activity.ComponentActivity
import com.liweiwen.voicenotes.widget.NotesWidgetProvider

class MainActivity : ComponentActivity() {
    private val headsetReceiver = HeadsetButtonReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 小组件「录音」按钮：带 extra 打开，转发为开始录音事件
        if (intent.getBooleanExtra(NotesWidgetProvider.EXTRA_START_RECORD, false)) {
            AppBus.emit(AppBus.START_RECORD)
            intent.removeExtra(NotesWidgetProvider.EXTRA_START_RECORD)
        }
        // 耳机线控只响应媒体键，注册高优先级动态接收器
        runCatching {
            registerReceiver(headsetReceiver, IntentFilter(Intent.ACTION_MEDIA_BUTTON).apply {
                priority = 1000
            })
        }

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val noteStore = remember { NoteStore(context.applicationContext) }
            val settingsStore = remember { SettingsStore(context.applicationContext) }

            var unlocked by remember {
                mutableStateOf(!settingsStore.lockEnabled || settingsStore.lockPinHash.isBlank())
            }
            // App 退到后台再回来需重新解锁（隐私锁开启时）
            LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
                if (settingsStore.lockEnabled && settingsStore.lockPinHash.isNotBlank()) {
                    unlocked = false
                }
            }

            if (!unlocked) {
                LockScreen(settingsStore) { unlocked = true }
            } else {
                NoteScreen(noteStore, settingsStore)
            }
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(headsetReceiver) }
        super.onDestroy()
    }
}
