package com.music.bitchord.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.music.bitchord.data.settings.AppSettings
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
) {
    private var key: Pair<Any, Any>? = null
    private var pairNumber = 0L
    private var draft = AutomixEdit()
    private var applied = AutomixEdit()
    private var appliedPlan: TransitionPlan? = null
    private var appliedSources: Pair<MediaItem, MediaItem>? = null
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

    private fun setPair(pair: AutomixPair?) {
        if (key == pair?.identity) return
        key = pair?.identity
        pairNumber++
        draft = AutomixEdit()
        applied = AutomixEdit()
        appliedPlan = null
        appliedSources = null
        cancelPreview(resume = false)
        audition = AutomixAudition()
    }

    fun livePlan(pair: AutomixPair): TransitionPlan {
        setPair(pair.takeIf { eligible() && AppSettings.smartFadeEnabled.value })
        if (appliedSources == (pair.outgoing to pair.incoming)) appliedPlan?.let { return it }
        return pair.plan(applied)
    }

    private fun refresh() {
        if (AndroidAutomixStudio.session !== this) return
        if (AndroidAutomixStudio.state.subscriptionCount.value == 0 && key == null) return
        val pair = currentPair()
        setPair(pair.takeIf { AppSettings.smartFadeEnabled.value })
        AndroidAutomixStudio.state.value = pair?.let {
            AutomixStudioState(
                pair = pairNumber,
                outgoing = it.outgoing.toSong(), incoming = it.incoming.toSong(),
                outgoingDuration = it.outgoingDurationMs / 1000.0,
                incomingDuration = it.incomingDurationMs / 1000.0,
                plan = it.plan(draft), edit = draft,
                outgoingBeats = it.outgoingAnalysis.downbeats.takeIf { _ -> it.outgoingAnalysis.beatConfidence >= 0.5 }.orEmpty(),
                incomingBeats = it.incomingAnalysis.downbeats.takeIf { _ -> it.incomingAnalysis.beatConfidence >= 0.5 }.orEmpty(),
                busy = liveRenderer()?.studioBusy != false,
                audition = audition,
            )
        }
    }

    private fun editable(pair: Long): AutomixPair? = currentPair()?.takeIf {
        pair == pairNumber && it.identity == key && AppSettings.smartFadeEnabled.value &&
            liveRenderer()?.studioBusy == false
    }

    fun edit(pair: Long, edit: AutomixEdit) {
        if (editable(pair) == null || previewJob?.isActive == true) return
        draft = edit
        audition = AutomixAudition()
        refresh()
    }

    fun apply(pair: Long): Boolean {
        val current = editable(pair) ?: return false
        val plan = current.plan(draft)
        if (previewJob?.isActive == true || !canUse(plan, player()?.currentPosition ?: return false)) return false
        applied = draft
        appliedPlan = plan.takeUnless { draft == AutomixEdit() }
        appliedSources = current.outgoing to current.incoming
        return true
    }

    private fun canUse(plan: TransitionPlan, position: Long): Boolean =
        plan.reason == "studio-off" || (!plan.blocked && plan.transitionStart * 1000 > position)

    fun stopPreview(pair: Long) {
        if (pair == pairNumber) cancelPreview(resume = true)
    }

    fun close(pair: Long) {
        if (pair != pairNumber) return
        cancelPreview(resume = true)
        draft = applied
        audition = AutomixAudition()
        refresh()
    }

    fun cancelPreview(resume: Boolean = false) {
        resumeAfterPreview = resume
        previewJob?.cancel()
    }

    fun replaceQueue() = setPair(null)

    fun detach() {
        cancelPreview()
        if (AndroidAutomixStudio.session === this) {
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
                eligible() && AppSettings.smartFadeEnabled.value && liveRenderer()?.studioBusy == false

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
                        val progress = if (active === left) {
                            if (left.currentPosition < start || cut) {
                                0.40f * (left.currentPosition - startWithContext) / (start - startWithContext).coerceAtLeast(1)
                            } else {
                                0.40f + 0.39f * (left.currentPosition - start) / plan.fadeMs.coerceAtLeast(1)
                            }
                        } else {
                            val elapsed = (right.currentPosition - incomingCueMs).coerceAtLeast(0)
                            when {
                                cut -> 0.40f + 0.60f * elapsed / 2000
                                elapsed <= incomingOverlapMs -> 0.40f + 0.39f * elapsed / incomingOverlapMs.coerceAtLeast(1)
                                else -> 0.79f + 0.21f * (elapsed - incomingOverlapMs) / 2000
                            }
                        }
                        audition = AutomixAudition(AutomixAuditionPhase.PLAYING, compare, progress.coerceIn(0f, 1f), plan)
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
