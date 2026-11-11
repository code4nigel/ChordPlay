package com.music.bitchord.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * A rectangle whose corners are quarters of a superellipse rather than circular arcs:
 * the continuous curvature "squircle" of modern Apple design.
 *
 * The superellipse curve meets each straight edge with zero change in curvature (continuous
 * curvature), eliminating the pinched-in feeling of circular rounded rectangles and ensuring
 * nested concentric cards align harmoniously.
 *
 * @param radius How far in from each edge the corner curve reaches.
 * @param exponent The superellipse exponent `n` in `|x/a|^n + |y/b|^n = 1`. Defaults to 4.0f (squircle).
 */
@Immutable
data class SmoothCornerShape(
    val radius: Dp,
    val exponent: Float = 4f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { radius.toPx() }.coerceAtMost(minOf(size.width, size.height) / 2f)
        if (r <= 0f) return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        return Outline.Generic(
            Path().apply { addSmoothRoundedRect(size.width, size.height, r, exponent.toDouble()) }
        )
    }
}

private const val CORNER_STEPS = 12

private fun Path.addSmoothRoundedRect(w: Float, h: Float, r: Float, exponent: Double) {
    val power = 2.0 / exponent

    fun along(t: Double): Float = (r * abs(cos(t)).pow(power)).toFloat()
    fun across(t: Double): Float = (r * abs(sin(t)).pow(power)).toFloat()

    moveTo(r, 0f)
    lineTo(w - r, 0f)

    // Top-right corner
    for (i in 1..CORNER_STEPS) {
        val t = (PI / 2.0) * i / CORNER_STEPS
        lineTo(w - r + across(t), r - along(t))
    }
    lineTo(w, h - r)

    // Bottom-right corner
    for (i in 1..CORNER_STEPS) {
        val t = (PI / 2.0) * i / CORNER_STEPS
        lineTo(w - r + along(t), h - r + across(t))
    }
    lineTo(r, h)

    // Bottom-left corner
    for (i in 1..CORNER_STEPS) {
        val t = (PI / 2.0) * i / CORNER_STEPS
        lineTo(r - across(t), h - r + along(t))
    }
    lineTo(0f, r)

    // Top-left corner
    for (i in 1..CORNER_STEPS) {
        val t = (PI / 2.0) * i / CORNER_STEPS
        lineTo(r - along(t), r - across(t))
    }
    close()
}

