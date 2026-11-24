package com.music.bitchord.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.settings.CoverAnimationMode
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

internal class CoverPresentation(
    val outgoing: Painter?,
    val incoming: Painter?,
    val mode: CoverAnimationMode,
    val direction: Int,
    val progress: () -> Float,
    val outgoingColors: List<Color>,
    val seed: Int,
)

internal data class CoverLayerTransform(
    val translationX: Float,
    val translationY: Float,
    val rotationY: Float,
    val rotationX: Float,
    val scale: Float,
    val alpha: Float,
    val cameraDistanceDp: Float,
)

private data class DisplayedCover(val entry: String, val painter: Painter)

private val DefaultDustColors = listOf(
    Color(0xFFDCE2EE),
    Color(0xFF9FB4D6),
    Color(0xFFE7B892),
    Color(0xFFA292C9),
)

internal fun computeCoverTransform(
    mode: CoverAnimationMode,
    direction: Int,
    entering: Boolean,
    progress: Float,
    width: Float,
    @Suppress("UNUSED_PARAMETER") height: Float,
): CoverLayerTransform {
    val t = progress.coerceIn(0f, 1f)
    val rem = 1f - t
    if (mode == CoverAnimationMode.NONE) {
        return CoverLayerTransform(
            translationX = 0f,
            translationY = 0f,
            rotationY = 0f,
            rotationX = 0f,
            scale = 1f,
            alpha = if (entering) 1f else 0f,
            cameraDistanceDp = 16f,
        )
    }

    val directional = direction != 0
    return when (mode) {
        CoverAnimationMode.DIRECTIONAL_SWEEP -> {
            val cameraDp = 16f
            if (directional) {
                if (entering) {
                    CoverLayerTransform(
                        translationX = direction * width * 0.14f * rem,
                        translationY = 0f,
                        rotationY = direction * 18f * rem,
                        rotationX = 3.5f * rem,
                        scale = 0.92f + 0.08f * t,
                        alpha = (t / 0.18f).coerceIn(0f, 1f),
                        cameraDistanceDp = cameraDp,
                    )
                } else {
                    CoverLayerTransform(
                        translationX = -direction * width * 0.16f * t,
                        translationY = 0f,
                        rotationY = -direction * 22f * t,
                        rotationX = -3f * t,
                        scale = 1f - 0.08f * t,
                        alpha = 1f - ((t - 0.72f) / 0.28f).coerceIn(0f, 1f),
                        cameraDistanceDp = cameraDp,
                    )
                }
            } else {
                if (entering) {
                    CoverLayerTransform(
                        translationX = 0f,
                        translationY = 0f,
                        rotationY = 0f,
                        rotationX = 8f * rem,
                        scale = 0.88f + 0.12f * t,
                        alpha = (t / 0.20f).coerceIn(0f, 1f),
                        cameraDistanceDp = cameraDp,
                    )
                } else {
                    CoverLayerTransform(
                        translationX = 0f,
                        translationY = 0f,
                        rotationY = 0f,
                        rotationX = -6f * t,
                        scale = 1f + 0.07f * t,
                        alpha = 1f - ((t - 0.65f) / 0.35f).coerceIn(0f, 1f),
                        cameraDistanceDp = cameraDp,
                    )
                }
            }
        }

        CoverAnimationMode.DISINTEGRATE -> {
            val cameraDp = 14f
            if (directional) {
                if (entering) {
                    CoverLayerTransform(
                        translationX = direction * width * 0.18f * rem,
                        translationY = 0f,
                        rotationY = direction * 24f * rem,
                        rotationX = 4.5f * rem,
                        scale = 0.90f + 0.10f * t,
                        alpha = ((t - 0.06f) / 0.24f).coerceIn(0f, 1f),
                        cameraDistanceDp = cameraDp,
                    )
                } else {
                    CoverLayerTransform(
                        translationX = -direction * width * 0.22f * t,
                        translationY = 0f,
                        rotationY = -direction * 28f * t,
                        rotationX = -5f * t,
                        scale = 1f - 0.10f * t,
                        alpha = 1f - ((t - 0.78f) / 0.22f).coerceIn(0f, 1f),
                        cameraDistanceDp = cameraDp,
                    )
                }
            } else {
                if (entering) {
                    CoverLayerTransform(
                        translationX = 0f,
                        translationY = 0f,
                        rotationY = 0f,
                        rotationX = 9f * rem,
                        scale = 0.86f + 0.14f * t,
                        alpha = ((t - 0.08f) / 0.24f).coerceIn(0f, 1f),
                        cameraDistanceDp = cameraDp,
                    )
                } else {
                    CoverLayerTransform(
                        translationX = 0f,
                        translationY = 0f,
                        rotationY = 0f,
                        rotationX = -10f * t,
                        scale = 1f - 0.10f * t,
                        alpha = 1f - ((t - 0.75f) / 0.25f).coerceIn(0f, 1f),
                        cameraDistanceDp = cameraDp,
                    )
                }
            }
        }

        CoverAnimationMode.NONE -> error("Unreachable")
    }
}

