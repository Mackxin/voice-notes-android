package com.liweiwen.voicenotes.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** 备份导出/导入结果。 */
data class BackupResult(val noteCount: Int, val audioCount: Int, val settingsRestored: Boolean)

/**
 * 备份模块：把记录、设置与本机 WAV 语音打包成一个 ZIP 文件。
 * 全程离线，只通过系统文件选择器读写用户指定的位置，不联网、不上传。
 */
object BackupManager {
    private const val NOTES_ENTRY = "notes.json"
    private const val SETTINGS_ENTRY = "settings.json"
    private const val AUDIO_DIR = "recordings/"

    /** 导出到用户选择的 Uri（CreateDocument 返回的 URI）。 */
    fun export(context: Context, uri: Uri, noteStore: NoteStore, settingsStore: SettingsStore): BackupResult {
        val notes = noteStore.snapshot()
        val usedAudio = notes.mapNotNull { it.audioPath }.map { File(it) }.filter { it.exists() }.distinctBy { it.absolutePath }
        val out = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("无法写入所选位置")
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            zip.putNextEntry(ZipEntry(NOTES_ENTRY))
            zip.write(buildNotesJson(notes).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(SETTINGS_ENTRY))
            zip.write(buildSettingsJson(settingsStore).toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            usedAudio.forEach { wav ->
                zip.putNextEntry(ZipEntry(AUDIO_DIR + wav.name))
                wav.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return BackupResult(notes.size, usedAudio.size, true)
    }

    /**
     * 从用户选择的 ZIP 导入恢复。
     * - 按记录 id 去重合并，不覆盖现有数据。
     * - 语音文件复制到本机录音目录后重映射路径。
     * - 设置项一并恢复（主题、外观、API 配置等）。
     */
    fun import(context: Context, uri: Uri, noteStore: NoteStore, settingsStore: SettingsStore): BackupResult {
        val tmp = File(context.cacheDir, "backup_import_${System.currentTimeMillis()}")
        tmp.mkdirs()
        var audioCount = 0
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("无法读取所选文件")
            ZipInputStream(input).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    // 防目录穿越：只接受相对路径，且不含 ".."
                    if (!entry.isDirectory && !name.startsWith("/") && !name.split('/').contains("..")) {
                        val target = File(tmp, name)
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val notesFile = File(tmp, NOTES_ENTRY)
            if (!notesFile.exists()) throw IllegalStateException("备份包缺少 notes.json，可能不是随手说的备份文件")
            val array = JSONArray(notesFile.readText(Charsets.UTF_8))
            // 已有记录（含回收站）直接跳过，连音频都不复制，避免产生没有记录引用的孤儿 WAV
            val existingIds = noteStore.snapshot().map { it.id }.toSet()
            val incoming = mutableListOf<Note>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("id") in existingIds) continue
                val tags = mutableListOf<String>()
                obj.optJSONArray("tags")?.let { arr ->
                    for (j in 0 until arr.length()) arr.optString(j).takeIf { it.isNotBlank() }?.let { tags.add(it) }
                }
                // 备份里存的是相对文件名，导入时复制回本机录音目录并改写路径
                val relAudio = obj.optString("audioPath").ifBlank { null }
                val localAudio = relAudio?.let { name ->
                    val src = File(tmp, AUDIO_DIR + File(name).name)
                    if (src.exists()) {
                        val dst = noteStore.newAudioFile()
                        src.copyTo(dst, overwrite = true)
                        audioCount++
                        dst.absolutePath
                    } else null
                }
                incoming.add(
                    Note(
                        id = obj.getString("id"),
                        text = obj.getString("text"),
                        time = obj.getLong("time"),
                        fromVoice = obj.optBoolean("fromVoice"),
                        audioPath = localAudio,
                        durationMs = obj.optLong("durationMs"),
                        tags = tags.distinct(),
                        deletedAt = obj.optLong("deletedAt"),
                    ),
                )
            }
            val added = noteStore.importNotes(incoming)

            var settingsRestored = false
            val settingsFile = File(tmp, SETTINGS_ENTRY)
            if (settingsFile.exists()) {
                val s = JSONObject(settingsFile.readText(Charsets.UTF_8))
                settingsStore.apiKey = s.optString("api_key", settingsStore.apiKey)
                settingsStore.workspaceId = s.optString("workspace_id", settingsStore.workspaceId)
                settingsStore.model = s.optString("model", settingsStore.model)
                settingsStore.theme = s.optString("theme", settingsStore.theme)
                settingsStore.themeColor = s.optString("theme_color", settingsStore.themeColor)
                settingsStore.customAccent = s.optString("custom_accent", settingsStore.customAccent)
                settingsStore.heroPinned = s.optBoolean("hero_pinned", settingsStore.heroPinned)
                settingsRestored = true
            }
            return BackupResult(added, audioCount, settingsRestored)
        } finally {
            tmp.deleteRecursively()
        }
    }

    private fun buildNotesJson(notes: List<Note>): String {
        val root = JSONArray()
        notes.forEach { note ->
            val tags = JSONArray()
            note.tags.forEach { tags.put(it) }
            root.put(
                JSONObject()
                    .put("id", note.id)
                    .put("text", note.text)
                    .put("time", note.time)
                    .put("fromVoice", note.fromVoice)
                    // 备份里只放文件名，避免绝对路径在换机后失效
                    .put("audioPath", note.audioPath?.let { File(it).name }.orEmpty())
                    .put("durationMs", note.durationMs)
                    .put("tags", tags)
                    .put("deletedAt", note.deletedAt),
            )
        }
        return root.toString()
    }

    private fun buildSettingsJson(s: SettingsStore): JSONObject = JSONObject()
        .put("api_key", s.apiKey)
        .put("workspace_id", s.workspaceId)
        .put("model", s.model)
        .put("theme", s.theme)
        .put("theme_color", s.themeColor)
        .put("custom_accent", s.customAccent)
        .put("hero_pinned", s.heroPinned)
}
