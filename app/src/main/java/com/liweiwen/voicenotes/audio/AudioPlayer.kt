package com.liweiwen.voicenotes.audio

import android.media.MediaPlayer

/** 本地 WAV 播放器，同一时刻只播一条。 */
class AudioPlayer {
    private var player: MediaPlayer? = null
    var playingPath: String? = null
        private set

    fun play(path: String, onFinished: () -> Unit) {
        stop()
        try {
            player = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener {
                    stop()
                    onFinished()
                }
                prepare()
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
