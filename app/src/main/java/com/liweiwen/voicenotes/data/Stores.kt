package com.liweiwen.voicenotes.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** 一条记录。audioPath 指向本地 WAV 文件，durationMs 为语音时长。 */
data class Note(
    val id: String,
    val text: String,
    val time: Long,
    val fromVoice: Boolean,
    val audioPath: String? = null,
    val durationMs: Long = 0L,
) {
    val hasAudio: Boolean get() = !audioPath.isNullOrBlank()
}

/** 本地 JSON + WAV 文件存储：完全离线，不上传任何记录内容。 */
class NoteStore(context: Context) {
    private val file = File(context.filesDir, "notes.json")
    val audioDir = File(context.filesDir, "recordings").apply { mkdirs() }
    private val notes = mutableListOf<Note>()

    init {
        load()
    }

    fun all(): List<Note> = notes.toList()

    fun add(
        text: String,
        fromVoice: Boolean,
        audioPath: String? = null,
        durationMs: Long = 0L,
    ): Note {
        val note = Note(
            id = UUID.randomUUID().toString(),
            text = text,
            time = System.currentTimeMillis(),
            fromVoice = fromVoice,
            audioPath = audioPath,
            durationMs = durationMs,
        )
        notes.add(note)
        save()
        return note
    }

    /** 再次编辑：只改文字，保留语音文件与原始时间。 */
    fun updateText(id: String, text: String) {
        val index = notes.indexOfFirst { it.id == id }
        if (index < 0) return
        notes[index] = notes[index].copy(text = text)
        save()
    }

    /** 删除记录，同时删除对应语音文件。 */
    fun delete(id: String) {
        val note = notes.firstOrNull { it.id == id } ?: return
        note.audioPath?.let { path -> runCatching { File(path).delete() } }
        notes.removeAll { it.id == id }
        save()
    }

    fun newAudioFile(): File = File(audioDir, "${UUID.randomUUID()}.wav")

    /** 数据管理统计：记录条数、语音文件数、语音占用字节数。 */
    fun stats(): StorageStats {
        val wavs = audioDir.listFiles { f -> f.isFile && f.name.endsWith(".wav", true) }
        val count = wavs?.size ?: 0
        val bytes = wavs?.sumOf { it.length() } ?: 0L
        return StorageStats(notes.size, count, bytes)
    }

    /** 清空全部记录及其语音文件（不可撤销）。 */
    fun clearAll() {
        audioDir.listFiles { f -> f.isFile }?.forEach { runCatching { it.delete() } }
        notes.clear()
        save()
    }

    /** 只删除所有语音文件但保留文字记录（不可撤销）。 */
    fun clearAudioOnly() {
        audioDir.listFiles { f -> f.isFile }?.forEach { runCatching { it.delete() } }
        notes.forEachIndexed { i, note ->
            if (note.hasAudio) notes[i] = note.copy(audioPath = null, durationMs = 0L)
        }
        save()
    }

    private fun load() {
        if (!file.exists()) return
        try {
            val array = JSONArray(file.readText())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                notes.add(
                    Note(
                        id = obj.getString("id"),
                        text = obj.getString("text"),
                        time = obj.getLong("time"),
                        fromVoice = obj.optBoolean("fromVoice"),
                        audioPath = obj.optString("audioPath").ifBlank { null },
                        durationMs = obj.optLong("durationMs"),
                    ),
                )
            }
        } catch (_: Exception) {
            // 文件损坏时从空列表开始
        }
    }

    private fun save() {
        val array = JSONArray()
        notes.forEach { note ->
            array.put(
                JSONObject()
                    .put("id", note.id)
                    .put("text", note.text)
                    .put("time", note.time)
                    .put("fromVoice", note.fromVoice)
                    .put("audioPath", note.audioPath ?: "")
                    .put("durationMs", note.durationMs),
            )
        }
        file.writeText(array.toString())
    }
}

/** 数据管理统计结果。 */
data class StorageStats(val noteCount: Int, val audioCount: Int, val audioBytes: Long)

/** 设置只写入本机 SharedPreferences。 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("voice_notes_settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString("api_key", "").orEmpty()
        set(value) { prefs.edit().putString("api_key", value).apply() }

    var workspaceId: String
        get() = prefs.getString("workspace_id", "").orEmpty()
        set(value) { prefs.edit().putString("workspace_id", value).apply() }

    var model: String
        get() = prefs.getString("model", DEFAULT_MODEL).orEmpty()
        set(value) { prefs.edit().putString("model", value).apply() }

    /** dark=深色 / light=浅色 / system=跟随系统 */
    var theme: String
        get() = prefs.getString("theme", "dark").orEmpty()
        set(value) { prefs.edit().putString("theme", value).apply() }

    val wsUrl: String
        get() = if (workspaceId.isBlank()) {
            "wss://dashscope.aliyuncs.com/api-ws/v1/inference"
        } else {
            "wss://${workspaceId}.cn-beijing.maas.aliyuncs.com/api-ws/v1/inference"
        }

    companion object {
        const val DEFAULT_MODEL = "paraformer-realtime-v2"
    }
}
