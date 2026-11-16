package com.liweiwen.voicenotes.audio

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 把录音 PCM 数据封装成标准 WAV 文件（44 字节头 + 数据块）。
 * 只写 App 私有目录，不需要存储权限。
 */
class WavWriter(private val file: File, private val sampleRate: Int) {
    private var out: RandomAccessFile? = null
    private var dataSize = 0

    fun open() {
        file.parentFile?.mkdirs()
        if (file.exists()) file.delete()
        val raf = RandomAccessFile(file, "rw")
        raf.setLength(HEADER_SIZE.toLong()) // 先占 44 字节头的位置
        raf.seek(HEADER_SIZE.toLong())
        out = raf
        dataSize = 0
    }

    fun write(pcm: ByteArray) {
        val raf = out ?: return
        raf.write(pcm)
        dataSize += pcm.size
    }

    fun close(): Long {
        val raf = out ?: return 0L
        out = null
        return try {
            raf.seek(0)
            raf.write(buildHeader(dataSize))
            // 返回毫秒：字节数 / 每秒字节数得到秒，再乘 1000。
            // 之前漏乘导致所有记录都显示 0:00。
            dataSize.toLong() * 1000L / (sampleRate * BYTES_PER_SAMPLE)
        } finally {
            raf.close()
        }
    }

    private fun buildHeader(size: Int): ByteArray {
        val byteRate = sampleRate * BYTES_PER_SAMPLE
        return ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray(Charsets.US_ASCII))
            .putInt(36 + size)
            .put("WAVE".toByteArray(Charsets.US_ASCII))
            .put("fmt ".toByteArray(Charsets.US_ASCII))
            .putInt(16) // PCM fmt chunk 长度
            .putShort(1.toShort()) // 编码 = PCM
            .putShort(1.toShort()) // 单声道
            .putInt(sampleRate)
            .putInt(byteRate)
            .putShort(BYTES_PER_SAMPLE.toShort()) // block align
            .putShort(16.toShort()) // 位深
            .put("data".toByteArray(Charsets.US_ASCII))
            .putInt(size)
            .array()
    }

    companion object {
        private const val HEADER_SIZE = 44
        private const val BYTES_PER_SAMPLE = 2 // 16bit 单声道
    }
}
