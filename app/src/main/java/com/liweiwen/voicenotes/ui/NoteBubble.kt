package com.liweiwen.voicenotes.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liweiwen.voicenotes.data.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Accent = Color(0xFF07C160)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteBubble(
    note: Note,
    dark: Boolean,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val bubbleBg = if (dark) Color(0xFF2E7D4F) else Color(0xFF95EC69)
    val bubbleText = if (dark) Color(0xFFF5FFF8) else Color(0xFF1A1A1A)
    val subText = if (dark) Color(0xFF8A8A8A) else Color(0xFF999999)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp, bottom = 3.dp),
            ) {
                Text(formatTime(note.time), fontSize = 11.sp, color = subText)
                if (note.hasAudio) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "· 语音 ${formatDuration(note.durationMs)}",
                        fontSize = 11.sp,
                        color = Accent,
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                if (note.hasAudio) {
                    IconButton(onClick = onPlayToggle, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "停止播放" else "播放语音",
                            tint = Accent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 14.dp,
                                topEnd = 3.dp,
                                bottomStart = 14.dp,
                                bottomEnd = 14.dp,
                            ),
                        )
                        .background(bubbleBg)
                        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                        .padding(horizontal = 13.dp, vertical = 9.dp),
                ) {
                    Text(note.text, fontSize = 16.sp, lineHeight = 22.sp, color = bubbleText)
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
