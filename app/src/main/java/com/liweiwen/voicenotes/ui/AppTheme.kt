package com.liweiwen.voicenotes.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** 一套主题配色：accent=浅色模式主色，accentBright=深色模式主色，secondary=辅助色。 */
data class AppPalette(
    val key: String,
    val name: String,
    val accent: Color,
    val accentBright: Color,
    val secondary: Color,
)

/** 内置主题配色，设置页可即时切换，切换后全局生效并本机持久化。 */
object AppPalettes {
    val MINT = AppPalette("mint", "薄荷绿", Color(0xFF00A88C), Color(0xFF62E6C8), Color(0xFF7C6CFF))
    val OCEAN = AppPalette("ocean", "海洋蓝", Color(0xFF2E7CD6), Color(0xFF6BA8F5), Color(0xFF00B8A9))
    val VIOLET = AppPalette("violet", "星云紫", Color(0xFF7C5CFF), Color(0xFFB39CFF), Color(0xFFFF7AB8))
    val SUNSET = AppPalette("sunset", "暖阳橙", Color(0xFFE8712F), Color(0xFFFFA96B), Color(0xFFFF5C8A))
    val ROSE = AppPalette("rose", "樱花粉", Color(0xFFD84F7E), Color(0xFFFF8FB1), Color(0xFF8B6CFF))

    val all = listOf(MINT, OCEAN, VIOLET, SUNSET, ROSE)

    fun from(key: String): AppPalette = all.firstOrNull { it.key == key } ?: MINT
}

/** 全局配色入口：默认薄荷绿，由 NoteScreen 用当前设置覆盖。 */
val LocalPalette = compositionLocalOf { AppPalettes.MINT }
