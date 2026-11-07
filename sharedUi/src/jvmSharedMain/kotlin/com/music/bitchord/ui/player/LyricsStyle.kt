package com.music.bitchord.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.parkinsans_bold
import com.music.bitchord.sharedui.resources.parkinsans_extrabold
import com.music.bitchord.sharedui.resources.parkinsans_medium
import com.music.bitchord.sharedui.resources.parkinsans_semibold
import com.music.bitchord.sharedui.resources.poppins_bold
import com.music.bitchord.sharedui.resources.poppins_extrabold
import com.music.bitchord.sharedui.resources.poppins_medium
import com.music.bitchord.sharedui.resources.poppins_regular
import com.music.bitchord.sharedui.resources.poppins_semibold
import com.music.bitchord.sharedui.resources.sf_pro_display_bold
import com.music.bitchord.sharedui.resources.sf_pro_display_heavy
import com.music.bitchord.sharedui.resources.sf_pro_display_medium
import com.music.bitchord.sharedui.resources.sf_pro_display_regular
import com.music.bitchord.sharedui.resources.sf_pro_display_semibold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.resources.Font

/**
 * Supported font families for lyrics rendering.
 */
enum class LyricsFontChoice(val label: String) {
    DEFAULT("Default"),
    POPPINS("Poppins"),
    PARKINSANS("Parkinsans"),
    SERIF("Serif"),
    SANS_SERIF("Sans-Serif"),
    MONOSPACE("Monospace"),
    CURSIVE("Script"),
    CUSTOM("Custom"),
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
    val customFontName: String? = null,
    val customFontPath: String? = null,
)

/**
 * Resolves the Compose [FontFamily] for the given [LyricsCustomStyle].
 */
@Composable
fun rememberLyricsFontFamily(style: LyricsCustomStyle): FontFamily {
    return when (style.fontChoice) {
        LyricsFontChoice.DEFAULT -> {
            val reg = Font(Res.font.sf_pro_display_regular, FontWeight.Normal)
            val med = Font(Res.font.sf_pro_display_medium, FontWeight.Medium)
            val semi = Font(Res.font.sf_pro_display_semibold, FontWeight.SemiBold)
            val bold = Font(Res.font.sf_pro_display_bold, FontWeight.Bold)
            val heavy = Font(Res.font.sf_pro_display_heavy, FontWeight.ExtraBold)
            remember(reg, med, semi, bold, heavy) { FontFamily(reg, med, semi, bold, heavy) }
        }
        LyricsFontChoice.POPPINS -> {
            val reg = Font(Res.font.poppins_regular, FontWeight.Normal)
            val med = Font(Res.font.poppins_medium, FontWeight.Medium)
            val semi = Font(Res.font.poppins_semibold, FontWeight.SemiBold)
            val bold = Font(Res.font.poppins_bold, FontWeight.Bold)
            val heavy = Font(Res.font.poppins_extrabold, FontWeight.ExtraBold)
            remember(reg, med, semi, bold, heavy) { FontFamily(reg, med, semi, bold, heavy) }
        }
        LyricsFontChoice.PARKINSANS -> {
            val med = Font(Res.font.parkinsans_medium, FontWeight.Medium)
            val semi = Font(Res.font.parkinsans_semibold, FontWeight.SemiBold)
            val bold = Font(Res.font.parkinsans_bold, FontWeight.Bold)
            val heavy = Font(Res.font.parkinsans_extrabold, FontWeight.ExtraBold)
            remember(med, semi, bold, heavy) { FontFamily(med, semi, bold, heavy) }
        }
        LyricsFontChoice.SERIF -> FontFamily.Serif
        LyricsFontChoice.SANS_SERIF -> FontFamily.SansSerif
        LyricsFontChoice.MONOSPACE -> FontFamily.Monospace
        LyricsFontChoice.CURSIVE -> FontFamily.Cursive
        LyricsFontChoice.CUSTOM -> {
            val path = style.customFontPath
            if (!path.isNullOrBlank()) {
                remember(path) { createFontFamilyFromFile(path) }
            } else {
                FontFamily.Default
            }
        }
    }
}

/**
 * Reactive state manager for lyrics typography and styling across Android and Desktop.
 */
object LyricsStyleManager {
    private val _style = MutableStateFlow(LyricsCustomStyle())
    val style: StateFlow<LyricsCustomStyle> = _style.asStateFlow()

    private var persister: ((LyricsCustomStyle) -> Unit)? = null

    fun setInitialStyle(initial: LyricsCustomStyle) {
        _style.value = initial
    }

    fun setPersister(onSave: (LyricsCustomStyle) -> Unit) {
        persister = onSave
    }

    private fun updateAndPersist(newStyle: LyricsCustomStyle) {
        _style.value = newStyle
        persister?.invoke(newStyle)
    }

    fun updateFont(choice: LyricsFontChoice) {
        updateAndPersist(_style.value.copy(fontChoice = choice))
    }

    fun setCustomFont(name: String, path: String) {
        updateAndPersist(
            _style.value.copy(
                fontChoice = LyricsFontChoice.CUSTOM,
                customFontName = name,
                customFontPath = path,
            ),
        )
    }

    fun updateScale(scale: LyricsSizeScale) {
        updateAndPersist(_style.value.copy(sizeScale = scale))
    }

    fun updateWeight(weight: LyricsWeightChoice) {
        updateAndPersist(_style.value.copy(weightChoice = weight))
    }

    fun updateCenterAligned(center: Boolean) {
        updateAndPersist(_style.value.copy(centerAligned = center))
    }

    fun reset() {
        updateAndPersist(LyricsCustomStyle())
    }
}

