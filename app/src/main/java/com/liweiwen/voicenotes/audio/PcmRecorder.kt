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
import java.io.File

/**
 * 麦克风采集 16kHz / 16bit / 单声道 PCM。
 * - 每帧回调给 ASR 流式发送
 * - 同一帧顺带写入 WAV 文件，实现「语音文件本地保存」
 */
class PcmRecorder(
    private val onChunk: (ByteArray) -> Unit,
    wavFile: File? = null,
) {
    private val writer = wavFile?.let { WavWriter(it, SAMPLE_RATE) }
    private var recorder: AudioRecord? = null
    private var scope: CoroutineScope? = null

    /** 本次录音时长（毫秒），stop() 后可读。 */
    var durationMs: Long = 0L
        private set

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

        writer?.open()
        recorder = audioRecord
        audioRecord.startRecording()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { coroutineScope ->
            coroutineScope.launch {
                val buffer = ByteArray(FRAME_BYTES)
                while (isActive && recorder === audioRecord &&
                    audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING
                ) {
                    val size = audioRecord.read(buffer, 0, buffer.size)
                    if (size > 0) {
                        val chunk = buffer.copyOf(size)
                        writer?.write(chunk)
                        onChunk(chunk)
                    }
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
            // 已停止则忽略
        }
        audioRecord.release()
        durationMs = writer?.close() ?: 0L
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        const val FRAME_BYTES = 3_200 // 100ms * 16000 * 2 bytes
    }
}
