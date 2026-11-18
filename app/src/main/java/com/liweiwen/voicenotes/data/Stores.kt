package com.liweiwen.voicenotes.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * 一条记录。
 * - audioPath 指向本机 WAV 文件，durationMs 为语音时长。
 * - tags 为自定义标签（分类用）。
 * - deletedAt > 0 表示这条记录已移入回收站，值为移入时间戳；0 表示正常。
 */
data class Note(
    val id: String,
    val text: String,
    val time: Long,
    val fromVoice: Boolean,
    val audioPath: String? = null,
    val durationMs: Long = 0L,
    val tags: List<String> = emptyList(),
    val deletedAt: Long = 0L,
) {
    val hasAudio: Boolean get() = !audioPath.isNullOrBlank()
    val isTrashed: Boolean get() = deletedAt > 0L

    /**
     * 显示用时长。旧版本把秒当成毫秒写入 notes.json，导致记录全是 0:00；
     * 这里在 durationMs 无效时按 WAV 文件大小反推时长，保证老数据也能正确显示。
     */
    val effectiveDurationMs: Long
        get() {
            // WAV 文件大小是权威值：旧版把「秒」误写成「毫秒」，
            // 所以只要有语音文件就优先按文件大小反推，避免继续显示 0:00。
            val path = audioPath
            if (!path.isNullOrBlank()) {
                val fromFile = runCatching {
                    val pcmBytes = File(path).length() - WAV_HEADER_BYTES
                    if (pcmBytes <= 0L) 0L else pcmBytes * 1000L / (PCM_SAMPLE_RATE * PCM_BYTES_PER_FRAME)
                }.getOrDefault(0L)
                if (fromFile > 0L) return fromFile
            }
            return durationMs.coerceAtLeast(0L)
        }

    private companion object {
        const val WAV_HEADER_BYTES = 44L
        const val PCM_SAMPLE_RATE = 16_000L
        const val PCM_BYTES_PER_FRAME = 2L // 16bit 单声道
    }
}

/** 本地 JSON + WAV 文件存储：完全离线，不上传任何记录内容。 */
class NoteStore(context: Context) {
    private val file = File(context.filesDir, "notes.json")
    val audioDir = File(context.filesDir, "recordings").apply { mkdirs() }
    /** notes.json 所在目录，备份模块用它把元数据一起打包。 */
    val filesDir: File = context.filesDir
    private val notes = mutableListOf<Note>()

    /** 设置页展示用的本机存储路径 */
    val notesFilePath: String get() = file.absolutePath
    val recordingsDirPath: String get() = audioDir.absolutePath

    init {
        load()
        purgeExpiredTrash()
    }

    /** 正常记录（不含回收站）。 */
    fun all(): List<Note> = notes.filter { !it.isTrashed }

    /** 全部记录（含回收站），备份导出用。 */
    fun snapshot(): List<Note> = notes.toList()

    /** 回收站里的记录，按移入时间从新到旧。 */
    fun trash(): List<Note> = notes.filter { it.isTrashed }.sortedByDescending { it.deletedAt }

