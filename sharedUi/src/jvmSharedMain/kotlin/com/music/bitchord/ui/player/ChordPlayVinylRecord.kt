package com.music.bitchord.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.isActive

/**
 * High-fidelity vinyl record disc that spins continuously during playback
 * and slides out from behind the album jacket in [PlayerLayoutMode.VINYL_RECORD].
 */
@Composable
fun ChordPlayVinylRecord(
    model: Any?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val currentRotation = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                currentRotation.animateTo(
                    targetValue = currentRotation.value + 360f,
                    animationSpec = tween(durationMillis = 6500, easing = LinearEasing),
                )
            }
        }
    }

    Box(
        modifier = modifier
            .shadow(16.dp, CircleShape, spotColor = Color.Black)
            .clip(CircleShape)
            .background(Color(0xFF101114)),
        contentAlignment = Alignment.Center,
    ) {
        // Grooved vinyl surface with realistic anisotropic light sheen
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val maxR = size.minDimension / 2f
            val labelR = maxR * 0.38f

            // Micro-grooves between the center label and the outer run-in groove
            var r = labelR + 4f
            val step = 5f
            while (r < maxR - 8f) {
                drawCircle(
                    color = Color(0x14FFFFFF),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1f),
                )
                r += step
            }

            // Outer lead-in groove
            drawCircle(
                color = Color(0x28FFFFFF),
                radius = maxR - 3f,
                center = center,
                style = Stroke(width = 1.5f),
            )

            // Inner lead-out groove
            drawCircle(
                color = Color(0x28FFFFFF),
                radius = labelR + 2f,
                center = center,
                style = Stroke(width = 1.5f),
            )

            // Anisotropic light sheen reflecting overhead lighting on the micro-grooves
            val sheenBrush = Brush.sweepGradient(
                0.00f to Color.Transparent,
                0.12f to Color(0x18FFFFFF),
                0.25f to Color.Transparent,
                0.50f to Color.Transparent,
                0.62f to Color(0x18FFFFFF),
                0.75f to Color.Transparent,
                1.00f to Color.Transparent,
            )
            drawCircle(
                brush = sheenBrush,
                radius = maxR,
                center = center,
            )
        }

        // Center album label that rotates with playback
        Box(
            modifier = Modifier
                .fillMaxSize(0.38f)
                .rotate(currentRotation.value)
                .clip(CircleShape)
                .border(1.dp, Color(0x33FFFFFF), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Inner dark spindle core with metallic rim
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0C0D10))
                    .border(1.5.dp, Color(0xFF636670), CircleShape),
            )
        }
    }
}
