package com.music.bitchord.ui.player

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.CoverAnimationMode

/** Direction and timing describe the confirmed track change. */
enum class CoverChange(val direction: Int, automatic: Boolean = false) {
    NEXT(1),
    PREVIOUS(-1),
    AUTOMATIC(1, automatic = true),
    SELECTION(0);

    val durationMs: Int = if (automatic) 560 else 380
    val easing: Easing = if (automatic) {
        CubicBezierEasing(0.35f, 0f, 0.25f, 1f)
    } else {
        CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    }
    val animationSpec = tween<Float>(durationMs, easing = easing)

    fun durationMsFor(mode: CoverAnimationMode): Int = when (mode) {
        CoverAnimationMode.NONE -> 0
        CoverAnimationMode.DIRECTIONAL_SWEEP -> 600
        CoverAnimationMode.DISINTEGRATE -> 680
    }

    fun animationSpecFor(mode: CoverAnimationMode) = tween<Float>(durationMsFor(mode), easing = AUTOMATIC.easing)
}

data class CoverTrackChange(
    val outgoing: Song,
    val incoming: Song,
    val change: CoverChange,
)

fun Song.coverEntryKey(): String = queueEntryId ?: videoId
