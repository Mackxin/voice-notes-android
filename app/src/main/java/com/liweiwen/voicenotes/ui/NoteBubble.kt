package com.liweiwen.voicenotes.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liweiwen.voicenotes.data.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteBubble(
    note: Note,
    dark: Boolean,
    isPlaying: Boolean,
    multiSelect: Boolean = false,
    selected: Boolean = false,
    onPlayToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val cardBg = if (dark) Color(0xFF1B2438) else Color(0xFFFFFFFF)
    val cardBorder = if (dark) Color(0x24FFFFFF) else Color(0x12000000)
    val textColor = if (dark) Color(0xFFEAF6F3) else Color(0xFF19322E)
    val subText = if (dark) Color(0xFF9FB3B0) else Color(0xFF5F7774)
    val accent = if (dark) Color(0xFF62E6C8) else Color(0xFF00A88C)

    // 纯语音记录（无转写文字）：不显示文字行与复制图标，只保留波形 + 时长
    val voiceOnly = note.hasAudio && (note.text == "[语音消息]" || note.text.isBlank())
    val bubbleBorder by animateColorAsState(
        targetValue = if (multiSelect && selected) accent else cardBorder,
        animationSpec = tween(180),
        label = "bubbleBorder",
    )
    val bubbleBg by animateColorAsState(
        targetValue = if (multiSelect && selected) accent.copy(alpha = if (dark) 0.16f else 0.10f) else cardBg,
        animationSpec = tween(180),
        label = "bubbleBg",
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 6.dp, bottom = 4.dp),
            ) {
                Text(formatTime(note.time), fontSize = 10.sp, color = subText)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        if (note.hasAudio) "语音 ${formatDuration(note.effectiveDurationMs)}" else "文字",
                        fontSize = 10.sp,
                        color = accent,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                if (note.hasAudio && !multiSelect) {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp, bottom = 4.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.14f))
                            .combinedClickable(onClick = onPlayToggle),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "停止播放" else "播放语音",
                            tint = accent,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .widthIn(max = 290.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp), clip = true)
                        .clip(RoundedCornerShape(20.dp))
                        .background(bubbleBg)
                        .border(1.dp, bubbleBorder, RoundedCornerShape(20.dp))
                        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    // 纯语音记录不显示文字行，只留波形 + 时长
                    if (!voiceOnly) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                note.text,
                                modifier = Modifier.weight(1f),
                                fontSize = 15.sp,
                                lineHeight = 21.sp,
                                color = textColor,
                            )
                            Spacer(Modifier.width(8.dp))
                            if (!multiSelect) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "点卡片复制",
                                    tint = subText.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                    if (note.hasAudio) {
                        if (!voiceOnly) Spacer(Modifier.height(9.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            WaveGlyph(color = accent, modifier = Modifier.size(if (voiceOnly) 52.dp else 34.dp, 16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(formatDuration(note.effectiveDurationMs), fontSize = if (voiceOnly) 12.sp else 10.sp, color = subText)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(time: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(time))

private fun formatDuration(ms: Long): String {
    val sec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(sec / 60, sec % 60)
}
