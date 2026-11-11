package com.music.bitchord.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Animated mechanical odometer-style rolling digit text.
 * When numbers update (e.g. playback timer 0:42 -> 0:43), the changing digits roll vertically
 * with smooth easing while non-digits remain static and layout bounds stay completely jitter-free.
 */
@Composable
fun OdometerText(
    value: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    animateOnScrub: Boolean = false,
) {
    val resolvedColor = if (color.isSpecified) color else style.color
    val fontSize = if (style.fontSize.isSpecified) style.fontSize else 14.sp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        value.forEach { char ->
            if (char.isDigit()) {
                SingleOdometerDigit(
                    targetDigit = char.digitToInt(),
                    fontSize = fontSize,
                    style = style,
                    color = resolvedColor,
                    animateOnScrub = animateOnScrub,
                )
            } else {
                Text(
                    text = char.toString(),
                    style = style,
                    color = resolvedColor,
                )
            }
        }
    }
}

@Composable
private fun SingleOdometerDigit(
    targetDigit: Int,
    fontSize: TextUnit,
    style: TextStyle,
    color: Color,
    animateOnScrub: Boolean,
) {
    val density = LocalDensity.current
    val digitHeightDp: Dp = with(density) { fontSize.toDp() * 1.25f }
    val digitWidthDp: Dp = with(density) { fontSize.toDp() * 0.62f }
    val digitHeightPx = with(density) { digitHeightDp.toPx() }

    val animatable = remember { Animatable(targetDigit.toFloat()) }

    LaunchedEffect(targetDigit) {
        val current = animatable.targetValue.toInt()
        val diff = abs(targetDigit - current)
        // If the jump is large (e.g. scrubbing or seeking tracks), snap directly to avoid spinning through digits
        if (!animateOnScrub && diff > 1) {
            animatable.snapTo(targetDigit.toFloat())
        } else {
            animatable.animateTo(
                targetValue = targetDigit.toFloat(),
                animationSpec = tween(
                    durationMillis = 360,
                    easing = FastOutSlowInEasing,
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .width(digitWidthDp)
            .height(digitHeightDp)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        val currentVal = animatable.value

        // Draw visible digits in the reel
        for (i in 0..9) {
            val offsetFromCurrent = i - currentVal
            if (offsetFromCurrent in -1.2f..1.2f) {
                val yOffsetDp = with(density) { (offsetFromCurrent * digitHeightPx).toDp() }
                val alpha = (1f - abs(offsetFromCurrent)).coerceIn(0f, 1f)

                Text(
                    text = i.toString(),
                    style = style,
                    color = color.copy(alpha = (color.alpha * alpha).coerceIn(0f, 1f)),
                    modifier = Modifier
                        .offset(y = yOffsetDp)
                        .graphicsLayer {
                            // Subtle vertical stretch during movement for natural motion blur feel
                            val stretch = 1f + abs(offsetFromCurrent) * 0.05f
                            scaleY = stretch
                        }
                )
            }
        }
    }
}

