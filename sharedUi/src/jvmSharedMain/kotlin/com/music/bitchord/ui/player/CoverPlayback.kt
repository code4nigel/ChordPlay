package com.music.bitchord.ui.player

import com.music.bitchord.data.model.Song
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

/** Direction and timing describe the confirmed track change. */
enum class CoverChange(val direction: Int, automatic: Boolean = false) {
    NEXT(1),
    PREVIOUS(-1),
    AUTOMATIC(1, automatic = true),
    SELECTION(0);

    val durationMs: Int = if (automatic) 420 else 220
    val easing: Easing = if (automatic) {
        CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    } else {
        CubicBezierEasing(0.35f, 0f, 0.20f, 1f)
    }
    val animationSpec = tween<Float>(durationMs, easing = easing)
}

data class CoverTrackChange(
    val outgoing: Song,
    val incoming: Song,
    val change: CoverChange,
)

fun Song.coverEntryKey(): String = queueEntryId ?: videoId
