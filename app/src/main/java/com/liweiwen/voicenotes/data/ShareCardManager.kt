package com.liweiwen.voicenotes.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 分享卡片：把单条记录画成一张竖版 PNG，写入公开「图片/随手说」，
 * 全机可见，可直接在系统相册/文件管理器里分享到微信。
 * 纯本机绘制，不联网、不上传。
 */
object ShareCardManager {
    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /** 生成卡片位图。宽 1080，高按正文行数自适应。 */
    fun render(context: Context, note: Note, accentHex: String = "#00A88C"): Bitmap {
        val width = 1080
        val padding = 84
        val accent = runCatching { Color.parseColor(accentHex) }.getOrDefault(Color.parseColor("#00A88C"))
        val density = context.resources.displayMetrics.density

        val bodyText = if (note.text == "[语音消息]") "🎙 语音消息 · ${note.effectiveDurationMs / 1000} 秒" else note.text
        val summaryText = if (note.summary.isNotBlank()) "✦ ${note.summary}" else ""

        // 先用同款 Paint 测量正文高度
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A2B28")
            textSize = 46f * density
        }
        val contentWidth = width - padding * 2
        val bodyLayout = StaticLayout.Builder.obtain(bodyText, 0, bodyText.length, bodyPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.32f)
            .setIncludePad(false)
            .build()

        val summaryLayout = if (summaryText.isNotBlank()) {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accent
                textSize = 32f * density
            }
            StaticLayout.Builder.obtain(summaryText, 0, summaryText.length, paint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.2f)
                .setIncludePad(false)
                .build()
        } else null

        var y = padding.toFloat()
        y += 70f * density // 顶部品牌行
        y += 24f * density
        y += bodyLayout.height.toFloat()
        if (summaryLayout != null) {
            y += 28f * density
            y += summaryLayout.height.toFloat()
        }
        if (note.tags.isNotEmpty()) y += 70f * density
        y += 40f * density
        y += 60f * density // 底部时间行
        y += padding.toFloat()
        val height = y.toInt().coerceAtLeast(720)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // 顶部强调色色带
        canvas.drawRect(0f, 0f, width.toFloat(), 16f * density, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })

        var cy = padding.toFloat()

        // 品牌行
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 34f * density
            isFakeBoldText = true
        }
        canvas.drawText("随手说", padding.toFloat(), cy + 40f * density, brandPaint)
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9AA8A4")
            textSize = 24f * density
        }
        canvas.drawText("随手记下的每一句", padding.toFloat(), cy + 78f * density, subPaint)
        cy += 70f * density + 24f * density

        // 正文
        canvas.save()
        canvas.translate(padding.toFloat(), cy)
        bodyLayout.draw(canvas)
        canvas.restore()
        cy += bodyLayout.height.toFloat()

        // 摘要
        if (summaryLayout != null) {
            cy += 28f * density
            canvas.save()
            canvas.translate(padding.toFloat(), cy)
            summaryLayout.draw(canvas)
            canvas.restore()
            cy += summaryLayout.height.toFloat()
        }

        // 标签胶囊
        if (note.tags.isNotEmpty()) {
            cy += 34f * density
            var x = padding.toFloat()
            val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E6F5F1") }
            val chipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accent
                textSize = 24f * density
            }
            note.tags.take(5).forEach { tag ->
                val label = "#$tag"
                val tw = chipTextPaint.measureText(label)
                val cw = tw + 36f * density
                if (x + cw > width - padding) return@forEach
                canvas.drawRoundRect(RectF(x, cy, x + cw, cy + 48f * density), 24f * density, 24f * density, chipPaint)
                canvas.drawText(label, x + 18f * density, cy + 33f * density, chipTextPaint)
                x += cw + 16f * density
            }
            cy += 70f * density
        }

        // 底部时间 + 标记
        cy += 20f * density
        val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9AA8A4")
            textSize = 24f * density
        }
        val marks = buildList {
            if (note.isPinned) add("置顶")
            if (note.isDone) add("已完成")
            if (note.hasReminder) add("有提醒")
        }
        val foot = timeFmt.format(Date(note.time)) + if (marks.isNotEmpty()) "  ·  ${marks.joinToString(" ")}" else ""
        canvas.drawText(foot, padding.toFloat(), cy + 40f * density, footPaint)

        return bitmap
    }

    /** 保存到「图片/随手说」，返回相对路径；失败返回 null。 */
    fun saveToGallery(context: Context, bitmap: Bitmap, displayName: String): String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/随手说")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching null
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: return@runCatching null
            "图片/随手说/$displayName.png"
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val folder = java.io.File(dir, "随手说").apply { mkdirs() }
            val file = java.io.File(folder, "$displayName.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            file.absolutePath
        }
    }.getOrNull()
}
