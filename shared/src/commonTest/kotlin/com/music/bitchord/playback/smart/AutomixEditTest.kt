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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutomixEditTest {
    private val analysis = TrackAnalysis(beatInterval = 0.5, firstBeat = 0.25)
    private val automatic = TransitionPlan(
        transitionStart = 90.0, transitionEnd = 98.0, fadeSeconds = 8.0,
        incomingCueTime = 4.0, incomingPlaybackRate = 1.06, beatSeconds = 0.5,
        transitionStyle = TransitionStyle.DJ_BLEND, phaseLock = true, bassSwap = true,
    )

    private fun apply(edit: AutomixEdit) = automatic.withAutomixEdit(edit, 100.0, 60.0, analysis, analysis)

    @Test fun autoKeepsThePlannerOutput() {
        assertEquals(automatic, apply(AutomixEdit()))
    }

    @Test fun offUsesTheNaturalEnd() {
        val plan = apply(AutomixEdit(style = AutomixStyle.OFF))
        assertTrue(plan.blocked)
        assertEquals(100.0, plan.transitionStart)
    }

    @Test fun aTempoLimitDropsTheStretchAndEffects() {
        val plan = apply(AutomixEdit(style = AutomixStyle.DJ, tempoLimitPercent = 3))
        assertEquals(1.0, plan.incomingPlaybackRate)
        assertEquals(TransitionStyle.EQUAL_POWER, plan.transitionStyle)
        assertEquals("studio-tempo-limit", plan.reason)
        assertFalse(plan.bassSwap)
    }

    @Test fun editedCuesSnapAndLeaveEnoughIncomingAudio() {
        val plan = apply(AutomixEdit(outgoingCue = 94.71, incomingCue = 59.9, overlapSeconds = 12.0))
        assertEquals(94.75, plan.transitionStart)
        assertEquals(57.0, plan.incomingCueTime)
        assertEquals(1.0, plan.fadeSeconds)
        assertFalse(plan.bassSwap)
    }

    @Test fun djUsesAFullSweepWhenThePlannerAllowsFiltering() {
        val filtered = automatic.copy(transitionStyle = TransitionStyle.DJ_FILTER, filterSweep = 0.4)
        val plan = filtered.withAutomixEdit(AutomixEdit(style = AutomixStyle.DJ), 100.0, 60.0, analysis, analysis)
        assertEquals(1.0, plan.filterSweep)
    }
}
