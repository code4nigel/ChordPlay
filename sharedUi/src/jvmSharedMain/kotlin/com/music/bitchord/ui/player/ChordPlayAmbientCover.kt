package com.music.bitchord.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Breathing ambient aura rendered behind the squircle album cover in ChordPlay Modern mode.
 * Creates an atmospheric audio stage that pulses rhythmically during audio playback.
 */
@Composable
fun ChordPlayAmbientAura(
    model: Any?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ChordPlayAuraPulse")

    // Breathing pulse scale: 1.05f to 1.12f when playing
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.05f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "AuraScalePulse",
    )

    // Breathing pulse opacity: 0.45f to 0.78f when playing
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "AuraAlphaPulse",
    )

    val targetScale = if (isPlaying) pulseScale else 1.01f
    val targetAlpha = if (isPlaying) pulseAlpha else 0.22f

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "AuraScale",
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "AuraAlpha",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
            }
            .blur(38.dp, BlurredEdgeTreatment.Unbounded)
            .clip(RoundedCornerShape(32.dp)),
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Deep nocturnal obsidian-espresso backdrop vignette and flowing organic synesthesia
 * ribbons derived from the artwork's extracted [MeshPalette].
 */
@Composable
fun SynesthesiaNocturneOverlay(
    palette: MeshPalette,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val c0 by animateColorAsState(
        targetValue = palette.colors.getOrElse(0) { Color(0xFFFF4B2B) },
        animationSpec = tween(900),
        label = "synC0",
    )
    val c1 by animateColorAsState(
        targetValue = palette.colors.getOrElse(1) { Color(0xFFFF8C42) },
        animationSpec = tween(900),
        label = "synC1",
    )
    val c2 by animateColorAsState(
        targetValue = palette.colors.getOrElse(2) { Color(0xFF2CE668) },
        animationSpec = tween(900),
        label = "synC2",
    )

    val infinite = rememberInfiniteTransition(label = "SynesthesiaRibbons")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "RibbonPhase",
    )
    val activeAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.55f,
        animationSpec = tween(700),
        label = "RibbonActiveAlpha",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Soft atmospheric vignette so flipped blurred artwork shines through
        drawRect(
            brush = Brush.verticalGradient(
                0.00f to Color.Transparent,
                0.45f to Color.Black.copy(alpha = 0.15f),
                0.72f to Color.Black.copy(alpha = 0.40f),
                1.00f to Color.Black.copy(alpha = 0.58f),
            ),
        )

        // 2. Ambient color bloom pockets from artwork palette
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c0.copy(alpha = 0.20f * activeAlpha), Color.Transparent),
                center = Offset(w * 0.28f, h * 0.50f),
                radius = w * 0.65f,
            ),
            center = Offset(w * 0.28f, h * 0.50f),
            radius = w * 0.65f,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c2.copy(alpha = 0.18f * activeAlpha), Color.Transparent),
                center = Offset(w * 0.82f, h * 0.42f),
                radius = w * 0.60f,
            ),
            center = Offset(w * 0.82f, h * 0.42f),
            radius = w * 0.60f,
        )

        val drift1 = sin(phase) * 14.dp.toPx() * activeAlpha
        val drift2 = cos(phase * 0.8f) * 18.dp.toPx() * activeAlpha

        // 3. Lower Footer Atmospheric Sweep (bottom edge glow)
        val ribbonBottom = Path().apply {
            moveTo(w * 0.15f, h * 0.99f)
            cubicTo(
                w * 0.48f, h * 0.95f + drift1 * 0.4f,
                w * 0.78f, h * 0.96f - drift2 * 0.4f,
                w * 1.05f, h * 0.85f,
            )
        }
        val brushBottom = Brush.linearGradient(
            colors = listOf(c0.copy(alpha = 0.45f), c1.copy(alpha = 0.35f), c2.copy(alpha = 0.55f)),
            start = Offset(w * 0.15f, h),
            end = Offset(w, h * 0.85f),
        )
        drawPath(ribbonBottom, brushBottom, alpha = 0.16f * activeAlpha, style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round))
        drawPath(ribbonBottom, brushBottom, alpha = 0.50f * activeAlpha, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
    }
}
