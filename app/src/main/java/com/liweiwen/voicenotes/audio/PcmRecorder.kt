package com.liweiwen.voicenotes.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 从麦克风采集 16kHz / 16bit / 单声道 PCM，每约 100ms 回调一帧。
 * 这个格式与百炼 paraformer-realtime-v2 的 format=pcm、sample_rate=16000 一致。
 */
class PcmRecorder(private val onChunk: (ByteArray) -> Unit) {
    private var recorder: AudioRecord? = null
    private var scope: CoroutineScope? = null

    fun start(): Boolean {
        if (recorder != null) return true
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) return false

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, FRAME_BYTES),
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            return false
        }

        recorder = audioRecord
        audioRecord.startRecording()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { coroutineScope ->
            coroutineScope.launch {
                val buffer = ByteArray(FRAME_BYTES)
                while (isActive && recorder === audioRecord &&
                    audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING
                ) {
                    val size = audioRecord.read(buffer, 0, buffer.size)
                    if (size > 0) onChunk(buffer.copyOf(size))
                }
            }
        }
        return true
    }

    fun stop() {
        scope?.cancel()
        scope = null
        val audioRecord = recorder ?: return
        recorder = null
        try {
            if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop()
            }
        } catch (_: Exception) {
            // 录音已经停止时忽略
        }
        audioRecord.release()
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        const val FRAME_BYTES = 3_200 // 100ms * 16000 * 2 bytes
    }
}
