package com.liweiwen.voicenotes.ui

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.liweiwen.voicenotes.asr.AsrResult
import com.liweiwen.voicenotes.asr.DashScopeAsr
import com.liweiwen.voicenotes.audio.AudioPlayer
import com.liweiwen.voicenotes.audio.PcmRecorder
import com.liweiwen.voicenotes.data.Note
import com.liweiwen.voicenotes.data.NoteStore
import com.liweiwen.voicenotes.data.SettingsStore
import com.liweiwen.voicenotes.data.VoiceNotesDocumentsProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.math.hypot

enum class SlideMode { CENTER, CANCEL, EDIT }

private const val LONG_PRESS_MS = 260L
private val Accent = Color(0xFF00A88C)
private val AccentBright = Color(0xFF62E6C8)
private val Secondary = Color(0xFF7C6CFF)
private val BgDark = Color(0xFF0B1020)
private val SurfaceDark = Color(0xFF141B2D)
private val CardDark = Color(0xFF1B2438)
private val InputDark = CardDark
private val BgLight = Color(0xFFF4F7FB)
private val SurfaceLight = Color(0xFFFFFFFF)
private val CardLight = Color(0xFFEEF3F9)

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
        darkColorScheme(
            primary = AccentBright,
            secondary = Secondary,
            background = BgDark,
            surface = SurfaceDark,
            onPrimary = Color(0xFF06231D),
        )
    } else {
        lightColorScheme(
            primary = Accent,
            secondary = Secondary,
            background = BgLight,
            surface = SurfaceLight,
            onPrimary = Color.White,
        )
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
    val systemClipboard = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    var clipboardText by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val slideThresholdPx = with(density) { 80.dp.toPx() }
    // 声波按钮点击热区（按钮 44dp + 间距 8dp），短按落在这里视为「语音待命」
    val waveZonePx = with(density) { 56.dp.toPx() }

    var notes by remember { mutableStateOf(noteStore.all()) }
    var draft by remember { mutableStateOf("") }
    // 默认语音模式：输入栏是纯文本按钮，不含可聚焦的 EditText，长按绝不会弹键盘。
    // 短按文字区切到打字模式；点左侧声波按钮进入「语音待命」，占位文字变「按住说话」。
    var textMode by remember { mutableStateOf(false) }
    var voiceArmed by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    var slideMode by remember { mutableStateOf(SlideMode.CENTER) }
    var liveText by remember { mutableStateOf("") }
    var elapsedSec by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Note?>(null) }
    var pendingEdit by remember { mutableStateOf<Note?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var dataStats by remember { mutableStateOf(noteStore.stats()) }
    var editText by remember { mutableStateOf("") }
    // 「编辑」模式松手后暂存的语音文件，用户点发送时一并入库
    var pendingAudio by remember { mutableStateOf<Pair<String, Long>?>(null) }

    val recorderRef = remember { mutableStateOf<PcmRecorder?>(null) }
    val asrRef = remember { mutableStateOf<DashScopeAsr?>(null) }
    var currentAudioFile by remember { mutableStateOf<File?>(null) }
    var finishJob by remember { mutableStateOf<Job?>(null) }

    // 全局搜索：匹配记录正文、语音标记与显示时间
    val keyword = searchQuery.trim()
    val visibleNotes = if (keyword.isBlank()) {
        notes
    } else {
        notes.filter { note ->
            note.text.contains(keyword, ignoreCase = true) ||
                (note.hasAudio && "语音".contains(keyword, ignoreCase = true)) ||
                formatRecordTime(note.time).contains(keyword, ignoreCase = true)
        }
    }

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

    /** 打开 App 或从后台回来时读取剪贴板，复制变化也实时跟进。 */
    fun refreshClipboard() {
        val text = runCatching {
            if (systemClipboard.hasPrimaryClip()) {
                systemClipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
            } else null
        }.getOrNull()
        clipboardText = text
            ?.replace('\n', ' ')
            ?.take(140)
            ?.ifBlank { null }
    }

    // 首次进入也要读一次：ON_RESUME 可能在监听注册前就发生了
    LaunchedEffect(Unit) { refreshClipboard() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshClipboard() }
    DisposableEffect(systemClipboard) {
        val listener = ClipboardManager.OnPrimaryClipChangedListener { refreshClipboard() }
        systemClipboard.addPrimaryClipChangedListener(listener)
        onDispose { systemClipboard.removePrimaryClipChangedListener(listener) }
    }

    /** 退出打字模式：收起键盘、清除焦点，回到语音模式。返回键/Done 键/收起按钮均走这里。 */
    fun exitTextMode() {
        textMode = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun refreshData() {
        notes = noteStore.all()
        dataStats = noteStore.stats()
    }

    /** 点击设置页的本机路径，直达系统文件应用里的「随手说本机数据」目录。 */
    fun openPath(path: String, directory: Boolean) {
        val target = File(path)
        if (!target.exists()) {
            Toast.makeText(context, "文件不存在或已被清理", Toast.LENGTH_SHORT).show()
            return
        }
        val docId = if (directory) {
            VoiceNotesDocumentsProvider.DOC_RECORDINGS
        } else {
            VoiceNotesDocumentsProvider.DOC_NOTES
        }
        val docUri = DocumentsContract.buildDocumentUri(VoiceNotesDocumentsProvider.AUTHORITY, docId)
        val docIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (directory) "*/*" else "text/plain"
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, docUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val opened = runCatching { context.startActivity(docIntent) }.isSuccess
        if (!opened) {
            // 兜底：少数设备没有实现 EXTRA_INITIAL_URI，退化为普通文件分享
            val uri = runCatching {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
            }.getOrNull() ?: return
            val fallback = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, if (directory) "resource/folder" else "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }.onFailure {
                Toast.makeText(context, "没有可用的文件应用，路径：$path", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 切到打字模式时，等 TextField 组合完成后再聚焦并弹键盘
    LaunchedEffect(textMode) {
        if (textMode) {
            clipFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(searchOpen) {
        if (searchOpen) searchFocusRequester.requestFocus()
    }

    // 返回键优先级：设置页 > 搜索 > 打字模式 >（不拦截，交给系统退出）
    BackHandler(enabled = showSettings) {
        showSettings = false
        refreshData()
    }
    BackHandler(enabled = !showSettings && searchOpen) {
        searchOpen = false
        searchQuery = ""
    }
    BackHandler(enabled = !showSettings && !searchOpen && textMode) {
        exitTextMode()
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
        exitTextMode()
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
            "正在录音 · 右滑转文字，左滑取消"
        } else {
            "正在录音 · 右滑可试转文字（当前未连接）"
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

        if (mode == SlideMode.CENTER) {
            // 语音待命下原地松手：只保存语音文件，不做识别
            asr?.cancel()
            saveNote("", true, wavFile?.absolutePath, duration)
            recording = false
            busy = false
            status = "语音已保存到本机"
            liveText = ""
            elapsedSec = 0
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
    val textColor = if (dark) Color(0xFFECECEC) else Color(0xFF222222)
    val subColor = if (dark) Color(0xFF8A8A8A) else Color(0xFF999999)

    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                title = {
                    if (searchOpen) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .focusRequester(searchFocusRequester),
                            placeholder = { Text("搜索记录内容", fontSize = 14.sp, color = subColor) },
                            singleLine = true,
                            trailingIcon = {
                                Row {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "清空搜索", tint = subColor)
                                        }
                                    }
                                    IconButton(onClick = { searchOpen = false; searchQuery = "" }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "退出搜索", tint = subColor)
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Accent,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                            ),
                        )
                    } else {
                        Text("随手说", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
                },
                actions = {
                    if (!searchOpen) {
                        IconButton(onClick = { searchOpen = true }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索记录", tint = textColor)
                        }
                    }
                    IconButton(onClick = {
                        apiKeyInput = settingsStore.apiKey
                        workspaceInput = settingsStore.workspaceId
                        modelInput = settingsStore.model
                        dataStats = noteStore.stats()
                        exitTextMode()
                        searchOpen = false
                        searchQuery = ""
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
            Column(modifier = Modifier.fillMaxSize()) {
                HomeHero(
                    noteCount = notes.size,
                    audioCount = notes.count { it.hasAudio },
                    dark = dark,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
            if (visibleNotes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .pointerInput(textMode) {
                            detectTapGestures { if (textMode) exitTextMode() }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    WaveGlyph(color = Accent.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (keyword.isBlank()) "按住输入栏，说话即记录" else "没有找到匹配的记录",
                        fontSize = 16.sp,
                        color = textColor,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (keyword.isBlank()) "语音与文字都保存在本机" else "换个关键词试试，共 ${notes.size} 条记录",
                        fontSize = 12.sp,
                        color = subColor,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(textMode) {
                            detectTapGestures { if (textMode) exitTextMode() }
                        },
                    contentPadding = PaddingValues(top = 12.dp, bottom = 116.dp),
                ) {
                    items(visibleNotes.asReversed(), key = { it.id }) { note ->
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
                }
            }

            // 底部悬浮输入栏 + 剪贴板快贴
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                val pasted = clipboardText
                if (pasted != null && draft.isBlank()) {
                    QuickPasteBar(
                        text = pasted,
                        dark = dark,
                        onPaste = {
                            draft = pasted
                            clipboardText = null
                            voiceArmed = false
                            textMode = true
                            status = ""
                        },
                        onDismiss = { clipboardText = null },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                val pillBg = if (dark) Color(0x141B2D) else Color(0xFFFFFFFF)
                val waveCircleBg = if (dark) AccentBright.copy(alpha = 0.16f) else Accent.copy(alpha = 0.10f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(24.dp), clip = true)
                        .clip(RoundedCornerShape(24.dp))
                        .background(surface)
                        .border(
                            1.dp,
                            if (dark) Color(0x22FFFFFF) else Color(0x12000000),
                            RoundedCornerShape(24.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 录音手势：仅语音模式套用。打字模式让 TextField 独占触摸，
                    // 否则手势会在 Initial 阶段消费 down，导致收起按钮/光标点击全部失效。
                    val recordModifier = recordGesture(
                        thresholdPx = slideThresholdPx,
                        onTap = { x ->
                            if (x <= waveZonePx) {
                                // 点左下角声波按钮：语音待命 ⇄ 打字模式，来回切换
                                voiceArmed = !voiceArmed
                                if (voiceArmed) {
                                    textMode = false
                                    status = "按住输入栏，松手保存语音"
                                } else {
                                    textMode = true
                                    status = ""
                                }
                            } else {
                                // 点文字区：切到打字模式，由 LaunchedEffect 聚焦并弹键盘
                                voiceArmed = false
                                textMode = true
                                status = ""
                            }
                        },
                        onStart = {
                            finishJob?.cancel()
                            finishJob = scope.launch { startRecording() }
                        },
                        onSlideModeChange = { slideMode = it },
                        onEnd = { mode -> finishJob = scope.launch { stopRecording(mode) } },
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(pillBg)
                            .then(if (textMode) Modifier else recordModifier),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(waveCircleBg)
                                .then(
                                    if (textMode) Modifier.clickable {
                                        exitTextMode()
                                        voiceArmed = true
                                        status = "按住输入栏，松手保存语音"
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            WaveGlyph(color = if (dark) AccentBright else Accent, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        if (textMode) {
                            TextField(
                                value = draft,
                                onValueChange = { draft = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .focusRequester(clipFocusRequester),
                                placeholder = { Text("输入文字…", fontSize = 14.sp, color = subColor, maxLines = 1) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { exitTextMode() }),
                                trailingIcon = {
                                    IconButton(onClick = { exitTextMode() }) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "收起键盘", tint = subColor)
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = Accent,
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                ),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 14.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    text = if (draft.isBlank()) {
                                        if (voiceArmed) "按住说话" else "点此打字"
                                    } else draft,
                                    fontSize = 14.sp,
                                    color = if (draft.isBlank()) subColor else textColor,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    if (draft.isNotBlank() || pendingAudio != null) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Accent.copy(alpha = 0.14f))
                                .clickable { sendDraft() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "保存", tint = Accent, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                val hint = if (pendingAudio != null) "待保存的语音 · 输入文字后点发送" else status
                if (hint.isNotBlank()) {
                    Text(
                        hint,
                        fontSize = 10.sp,
                        color = subColor,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 18.dp, bottom = 6.dp),
                    )
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

    // 全屏设置页（非弹窗）
    if (showSettings) {
        SettingsPage(
            dark = dark,
            apiKeyInput = apiKeyInput,
            workspaceInput = workspaceInput,
            modelInput = modelInput,
            theme = theme,
            stats = dataStats,
            notes = notes,
            notesFilePath = noteStore.notesFilePath,
            recordingsDirPath = noteStore.recordingsDirPath,
            onRefresh = { refreshData() },
            onOpenPath = { path, isDir -> openPath(path, isDir) },
            onApiKeyChange = { apiKeyInput = it },
            onWorkspaceChange = { workspaceInput = it },
            onModelChange = { modelInput = it },
            onThemeChange = onThemeChange,
            onBack = { showSettings = false },
            onSave = {
                settingsStore.apiKey = apiKeyInput.trim()
                settingsStore.workspaceId = workspaceInput.trim()
                settingsStore.model = modelInput.trim().ifBlank { SettingsStore.DEFAULT_MODEL }
                showSettings = false
                status = "设置已保存"
            },
            onClearAll = { confirmClear = true },
            onClearAudio = {
                noteStore.clearAudioOnly()
                refreshData()
                Toast.makeText(context, "已清除语音文件，文字记录保留", Toast.LENGTH_SHORT).show()
            },
        )
    }

    // 清空全部数据确认（设置页内的二次确认）
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空全部数据？") },
            text = { Text("将删除全部 ${dataStats.noteCount} 条记录和所有本地语音文件，且不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    noteStore.clearAll()
                    confirmClear = false
                    showSettings = false
                    playingPath?.let { audioPlayer.stop() }
                    playingPath = null
                    refreshData()
                    Toast.makeText(context, "已清空全部数据", Toast.LENGTH_SHORT).show()
                }) { Text("确认清空", color = Color(0xFFE64340)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
        )
    }
}

/** 首页概览：把「本地、条数、语音段数」一眼说清。 */
@Composable
private fun HomeHero(
    noteCount: Int,
    audioCount: Int,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val base = if (dark) CardDark else CardLight
    val tint = if (dark) AccentBright.copy(alpha = 0.10f) else Accent.copy(alpha = 0.09f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(base, tint)))
            .border(1.dp, if (dark) Color(0x1AFFFFFF) else Color(0x0D000000), RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (dark) AccentBright else Accent,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "本机速记 · 不上传",
                        fontSize = 11.sp,
                        color = if (dark) AccentBright else Accent,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "$noteCount 条记录  ·  $audioCount 段语音",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (dark) Color(0xFFEAF6F3) else Color(0xFF163A36),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "按住说话，松手存语音；右滑转文字",
                    fontSize = 11.sp,
                    color = if (dark) Color(0xFF9FB3B0) else Color(0xFF5F7774),
                )
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background((if (dark) AccentBright else Accent).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                WaveGlyph(
                    color = if (dark) AccentBright else Accent,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

/** 剪贴板快贴条：打开 App 就提示上一条复制内容。 */
@Composable
private fun QuickPasteBar(
    text: String,
    dark: Boolean,
    onPaste: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (dark) CardDark else CardLight)
            .border(1.dp, (if (dark) AccentBright else Accent).copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.ContentPaste,
            contentDescription = null,
            tint = if (dark) AccentBright else Accent,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("已检测到复制内容", fontSize = 10.sp, color = if (dark) Color(0xFF9FB3B0) else Color(0xFF5F7774))
            Text(text, fontSize = 12.sp, maxLines = 1, color = if (dark) Color(0xFFEAF6F3) else Color(0xFF163A36))
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onPaste) { Text("粘贴", color = if (dark) AccentBright else Accent) }
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "忽略剪贴板",
                tint = if (dark) Color(0xFF9FB3B0) else Color(0xFF5F7774),
                modifier = Modifier.size(16.dp),
            )
        }
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
    onTap: (x: Float) -> Unit,
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
        if (longPressed) onEnd(mode) else onTap(downX)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPage(
    dark: Boolean,
    apiKeyInput: String,
    workspaceInput: String,
    modelInput: String,
    theme: String,
    stats: com.liweiwen.voicenotes.data.StorageStats,
    notes: List<com.liweiwen.voicenotes.data.Note>,
    notesFilePath: String,
    recordingsDirPath: String,
    onRefresh: () -> Unit,
    onOpenPath: (String, Boolean) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onWorkspaceChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onClearAll: () -> Unit,
    onClearAudio: () -> Unit,
) {
    val surface = if (dark) SurfaceDark else SurfaceLight
    val background = if (dark) BgDark else BgLight
    val textColor = if (dark) Color(0xFFECECEC) else Color(0xFF222222)
    val subColor = if (dark) Color(0xFF8A8A8A) else Color(0xFF999999)
    val cardBg = if (dark) InputDark else Color.White

    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                title = { Text("设置", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surface, titleContentColor = textColor),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // 数据管理
            SettingsSection(title = "数据管理") {
                CardBox(cardBg) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    ) {
                        StatItem("记录", "${stats.noteCount} 条", textColor)
                        StatItem("语音", "${stats.audioCount} 个", textColor)
                        StatItem("占用", formatBytes(stats.audioBytes), textColor)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("本机数据", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onRefresh) { Text("刷新", fontSize = 12.sp) }
                }
                // 本机存储路径
                CardBox(cardBg) {
                    Column(Modifier.padding(12.dp)) {
                        PathLine("记录文件", notesFilePath, subColor, textColor) {
                            onOpenPath(notesFilePath, false)
                        }
                        Spacer(Modifier.height(6.dp))
                        PathLine("语音目录", recordingsDirPath, subColor, textColor) {
                            onOpenPath(recordingsDirPath, true)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                // 实时记录列表（最近 20 条）
                if (notes.isEmpty()) {
                    Text("暂无本地记录", fontSize = 12.sp, color = subColor, modifier = Modifier.padding(vertical = 6.dp))
                } else {
                    CardBox(cardBg) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            notes.take(20).forEach { note ->
                                DataRow(note, textColor, subColor)
                            }
                            if (notes.size > 20) {
                                Text("…还有 ${notes.size - 20} 条", fontSize = 11.sp, color = subColor, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onClearAudio) {
                    Text("清除语音文件（保留文字）", color = if (dark) Color(0xFFE8B04B) else Color(0xFFB8860B))
                }
                TextButton(onClick = onClearAll) {
                    Text("清空全部数据", color = Color(0xFFE64340))
                }
            }

            Spacer(Modifier.height(12.dp))

            // 外观
            SettingsSection(title = "外观") {
                Row {
                    listOf("system" to "跟随系统", "dark" to "深色", "light" to "浅色").forEach { (value, label) ->
                        TextButton(onClick = { onThemeChange(value) }) {
                            Text(
                                label,
                                color = if (theme == value) Accent else subColor,
                                fontWeight = if (theme == value) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 语音转写
            SettingsSection(title = "语音转写") {
                Text(
                    "录音保存为本机 WAV 文件，不需要存储权限；仅转写时把音频流式发给百炼。API Key 只存本机。",
                    fontSize = 12.sp,
                    color = subColor,
                )
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

            Spacer(Modifier.height(20.dp))
            androidx.compose.material3.Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Accent),
            ) {
                Text("保存设置", color = Color.White, fontSize = 16.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Accent)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun CardBox(bg: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
    ) { content() }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 12.sp, color = Color(0xFF999999))
    }
}

@Composable
private fun PathLine(
    label: String,
    path: String,
    subColor: Color,
    textColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = subColor)
            Text(path, fontSize = 10.sp, color = textColor)
        }
        Icon(Icons.Default.FolderOpen, contentDescription = "打开目录", tint = Accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DataRow(note: com.liweiwen.voicenotes.data.Note, textColor: Color, subColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                note.text.take(28) + if (note.text.length > 28) "…" else "",
                fontSize = 12.sp,
                color = textColor,
                maxLines = 1,
            )
            Text(
                formatRecordTime(note.time) + if (note.hasAudio) " · 语音 ${formatDuration(note.effectiveDurationMs)}" else " · 文字",
                fontSize = 10.sp,
                color = subColor,
            )
        }
    }
}

private fun formatRecordTime(time: Long): String {
    val d = java.util.Date(time)
    return java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(d)
}

private fun formatDuration(ms: Long): String {
    val sec = ms / 1000
    return "%d:%02d".format(sec / 60, sec % 60)
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024f)
    else -> "%.1f MB".format(bytes / (1024f * 1024f))
}
