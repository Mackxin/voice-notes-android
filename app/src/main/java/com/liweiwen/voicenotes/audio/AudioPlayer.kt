package com.liweiwen.voicenotes.audio

import android.media.MediaPlayer

/** 本地 WAV 播放器，同一时刻只播一条，支持倍速。 */
class AudioPlayer {
    private var player: MediaPlayer? = null
    var playingPath: String? = null
        private set

    /**
     * 播放本地音频。
     * @param speed 播放倍速，如 1.0 / 1.5 / 2.0。
     */
    fun play(path: String, speed: Float = 1.0f, onFinished: () -> Unit) {
        stop()
        try {
            player = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener {
                    stop()
                    onFinished()
                }
                prepare()
                runCatching { playbackParams = playbackParams.setSpeed(speed) }
                start()
            }
            playingPath = path
        } catch (_: Exception) {
            stop()
        }
    }

    fun stop() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
        playingPath = null
    }

    fun release() = stop()
}
