package com.liweiwen.voicenotes.ui

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * 微信式底部录音面板：
 * 中央大圆录音区 + 两侧沿圆弧分布的「月牙形」按钮「取消」「语音转文字」，
 * 与用户参考图一致：按钮呈弧形切片、文字倾斜贴在弧上。
 * 手指滑到哪个按钮，哪个按钮高亮。
 */
@Composable
fun RecordingPanel(mode: SlideMode, liveText: String, elapsedSec: Int, dark: Boolean) {
    val infinite = rememberInfiniteTransition(label = "recording")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(720, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse",
    )

    val scrim = if (dark) Color(0xD9000000) else Color(0x99000000)
    val panelBg = if (dark) Color(0xFF5A5A5A) else Color(0xFFB8B8B8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrim),
    ) {
        // 实时转写文字（居中显示）
        if (liveText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 36.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(if (dark) Color(0xFF2A2A2A) else Color(0xEEFFFFFF))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Text(
                    liveText,
                    color = if (dark) Color(0xFFECECEC) else Color(0xFF222222),
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // 底部弧形面板
        ArcRecordingPanel(
            mode = mode,
            elapsedSec = elapsedSec,
            panelBg = panelBg,
            pulse = pulse,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

/**
 * 底部弧形录音面板：中央半圆 + 左右两片月牙按钮。
 * 用 Canvas 绘制圆环扇形（外半径>内半径）得到月牙形，倾斜文字用原生 Canvas 旋转绘制。
 */
@Composable
private fun ArcRecordingPanel(
    mode: SlideMode,
    elapsedSec: Int,
    panelBg: Color,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val panelHeight = 272.dp
    val cancelActive = mode == SlideMode.CANCEL
    val editActive = mode == SlideMode.EDIT
    val inactiveBtn = Color(0xFF2C2C2C)
    val cancelColor = if (cancelActive) Color(0xFFE64340) else inactiveBtn
    val editColor = if (editActive) Color(0xFF07C160) else inactiveBtn

    Box(modifier = modifier.height(panelHeight)) {
        // 图形层：中央半圆 + 两片月牙
        Canvas(modifier = Modifier.fillMaxWidth().height(panelHeight)) {
            val w = size.width
            val h = size.height
            // 中央半圆：圆心在底边中央
            val cx = w / 2f
            val cy = h
            val radius = w.coerceAtMost(h * 1.5f) * 0.235f

            // 左月牙：更宽的圆环扇形，贴在半圆左上方
            drawArcSector(
                cx = cx, cy = cy,
                innerR = radius + 4f,
                outerR = radius + 108f,
                startDeg = 196f, sweepDeg = 60f,
                color = cancelColor,
            )
            // 右月牙：对称贴在右上方
            drawArcSector(
                cx = cx, cy = cy,
                innerR = radius + 4f,
                outerR = radius + 108f,
                startDeg = -56f, sweepDeg = 60f,
                color = editColor,
            )

            // 中央录音半圆（覆盖在月牙内侧之上）
            drawArc(
                color = panelBg,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
            )
        }

        // 月牙上的倾斜文字（原生 Canvas 旋转绘制，贴合弧面）
        Canvas(modifier = Modifier.fillMaxWidth().height(panelHeight)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h
            val radius = w.coerceAtMost(h * 1.5f) * 0.235f
            val textR = radius + 56f
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 42f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                // 左「取消」：月牙中点约 226°
                drawRotatedText(canvas, paint, "取消", cx, cy, textR, 226f, -26f)
                // 右「语音转文字」：单行，月牙中点约 -26°
                drawRotatedText(canvas, paint, "语音转文字", cx, cy, textR, -26f, 26f)
            }
        }

        // 中央半圆内容：波形 + 计时 + 提示
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WaveGlyph(
                color = Color.White,
                modifier = Modifier.scale(pulse).size(44.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = formatElapsed(elapsedSec),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when (mode) {
                    SlideMode.CANCEL -> "松开取消"
                    SlideMode.EDIT -> "松开后转成文字"
                    SlideMode.CENTER -> "松手转文字并保存"
                },
                color = Color(0xFFEEEEEE),
                fontSize = 12.sp,
            )
        }
    }
}

/** 绘制圆环扇形（月牙形）：外弧 - 内弧 围成。角度采用 Compose 约定（0°=右，顺时针为正，y 向下）。 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArcSector(
    cx: Float, cy: Float, innerR: Float, outerR: Float,
    startDeg: Float, sweepDeg: Float, color: Color,
) {
    val path = androidx.compose.ui.graphics.Path().apply {
        // 外弧起点
        val s = Math.toRadians(startDeg.toDouble())
        val e = Math.toRadians((startDeg + sweepDeg).toDouble())
        moveTo(cx + outerR * cos(s).toFloat(), cy + outerR * sin(s).toFloat())
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(cx - outerR, cy - outerR, cx + outerR, cy + outerR),
            startAngleDegrees = startDeg,
            sweepAngleDegrees = sweepDeg,
            forceMoveTo = false,
        )
        // 连到内弧终点并反向画回
        lineTo(cx + innerR * cos(e).toFloat(), cy + innerR * sin(e).toFloat())
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(cx - innerR, cy - innerR, cx + innerR, cy + innerR),
            startAngleDegrees = startDeg + sweepDeg,
            sweepAngleDegrees = -sweepDeg,
            forceMoveTo = false,
        )
        close()
    }
    drawPath(path, color)
}

/** 在极坐标半径 textR、角度 posAngleDeg 处绘制文字，文字按 rotateDeg 倾斜。 */
private fun drawRotatedText(
    canvas: androidx.compose.ui.graphics.Canvas,
    paint: android.graphics.Paint,
    text: String,
    cx: Float, cy: Float, textR: Float, posAngleDeg: Float, rotateDeg: Float,
) {
    val rad = Math.toRadians(posAngleDeg.toDouble())
    val x = cx + textR * cos(rad).toFloat()
    val y = cy + textR * sin(rad).toFloat()
    val native = canvas.nativeCanvas
    native.save()
    native.translate(x, y)
    native.rotate(rotateDeg)
    native.drawText(text, 0f, 0f, paint)
    native.restore()
}

/** 六条声波竖线组成的图标（不依赖图标库）。 */
@Composable
fun WaveGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bars = listOf(0.35f, 0.7f, 1f, 0.62f, 0.28f)
        val barW = w / (bars.size * 2f)
        bars.forEachIndexed { i, ratio ->
            val bx = barW * (2 * i + 1)
            val bh = h * ratio
            drawLine(
                color = color,
                start = Offset(bx, (h - bh) / 2),
                end = Offset(bx, (h + bh) / 2),
                strokeWidth = barW * 0.62f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

private fun formatElapsed(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)
