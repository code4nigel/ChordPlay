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
package com.music.bitchord.ui.performance

import android.os.Build
import com.music.bitchord.data.settings.PerformanceTier
import org.junit.Assert.assertEquals
import org.junit.Test

class DevicePerformanceClassifierTest {

    @Test
    fun flagshipModernHardwareResolvesToBalanced() {
        val tier = DevicePerformanceClassifier.classifyMetrics(
            sdkInt = Build.VERSION_CODES.VANILLA_ICE_CREAM,
            isLowRamDevice = false,
            totalMemBytes = 7_500_000_000L,
            cpuCores = 8,
        )
        assertEquals(PerformanceTier.BALANCED, tier)
    }

    @Test
    fun legacyAndroidSdkResolvesToSaverEvenWithAdequateSpecs() {
        val tier = DevicePerformanceClassifier.classifyMetrics(
            sdkInt = Build.VERSION_CODES.Q,
            isLowRamDevice = false,
            totalMemBytes = 6_000_000_000L,
            cpuCores = 8,
        )
        assertEquals(PerformanceTier.SAVER, tier)
    }

    @Test
    fun lowRamFlagOrFourGbOrQuadCoreResolvesToSaver() {
        assertEquals(
            PerformanceTier.SAVER,
            DevicePerformanceClassifier.classifyMetrics(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                isLowRamDevice = true,
                totalMemBytes = 6_000_000_000L,
                cpuCores = 8,
            ),
        )
        assertEquals(
            PerformanceTier.SAVER,
            DevicePerformanceClassifier.classifyMetrics(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                isLowRamDevice = false,
                totalMemBytes = 4_000_000_000L,
                cpuCores = 8,
            ),
        )
        assertEquals(
            PerformanceTier.SAVER,
            DevicePerformanceClassifier.classifyMetrics(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                isLowRamDevice = false,
                totalMemBytes = 8_000_000_000L,
                cpuCores = 4,
            ),
        )
    }

    @Test
    fun zeroMemoryInfoDoesNotFalselyDowngradeModernDevice() {
        val tier = DevicePerformanceClassifier.classifyMetrics(
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            isLowRamDevice = false,
            totalMemBytes = 0L,
            cpuCores = 8,
        )
        assertEquals(PerformanceTier.BALANCED, tier)
    }
}
