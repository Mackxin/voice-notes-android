package com.liweiwen.voicenotes.ui

import android.Manifest
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Slider
import com.liweiwen.voicenotes.audio.TtsSpeaker
import com.liweiwen.voicenotes.data.BackupManager
import com.liweiwen.voicenotes.data.UpdateManager
import com.liweiwen.voicenotes.data.AppBus
import com.liweiwen.voicenotes.data.AutoBackupWorker
import com.liweiwen.voicenotes.data.ExportManager
import com.liweiwen.voicenotes.data.LocationHelper
import com.liweiwen.voicenotes.data.ReminderManager
import com.liweiwen.voicenotes.data.SummaryManager
import com.liweiwen.voicenotes.widget.NotesWidgetProvider
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import java.util.Calendar
import kotlin.math.hypot

enum class SlideMode { CENTER, CANCEL, EDIT }

/**
 * 输入栏两种模式（对齐微信 8.0.80）：
 * - TRANSCRIBE「可按住转文字」：点按打字；长按语音转文字；左滑取消；右滑转成文字可编辑。
 * - VOICE「按住说话」：长按录音，松手直接存语音文件；左滑取消；右滑语音转文字。
 */
enum class InputMode { TRANSCRIBE, VOICE }

/** 记录列表的两种浏览方式：普通列表 / 按日期分组的时间轴。 */
enum class ViewMode { LIST, TIMELINE }