@Composable
internal fun rememberCoverPresentation(
    song: Song,
    currentArt: PlayerArtwork,
    artPx: Int,
    enabled: Boolean,
    upcoming: Song?,
    paletteColors: List<Color> = emptyList(),
): CoverPresentation? {
    val host = PlayerPlatform.host
    val mode by host.settings.coverAnimation.collectAsStateWithLifecycle()
    val firstChange = remember(host) { host.coverTrackChange.value }
    val entry = song.coverEntryKey()
    var lastShown by remember { mutableStateOf<DisplayedCover?>(null) }
    var lastPalette by remember { mutableStateOf(paletteColors) }
    val steadyPainter = currentArt.loadedPainter
    if (!enabled || mode == CoverAnimationMode.NONE) {
        if (steadyPainter != null) {
            SideEffect {
                lastShown = DisplayedCover(entry, steadyPainter)
                if (paletteColors.isNotEmpty()) lastPalette = paletteColors
            }
        }
        return null
    }
    val trackChange by host.coverTrackChange.collectAsStateWithLifecycle()
    val change = trackChange?.takeIf { it !== firstChange && it.incoming.coverEntryKey() == entry }
    val dustColors = remember(change) {
        when {
            lastPalette.isNotEmpty() -> lastPalette
            paletteColors.isNotEmpty() -> paletteColors
            else -> DefaultDustColors
        }
    }
    val timed = remember(change) { Animatable(0f) }
    val finished by remember(timed) { derivedStateOf { timed.value >= 1f } }
    val pending = change?.takeUnless { finished }
    val from = pending?.outgoing
    val to = pending?.incoming ?: upcoming
    val outgoingUrl = rememberRemoteArtworkUrl(from)
    val incomingUrl = rememberRemoteArtworkUrl(to)
    val canBlend = from != null && outgoingUrl?.artworkAt(artPx) != incomingUrl?.artworkAt(artPx)
    val outgoing = rememberCoverPainter(outgoingUrl.takeIf { canBlend }, artPx)
    val incoming = rememberCoverPainter(incomingUrl, artPx)
    val incomingState = incoming?.state?.collectAsStateWithLifecycle()?.value
    val outgoingState = outgoing?.state?.collectAsStateWithLifecycle()?.value
    val outgoingPainter = lastShown?.painter
        ?: (outgoingState as? AsyncImagePainter.State.Success)?.painter
    val incomingPainter = steadyPainter.takeIf { to?.coverEntryKey() == entry }
        ?: (incomingState as? AsyncImagePainter.State.Success)?.painter
    val incomingReady = incomingPainter != null
    val unavailable = !incomingReady && (incoming == null || incomingState is AsyncImagePainter.State.Error)

    LaunchedEffect(timed, incomingReady, unavailable, canBlend, mode) {
        if (pending == null) return@LaunchedEffect
        when {
            unavailable || !canBlend -> timed.snapTo(1f)
            incomingReady -> timed.animateTo(1f, pending.change.animationSpecFor(mode))
        }
    }

    if (pending == null || !canBlend || unavailable || (outgoingPainter == null && !incomingReady)) {
        val held = lastShown?.takeIf {
            it.entry != entry && steadyPainter == null && !currentArt.failed
        }
        if (held != null) {
            return CoverPresentation(
                outgoing = held.painter,
                incoming = null,
                mode = mode,
                direction = 0,
                progress = { 0f },
                outgoingColors = dustColors,
                seed = entry.hashCode(),
            )
        }
        if (steadyPainter != null) {
            SideEffect {
                lastShown = DisplayedCover(entry, steadyPainter)
                if (paletteColors.isNotEmpty()) lastPalette = paletteColors
            }
        }
        return null
    }
    return CoverPresentation(
        outgoing = outgoingPainter,
        incoming = incomingPainter,
        mode = mode,
        direction = pending.change.direction,
        progress = { if (!incomingReady) 0f else if (outgoingPainter == null) 1f else timed.value },
        outgoingColors = dustColors,
        seed = change.hashCode(),
    )
}

