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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPlaybackVolumeTest {
    @Test
    fun everyPercentSettlesAndHasDistinctLevelAcrossHardwareSteps() {
        for (max in listOf(1, 7, 15, 25, 30, 100, 255)) {
            var previous = -1f
            for (percent in 0..100) {
                val level = percent / 100f
                val target = localVolumeTarget(level, max, fineVolume = true)
                val effective = target.index.toFloat() / max * target.gain
                assertEquals(level, target.displayed, 0f)
                assertEquals(level, effective, 0.0001f)
                assertTrue(effective > previous)
                assertTrue(target.gain in 0f..1f)
                previous = effective
            }
        }
    }

    @Test
    fun interpolatesTheRouteCurveAndKeepsHardwareOnlyBehavior() {
        val fine = localVolumeTarget(0.15f, 10, fineVolume = true) { index -> (index - 2) * 20f }
        assertEquals(2, fine.index)
        assertEquals(0.55f, fine.gain, 0.001f)
        val flatRoute = localVolumeTarget(0.15f, 10, fineVolume = true) { 0f }
        assertEquals(0.75f, flatRoute.gain, 0.001f)
        val minimum = localVolumeTarget(0.03f, 15, fineVolume = true, min = 2)
        assertEquals(2, minimum.index)
        assertEquals(0.225f, minimum.gain, 0.001f)
        val hardware = localVolumeTarget(0.03f, 15, fineVolume = false)
        assertEquals(0, hardware.index)
        assertEquals(1f, hardware.gain, 0f)
    }
}
