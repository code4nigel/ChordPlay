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

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.music.bitchord.data.settings.PerformanceTier

/**
 * Probes device memory, CPU core count, and Android OS level once so
 * [com.music.bitchord.data.settings.PerformanceProfile.AUTO] selects an
 * appropriate runtime tier without degrading modern flagships or overloading
 * legacy / low-RAM devices.
 */
object DevicePerformanceClassifier {
    internal const val LOW_RAM_BYTES_THRESHOLD = 4_300_000_000L
    internal const val MIN_MODERN_CORES = 4
    internal const val MIN_MODERN_SDK = Build.VERSION_CODES.R

    @Volatile
    private var cachedTier: PerformanceTier? = null

    fun classify(context: Context): PerformanceTier {
        cachedTier?.let { return it }
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }
        return classifyMetrics(
            sdkInt = Build.VERSION.SDK_INT,
            isLowRamDevice = am?.isLowRamDevice == true,
            totalMemBytes = memInfo.totalMem,
            cpuCores = Runtime.getRuntime().availableProcessors(),
        ).also { cachedTier = it }
    }

    internal fun classifyMetrics(
        sdkInt: Int,
        isLowRamDevice: Boolean,
        totalMemBytes: Long,
        cpuCores: Int,
    ): PerformanceTier {
        val lowMem = totalMemBytes in 1L..LOW_RAM_BYTES_THRESHOLD
        val fewCores = cpuCores in 1..MIN_MODERN_CORES
        val legacyAndroid = sdkInt < MIN_MODERN_SDK
        return if (isLowRamDevice || lowMem || fewCores || legacyAndroid) {
            PerformanceTier.SAVER
        } else {
            PerformanceTier.BALANCED
        }
    }
}
