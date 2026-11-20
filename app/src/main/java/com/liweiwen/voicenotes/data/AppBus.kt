package com.liweiwen.voicenotes.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 轻量全局事件总线：用于跨组件触发录音。
 * 桌面小组件、耳机线控在 App 外部/其他组件里发事件，主界面收集后执行。
 */
object AppBus {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun emit(event: String) {
        _events.tryEmit(event)
    }

    const val TOGGLE_RECORD = "toggle_record"
    const val START_RECORD = "start_record"
    const val STOP_RECORD = "stop_record"
    const val THEME_REFRESH = "theme_refresh"
}
