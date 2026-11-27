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

/** User-selectable performance profile in Advanced & Performance settings. */
enum class PerformanceProfile {
    AUTO,
    BALANCED,
    SAVER,
    HIGH,
}

/** Resolved hardware/runtime tier produced by a [PerformanceProfile]. */
enum class PerformanceTier {
    HIGH,
    BALANCED,
    SAVER,
}

/** Window display refresh-rate policy driven by the active [PerformanceTier]. */
enum class RefreshRatePolicy {
    PINNED_MAX,
    ADAPTIVE_VRR,
    CAP_60HZ,
}

/**
 * Snapshot of all effective rendering, motion, refresh-rate, and optional
 * audio/canvas integration choices derived from [PerformanceProfile] + overrides.
 */
data class EffectivePerformancePolicy(
    val selectedProfile: PerformanceProfile = PerformanceProfile.AUTO,
    val resolvedTier: PerformanceTier = PerformanceTier.BALANCED,
    val reduceDynamicBlur: Boolean = false,
    /**
     * Disables infinite/continuous decorative frame loops (ChordWave idle,
     * shimmers, wave breathing, OrbContours, press3D tilt, 3D cover sweeps)
     * while still allowing crisp short UI transitions unless [reduceAnimation] is set.
     */
    val disableInfiniteLoops: Boolean = false,
    /** Full reduced-motion mode: snaps sheets, tabs, and dock transitions too. */
    val reduceAnimation: Boolean = false,
    val refreshRatePolicy: RefreshRatePolicy = RefreshRatePolicy.ADAPTIVE_VRR,
    /**
     * Target interval in ms for continuous decorative canvases when untouched:
     * `0L` = full display rate, `33L` = ~30 fps idle pacing, [Long.MAX_VALUE] = static.
     */
    val idleAnimationFrameMs: Long = DEFAULT_BALANCED_IDLE_FRAME_MS,
    val isCustomized: Boolean = false,
    val linkAutomixAndCanvas: Boolean = false,
) {
    val highPerformanceMode: Boolean
        get() = resolvedTier == PerformanceTier.HIGH

    companion object {
        const val DEFAULT_BALANCED_IDLE_FRAME_MS = 33L
    }
}

/** Pure policy resolver shared by Android runtime and JVM unit tests. */
fun resolveEffectivePerformancePolicy(
    selectedProfile: PerformanceProfile,
    autoDeviceTier: PerformanceTier,
    reduceBlurOverride: Boolean? = null,
    reduceAnimationOverride: Boolean? = null,
    linkAutomixAndCanvas: Boolean = false,
): EffectivePerformancePolicy {
    val resolvedTier = when (selectedProfile) {
        PerformanceProfile.AUTO -> autoDeviceTier
        PerformanceProfile.HIGH -> PerformanceTier.HIGH
        PerformanceProfile.BALANCED -> PerformanceTier.BALANCED
        PerformanceProfile.SAVER -> PerformanceTier.SAVER
    }
    val baseReduceBlur = resolvedTier == PerformanceTier.SAVER
    val baseDisableLoops = resolvedTier == PerformanceTier.SAVER
    val baseReduceAnimation = false

    val effectiveReduceBlur = reduceBlurOverride ?: baseReduceBlur
    val effectiveReduceAnimation = reduceAnimationOverride ?: baseReduceAnimation
    val effectiveDisableLoops = baseDisableLoops || effectiveReduceAnimation

    val refreshRatePolicy = when (resolvedTier) {
        PerformanceTier.HIGH -> RefreshRatePolicy.PINNED_MAX
        PerformanceTier.BALANCED -> RefreshRatePolicy.ADAPTIVE_VRR
        PerformanceTier.SAVER -> RefreshRatePolicy.CAP_60HZ
    }

    val idleFrameMs = when {
        effectiveDisableLoops -> Long.MAX_VALUE
        resolvedTier == PerformanceTier.HIGH -> 0L
        else -> EffectivePerformancePolicy.DEFAULT_BALANCED_IDLE_FRAME_MS
    }

    val customized = (reduceBlurOverride != null && reduceBlurOverride != baseReduceBlur) ||
        (reduceAnimationOverride != null && reduceAnimationOverride != baseReduceAnimation)

    return EffectivePerformancePolicy(
        selectedProfile = selectedProfile,
        resolvedTier = resolvedTier,
        reduceDynamicBlur = effectiveReduceBlur,
        disableInfiniteLoops = effectiveDisableLoops,
        reduceAnimation = effectiveReduceAnimation,
        refreshRatePolicy = refreshRatePolicy,
        idleAnimationFrameMs = idleFrameMs,
        isCustomized = customized,
        linkAutomixAndCanvas = linkAutomixAndCanvas,
    )
}
