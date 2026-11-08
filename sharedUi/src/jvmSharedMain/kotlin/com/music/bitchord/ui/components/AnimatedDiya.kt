package com.music.bitchord.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animated traditional earthen Diya (lamp) with an active, flickering golden flame
 * and ambient radial glow aura for festive celebrations.
 */
@Composable
fun AnimatedDiya(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    isLit: Boolean = true,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "DiyaFlameTransition")

    // Flame height breathing
    val flameScaleY by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "FlameScaleY",
    )

    // Subtle horizontal flame sway
    val flameSwayDeg by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "FlameSwayDeg",
    )

    // Flame core radiance pulse
    val flameGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "FlameGlowAlpha",
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // 1. Ambient Golden Halo / Radial Glow
        if (isLit) {
            val flameCenter = Offset(w * 0.5f, h * 0.36f)
            val glowRadius = w * 0.58f * flameScaleY
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFB300).copy(alpha = 0.40f * flameGlowAlpha),
                        Color(0xFFFF7043).copy(alpha = 0.18f * flameGlowAlpha),
                        Color.Transparent,
                    ),
                    center = flameCenter,
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = flameCenter,
            )
        }

        // 2. Terracotta Clay Lamp Base (Earthen Deepam)
        drawDiyaBase(w, h)

        // 3. Flickering Flame & Radiant Core
        if (isLit) {
            rotate(
                degrees = flameSwayDeg,
                pivot = Offset(w * 0.5f, h * 0.46f),
            ) {
                scale(
                    scaleX = 1f,
                    scaleY = flameScaleY,
                    pivot = Offset(w * 0.5f, h * 0.46f),
                ) {
                    drawFlame(w, h, flameGlowAlpha)
                }
            }
        }
    }
}

private fun DrawScope.drawDiyaBase(w: Float, h: Float) {
    // Elegant curved earthen bowl
    val bowlPath = Path().apply {
        moveTo(w * 0.12f, h * 0.55f)
        // Upper rim lip dipping gently in center
        cubicTo(
            w * 0.30f, h * 0.58f,
            w * 0.70f, h * 0.58f,
            w * 0.88f, h * 0.55f,
        )
        // Right bowl flank curving to base
        cubicTo(
            w * 0.88f, h * 0.78f,
            w * 0.68f, h * 0.88f,
            w * 0.58f, h * 0.88f,
        )
        // Flat pedestal foot
        lineTo(w * 0.42f, h * 0.88f)
        // Left bowl flank curving back up
        cubicTo(
            w * 0.32f, h * 0.88f,
            w * 0.12f, h * 0.78f,
            w * 0.12f, h * 0.55f,
        )
        close()
    }

    // Rich terracotta & burnished gold gradient
    val baseBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE65100), // Rich amber terracotta
            Color(0xFFBF360C), // Deep fired clay
            Color(0xFF870000), // Shadowed base
        ),
        startY = h * 0.55f,
        endY = h * 0.88f,
    )
    drawPath(bowlPath, brush = baseBrush)

    // Golden rim highlight
    val rimPath = Path().apply {
        moveTo(w * 0.15f, h * 0.55f)
        cubicTo(
            w * 0.32f, h * 0.59f,
            w * 0.68f, h * 0.59f,
            w * 0.85f, h * 0.55f,
        )
    }
    drawPath(
        path = rimPath,
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFFD54F).copy(alpha = 0.5f),
                Color(0xFFFFE082),
                Color(0xFFFFD54F).copy(alpha = 0.5f),
            ),
        ),
        style = Stroke(width = w * 0.06f),
    )
}

private fun DrawScope.drawFlame(w: Float, h: Float, glowAlpha: Float) {
    // Outer flame teardrop (Orange to Golden Yellow)
    val outerFlamePath = Path().apply {
        moveTo(w * 0.50f, h * 0.12f) // Tip
        cubicTo(
            w * 0.66f, h * 0.28f,
            w * 0.64f, h * 0.46f,
            w * 0.50f, h * 0.48f, // Base
        )
        cubicTo(
            w * 0.36f, h * 0.46f,
            w * 0.34f, h * 0.28f,
            w * 0.50f, h * 0.12f, // Return to tip
        )
        close()
    }

    val outerFlameBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFD600), // Brilliant gold at tip
            Color(0xFFFF9100), // Warm amber
            Color(0xFFFF3D00).copy(alpha = 0.9f), // Fiery orange-red at wick base
        ),
        startY = h * 0.12f,
        endY = h * 0.48f,
    )
    drawPath(outerFlamePath, brush = outerFlameBrush)

    // Inner incandescent white-gold core
    val innerFlamePath = Path().apply {
        moveTo(w * 0.50f, h * 0.24f)
        cubicTo(
            w * 0.58f, h * 0.34f,
            w * 0.56f, h * 0.45f,
            w * 0.50f, h * 0.46f,
        )
        cubicTo(
            w * 0.44f, h * 0.45f,
            w * 0.42f, h * 0.34f,
            w * 0.50f, h * 0.24f,
        )
        close()
    }

    val innerFlameBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF).copy(alpha = glowAlpha),
            Color(0xFFFFF9C4).copy(alpha = glowAlpha * 0.95f),
            Color(0xFFFFE082).copy(alpha = 0.6f),
        ),
        startY = h * 0.24f,
        endY = h * 0.46f,
    )
    drawPath(innerFlamePath, brush = innerFlameBrush)
}

