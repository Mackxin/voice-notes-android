package com.liweiwen.voicenotes.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** 一条笔记。fromVoice=true 表示由语音转写生成。 */
data class Note(
    val id: String,
    val text: String,
    val time: Long,
    val fromVoice: Boolean,
)

/** 本地 JSON 文件存储：纯离线，不联网上传任何笔记内容。 */
class NoteStore(context: Context) {
    private val file = File(context.filesDir, "notes.json")
    private val notes = mutableListOf<Note>()

    init {
        load()
    }

    fun all(): List<Note> = notes.toList()

    fun add(text: String, fromVoice: Boolean): Note {
        val note = Note(UUID.randomUUID().toString(), text, System.currentTimeMillis(), fromVoice)
        notes.add(note)
        save()
        return note
    }

    fun delete(id: String) {
        notes.removeAll { it.id == id }
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
                    ),
                )
            }
        } catch (_: Exception) {
            // 文件损坏时从空列表开始，避免整个 App 起不来
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
                    .put("fromVoice", note.fromVoice),
            )
        }
        file.writeText(array.toString())
    }
}

/** API Key 等业务空间配置只写入本机 SharedPreferences。 */
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

    /**
     * 未填业务空间 ID 走兼容域名（只需 API Key）；
     * 填了则走百炼推荐的空间专属域名。
     */
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