private const val LONG_PRESS_MS = 260L
// 强调色改为读当前主题配色（CompositionLocal），41 处调用点无需改动
@get:Composable private val Accent: Color get() = LocalPalette.current.accent
@get:Composable private val AccentBright: Color get() = LocalPalette.current.accentBright
@get:Composable private val Secondary: Color get() = LocalPalette.current.secondary
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
    var heroPinned by remember { mutableStateOf(settingsStore.heroPinned) }
    var themeColor by remember { mutableStateOf(settingsStore.themeColor) }
    var customAccent by remember { mutableStateOf(settingsStore.customAccent) }
    val systemDark = isSystemInDarkTheme()
    val dark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val palette = AppPalettes.from(themeColor, customAccent)
    // Provider 必须在 colorScheme 之外：AccentBright / Accent 现在读 LocalPalette
    CompositionLocalProvider(LocalPalette provides palette) {
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
                heroPinned = heroPinned,
                onHeroPinnedChange = { value ->
                    settingsStore.heroPinned = value
                    heroPinned = value
                },
                themeColor = themeColor,
                onThemeColorChange = { value ->
                    settingsStore.themeColor = value
                    themeColor = value
                },
                customAccent = customAccent,
                onCustomAccentChange = { value ->
                    settingsStore.customAccent = value
                    customAccent = value
                },
            )
        }
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
    heroPinned: Boolean,
    onHeroPinnedChange: (Boolean) -> Unit,
    themeColor: String,
    onThemeColorChange: (String) -> Unit,
    customAccent: String,
    onCustomAccentChange: (String) -> Unit,
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
    // 输入栏默认「可按住转文字」（微信 8.0.80 同款）：不含可聚焦的 EditText，长按不弹键盘。
    // textMode 才是真正打字；inputMode 在「可按住转文字 / 按住说话」之间由左下角按钮切换。
    var textMode by remember { mutableStateOf(false) }
    // 文字多时进全屏编辑页：草稿非空时点输入栏直接进入，不必挤在单行输入框里改
    var fullEditorOpen by remember { mutableStateOf(false) }
    // 本机文件浏览器：null 关闭；值为「记录文件」或「语音目录」的完整路径
    var fileBrowserPath by remember { mutableStateOf<String?>(null) }
    var fileBrowserReturnToSettings by remember { mutableStateOf(false) }
    var inputMode by remember { mutableStateOf(InputMode.TRANSCRIBE) }
    var status by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    var slideMode by remember { mutableStateOf(SlideMode.CENTER) }
    // 手指相对起点的水平位移（px），驱动录音面板的跟手反馈
    var slideDx by remember { mutableStateOf(0f) }
    var liveText by remember { mutableStateOf("") }
    var elapsedSec by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    // v2.5：右上角菜单展开状态；标题点击滚动方向（true 下一次滚到底）
    var appMenuOpen by remember { mutableStateOf(false) }
    var titleScrollToBottom by remember { mutableStateOf(true) }
    var pendingDelete by remember { mutableStateOf<Note?>(null) }
    var pendingEdit by remember { mutableStateOf<Note?>(null) }
    // 长按记录弹出的操作面板；null 表示不显示
    var actionNote by remember { mutableStateOf<Note?>(null) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var transcribingId by remember { mutableStateOf<String?>(null) }
    var multiSelect by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var confirmBatchDelete by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var dataStats by remember { mutableStateOf(noteStore.stats()) }
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

    // ===== v2.0 新增功能的界面状态 =====
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var activeTag by remember { mutableStateOf<String?>(null) }
    var trashOpen by remember { mutableStateOf(false) }
    var trashNotes by remember { mutableStateOf(noteStore.trash()) }
    var confirmEmptyTrash by remember { mutableStateOf(false) }
    var tagTarget by remember { mutableStateOf<Note?>(null) }
    var tagDraft by remember { mutableStateOf("") }
    var colorPickerOpen by remember { mutableStateOf(false) }
    var ttsSpeaking by remember { mutableStateOf(false) }

    // ===== v2.1 关于/使用手册/内置更新 =====
    var showAbout by remember { mutableStateOf(false) }
    var updateChecking by remember { mutableStateOf(false) }
    var updateDownloading by remember { mutableStateOf(false) }
    var updateProgress by remember { mutableStateOf(0) }
    var updateInfo by remember { mutableStateOf<UpdateManager.ReleaseInfo?>(null) }

    // ===== v2.4：提醒 / 摘要 / 私密 / 定位 / 导出 / 隐私锁 =====
    var reminderTarget by remember { mutableStateOf<Note?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingReminderMillis by remember { mutableStateOf(0L) }
    var summarizingId by remember { mutableStateOf<String?>(null) }
    // v2.6：摘要结果/失败原因用弹窗明确呈现，不再靠一闪而过的 Toast
    var summaryDialog by remember { mutableStateOf("") }
    var showTextExport by remember { mutableStateOf(false) }
    var exportDone by remember { mutableStateOf("") }
    var showLockSetup by remember { mutableStateOf(false) }
    var lockPinInput by remember { mutableStateOf("") }
    var lockMsg by remember { mutableStateOf("") }
    // 新设置项的界面状态（与 SettingsStore 双向同步）
    var autoBackup by remember { mutableStateOf(settingsStore.autoBackup) }
    var headsetControl by remember { mutableStateOf(settingsStore.headsetControl) }
    var locationEnabled by remember { mutableStateOf(settingsStore.locationEnabled) }
    var lockEnabled by remember { mutableStateOf(settingsStore.lockEnabled) }
    var lockBiometric by remember { mutableStateOf(settingsStore.lockBiometric) }
    var hidePrivate by remember { mutableStateOf(settingsStore.hidePrivate) }
    var summaryModelInput by remember { mutableStateOf(settingsStore.summaryModel) }
    var privateRevealed by remember { mutableStateOf(false) }
    val appVersion = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "-" }
            .getOrDefault("-")
    }

    // 系统 TTS：文字朗读，与语音播放互不干扰（触发时互相停止）
    val tts = remember { TtsSpeaker(context) }
    DisposableEffect(Unit) {
        onDispose { tts.release() }
    }
    tts.onSpeakingChange = { ttsSpeaking = it }

    // 先按标签筛选，再按关键词搜索：两者可叠加使用
    val keyword = searchQuery.trim()
    val tagFilter = activeTag
    val tagScoped = if (tagFilter == null) notes else notes.filter { tagFilter in it.tags }
    val visibleNotes = if (keyword.isBlank()) {
        tagScoped
    } else {
        tagScoped.filter { note ->
            note.text.contains(keyword, ignoreCase = true) ||
                (note.hasAudio && "语音".contains(keyword, ignoreCase = true)) ||
                formatRecordTime(note.time).contains(keyword, ignoreCase = true)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        status = if (granted) "已获得麦克风权限，按住输入栏说话" else "需要麦克风权限才能录音"
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        status = if (granted) "已获得定位权限，可用「位置」标记记录" else "需要定位权限才能标记位置"
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 通知权限仅影响提醒展示，不阻断其他功能 */ }

    // Android 13+ 首次请求通知权限，保证提醒能弹出来
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        ReminderManager.ensureChannel(context)
        // v2.5 兜底：系统可能已清掉闹钟，按本机记录重建所有未到期提醒
        ReminderManager.rescheduleAll(context, noteStore.all())
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
        trashNotes = noteStore.trash()
    }

    /** 导入备份后，把设置相关的界面状态同步为本机最新值。 */
    fun reloadSettingsState() {
        apiKeyInput = settingsStore.apiKey
        workspaceInput = settingsStore.workspaceId
        modelInput = settingsStore.model
        onThemeChange(settingsStore.theme)
        onThemeColorChange(settingsStore.themeColor)
        onCustomAccentChange(settingsStore.customAccent)
        onHeroPinnedChange(settingsStore.heroPinned)
        summaryModelInput = settingsStore.summaryModel
        autoBackup = settingsStore.autoBackup
        headsetControl = settingsStore.headsetControl
        locationEnabled = settingsStore.locationEnabled
        lockEnabled = settingsStore.lockEnabled
        hidePrivate = settingsStore.hidePrivate
    }

    // 备份：系统文件选择器负责选保存位置 / 选备份包，App 只读写选中的 URI
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        uri?.let {
            scope.launch {
                val result = runCatching { BackupManager.export(context, it, noteStore, settingsStore) }
                if (result.isSuccess) {
                    val r = result.getOrThrow()
                    Toast.makeText(
                        context,
                        "已导出 ${r.noteCount} 条记录 · ${r.audioCount} 段语音",
                        Toast.LENGTH_LONG,
                    ).show()
                } else {
                    Toast.makeText(context, "导出失败：${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Android 9 及以下导出单个文件：系统另存为
    var pendingExportFile by remember { mutableStateOf<File?>(null) }
    val saveAsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*"),
    ) { uri ->
        val file = pendingExportFile
        if (uri != null && file != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            }.onSuccess { Toast.makeText(context, "已保存到你选择的位置", Toast.LENGTH_LONG).show() }
        }
        pendingExportFile = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            scope.launch {
                val result = runCatching { BackupManager.import(context, it, noteStore, settingsStore) }
                if (result.isSuccess) {
                    val r = result.getOrThrow()
                    reloadSettingsState()
                    refreshData()
                    activeTag = null
                    Toast.makeText(
                        context,
                        "已恢复 ${r.noteCount} 条记录 · ${r.audioCount} 段语音",
                        Toast.LENGTH_LONG,
                    ).show()
                } else {
                    Toast.makeText(context, "导入失败：${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun exitMultiSelect() {
        multiSelect = false
        selectedIds = emptySet()
    }

    /** 点击设置页的本机路径：进 App 内文件浏览器，直接看到完整路径和文件，不再跳系统选择器。 */
    fun openPath(path: String, directory: Boolean) {
        if (!File(path).exists()) {
            Toast.makeText(context, "文件不存在或已被清理", Toast.LENGTH_SHORT).show()
            return
        }
        // 设置页在组合树的后面，会盖住浏览器；先关掉，返回时再恢复
        fileBrowserReturnToSettings = showSettings
        showSettings = false
        fileBrowserPath = path
    }

    fun closeFileBrowser() {
        fileBrowserPath = null
        if (fileBrowserReturnToSettings) showSettings = true
        fileBrowserReturnToSettings = false
    }

    /** 导出单个文件到「下载/随手说」，让用户能在系统文件管理器里找到。 */
    fun exportFile(file: File) {
        val mime = if (file.name.endsWith(".wav", true)) "audio/wav" else "application/json"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/随手说")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri == null) {
                Toast.makeText(context, "导出失败：无法写入下载目录", Toast.LENGTH_LONG).show()
                return
            }
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
            }.onSuccess {
                Toast.makeText(context, "已导出到 下载/随手说/${file.name}", Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(context, "导出失败：${it.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            // Android 9 及以下无 MediaStore.Downloads，走系统另存为
            pendingExportFile = file
            saveAsLauncher.launch(file.name)
        }
    }

    fun shareFile(file: File) {
        val uri = runCatching {
            androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull() ?: return
        val mime = if (file.name.endsWith(".wav", true)) "audio/wav" else "application/json"
        val send = Intent(Intent.ACTION_SEND).apply {
            setDataAndType(uri, mime)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(send, "分享文件")) }.onFailure {
            Toast.makeText(context, "没有可用的分享应用", Toast.LENGTH_SHORT).show()
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

    // 返回键优先级：关于 > 回收站 > 设置页 > 多选 > 搜索 > 打字模式 >（不拦截，交给系统退出）
    BackHandler(enabled = fileBrowserPath != null) {
        closeFileBrowser()
    }
    BackHandler(enabled = fileBrowserPath == null && showAbout) {
        showAbout = false
    }
    BackHandler(enabled = fileBrowserPath == null && !showAbout && trashOpen) {
        trashOpen = false
        refreshData()
    }
    BackHandler(enabled = fileBrowserPath == null && !showAbout && !trashOpen && showSettings) {
        showSettings = false
        refreshData()
    }
    BackHandler(enabled = fileBrowserPath == null && !showAbout && !trashOpen && !showSettings && multiSelect) {
        exitMultiSelect()
    }
    BackHandler(enabled = fileBrowserPath == null && !showAbout && !trashOpen && !showSettings && !multiSelect && searchOpen) {
        searchOpen = false
        searchQuery = ""
    }
    BackHandler(enabled = fileBrowserPath == null && !showAbout && !trashOpen && !showSettings && !multiSelect && !searchOpen && textMode) {
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

    // 定时备份开关变化时启用/取消后台任务
    LaunchedEffect(autoBackup) {
        if (autoBackup) AutoBackupWorker.enable(context) else AutoBackupWorker.disable(context)
    }

    fun refreshNotes() {
        notes = noteStore.all()
        NotesWidgetProvider.refreshAll(context)
    }

    /** 点标题栏「随手说」：到底部 ↔ 回顶部往复切换。 */
    fun toggleTitleScroll() {
        scope.launch {
            val lastIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
            // 列表 reverseLayout：index 0 在底部；时间轴正常布局：末项在底部
            val bottomIndex = if (viewMode == ViewMode.LIST) 0 else lastIndex
            val topIndex = if (viewMode == ViewMode.LIST) lastIndex else 0
            val target = if (titleScrollToBottom) bottomIndex else topIndex
            runCatching { listState.animateScrollToItem(target) }
            titleScrollToBottom = !titleScrollToBottom
        }
    }

    fun toggleSelected(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    /** 点按气泡：语音播放/停止（用当前倍速）；文字记录不再单击复制，改为双击。 */
    fun onBubbleClick(note: Note) {
        if (multiSelect) {
            toggleSelected(note.id)
            return
        }
        if (note.hasAudio) {
            val path = note.audioPath ?: return
            if (playingPath == path) {
                audioPlayer.stop()
                playingPath = null
            } else {
                tts.stop()
                audioPlayer.play(path, playbackSpeed, onFinished = { playingPath = null })
                playingPath = path
            }
        }
        // 纯文字记录单击不再复制，避免误触；复制统一走双击
    }

    /** 双击气泡：复制文字（纯文字与已转写语音都支持）。 */
    fun onBubbleDoubleClick(note: Note) {
        if (multiSelect) return
        val text = note.text
        if (text.isBlank() || text == "[语音消息]") return
        clipboard.setText(AnnotatedString(text))
        Toast.makeText(context, "已复制文字", Toast.LENGTH_SHORT).show()
    }

    /** 长按气泡：弹出操作面板（多选态下仍为切换选中）。 */
    fun onBubbleLongClick(note: Note) {
        if (multiSelect) {
            toggleSelected(note.id)
        } else {
            audioPlayer.stop()
            playingPath = null
            playbackSpeed = 1.0f
            actionNote = note
        }
    }

    /** 用倍速播放某条语音，供操作面板调用。 */
    fun playWithSpeed(note: Note, speed: Float) {
        val path = note.audioPath ?: return
        playbackSpeed = speed
        tts.stop()
        audioPlayer.stop()
        audioPlayer.play(path, speed, onFinished = { playingPath = null })
        playingPath = path
        Toast.makeText(context, "以 ${speed}x 播放", Toast.LENGTH_SHORT).show()
    }

    /** 朗读文字记录：先停语音播放，避免两种声音叠加。正在朗读时再点即为停止。 */
    fun readAloud(note: Note) {
        if (ttsSpeaking) {
            tts.stop()
            Toast.makeText(context, "已停止朗读", Toast.LENGTH_SHORT).show()
            return
        }
        audioPlayer.stop()
        playingPath = null
        val text = note.text.takeIf { it.isNotBlank() && it != "[语音消息]" }
        if (text == null) {
            Toast.makeText(context, "这条记录没有可朗读的文字", Toast.LENGTH_SHORT).show()
            return
        }
        if (!tts.speak(text)) {
            Toast.makeText(context, "语音朗读暂不可用，请检查系统中文语音包", Toast.LENGTH_LONG).show()
        }
    }

    /** 给记录添加或移除标签。 */
    fun toggleTag(note: Note, tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isBlank()) return
        val adding = trimmed !in note.tags
        val next = if (adding) note.tags + trimmed else note.tags - trimmed
        noteStore.setTags(note.id, next)
        refreshNotes()
        Toast.makeText(
            context,
            if (adding) "已添加标签 #$trimmed" else "已移除标签 #$trimmed",
            Toast.LENGTH_SHORT,
        ).show()
    }

    /**
     * 把已保存的语音重新转成文字：读取 WAV 的 PCM 数据，分块送给百炼实时 ASR。
     * 全程离线失败时给出提示，不改动原记录。
     */
    suspend fun transcribeExisting(note: Note) {
        val path = note.audioPath
        if (path.isNullOrBlank()) return
        if (settingsStore.apiKey.isBlank()) {
            Toast.makeText(context, "请先在设置里填写 API Key", Toast.LENGTH_SHORT).show()
            return
        }
        transcribingId = note.id
        val asr = DashScopeAsr(
            apiKey = settingsStore.apiKey,
            wsUrl = settingsStore.wsUrl,
            model = settingsStore.model.ifBlank { SettingsStore.DEFAULT_MODEL },
            onPartial = {},
        )
        val result = try {
            if (!withTimeout(6_000) { asr.connect() }) {
                AsrResult.Failure("连接转写服务失败")
            } else {
                val bytes = File(path).readBytes()
                if (bytes.size > 44) {
                    var offset = 44
                    while (offset < bytes.size) {
                        val end = minOf(offset + PcmRecorder.FRAME_BYTES, bytes.size)
                        asr.sendAudio(bytes.copyOfRange(offset, end))
                        offset = end
                        kotlinx.coroutines.delay(20)
                    }
                }
                withTimeout(20_000) { asr.finish() }
            }
        } catch (e: Exception) {
            AsrResult.Failure(e.message ?: "转写失败")
        } finally {
            asr.cancel()
        }
        transcribingId = null
        when (result) {
            is AsrResult.Success -> {
                val text = result.text
                if (text.isBlank()) {
                    Toast.makeText(context, "没能识别出文字", Toast.LENGTH_SHORT).show()
                } else {
                    noteStore.updateText(note.id, text)
                    refreshNotes()
                    Toast.makeText(context, "已转成文字", Toast.LENGTH_SHORT).show()
                }
            }
            is AsrResult.Failure -> Toast.makeText(context, "转写失败：${result.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** 本地摘要：没有有效 API Key 时的兜底，取首句，保证功能永远可用。 */
    fun localSummary(text: String): String {
        val clean = text.trim().replace(Regex("\\s+"), " ")
        if (clean.isBlank() || clean == "[语音消息]") return "这条记录没有可摘要的文字"
        val firstSentence = clean.split(Regex("[。！？!?\\n]")).firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        val base = (firstSentence.ifBlank { clean })
        return if (base.length > 42) base.take(42) + "…" else base
    }

    /** AI 摘要：有有效 Key 走百炼；没有 / 失败则本地兜底，并用弹窗明确呈现结果。 */
    suspend fun summarizeNote(note: Note) {
        summarizingId = note.id
        val key = settingsStore.apiKey.trim()
        // sk-test123 之类的占位 key 直接走本地，不发无效请求
        val keyLooksReal = key.startsWith("sk-") && key.length > 20
        var usedLocal = false
        val result = if (!keyLooksReal) {
            usedLocal = true
            SummaryManager.Result.Success(localSummary(note.text))
        } else {
            SummaryManager.summarize(apiKey = key, model = settingsStore.summaryModel, text = note.text)
        }
        summarizingId = null
        when (result) {
            is SummaryManager.Result.Success -> {
                noteStore.setSummary(note.id, result.text)
                refreshNotes()
                NotesWidgetProvider.refreshAll(context)
                summaryDialog = if (usedLocal) {
                    "✦ ${result.text}\n\n（本机摘要：当前未填写有效的百炼 API Key。" +
                        "想要 AI 提炼要点，可到「设置」里填入真实 Key。）"
                } else {
                    "✦ ${result.text}"
                }
            }
            is SummaryManager.Result.Failure -> {
                // 网络/服务失败也降级为本地摘要，保证用户点了就有结果
                val fallback = localSummary(note.text)
                noteStore.setSummary(note.id, fallback)
                refreshNotes()
                summaryDialog = "AI 摘要失败：${result.message}\n\n已改用本机摘要：\n✦ $fallback"
            }
        }
    }

    /** 设置/取消提醒：先选日期再选时间，最后调度闹钟。 */
    fun applyReminder(note: Note, millis: Long) {
        noteStore.setReminder(note.id, millis)
        ReminderManager.schedule(context, note.id, note.text, millis)
        refreshNotes()
        Toast.makeText(
            context,
            if (millis > 0) "已设置提醒" else "已取消提醒",
            Toast.LENGTH_SHORT,
        ).show()
    }

    /** 给记录附加当前位置（需定位权限，无权限时提示）。 */
    fun tagLocation(note: Note) {
        val place = LocationHelper.lastPlace(context)
        if (place == null) {
            Toast.makeText(context, "未获取到位置，请先在系统设置里允许定位", Toast.LENGTH_LONG).show()
            return
        }
        noteStore.setLocation(note.id, place.latitude, place.longitude, place.label)
        refreshNotes()
        Toast.makeText(context, "已标记位置：${place.label}", Toast.LENGTH_SHORT).show()
    }

    /** 切换私密标记。 */
    fun togglePrivate(note: Note) {
        val next = !note.isPrivate
        noteStore.setPrivate(note.id, next)
        refreshNotes()
        Toast.makeText(context, if (next) "已标记为私密" else "已取消私密", Toast.LENGTH_SHORT).show()
    }

    /** 导出文字：生成 Markdown / CSV 到下载目录。 */
    fun exportText(kind: String) {
        val all = noteStore.all()
        val stamp = ExportManager.timestamp()
        val path = if (kind == "md") {
            ExportManager.writeToDownloads(
                context, "随手说记录-$stamp.md", "text/markdown", ExportManager.markdown(all),
            )
        } else {
            ExportManager.writeToDownloads(
                context, "随手说记录-$stamp.csv", "text/csv", ExportManager.csv(all),
            )
        }
        exportDone = if (path != null) "已导出 ${all.size} 条到 $path" else "导出失败，请检查存储权限"
    }

    fun copySelected() {
        val picked = notes.filter { it.id in selectedIds && it.text.isNotBlank() && it.text != "[语音消息]" }
        if (picked.isEmpty()) {
            Toast.makeText(context, "所选记录没有可复制的文字", Toast.LENGTH_SHORT).show()
            return
        }
        clipboard.setText(AnnotatedString(picked.joinToString("\n") { it.text }))
        Toast.makeText(context, "已复制 ${picked.size} 条文字", Toast.LENGTH_SHORT).show()
        exitMultiSelect()
    }

    fun deleteSelected() {
        val removed = notes.filter { it.id in selectedIds }
        if (playingPath != null && removed.any { it.audioPath == playingPath }) {
            audioPlayer.stop()
            playingPath = null
        }
        noteStore.moveManyToTrash(selectedIds)
        refreshNotes()
        exitMultiSelect()
        Toast.makeText(context, "已移入回收站 ${removed.size} 条", Toast.LENGTH_SHORT).show()
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
            "正在录音 · 左滑取消，右滑转文字"
        } else {
            "正在录音 · 右滑可试转文字（当前未连接）"
        }
    }

    suspend fun stopRecording(mode: SlideMode) {
        val recorder = recorderRef.value ?: return
        val asr = asrRef.value
        val wavFile = currentAudioFile
        val modeAtStart = inputMode // 以按下时的模式为准，避免录音过程中切换造成歧义
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

        // 「按住说话」+ 原地松手：只保存语音文件，不转文字
        if (modeAtStart == InputMode.VOICE && mode == SlideMode.CENTER) {
            asr?.cancel()
            saveNote("", true, wavFile?.absolutePath, duration)
            recording = false
            busy = false
            status = "语音已保存到本机"
            liveText = ""
            elapsedSec = 0
            return
        }

        // 其余情况都要转文字：
        // - 可按住转文字 + 原地松手 → 直接保存文字记录
        // - 可按住转文字 + 右滑 / 按住说话 + 右滑 → 转成文字进编辑框
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
            // 右滑：转成文字进入编辑框，可改后再保存；语音文件一并暂存
            draft = spoken
            textMode = true
            wavFile?.let { pendingAudio = it.absolutePath to duration }
            status = if (result is AsrResult.Failure) {
                "转写失败，语音已保留，可手动输入文字"
            } else {
                "已转成文字，可编辑后点发送"
            }
        } else {
            // 可按住转文字 + 原地松手：直接记一条文字记录
            if (spoken.isBlank()) wavFile?.delete()
            saveNote(spoken, true, if (spoken.isBlank()) null else wavFile?.absolutePath, duration)
            status = if (spoken.isBlank()) {
                "没听清，请再试一次"
            } else {
                "已转成文字并记下"
            }
        }
        liveText = ""
        elapsedSec = 0
    }

    // 耳机线控 / 桌面小组件的录音事件：按一下切换，实现免手动录音
    LaunchedEffect(Unit) {
        AppBus.events.collect { event ->
            when (event) {
                AppBus.TOGGLE_RECORD -> {
                    if (recording) stopRecording(SlideMode.CENTER) else startRecording()
                }
                AppBus.START_RECORD -> if (!recording) startRecording()
                AppBus.STOP_RECORD -> if (recording) stopRecording(SlideMode.CENTER)
            }
        }
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
                    } else if (multiSelect) {
                        Text("已选 ${selectedIds.size} 项", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    } else {
                        Row(
                            modifier = Modifier.clickable { toggleTitleScroll() },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "随手说",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dark) AccentBright else Accent,
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "滚动到底部或顶部",
                                tint = if (dark) AccentBright else Accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                actions = {
                    if (multiSelect) {
                        IconButton(onClick = {
                            selectedIds = if (selectedIds.size == visibleNotes.size) emptySet() else visibleNotes.map { it.id }.toSet()
                        }) {
                            Icon(AppIcons.SelectAll, contentDescription = "全选/取消全选", tint = textColor)
                        }
                        IconButton(onClick = { exitMultiSelect() }) {
                            Icon(Icons.Default.Close, contentDescription = "退出多选", tint = textColor)
                        }
                    } else {
                        if (!searchOpen) {
                            IconButton(onClick = {
                                exitTextMode()
                                activeTag = null
                                viewMode = if (viewMode == ViewMode.LIST) ViewMode.TIMELINE else ViewMode.LIST
                            }) {
                                Icon(
                                    AppIcons.Calendar,
                                    contentDescription = if (viewMode == ViewMode.LIST) "时间轴视图" else "列表视图",
                                    tint = if (viewMode == ViewMode.TIMELINE) Accent else textColor,
                                )
                            }
                            IconButton(onClick = {
                                exitTextMode()
                                multiSelect = true
                                selectedIds = emptySet()
                            }) {
                                Icon(AppIcons.SelectAll, contentDescription = "多选", tint = textColor)
                            }
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
                            exitMultiSelect()
                            searchOpen = false
                            searchQuery = ""
                            showSettings = true
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "设置", tint = textColor)
                        }
                    }
                    // v2.5：统一菜单入口
                    Box {
                        IconButton(onClick = { appMenuOpen = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单", tint = textColor)
                        }
                        DropdownMenu(
                            expanded = appMenuOpen,
                            onDismissRequest = { appMenuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (viewMode == ViewMode.LIST) "时间轴视图" else "列表视图") },
                                onClick = {
                                    appMenuOpen = false
                                    exitTextMode()
                                    activeTag = null
                                    viewMode = if (viewMode == ViewMode.LIST) ViewMode.TIMELINE else ViewMode.LIST
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("回收站 · ${trashNotes.size}") },
                                onClick = {
                                    appMenuOpen = false
                                    trashNotes = noteStore.trash()
                                    showSettings = false
                                    trashOpen = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("导出文字（MD / CSV）") },
                                onClick = { appMenuOpen = false; showTextExport = true },
                            )
                            DropdownMenuItem(
                                text = { Text("本机文件浏览器") },
                                onClick = {
                                    appMenuOpen = false
                                    exitTextMode()
                                    showSettings = false
                                    fileBrowserReturnToSettings = false
                                    fileBrowserPath = noteStore.filesDir.absolutePath
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("检查更新") },
                                onClick = {
                                    appMenuOpen = false
                                    updateChecking = true
                                    scope.launch {
                                        val result = UpdateManager.checkLatest()
                                        updateChecking = false
                                        result.onSuccess { info ->
                                            if (UpdateManager.isNewer(info.version, appVersion)) {
                                                updateInfo = info
                                            } else {
                                                Toast.makeText(context, "已是最新版本（$appVersion）", Toast.LENGTH_SHORT).show()
                                            }
                                        }.onFailure { e ->
                                            Toast.makeText(context, "检查失败：${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("关于与帮助") },
                                onClick = { appMenuOpen = false; showAbout = true },
                            )
                            DropdownMenuItem(
                                text = { Text("设置") },
                                onClick = {
                                    appMenuOpen = false
                                    apiKeyInput = settingsStore.apiKey
                                    workspaceInput = settingsStore.workspaceId
                                    modelInput = settingsStore.model
                                    dataStats = noteStore.stats()
                                    exitTextMode()
                                    exitMultiSelect()
                                    searchOpen = false
                                    searchQuery = ""
                                    showSettings = true
                                },
                            )
                        }
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
                // 置顶开启（或空列表）时，统计卡固定在列表上方
                if (heroPinned || visibleNotes.isEmpty()) {
                    HomeHero(
                        noteCount = notes.size,
                        audioCount = notes.count { it.hasAudio },
                        dark = dark,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                // 标签筛选条：横向滚动，点一下只看该标签，再点一下取消
                val tagOptions = noteStore.allTags()
                if (tagOptions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TagChip("全部", activeTag == null, dark) { activeTag = null }
                        tagOptions.forEach { tag ->
                            Spacer(Modifier.width(6.dp))
                            TagChip(tag, activeTag == tag, dark) {
                                activeTag = if (activeTag == tag) null else tag
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                }

                // 列表视图与时间轴视图共用同一套气泡渲染，避免两处逻辑分叉
                val renderNote: @Composable (Note) -> Unit = { note ->
                    NoteBubble(
                        note = note,
                        dark = dark,
                        isPlaying = playingPath == note.audioPath && note.audioPath != null,
                        multiSelect = multiSelect,
                        selected = note.id in selectedIds,
                        onClick = { onBubbleClick(note) },
                        onLongClick = { onBubbleLongClick(note) },
                        onDoubleClick = { onBubbleDoubleClick(note) },
                        menuOpen = actionNote?.id == note.id,
                        onDismissMenu = { actionNote = null },
                        hidePrivate = hidePrivate,
                        privateRevealed = privateRevealed,
                        onRevealPrivate = { privateRevealed = true },
                        onOpenReminder = {
                            reminderTarget = note
                            showDatePicker = true
                        },
                        onOpenTag = {
                            actionNote = null
                            tagTarget = note
                            tagDraft = ""
                        },
                        menu = {
                            NoteActionMenu(
                                note = note,
                                dark = dark,
                                transcribing = transcribingId == note.id,
                                ttsSpeaking = ttsSpeaking && actionNote?.id == note.id,
                                onDismiss = { actionNote = null },
                                onPlay = { speed -> actionNote = null; playWithSpeed(note, speed) },
                                onTranscribe = { actionNote = null; scope.launch { transcribeExisting(note) } },
                                onRead = { actionNote = null; readAloud(note) },
                                onTag = { tagTarget = note; tagDraft = "" },
                                onCopy = {
                                    actionNote = null
                                    clipboard.setText(AnnotatedString(note.text))
                                    Toast.makeText(context, "已复制文字", Toast.LENGTH_SHORT).show()
                                },
                                onEdit = { actionNote = null; editText = note.text; pendingEdit = note },
                                onMultiSelect = { actionNote = null; multiSelect = true; selectedIds = setOf(note.id) },
                                onDelete = { actionNote = null; pendingDelete = note },
                                summarizing = summarizingId == note.id,
                                onRemind = {
                                    actionNote = null
                                    reminderTarget = note
                                    showDatePicker = true
                                },
                                onSummarize = { actionNote = null; scope.launch { summarizeNote(note) } },
                                onPrivate = { actionNote = null; togglePrivate(note) },
                                onLocation = { actionNote = null; tagLocation(note) },
                            )
                        },
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
            if (viewMode == ViewMode.TIMELINE && visibleNotes.isNotEmpty()) {
                // 时间轴：按日期分组，顶部日期吸顶，同样支持点空白收键盘
                TimelineNotes(
                    notes = visibleNotes.asReversed(),
                    listState = listState,
                    renderNote = renderNote,
                    showHero = !heroPinned,
                    hero = {
                        HomeHero(
                            noteCount = notes.size,
                            audioCount = notes.count { it.hasAudio },
                            dark = dark,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    },
                    modifier = Modifier.pointerInput(textMode, searchOpen) {
                        detectTapGestures {
                            if (textMode) exitTextMode()
                            if (searchOpen) {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        }
                    },
                )
            } else if (visibleNotes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .pointerInput(textMode, searchOpen) {
                            detectTapGestures {
                                if (textMode) exitTextMode()
                                if (searchOpen) {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    WaveGlyph(color = Accent.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (keyword.isNotBlank()) "没有找到匹配的记录"
                        else if (activeTag != null) "「$activeTag」下还没有记录"
                        else "按住输入栏，说话即记录",
                        fontSize = 16.sp,
                        color = textColor,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (keyword.isNotBlank()) "换个关键词试试，共 ${notes.size} 条记录"
                        else if (activeTag != null) "点上方「全部」返回所有记录"
                        else "语音与文字都保存在本机",
                        fontSize = 12.sp,
                        color = subColor,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(textMode, searchOpen) {
                            detectTapGestures {
                                if (textMode) exitTextMode()
                                if (searchOpen) {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            }
                        },
                    contentPadding = PaddingValues(top = 12.dp, bottom = 116.dp),
                ) {
                    items(visibleNotes.asReversed(), key = { it.id }) { note ->
                        renderNote(note)
                    }
                    // 不置顶时：统计卡作为列表最后一项，reverseLayout 下显示在最顶端，随列表滚动
                    if (!heroPinned) {
                        item(key = "home_hero") {
                            HomeHero(
                                noteCount = notes.size,
                                audioCount = notes.count { it.hasAudio },
                                dark = dark,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            }
                }
            }

            if (multiSelect) {
                // 多选操作栏
                val barAccent = if (dark) AccentBright else Accent
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(50), clip = true)
                        .clip(RoundedCornerShape(50))
                        .background(surface)
                        .border(1.dp, if (dark) Color(0x22FFFFFF) else Color(0x12000000), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TextButton(onClick = {
                        selectedIds = if (selectedIds.size == visibleNotes.size) emptySet() else visibleNotes.map { it.id }.toSet()
                    }) {
                        Text(if (selectedIds.size == visibleNotes.size) "取消全选" else "全选", color = barAccent)
                    }
                    TextButton(onClick = { copySelected() }) {
                        Icon(AppIcons.ContentPaste, contentDescription = null, tint = barAccent, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("复制", color = barAccent)
                    }
                    TextButton(onClick = { confirmBatchDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE64340), modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("删除", color = Color(0xFFE64340))
                    }
                }
            } else {
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
                            textMode = true
                            status = ""
                        },
                        onDismiss = { clipboardText = null },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                val pillBg = if (dark) Color(0x141B2D) else Color(0xFFFFFFFF)
                // 模式强调色平滑过渡：可按住转文字=薄荷绿，按住说话=紫罗兰
                val smoothSpring = spring<Float>(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioNoBouncy,
                )
                val modeAccent by animateColorAsState(
                    targetValue = if (inputMode == InputMode.TRANSCRIBE) {
                        if (dark) AccentBright else Accent
                    } else {
                        if (dark) Color(0xFFB4A5FF) else Secondary
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
                    label = "modeAccent",
                )
                val modeIconRotation by animateFloatAsState(
                    targetValue = if (inputMode == InputMode.TRANSCRIBE) 0f else 180f,
                    animationSpec = smoothSpring,
                    label = "modeIconRotation",
                )
                val waveCircleBg = modeAccent.copy(alpha = if (dark) 0.16f else 0.11f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(50), clip = true)
                        .clip(RoundedCornerShape(50))
                        .background(surface)
                        .border(
                            1.dp,
                            if (dark) Color(0x22FFFFFF) else Color(0x12000000),
                            RoundedCornerShape(50),
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 录音手势：仅语音模式套用。打字模式让 TextField 独占触摸，
                    // 否则手势会在 Initial 阶段消费 down，导致收起按钮/光标点击全部失效。
                    val recordModifier = recordGesture(
                        thresholdPx = slideThresholdPx,
                        onTap = { x ->
                            if (x <= waveZonePx) {
                                // 左下角按钮：来回切换「可按住转文字 ⇄ 按住说话」
                                if (textMode) {
                                    exitTextMode()
                                } else {
                                    inputMode = if (inputMode == InputMode.TRANSCRIBE) {
                                        InputMode.VOICE
                                    } else {
                                        InputMode.TRANSCRIBE
                                    }
                                }
                                status = ""
                            } else {
                                // 草稿已有时直接进全屏编辑；空草稿才切到单行打字模式
                                if (draft.isNotBlank()) {
                                    keyboardController?.hide()
                                    fullEditorOpen = true
                                } else {
                                    textMode = true
                                }
                                status = ""
                            }
                        },
                        onStart = {
                            finishJob?.cancel()
                            finishJob = scope.launch { startRecording() }
                        },
                        onSlideModeChange = { slideMode = it },
                        onSlideDx = { slideDx = it },
                        onEnd = { mode ->
                            slideDx = 0f
                            finishJob = scope.launch { stopRecording(mode) }
                        },
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(50))
                            .background(pillBg)
                            .then(if (textMode) Modifier else recordModifier),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(start = 5.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(waveCircleBg)
                                .rotate(modeIconRotation)
                                .then(
                                    if (textMode) Modifier.clickable {
                                        exitTextMode()
                                        status = ""
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            // 图标随模式淡入淡出：声波=转文字，话筒=按住说话
                            AnimatedContent(
                                targetState = inputMode,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(160)) + scaleIn(initialScale = 0.6f, animationSpec = smoothSpring)) togetherWith
                                        (fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.6f, animationSpec = smoothSpring))
                                },
                                label = "modeIcon",
                            ) { mode ->
                                if (mode == InputMode.TRANSCRIBE) {
                                    WaveGlyph(color = modeAccent, modifier = Modifier.size(21.dp))
                                } else {
                                    Icon(
                                        AppIcons.Mic,
                                        contentDescription = "按住说话",
                                        tint = modeAccent,
                                        modifier = Modifier.size(21.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        if (textMode) {
                            // 用 BasicTextField 自绘占位符：占位文字右移与光标错开，避免光标压在首字上显示不全
                            BasicTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .focusRequester(clipFocusRequester),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = textColor),
                                // 空字段时隐藏光标，避免竖线压住占位文字；开始输入后恢复
                                cursorBrush = SolidColor(if (draft.isEmpty()) Color.Transparent else modeAccent),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { exitTextMode() }),
                                decorationBox = { inner ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        if (draft.isEmpty()) {
                                            Text("输入文字…", fontSize = 14.sp, color = subColor, maxLines = 1)
                                        }
                                        inner()
                                    }
                                },
                            )
                            // 全屏编辑：长文字在这里改，不受单行高度限制
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    fullEditorOpen = true
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "全屏编辑", tint = modeAccent, modifier = Modifier.size(18.dp))
                            }
                            // 收起键盘按钮移到输入区外部独立占位，避免挤压输入文字
                            IconButton(
                                onClick = { exitTextMode() },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "收起键盘", tint = subColor, modifier = Modifier.size(20.dp))
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                val placeholderText = if (draft.isNotBlank()) {
                                    draft
                                } else if (inputMode == InputMode.TRANSCRIBE) {
                                    "可按住 转文字"
                                } else {
                                    "按住说话"
                                }
                                AnimatedContent(
                                    targetState = placeholderText,
                                    transitionSpec = {
                                        (fadeIn(animationSpec = tween(180)) + slideInHorizontally(animationSpec = spring<androidx.compose.ui.unit.IntOffset>(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) { it / 5 }) togetherWith
                                            (fadeOut(animationSpec = tween(120)) + slideOutHorizontally(animationSpec = spring<androidx.compose.ui.unit.IntOffset>(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) { -it / 5 })
                                    },
                                    label = "placeholder",
                                ) { txt ->
                                    Text(
                                        text = txt,
                                        fontSize = 14.sp,
                                        color = if (draft.isBlank()) subColor else textColor,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                    if (draft.isNotBlank() || pendingAudio != null) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(modeAccent.copy(alpha = 0.16f))
                                .clickable { sendDraft() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "保存", tint = modeAccent, modifier = Modifier.size(20.dp))
                        }
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
                    inputMode = inputMode,
                    liveText = liveText,
                    elapsedSec = elapsedSec,
                    dark = dark,
                    slideDx = slideDx,
                    slideThreshold = with(density) { 80.dp.toPx() },
                )
            }

        }
    }

    // 全屏文字编辑：无灰罩、铺满屏幕，多行 TextField 直接改草稿
    if (fullEditorOpen) {
        Dialog(
            onDismissRequest = { fullEditorOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            FullScreenTextEditor(
                draft = draft,
                onDraftChange = { draft = it },
                accent = if (dark) AccentBright else Accent,
                dark = dark,
                onDone = { fullEditorOpen = false },
            )
        }
    }

    // 本机文件浏览器：显示完整路径与文件列表，可导出到「下载/随手说」
    fileBrowserPath?.let { path ->
        FileBrowserPage(
            path = path,
            dark = dark,
            onBack = { closeFileBrowser() },
            onExport = { exportFile(it) },
            onExportAll = { files -> files.forEach { exportFile(it) } },
            onShare = { shareFile(it) },
        )
    }

    // 回收站全屏页
    if (trashOpen) {
        TrashPage(
            dark = dark,
            notes = trashNotes,
            onBack = {
                trashOpen = false
                refreshData()
            },
            onRestore = { id ->
                noteStore.restore(id)
                refreshNotes()
                refreshData()
                Toast.makeText(context, "已恢复到记录列表", Toast.LENGTH_SHORT).show()
            },
            onDeleteForever = { id ->
                noteStore.delete(id)
                refreshData()
                Toast.makeText(context, "已彻底删除", Toast.LENGTH_SHORT).show()
            },
            onEmpty = { confirmEmptyTrash = true },
        )
    }

    // 标签编辑：已有标签点选切换，也可以直接输入新标签
    tagTarget?.let { note ->
        TagDialog(
            note = note,
            allTags = noteStore.allTags(),
            dark = dark,
            draft = tagDraft,
            onDraftChange = { tagDraft = it },
            onToggleTag = { tag ->
                toggleTag(note, tag)
                // 重新取回最新对象，让弹窗里的勾选状态立刻刷新
                tagTarget = noteStore.snapshot().firstOrNull { it.id == note.id } ?: note
            },
            onDismiss = { tagTarget = null },
        )
    }

    // 自定义主题色取色器
    if (colorPickerOpen) {
        ColorPickerDialog(
            dark = dark,
            initial = customAccent,
            onPick = { value ->
                onCustomAccentChange(value)
                onThemeColorChange(AppPalettes.CUSTOM_KEY)
                colorPickerOpen = false
            },
            onDismiss = { colorPickerOpen = false },
        )
    }

    // 清空回收站确认
    if (confirmEmptyTrash) {
        AlertDialog(
            onDismissRequest = { confirmEmptyTrash = false },
            title = { Text("清空回收站？") },
            text = { Text("其中 ${trashNotes.size} 条记录及语音文件将被永久删除，不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    noteStore.emptyTrash()
                    confirmEmptyTrash = false
                    refreshData()
                    Toast.makeText(context, "回收站已清空", Toast.LENGTH_SHORT).show()
                }) { Text("清空", color = Color(0xFFE64340)) }
            },
            dismissButton = { TextButton(onClick = { confirmEmptyTrash = false }) { Text("取消") } },
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
                        noteStore.moveToTrash(note.id)
                        refreshNotes()
                        pendingEdit = null
                        Toast.makeText(context, "已移入回收站", Toast.LENGTH_SHORT).show()
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
                    note.text.take(80) + "\n（移入回收站，30 天后自动清除，期间可恢复）",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    noteStore.moveToTrash(note.id)
                    refreshNotes()
                    if (playingPath == note.audioPath) {
                        audioPlayer.stop()
                        playingPath = null
                    }
                    pendingDelete = null
                    Toast.makeText(context, "已移入回收站", Toast.LENGTH_SHORT).show()
                }) { Text("移入回收站") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }

    // 批量删除确认
    if (confirmBatchDelete) {
        AlertDialog(
            onDismissRequest = { confirmBatchDelete = false },
            title = { Text("移入回收站 ${selectedIds.size} 条？") },
            text = { Text("所选记录将进入回收站，30 天后自动清除，期间可在设置里恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmBatchDelete = false
                    deleteSelected()
                }) { Text("移入回收站", color = Color(0xFFE64340)) }
            },
            dismissButton = { TextButton(onClick = { confirmBatchDelete = false }) { Text("取消") } },
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
            heroPinned = heroPinned,
            onHeroPinnedChange = onHeroPinnedChange,
            themeColor = themeColor,
            onThemeColorChange = onThemeColorChange,
            customAccent = customAccent,
            onCustomAccentChange = onCustomAccentChange,
            onCustomColor = { colorPickerOpen = true },
            trashCount = trashNotes.size,
            onOpenTrash = {
                trashNotes = noteStore.trash()
                showSettings = false
                trashOpen = true
            },
            onExport = {
                val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
                exportLauncher.launch("随手说备份_$stamp.zip")
            },
            onImport = { importLauncher.launch(arrayOf("application/zip", "*/*")) },
            stats = dataStats,
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
                settingsStore.summaryModel = summaryModelInput.trim().ifBlank { "qwen-plus" }
                showSettings = false
                status = "设置已保存"
            },
            onClearAll = { confirmClear = true },
            onClearAudio = {
                noteStore.clearAudioOnly()
                refreshData()
                Toast.makeText(context, "已清除语音文件，文字记录保留", Toast.LENGTH_SHORT).show()
            },
            appVersion = appVersion,
            updateChecking = updateChecking,
            onOpenAbout = { showAbout = true },
            onCheckUpdate = {
                updateChecking = true
                scope.launch {
                    val result = UpdateManager.checkLatest()
                    updateChecking = false
                    result.onSuccess { info ->
                        if (UpdateManager.isNewer(info.version, appVersion)) {
                            updateInfo = info
                        } else {
                            Toast.makeText(context, "已是最新版本（$appVersion）", Toast.LENGTH_SHORT).show()
                        }
                    }.onFailure { e ->
                        Toast.makeText(context, "检查更新失败：${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            // v2.4 新功能参数
            autoBackup = autoBackup,
            onAutoBackupChange = {
                autoBackup = it
                settingsStore.autoBackup = it
            },
            lastBackupTime = settingsStore.lastBackupTime,
            headsetControl = headsetControl,
            onHeadsetChange = {
                headsetControl = it
                settingsStore.headsetControl = it
            },
            locationEnabled = locationEnabled,
            onLocationChange = {
                locationEnabled = it
                settingsStore.locationEnabled = it
                if (it) {
                    // 开启定位时请求权限
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            lockEnabled = lockEnabled,
            onLockChange = {
                if (it && settingsStore.lockPinHash.isBlank()) {
                    showLockSetup = true
                } else {
                    lockEnabled = it
                    settingsStore.lockEnabled = it
                }
            },
            onLockSetup = { showLockSetup = true },
            onLockDisable = {
                lockEnabled = false
                settingsStore.lockEnabled = false
                Toast.makeText(context, "隐私锁已关闭", Toast.LENGTH_SHORT).show()
            },
            lockBiometric = lockBiometric,
            onLockBiometricChange = {
                lockBiometric = it
                settingsStore.lockBiometric = it
            },
            hidePrivate = hidePrivate,
            onHidePrivateChange = {
                hidePrivate = it
                settingsStore.hidePrivate = it
                if (!it) privateRevealed = false
            },
            summaryModelInput = summaryModelInput,
            onSummaryModelChange = { summaryModelInput = it },
            onOpenTextExport = { showTextExport = true; exportDone = "" },
        )
    }

    // 关于 / 使用手册全屏页
    if (showAbout) {
        AboutPage(
            dark = dark,
            appVersion = appVersion,
            onBack = { showAbout = false },
            onCheckUpdate = {
                updateChecking = true
                scope.launch {
                    val result = UpdateManager.checkLatest()
                    updateChecking = false
                    result.onSuccess { info ->
                        if (UpdateManager.isNewer(info.version, appVersion)) {
                            updateInfo = info
                        } else {
                            Toast.makeText(context, "已是最新版本（$appVersion）", Toast.LENGTH_SHORT).show()
                        }
                    }.onFailure { e ->
                        Toast.makeText(context, "检查更新失败：${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            updateChecking = updateChecking,
        )
    }

    // ===== v2.4 对话框组 =====
    // 0) 摘要结果：明确呈现，避免 Toast 一闪而过看不到
    if (summaryDialog.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { summaryDialog = "" },
            title = { Text("摘要") },
            text = { Text(summaryDialog) },
            confirmButton = { TextButton(onClick = { summaryDialog = "" }) { Text("知道了") } },
        )
    }

    // 1) 提醒：日期选择
    if (showDatePicker) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    pendingReminderMillis = dateState.selectedDateMillis ?: System.currentTimeMillis()
                    showTimePicker = true
                }) { Text("下一步") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) {
            DatePicker(state = dateState, showModeToggle = false)
        }
    }

    // 2) 提醒：时间选择（拿到日期后再选时分）
    if (showTimePicker) {
        // DatePicker 的 selectedDateMillis 是 UTC 零点，直接当本地时间会差 8 小时；
        // 这里按本地时区取出年月日再交给 TimePicker。
        val cal = Calendar.getInstance().apply { timeInMillis = pendingReminderMillis }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("提醒时间") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timeState)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        // 取消提醒
                        reminderTarget?.let { applyReminder(it, 0L) }
                        reminderTarget = null
                        showTimePicker = false
                    }) { Text("取消这条提醒", color = Color(0xFFE64340)) }
                    TextButton(onClick = {
                        // 10 秒后提醒，便于快速验证闹钟链路
                        reminderTarget?.let { applyReminder(it, System.currentTimeMillis() + 10_000) }
                        reminderTarget = null
                        showTimePicker = false
                    }) { Text("10 秒后测试") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // selectedDateMillis 是 UTC 零点：先用 UTC 日历取出年月日，
                    // 再用本地日历组装提醒时间，避免跨时区日期错位。
                    val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = pendingReminderMillis
                    }
                    val chosen = Calendar.getInstance().apply {
                        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, timeState.hour)
                        set(Calendar.MINUTE, timeState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    reminderTarget?.let { applyReminder(it, chosen.timeInMillis) }
                    reminderTarget = null
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("取消") } },
        )
    }

    // 3) 文字导出（Markdown / CSV）
    if (showTextExport) {
        AlertDialog(
            onDismissRequest = { showTextExport = false },
            title = { Text("导出文字记录") },
            text = {
                Column {
                    Text("把全部记录导出到「下载/随手说」，共 ${noteStore.all().size} 条。", fontSize = 13.sp)
                    if (exportDone.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(exportDone, fontSize = 12.sp, color = if (dark) AccentBright else Accent)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { exportText("md"); exportText("csv") }) {
                    Text("导出 MD + CSV")
                }
            },
            dismissButton = { TextButton(onClick = { showTextExport = false }) { Text("关闭") } },
        )
    }

    // 4) 隐私锁密码设置
    if (showLockSetup) {
        AlertDialog(
            onDismissRequest = { showLockSetup = false },
            title = { Text("设置锁屏密码") },
            text = {
                Column {
                    Text("4-12 位数字密码，只在本机保存摘要（SHA-256），忘记无法找回。", fontSize = 12.sp, color = subColor)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lockPinInput,
                        onValueChange = { lockPinInput = it.filter { c -> c.isDigit() }.take(12) },
                        label = { Text("密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (lockMsg.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(lockMsg, fontSize = 12.sp, color = Color(0xFFE64340))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (lockPinInput.length < 4) {
                        lockMsg = "密码至少 4 位"
                    } else {
                        settingsStore.lockPinHash = SettingsStore.sha256(lockPinInput)
                        settingsStore.lockEnabled = true
                        lockEnabled = true
                        lockPinInput = ""
                        lockMsg = ""
                        showLockSetup = false
                        Toast.makeText(context, "隐私锁已开启", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showLockSetup = false }) { Text("取消") } },
        )
    }

    // 发现新版本：提示下载并拉起系统安装界面
    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { if (!updateDownloading) updateInfo = null },
            title = { Text("发现新版本 ${info.version}") },
            text = {
                Column {
                    Text("当前版本 $appVersion", fontSize = 12.sp, color = Color(0xFF999999))
                    Spacer(Modifier.height(8.dp))
                    Text(info.notes.take(400), fontSize = 13.sp)
                    if (updateDownloading) {
                        Spacer(Modifier.height(14.dp))
                        Text("正在下载… $updateProgress%", fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { updateProgress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = Accent,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !updateDownloading,
                    onClick = {
                        updateDownloading = true
                        updateProgress = 0
                        scope.launch {
                            val result = UpdateManager.download(context, info.apkUrl) { updateProgress = it }
                            updateDownloading = false
                            result.onSuccess { file ->
                                updateInfo = null
                                runCatching { UpdateManager.install(context, file) }
                                    .onFailure { Toast.makeText(context, "无法打开安装界面，请检查未知来源安装权限", Toast.LENGTH_LONG).show() }
                            }.onFailure { e ->
                                Toast.makeText(context, "下载失败：${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                ) { Text(if (updateDownloading) "下载中" else "下载并安装") }
            },
            dismissButton = {
                TextButton(onClick = { updateInfo = null }, enabled = !updateDownloading) { Text("稍后") }
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
            .clip(RoundedCornerShape(20))
            .background(Brush.linearGradient(listOf(base, tint)))
            .border(1.dp, if (dark) Color(0x1AFFFFFF) else Color(0x0D000000), RoundedCornerShape(20))
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
                    "可按住转文字 · 按住说话，两种模式随心切",
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
            .clip(RoundedCornerShape(50))
            .background(if (dark) CardDark else CardLight)
            .border(1.dp, (if (dark) AccentBright else Accent).copy(alpha = 0.25f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            AppIcons.ContentPaste,
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
    onSlideDx: (Float) -> Unit,
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
                onSlideDx(dx)
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
        if (longPressed) {
            onSlideDx(0f)
            onEnd(mode)
        } else onTap(downX)
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
    heroPinned: Boolean,
    onHeroPinnedChange: (Boolean) -> Unit,
    themeColor: String,
    onThemeColorChange: (String) -> Unit,
    customAccent: String,
    onCustomAccentChange: (String) -> Unit,
    onCustomColor: () -> Unit,
    trashCount: Int,
    onOpenTrash: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    stats: com.liweiwen.voicenotes.data.StorageStats,
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
    appVersion: String,
    updateChecking: Boolean,
    onOpenAbout: () -> Unit,
    onCheckUpdate: () -> Unit,
    // v2.4 新增
    autoBackup: Boolean,
    onAutoBackupChange: (Boolean) -> Unit,
    lastBackupTime: Long,
    headsetControl: Boolean,
    onHeadsetChange: (Boolean) -> Unit,
    locationEnabled: Boolean,
    onLocationChange: (Boolean) -> Unit,
    lockEnabled: Boolean,
    onLockChange: (Boolean) -> Unit,
    onLockSetup: () -> Unit,
    onLockDisable: () -> Unit,
    lockBiometric: Boolean,
    onLockBiometricChange: (Boolean) -> Unit,
    hidePrivate: Boolean,
    onHidePrivateChange: (Boolean) -> Unit,
    summaryModelInput: String,
    onSummaryModelChange: (String) -> Unit,
    onOpenTextExport: () -> Unit,
) {
    val surface = if (dark) SurfaceDark else SurfaceLight
    val background = if (dark) BgDark else BgLight
    val textColor = if (dark) Color(0xFFECECEC) else Color(0xFF222222)
    val subColor = if (dark) Color(0xFF8A8A8A) else Color(0xFF999999)
    val cardBg = if (dark) InputDark else Color.White
    // API 配置默认收起，避免暴露默认模型 Key 等敏感信息；点开才显示
    var showApiConfig by remember { mutableStateOf(false) }

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
            // 数据管理：统计 / 路径 / 操作收进同一张卡，不再层层散落
            SettingsSection(title = "数据管理") {
                CardBox(cardBg) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("本机数据", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor)
                            Spacer(Modifier.weight(1f))
                            TextButton(
                                onClick = onRefresh,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            ) {
                                Text("刷新", fontSize = 12.sp, color = if (dark) AccentBright else Accent)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatItem("记录", "${stats.noteCount} 条", textColor)
                            StatItem("语音", "${stats.audioCount} 个", textColor)
                            StatItem("占用", formatBytes(stats.audioBytes), textColor)
                        }
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(if (dark) Color(0x24FFFFFF) else Color(0x14000000)))
                        Spacer(Modifier.height(2.dp))
                        DataPathRow(AppIcons.ContentPaste, "记录文件", "notes.json · 查看路径并导出", textColor, subColor, dark) {
                            onOpenPath(notesFilePath, false)
                        }
                        DataPathRow(AppIcons.FolderOpen, "语音目录", "${stats.audioCount} 个 WAV · 查看路径并导出", textColor, subColor, dark) {
                            onOpenPath(recordingsDirPath, true)
                        }
                        Spacer(Modifier.height(2.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(if (dark) Color(0x24FFFFFF) else Color(0x14000000)))
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth()) {
                            TextButton(onClick = onOpenTrash, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
                                Text(if (trashCount > 0) "回收站 ($trashCount)" else "回收站", fontSize = 12.sp, color = if (dark) AccentBright else Accent)
                            }
                            TextButton(onClick = onExport, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
                                Text("导出备份", fontSize = 12.sp, color = if (dark) AccentBright else Accent)
                            }
                            TextButton(onClick = onImport, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
                                Text("导入恢复", fontSize = 12.sp, color = if (dark) AccentBright else Accent)
                            }
                        }
                        TextButton(onClick = onOpenTextExport, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                            Text("导出文字记录（Markdown / CSV）", fontSize = 12.sp, color = if (dark) AccentBright else Accent)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = onClearAudio, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("清除语音文件（保留文字）", fontSize = 12.sp, color = if (dark) Color(0xFFE8B04B) else Color(0xFFB8860B))
                    }
                    TextButton(onClick = onClearAll, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("清空全部数据", fontSize = 12.sp, color = Color(0xFFE64340))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 外观
            SettingsSection(title = "外观") {
                Text("显示模式", fontSize = 12.sp, color = subColor)
                Spacer(Modifier.height(8.dp))
                // 分段控件：选中段填充主题色，圆角轨道，现代简洁
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12))
                        .background(if (dark) Color(0xFF1A2338) else Color(0xFFEEF1F4))
                        .padding(4.dp),
                ) {
                    listOf("system" to "跟随系统", "dark" to "深色", "light" to "浅色").forEach { (value, label) ->
                        val selected = theme == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9))
                                .background(if (selected) (if (dark) AccentBright else Accent) else Color.Transparent)
                                .clickable { onThemeChange(value) }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color.White else subColor,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                // 主题配色：卡片网格，每张卡上方一段渐变预览，选中加描边+勾
                Text("主题配色", fontSize = 12.sp, color = subColor)
                Spacer(Modifier.height(8.dp))
                val customSelected = themeColor == AppPalettes.CUSTOM_KEY
                val customColor = runCatching {
                    Color(android.graphics.Color.parseColor(if (customAccent.isBlank()) "#00A88C" else customAccent))
                }.getOrDefault(Color(0xFF00A88C))
                // 6 张卡片分两行，每行 3 张等宽
                val swatches = AppPalettes.all.map { p ->
                    Triple(p.name, if (dark) p.accentBright else p.accent, { -> onThemeColorChange(p.key) }) to p.key
                } + listOf(
                    Triple("自选", customColor, { -> onCustomColor() }) to AppPalettes.CUSTOM_KEY,
                )
                swatches.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { (item, key) ->
                            ThemeSwatchCard(
                                modifier = Modifier.weight(1f),
                                name = item.first,
                                swatch = item.second,
                                secondary = if (key == AppPalettes.CUSTOM_KEY) Color(0xFF7C4DFF) else AppPalettes.from(key, customAccent).secondary,
                                selected = key == themeColor,
                                dark = dark,
                                cardBg = cardBg,
                                rainbow = key == AppPalettes.CUSTOM_KEY,
                                onClick = item.third,
                            )
                        }
                    }
                }
                if (themeColor == AppPalettes.CUSTOM_KEY) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onCustomColor) {
                        Text("调整自选颜色", color = if (dark) AccentBright else Accent)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("统计卡固定置顶", fontSize = 13.sp, color = textColor)
                        Text(
                            if (heroPinned) "统计卡固定在顶部，不随列表滚动" else "统计卡随记录列表一起滚动",
                            fontSize = 11.sp,
                            color = subColor,
                        )
                    }
                    Switch(
                        checked = heroPinned,
                        onCheckedChange = onHeroPinnedChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Accent,
                            checkedTrackColor = Accent.copy(alpha = 0.5f),
                        ),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 语音转写：API 配置默认收起，不直接展示 Key / 模型等默认值
            SettingsSection(title = "语音转写") {
                Text(
                    "录音保存为本机 WAV 文件，不需要存储权限；仅转写时把音频流式发给百炼。API Key 只存本机。",
                    fontSize = 12.sp,
                    color = subColor,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showApiConfig = !showApiConfig }) {
                    Text(
                        if (showApiConfig) "收起 API 配置" else "API 配置（点开填写 Key）",
                        color = if (dark) AccentBright else Accent,
                    )
                }
                if (showApiConfig) {
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
                    OutlinedTextField(
                        value = summaryModelInput,
                        onValueChange = onSummaryModelChange,
                        label = { Text("AI 摘要模型（推荐 qwen-plus）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 智能与自动化：定时备份 / 耳机线控 / 地理标记
            SettingsSection(title = "智能与自动化") {
                CardBox(cardBg) {
                    Column(Modifier.padding(16.dp)) {
                        SettingsSwitchRow(
                            title = "每天自动备份",
                            desc = if (lastBackupTime > 0) {
                                "上次备份：${java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastBackupTime))} · 存到 下载/随手说/自动备份"
                            } else "每天自动打包到 下载/随手说/自动备份，不联网",
                            checked = autoBackup,
                            dark = dark,
                            onChange = onAutoBackupChange,
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(if (dark) Color(0x24FFFFFF) else Color(0x14000000)))
                        Spacer(Modifier.height(10.dp))
                        SettingsSwitchRow(
                            title = "耳机线控录音",
                            desc = "按一下耳机播放键开始/停止录音（App 在前台时）",
                            checked = headsetControl,
                            dark = dark,
                            onChange = onHeadsetChange,
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(if (dark) Color(0x24FFFFFF) else Color(0x14000000)))
                        Spacer(Modifier.height(10.dp))
                        SettingsSwitchRow(
                            title = "记录附带位置",
                            desc = "标记记录时附带当前经纬度（需定位权限）",
                            checked = locationEnabled,
                            dark = dark,
                            onChange = onLocationChange,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 隐私与安全
            SettingsSection(title = "隐私与安全") {
                CardBox(cardBg) {
                    Column(Modifier.padding(16.dp)) {
                        SettingsSwitchRow(
                            title = "隐私锁",
                            desc = if (lockEnabled) "进入 App 需验证密码或指纹" else "开启后需设置密码，离开再回来需重新解锁",
                            checked = lockEnabled,
                            dark = dark,
                            onChange = onLockChange,
                        )
                        if (lockEnabled) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = onLockSetup, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                    Text("修改密码", fontSize = 12.sp, color = if (dark) AccentBright else Accent)
                                }
                                TextButton(onClick = onLockDisable, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                    Text("关闭隐私锁", fontSize = 12.sp, color = Color(0xFFE64340))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            SettingsSwitchRow(
                                title = "指纹/面容解锁",
                                desc = "本机支持时优先用生物识别",
                                checked = lockBiometric,
                                dark = dark,
                                onChange = onLockBiometricChange,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(if (dark) Color(0x24FFFFFF) else Color(0x14000000)))
                        Spacer(Modifier.height(10.dp))
                        SettingsSwitchRow(
                            title = "隐藏私密记录内容",
                            desc = "私密记录在列表里打码，点一下临时显示",
                            checked = hidePrivate,
                            dark = dark,
                            onChange = onHidePrivateChange,
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // 关于与更新
            SettingsSection(title = "关于与更新") {
                CardBox(cardBg) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(if (dark) AccentBright else Accent, Secondary),
                                        ),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("随", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("随手说", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                                Text("版本 $appVersion", fontSize = 12.sp, color = subColor)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = onOpenAbout, modifier = Modifier.fillMaxWidth()) {
                            Text("功能介绍与使用手册", color = if (dark) AccentBright else Accent)
                        }
                        TextButton(onClick = onCheckUpdate, modifier = Modifier.fillMaxWidth(), enabled = !updateChecking) {
                            Text(
                                if (updateChecking) "正在检查更新…" else "检查更新",
                                color = if (dark) AccentBright else Accent,
                            )
                        }
                    }
                }
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
private fun SettingsSwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    dark: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, color = if (dark) Color(0xFFECECEC) else Color(0xFF222222))
            Spacer(Modifier.height(2.dp))
            Text(desc, fontSize = 11.sp, color = if (dark) Color(0xFF8A8A8A) else Color(0xFF999999))
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = if (dark) AccentBright else Accent,
            ),
        )
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

/** 主题配色卡片：上半部渐变预览，下半部名称，选中加描边+对勾。 */
@Composable
private fun ThemeSwatchCard(
    modifier: Modifier = Modifier,
    name: String,
    swatch: Color,
    secondary: Color,
    selected: Boolean,
    dark: Boolean,
    cardBg: Color,
    rainbow: Boolean = false,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) (if (dark) AccentBright else Accent) else if (dark) Color(0x20FFFFFF) else Color(0x12000000)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14))
            .background(cardBg)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(14))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(9))
                .background(
                    if (rainbow) {
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFFFF5252), Color(0xFFFFD740), Color(0xFF69F0AE),
                                Color(0xFF40C4FF), Color(0xFF7C4DFF), Color(0xFFFF5252),
                            ),
                        )
                    } else {
                        Brush.linearGradient(listOf(swatch, secondary))
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) (if (dark) AccentBright else Accent) else if (dark) Color(0xFFBBBBBB) else Color(0xFF666666),
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 12.sp, color = Color(0xFF999999))
    }
}

/** 设置页路径行：图标 + 名称 + 简短说明 + 右箭头，点击直达系统文件应用。 */
@Composable
private fun DataPathRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String,
    textColor: Color,
    subColor: Color,
    dark: Boolean,
    onClick: () -> Unit,
) {
    val accent = if (dark) AccentBright else Accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, color = textColor)
            Text(detail, fontSize = 11.sp, color = subColor)
        }
        Text("›", fontSize = 22.sp, color = subColor)
    }
}

/** 全屏文字编辑器：多行输入、自动聚焦、字数提示；返回与完成只关页面，草稿保留。 */
@Composable
private fun FullScreenTextEditor(
    draft: String,
    onDraftChange: (String) -> Unit,
    accent: Color,
    dark: Boolean,
    onDone: () -> Unit,
) {
    val bg = if (dark) Color(0xFF0B1020) else Color(0xFFF7F9FB)
    val textColor = if (dark) Color(0xFFEAF6F3) else Color(0xFF19322E)
    val subColor = if (dark) Color(0xFF9FB3B0) else Color(0xFF8A9996)
    val focusRequester = remember { FocusRequester() }
    BackHandler(onBack = onDone)
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Column(Modifier.fillMaxSize().background(bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDone) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = textColor)
            }
            Text("编辑文字", Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor)
            TextButton(onClick = onDone) { Text("完成", color = accent, fontSize = 14.sp) }
        }
        BasicTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .focusRequester(focusRequester),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, lineHeight = 26.sp, color = textColor),
            cursorBrush = SolidColor(accent),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize()) {
                    if (draft.isEmpty()) Text("输入文字…", fontSize = 16.sp, color = subColor)
                    inner()
                }
            },
        )
        Text("共 ${draft.length} 字", modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp), fontSize = 12.sp, color = subColor)
    }
}

/** App 内本机文件浏览器：直接显示完整路径与文件，可导出到「下载/随手说」。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileBrowserPage(
    path: String,
    dark: Boolean,
    onBack: () -> Unit,
    onExport: (File) -> Unit,
    onExportAll: (List<File>) -> Unit,
    onShare: (File) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val surface = if (dark) SurfaceDark else SurfaceLight
    val background = if (dark) BgDark else BgLight
    val textColor = if (dark) Color(0xFFECECEC) else Color(0xFF222222)
    val subColor = if (dark) Color(0xFF8A8A8A) else Color(0xFF999999)
    val cardBg = if (dark) InputDark else Color.White
    val accent = if (dark) AccentBright else Accent
    val target = File(path)
    val isDir = target.isDirectory
    val files = remember(path) {
        if (isDir) {
            target.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else listOf(target)
    }
    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                title = { Text(if (isDir) "语音目录" else "记录文件", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
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
                .padding(16.dp),
        ) {
            CardBox(cardBg) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("完整路径", fontSize = 12.sp, color = subColor, modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(path))
                                Toast.makeText(context, "路径已复制", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text("复制", fontSize = 12.sp, color = accent)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(path, fontSize = 12.sp, color = textColor)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "这些文件在 App 私有目录，系统文件管理器无法直接打开。点「导出」复制到「下载 / 随手说」，即可在系统文件管理器里找到。",
                fontSize = 12.sp,
                color = subColor,
            )
            Spacer(Modifier.height(12.dp))
            Text("文件（${files.size}）", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor)
            Spacer(Modifier.height(8.dp))
            if (files.isEmpty()) {
                Text("这里还没有文件", fontSize = 13.sp, color = subColor)
            } else {
                files.forEach { file ->
                    CardBox(cardBg) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (file.name.endsWith(".wav", true)) AppIcons.VolumeUp else AppIcons.ContentPaste,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(file.name, fontSize = 13.sp, color = textColor)
                                Text(
                                    "${formatBytes(file.length())} · ${formatRecordTime(file.lastModified())}",
                                    fontSize = 11.sp,
                                    color = subColor,
                                )
                            }
                            IconButton(onClick = { onShare(file) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "分享", tint = accent, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onExport(file) }, modifier = Modifier.size(36.dp)) {
                                Icon(AppIcons.Backup, contentDescription = "导出", tint = accent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { onExportAll(files) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = accent),
                ) {
                    Text("导出全部到 下载/随手说", color = Color.White, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 长按记录后就地弹出的横向操作条：图标+文字横排，高度小、不遮键盘、不会超出屏幕底部。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteActionMenu(
    note: com.liweiwen.voicenotes.data.Note,
    dark: Boolean,
    transcribing: Boolean,
    ttsSpeaking: Boolean,
    onDismiss: () -> Unit,
    onPlay: (Float) -> Unit,
    onTranscribe: () -> Unit,
    onRead: () -> Unit,
    onTag: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onMultiSelect: () -> Unit,
    onDelete: () -> Unit,
    summarizing: Boolean = false,
    onRemind: () -> Unit = {},
    onSummarize: () -> Unit = {},
    onPrivate: () -> Unit = {},
    onLocation: () -> Unit = {},
) {
    val accent = if (dark) AccentBright else Accent
    val menuBg = if (dark) Color(0xFF243049) else Color(0xFFFFFFFF)
    val danger = Color(0xFFE64340)

    // 自动换行：一行放不下时排到第二行，完整展示所有操作，不再横向裁切
    FlowRow(
        modifier = Modifier
            .widthIn(max = 250.dp)
            .clip(RoundedCornerShape(18))
            .background(menuBg)
            .border(1.dp, if (dark) Color(0x2AFFFFFF) else Color(0x16000000), RoundedCornerShape(18))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center,
        maxItemsInEachRow = 5,
    ) {
        if (note.hasAudio) {
            MenuItem(Icons.Default.PlayArrow, "播放", accent) { onPlay(1.0f) }
            MenuItem(Icons.Default.PlayArrow, "1.5x", accent) { onPlay(1.5f) }
            MenuItem(Icons.Default.PlayArrow, "2x", accent) { onPlay(2.0f) }
            MenuItem(
                Icons.Default.Edit,
                if (transcribing) "转写中" else "转文字",
                accent,
                enabled = !transcribing,
                onClick = onTranscribe,
            )
        }
        if (note.text.isNotBlank() && note.text != "[语音消息]") {
            MenuItem(AppIcons.ContentCopy, "复制", accent, onClick = onCopy)
            MenuItem(Icons.Default.Edit, "编辑", accent, onClick = onEdit)
            MenuItem(AppIcons.VolumeUp, if (ttsSpeaking) "停止" else "朗读", accent, onClick = onRead)
        }
        MenuItem(AppIcons.Label, "标签", accent, onClick = onTag)
        // v2.4 新操作：提醒 / AI 摘要 / 私密 / 位置
        MenuItem(AppIcons.Alarm, "提醒", accent, onClick = onRemind)
        if (note.text.isNotBlank() && note.text != "[语音消息]") {
            MenuItem(
                AppIcons.AutoAwesome,
                if (summarizing) "摘要中" else "摘要",
                accent,
                enabled = !summarizing,
                onClick = onSummarize,
            )
        }
        MenuItem(AppIcons.Lock, if (note.isPrivate) "取消私密" else "私密", accent, onClick = onPrivate)
        MenuItem(AppIcons.LocationOn, "位置", accent, onClick = onLocation)
        MenuItem(AppIcons.SelectAll, "多选", accent, onClick = onMultiSelect)
        MenuItem(Icons.Default.Delete, "删除", danger, onClick = onDelete)
    }
}

/** 横向操作条里的单个按钮：上图标下文字，紧凑排布。 */
@Composable
private fun MenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val itemColor = if (enabled) color else color.copy(alpha = 0.4f)
    Column(
        modifier = Modifier
            .width(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = itemColor, modifier = Modifier.size(17.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 10.sp, color = itemColor, maxLines = 1)
    }
}

private fun formatRecordTime(time: Long): String {
    val d = java.util.Date(time)
    return java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(d)
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024f)
    else -> "%.1f MB".format(bytes / (1024f * 1024f))
}

/** 标签筛选胶囊：选中时用主题色淡底，未选中时低调灰色。 */
@Composable
private fun TagChip(text: String, selected: Boolean, dark: Boolean, onClick: () -> Unit) {
    val accent = if (dark) AccentBright else Accent
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent.copy(alpha = 0.18f) else if (dark) Color(0x14FFFFFF) else Color(0x0D000000))
            .border(1.dp, if (selected) accent.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            fontSize = 12.sp,
            maxLines = 1,
            color = if (selected) accent else if (dark) Color(0xFF9FB3B0) else Color(0xFF777777),
        )
    }
}

/** 时间轴视图：按日期分组，日期标题吸顶，组内记录仍由调用方统一渲染。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineNotes(
    notes: List<Note>,
    listState: LazyListState,
    renderNote: @Composable (Note) -> Unit,
    showHero: Boolean,
    hero: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = notes.groupBy { dayKey(it.time) }.toList()
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 116.dp),
    ) {
        if (showHero) {
            item(key = "timeline_hero") { hero() }
        }
        groups.forEach { (day, dayNotes) ->
            item(key = "day_$day") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                        .padding(horizontal = 20.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(LocalPalette.current.accent),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        dayLabel(day),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            items(dayNotes, key = { it.id }) { note -> renderNote(note) }
        }
    }
}

private fun dayKey(time: Long): String =
    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(time))

private fun dayLabel(key: String): String {
    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val today = fmt.format(java.util.Date())
    val yesterday = fmt.format(java.util.Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000))
    val md = java.text.SimpleDateFormat("M月d日", java.util.Locale.getDefault())
        .format(runCatching { fmt.parse(key) }.getOrNull() ?: java.util.Date())
    return when (key) {
        today -> "今天 · $md"
        yesterday -> "昨天 · $md"
        else -> md
    }
}

/** 关于 / 使用手册：版本号、功能清单与交互说明、内置更新入口。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutPage(
    dark: Boolean,
    appVersion: String,
    updateChecking: Boolean,
    onBack: () -> Unit,
    onCheckUpdate: () -> Unit,
) {
    val surface = if (dark) SurfaceDark else SurfaceLight
    val background = if (dark) BgDark else BgLight
    val textColor = if (dark) Color(0xFFECECEC) else Color(0xFF222222)
    val subColor = if (dark) Color(0xFF9FB3B0) else Color(0xFF6B7C79)
    val cardBg = if (dark) InputDark else Color.White
    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                title = { Text("关于与使用手册", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // 顶部品牌卡
            CardBox(cardBg) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18))
                            .background(Brush.linearGradient(listOf(if (dark) AccentBright else Accent, Secondary))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("随", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("随手说", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(Modifier.height(4.dp))
                    Text("版本 $appVersion", fontSize = 13.sp, color = subColor)
                    Spacer(Modifier.height(4.dp))
                    Text("本机优先的语音速记 · 数据不上传", fontSize = 12.sp, color = subColor)
                    Spacer(Modifier.height(14.dp))
                    TextButton(onClick = onCheckUpdate, enabled = !updateChecking) {
                        Text(
                            if (updateChecking) "正在检查更新…" else "检查更新",
                            color = if (dark) AccentBright else Accent,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            SettingsSection(title = "功能介绍") {
                CardBox(cardBg) {
                    Column(Modifier.padding(14.dp)) {
                        AboutFeature(dark = dark, AppIcons.Mic, "两种输入模式", "底部输入栏左下角按钮切换「可按住转文字」与「按住说话」。点按打字，长按录音。")
                        AboutFeature(dark = dark, AppIcons.Restore, "录音滑动手势", "长按录音时：左滑取消，右滑转文字，原地松手按当前模式保存语音或文字。")
                        AboutFeature(dark = dark, Icons.Default.Edit, "记录编辑", "双击文字记录可复制；长按记录可播放 / 倍速 / 转文字 / 朗读 / 加标签 / 多选 / 删除。")
                        AboutFeature(dark = dark, AppIcons.VolumeUp, "文字朗读", "长按文字记录选「朗读」，用系统中文语音读出；与语音播放互斥。")
                        AboutFeature(dark = dark, AppIcons.Label, "标签分类", "长按记录加标签，或直接点气泡里的 #标签 修改；首页顶部横向标签栏一键筛选。")
                        AboutFeature(dark = dark, AppIcons.Calendar, "日历时间轴", "顶栏日历图标切换按日期分组的时间轴视图。")
                        AboutFeature(dark = dark, Icons.Default.Delete, "回收站", "删除先进回收站保留 30 天，可在设置里恢复或彻底删除。")
                        AboutFeature(dark = dark, AppIcons.Backup, "备份与恢复", "设置里导出 ZIP（含记录、设置与语音），导入即可整机迁移或恢复。")
                        AboutFeature(dark = dark, Icons.Default.Settings, "主题系统", "浅色 / 深色 / 跟随系统 + 多套配色与自定义色，实时生效。")
                        AboutFeature(dark = dark, Icons.Default.Lock, "本机存储", "录音 WAV 与文字全部保存在本机，仅转写时把音频流式发给阿里云百炼。")
                        AboutFeature(dark = dark, AppIcons.Alarm, "待办提醒", "长按记录选「提醒」，选日期时间，到点弹出通知，点击直达该记录。")
                        AboutFeature(dark = dark, AppIcons.AutoAwesome, "AI 摘要", "长按文字记录选「摘要」。填了百炼 Key 用 AI 提炼；没填也能本机截取首句做摘要，结果显示在气泡里。")
                        AboutFeature(dark = dark, AppIcons.Backup, "定时备份", "设置里开启每天自动备份，ZIP 存到 下载/随手说/自动备份，全程离线。")
                        AboutFeature(dark = dark, AppIcons.Download, "文字导出", "设置里一键把全部记录导出为 Markdown / CSV，存到 下载/随手说。")
                        AboutFeature(dark = dark, AppIcons.Lock, "隐私锁", "设置密码或指纹，离开再回来需重新解锁；私密记录可打码隐藏。")
                        AboutFeature(dark = dark, AppIcons.Headset, "耳机线控", "App 在前台时按一下耳机播放键即可开始/停止录音。")
                        AboutFeature(dark = dark, AppIcons.LocationOn, "地理标记", "长按记录选「位置」附上当前经纬度（需系统定位权限）。")
                        AboutFeature(dark = dark, AppIcons.GridView, "桌面小组件", "长按桌面添加「随手说」小组件，一键录音、查看最近两条记录。")
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            SettingsSection(title = "使用手册") {
                CardBox(cardBg) {
                    Column(Modifier.padding(14.dp)) {
                        AboutStep(dark = dark, "1", "开始记录", "按住底部输入栏说话；或点一下输入栏打字。松手即保存。")
                        AboutStep(dark = dark, "2", "切换模式", "点左下角声波/话筒圆钮，在「可按住转文字」和「按住说话」间切换。")
                        AboutStep(dark = dark, "3", "转文字", "录音后长按该记录点「转文字」，需先在设置里填阿里云百炼 API Key。")
                        AboutStep(dark = dark, "4", "管理与查找", "顶栏可搜索、多选、切换时间轴；长按记录可加标签、播放、朗读、删除。")
                        AboutStep(dark = dark, "5", "备份", "设置 → 导出备份保存 ZIP；换机后导入恢复即可。")
                        AboutStep(dark = dark, "6", "更新", "设置 → 检查更新，发现新版本可直接下载并跳转到系统安装界面。")
                        AboutStep(dark = dark, "7", "更多玩法", "长按记录可设提醒/摘要/私密/位置；设置里开定时备份、隐私锁、耳机线控；桌面可加小组件。")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AboutFeature(dark: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9))
                .background((if (dark) AccentBright else Accent).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (dark) AccentBright else Accent, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (dark) Color(0xFFECECEC) else Color(0xFF222222))
            Spacer(Modifier.height(2.dp))
            Text(desc, fontSize = 12.sp, color = if (dark) Color(0xFF9FB3B0) else Color(0xFF6B7C79))
        }
    }
}

@Composable
private fun AboutStep(dark: Boolean, no: String, title: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background((if (dark) AccentBright else Accent).copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(no, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (dark) AccentBright else Accent)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (dark) Color(0xFFECECEC) else Color(0xFF222222))
            Spacer(Modifier.height(2.dp))
            Text(desc, fontSize = 12.sp, color = if (dark) Color(0xFF9FB3B0) else Color(0xFF6B7C79))
        }
    }
}

/** 回收站：恢复、彻底删除、清空。全部是本机操作，不联网。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashPage(
    dark: Boolean,
    notes: List<Note>,
    onBack: () -> Unit,
    onRestore: (String) -> Unit,
    onDeleteForever: (String) -> Unit,
    onEmpty: () -> Unit,
) {
    val surface = if (dark) SurfaceDark else SurfaceLight
    val background = if (dark) BgDark else BgLight
    val textColor = if (dark) Color(0xFFECECEC) else Color(0xFF222222)
    val subColor = if (dark) Color(0xFF8A8A8A) else Color(0xFF999999)
    val accent = if (dark) AccentBright else Accent
    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                title = { Text("回收站", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = textColor)
                    }
                },
                actions = {
                    if (notes.isNotEmpty()) {
                        TextButton(onClick = onEmpty) { Text("清空", color = Color(0xFFE64340)) }
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
                .background(background),
        ) {
            Text(
                "删除的记录保留 30 天，期间可恢复；过期后自动清除。",
                fontSize = 12.sp,
                color = subColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
            if (notes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("回收站是空的", fontSize = 15.sp, color = subColor)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    items(notes, key = { it.id }) { note ->
                        CardBox(if (dark) CardDark else Color.White) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    formatRecordTime(note.time) + " 删除",
                                    fontSize = 11.sp,
                                    color = subColor,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    note.text.take(160),
                                    fontSize = 14.sp,
                                    color = textColor,
                                    maxLines = 3,
                                )
                                if (note.tags.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        note.tags.joinToString("   ") { "#$it" },
                                        fontSize = 11.sp,
                                        color = accent,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Row {
                                    TextButton(onClick = { onRestore(note.id) }) {
                                        Text("恢复", color = accent)
                                    }
                                    TextButton(onClick = { onDeleteForever(note.id) }) {
                                        Text("彻底删除", color = Color(0xFFE64340))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

/** 标签编辑弹窗：已有标签点一下切换，输入框可添加新标签。 */
@Composable
private fun TagDialog(
    note: Note,
    allTags: List<String>,
    dark: Boolean,
    draft: String,
    onDraftChange: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = if (dark) AccentBright else Accent
    val subColor = if (dark) Color(0xFF8A8A8A) else Color(0xFF999999)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("给这条记录加标签") },
        text = {
            Column {
                Text("点选可添加或取消", fontSize = 12.sp, color = subColor)
                Spacer(Modifier.height(8.dp))
                if (allTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        allTags.forEach { tag ->
                            val on = tag in note.tags
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (on) accent.copy(alpha = 0.18f) else if (dark) Color(0x14FFFFFF) else Color(0x0D000000))
                                    .border(1.dp, if (on) accent else Color.Transparent, RoundedCornerShape(50))
                                    .clickable { onToggleTag(tag) }
                                    .padding(horizontal = 11.dp, vertical = 6.dp),
                            ) {
                                Text(tag, fontSize = 12.sp, color = if (on) accent else subColor)
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    label = { Text("新标签") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (draft.isNotBlank()) {
                                onToggleTag(draft.trim())
                                onDraftChange("")
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "添加标签", tint = accent)
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "当前标签：${note.tags.joinToString("、").ifEmpty { "无" }}",
                    fontSize = 11.sp,
                    color = subColor,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

/** 自定义主题色：色相 + 鲜艳度两个滑条，实时预览，确认后写回设置。 */
@Composable
private fun ColorPickerDialog(
    dark: Boolean,
    initial: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialHsv = remember {
        val hsv = FloatArray(3)
        val fallback = android.graphics.Color.parseColor("#00A88C")
        val base = runCatching {
            android.graphics.Color.parseColor(if (initial.isBlank()) "#00A88C" else initial)
        }.getOrDefault(fallback)
        android.graphics.Color.colorToHSV(base, hsv)
        hsv
    }
    var hue by remember { mutableStateOf(initialHsv[0]) }
    var saturation by remember { mutableStateOf(initialHsv[1].coerceIn(0.3f, 1f)) }
    val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 0.96f))
    val preview = Color(argb)
    val subColor = if (dark) Color(0xFF8A8A8A) else Color(0xFF777777)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自选主题色") },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(preview),
                )
                Spacer(Modifier.height(14.dp))
                Text("色相", fontSize = 12.sp, color = subColor)
                Slider(
                    value = hue,
                    valueRange = 0f..360f,
                    onValueChange = { hue = it },
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        activeTrackColor = preview,
                        thumbColor = preview,
                    ),
                )
                Text("鲜艳度", fontSize = 12.sp, color = subColor)
                Slider(
                    value = saturation,
                    valueRange = 0.3f..1f,
                    onValueChange = { saturation = it },
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        activeTrackColor = preview,
                        thumbColor = preview,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick("#" + "%08X".format(argb)) }) {
                Text("使用这个颜色")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
