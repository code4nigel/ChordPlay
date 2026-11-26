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
package com.music.bitchord.ui.components

import kotlin.test.Test
import kotlin.test.assertTrue

class MixSourceBadgeTest {

    @Test
    fun computeMixBadgeGroundEnforcesWcagContrastAcrossExtremeCoverColors() {
        val sampleColors = listOf(
            0xFFFFFFFF.toInt(), // Pure white
            0xFFFFFF00.toInt(), // Bright yellow
            0xFF000000.toInt(), // Pure black
            0xFF00E5FF.toInt(), // Bright cyan
            0xFFFF3B30.toInt(), // Vivid red
        )

        sampleColors.forEach { seed ->
            val ground = computeMixBadgeGroundArgb(seed)
            val contrast = whiteContrastRatio(ground)
            assertTrue(
                contrast >= 4.5,
                "Expected WCAG AA contrast >= 4.5 for seed 0x${seed.toUInt().toString(16)}, got $contrast",
            )
        }
    }
}
