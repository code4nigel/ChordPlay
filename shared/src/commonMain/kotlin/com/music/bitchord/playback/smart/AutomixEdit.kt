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
package com.music.bitchord.playback.smart

import kotlin.math.abs
import kotlin.math.round
import kotlinx.serialization.Serializable

@Serializable
enum class AutomixStyle { AUTO, GENTLE, DJ, OFF }

@Serializable
data class AutomixEdit(
    val style: AutomixStyle = AutomixStyle.AUTO,
    val overlapSeconds: Double? = null,
    val outgoingCue: Double? = null,
    val incomingCue: Double? = null,
    val tempoLimitPercent: Int = 10,
)

/** Applies an edit to the automatic plan before any player is armed. */
fun TransitionPlan.withAutomixEdit(
    edit: AutomixEdit,
    outgoingDuration: Double,
    incomingDuration: Double,
    outgoing: TrackAnalysis,
    incoming: TrackAnalysis,
): TransitionPlan {
    if (edit.style == AutomixStyle.OFF) return TransitionPlan(
        blocked = true,
        transitionStart = outgoingDuration,
        transitionEnd = outgoingDuration,
        reason = "studio-off",
    )
    if (blocked || edit == AutomixEdit()) return this

    val tempoChange = (maxOf(outgoingPlaybackRate, incomingPlaybackRate) - 1.0) * 100.0
    val atOriginalSpeed = edit.style == AutomixStyle.GENTLE || tempoChange > edit.tempoLimitPercent + 0.0001
    val core = when {
        atOriginalSpeed -> TransitionPlan(
            transitionStyle = TransitionStyle.EQUAL_POWER,
            reason = if (edit.style == AutomixStyle.GENTLE) "studio-gentle" else "studio-tempo-limit",
        )
        edit.style == AutomixStyle.DJ && transitionStyle == TransitionStyle.DJ_FILTER ->
            copy(filterSweep = 1.0, filterVariant = FilterTransitionVariant.SWEEP)
        else -> this
    }
    val timingEdited = edit.overlapSeconds != null || edit.outgoingCue != null || edit.incomingCue != null
    if (!atOriginalSpeed && !timingEdited) return core

    val align = core.phaseLock && core.beatSeconds > 0.0
    val requestedLength = (edit.overlapSeconds?.takeIf(Double::isFinite) ?: fadeSeconds).coerceIn(1.0, 12.0)
    val defaultStart = if (edit.style == AutomixStyle.GENTLE) outgoingDuration - requestedLength else
        transitionEnd - requestedLength
    val start = snapCue(edit.outgoingCue, defaultStart, outgoing, align).coerceIn(0.0, outgoingDuration)
    val defaultCue = if (edit.style == AutomixStyle.GENTLE) 0.0 else incomingCueTime
    val requestedCue = snapCue(edit.incomingCue, defaultCue, incoming, align).coerceAtLeast(0.0)
    val cue = if (incomingDuration > 0) requestedCue.coerceAtMost((incomingDuration - 3.0).coerceAtLeast(0.0)) else defaultCue
    val incomingCap = if (incomingDuration > 0) (incomingDuration - cue) / 3 else requestedLength
    val length = minOf(requestedLength, outgoingDuration - start, incomingCap)
    if (length <= 0.0) return core.copy(blocked = true, reason = "studio-no-room")

    // An edited cue can land elsewhere in the song. Leave the time-based
    // effects off when their original vocal/bass anchor no longer applies.
    val changedCue = abs(cue - incomingCueTime) > 0.01 || abs(start - transitionStart) > 0.01
    return core.copy(
        markerVisible = true,
        transitionStart = start,
        transitionEnd = start + length,
        fadeSeconds = length,
        incomingCueTime = cue,
        incomingHandoffTime = cue + length,
        bassSwap = core.bassSwap && !changedCue,
        vocalOverlap = if (changedCue) 0.0 else core.vocalOverlap,
        echoSeconds = if (changedCue) 0.0 else core.echoSeconds,
    )
}

private fun snapCue(requested: Double?, fallback: Double, analysis: TrackAnalysis, align: Boolean): Double {
    if (requested == null || !requested.isFinite()) return fallback
    if (!align || analysis.beatInterval <= 0.0) return requested
    val anchor = analysis.downbeats.minByOrNull { abs(it - requested) } ?: analysis.firstBeat
    return anchor + round((requested - anchor) / analysis.beatInterval) * analysis.beatInterval
}
