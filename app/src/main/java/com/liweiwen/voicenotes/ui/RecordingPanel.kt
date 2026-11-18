package com.liweiwen.voicenotes.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 新版录音面板：声纹光球 + 扩散涟漪 + 左右药丸按钮。
 * 手势判定仍在输入栏完成，这里只负责视觉反馈。
 */
@Composable
fun RecordingPanel(mode: SlideMode, liveText: String, elapsedSec: Int, dark: Boolean) {
    val transition = rememberInfiniteTransition(label = "recording")
    val ripple by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Restart),
        label = "ripple",
    )
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val cancelScale by animateFloatAsState(
        targetValue = if (mode == SlideMode.CANCEL) 1.12f else 1f,
        animationSpec = tween(180),
        label = "cancelScale",
    )
    val editScale by animateFloatAsState(
        targetValue = if (mode == SlideMode.EDIT) 1.12f else 1f,
        animationSpec = tween(180),
        label = "editScale",
    )

    val primary = if (dark) Color(0xFF62E6C8) else Color(0xFF00A88C)
    // 录音时几乎不透明，避免底层记录文字透上来干扰读数
    val scrim = if (dark) Color(0xF2070B14) else Color(0xF4F4F8FB)
    val titleColor = if (dark) Color(0xFFEAF6F3) else Color(0xFF163A36)
    val subColor = if (dark) Color(0xFF9FB3B0) else Color(0xFF5F7774)
    val cancelColor = if (mode == SlideMode.CANCEL) Color(0xFFFF6B6B) else subColor
    val editColor = if (mode == SlideMode.EDIT) primary else subColor

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrim),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 84.dp)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("正在录音", color = titleColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            if (liveText.isNotBlank()) {
                Text(
                    liveText,
                    color = titleColor,
                    fontSize = 17.sp,
                    lineHeight = 25.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (dark) Color(0x1AFFFFFF) else Color(0xFFFFFFFF))
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                )
            } else {
                Text("说出来，随手就记下", color = subColor, fontSize = 13.sp)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(240.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val px = 1.dp.toPx()

                // 柔和光晕
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = 118.dp.toPx(),
                    ),
                    radius = 118.dp.toPx(),
                    center = Offset(cx, cy),
                )

                // 三层扩散涟漪
                repeat(3) { index ->
                    val p = (ripple + index / 3f) % 1f
                    drawCircle(
                        color = primary.copy(alpha = (1f - p) * 0.28f),
                        radius = (64f + p * 58f) * px,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f * px),
                    )
                }

                // 环形声波：64 根短线，随相位起伏
                val bars = 64
                for (i in 0 until bars) {
                    val angle = i * (2 * PI / bars).toFloat() + phase * 0.16f
                    val amp = 6f + abs(sin(phase * 1.7f + i * 0.55f)) * 17f
                    val inner = 70f * px
                    val outer = inner + amp * px
                    drawLine(
                        color = primary.copy(alpha = 0.35f + abs(sin(phase + i * 0.3f)) * 0.5f),
                        start = Offset(cx + cos(angle) * inner, cy + sin(angle) * inner),
                        end = Offset(cx + cos(angle) * outer, cy + sin(angle) * outer),
                        strokeWidth = 2.6f * px,
                        cap = StrokeCap.Round,
                    )
                }

                // 内核
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.95f), primary.copy(alpha = 0.55f)),
                        center = Offset(cx, cy),
                        radius = 56.dp.toPx(),
                    ),
                    radius = 56.dp.toPx(),
                    center = Offset(cx, cy),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatElapsed(elapsedSec),
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    when (mode) {
                        SlideMode.CANCEL -> "松开取消"
                        SlideMode.EDIT -> "松开转文字"
                        SlideMode.CENTER -> "松手保存语音"
                    },
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 11.sp,
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 52.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionPill("取消", cancelColor, cancelScale)
            ActionPill("语音转文字", editColor, editScale)
        }
        Text(
            "左滑取消 · 右滑转文字 · 原地松手存语音",
            color = subColor,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp),
        )
    }
}

@Composable
private fun ActionPill(label: String, color: Color, scale: Float) {
    Box(
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(40.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.42f), RoundedCornerShape(40.dp))
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 六条声波竖线组成的图标（输入栏与空态复用）。 */
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
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun formatElapsed(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)
