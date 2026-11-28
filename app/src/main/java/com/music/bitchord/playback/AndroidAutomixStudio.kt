/*
 * Copyright (C) 2026 Shivanshu (code4nigel) <https://github.com/code4nigel/ChordPlay>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.music.bitchord.playback

import com.music.bitchord.ui.player.AutomixWaveformReadiness

import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.smart.AutomixEdit
import com.music.bitchord.playback.smart.TrackAnalysis
import com.music.bitchord.playback.smart.TransitionPlan
import com.music.bitchord.playback.smart.withAutomixEdit
import com.music.bitchord.ui.player.AutomixAudition
import com.music.bitchord.ui.player.AutomixAuditionPhase
import com.music.bitchord.ui.player.AutomixStudio
import com.music.bitchord.ui.player.AutomixStudioState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.math.roundToLong

internal data class AutomixPair(
    val identity: Pair<Any, Any>,
    val outgoing: MediaItem,
    val incoming: MediaItem,
    val outgoingDurationMs: Long,
    val incomingDurationMs: Long,
    val outgoingAnalysis: TrackAnalysis,
    val incomingAnalysis: TrackAnalysis,
    val automatic: TransitionPlan,
) {
    fun plan(edit: AutomixEdit): TransitionPlan = automatic.withAutomixEdit(
        edit, outgoingDurationMs / 1000.0, incomingDurationMs / 1000.0, outgoingAnalysis, incomingAnalysis,
    )
}

/** The player UI only sees the currently attached playback service. Main-thread confined. */
internal object AndroidAutomixStudio : AutomixStudio {
    override val state = MutableStateFlow<AutomixStudioState?>(null)
    var session: AutomixStudioSession? = null
    fun profileChanged(profile: QueueMixProfile?) { session?.profileChanged(profile) }
    override fun open(pair: Long) { session?.open(pair) }
    override fun edit(pair: Long, edit: AutomixEdit) { session?.edit(pair, edit) }
    override fun apply(pair: Long): Boolean = session?.apply(pair) == true
    override fun preview(pair: Long, compare: Boolean) { session?.preview(pair, compare) }
    override fun stopPreview(pair: Long) { session?.stopPreview(pair) }
    override fun close(pair: Long) { session?.close(pair) }
}

