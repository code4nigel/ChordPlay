package com.music.bitchord.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.NerdStats
import com.music.bitchord.data.model.Song
import com.music.bitchord.sharedui.resources.*
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.icons.BitChordIcons
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

private class RippleWave(
    val id: Long,
    val anim: Animatable<Float, AnimationVector1D> = Animatable(0f),
)

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%d:%02d".format(Locale.ROOT, minutes, seconds)
}

/**
 * Computes a song-specific, deterministic audio energy profile across 96 timeline points.
 * Every song receives a distinct, consistent waveform profile with chorus peaks and verse dips.
 */
@Composable
private fun rememberSongWaveform(trackKey: String, durationMs: Long): FloatArray {
    return remember(trackKey, durationMs) {
        val seed = (trackKey.hashCode().toLong() shl 16) xor durationMs
        val rng = Random(seed)
        val numPeaks = 4 + (rng.nextInt(4))
        val peakCenters = FloatArray(numPeaks) {
            0.10f + 0.80f * (it.toFloat() / (numPeaks - 1)) + (rng.nextFloat() * 0.08f - 0.04f)
        }
        val peakWidths = FloatArray(numPeaks) { 0.05f + rng.nextFloat() * 0.045f }
        val peakHeights = FloatArray(numPeaks) { 0.40f + rng.nextFloat() * 0.55f }
        val secondaryFrequencies = FloatArray(3) { 8f + rng.nextFloat() * 12f }
        val secondaryPhases = FloatArray(3) { rng.nextFloat() * (2f * PI).toFloat() }

        val samples = 96
        FloatArray(samples + 1) { i ->
            val t = i.toFloat() / samples
            var energy = 0.08f
            for (p in 0 until numPeaks) {
                val dx = (t - peakCenters[p]) / peakWidths[p]
                energy += peakHeights[p] * exp(-dx * dx)
            }
            for (s in 0 until 3) {
                energy += 0.04f * sin(t * secondaryFrequencies[s] + secondaryPhases[s])
            }
            val taper = sin((t * PI).toFloat()).coerceIn(0f, 1f)
            (energy * (0.35f + 0.65f * taper)).coerceIn(0.06f, 1.0f)
        }
    }
}

/**
 * Organic multi-peak waveform seekbar for WavePlay mode.
 * Adapts dynamically to the song's energy profile and extracted artwork palette.
 */
