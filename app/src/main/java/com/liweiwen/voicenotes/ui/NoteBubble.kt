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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    onDoubleClick: () -> Unit = {},
    menuOpen: Boolean = false,
    onDismissMenu: () -> Unit = {},
    menu: @Composable () -> Unit = {},
    hidePrivate: Boolean = false,
    privateRevealed: Boolean = false,
    onRevealPrivate: () -> Unit = {},
    onOpenReminder: () -> Unit = {},
) {
    val cardBg = if (dark) Color(0xFF1B2438) else Color(0xFFFFFFFF)
    val cardBorder = if (dark) Color(0x24FFFFFF) else Color(0x12000000)
    val textColor = if (dark) Color(0xFFEAF6F3) else Color(0xFF19322E)
    val subText = if (dark) Color(0xFF9FB3B0) else Color(0xFF5F7774)
    val palette = LocalPalette.current
    val accent = if (dark) palette.accentBright else palette.accent

    // 纯语音记录（无转写文字）：只显示波形 + 时长
    val voiceOnly = note.hasAudio && (note.text == "[语音消息]" || note.text.isBlank())
    // 有真实文字的记录（含已转成文字的语音）：只显示文字，不再显示波形 + 时长
    val showWave = note.hasAudio && voiceOnly
    // 纯语音保留胶囊；文字记录改圆角矩形，长文本不会被椭圆裁掉边角
    val bubbleShape = if (voiceOnly) RoundedCornerShape(50) else RoundedCornerShape(18.dp)
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
        horizontalArrangement = Arrangement.Start,
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
        Column(horizontalAlignment = Alignment.Start) {
            // 只保留时间；语音时长已在气泡内显示，不再重复标注"语音 X:XX"
            Text(formatTime(note.time), fontSize = 10.sp, color = subText, modifier = Modifier.padding(start = 6.dp, bottom = 4.dp))
            Box {
                Column(
                    modifier = Modifier
                        .widthIn(max = 290.dp)
                        .clip(bubbleShape)
                    .background(cardBg)
                    .border(1.dp, bubbleBorder, bubbleShape)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                        onDoubleClick = onDoubleClick,
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    // 私密且未揭开：正文打码，点一下临时显示
                    val masked = note.isPrivate && hidePrivate && !privateRevealed
                    // 纯语音记录不显示文字行，只留波形 + 时长
                    if (!voiceOnly) {
                        Text(
                            if (masked) "🔒 私密记录 · 点击显示" else note.text,
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                            color = if (masked) subText else textColor,
                            modifier = if (masked) Modifier.clickable(onClick = onRevealPrivate) else Modifier,
                        )
                    }
                    // AI 摘要小字
                    if (note.summary.isNotBlank() && !masked) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "✦ ${note.summary}",
                            fontSize = 11.sp,
                            color = accent,
                        )
                    }
                    // 位置 / 提醒小徽标
                    val badges = mutableListOf<String>()
                    if (note.hasReminder) badges.add("⏰ 提醒")
                    if (note.hasLocation) badges.add("📍 ${note.placeName}")
                    if (note.isPrivate) badges.add("🔒")
                    if (badges.isNotEmpty()) {
                        Spacer(Modifier.height(5.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable(enabled = note.hasReminder, onClick = onOpenReminder),
                        ) {
                            badges.forEach { Text(it, fontSize = 10.sp, color = subText) }
                        }
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
                    // 标签小胶囊：紧跟正文下方，一眼看出这条属于哪个分类
                    if (note.tags.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            note.tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(accent.copy(alpha = 0.14f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text("#$tag", fontSize = 10.sp, color = accent)
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                        }
                    }
                }
                // 就地弹出的紧凑操作菜单：锚定在气泡旁，点击外部或返回键关闭
                if (menuOpen) {
                    val density = LocalDensity.current
                    Popup(
                        // 气泡靠左：操作条底边对齐气泡底边、左缘对齐，从气泡上方往右展开，避免超出屏幕左右边界
                        alignment = Alignment.BottomStart,
                        offset = IntOffset(
                            x = with(density) { 6.dp.toPx() }.toInt(),
                            y = with(density) { (-8).dp.toPx() }.toInt(),
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
