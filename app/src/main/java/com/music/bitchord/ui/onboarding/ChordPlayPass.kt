package com.music.bitchord.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.music.bitchord.R
import com.music.bitchord.ui.player.MeshPalette
import kotlin.math.PI
import kotlin.math.sin

/**
 * Creative acoustic / vinyl-chord orbital hero visual rendered purely with
 * Compose [Canvas] + frosted glass styling (tinted by [MeshPalette], with
 * subtle [rememberDeviceTilt] parallax gated by `reduceAnimation`).
 */
@Composable
internal fun ChordPlayCommunityHero(
    modifier: Modifier = Modifier,
    palette: MeshPalette? = null,
    tilt: () -> Pair<Float, Float> = { 0f to 0f },
) {
    val shape = RoundedCornerShape(24.dp)
    val primaryGlow = palette?.colors?.getOrNull(0) ?: Color(0xFFFD3F69)
    val secondaryGlow = palette?.colors?.getOrNull(1) ?: Color(0xFF7C4DFF)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(198.dp)
            .graphicsLayer {
                val (rx, ry) = tilt()
                rotationX = -rx
                rotationY = ry
                cameraDistance = 18f * density
            }
            .onbSurface(shape = shape, elevated = true)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1A1424), Color(0xFF12121A), Color(0xFF0B0B10)),
                    start = Offset.Zero,
                    end = Offset(900f, 540f),
                ),
            )
            .border(1.dp, Onb.Hairline, shape),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Soft atmospheric glows
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryGlow.copy(alpha = 0.42f), Color.Transparent),
                    center = Offset(size.width * 0.78f, size.height * 0.30f),
                    radius = size.width * 0.54f,
                ),
                radius = size.width * 0.54f,
                center = Offset(size.width * 0.78f, size.height * 0.30f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondaryGlow.copy(alpha = 0.32f), Color.Transparent),
                    center = Offset(size.width * 0.16f, size.height * 0.84f),
                    radius = size.width * 0.44f,
                ),
                radius = size.width * 0.44f,
                center = Offset(size.width * 0.16f, size.height * 0.84f),
            )

            // Vinyl-chord orbital geometry centered behind the emblem focal axis
            val orbit = Offset(size.width * 0.80f, size.height * 0.36f)
            val baseRadius = size.height * 0.24f
            repeat(4) { idx ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.11f - idx * 0.02f),
                    radius = baseRadius * (1f + idx * 0.38f),
                    center = orbit,
                    style = Stroke(width = 1.2f * density),
                )
            }
            // Orbiting harmonic node dot
            drawCircle(
                color = primaryGlow.copy(alpha = 0.92f),
                radius = 3.5f * density,
                center = Offset(orbit.x - baseRadius * 0.92f, orbit.y + baseRadius * 0.42f),
            )

            // Dual acoustic soundwave arcs across the lower horizon
            val waveMidY = size.height * 0.72f
            val primaryWave = Path().apply {
                val steps = 40
                for (i in 0..steps) {
                    val fraction = i / steps.toFloat()
                    val x = size.width * fraction
                    val envelope = sin(fraction * PI).toFloat()
                    val y = waveMidY + sin(fraction * PI * 3.5f).toFloat() * (14f * density) * envelope
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(
                path = primaryWave,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        secondaryGlow.copy(alpha = 0.50f),
                        primaryGlow.copy(alpha = 0.75f),
                        Color.Transparent,
                    ),
                ),
                style = Stroke(width = 1.8f * density, cap = StrokeCap.Round),
            )

            val secondaryWave = Path().apply {
                val steps = 40
                for (i in 0..steps) {
                    val fraction = i / steps.toFloat()
                    val x = size.width * fraction
                    val envelope = sin(fraction * PI).toFloat()
                    val y = waveMidY + sin((fraction + 0.18f) * PI * 2.5f).toFloat() * (9f * density) * envelope
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(
                path = secondaryWave,
                color = Color.White.copy(alpha = 0.16f),
                style = Stroke(width = 1.1f * density, cap = StrokeCap.Round),
            )

            // Crisp miniature acoustic spectrum bars on the right side
            val barCount = 9
            val barWidth = 3.dp.toPx()
            val barGap = 4.5.dp.toPx()
            val startX = size.width - 20.dp.toPx() - barCount * (barWidth + barGap)
            val baseBottom = size.height - 20.dp.toPx()
            val heights = floatArrayOf(0.36f, 0.62f, 0.90f, 0.54f, 0.82f, 1.0f, 0.68f, 0.44f, 0.28f)
            heights.forEachIndexed { i, h ->
                val barHeight = 28.dp.toPx() * h
                drawRoundRect(
                    color = if (i % 2 == 0) primaryGlow.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.36f),
                    topLeft = Offset(startX + i * (barWidth + barGap), baseBottom - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val iconShape = RoundedCornerShape(16.dp)
                Image(
                    painter = painterResource(R.drawable.ic_chordplay_squircle),
                    contentDescription = "ChordPlay",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(iconShape)
                        .border(1.dp, Color.White.copy(alpha = 0.22f), iconShape),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "CHORDPLAY",
                        color = primaryGlow.copy(alpha = 0.92f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.20.em,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Built with Passion",
                        color = Onb.Title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = OnbSubtitleTracking,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Open-source music streaming,\nno ads & zero compromises.",
                    color = Onb.Title.copy(alpha = 0.88f),
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(0.75.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "by Shivanshu (@code4nigel)",
                            color = Onb.Title.copy(alpha = 0.86f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
