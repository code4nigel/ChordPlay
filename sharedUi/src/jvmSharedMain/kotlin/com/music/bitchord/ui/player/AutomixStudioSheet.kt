package com.music.bitchord.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.smart.AutomixEdit
import com.music.bitchord.playback.smart.AutomixStyle
import com.music.bitchord.playback.smart.TransitionPlan
import com.music.bitchord.playback.smart.TransitionStyle
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.utils.containSheetGestures
import dev.chrisbanes.haze.HazeState
import com.music.bitchord.sharedui.resources.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

internal val LocalOpenAutomixStudio = staticCompositionLocalOf<(() -> Unit)?> { null }

@Composable
internal fun AutomixQueueLink(controlsLocked: Boolean) {
    val studio = PlayerPlatform.host.automixStudio ?: return
    val open = LocalOpenAutomixStudio.current ?: return
    if (controlsLocked) return
    val available by remember(studio) { studio.state.map { it != null }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(studio.state.value != null)
    if (!available) return
    val enabled by PlayerSettings.smartFadeEnabled.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    TextButton(
        onClick = {
            haptics.play(Haptic.Select)
            if (enabled) open() else PlayerSettings.setSmartFadeEnabled(true)
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Text(stringResource(if (enabled) Res.string.studio_open else Res.string.studio_enable))
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun AutomixStudioSheet(
    haze: HazeState?,
    accent: Color?,
    onClose: () -> Unit,
) {
    val studio = PlayerPlatform.host.automixStudio ?: return
    val state by studio.state.collectAsStateWithLifecycle()
    val enabled by PlayerSettings.smartFadeEnabled.collectAsStateWithLifecycle()
    val pair = remember { state?.pair }
    LaunchedEffect(state?.pair, enabled) { if (pair == null || pair != state?.pair || !enabled) onClose() }
    if (pair == null) return
    DisposableEffect(studio, pair) { onDispose { studio.close(pair) } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP, onEvent = onClose)
    val current = state?.takeIf { it.pair == pair } ?: return
    val reduceAnimation by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()
    val reduceBlur by PlayerSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val animationSpeed by PlayerSettings.playerAnimationSpeed.collectAsStateWithLifecycle()
    val enter = remember { Animatable(0f) }
    LaunchedEffect(reduceAnimation) {
        if (reduceAnimation) enter.snapTo(1f) else enter.animateTo(1f, spring(stiffness = 380f * animationSpeed))
    }
    val haptics = rememberHaptics()
    var advanced by remember { mutableStateOf(false) }
    var applyFailed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf<AutomixEdit?>(null) }
    val edit = dragging ?: current.edit
    val plan = current.audition.plan ?: current.plan
    val previewing = current.audition.phase == AutomixAuditionPhase.BUFFERING ||
        current.audition.phase == AutomixAuditionPhase.PLAYING
    val editable = !current.busy && !previewing
    val cut = current.edit.style == AutomixStyle.OFF
    val shape = RoundedCornerShape(30.dp)
    val highlight = accent ?: Color(0xFFC4B8FF)
    val timelineAccent = if (highlight.luminance() < 0.35f) lerp(highlight, Color.White, 0.50f) else highlight
    fun change(value: AutomixEdit) {
        applyFailed = false
        studio.edit(pair, value)
    }

    PlayerBackHandler(enabled = true, onBack = onClose)
    Box(
        modifier = Modifier.fillMaxSize().containSheetGestures().background(Color.Black.copy(alpha = 0.5f * enter.value))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().navigationBarsPadding().padding(12.dp)
                .heightIn(max = 660.dp)
                .graphicsLayer { alpha = enter.value; translationY = 24.dp.toPx() * (1 - enter.value) }
                .then(playerModalSurfaceModifier(shape, haze, highlight, reduceBlur))
                .clickable(indication = null, interactionSource = null, onClick = {})
                .verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.studio_title), Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onClose) { Text(stringResource(Res.string.studio_close)) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StudioArtwork(current.outgoing, Modifier.weight(1f))
                StudioArtwork(current.incoming, Modifier.weight(1f))
            }
            StudioTimeline(current, plan, timelineAccent, reduceAnimation)

            val duration = (plan.fadeSeconds * 10).roundToInt() / 10.0
            val speed = ((maxOf(plan.incomingPlaybackRate, plan.outgoingPlaybackRate) - 1) * 1000).roundToInt() / 10.0
            val description = studioDescription(plan)
            val length = if (plan.blocked) "" else " · $duration s"
            val tempo = if (speed > 0) " · " + stringResource(Res.string.studio_tempo_amount, speed.toString()) else ""
            Text(description + length + tempo, color = Color.White, style = MaterialTheme.typography.titleMedium)
            StudioExplanation(current, plan)

            if (previewing) {
                OutlinedButton(onClick = { studio.stopPreview(pair) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(Res.string.studio_stop))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ studio.preview(pair, compare = true) }, Modifier.weight(1f).heightIn(min = 48.dp), enabled = editable) {
                        Text(stringResource(Res.string.studio_compare))
                    }
                    Button({ studio.preview(pair, compare = false) }, Modifier.weight(1f).heightIn(min = 48.dp), enabled = editable) {
                        Text(stringResource(Res.string.studio_preview))
                    }
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AutomixStyle.entries.forEach { style ->
                    FilterChip(
                        selected = edit.style == style,
                        enabled = editable,
                        onClick = { haptics.play(Haptic.Select); change(edit.copy(style = style)) },
                        label = { Text(studioStyle(style)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            if (!cut) {
                val overlap = (edit.overlapSeconds ?: current.plan.fadeSeconds).coerceIn(1.0, 12.0)
                StudioSlider(stringResource(Res.string.studio_overlap, overlap.roundToInt().toString()), overlap, 12f, editable,
                    min = 1f, steps = 10,
                    onChange = { dragging = edit.copy(overlapSeconds = it) },
                    onChangeFinished = { dragging?.let(::change); dragging = null })
                TextButton({ advanced = !advanced }, Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(Res.string.studio_timing))
                }
                if (advanced) {
                    val outCue = edit.outgoingCue ?: current.plan.transitionStart
                    val inCue = edit.incomingCue ?: current.plan.incomingCueTime
                    StudioSlider(stringResource(Res.string.studio_outgoing, studioTimestamp(outCue)), outCue,
                        (current.outgoingDuration - 1).toFloat(), editable,
                        onChange = { dragging = edit.copy(outgoingCue = it) },
                        onChangeFinished = { dragging?.let(::change); dragging = null })
                    StudioSlider(stringResource(Res.string.studio_incoming, studioTimestamp(inCue)), inCue,
                        (current.incomingDuration - 3).toFloat(), editable && current.incomingDuration > 3,
                        onChange = { dragging = edit.copy(incomingCue = it) },
                        onChangeFinished = { dragging?.let(::change); dragging = null })
                    Text(stringResource(Res.string.studio_tempo), color = Color.White)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 3, 6, 10).forEach { cap ->
                            FilterChip(edit.tempoLimitPercent == cap, { change(edit.copy(tempoLimitPercent = cap)) },
                                label = { Text(stringResource(Res.string.studio_percent, cap.toString())) },
                                enabled = editable, modifier = Modifier.heightIn(min = 48.dp))
                        }
                    }
                }
            }

            if (applyFailed) Text(stringResource(Res.string.studio_apply_unavailable), color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton({ change(AutomixEdit()) }, Modifier.heightIn(min = 48.dp), enabled = editable) {
                    Text(stringResource(Res.string.studio_reset))
                }
                Button(
                    onClick = { if (studio.apply(pair)) onClose() else applyFailed = true },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    enabled = editable,
                ) { Text(stringResource(Res.string.studio_apply)) }
            }
        }
    }
}

@Composable
private fun StudioArtwork(song: Song, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AsyncImage(song.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f)))
        Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White, style = MaterialTheme.typography.titleSmall)
        Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StudioTimeline(state: AutomixStudioState, plan: TransitionPlan, accent: Color, reduceAnimation: Boolean) {
    val outgoingTime = studioTimestamp(plan.transitionStart)
    val incomingTime = studioTimestamp(plan.incomingCueTime)
    val fadeStart = stringResource(Res.string.studio_outgoing, outgoingTime)
    val incomingStart = stringResource(Res.string.studio_incoming, incomingTime)
    val playing = state.audition.phase == AutomixAuditionPhase.PLAYING
    Box(Modifier.fillMaxWidth().height(66.dp).semantics(mergeDescendants = true) {
        contentDescription = "$fadeStart. $incomingStart"
    }) {
        StudioTimelineLanes(state, plan, accent, playing && !reduceAnimation, Modifier.matchParentSize())
        Text(outgoingTime, Modifier.align(Alignment.TopEnd).padding(top = 9.dp, end = 10.dp), color = Color.White,
            style = MaterialTheme.typography.labelSmall)
        Text(incomingTime, Modifier.align(Alignment.BottomStart).padding(bottom = 7.dp, start = 10.dp), color = Color.White,
            style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StudioTimelineLanes(state: AutomixStudioState, plan: TransitionPlan, accent: Color, showPlayhead: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val pad = 10.dp.toPx()
        val width = size.width - pad * 2
        val split = 0.40f
        val end = if (plan.blocked) split else 0.79f
        val top = size.height * 0.25f
        val bottom = size.height * 0.70f
        val bar = 5.dp.toPx()
        fun x(fraction: Float) = pad + width * fraction
        drawRoundRect(Color.White.copy(alpha = 0.08f), cornerRadius = CornerRadius(14.dp.toPx()))
        drawRect(Brush.horizontalGradient(listOf(Color.White, accent.copy(alpha = 0.25f)), x(0f), x(end)), Offset(x(0f), top), Size(width * end, bar))
        drawRect(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.45f), accent), x(split), x(1f)), Offset(x(split), bottom), Size(width * (1 - split), bar))
        if (!plan.blocked && plan.fadeSeconds > 0) {
            state.outgoingBeats.filter { it in plan.transitionStart..plan.transitionEnd }.forEach {
                val tick = x(split + ((it - plan.transitionStart) / plan.fadeSeconds).toFloat() * (end - split))
                drawLine(Color.White, Offset(tick, top - bar), Offset(tick, top + bar), 1.dp.toPx())
            }
            val incomingSpan = plan.fadeSeconds * plan.incomingPlaybackRate / plan.outgoingPlaybackRate
            state.incomingBeats.filter { it in plan.incomingCueTime..(plan.incomingCueTime + incomingSpan) }.forEach {
                val tick = x(split + ((it - plan.incomingCueTime) / incomingSpan).toFloat() * (end - split))
                drawLine(Color.White, Offset(tick, bottom), Offset(tick, bottom + bar * 2), 1.dp.toPx())
            }
        }
        drawLine(Color.White.copy(alpha = 0.55f), Offset(x(split), top), Offset(x(split), bottom + bar), 1.dp.toPx())
        if (showPlayhead) {
            val playhead = x(state.audition.position)
            drawLine(Color.White, Offset(playhead, 6.dp.toPx()), Offset(playhead, size.height - 6.dp.toPx()), 2.dp.toPx())
        }
    }
}

