package com.music.bitchord.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.music.bitchord.data.settings.CoverAnimationMode

/** Whether GPU AGSL shader effects are supported for cover transitions on this platform. */
internal expect val coverMotionShaderSupported: Boolean

/** Applies the active GPU cover dissolve/sweep shader when supported by the host platform. */
@Composable
internal expect fun Modifier.coverMotionShaderEffect(
    mode: CoverAnimationMode,
    direction: Int,
    entering: Boolean,
    progress: () -> Float,
): Modifier
