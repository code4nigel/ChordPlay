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
package com.music.bitchord.data.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PerformanceProfileTest {

    @Test
    fun autoProfileResolvesToDetectedHardwareTier() {
        val autoSaver = resolveEffectivePerformancePolicy(
            selectedProfile = PerformanceProfile.AUTO,
            autoDeviceTier = PerformanceTier.SAVER,
        )
        assertEquals(PerformanceTier.SAVER, autoSaver.resolvedTier)
        assertTrue(autoSaver.reduceDynamicBlur)
        assertTrue(autoSaver.disableInfiniteLoops)
        assertFalse(autoSaver.reduceAnimation)
        assertEquals(RefreshRatePolicy.CAP_60HZ, autoSaver.refreshRatePolicy)
        assertEquals(Long.MAX_VALUE, autoSaver.idleAnimationFrameMs)
        assertFalse(autoSaver.isCustomized)

        val autoBalanced = resolveEffectivePerformancePolicy(
            selectedProfile = PerformanceProfile.AUTO,
            autoDeviceTier = PerformanceTier.BALANCED,
        )
        assertEquals(PerformanceTier.BALANCED, autoBalanced.resolvedTier)
        assertFalse(autoBalanced.reduceDynamicBlur)
        assertFalse(autoBalanced.disableInfiniteLoops)
        assertFalse(autoBalanced.reduceAnimation)
        assertEquals(RefreshRatePolicy.ADAPTIVE_VRR, autoBalanced.refreshRatePolicy)
        assertEquals(EffectivePerformancePolicy.DEFAULT_BALANCED_IDLE_FRAME_MS, autoBalanced.idleAnimationFrameMs)
        assertFalse(autoBalanced.isCustomized)
    }

    @Test
    fun explicitProfilesEnforceExpectedPolicies() {
        val high = resolveEffectivePerformancePolicy(
            selectedProfile = PerformanceProfile.HIGH,
            autoDeviceTier = PerformanceTier.SAVER,
        )
        assertEquals(PerformanceTier.HIGH, high.resolvedTier)
        assertTrue(high.highPerformanceMode)
        assertEquals(RefreshRatePolicy.PINNED_MAX, high.refreshRatePolicy)
        assertEquals(0L, high.idleAnimationFrameMs)
        assertFalse(high.reduceDynamicBlur)
        assertFalse(high.disableInfiniteLoops)

        val saver = resolveEffectivePerformancePolicy(
            selectedProfile = PerformanceProfile.SAVER,
            autoDeviceTier = PerformanceTier.BALANCED,
        )
        assertEquals(PerformanceTier.SAVER, saver.resolvedTier)
        assertFalse(saver.highPerformanceMode)
        assertTrue(saver.reduceDynamicBlur)
        assertTrue(saver.disableInfiniteLoops)
        assertFalse(saver.reduceAnimation)
        assertEquals(RefreshRatePolicy.CAP_60HZ, saver.refreshRatePolicy)
        assertEquals(Long.MAX_VALUE, saver.idleAnimationFrameMs)
    }

    @Test
    fun manualOverridesMarkPolicyCustomizedOnlyWhenDifferingFromBase() {
        val matchingBalanced = resolveEffectivePerformancePolicy(
            selectedProfile = PerformanceProfile.BALANCED,
            autoDeviceTier = PerformanceTier.BALANCED,
            reduceBlurOverride = false,
            reduceAnimationOverride = false,
        )
        assertFalse(matchingBalanced.isCustomized)

        val reducedBlurBalanced = resolveEffectivePerformancePolicy(
            selectedProfile = PerformanceProfile.BALANCED,
            autoDeviceTier = PerformanceTier.BALANCED,
            reduceBlurOverride = true,
        )
        assertTrue(reducedBlurBalanced.isCustomized)
        assertTrue(reducedBlurBalanced.reduceDynamicBlur)
        assertFalse(reducedBlurBalanced.disableInfiniteLoops)

        val reducedMotionHigh = resolveEffectivePerformancePolicy(
            selectedProfile = PerformanceProfile.HIGH,
            autoDeviceTier = PerformanceTier.BALANCED,
            reduceAnimationOverride = true,
        )
        assertTrue(reducedMotionHigh.isCustomized)
        assertTrue(reducedMotionHigh.reduceAnimation)
        assertTrue(reducedMotionHigh.disableInfiniteLoops)
        assertEquals(Long.MAX_VALUE, reducedMotionHigh.idleAnimationFrameMs)

        val reEnabledBlurOnSaver = resolveEffectivePerformancePolicy(
            selectedProfile = PerformanceProfile.SAVER,
            autoDeviceTier = PerformanceTier.SAVER,
            reduceBlurOverride = false,
        )
        assertTrue(reEnabledBlurOnSaver.isCustomized)
        assertFalse(reEnabledBlurOnSaver.reduceDynamicBlur)
        assertTrue(reEnabledBlurOnSaver.disableInfiniteLoops)
    }
}