@Composable
private fun StudioSlider(label: String, value: Double, max: Float, enabled: Boolean, min: Float = 0f, steps: Int = 0,
    onChange: (Double) -> Unit, onChangeFinished: () -> Unit) {
    val range = min..max.coerceAtLeast(min + 0.01f)
    Column {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
        Slider(value.toFloat().coerceIn(range.start, range.endInclusive), { onChange(it.toDouble()) },
            modifier = Modifier.semantics { contentDescription = label }, valueRange = range, steps = steps,
            enabled = enabled, onValueChangeFinished = onChangeFinished)
    }
}

@Composable
private fun studioStyle(style: AutomixStyle): String = stringResource(when (style) {
    AutomixStyle.AUTO -> Res.string.studio_auto
    AutomixStyle.GENTLE -> Res.string.studio_gentle
    AutomixStyle.DJ -> Res.string.studio_dj
    AutomixStyle.OFF -> Res.string.studio_off
})

@Composable
private fun studioDescription(plan: TransitionPlan): String = stringResource(if (plan.blocked) Res.string.studio_cut else when (plan.transitionStyle) {
    TransitionStyle.EQUAL_POWER -> Res.string.studio_plain_fade
    TransitionStyle.DJ_BLEND -> Res.string.studio_beat_blend
    TransitionStyle.DJ_FILTER -> Res.string.studio_filter
    TransitionStyle.GAPLESS -> Res.string.studio_gapless
})

@Composable
private fun StudioExplanation(state: AutomixStudioState, plan: TransitionPlan) {
    val explanation = when {
        state.busy -> Res.string.studio_armed
        state.audition.phase == AutomixAuditionPhase.BUFFERING -> Res.string.studio_loading
        state.audition.phase == AutomixAuditionPhase.UNAVAILABLE -> Res.string.studio_unavailable
        plan.reason == "studio-no-room" -> Res.string.studio_invalid
        plan.reason == "studio-tempo-limit" -> Res.string.studio_tempo_limited
        plan.reason.contains("analysis-fallback") -> Res.string.studio_analysis
        plan.policyReasons.isNotEmpty() && plan.transitionStyle == TransitionStyle.EQUAL_POWER -> Res.string.studio_confidence
        else -> null
    }
    explanation?.let { Text(stringResource(it), color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall) }
}

private fun studioTimestamp(seconds: Double): String {
    val whole = seconds.coerceAtLeast(0.0).toInt()
    return "${whole / 60}:${(whole % 60).toString().padStart(2, '0')}"
}
