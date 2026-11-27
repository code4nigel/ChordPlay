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
package com.music.bitchord.playback.audio

import com.music.bitchord.playback.EqCurve
import com.music.bitchord.playback.EqLayout
import com.music.bitchord.playback.EqualizerProcessor
import com.music.bitchord.playback.LoudnessProcessor
import com.music.bitchord.playback.SpatialAudioProcessor
import com.music.bitchord.playback.TransitionFilterProcessor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DspChainIdleTest {

    private val sampleRate = 44_100

    private fun configuredChain(): Pair<DspChain, AudioBlock> {
        val chain = DspChain(
            spatial = SpatialAudioProcessor(),
            equalizer = EqualizerProcessor(),
            transition = TransitionFilterProcessor(),
            loudness = LoudnessProcessor(),
        )
        chain.configure(sampleRate, 2)
        val block = AudioBlock(channelCount = 2, capacityFrames = 512).apply { reset(512) }
        // First process settles initial snap on loudness so the chain starts cleanly idle.
        chain.process(block)
        return chain to block
    }

    @Test
    fun freshConfiguredChainBecomesIdleAfterInitialSnap() {
        val (chain, _) = configuredChain()
        assertTrue(chain.spatial.isIdle)
        assertTrue(chain.equalizer.isIdle)
        assertTrue(chain.transition.isIdle)
        assertTrue(chain.loudness.isIdle)
        assertTrue(chain.isIdle)
    }

    @Test
    @Suppress("DEPRECATION")
    fun spatialAndEqualizerTrackIdleStateAccurately() {
        val (chain, block) = configuredChain()

        chain.spatial.enabled = true
        assertFalse(chain.isIdle)
        chain.spatial.enabled = false
        assertTrue(chain.isIdle)

        val gains = FloatArray(EqLayout.SLOTS)
        val qs = FloatArray(EqLayout.SLOTS) { 0.707f }
        gains[3] = 4.5f
        chain.equalizer.setTuning(enabled = true, EqCurve(gains, qs, 0f), balance = 0f)
        assertFalse(chain.equalizer.isIdle)
        assertFalse(chain.isIdle)

        chain.equalizer.setTuning(enabled = false, EqCurve(FloatArray(EqLayout.SLOTS), qs, 0f), balance = 0f)
        // Equalizer smooths/settles when processed or flushed back to flat.
        repeat(8) { chain.process(block) }
        chain.equalizer.flush()
        assertTrue(chain.equalizer.isIdle)
        assertTrue(chain.isIdle)
    }

    @Test
    fun transitionFilterStaysNonIdleUntilParkedStateClearsIntegrators() {
        val (chain, block) = configuredChain()

        chain.transition.setCutoffs(1_200f, 0f)
        chain.process(block)
        assertFalse(chain.transition.isIdle)
        assertFalse(chain.isIdle)

        chain.transition.open()
        // Integrator state stays dirty until at least one parked block runs clearStateIfNeeded().
        repeat(64) { chain.process(block) }
        assertTrue(chain.transition.isIdle)

        chain.transition.setEcho(delaySeconds = 0.25f, send = 0.4f, dry = 0.8f)
        assertFalse(chain.transition.isIdle)
        assertFalse(chain.isIdle)

        chain.transition.parkEcho()
        assertTrue(chain.transition.isIdle)
        assertTrue(chain.isIdle)
    }

    @Test
    fun loudnessNormalizationAndBlendTrimControlIdleState() {
        val (chain, block) = configuredChain()

        chain.loudness.gainFor = { 0.75f }
        chain.loudness.track("track-a", null)
        assertFalse(chain.loudness.isIdle)
        assertFalse(chain.isIdle)

        chain.loudness.gainFor = { 1f }
        chain.loudness.onStreamBoundary()
        // Unsettled snap keeps isIdle false until processed.
        assertFalse(chain.loudness.isIdle)
        chain.process(block)
        assertTrue(chain.loudness.isIdle)

        chain.loudness.setBlendTrim(0.8f)
        chain.process(block)
        assertFalse(chain.loudness.isIdle)
        chain.loudness.setBlendTrim(1f)
        repeat(32) { chain.process(block) }
        assertTrue(chain.loudness.isIdle)
        assertTrue(chain.isIdle)
    }
}
