package com.liweiwen.voicenotes.data

import android.content.Context
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 文字导出：把全部记录生成 Markdown / CSV，写到公开「下载/随手说」目录，
 * 用户用系统文件管理器即可找到（区别于 App 私有目录）。
 */
object ExportManager {
    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun markdown(notes: List<Note>): String {
        val sb = StringBuilder()
        sb.appendLine("# 随手说 · 记录导出")
        sb.appendLine()
        sb.appendLine("> 共 ${notes.size} 条 · 导出于 ${timeFmt.format(Date())}")
        sb.appendLine()
        notes.forEach { note ->
            sb.appendLine("## ${timeFmt.format(Date(note.time))}")
            if (note.isPrivate) sb.appendLine("> 🔒 私密")
            val body = if (note.text == "[语音消息]") "（语音消息，${note.effectiveDurationMs / 1000} 秒）" else note.text
            sb.appendLine(body)
            if (note.summary.isNotBlank()) sb.appendLine("\n**摘要**：${note.summary}")
            if (note.tags.isNotEmpty()) sb.appendLine("\n标签：${note.tags.joinToString(" ") { "#$it" }}")
            if (note.hasLocation) sb.appendLine("位置：${note.placeName}")
            if (note.hasReminder) sb.appendLine("提醒：${timeFmt.format(Date(note.reminderAt))}")
            sb.appendLine()
            sb.appendLine("---")
            sb.appendLine()
        }
        return sb.toString()
    }

    fun csv(notes: List<Note>): String {
        fun esc(value: String) = "\"${value.replace("\"", "\"\"").replace("\n", " ")}\""
        val sb = StringBuilder()
        sb.appendLine("时间,类型,内容,摘要,标签,位置,提醒,私密")
        notes.forEach { note ->
            val type = when {
                note.hasAudio && note.text == "[语音消息]" -> "语音"
                note.hasAudio -> "语音转文字"
                else -> "文字"
            }
            sb.append(listOf(
                esc(timeFmt.format(Date(note.time))),
                esc(type),
                esc(note.text),
                esc(note.summary),
                esc(note.tags.joinToString(" ")),
                esc(note.placeName),
                if (note.hasReminder) esc(timeFmt.format(Date(note.reminderAt))) else esc(""),
                if (note.isPrivate) "是" else "否",
            ).joinToString(","))
            sb.appendLine()
        }
        return sb.toString()
    }

    /** 写入「下载/随手说」，返回用户可见的相对路径；失败返回 null。 */
    fun writeToDownloads(context: Context, displayName: String, mime: String, content: String): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/随手说")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            } ?: return null
            return "下载/随手说/$displayName"
        } else {
            val dir = android.os.Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS,
            )
            val folder = java.io.File(dir, "随手说").apply { mkdirs() }
            val file = java.io.File(folder, displayName)
            file.writeText(content, Charsets.UTF_8)
            return file.absolutePath
        }
    }

    fun timestamp(): String = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
}
