package com.liweiwen.voicenotes.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.liweiwen.voicenotes.data.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteBubble(
    note: Note,
    dark: Boolean,
    isPlaying: Boolean,
    multiSelect: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    menuOpen: Boolean = false,
    onDismissMenu: () -> Unit = {},
    menu: @Composable () -> Unit = {},
) {
    val cardBg = if (dark) Color(0xFF1B2438) else Color(0xFFFFFFFF)
    val cardBorder = if (dark) Color(0x24FFFFFF) else Color(0x12000000)
    val textColor = if (dark) Color(0xFFEAF6F3) else Color(0xFF19322E)
    val subText = if (dark) Color(0xFF9FB3B0) else Color(0xFF5F7774)
    val accent = if (dark) Color(0xFF62E6C8) else Color(0xFF00A88C)

    // 纯语音记录（无转写文字）：只显示波形 + 时长
    val voiceOnly = note.hasAudio && (note.text == "[语音消息]" || note.text.isBlank())
    // 有真实文字的记录（含已转成文字的语音）：只显示文字，不再显示波形 + 时长
    val showWave = note.hasAudio && voiceOnly
    val bubbleBorder by animateColorAsState(
        targetValue = if (multiSelect && selected) accent else if (isPlaying) accent.copy(alpha = 0.7f) else cardBorder,
        animationSpec = tween(180),
        label = "bubbleBorder",
    )

    // 播放时波形轻微起伏，给用户明确的"正在响"反馈
    val transition = rememberInfiniteTransition(label = "playWave")
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "wavePhase",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 多选勾选框：放在气泡左侧，选中变实心
        if (multiSelect) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) accent else Color.Transparent)
                    .border(2.dp, if (selected) accent else subText, CircleShape)
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(Icons.Default.Check, contentDescription = "已选中", tint = if (dark) Color(0xFF0B1020) else Color.White, modifier = Modifier.size(15.dp))
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(horizontalAlignment = Alignment.End) {
            // 只保留时间；语音时长已在气泡内显示，不再重复标注"语音 X:XX"
            Text(formatTime(note.time), fontSize = 10.sp, color = subText, modifier = Modifier.padding(end = 6.dp, bottom = 4.dp))
            Box {
                Column(
                    modifier = Modifier
                        .widthIn(max = 290.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBg)
                        .border(1.dp, bubbleBorder, RoundedCornerShape(20.dp))
                        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    // 纯语音记录不显示文字行，只留波形 + 时长
                    if (!voiceOnly) {
                        Text(
                            note.text,
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                            color = textColor,
                        )
                    }
                    if (showWave) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedWave(
                                color = if (isPlaying) accent else accent.copy(alpha = 0.75f),
                                animate = isPlaying,
                                phase = wavePhase,
                                modifier = Modifier.size(if (voiceOnly) 56.dp else 40.dp, 20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                formatDuration(note.effectiveDurationMs),
                                fontSize = if (voiceOnly) 13.sp else 11.sp,
                                color = if (isPlaying) accent else subText,
                                fontWeight = if (voiceOnly) FontWeight.Medium else FontWeight.Normal,
                            )
                        }
                    }
                }
                // 就地弹出的紧凑操作菜单：锚定在气泡旁，点击外部或返回键关闭
                if (menuOpen) {
                    val density = LocalDensity.current
                    Popup(
                        // 气泡贴屏幕右侧，菜单从气泡右缘向左展开，避免超出右边界被裁切
                        alignment = Alignment.TopEnd,
                        offset = IntOffset(
                            x = with(density) { (-6).dp.toPx() }.toInt(),
                            y = with(density) { (-6).dp.toPx() }.toInt(),
                        ),
                        onDismissRequest = onDismissMenu,
                        properties = PopupProperties(focusable = true, clippingEnabled = false),
                    ) {
                        menu()
                    }
                }
            }
        }
    }
}

/** 播放时随相位起伏的声波；静止时为固定造型。 */
@Composable
private fun AnimatedWave(
    color: Color,
    animate: Boolean,
    phase: Float,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.Canvas(modifier.graphicsLayer { alpha = if (animate) 1f else 0.85f }) {
        val bars = listOf(0.35f, 0.7f, 1f, 0.62f, 0.28f, 0.8f, 0.45f)
        val barW = size.width / (bars.size * 2f)
        bars.forEachIndexed { i, ratio ->
            val lift = if (animate) (0.5f + 0.5f * sin(phase + i * 0.9f)) else 1f
            val h = size.height * ratio * (0.55f + 0.45f * lift)
            val bx = barW * (2 * i + 1)
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(bx, (size.height - h) / 2),
                end = androidx.compose.ui.geometry.Offset(bx, (size.height + h) / 2),
                strokeWidth = barW * 0.66f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

private fun formatTime(time: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(time))

private fun formatDuration(ms: Long): String {
    val sec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(sec / 60, sec % 60)
}