@Composable
private fun rememberCoverPainter(url: String?, artPx: Int): AsyncImagePainter? {
    if (url == null) return null
    val art = rememberPlayerArtwork(url, artPx)
    return rememberAsyncImagePainter(art.request, contentScale = ContentScale.Crop, onState = art::onState)
}

@Composable
internal fun CoverMotion(
    presentation: CoverPresentation,
    modifier: Modifier = Modifier,
) {
    CoverMotionLayers(
        mode = presentation.mode,
        direction = presentation.direction,
        progress = presentation.progress,
        modifier = modifier,
        outgoingColors = presentation.outgoingColors,
        seed = presentation.seed,
    ) { entering, layerModifier ->
        val painter = if (entering) presentation.incoming else presentation.outgoing
        if (painter != null) Image(painter, null, layerModifier, contentScale = ContentScale.Crop)
    }
}

/** Shared with the settings preview so the selected motion is the one it actually shows. */
@Composable
fun CoverMotionLayers(
    mode: CoverAnimationMode,
    direction: Int,
    progress: () -> Float,
    modifier: Modifier = Modifier,
    outgoingColors: List<Color> = DefaultDustColors,
    seed: Int = 0,
    cover: @Composable (entering: Boolean, modifier: Modifier) -> Unit,
) {
    Box(modifier.fillMaxSize().clipToBounds()) {
        val baseIncoming = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val t = if (mode == CoverAnimationMode.NONE) 1f else progress().coerceIn(0f, 1f)
                val xform = computeCoverTransform(mode, direction, entering = true, t, size.width, size.height)
                translationX = xform.translationX
                translationY = xform.translationY
                rotationY = xform.rotationY
                rotationX = xform.rotationX
                scaleX = xform.scale
                scaleY = xform.scale
                alpha = xform.alpha
                cameraDistance = xform.cameraDistanceDp * density
            }
        val incomingModifier = when {
            mode == CoverAnimationMode.NONE -> baseIncoming
            coverMotionShaderSupported -> baseIncoming.coverMotionShaderEffect(
                mode = mode,
                direction = direction,
                entering = true,
                progress = progress,
            )
            else -> baseIncoming.coverFallbackSweepMask(
                mode = mode,
                direction = direction,
                entering = true,
                progress = progress,
            )
        }
        cover(true, incomingModifier)

        if (mode != CoverAnimationMode.NONE) {
            val baseOutgoing = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val t = progress().coerceIn(0f, 1f)
                    val xform = computeCoverTransform(mode, direction, entering = false, t, size.width, size.height)
                    translationX = xform.translationX
                    translationY = xform.translationY
                    rotationY = xform.rotationY
                    rotationX = xform.rotationX
                    scaleX = xform.scale
                    scaleY = xform.scale
                    alpha = xform.alpha
                    cameraDistance = xform.cameraDistanceDp * density
                }
            val outgoingModifier = if (coverMotionShaderSupported) {
                baseOutgoing.coverMotionShaderEffect(
                    mode = mode,
                    direction = direction,
                    entering = false,
                    progress = progress,
                )
            } else {
                baseOutgoing.coverFallbackSweepMask(
                    mode = mode,
                    direction = direction,
                    entering = false,
                    progress = progress,
                )
            }
            cover(false, outgoingModifier)
        }

        if (mode == CoverAnimationMode.DISINTEGRATE) {
            DisintegrateParticlesOverlay(
                direction = direction,
                progress = progress,
                palette = outgoingColors,
                seed = seed,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun Modifier.coverFallbackSweepMask(
    mode: CoverAnimationMode,
    direction: Int,
    entering: Boolean,
    progress: () -> Float,
): Modifier {
    if (mode == CoverAnimationMode.NONE) return this
    val feather = if (mode == CoverAnimationMode.DISINTEGRATE) 0.44f else 0.54f
    return this
        .graphicsLayer {
            val t = progress().coerceIn(0f, 1f)
            if (t in 0.001f..0.999f) {
                compositingStrategy = CompositingStrategy.Offscreen
            }
        }
        .drawWithContent {
            drawContent()
            val t = progress().coerceIn(0f, 1f)
            if (t <= 0.001f || t >= 0.999f || size.width <= 1f || size.height <= 1f) return@drawWithContent
            val front = -0.02f + (1.04f + feather) * t
            val brush = if (direction != 0) {
                if (direction > 0) {
                    val x0 = (1f - front) * size.width
                    val x1 = (1f - (front - feather)) * size.width
                    val colors = if (entering) {
                        listOf(Color.Transparent, Color.Black)
                    } else {
                        listOf(Color.Black, Color.Transparent)
                    }
                    Brush.horizontalGradient(colors = colors, startX = x0, endX = x1)
                } else {
                    val x0 = (front - feather) * size.width
                    val x1 = front * size.width
                    val colors = if (entering) {
                        listOf(Color.Black, Color.Transparent)
                    } else {
                        listOf(Color.Transparent, Color.Black)
                    }
                    Brush.horizontalGradient(colors = colors, startX = x0, endX = x1)
                }
            } else {
                val maxR = hypot(size.width, size.height) * 0.55f
                val r1 = (front * maxR).coerceAtLeast(1f)
                val r0 = ((front - feather) * maxR).coerceIn(0f, r1 - 0.5f)
                val stop = (r0 / r1).coerceIn(0f, 0.99f)
                if (entering) {
                    Brush.radialGradient(
                        0f to Color.Black,
                        stop to Color.Black,
                        1f to Color.Transparent,
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = r1,
                    )
                } else {
                    Brush.radialGradient(
                        0f to Color.Transparent,
                        stop to Color.Transparent,
                        1f to Color.Black,
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = r1,
                    )
                }
            }
            drawRect(brush = brush, blendMode = BlendMode.DstIn)
        }
}

private data class DustParticle(
    val homeX: Float,
    val homeY: Float,
    val triggerBias: Float,
    val speedX: Float,
    val liftY: Float,
    val curlAmp: Float,
    val curlFreq: Float,
    val phase: Float,
    val radiusDp: Float,
    val colorIdx: Int,
    val glow: Boolean,
    val life: Float,
)

private fun buildDustParticles(seed: Int, count: Int = 150): List<DustParticle> {
    var state = (seed.toLong() * 6364136223846793005L + 1442695040888963407L)
    fun nextFloat(): Float {
        state = state * 6364136223846793005L + 1442695040888963407L
        val bits = ((state ushr 40) and 0xFFFFFFL).toInt()
        return bits / 16777216f
    }
    return List(count) { i ->
        DustParticle(
            homeX = 0.05f + nextFloat() * 0.90f,
            homeY = 0.05f + nextFloat() * 0.90f,
            triggerBias = nextFloat(),
            speedX = 0.16f + nextFloat() * 0.24f,
            liftY = -(0.04f + nextFloat() * 0.16f) + (nextFloat() - 0.5f) * 0.05f,
            curlAmp = 0.015f + nextFloat() * 0.04f,
            curlFreq = 4f + nextFloat() * 5f,
            phase = nextFloat() * 6.283185f,
            radiusDp = 1.4f + nextFloat() * 2.4f,
            colorIdx = (nextFloat() * 1024f).toInt(),
            glow = i % 4 == 0,
            life = 0.28f + nextFloat() * 0.20f,
        )
    }
}

@Composable
private fun DisintegrateParticlesOverlay(
    direction: Int,
    progress: () -> Float,
    palette: List<Color>,
    seed: Int,
    modifier: Modifier = Modifier,
) {
    val particles = remember(seed) { buildDustParticles(seed) }
    val resolvedPalette = remember(palette) {
        val base = palette.ifEmpty { DefaultDustColors }
        base.map { c -> lerp(c, Color.White, 0.14f).copy(alpha = 1f) }
    }
    Canvas(modifier) {
        val t = progress().coerceIn(0f, 1f)
        if (t <= 0.01f || t >= 0.99f || size.width <= 1f || size.height <= 1f) return@Canvas
        val minDim = minOf(size.width, size.height)
        for (i in particles.indices) {
            val p = particles[i]
            val coord = if (direction != 0) {
                if (direction > 0) 1f - p.homeX else p.homeX
            } else {
                hypot(p.homeX - 0.5f, p.homeY - 0.5f) * 1.45f
            }
            val triggerT = (coord * 0.52f + p.triggerBias * 0.18f).coerceIn(0.02f, 0.70f)
            if (t <= triggerT) continue
            val localT = ((t - triggerT) / p.life).coerceIn(0f, 1f)
            if (localT >= 1f) continue

            val fadeIn = (localT / 0.12f).coerceIn(0f, 1f)
            val fadeOut = (1f - localT).pow(1.6f)
            val alpha = (fadeIn * fadeOut).coerceIn(0f, 1f)
            if (alpha <= 0.01f) continue

            val anchorX = if (direction != 0) -direction * size.width * 0.22f * triggerT else 0f
            val baseX = p.homeX * size.width + anchorX
            val baseY = p.homeY * size.height
            val px: Float
            val py: Float
            if (direction != 0) {
                val drift = -direction * size.width * p.speedX * localT * (0.4f + 0.6f * localT)
                val dy = size.height * (p.liftY * localT + p.curlAmp * sin(localT * p.curlFreq + p.phase))
                px = baseX + drift
                py = baseY + dy
            } else {
                val ox = p.homeX - 0.5f
                val oy = p.homeY - 0.5f
                val len = hypot(ox, oy).coerceAtLeast(0.001f)
                val vx = ox / len
                val vy = oy / len
                val radialDist = minDim * p.speedX * 0.75f * localT * (0.4f + 0.6f * localT)
                val swirl = minDim * p.curlAmp * sin(localT * p.curlFreq + p.phase)
                px = baseX + vx * radialDist - vy * swirl
                py = baseY + vy * radialDist + vx * swirl + size.height * p.liftY * 0.45f * localT
            }

            val r = p.radiusDp.dp.toPx() * (1f - 0.55f * localT)
            if (r <= 0.25f) continue
            val color = resolvedPalette[p.colorIdx % resolvedPalette.size]
            if (p.glow) {
                drawCircle(
                    color = color.copy(alpha = alpha * 0.26f),
                    radius = r * 2.4f,
                    center = Offset(px, py),
                )
            }
            drawCircle(
                color = color.copy(alpha = alpha * 0.92f),
                radius = r,
                center = Offset(px, py),
            )
        }
    }
}
