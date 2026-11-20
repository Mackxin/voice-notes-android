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
    // ===== v2.4 新增字段（全部带默认值，旧数据与备份包无缝兼容） =====
    val reminderAt: Long = 0L,      // 提醒时间戳，0 表示未设置
    val summary: String = "",       // AI 摘要（百炼生成）
    val isPrivate: Boolean = false, // 私密记录，锁屏时隐藏
    val latitude: Double = 0.0,     // 地理标记纬度
    val longitude: Double = 0.0,    // 地理标记经度
    val placeName: String = "",     // 位置名称（经纬度或反查地址）
    // ===== v2.7 新增字段（同样带默认值，旧数据与备份无缝兼容） =====
    val isPinned: Boolean = false,  // 置顶记录
    val isDone: Boolean = false,    // 待办完成标记
) {
    val hasAudio: Boolean get() = !audioPath.isNullOrBlank()
    val isTrashed: Boolean get() = deletedAt > 0L
    val hasReminder: Boolean get() = reminderAt > 0L
    val hasLocation: Boolean get() = placeName.isNotBlank() || (latitude != 0.0 && longitude != 0.0)

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

    /** 正常记录（不含回收站），按写入顺序。界面根据 isPinned 决定置顶展示。 */
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

    /** 设置提醒时间（0 表示取消提醒）。 */
    fun setReminder(id: String, reminderAt: Long) {
        val index = notes.indexOfFirst { it.id == id }
        if (index < 0) return
        notes[index] = notes[index].copy(reminderAt = reminderAt)
        save()
    }

    /** AI 摘要写入。 */
    fun setSummary(id: String, summary: String) {
        val index = notes.indexOfFirst { it.id == id }
        if (index < 0) return
        notes[index] = notes[index].copy(summary = summary.trim())
        save()
    }

    /** 切换私密标记。 */
    fun setPrivate(id: String, value: Boolean) {
        val index = notes.indexOfFirst { it.id == id }
        if (index < 0) return
        notes[index] = notes[index].copy(isPrivate = value)
        save()
    }

    /** 写入地理标记。 */
    fun setLocation(id: String, latitude: Double, longitude: Double, placeName: String) {
        val index = notes.indexOfFirst { it.id == id }
        if (index < 0) return
        notes[index] = notes[index].copy(latitude = latitude, longitude = longitude, placeName = placeName)
        save()
    }

    /** 切换置顶。 */
    fun setPinned(id: String, value: Boolean) {
        val index = notes.indexOfFirst { it.id == id }
        if (index < 0) return
        notes[index] = notes[index].copy(isPinned = value)
        save()
    }

    /** 切换待办完成。 */
    fun setDone(id: String, value: Boolean) {
        val index = notes.indexOfFirst { it.id == id }
        if (index < 0) return
        notes[index] = notes[index].copy(isDone = value)
        save()
    }

    /** 合并 AI 自动打出的标签（去重，不删除已有标签）。 */
    fun addTags(id: String, more: List<String>) {
        val index = notes.indexOfFirst { it.id == id }
        if (index < 0) return
        val merged = (notes[index].tags + more).distinct().filter { it.isNotBlank() }
        notes[index] = notes[index].copy(tags = merged)
        save()
    }

    /** 到期的提醒（reminderAt 在 now 之前、未删除），供启动与通知点击后刷新。 */
    fun dueReminders(now: Long): List<Note> = notes.filter { !it.isTrashed && it.hasReminder && it.reminderAt <= now }

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
                    reminderAt = obj.optLong("reminderAt"),
                    summary = obj.optString("summary"),
                    isPrivate = obj.optBoolean("isPrivate"),
                    latitude = obj.optDouble("latitude", 0.0),
                    longitude = obj.optDouble("longitude", 0.0),
                    placeName = obj.optString("placeName"),
                    isPinned = obj.optBoolean("isPinned"),
                    isDone = obj.optBoolean("isDone"),
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
                    .put("deletedAt", note.deletedAt)
                    .put("reminderAt", note.reminderAt)
                    .put("summary", note.summary)
                    .put("isPrivate", note.isPrivate)
                    .put("latitude", note.latitude)
                    .put("longitude", note.longitude)
                    .put("placeName", note.placeName)
                    .put("isPinned", note.isPinned)
                    .put("isDone", note.isDone),
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

    // ===== v2.4 新增设置 =====
    /** 摘要模型（对话类），默认 qwen-plus。 */
    var summaryModel: String
        get() = prefs.getString("summary_model", "qwen-plus").orEmpty()
        set(value) { prefs.edit().putString("summary_model", value).apply() }

    /** 定时备份开关。 */
    var autoBackup: Boolean
        get() = prefs.getBoolean("auto_backup", false)
        set(value) { prefs.edit().putBoolean("auto_backup", value).apply() }

    /** 上次定时备份时间戳。 */
    var lastBackupTime: Long
        get() = prefs.getLong("last_backup_time", 0L)
        set(value) { prefs.edit().putLong("last_backup_time", value).apply() }

    /** 耳机线控录音开关。 */
    var headsetControl: Boolean
        get() = prefs.getBoolean("headset_control", true)
        set(value) { prefs.edit().putBoolean("headset_control", value).apply() }

    /** 地理标记开关。 */
    var locationEnabled: Boolean
        get() = prefs.getBoolean("location_enabled", false)
        set(value) { prefs.edit().putBoolean("location_enabled", value).apply() }

    /** 隐私锁开关。 */
    var lockEnabled: Boolean
        get() = prefs.getBoolean("lock_enabled", false)
        set(value) { prefs.edit().putBoolean("lock_enabled", value).apply() }

    /** 隐私锁密码（SHA-256 十六进制），空表示未设置。 */
    var lockPinHash: String
        get() = prefs.getString("lock_pin_hash", "").orEmpty()
        set(value) { prefs.edit().putString("lock_pin_hash", value).apply() }

    /** 是否用系统生物识别（指纹/面容）解锁，需 lockEnabled。 */
    var lockBiometric: Boolean
        get() = prefs.getBoolean("lock_biometric", true)
        set(value) { prefs.edit().putBoolean("lock_biometric", value).apply() }

    /** 私密记录在列表里是否用遮罩隐藏内容。 */
    var hidePrivate: Boolean
        get() = prefs.getBoolean("hide_private", true)
        set(value) { prefs.edit().putBoolean("hide_private", value).apply() }

    // ===== v2.7 新增设置 =====
    /** 常用短语，多个用换行分隔，输入栏一键插入。 */
    var quickPhrases: String
        get() = prefs.getString("quick_phrases", "").orEmpty()
        set(value) { prefs.edit().putString("quick_phrases", value).apply() }

    /** 深色模式定时切换开关。 */
    var themeScheduleEnabled: Boolean
        get() = prefs.getBoolean("theme_schedule_enabled", false)
        set(value) { prefs.edit().putBoolean("theme_schedule_enabled", value).apply() }

    /** 定时切到深色的小时（0-23）。 */
    var themeDarkHour: Int
        get() = prefs.getInt("theme_dark_hour", 19)
        set(value) { prefs.edit().putInt("theme_dark_hour", value).apply() }

    /** 定时切回浅色的小时（0-23）。 */
    var themeLightHour: Int
        get() = prefs.getInt("theme_light_hour", 7)
        set(value) { prefs.edit().putInt("theme_light_hour", value).apply() }

    /** AI 摘要时顺带自动打标签开关。 */
    var autoTagEnabled: Boolean
        get() = prefs.getBoolean("auto_tag_enabled", true)
        set(value) { prefs.edit().putBoolean("auto_tag_enabled", value).apply() }

    companion object {
        const val DEFAULT_MODEL = "paraformer-realtime-v2"

        /** 简单的 SHA-256，避免明文存密码。 */
        fun sha256(input: String): String = runCatching {
            val bytes = java.security.MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        }.getOrDefault("")
    }
}
