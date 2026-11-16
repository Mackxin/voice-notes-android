package com.liweiwen.voicenotes.ui

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 微信式底部录音面板：
 * 中央半圆录音区 + 左右两个独立圆角按钮「取消」「编辑」。
 * 手指滑到哪个按钮，哪个按钮高亮。
 */
@Composable
fun RecordingPanel(mode: SlideMode, liveText: String, elapsedSec: Int, dark: Boolean) {
    // 录音区呼吸动画
    val infinite = rememberInfiniteTransition(label = "recording")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(720, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse",
    )

    val scrim = if (dark) Color(0xD9000000) else Color(0x99000000)
    val panelBg = if (dark) Color(0xFF3A3A3A) else Color(0xFFB8B8B8)
    val buttonBg = if (dark) Color(0xFF1F1F1F) else Color(0xFF2A2A2A)

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
                    .clip(RoundedCornerShape(16.dp))
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

        // 底部面板
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SlideButton(
                    label = "取消",
                    active = mode == SlideMode.CANCEL,
                    activeColor = Color(0xFFE64340),
                    bg = buttonBg,
                )
                SlideButton(
                    label = "编辑",
                    active = mode == SlideMode.EDIT,
                    activeColor = Color(0xFF07C160),
                    bg = buttonBg,
                )
            }

            Spacer(Modifier.height(10.dp))

            // 中央录音半圆
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(topStart = 96.dp, topEnd = 96.dp))
                    .background(panelBg),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WaveGlyph(
                        color = Color.White,
                        modifier = Modifier.scale(pulse).size(44.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = formatElapsed(elapsedSec),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = when (mode) {
                            SlideMode.CANCEL -> "松开取消"
                            SlideMode.EDIT -> "松开后编辑文字"
                            SlideMode.CENTER -> "松手转文字并保存"
                        },
                        color = Color(0xFFEEEEEE),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SlideButton(label: String, active: Boolean, activeColor: Color, bg: Color) {
    Box(
        modifier = Modifier
            .width(112.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (active) activeColor else bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

/** 四条声波竖线组成的图标（不依赖图标库）。 */
@Composable
fun WaveGlyph(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bars = listOf(0.35f, 0.7f, 1f, 0.62f, 0.28f)
        val barW = w / (bars.size * 2f)
        bars.forEachIndexed { i, ratio ->
            val cx = barW * (2 * i + 1)
            val bh = h * ratio
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(cx, (h - bh) / 2),
                end = androidx.compose.ui.geometry.Offset(cx, (h + bh) / 2),
                strokeWidth = barW * 0.62f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

private fun formatElapsed(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)
