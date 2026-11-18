package com.liweiwen.voicenotes.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * 用系统内置 TextToSpeech 朗读文字记录。
 * 不依赖网络、不需要额外权限；设备缺中文语音包时会通过 onReady 回调告知。
 */
class TtsSpeaker(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    var onReady: ((Boolean) -> Unit)? = null
    var onSpeakingChange: ((Boolean) -> Unit)? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.CHINA)
                    ready = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
                } else {
                    ready = false
                }
                onReady?.invoke(ready)
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { onSpeakingChange?.invoke(true) }
                override fun onDone(utteranceId: String?) { onSpeakingChange?.invoke(false) }
                override fun onError(utteranceId: String?) { onSpeakingChange?.invoke(false) }
            })
        } catch (_: Exception) {
            ready = false
        }
    }

    /** 朗读一段文字；语速 1.0 为正常。返回 false 表示当前不可用。 */
    fun speak(text: String, speed: Float = 1.0f): Boolean {
        val content = text.trim()
        if (content.isBlank() || !ready) return false
        tts?.setSpeechRate(speed)
        val params = Bundle().apply { putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC) }
        tts?.speak(content, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
        return true
    }

    fun stop() {
        tts?.stop()
        onSpeakingChange?.invoke(false)
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
        ready = false
    }

    private companion object {
        const val UTTERANCE_ID = "voice_note_read"
    }
}
