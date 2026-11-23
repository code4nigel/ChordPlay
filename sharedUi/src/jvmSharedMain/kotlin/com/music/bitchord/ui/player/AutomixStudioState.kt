package com.music.bitchord.ui.player

import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.smart.AutomixEdit
import com.music.bitchord.playback.smart.TransitionPlan
import kotlinx.coroutines.flow.StateFlow

interface AutomixStudio {
    val state: StateFlow<AutomixStudioState?>
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
    val edit: AutomixEdit,
    val outgoingBeats: List<Double>,
    val incomingBeats: List<Double>,
    val busy: Boolean,
    val audition: AutomixAudition = AutomixAudition(),
)

enum class AutomixAuditionPhase { IDLE, BUFFERING, PLAYING, UNAVAILABLE }

data class AutomixAudition(
    val phase: AutomixAuditionPhase = AutomixAuditionPhase.IDLE,
    val comparing: Boolean = false,
    val position: Float = 0f,
    val plan: TransitionPlan? = null,
)