    fun add(
        text: String,
        fromVoice: Boolean,
        audioPath: String? = null,
        durationMs: Long = 0L,
        tags: List<String> = emptyList(),
    ): Note {
        // 纯语音记录用统一占位文案，界面再按 hasAudio 显示时长与播放键
        val finalText = if (text.isBlank() && !audioPath.isNullOrBlank()) "[语音消息]" else text.trim()
        val note = Note(
            id = UUID.randomUUID().toString(),
            text = finalText,
            time = System.currentTimeMillis(),
            fromVoice = fromVoice,
            audioPath = audioPath,
            durationMs = durationMs,
            tags = tags.distinct(),
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

    /** 设置一条记录的标签。 */
    fun setTags(id: String, tags: List<String>) {
        val index = notes.indexOfFirst { it.id == id }
        if (index < 0) return
        notes[index] = notes[index].copy(tags = tags.distinct().filter { it.isNotBlank() })
        save()
    }

    /** 所有用过的标签，按使用次数从多到少，供筛选栏与标签选择用。 */
    fun allTags(): List<String> = notes
        .filter { !it.isTrashed }
        .flatMap { it.tags }
        .groupingBy { it }
        .eachCount()
        .toList()
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
        .map { it.first }

    /** 移入回收站（不删语音文件，可恢复）。 */
    fun moveToTrash(id: String) {
        val index = notes.indexOfFirst { it.id == id && !it.isTrashed }
        if (index < 0) return
        notes[index] = notes[index].copy(deletedAt = System.currentTimeMillis())
        save()
    }

    fun moveManyToTrash(ids: Set<String>) {
        if (ids.isEmpty()) return
        val now = System.currentTimeMillis()
        var changed = false
        notes.forEachIndexed { i, note ->
            if (note.id in ids && !note.isTrashed) {
                notes[i] = note.copy(deletedAt = now)
                changed = true
            }
        }
        if (changed) save()
    }

    /** 从回收站恢复。 */
    fun restore(id: String) {
        val index = notes.indexOfFirst { it.id == id && it.isTrashed }
        if (index < 0) return
        notes[index] = notes[index].copy(deletedAt = 0L)
        save()
    }

    fun restoreMany(ids: Set<String>) {
        var changed = false
        notes.forEachIndexed { i, note ->
            if (note.id in ids && note.isTrashed) {
                notes[i] = note.copy(deletedAt = 0L)
                changed = true
            }
        }
        if (changed) save()
    }

    /** 彻底删除单条（连语音文件一起删）。 */
    fun delete(id: String) {
        val note = notes.firstOrNull { it.id == id } ?: return
        note.audioPath?.let { path -> runCatching { File(path).delete() } }
        notes.removeAll { it.id == id }
        save()
    }

    /** 批量彻底删除（多选模式的永久删除用）。 */
    fun deleteMany(ids: Set<String>) {
        if (ids.isEmpty()) return
        notes.filter { it.id in ids }.forEach { note ->
            note.audioPath?.let { path -> runCatching { File(path).delete() } }
        }
        notes.removeAll { it.id in ids }
        save()
    }

    /** 清空回收站。 */
    fun emptyTrash() {
        val trashed = notes.filter { it.isTrashed }
        if (trashed.isEmpty()) return
        deleteMany(trashed.map { it.id }.toSet())
    }

    /** 自动清理超过 30 天的回收站记录。 */
    private fun purgeExpiredTrash() {
        val cutoff = System.currentTimeMillis() - TRASH_TTL_MS
        val expired = notes.filter { it.isTrashed && it.deletedAt < cutoff }.map { it.id }.toSet()
        if (expired.isNotEmpty()) deleteMany(expired)
    }

    fun newAudioFile(): File = File(audioDir, "${UUID.randomUUID()}.wav")

    /** 数据管理统计：记录条数、语音文件数、语音占用字节数。 */
    fun stats(): StorageStats {
        val wavs = audioDir.listFiles { f -> f.isFile && f.name.endsWith(".wav", true) }
        val count = wavs?.size ?: 0
        val bytes = wavs?.sumOf { it.length() } ?: 0L
        return StorageStats(all().size, count, bytes)
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

    /**
     * 导入恢复：按 id 去重合并，已存在的记录保持不变。
     * incoming 的 audioPath 应已是本机有效路径（由 BackupManager 解压后重映射）。
     * 返回实际新增条数。
     */
    fun importNotes(incoming: List<Note>): Int {
        val existing = notes.map { it.id }.toSet()
        val fresh = incoming.filter { it.id !in existing }
        if (fresh.isEmpty()) return 0
        notes.addAll(fresh)
        save()
        return fresh.size
    }

    private fun load() {
        if (!file.exists()) return
        try {
            val array = JSONArray(file.readText())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val tags = mutableListOf<String>()
                obj.optJSONArray("tags")?.let { arr ->
                    for (j in 0 until arr.length()) {
                        arr.optString(j).takeIf { it.isNotBlank() }?.let { tags.add(it) }
                    }
                }
                val loaded = Note(
                    id = obj.getString("id"),
                    text = obj.getString("text"),
                    time = obj.getLong("time"),
                    fromVoice = obj.optBoolean("fromVoice"),
                    audioPath = obj.optString("audioPath").ifBlank { null },
                    durationMs = obj.optLong("durationMs"),
                    tags = tags.distinct(),
                    deletedAt = obj.optLong("deletedAt"),
                )
                // 老数据顺手修正一次时长（旧版把秒当成毫秒写进 JSON）
                notes.add(
                    if (loaded.hasAudio) {
                        loaded.copy(durationMs = loaded.effectiveDurationMs)
                    } else loaded,
                )
            }
        } catch (_: Exception) {
            // 文件损坏时从空列表开始
        }
    }

    private fun save() {
        val array = JSONArray()
        notes.forEach { note ->
            val tags = JSONArray()
            note.tags.forEach { tags.put(it) }
            array.put(
                JSONObject()
                    .put("id", note.id)
                    .put("text", note.text)
                    .put("time", note.time)
                    .put("fromVoice", note.fromVoice)
                    .put("audioPath", note.audioPath ?: "")
                    .put("durationMs", note.durationMs)
                    .put("tags", tags)
                    .put("deletedAt", note.deletedAt),
            )
        }
        file.writeText(array.toString())
    }

    companion object {
        /** 回收站保留 30 天。 */
        const val TRASH_TTL_MS = 30L * 24 * 60 * 60 * 1000
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

    /** 首页统计卡是否固定置顶（不随列表滚动）。默认置顶。 */
    var heroPinned: Boolean
        get() = prefs.getBoolean("hero_pinned", true)
        set(value) { prefs.edit().putBoolean("hero_pinned", value).apply() }

    /** 主题配色 key（mint/ocean/violet/sunset/rose/custom）。默认薄荷绿。 */
    var themeColor: String
        get() = prefs.getString("theme_color", "mint").orEmpty()
        set(value) { prefs.edit().putString("theme_color", value).apply() }

    /**
     * 自定义强调色，格式为 "#AARRGGBB" 或 "#RRGGBB"；空表示未设置。
     * 当 themeColor == "custom" 时用它生成整套配色。
     */
    var customAccent: String
        get() = prefs.getString("custom_accent", "").orEmpty()
        set(value) { prefs.edit().putString("custom_accent", value).apply() }

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
