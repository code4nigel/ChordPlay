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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
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
import com.music.bitchord.playback.smart.TrackAnalysis
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.utils.containSheetGestures
import dev.chrisbanes.haze.HazeState
import com.music.bitchord.sharedui.resources.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import kotlin.math.floor
import kotlin.math.ceil

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
    var initialRequestStarted by remember(pair) { mutableStateOf(false) }
    DisposableEffect(studio, pair) {
        studio.open(pair)
        initialRequestStarted = true
        onDispose { studio.close(pair) }
    }
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
    val plan = current.audition.plan ?: dragging?.let(current::planFor) ?: current.plan
    val tempoMatching by (PlayerSettings.automixTempoMatching ?: return).collectAsStateWithLifecycle()
    val previewing = current.audition.phase == AutomixAuditionPhase.BUFFERING ||
        current.audition.phase == AutomixAuditionPhase.PLAYING
    val canPreview = !current.busy && !previewing
    val waitingForWaveforms = !initialRequestStarted || current.waveformLoading
    val editable = canPreview && !waitingForWaveforms
    val editWaitMessage = stringResource(Res.string.studio_wait_for_waveforms)
    var triedLockedEdit by remember(pair, waitingForWaveforms) { mutableStateOf(false) }
    LaunchedEffect(waitingForWaveforms) {
        if (waitingForWaveforms) dragging = null
    }
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
                StudioArtwork(current.outgoing, current.outgoingAnalysis, plan.outgoingPlaybackRate, Modifier.weight(1f))
                StudioArtwork(current.incoming, current.incomingAnalysis, plan.incomingPlaybackRate, Modifier.weight(1f))
            }
            StudioTimeline(current, plan, timelineAccent, reduceAnimation)

            val duration = (plan.fadeSeconds * 10).roundToInt() / 10.0
            val description = studioDescription(plan)
            val length = if (plan.blocked) "" else " · $duration s"
            Text(description + length, color = Color.White, style = MaterialTheme.typography.titleMedium)
            StudioExplanation(current, plan)

            if (previewing) {
                OutlinedButton(onClick = { studio.stopPreview(pair) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(Res.string.studio_stop))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ studio.preview(pair, compare = true) }, Modifier.weight(1f).heightIn(min = 48.dp), enabled = canPreview) {
                        Text(stringResource(Res.string.studio_compare))
                    }
                    Button({ studio.preview(pair, compare = false) }, Modifier.weight(1f).heightIn(min = 48.dp), enabled = canPreview) {
                        Text(stringResource(Res.string.studio_preview))
                    }
                }
            }

            if (waitingForWaveforms) {
                Text(
                    editWaitMessage,
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = if (triedLockedEdit) timelineAccent else Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(
                Modifier.then(
                    if (waitingForWaveforms) Modifier.clickable(onClickLabel = editWaitMessage, role = Role.Button) {
                        triedLockedEdit = true
                        haptics.play(Haptic.Tap)
                    } else Modifier,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(AutomixStyle.AUTO, AutomixStyle.GENTLE, AutomixStyle.OFF).forEach { style ->
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
                    val cueAction = stringResource(if (advanced) Res.string.studio_collapse_cues else Res.string.studio_expand_cues)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable(enabled = !waitingForWaveforms, onClickLabel = cueAction, role = Role.Button) { advanced = !advanced }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(Res.string.studio_timing), color = Color.White,
                                style = MaterialTheme.typography.titleSmall)
                            val out = stringResource(Res.string.studio_outgoing, studioTimestamp(plan.transitionStart))
                            val incoming = stringResource(Res.string.studio_incoming, studioTimestamp(plan.incomingCueTime))
                            Text("$out  •  $incoming", color = Color.White.copy(alpha = 0.78f),
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(if (advanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = Color.White)
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
                        if (tempoMatching) {
                            Text(stringResource(Res.string.studio_tempo), color = Color.White)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(0, 3, 6, 10).forEach { cap ->
                                    FilterChip(edit.tempoLimitPercent == cap, { change(edit.copy(tempoLimitPercent = cap)) },
                                        label = { Text(stringResource(Res.string.studio_percent, cap.toString())) },
                                        enabled = editable, modifier = Modifier.heightIn(min = 48.dp))
                                }
                            }
                        } else {
                            Text(stringResource(Res.string.automix_original_speed), color = Color.White)
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
}

@Composable
private fun StudioArtwork(song: Song, analysis: TrackAnalysis, rate: Double, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AsyncImage(song.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f)))
        Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White, style = MaterialTheme.typography.titleSmall)
        Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
        if (analysis.beatConfidence >= 0.5 && analysis.bpm > 0.0) {
            Text(stringResource(Res.string.studio_bpm, analysis.bpm.roundToInt().toString()), color = Color.White,
                style = MaterialTheme.typography.labelMedium)
        }
        if (rate > 1.0) {
            val change = ((rate - 1) * 1000).roundToInt() / 10.0
            Text(stringResource(Res.string.studio_speed_track, song.title, change.toString()), color = Color.White,
                style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun StudioTimeline(state: AutomixStudioState, plan: TransitionPlan, accent: Color, reduceAnimation: Boolean) {
    val outgoingCue = if (plan.blocked) state.outgoingDuration else plan.transitionStart
    val incomingCue = if (plan.blocked) 0.0 else plan.incomingCueTime
    val outgoingTime = studioTimestamp(outgoingCue)
    val incomingTime = studioTimestamp(incomingCue)
    val fadeStart = stringResource(Res.string.studio_outgoing, outgoingTime)
    val incomingStart = stringResource(Res.string.studio_incoming, incomingTime)
    val before = minOf(2.0, outgoingCue) / plan.outgoingPlaybackRate
    val overlap = if (plan.blocked) 0.0 else plan.fadeSeconds / plan.outgoingPlaybackRate
    val after = 2.0 / plan.incomingPlaybackRate
    val missing = stringResource(Res.string.studio_waveform_unavailable)
    fun hasWaveform(analysis: TrackAnalysis, cue: Double, rate: Double): Boolean =
        analysis.waveform?.let { it.end > cue - before * rate && it.start < cue + (overlap + after) * rate } == true
    val loading = stringResource(Res.string.studio_waveform_loading)
    val outgoingLabel = when {
        hasWaveform(state.outgoingAnalysis, outgoingCue, plan.outgoingPlaybackRate) -> fadeStart
        state.outgoingWaveform == AutomixWaveformReadiness.LOADING -> "$fadeStart · $loading"
        else -> "$fadeStart · $missing"
    }
    val incomingLabel = when {
        hasWaveform(state.incomingAnalysis, incomingCue, plan.incomingPlaybackRate) -> incomingStart
        state.incomingWaveform == AutomixWaveformReadiness.LOADING -> "$incomingStart · $loading"
        else -> "$incomingStart · $missing"
    }
    val playhead = state.audition.secondsFromCue.takeIf {
        state.audition.phase == AutomixAuditionPhase.PLAYING && !reduceAnimation
    }
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
        contentDescription = "$outgoingLabel. $incomingLabel"
    }) {
        Text(outgoingLabel, Modifier.padding(start = 4.dp, bottom = 4.dp), color = Color.White,
            style = MaterialTheme.typography.labelSmall)
        Canvas(Modifier.fillMaxWidth().height(90.dp)) {
            val padding = 9.dp.toPx()
            val width = (size.width - 2 * padding).coerceAtLeast(1f)
            val window = before + overlap + after
            val top = size.height * 0.25f
            val bottom = size.height * 0.75f
            fun x(seconds: Double) = padding + width * ((seconds + before) / window).toFloat()
            drawRoundRect(Color.White.copy(alpha = 0.08f), cornerRadius = CornerRadius(14.dp.toPx()))
            if (overlap > 0) drawRect(accent.copy(alpha = 0.17f), Offset(x(0.0), 0f), Size(x(overlap) - x(0.0), size.height))
            fun lane(analysis: TrackAnalysis, cue: Double, rate: Double, center: Float, color: Color) {
                drawLine(color.copy(alpha = 0.25f), Offset(padding, center), Offset(padding + width, center), 1.dp.toPx())
                val waveform = analysis.waveform
                if (waveform != null) {
                    val first = floor((cue - before * rate - waveform.start) / waveform.sampleSeconds).toInt().coerceAtLeast(0)
                    val last = ceil((cue + (overlap + after) * rate - waveform.start) / waveform.sampleSeconds)
                        .toInt().coerceAtMost(waveform.peaks.lastIndex)
                    for (index in first..last) {
                        val time = (waveform.start + (index + 0.5) * waveform.sampleSeconds - cue) / rate
                        if (time < -before || time > overlap + after) continue
                        val px = x(time)
                        val peak = 0.5.dp.toPx() + size.height * 0.20f * (waveform.peaks[index].toInt() / 127f)
                        drawLine(color, Offset(px, center - peak), Offset(px, center + peak), 1.dp.toPx())
                    }
                }
                if (analysis.beatConfidence >= 0.5) {
                    for (beat in analysis.downbeats) {
                        val time = (beat - cue) / rate
                        if (time < -before || time > overlap + after) continue
                        val px = x(time)
                        drawLine(color.copy(alpha = 0.65f), Offset(px, center + 13.dp.toPx()),
                            Offset(px, center + 18.dp.toPx()), 1.dp.toPx())
                    }
                }
            }
            lane(state.outgoingAnalysis, outgoingCue, plan.outgoingPlaybackRate, top, Color.White.copy(alpha = 0.86f))
            lane(state.incomingAnalysis, incomingCue, plan.incomingPlaybackRate, bottom, accent)
            drawLine(Color.White.copy(alpha = 0.65f), Offset(x(0.0), 0f), Offset(x(0.0), size.height), 1.dp.toPx())
            if (playhead != null) {
                val px = x(playhead.coerceIn(-before, overlap + after))
                drawLine(Color.White, Offset(px, 3.dp.toPx()), Offset(px, size.height - 3.dp.toPx()), 2.dp.toPx())
            }
        }
        Text(incomingLabel, Modifier.padding(start = 4.dp, top = 4.dp), color = Color.White,
            style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StudioSlider(label: String, value: Double, max: Float, enabled: Boolean, min: Float = 0f, steps: Int = 0,
    onChange: (Double) -> Unit, onChangeFinished: () -> Unit) {
    val range = min..max.coerceAtLeast(min + 0.01f)
    val current = value.toFloat().coerceIn(range)
    val colors = SliderDefaults.colors()
    Column(
        Modifier.fillMaxWidth().relativeSlider(
            label, current, range, steps, enabled,
            onValueChange = { onChange(it.toDouble()) },
            onFinished = onChangeFinished,
        ),
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
        Slider(
            value = current,
            onValueChange = {},
            modifier = Modifier.clearAndSetSemantics {},
            valueRange = range,
            steps = steps,
            enabled = false,
            colors = if (enabled) SliderDefaults.colors(
                disabledThumbColor = colors.thumbColor,
                disabledActiveTrackColor = colors.activeTrackColor,
                disabledActiveTickColor = colors.activeTickColor,
                disabledInactiveTrackColor = colors.inactiveTrackColor,
                disabledInactiveTickColor = colors.inactiveTickColor,
            ) else colors,
        )
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
