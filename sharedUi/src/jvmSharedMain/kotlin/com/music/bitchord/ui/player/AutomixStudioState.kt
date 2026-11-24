package com.music.bitchord.ui.player

import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.smart.AutomixEdit
import com.music.bitchord.playback.smart.TransitionPlan
import com.music.bitchord.playback.smart.TrackAnalysis
import com.music.bitchord.playback.smart.withAutomixEdit
import kotlinx.coroutines.flow.StateFlow

interface AutomixStudio {
    val state: StateFlow<AutomixStudioState?>
    fun open(pair: Long)
    fun edit(pair: Long, edit: AutomixEdit)
    fun apply(pair: Long): Boolean
    fun preview(pair: Long, compare: Boolean)
    fun stopPreview(pair: Long)
    fun close(pair: Long)
}

data class AutomixStudioState(
    val pair: Long,
    val outgoing: Song,
    val incoming: Song,
    val outgoingDuration: Double,
    val incomingDuration: Double,
    val plan: TransitionPlan,
    val automaticPlan: TransitionPlan,
    val edit: AutomixEdit,
    val outgoingAnalysis: TrackAnalysis,
    val incomingAnalysis: TrackAnalysis,
    val busy: Boolean,
    val audition: AutomixAudition = AutomixAudition(),
    val outgoingWaveform: AutomixWaveformReadiness = AutomixWaveformReadiness.UNAVAILABLE,
    val incomingWaveform: AutomixWaveformReadiness = AutomixWaveformReadiness.UNAVAILABLE,
) {
    val waveformLoading: Boolean
        get() = outgoingWaveform == AutomixWaveformReadiness.LOADING ||
            incomingWaveform == AutomixWaveformReadiness.LOADING

    fun planFor(edit: AutomixEdit): TransitionPlan = automaticPlan.withAutomixEdit(
        edit, outgoingDuration, incomingDuration, outgoingAnalysis, incomingAnalysis,
    )
}

enum class AutomixWaveformReadiness { LOADING, READY, UNAVAILABLE }

enum class AutomixAuditionPhase { IDLE, BUFFERING, PLAYING, UNAVAILABLE }

data class AutomixAudition(
    val phase: AutomixAuditionPhase = AutomixAuditionPhase.IDLE,
    val comparing: Boolean = false,
    val secondsFromCue: Double = 0.0,
    val plan: TransitionPlan? = null,
)
