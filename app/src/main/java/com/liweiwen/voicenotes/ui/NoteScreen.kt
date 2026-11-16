package com.liweiwen.voicenotes.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.liweiwen.voicenotes.asr.AsrResult
import com.liweiwen.voicenotes.asr.DashScopeAsr
import com.liweiwen.voicenotes.audio.AudioPlayer
import com.liweiwen.voicenotes.audio.PcmRecorder
import com.liweiwen.voicenotes.data.Note
import com.liweiwen.voicenotes.data.NoteStore
import com.liweiwen.voicenotes.data.SettingsStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.math.hypot

enum class SlideMode { CENTER, CANCEL, EDIT }

private const val LONG_PRESS_MS = 260L
private val Accent = Color(0xFF07C160)
private val BgDark = Color(0xFF121212)
private val SurfaceDark = Color(0xFF1E1E1E)
private val InputDark = Color(0xFF2A2A2A)
private val BgLight = Color(0xFFF5F5F5)
private val SurfaceLight = Color(0xFFFFFFFF)

/** 主体：主题放在内部，切换设置即可即时生效。 */
@Composable
fun NoteScreen(noteStore: NoteStore, settingsStore: SettingsStore) {
    var theme by remember { mutableStateOf(settingsStore.theme) }
    val systemDark = isSystemInDarkTheme()
    val dark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val colors = if (dark) {
        darkColorScheme(primary = Accent, background = BgDark, surface = SurfaceDark)
    } else {
        lightColorScheme(primary = Accent, background = BgLight, surface = SurfaceLight)
    }

    MaterialTheme(colorScheme = colors) {
        NoteScreenContent(
            noteStore = noteStore,
            settingsStore = settingsStore,
            dark = dark,
            theme = theme,
            onThemeChange = { value ->
                settingsStore.theme = value
                theme = value
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun NoteScreenContent(
    noteStore: NoteStore,
    settingsStore: SettingsStore,
    dark: Boolean,
    theme: String,
    onThemeChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipFocusRequester = remember { FocusRequester() }
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val slideThresholdPx = with(density) { 80.dp.toPx() }

    var notes by remember { mutableStateOf(noteStore.all()) }
    var draft by remember { mutableStateOf("") }
    // 默认语音模式：输入栏是纯文本按钮，不含可聚焦的 EditText，长按绝不会弹键盘。
    // 短按才切到打字模式，换成真正的 TextField 并聚焦。
    var textMode by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("点按输入框打字，或按住输入栏说话") }
    var recording by remember { mutableStateOf(false) }
    var slideMode by remember { mutableStateOf(SlideMode.CENTER) }
    var liveText by remember { mutableStateOf("") }
    var elapsedSec by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(settingsStore.apiKey.isBlank()) }
    var pendingDelete by remember { mutableStateOf<Note?>(null) }
    var pendingEdit by remember { mutableStateOf<Note?>(null) }
    var editText by remember { mutableStateOf("") }
    // 「编辑」模式松手后暂存的语音文件，用户点发送时一并入库
    var pendingAudio by remember { mutableStateOf<Pair<String, Long>?>(null) }

    val recorderRef = remember { mutableStateOf<PcmRecorder?>(null) }
    val asrRef = remember { mutableStateOf<DashScopeAsr?>(null) }
    var currentAudioFile by remember { mutableStateOf<File?>(null) }
    var finishJob by remember { mutableStateOf<Job?>(null) }

    val audioPlayer = remember { AudioPlayer() }
    var playingPath by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        onDispose { audioPlayer.release() }
    }

    var apiKeyInput by remember { mutableStateOf(settingsStore.apiKey) }
    var workspaceInput by remember { mutableStateOf(settingsStore.workspaceId) }
    var modelInput by remember { mutableStateOf(settingsStore.model) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        status = if (granted) "已获得麦克风权限，按住输入栏说话" else "需要麦克风权限才能录音"
    }

    // 切到打字模式时，等 TextField 组合完成后再聚焦并弹键盘
    LaunchedEffect(textMode) {
        if (textMode) {
            clipFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // 录音计时
    LaunchedEffect(recording) {
        if (!recording) return@LaunchedEffect
        val start = System.currentTimeMillis()
        while (true) {
            elapsedSec = ((System.currentTimeMillis() - start) / 1000).toInt()
            delay(200)
        }
    }

    fun refreshNotes() {
        notes = noteStore.all()
    }

    fun saveNote(text: String, fromVoice: Boolean, audioPath: String? = null, durationMs: Long = 0L) {
        noteStore.add(text.trim().ifBlank { "[语音消息]" }, fromVoice, audioPath, durationMs)
        refreshNotes()
    }

    fun sendDraft() {
        if (draft.isBlank() && pendingAudio == null) return
        val audio = pendingAudio
        saveNote(draft, audio != null, audio?.first, audio?.second ?: 0L)
        draft = ""
        pendingAudio = null
        textMode = false
        focusManager.clearFocus()
        keyboardController?.hide()
        status = "已记下"
    }

    suspend fun startRecording() {
        if (recording || busy) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        busy = true
        recording = true
        slideMode = SlideMode.CENTER
        liveText = ""
        elapsedSec = 0
        status = "准备录音…"
        focusManager.clearFocus()

        val wavFile = noteStore.newAudioFile()
        currentAudioFile = wavFile

        // 先尝试连百炼；连不上也照常录音，只是不转文字（本地优先）
        val asr = if (settingsStore.apiKey.isNotBlank()) {
            DashScopeAsr(
                apiKey = settingsStore.apiKey,
                wsUrl = settingsStore.wsUrl,
                model = settingsStore.model.ifBlank { SettingsStore.DEFAULT_MODEL },
                onPartial = { text -> liveText = text },
            )
        } else null

        var connected = false
        if (asr != null) {
            connected = try {
                withTimeout(4_000) { asr.connect() }
            } catch (_: Exception) {
                false
            }
        }
        if (connected) {
            asrRef.value = asr
        } else {
            asr?.cancel()
            asrRef.value = null
        }

        val recorder = PcmRecorder(
            onChunk = { chunk -> asrRef.value?.sendAudio(chunk) },
            wavFile = wavFile,
        )
        if (!recorder.start()) {
            wavFile.delete()
            currentAudioFile = null
            recording = false
            busy = false
            status = "麦克风启动失败"
            return
        }
        recorderRef.value = recorder
        busy = false
        status = if (connected) {
            "正在听… 滑到「取消」丢弃，滑到「编辑」改字"
        } else {
            "转写服务不可用，本次只保存语音"
        }
    }

    suspend fun stopRecording(mode: SlideMode) {
        val recorder = recorderRef.value ?: return
        val asr = asrRef.value
        val wavFile = currentAudioFile
        recorderRef.value = null
        asrRef.value = null
        currentAudioFile = null

        recorder.stop()
        val duration = recorder.durationMs

        if (mode == SlideMode.CANCEL) {
            asr?.cancel()
            wavFile?.delete()
            recording = false
            liveText = ""
            status = "已取消，语音未保存"
            return
        }

        busy = true
        status = "正在转文字…"
        val result: AsrResult = if (asr != null) {
            try {
                withTimeout(12_000) { asr.finish() }
            } catch (e: Exception) {
                AsrResult.Failure(e.message ?: "转写超时")
            }
        } else {
            AsrResult.Failure("未连接转写服务")
        }
        busy = false
        recording = false

        val spoken = when (result) {
            is AsrResult.Success -> result.text.ifBlank { liveText.trim() }
            is AsrResult.Failure -> liveText.trim()
        }

        if (mode == SlideMode.EDIT) {
            draft = spoken
            textMode = true
            wavFile?.let { pendingAudio = it.absolutePath to duration }
            status = if (result is AsrResult.Failure) {
                "转写失败，语音已保留，可手动输入文字"
            } else {
                "已转成文字，可编辑后点发送"
            }
        } else {
            saveNote(spoken, true, wavFile?.absolutePath, duration)
            status = if (result is AsrResult.Failure) {
                "语音已保存到本机（转写失败）"
            } else {
                "已记下（含语音）"
            }
        }
        liveText = ""
    }

    LaunchedEffect(notes.size) {
        if (notes.isNotEmpty()) listState.animateScrollToItem(0)
    }

    val surface = if (dark) SurfaceDark else SurfaceLight
    val background = if (dark) BgDark else BgLight
    val inputBg = if (dark) InputDark else Color.White
    val textColor = if (dark) Color(0xFFECECEC) else Color(0xFF222222)
    val subColor = if (dark) Color(0xFF8A8A8A) else Color(0xFF999999)

    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                title = { Text("随手说", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                actions = {
                    IconButton(onClick = {
                        apiKeyInput = settingsStore.apiKey
                        workspaceInput = settingsStore.workspaceId
                        modelInput = settingsStore.model
                        showSettings = true
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surface, titleContentColor = textColor),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(background),
        ) {
            if (notes.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    WaveGlyph(color = Accent.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("按住输入栏，说话即记录", fontSize = 16.sp, color = textColor)
                    Spacer(Modifier.height(6.dp))
                    Text("语音与文字都保存在本机", fontSize = 12.sp, color = subColor)
                }
            } else {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 116.dp),
                ) {
                    items(notes.asReversed(), key = { it.id }) { note ->
                        NoteBubble(
                            note = note,
                            dark = dark,
                            isPlaying = playingPath == note.audioPath && note.audioPath != null,
                            onPlayToggle = {
                                val path = note.audioPath
                                if (path == null) return@NoteBubble
                                if (playingPath == path) {
                                    audioPlayer.stop()
                                    playingPath = null
                                } else {
                                    audioPlayer.play(path, onFinished = { playingPath = null })
                                    playingPath = path
                                }
                            },
                            onClick = {
                                clipboard.setText(AnnotatedString(note.text))
                                Toast.makeText(context, "已复制，长按气泡可编辑", Toast.LENGTH_SHORT).show()
                            },
                            onLongClick = {
                                editText = note.text
                                pendingEdit = note
                            },
                        )
                    }
                }
            }

            // 底部输入栏
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(surface),
            ) {
                Text(
                    if (pendingAudio != null) "待保存的语音 · 输入文字后点发送" else status,
                    fontSize = 11.sp,
                    color = subColor,
                    modifier = Modifier.padding(start = 14.dp, top = 5.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 手势只覆盖「声波按钮 + 输入区」，发送按钮独立在外，避免被拦截
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                recordGesture(
                                    thresholdPx = slideThresholdPx,
                                    onTap = {
                                        // 短按：切到打字模式，由 LaunchedEffect 聚焦并弹键盘
                                        textMode = true
                                    },
                                    onStart = {
                                        // 长按：收起键盘、退出打字模式，再开始录音
                                        textMode = false
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        finishJob?.cancel()
                                        finishJob = scope.launch { startRecording() }
                                    },
                                    onSlideModeChange = { slideMode = it },
                                    onEnd = { mode -> finishJob = scope.launch { stopRecording(mode) } },
                                ),
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 图2 第一个按钮：声波图标
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(inputBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            WaveGlyph(color = Accent, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        if (textMode) {
                            // 打字模式：真正的 TextField，长按手势此时不会误触（键盘已在，长按用于录音）
                            TextField(
                                value = draft,
                                onValueChange = { draft = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 56.dp)
                                    .focusRequester(clipFocusRequester),
                                placeholder = { Text("输入文字…", fontSize = 14.sp, color = subColor, maxLines = 1) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = inputBg,
                                    unfocusedContainerColor = inputBg,
                                    disabledContainerColor = inputBg,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = Accent,
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                ),
                            )
                        } else {
                            // 语音模式：纯文本占位，没有可聚焦的 EditText，长按绝不会弹键盘
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 56.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(inputBg)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    text = if (draft.isBlank()) "按住说话 · 点此打字" else draft,
                                    fontSize = 14.sp,
                                    color = if (draft.isBlank()) subColor else textColor,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    if (draft.isNotBlank() || pendingAudio != null) {
                        Spacer(Modifier.width(6.dp))
                        IconButton(onClick = { sendDraft() }) {
                            Icon(Icons.Default.Send, contentDescription = "保存", tint = Accent)
                        }
                    }
                }
            }

            if (busy && !recording) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Accent,
                )
            }

            if (recording) {
                RecordingPanel(
                    mode = slideMode,
                    liveText = liveText,
                    elapsedSec = elapsedSec,
                    dark = dark,
                )
            }
        }
    }

    // 设置
    if (showSettings) {
        SettingsDialog(
            apiKeyInput = apiKeyInput,
            workspaceInput = workspaceInput,
            modelInput = modelInput,
            theme = theme,
            onApiKeyChange = { apiKeyInput = it },
            onWorkspaceChange = { workspaceInput = it },
            onModelChange = { modelInput = it },
            onThemeChange = onThemeChange,
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

    // 再次编辑
    pendingEdit?.let { note ->
        AlertDialog(
            onDismissRequest = { pendingEdit = null },
            title = { Text("编辑记录") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp),
                        label = { Text("内容") },
                        minLines = 3,
                    )
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = {
                        noteStore.delete(note.id)
                        refreshNotes()
                        pendingEdit = null
                    }) { Text("删除这条记录", color = Color(0xFFE64340)) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    noteStore.updateText(note.id, editText.trim())
                    refreshNotes()
                    pendingEdit = null
                    Toast.makeText(context, "已更新", Toast.LENGTH_SHORT).show()
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { pendingEdit = null }) { Text("取消") } },
        )
    }

    // 删除确认
    pendingDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条记录？") },
            text = {
                Text(
                    note.text.take(80) + if (note.hasAudio) "\n（同时删除本地语音文件）" else "",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    noteStore.delete(note.id)
                    refreshNotes()
                    if (playingPath == note.audioPath) {
                        audioPlayer.stop()
                        playingPath = null
                    }
                    pendingDelete = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

/**
 * 长按手势：按住 → 录音；左滑「取消」，右滑「编辑」；短按 → 打字。
 * 关键：在 Initial 阶段就消费 down 事件，让内部 TextField 收不到点击，
 * 从而彻底杜绝「长按却弹出输入法」。短按抬起时再手动把焦点交给输入框。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun recordGesture(
    thresholdPx: Float,
    onTap: () -> Unit,
    onStart: () -> Unit,
    onSlideModeChange: (SlideMode) -> Unit,
    onEnd: (SlideMode) -> Unit,
): Modifier = Modifier.pointerInput(thresholdPx) {
    awaitEachGesture {
        // Initial pass 早于 TextField 的点击处理；consume 后输入框不会自行聚焦弹键盘
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        down.consume()
        val downX = down.position.x
        val downY = down.position.y
        var longPressed = false
        var mode = SlideMode.CENTER
        val startTime = System.currentTimeMillis()
        val slop = viewConfiguration.touchSlop.toFloat()

        while (true) {
            val event = if (longPressed) {
                awaitPointerEvent(PointerEventPass.Initial)
            } else {
                withTimeoutOrNull(LONG_PRESS_MS) {
                    awaitPointerEvent(PointerEventPass.Initial)
                }
            }
            if (event == null) {
                longPressed = true
                onStart()
                continue
            }
            // 消费后续事件，防止事件继续传给输入框造成滚动或聚焦
            event.changes.forEach { if (it.id == down.id) it.consume() }
            val change = event.changes.firstOrNull { it.id == down.id }
            if (change == null || !change.pressed) break

            val dx = change.position.x - downX
            val dy = change.position.y - downY
            if (!longPressed) {
                if (hypot(dx, dy) > slop) break
                if (System.currentTimeMillis() - startTime >= LONG_PRESS_MS) {
                    longPressed = true
                    onStart()
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
        if (longPressed) onEnd(mode) else onTap()
    }
}

@Composable
private fun SettingsDialog(
    apiKeyInput: String,
    workspaceInput: String,
    modelInput: String,
    theme: String,
    onApiKeyChange: (String) -> Unit,
    onWorkspaceChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    canDismiss: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        title = { Text("设置") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "录音保存为本机 WAV 文件，不需要存储权限；仅转写时把音频流式发给百炼。API Key 只存本机。",
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                )
                Spacer(Modifier.height(12.dp))
                Text("外观", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Row {
                    listOf("system" to "跟随系统", "dark" to "深色", "light" to "浅色").forEach { (value, label) ->
                        TextButton(onClick = { onThemeChange(value) }) {
                            Text(
                                label,
                                color = if (theme == value) Accent else Color(0xFF999999),
                                fontWeight = if (theme == value) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
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
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("保存") } },
        dismissButton = if (canDismiss) {
            { TextButton(onClick = onDismiss) { Text("取消") } }
        } else null,
    )
}