@UnstableApi
internal class AutomixStudioSession(
    private val scope: CoroutineScope,
    private val player: () -> ExoPlayer?,
    private val liveRenderer: () -> CrossfadeController?,
    private val eligible: () -> Boolean,
    private val output: () -> Any?,
    private val buildDeck: (TransitionFilterProcessor, LoudnessProcessor) -> ExoPlayer,
    private val audioAttributes: AudioAttributes,
    private val requestWaveform: (MediaItem, Long) -> Unit,
    private val waveformPending: (MediaItem) -> Boolean,
) {
    private var key: Pair<Any, Any>? = null
    private var pairNumber = 0L
    private var draft = AutomixEdit()
    private var appliedPlan: TransitionPlan? = null
    private var appliedSources: Pair<MediaItem, MediaItem>? = null
    private var appliedTempoMatching = false
    private var replaying: SavedQueueMix? = null
    private data class EditedJoin(val outgoingId: String, val incomingId: String, val edit: AutomixEdit)
    private val appliedEdits = mutableMapOf<Pair<Any, Any>, EditedJoin>()
    private var audition = AutomixAudition()
    private var previewJob: Job? = null
    private var resumeAfterPreview = true

    init {
        AndroidAutomixStudio.session = this
        scope.launch {
            while (isActive) {
                refresh()
                delay(250)
            }
        }
    }

    private fun currentPair(): AutomixPair? = if (eligible()) liveRenderer()?.studioPair() else null

    private fun appliedEdit(pair: AutomixPair): AutomixEdit = appliedEdits[pair.identity]?.takeIf {
        it.outgoingId == pair.outgoing.mediaId && it.incomingId == pair.incoming.mediaId
    }?.edit
        ?: replaying?.editFor(pair.outgoing.toSong(), pair.incoming.toSong())
        ?: AutomixEdit()

    fun queueEdits(songs: List<Song>): Map<Int, AutomixEdit> = songs.zipWithNext().mapIndexedNotNull { index, (from, to) ->
        val id = from.queueEntryId
        val next = to.queueEntryId
        val edit = (if (id != null && next != null) appliedEdits[id to next] else null)?.takeIf {
            it.outgoingId == from.videoId && it.incomingId == to.videoId
        }?.edit
            ?: replaying?.editFor(from, to)
        if (edit == null || edit == AutomixEdit()) null else index to edit
    }.toMap()

    private fun setPair(pair: AutomixPair?) {
        if (key == pair?.identity) return
        key = pair?.identity
        pairNumber++
        draft = pair?.let(::appliedEdit) ?: AutomixEdit()
        appliedPlan = null
        appliedSources = null
        cancelPreview(resume = false)
        audition = AutomixAudition()
    }

    fun livePlan(pair: AutomixPair): TransitionPlan {
        setPair(pair.takeIf { eligible() && AppSettings.activeSmartFadeEnabled.value })
        if (appliedTempoMatching == AppSettings.activeAutomixTempoMatching.value &&
            appliedSources == (pair.outgoing to pair.incoming)) appliedPlan?.let { return it }
        val edit = appliedEdit(pair)
        val candidate = pair.plan(edit)
        return if (candidate.blocked && candidate.reason != "studio-off" ||
            edit.outgoingCue != null && candidate.transitionEnd * 1000 <= (player()?.currentPosition ?: 0L)
        ) pair.automatic else candidate
    }

    private fun refresh() {
        if (AndroidAutomixStudio.session !== this) return
        if (AndroidAutomixStudio.state.subscriptionCount.value == 0 && key == null) return
        val pair = currentPair()
        setPair(pair.takeIf { AppSettings.activeSmartFadeEnabled.value })
        if (pair != null && AndroidAutomixStudio.state.subscriptionCount.value > 0) {
            if (pair.outgoingAnalysis.waveform == null) requestWaveform(pair.outgoing, pair.outgoingDurationMs)
            if (pair.incomingAnalysis.waveform == null) requestWaveform(pair.incoming, pair.incomingDurationMs)
        }
        AndroidAutomixStudio.state.value = pair?.let {
            AutomixStudioState(
                pair = pairNumber,
                outgoing = it.outgoing.toSong(), incoming = it.incoming.toSong(),
                outgoingDuration = it.outgoingDurationMs / 1000.0,
                incomingDuration = it.incomingDurationMs / 1000.0,
                plan = it.plan(draft), automaticPlan = it.automatic, edit = draft,
                outgoingAnalysis = it.outgoingAnalysis, incomingAnalysis = it.incomingAnalysis,
                outgoingWaveform = waveformReadiness(it, outgoing = true),
                incomingWaveform = waveformReadiness(it, outgoing = false),
                busy = liveRenderer()?.studioBusy != false,
                audition = audition,
            )
        }
    }

    private fun editable(pair: Long): AutomixPair? = currentPair()?.takeIf {
        pair == pairNumber && it.identity == key && AppSettings.activeSmartFadeEnabled.value &&
            liveRenderer()?.studioBusy == false
    }

    private fun waveformReadiness(pair: AutomixPair, outgoing: Boolean): AutomixWaveformReadiness {
        val plan = pair.automatic
        val peaks = if (outgoing) pair.outgoingAnalysis.waveform else pair.incomingAnalysis.waveform
        val cue = if (outgoing) {
            if (plan.blocked) pair.outgoingDurationMs / 1000.0 else plan.transitionStart
        } else if (plan.blocked) 0.0 else plan.incomingCueTime
        val rate = if (outgoing) plan.outgoingPlaybackRate else plan.incomingPlaybackRate
        val overlap = if (plan.blocked) 0.0 else plan.fadeSeconds / plan.outgoingPlaybackRate
        val context = minOf(2.0, if (plan.blocked) pair.outgoingDurationMs / 1000.0 else plan.transitionStart)
        val start = cue - context / plan.outgoingPlaybackRate * rate
        val end = cue + (overlap + 2.0 / plan.incomingPlaybackRate) * rate
        return when {
            peaks != null && peaks.end > start && peaks.start < end -> AutomixWaveformReadiness.READY
            waveformPending(if (outgoing) pair.outgoing else pair.incoming) -> AutomixWaveformReadiness.LOADING
            else -> AutomixWaveformReadiness.UNAVAILABLE
        }
    }

    private fun waveformEditingReady(pair: AutomixPair): Boolean =
        waveformReadiness(pair, outgoing = true) != AutomixWaveformReadiness.LOADING &&
            waveformReadiness(pair, outgoing = false) != AutomixWaveformReadiness.LOADING

    fun edit(pair: Long, edit: AutomixEdit) {
        val tracks = editable(pair) ?: return
        if (!waveformEditingReady(tracks) || previewJob?.isActive == true) return
        draft = edit
        audition = AutomixAudition()
        refresh()
    }

    fun apply(pair: Long): Boolean {
        val current = editable(pair) ?: return false
        if (!waveformEditingReady(current)) return false
        val plan = current.plan(draft)
        if (previewJob?.isActive == true || !canUse(plan, player()?.currentPosition ?: return false)) return false
        appliedEdits[current.identity] = EditedJoin(current.outgoing.mediaId, current.incoming.mediaId, draft)
        appliedTempoMatching = AppSettings.activeAutomixTempoMatching.value
        appliedPlan = plan.takeUnless { draft == AutomixEdit() }
        appliedSources = current.outgoing to current.incoming
        return true
    }

    private fun canUse(plan: TransitionPlan, position: Long): Boolean =
        plan.reason == "studio-off" || (!plan.blocked && plan.transitionStart * 1000 > position)

    fun stopPreview(pair: Long) {
        if (pair == pairNumber) cancelPreview(resume = true)
    }

    fun open(pair: Long) {
        val tracks = currentPair()?.takeIf { pair == pairNumber && it.identity == key } ?: return
        requestWaveform(tracks.outgoing, tracks.outgoingDurationMs)
        requestWaveform(tracks.incoming, tracks.incomingDurationMs)
        refresh()
    }

    fun close(pair: Long) {
        if (pair != pairNumber) return
        cancelPreview(resume = true)
        draft = currentPair()?.takeIf { it.identity == key }?.let(::appliedEdit) ?: AutomixEdit()
        audition = AutomixAudition()
        refresh()
    }

    fun cancelPreview(resume: Boolean = false) {
        resumeAfterPreview = resume
        previewJob?.cancel()
    }

    fun replaceQueue(savedMix: SavedQueueMix? = null) {
        setPair(null)
        appliedEdits.clear()
        replaying = savedMix
        AppSettings.useQueueAutomix(savedMix?.let { it.automix to it.tempoMatching })
    }

    fun profileChanged(profile: QueueMixProfile?) {
        val savedMix = replaying ?: return
        if (savedMix.profile != profile) replaceQueue()
    }

    fun detach() {
        cancelPreview()
        if (AndroidAutomixStudio.session === this) {
            AppSettings.useQueueAutomix(null)
            AndroidAutomixStudio.session = null
            AndroidAutomixStudio.state.value = null
        }
    }

    fun preview(pair: Long, compare: Boolean) {
        if (previewJob?.isActive == true) return
        val tracks = editable(pair) ?: return
        val owner = player() ?: return
        val plan = if (compare) tracks.automatic else tracks.plan(draft)
        val cut = plan.reason == "studio-off" || plan.blocked
        val start = if (cut) tracks.outgoingDurationMs else (plan.transitionStart * 1000).roundToLong()
        val startWithContext = (start - 2000L).coerceAtLeast(0)
        val route = output()
        resumeAfterPreview = true
        audition = AutomixAudition(AutomixAuditionPhase.BUFFERING, compare, plan = plan)
        refresh()
        previewJob = scope.launch {
            val leftFilter = TransitionFilterProcessor()
            val rightFilter = TransitionFilterProcessor()
            val leftLoudness = LoudnessProcessor()
            val rightLoudness = LoudnessProcessor()
            var leftDeck: ExoPlayer? = null
            var rightDeck: ExoPlayer? = null
            var renderer: CrossfadeController? = null
            var pausedByPreview = false
            var unavailable = false
            fun stillCurrent() = player() === owner && key == tracks.identity && output() == route &&
                eligible() && AppSettings.activeSmartFadeEnabled.value && liveRenderer()?.studioBusy == false

            try {
                val left = buildDeck(leftFilter, leftLoudness).also { leftDeck = it }
                val right = buildDeck(rightFilter, rightLoudness).also { rightDeck = it }
                leftLoudness.track(tracks.outgoing.mediaId, tracks.incoming.mediaId)
                rightLoudness.track(tracks.incoming.mediaId, null)
                var active = left
                var spare = right
                fun activeFilter() = if (active === left) leftFilter else rightFilter
                fun spareFilter() = if (spare === left) leftFilter else rightFilter
                val mix = CrossfadeController(
                    scope = scope, active = { active }, standby = { spare },
                    onHandoff = { out, into ->
                        out.setAudioAttributes(audioAttributes, false)
                        into.setAudioAttributes(audioAttributes, true)
                        into.setHandleAudioBecomingNoisy(true)
                        active = into
                        spare = out
                    },
                    analysisFor = { if (it == tracks.outgoing) tracks.outgoingAnalysis else tracks.incomingAnalysis },
                    filters = object : TransitionFilters {
                        override fun incoming(lowPassHz: Float, highPassHz: Float) = activeFilter().setCutoffs(lowPassHz, highPassHz)
                        override fun outgoing(lowPassHz: Float, highPassHz: Float) = spareFilter().setCutoffs(lowPassHz, highPassHz)
                        override fun incomingLeadMs() = activeFilter().leadUs / 1000
                        override fun outgoingLeadMs() = spareFilter().leadUs / 1000
                        override fun incomingEcho(delaySeconds: Float, send: Float, dry: Float) = activeFilter().setEcho(delaySeconds, send, dry)
                        override fun outgoingEcho(delaySeconds: Float, send: Float, dry: Float) = spareFilter().setEcho(delaySeconds, send, dry)
                        override fun parkEchoes() { leftFilter.parkEcho(); rightFilter.parkEcho() }
                    },
                    onBlendHeadroom = { leftLoudness.setBlendTrim(it); rightLoudness.setBlendTrim(it) },
                    audition = true,
                ).also { renderer = it }
                left.setMediaItems(listOf(tracks.outgoing, tracks.incoming), 0, startWithContext)
                left.setPlaybackSpeed(AppSettings.playbackSpeed.value)
                left.prepare()
                if (cut) {
                    left.pauseAtEndOfMediaItems = true
                    right.setMediaItem(tracks.incoming)
                    right.setPlaybackSpeed(AppSettings.playbackSpeed.value)
                    right.prepare()
                } else {
                    check(mix.armAudition(plan))
                }
                val prepared = withTimeout(10_000) {
                    while (left.playbackState != Player.STATE_READY ||
                        if (cut) right.playbackState != Player.STATE_READY else !mix.auditionReady) {
                        if (!stillCurrent()) return@withTimeout false
                        check(left.playerError == null && right.playerError == null)
                        delay(30)
                    }
                    true
                }
                if (!prepared || !stillCurrent()) return@launch
                pausedByPreview = owner.playWhenReady
                owner.pause()
                left.setAudioAttributes(audioAttributes, true)
                left.setHandleAudioBecomingNoisy(true)
                left.play()
                val incomingCueMs = if (cut) 0L else (plan.incomingCueTime * 1000).roundToLong()
                val incomingOverlapMs = if (cut) 0L else
                    (plan.fadeMs * plan.incomingPlaybackRate / plan.outgoingPlaybackRate).roundToLong()
                val incomingEnd = incomingCueMs + incomingOverlapMs + 2000L
                withTimeout(25_000) {
                    while (stillCurrent() && !owner.playWhenReady) {
                        if (active.playerError != null || spare.playerError != null) { unavailable = true; break }
                        if (active.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE) {
                            resumeAfterPreview = false
                            break
                        }
                        if (cut && active === left && (!left.playWhenReady || left.playbackState == Player.STATE_ENDED)) {
                            if (left.currentPosition + 300L < start) { resumeAfterPreview = false; break }
                            left.stop()
                            left.setAudioAttributes(audioAttributes, false)
                            right.setAudioAttributes(audioAttributes, true)
                            right.setHandleAudioBecomingNoisy(true)
                            active = right
                            spare = left
                            right.play()
                        } else if (!cut) {
                            if (!active.playWhenReady) { resumeAfterPreview = false; break }
                            mix.advanceAudition()
                            if (!mix.auditionInFlight && active === left) { unavailable = true; break }
                        }
                        if (active === right && (active.currentPosition >= incomingEnd || !active.playWhenReady ||
                                active.playbackState == Player.STATE_ENDED)) break
                        val secondsFromCue = if (active === left) {
                            (left.currentPosition - start) / 1000.0 / plan.outgoingPlaybackRate
                        } else {
                            (right.currentPosition - incomingCueMs) / 1000.0 / plan.incomingPlaybackRate
                        }
                        audition = AutomixAudition(AutomixAuditionPhase.PLAYING, compare, secondsFromCue, plan)
                        AndroidAutomixStudio.state.value?.takeIf { it.pair == pair }?.let {
                            AndroidAutomixStudio.state.value = it.copy(audition = audition)
                        }
                        delay(25)
                    }
                }
            } catch (_: TimeoutCancellationException) {
                unavailable = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IllegalStateException) {
                unavailable = true
            } finally {
                renderer?.release()
                leftDeck?.release()
                rightDeck?.release()
                if (pausedByPreview && resumeAfterPreview && stillCurrent() && !owner.playWhenReady) owner.play()
                audition = AutomixAudition(if (unavailable) AutomixAuditionPhase.UNAVAILABLE else AutomixAuditionPhase.IDLE)
                refresh()
            }
        }
    }
}
