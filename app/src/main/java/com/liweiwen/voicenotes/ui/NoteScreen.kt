package com.liweiwen.voicenotes.ui

import android.Manifest
import android.content.ClipData
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.liweiwen.voicenotes.asr.AsrResult
import com.liweiwen.voicenotes.asr.DashScopeAsr
import com.liweiwen.voicenotes.audio.PcmRecorder
import com.liweiwen.voicenotes.data.Note
import com.liweiwen.voicenotes.data.NoteStore
import com.liweiwen.voicenotes.data.SettingsStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

enum class SlideMode { CENTER, CANCEL, EDIT }

private const val LONG_PRESS_MS = 260L

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(noteStore: NoteStore, settingsStore: SettingsStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val slideThresholdPx = with(density) { 72.dp.toPx() }

    var notes by remember { mutableStateOf(noteStore.all()) }
    var draft by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("点按输入框打字，按住麦克风说话") }
    var recording by remember { mutableStateOf(false) }
    var slideMode by remember { mutableStateOf(SlideMode.CENTER) }
    var liveText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(settingsStore.apiKey.isBlank()) }
    var pendingDelete by remember { mutableStateOf<Note?>(null) }

    var apiKeyInput by remember { mutableStateOf(settingsStore.apiKey) }
    var workspaceInput by remember { mutableStateOf(settingsStore.workspaceId) }
    var modelInput by remember { mutableStateOf(settingsStore.model) }

    val recorderRef = remember { mutableStateOf<PcmRecorder?>(null) }
    val asrRef = remember { mutableStateOf<DashScopeAsr?>(null) }
    var finishJob by remember { mutableStateOf<Job?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        status = if (granted) "麦克风已授权，按住绿色按钮说话" else "需要麦克风权限才能语音输入"
    }

    fun saveNote(text: String, fromVoice: Boolean) {
        if (text.isBlank()) return
        noteStore.add(text.trim(), fromVoice)
        notes = noteStore.all()
    }

    fun sendDraft() {
        if (draft.isBlank()) return
        saveNote(draft, false)
        draft = ""
    }

    suspend fun startRecording() {
        if (settingsStore.apiKey.isBlank()) {
            showSettings = true
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (recording || busy) return

        busy = true
        recording = true
        slideMode = SlideMode.CENTER
        liveText = ""
        status = "正在连接百炼…"

        val asr = DashScopeAsr(
            apiKey = settingsStore.apiKey,
            wsUrl = settingsStore.wsUrl,
            model = settingsStore.model.ifBlank { SettingsStore.DEFAULT_MODEL },
            onPartial = { text -> liveText = text },
        )
        asrRef.value = asr

        val connected = try {
            asr.connect()
        } catch (e: Exception) {
            status = "连接失败：${e.message ?: "未知错误"}"
            false
        }
        if (!connected) {
            asr.cancel()
            asrRef.value = null
            recording = false
            busy = false
            status = "连接百炼失败，请检查 API Key 与网络"
            return
        }

        val recorder = PcmRecorder { chunk -> asr.sendAudio(chunk) }
        if (!recorder.start()) {
            asr.cancel()
            asrRef.value = null
            recording = false
            busy = false
            status = "麦克风启动失败"
            return
        }
        recorderRef.value = recorder
        busy = false
        status = "正在听… 左滑取消 · 右滑转成文字编辑"
    }

    suspend fun stopRecording(mode: SlideMode) {
        val recorder = recorderRef.value
        val asr = asrRef.value
        recorderRef.value = null
        asrRef.value = null
        if (recorder == null || asr == null) return

        recorder.stop()
        if (mode == SlideMode.CANCEL) {
            asr.cancel()
            recording = false
            liveText = ""
            status = "已取消"
            return
        }

        busy = true
        status = "正在转文字…"
        val result = try {
            asr.finish()
        } catch (e: Exception) {
            AsrResult.Failure(e.message ?: "转写超时")
        }
        busy = false
        recording = false

        when (result) {
            is AsrResult.Success -> {
                val text = result.text.ifBlank { liveText.trim() }
                if (text.isBlank()) {
                    status = "没听清，再按一次试试"
                    return
                }
                if (mode == SlideMode.EDIT) {
                    draft = text
                    status = "已转成文字，改完点发送"
                } else {
                    saveNote(text, true)
                    status = "已记下"
                }
                liveText = ""
            }

            is AsrResult.Failure -> {
                draft = listOf(liveText.trim(), draft).filter { it.isNotBlank() }.joinToString(" ")
                status = "转写失败：${result.message}（文字已留在输入框）"
            }
        }
    }

    LaunchedEffect(notes.size) {
        if (notes.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("随手说", fontSize = 18.sp) },
                actions = {
                    IconButton(onClick = {
                        apiKeyInput = settingsStore.apiKey
                        workspaceInput = settingsStore.workspaceId
                        modelInput = settingsStore.model
                        showSettings = true
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7F7F7)),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
        ) {
            if (notes.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("还没有记录", fontSize = 17.sp, color = Color(0xFF666666))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "按住下方绿色麦克风说话，松手自动转文字",
                        fontSize = 13.sp,
                        color = Color(0xFF999999),
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 108.dp),
                ) {
                    items(notes.asReversed(), key = { it.id }) { note ->
                        NoteBubble(
                            note = note,
                            onClick = {
                                clipboard.setText(AnnotatedString(note.text))
                                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                            },
                            onLongClick = { pendingDelete = note },
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFFF7F7F7))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text(
                    status,
                    fontSize = 11.sp,
                    color = Color(0xFF999999),
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        placeholder = { Text("说点什么…", fontSize = 14.sp) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    MicButton(
                        recording = recording,
                        thresholdPx = slideThresholdPx,
                        onLongPressStart = { finishJob?.cancel(); finishJob = scope.launch { startRecording() } },
                        onSlideModeChange = { slideMode = it },
                        onLongPressEnd = { mode -> finishJob = scope.launch { stopRecording(mode) } },
                        onTap = { /* 语音键短按不做操作，点输入框打字 */ },
                    )
                    if (draft.isNotBlank()) {
                        IconButton(onClick = { sendDraft() }) {
                            Icon(Icons.Default.Send, contentDescription = "发送", tint = Color(0xFF07C160))
                        }
                    }
                }
            }

            if (busy && !recording) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF07C160),
                )
            }

            if (recording) {
                RecordingOverlay(slideMode, liveText)
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            apiKeyInput = apiKeyInput,
            workspaceInput = workspaceInput,
            modelInput = modelInput,
            onApiKeyChange = { apiKeyInput = it },
            onWorkspaceChange = { workspaceInput = it },
            onModelChange = { modelInput = it },
            canDismiss = settingsStore.apiKey.isNotBlank(),
            onDismiss = { showSettings = false },
            onSave = {
                settingsStore.apiKey = apiKeyInput.trim()
                settingsStore.workspaceId = workspaceInput.trim()
                settingsStore.model = modelInput.trim().ifBlank { SettingsStore.DEFAULT_MODEL }
                showSettings = false
                status = "设置已保存"
            },
        )
    }

    pendingDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条记录？") },
            text = { Text(note.text.take(80)) },
            confirmButton = {
                TextButton(onClick = {
                    noteStore.delete(note.id)
                    notes = noteStore.all()
                    pendingDelete = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun MicButton(
    recording: Boolean,
    thresholdPx: Float,
    onLongPressStart: () -> Unit,
    onSlideModeChange: (SlideMode) -> Unit,
    onLongPressEnd: (SlideMode) -> Unit,
    onTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (recording) Color(0xFF07C160) else Color(0xFFE5E5E5))
            .pointerInput(thresholdPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downX = down.position.x
                    val downY = down.position.y
                    var longPressed = false
                    var abortedByMove = false
                    var mode = SlideMode.CENTER
                    val startTime = System.currentTimeMillis()
                    val slop = viewConfiguration.touchSlop.toFloat()

                    while (true) {
                        val event = if (longPressed) {
                            awaitPointerEvent(PointerEventPass.Main)
                        } else {
                            withTimeoutOrNull(LONG_PRESS_MS) {
                                awaitPointerEvent(PointerEventPass.Main)
                            }
                        }
                        if (event == null) {
                            longPressed = true
                            onLongPressStart()
                            continue
                        }
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) break

                        val dx = change.position.x - downX
                        val dy = change.position.y - downY
                        if (!longPressed) {
                            if (hypot(dx, dy) > slop) {
                                abortedByMove = true
                                break
                            }
                            if (System.currentTimeMillis() - startTime >= LONG_PRESS_MS) {
                                longPressed = true
                                onLongPressStart()
                            }
                        } else {
                            val next = when {
                                dx <= -thresholdPx -> SlideMode.CANCEL
                                dx >= thresholdPx -> SlideMode.EDIT
                                else -> SlideMode.CENTER
                            }
                            if (next != mode) {
                                mode = next
                                onSlideModeChange(next)
                            }
                        }
                    }

                    if (longPressed) onLongPressEnd(mode) else if (!abortedByMove) onTap()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = "按住说话",
            tint = if (recording) Color.White else Color(0xFF555555),
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun RecordingOverlay(mode: SlideMode, liveText: String) {
    val color = when (mode) {
        SlideMode.CANCEL -> Color(0xFFE64340)
        SlideMode.EDIT -> Color(0xFF2F7DE1)
        SlideMode.CENTER -> Color(0xFF303030)
    }
    val label = when (mode) {
        SlideMode.CANCEL -> "松开取消"
        SlideMode.EDIT -> "松开编辑文字"
        SlideMode.CENTER -> "松手转文字并发送"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(label, color = Color.White, fontSize = 13.sp)
                }
            }
            if (liveText.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    liveText,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("← 左滑取消    右滑转文字编辑 →", color = Color(0xFFDDDDDD), fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteBubble(note: Note, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatTime(note.time) + if (note.fromVoice) " · 语音" else "",
                fontSize = 10.sp,
                color = Color(0xFFAAAAAA),
                modifier = Modifier.padding(end = 4.dp, bottom = 2.dp),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 2.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                    .background(Color(0xFF95EC69))
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(note.text, fontSize = 16.sp, color = Color(0xFF1A1A1A))
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    apiKeyInput: String,
    workspaceInput: String,
    modelInput: String,
    onApiKeyChange: (String) -> Unit,
    onWorkspaceChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    canDismiss: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        title = { Text("百炼语音设置") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "API Key 只保存在本机，不会上传。录音音频直接流式发给百炼，不存文件。",
                    fontSize = 12.sp,
                    color = Color(0xFF888888),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = onApiKeyChange,
                    label = { Text("DashScope API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = workspaceInput,
                    onValueChange = onWorkspaceChange,
                    label = { Text("业务空间 ID（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelInput,
                    onValueChange = onModelChange,
                    label = { Text("识别模型") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "业务空间 ID 留空时用 dashscope.aliyuncs.com 兼容地址；填写后使用空间专属域名。",
                    fontSize = 11.sp,
                    color = Color(0xFF999999),
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("保存") } },
        dismissButton = if (canDismiss) {
            { TextButton(onClick = onDismiss) { Text("取消") } }
        } else null,
    )
}

private fun formatTime(time: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(time))
