package com.liweiwen.voicenotes.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * 少量 core 图标包缺失的图标，用官方 Material 标准 pathData 本地定义。
 * 这样工程只需依赖轻量的 material-icons-core（约 850KB），
 * 不必为了 6 个图标引入 36MB 的 material-icons-extended。
 */
object AppIcons {
    val Mic: ImageVector by lazy { build("Mic", MIC_PATH) }
    val Pause: ImageVector by lazy { build("Pause", PAUSE_PATH) }
    val ContentCopy: ImageVector by lazy { build("ContentCopy", COPY_PATH) }
    val ContentPaste: ImageVector by lazy { build("ContentPaste", PASTE_PATH) }
    val FolderOpen: ImageVector by lazy { build("FolderOpen", FOLDER_OPEN_PATH) }
    val SelectAll: ImageVector by lazy { build("SelectAll", SELECT_ALL_PATH) }
    val Calendar: ImageVector by lazy { build("Calendar", CALENDAR_PATH) }
    val Label: ImageVector by lazy { build("Label", LABEL_PATH) }
    val Restore: ImageVector by lazy { build("Restore", RESTORE_PATH) }
    val VolumeUp: ImageVector by lazy { build("VolumeUp", VOLUME_UP_PATH) }
    val Backup: ImageVector by lazy { build("Backup", BACKUP_PATH) }
    val Lock: ImageVector by lazy { build("Lock", LOCK_PATH) }
    val Alarm: ImageVector by lazy { build("Alarm", ALARM_PATH) }
    val AutoAwesome: ImageVector by lazy { build("AutoAwesome", AUTO_AWESOME_PATH) }
    val Download: ImageVector by lazy { build("Download", DOWNLOAD_PATH) }
    val LocationOn: ImageVector by lazy { build("LocationOn", LOCATION_ON_PATH) }
    val Headset: ImageVector by lazy { build("Headset", HEADSET_PATH) }
    val GridView: ImageVector by lazy { build("GridView", GRID_VIEW_PATH) }

    private fun build(name: String, pathData: String): ImageVector {
        val nodes = PathParser().parsePathString(pathData).toNodes()
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = nodes,
            fill = SolidColor(Color.Black),
        ).build()
    }
}

// 以下均为 Material Design 官方标准图标 pathData（24x24 视口）
private const val MIC_PATH =
    "M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z" +
        "m5-3c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"
private const val PAUSE_PATH = "M6 19h4V5H6v14zm8-14v14h4V5h-4z"
private const val COPY_PATH =
    "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"
private const val PASTE_PATH =
    "M19 2h-4.18C14.4.84 13.3 0 12 0c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm7 18H5V4h2v3h10V4h2v16z"
private const val FOLDER_OPEN_PATH =
    "M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z"
private const val SELECT_ALL_PATH =
    "M3 5h2V3c-1.1 0-2 .9-2 2zm0 8h2v-2H3v2zm4 8h2v-2H7v2zM3 9h2V7H3v2zm10-6h-2v2h2V3zm6 0v2h2c0-1.1-.9-2-2-2zM5 21v-2H3c0 1.1.9 2 2 2zm-2-4h2v-2H3v2zm8 4h2v-2h-2v2zm8-10h2V7h-2v2zm0 4h2v-2h-2v2zm-4-6h2V3h-2v2zm2 10h2v-2h-2v2zm-4 0h2v-2h-2v2z"
private const val CALENDAR_PATH =
    "M20 3h-1V1h-2v2H7V1H5v2H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H4V9h16v10zM4 7V5h16v2H4z"
private const val LABEL_PATH =
    "M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58s1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41s-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z"
private const val RESTORE_PATH =
    "M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"
private const val VOLUME_UP_PATH =
    "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"
private const val BACKUP_PATH =
    "M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z"
private const val LOCK_PATH =
    "M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1s3.1 1.39 3.1 3.1v2z"
private const val ALARM_PATH =
    "M22 5.72l-4.6-3.86-1.29 1.53 4.6 3.86L22 5.72zM7.88 3.39L6.6 1.86 2 5.71l1.29 1.53 4.59-3.85zM12.5 8H11v6l4.75 2.85.75-1.23-4-2.37V8zM12 4c-4.97 0-9 4.03-9 9s4.02 9 9 9c4.97 0 9-4.03 9-9s-4.03-9-9-9zm0 16c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z"
private const val AUTO_AWESOME_PATH =
    "M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-4.5 3.5L17 14l1.5-1.5.5-1.5-1.5-.5L17 9l-1.5 1.5L14 11l1.5.5.5 1.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15zM11.5 10.5L10 7 8.5 10.5 5 12l3.5 1.5L10 17l1.5-3.5L15 12l-3.5-1.5zM7.5 2L6.75 3.75 5 4.5l1.75.75L7.5 7l.75-1.75L10 4.5 8.25 3.75 7.5 2z"
private const val DOWNLOAD_PATH =
    "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"
private const val LOCATION_ON_PATH =
    "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"
private const val HEADSET_PATH =
    "M12 1c-4.97 0-9 4.03-9 9v7c0 1.66 1.34 3 3 3h3v-8H5v-2c0-3.87 3.13-7 7-7s7 3.13 7 7v2h-4v8h3c1.66 0 3-1.34 3-3v-7c0-4.97-4.03-9-9-9z"
private const val GRID_VIEW_PATH =
    "M3 3v8h8V3H3zm6 6H5V5h4v4zm-6 7v8h8v-8H3zm6 6H5v-4h4v4zm7-20v8h8V3h-8zm6 6h-4V5h4v4zm-6 7v8h8v-8h-8zm6 6h-4v-4h4v4z"
