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
