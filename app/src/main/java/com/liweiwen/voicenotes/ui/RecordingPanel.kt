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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 录音面板：声纹光球 + 扩散涟漪 + 左右"跟手"操作块。
 * 手势判定在输入栏完成，这里根据 slideDx / slideMode 做视觉反馈：
 * 光球跟手位移、落入左/右操作区时变色，操作块放大吸附并显示填充进度。
 */
@Composable
fun RecordingPanel(
    mode: SlideMode,
    inputMode: InputMode,
    liveText: String,
    elapsedSec: Int,
    dark: Boolean,
    slideDx: Float = 0f,
    slideThreshold: Float = 1f,
) {
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

    val primary = if (dark) Color(0xFF62E6C8) else Color(0xFF00A88C)
    val danger = Color(0xFFFF6B6B)
    val scrim = if (dark) Color(0xF2070B14) else Color(0xF4F4F8FB)
    val titleColor = if (dark) Color(0xFFEAF6F3) else Color(0xFF163A36)
    val subColor = if (dark) Color(0xFF9FB3B0) else Color(0xFF5F7774)

    // 归一化进度：-1（完全左滑取消）~ 0 ~ +1（完全右滑转文字）
    val targetProgress = (slideDx / slideThreshold).coerceIn(-1f, 1f)
    val progress by animateFloatAsState(targetProgress, animationSpec = tween(80), label = "slideProgress")
    val cancelActive = mode == SlideMode.CANCEL
    val editActive = mode == SlideMode.EDIT
    // 光球当前主色：落区时切换为对应操作色
    val orbColor = when {
        cancelActive -> danger
        editActive -> primary
        else -> primary
    }
    // 光球跟手位移（限制在 ±56dp），用 dp 偏移
    val orbOffsetDp = (progress * 56f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrim),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 76.dp)
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

        // 中心光球：跟手位移 + 落区变色
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = orbOffsetDp.dp)
                .size(240.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val px = 1.dp.toPx()

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orbColor.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = 118.dp.toPx(),
                    ),
                    radius = 118.dp.toPx(),
                    center = Offset(cx, cy),
                )
                repeat(3) { index ->
                    val p = (ripple + index / 3f) % 1f
                    drawCircle(
                        color = orbColor.copy(alpha = (1f - p) * 0.28f),
                        radius = (64f + p * 58f) * px,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f * px),
                    )
                }
                val bars = 64
                for (i in 0 until bars) {
                    val angle = i * (2 * PI / bars).toFloat() + phase * 0.16f
                    val amp = 6f + abs(sin(phase * 1.7f + i * 0.55f)) * 17f
                    val inner = 70f * px
                    val outer = inner + amp * px
                    drawLine(
                        color = orbColor.copy(alpha = 0.35f + abs(sin(phase + i * 0.3f)) * 0.5f),
                        start = Offset(cx + cos(angle) * inner, cy + sin(angle) * inner),
                        end = Offset(cx + cos(angle) * outer, cy + sin(angle) * outer),
                        strokeWidth = 2.6f * px,
                        cap = StrokeCap.Round,
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orbColor.copy(alpha = 0.95f), orbColor.copy(alpha = 0.55f)),
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
                        SlideMode.CENTER -> if (inputMode == InputMode.VOICE) "松手保存语音" else "松手转成文字"
                    },
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                )
            }
        }

        // 底部左右操作块：色块填充 + 放大吸附 + 图标，随滑动进度连续变化
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionBlock(
                label = "取消",
                active = cancelActive,
                intensity = (-progress).coerceIn(0f, 1f),
                color = danger,
                isCancel = true,
                dark = dark,
            )
            // 中间留空（不要横线），仅用左右操作块的填充与放大反馈滑动落点
            Spacer(Modifier.weight(1f))
            ActionBlock(
                label = if (inputMode == InputMode.VOICE) "转文字" else "转文字编辑",
                active = editActive,
                intensity = progress.coerceIn(0f, 1f),
                color = primary,
                isCancel = false,
                dark = dark,
            )
        }
        Text(
            if (inputMode == InputMode.VOICE) {
                "左滑取消 · 右滑转文字 · 原地松手存语音"
            } else {
                "左滑取消 · 右滑转文字可编辑 · 原地松手转文字"
            },
            color = subColor,
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
        )
    }
}

/** 左右操作块：随滑动强度放大、色块填充加深、图标放大，形成吸附操作感。 */
@Composable
private fun ActionBlock(
    label: String,
    active: Boolean,
    intensity: Float,
    color: Color,
    isCancel: Boolean,
    dark: Boolean,
) {
    val scale by animateFloatAsState(
        if (active) 1.12f else 1f + intensity * 0.08f,
        animationSpec = tween(140),
        label = "blockScale",
    )
    val fillAlpha = 0.10f + intensity * 0.26f
    val borderAlpha = 0.35f + intensity * 0.55f
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(RoundedCornerShape(22.dp))
                .background(color.copy(alpha = fillAlpha))
                .border(2.dp, color.copy(alpha = borderAlpha), RoundedCornerShape(22.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isCancel) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "取消",
                    tint = color,
                    modifier = Modifier.size((26 + intensity * 8).dp),
                )
            } else {
                // 转文字：声波图标（复用 WaveGlyph）
                WaveGlyph(
                    color = color,
                    modifier = Modifier.size((30 + intensity * 8).dp, (22 + intensity * 6).dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            color = if (active || intensity > 0.05f) color else if (dark) Color(0xFF9FB3B0) else Color(0xFF5F7774),
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
        )
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