@Composable
fun SynesthesiaWaveformScrubber(
    shown: () -> Float,
    durationMs: Long,
    isPlaying: Boolean,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    trackKey: String = "",
    palette: MeshPalette = remember { MeshPalette(emptyList()) },
    modifier: Modifier = Modifier,
) {
    val progress = shown().coerceIn(0f, 1f)
    val haptics = rememberHaptics()
    var isDragging by remember { mutableStateOf(false) }
    var lastScrubSec by remember { mutableLongStateOf(-1L) }

    // Dynamic Artwork Colors
    val accentColor by animateColorAsState(
        targetValue = palette.colors.getOrElse(0) { Color(0xFFFF5722) },
        animationSpec = tween(700),
        label = "ScrubAccent",
    )
    val secondaryAccent by animateColorAsState(
        targetValue = palette.colors.getOrElse(1) { Color(0xFFFF9E58) },
        animationSpec = tween(700),
        label = "ScrubSecAccent",
    )

    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.28f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMedium),
        label = "WaveDragScale",
    )

    val infinite = rememberInfiniteTransition(label = "WaveBreath")
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "WavePhase",
    )
    val livePhase = if (isPlaying) wavePhase else 0f

    val songWave = rememberSongWaveform(trackKey, durationMs)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onScrub(frac)
                        onScrubFinished()
                    }
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onScrub(frac)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            if (durationMs > 0L) {
                                val sec = (frac * durationMs / 1000L).toLong()
                                if (sec != lastScrubSec) {
                                    lastScrubSec = sec
                                    haptics.play(Haptic.Tick)
                                }
                            }
                            onScrub(frac)
                        },
                        onDragEnd = {
                            isDragging = false
                            lastScrubSec = -1L
                            onScrubFinished()
                        },
                        onDragCancel = {
                            isDragging = false
                            lastScrubSec = -1L
                            onScrubFinished()
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h * 0.56f
                val maxAmp = h * 0.36f * dragScale
                val thumbX = (progress * w).coerceIn(0f, w)

                val steps = 96
                fun waveSample(i: Int): Float {
                    val base = if (i in songWave.indices) songWave[i] else 0.2f
                    val breath = if (isPlaying) 0.05f * sin(i * 0.25f + livePhase) else 0f
                    return (base + breath).coerceIn(0.05f, 1.0f)
                }

                fun waveLower(i: Int): Float {
                    val base = (if (i in songWave.indices) songWave[i] else 0.2f) * 0.45f
                    val breath = if (isPlaying) 0.03f * cos(i * 0.22f - livePhase) else 0f
                    return (base + breath).coerceIn(0.02f, 0.65f)
                }

                val upperPath = Path()
                val upperFillPath = Path()
                val lowerPath = Path()

                upperFillPath.moveTo(0f, midY)
                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    val x = t * w
                    val y = midY - waveSample(i) * maxAmp
                    if (i == 0) upperPath.moveTo(x, y) else upperPath.lineTo(x, y)
                    upperFillPath.lineTo(x, y)
                }
                upperFillPath.lineTo(w, midY)
                upperFillPath.close()

                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    val x = t * w
                    val y = midY + waveLower(i) * maxAmp
                    if (i == 0) lowerPath.moveTo(x, y) else lowerPath.lineTo(x, y)
                }

                // 1. Unplayed portion (right of thumbX)
                clipRect(left = thumbX, top = 0f, right = w, bottom = h) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.18f),
                        start = Offset(0f, midY),
                        end = Offset(w, midY),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawPath(
                        path = upperFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                            startY = midY - maxAmp,
                            endY = midY,
                        ),
                    )
                    drawPath(
                        path = upperPath,
                        color = Color.White.copy(alpha = 0.24f),
                        style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawPath(
                        path = lowerPath,
                        color = Color.White.copy(alpha = 0.16f),
                        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round),
                    )
                }

                // 2. Played portion (left of thumbX) — dynamic palette
                clipRect(left = 0f, top = 0f, right = thumbX, bottom = h) {
                    drawPath(
                        path = upperFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.58f),
                                secondaryAccent.copy(alpha = 0.22f),
                                Color.Transparent,
                            ),
                            startY = midY - maxAmp,
                            endY = midY + 4.dp.toPx(),
                        ),
                    )
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(accentColor, secondaryAccent),
                            startX = 0f,
                            endX = thumbX.coerceAtLeast(1f),
                        ),
                        start = Offset(0f, midY),
                        end = Offset(thumbX, midY),
                        strokeWidth = 2.6.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawPath(
                        path = upperPath,
                        color = accentColor.copy(alpha = 0.42f),
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawPath(
                        path = upperPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(accentColor, secondaryAccent),
                            startX = 0f,
                            endX = thumbX.coerceAtLeast(1f),
                        ),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    )
                }

                // 3. Glowing Playhead Indicator Orb
                val orbRadius = if (isDragging) 8.5.dp.toPx() else 6.8.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.65f), Color.Transparent),
                        center = Offset(thumbX, midY),
                        radius = orbRadius * 2.6f,
                    ),
                    center = Offset(thumbX, midY),
                    radius = orbRadius * 2.6f,
                )
                drawCircle(
                    color = accentColor,
                    center = Offset(thumbX, midY),
                    radius = orbRadius,
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.92f),
                    center = Offset(thumbX, midY),
                    radius = orbRadius,
                    style = Stroke(width = 1.8.dp.toPx()),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime((progress * durationMs).toLong()),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.65f),
            )
            Text(
                text = formatTime(durationMs.coerceAtLeast(0L)),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.65f),
            )
        }
    }
}

/**
 * Modern 5-button transport deck for WavePlay mode.
 * Central glowing orb synchronizes with the artwork palette, ignites active contour waves
 * only when playing, and becomes calmly static and subdued when paused.
 */
