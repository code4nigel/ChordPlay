package com.music.bitchord.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.music.bitchord.data.settings.CoverAnimationMode

internal actual val coverMotionShaderSupported: Boolean = false

@Composable
internal actual fun Modifier.coverMotionShaderEffect(
    mode: CoverAnimationMode,
    direction: Int,
    entering: Boolean,
    progress: () -> Float,
): Modifier = this
