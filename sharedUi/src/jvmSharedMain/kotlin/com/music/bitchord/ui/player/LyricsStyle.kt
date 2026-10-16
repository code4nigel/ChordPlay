package com.music.bitchord.ui.player

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Supported font families for lyrics rendering.
 */
enum class LyricsFontChoice(val label: String, val family: FontFamily) {
    DEFAULT("Default", FontFamily.Default),
    SERIF("Serif", FontFamily.Serif),
    SANS_SERIF("Sans-Serif", FontFamily.SansSerif),
    MONOSPACE("Monospace", FontFamily.Monospace),
    CURSIVE("Script", FontFamily.Cursive),
}

/**
 * Text size scaling options for lyrics.
 */
enum class LyricsSizeScale(val label: String, val scale: Float) {
    COMPACT("Small", 0.85f),
    STANDARD("Normal", 1.0f),
    LARGE("Large", 1.15f),
    EXTRA_LARGE("Huge", 1.35f),
}

/**
 * Font weight options for lyrics lines.
 */
enum class LyricsWeightChoice(val label: String, val weight: FontWeight) {
    MEDIUM("Medium", FontWeight.Medium),
    SEMI_BOLD("SemiBold", FontWeight.SemiBold),
    BOLD("Bold", FontWeight.Bold),
    HEAVY("Heavy", FontWeight.ExtraBold),
}

/**
 * The current customization applied to lyrics text rendering.
 */
data class LyricsCustomStyle(
    val fontChoice: LyricsFontChoice = LyricsFontChoice.DEFAULT,
    val sizeScale: LyricsSizeScale = LyricsSizeScale.STANDARD,
    val weightChoice: LyricsWeightChoice = LyricsWeightChoice.HEAVY,
    val centerAligned: Boolean = false,
)

/**
 * Reactive state manager for lyrics typography and styling across Android and Desktop.
 */
object LyricsStyleManager {
    private val _style = MutableStateFlow(LyricsCustomStyle())
    val style: StateFlow<LyricsCustomStyle> = _style.asStateFlow()

    fun updateFont(choice: LyricsFontChoice) {
        _style.value = _style.value.copy(fontChoice = choice)
    }

    fun updateScale(scale: LyricsSizeScale) {
        _style.value = _style.value.copy(sizeScale = scale)
    }

    fun updateWeight(weight: LyricsWeightChoice) {
        _style.value = _style.value.copy(weightChoice = weight)
    }

    fun updateCenterAligned(center: Boolean) {
        _style.value = _style.value.copy(centerAligned = center)
    }

    fun reset() {
        _style.value = LyricsCustomStyle()
    }
}