@Composable
fun ChordPlayTransportDeck(
    isPlaying: Boolean,
    isLoading: Boolean,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    shuffleEnabled: Boolean = false,
    repeatMode: Int = RepeatModes.OFF,
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    palette: MeshPalette = remember { MeshPalette(emptyList()) },
    hazeState: HazeState? = null,
    queueOpen: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    // Dynamic Artwork Colors
    val accentColor by animateColorAsState(
        targetValue = palette.colors.getOrElse(0) { Color(0xFFFF5722) },
        animationSpec = tween(700),
        label = "DeckAccent",
    )
    val secondaryAccent by animateColorAsState(
        targetValue = palette.colors.getOrElse(1) { Color(0xFFFF9E58) },
        animationSpec = tween(700),
        label = "DeckSecAccent",
    )

    // Outward energy shockwave burst on click
    val ripples = remember { mutableStateListOf<RippleWave>() }
    var rippleCounter by remember { mutableLongStateOf(0L) }
    val triggerRipple: () -> Unit = {
        if (ripples.size >= 5) ripples.removeAt(0)
        val wave = RippleWave(id = ++rippleCounter)
        ripples.add(wave)
        scope.launch {
            try {
                wave.anim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 620, easing = CubicBezierEasing(0.12f, 1f, 0.28f, 1f)),
                )
            } finally {
                ripples.remove(wave)
            }
        }
    }

    // Elastic tactile bounce on click
    val playInteraction = remember { MutableInteractionSource() }
    val isPlayPressed by playInteraction.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "PlayScale",
    )

    val playMorphProgress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "PlayMorphProgress",
    )

    val sideButtonsAlpha by animateFloatAsState(
        targetValue = if (queueOpen) 0f else 1f,
        animationSpec = tween(durationMillis = 280),
        label = "DeckSideAlpha",
    )
    val sideButtonsWidth by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (queueOpen) 0.dp else 44.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "DeckSideWidth",
    )
    val skipButtonSize by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (queueOpen) 64.dp else 52.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "DeckSkipButtonSize",
    )
    val skipIconSize by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (queueOpen) 40.dp else 30.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "DeckSkipIconSize",
    )

    val infinite = rememberInfiniteTransition(label = "OrbContours")
    val orbPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "OrbPhase",
    )
    val breathScale by infinite.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "OrbBreath",
    )

    // 4. Contour waves ONLY visible when playing, static and no waves when paused
    val ringsAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(500),
        label = "OrbRingsAlpha",
    )
    val liveBreathScale = if (isPlaying) breathScale else 1f
    val buttonDullAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.75f,
        animationSpec = tween(400),
        label = "ButtonDullAlpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. Shuffle
        Box(
            modifier = Modifier
                .width(sideButtonsWidth)
                .height(44.dp)
                .graphicsLayer {
                    alpha = sideButtonsAlpha
                    clip = true
                }
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !queueOpen,
                ) {
                    haptics.play(if (shuffleEnabled) Haptic.ToggleOff else Haptic.ToggleOn)
                    onToggleShuffle()
                },
            contentAlignment = Alignment.Center,
        ) {
            if (sideButtonsWidth > 4.dp) {
                Icon(
                    imageVector = BitChordIcons.Shuffle,
                    contentDescription = stringResource(if (shuffleEnabled) Res.string.shuffle_on else Res.string.shuffle_off),
                    tint = if (shuffleEnabled) accentColor else Color.White.copy(alpha = 0.58f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // 2. Previous Track
        Box(
            modifier = Modifier
                .size(skipButtonSize)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = previousEnabled,
                ) {
                    haptics.play(Haptic.SkipPrevious)
                    onPrevious()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_player_previous),
                contentDescription = stringResource(Res.string.widget_previous),
                tint = Color.White.copy(alpha = if (previousEnabled) 0.92f else 0.35f),
                modifier = Modifier.size(skipIconSize),
            )
        }

        // 3. Central Signature Synesthesia Glowing Play/Pause Orb
        Box(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    scaleX = playScale * liveBreathScale
                    scaleY = playScale * liveBreathScale
                    alpha = buttonDullAlpha
                    clip = false
                }
                .clickable(
                    interactionSource = playInteraction,
                    indication = null,
                ) {
                    triggerRipple()
                    haptics.play(if (isPlaying) Haptic.Pause else Haptic.Resume)
                    onPlayPause()
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { clip = false },
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val coreR = 34.dp.toPx()

                // Shockwave burst on click
                for (wave in ripples) {
                    val p = wave.anim.value
                    val r = coreR * (1f + p * 2.15f)
                    val a = (1f - p) * (1f - p) * 0.65f
                    drawCircle(
                        color = accentColor.copy(alpha = a * 0.30f),
                        radius = r * 0.9f,
                        center = Offset(cx, cy),
                    )
                    drawCircle(
                        color = secondaryAccent.copy(alpha = a),
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.4.dp.toPx() * (1f - p * 0.5f)),
                    )
                }

                // Ambient Radial Glow
                if (ringsAlpha > 0.01f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.46f * ringsAlpha),
                                secondaryAccent.copy(alpha = 0.16f * ringsAlpha),
                                Color.Transparent,
                            ),
                            center = Offset(cx, cy),
                            radius = coreR * 1.65f,
                        ),
                        center = Offset(cx, cy),
                        radius = coreR * 1.65f,
                    )

                    // 3 Seamless Undulating Contour Waves (harmonically continuous across loop boundaries)
                    data class ContourConfig(
                        val scaleBase: Float,
                        val harmonic1: Float,
                        val harmonic2: Float,
                        val phaseMult1: Float,
                        val phaseMult2: Float,
                        val amp1: Float,
                        val amp2: Float,
                        val ringColor: Color,
                    )
                    val contourConfigs = listOf(
                        ContourConfig(
                            scaleBase = 1.06f,
                            harmonic1 = 3f, harmonic2 = 5f,
                            phaseMult1 = 1f, phaseMult2 = -1f,
                            amp1 = 0.065f, amp2 = 0.040f,
                            ringColor = secondaryAccent.copy(alpha = 0.62f * ringsAlpha),
                        ),
                        ContourConfig(
                            scaleBase = 1.12f,
                            harmonic1 = 4f, harmonic2 = 2f,
                            phaseMult1 = -1f, phaseMult2 = 2f,
                            amp1 = 0.060f, amp2 = 0.045f,
                            ringColor = accentColor.copy(alpha = 0.48f * ringsAlpha),
                        ),
                        ContourConfig(
                            scaleBase = 1.03f,
                            harmonic1 = 2f, harmonic2 = 4f,
                            phaseMult1 = -1f, phaseMult2 = 1f,
                            amp1 = 0.055f, amp2 = 0.035f,
                            ringColor = Color.White.copy(alpha = 0.35f * ringsAlpha),
                        ),
                    )
                    val segs = 72
                    for (cfg in contourConfigs) {
                        val path = Path()
                        for (i in 0..segs) {
                            val theta = (i.toFloat() / segs) * (2f * PI).toFloat()
                            val wobble = cfg.amp1 * sin(cfg.harmonic1 * theta + orbPhase * cfg.phaseMult1) +
                                cfg.amp2 * cos(cfg.harmonic2 * theta + orbPhase * cfg.phaseMult2)
                            val r = coreR * (cfg.scaleBase + wobble)
                            val x = cx + r * cos(theta)
                            val y = cy + r * sin(theta)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(
                            path = path,
                            color = cfg.ringColor,
                            style = Stroke(width = 1.35.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }

                // Core Orb: Dynamic Gradient derived from artwork
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.85f),
                            accentColor,
                            secondaryAccent,
                        ),
                        center = Offset(cx, cy - coreR * 0.12f),
                        radius = coreR * 1.04f,
                    ),
                    center = Offset(cx, cy),
                    radius = coreR,
                )

                // Perimeter Rim Highlight
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.88f),
                            accentColor.copy(alpha = 0.55f),
                            secondaryAccent.copy(alpha = 0.90f),
                        ),
                        start = Offset(cx - coreR, cy - coreR),
                        end = Offset(cx + coreR, cy + coreR),
                    ),
                    center = Offset(cx, cy),
                    radius = coreR,
                    style = Stroke(width = 1.8.dp.toPx()),
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.8.dp,
                    modifier = Modifier.size(28.dp),
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_player_pause),
                        contentDescription = stringResource(Res.string.pause),
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer {
                                val s = 0.55f + 0.45f * playMorphProgress
                                scaleX = s
                                scaleY = s
                                rotationZ = (1f - playMorphProgress) * -70f
                                alpha = playMorphProgress
                            },
                    )
                    Icon(
                        painter = painterResource(Res.drawable.ic_player_play),
                        contentDescription = stringResource(Res.string.play),
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer {
                                val inv = 1f - playMorphProgress
                                val s = 0.55f + 0.45f * inv
                                scaleX = s
                                scaleY = s
                                rotationZ = playMorphProgress * 70f
                                alpha = inv
                            },
                    )
                }
            }
        }

        // 4. Next Track
        Box(
            modifier = Modifier
                .size(skipButtonSize)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = nextEnabled,
                ) {
                    haptics.play(Haptic.SkipNext)
                    onNext()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_player_next),
                contentDescription = stringResource(Res.string.widget_next),
                tint = Color.White.copy(alpha = if (nextEnabled) 0.92f else 0.35f),
                modifier = Modifier.size(skipIconSize),
            )
        }

        // 5. Repeat
        val repeatActive = repeatMode != RepeatModes.OFF
        Box(
            modifier = Modifier
                .width(sideButtonsWidth)
                .height(44.dp)
                .graphicsLayer {
                    alpha = sideButtonsAlpha
                    clip = true
                }
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !queueOpen,
                ) {
                    haptics.play(
                        when (repeatMode) {
                            RepeatModes.OFF -> Haptic.ToggleOn
                            RepeatModes.ONE -> Haptic.ToggleOff
                            else -> Haptic.Select
                        },
                    )
                    onCycleRepeat()
                },
            contentAlignment = Alignment.Center,
        ) {
            if (sideButtonsWidth > 4.dp) {
                if (repeatMode == RepeatModes.ONE) {
                    Text(
                        text = "1",
                        color = accentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    Icon(
                        imageVector = BitChordIcons.Repeat,
                        contentDescription = stringResource(Res.string.repeat_off),
                        tint = if (repeatActive) accentColor else Color.White.copy(alpha = 0.58f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

/**
 * Compact, proportional Audio Pipeline Bottom Pill (height 40.dp).
 * Perfectly sized alongside the Lyrics Quote and Queue icons.
 */
@Composable
fun ChordPlayPipelineBottomPill(
    song: Song,
    isLoading: Boolean,
    onOpenPipeline: () -> Unit,
    onOpenOutput: () -> Unit,
    palette: MeshPalette = remember { MeshPalette(emptyList()) },
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val videoQuality by PlayerSettings.videoQuality.collectAsStateWithLifecycle()
    val nerdStats by NerdStats.current.collectAsStateWithLifecycle()
    val racingLossless by NerdStats.racingLossless.collectAsStateWithLifecycle()
    val stillRacing = song.videoId in racingLossless

    val accentColor by animateColorAsState(
        targetValue = palette.colors.getOrElse(0) { Color(0xFFFF5722) },
        animationSpec = tween(700),
        label = "PillAccent",
    )

    val stats = nerdStats
    val hasMeasuredAudio = stats != null && (stats.mimeType != null || stats.bitrateKbps != null || stats.sampleRateHz != null)

    val primaryLabel = when {
        song.isVideo -> "VIDEO • ${videoQuality.badge}"
        isLoading || stillRacing || !hasMeasuredAudio -> "PROCESSING..."
        stats?.isHiRes == true -> "HD LOSSLESS"
        stats?.isLossless == true -> "LOSSLESS ${NerdStats.codecLabel(stats.mimeType) ?: "FLAC"}"
        stats?.isDolbyAtmos == true -> "DOLBY ATMOS"
        else -> {
            val codec = NerdStats.codecLabel(stats?.mimeType)?.uppercase() ?: "AUDIO"
            val kbps = stats?.bitrateKbps?.let { " • ${it}K" }.orEmpty()
            "$codec$kbps"
        }
    }

    val sampleRate = stats?.sampleRateHz
    val secondaryLabel = when {
        song.isVideo -> "VIDEO STREAM"
        isLoading || stillRacing || !hasMeasuredAudio -> "AUDIO PIPELINE"
        stats?.isLossless == true -> {
            val depth = stats.bitDepth ?: if (stats.isHiRes) 24 else 16
            val khz = ((sampleRate ?: 44100) / 1000f).let {
                if (it % 1f == 0f) it.toInt().toString() else "%.1f".format(it)
            }
            "${depth}-BIT / $khz kHz"
        }
        sampleRate != null -> {
            val khz = (sampleRate / 1000f).let {
                if (it % 1f == 0f) it.toInt().toString() else "%.1f".format(it)
            }
            "$khz kHz • ${stats?.sourceName?.uppercase() ?: "AUDIO PIPELINE"}"
        }
        else -> "AUDIO PIPELINE"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.28f))
            .background(accentColor.copy(alpha = 0.12f))
            .background(Color.White.copy(alpha = 0.08f))
            .border(
                width = 0.8.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.16f),
                    ),
                ),
                shape = CircleShape,
            )
            .padding(start = 14.dp, end = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    haptics.play(Haptic.Tap)
                    onOpenPipeline()
                }
                .padding(end = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = primaryLabel,
                    color = Color.White.copy(alpha = 0.96f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                )
                Text(
                    text = secondaryLabel,
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    lineHeight = 11.sp,
                    maxLines = 1,
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    haptics.play(Haptic.Tap)
                    onOpenOutput()
                },
        ) {
            Icon(
                imageVector = Icons.Rounded.Headphones,
                contentDescription = stringResource(Res.string.audio_output),
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
