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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomPresetLimitTest {
    @Test
    fun allowsUpToTenCustomPresetsAndRejectsEleventh() {
        assertEquals(10, AppSettings.MAX_CUSTOM_PALETTES)
        val initial = AppSettings.customPalettes.value
        initial.forEach { AppSettings.deleteCustomPalette(it.id) }
        try {
            repeat(10) { idx ->
                val saved = AppSettings.saveCustomPalette(
                    CustomPalette(
                        id = "test-$idx",
                        name = "Style ${idx + 1}",
                        primaryHex = 0xFF112233L,
                        secondaryHex = 0xFF445566L,
                        backgroundHex = 0xFF000000L,
                        surfaceHex = 0xFF101010L,
                    ),
                )
                assertTrue("Expected preset $idx to save", saved)
            }
            assertEquals(10, AppSettings.customPalettes.value.size)

            val overflow = AppSettings.saveCustomPalette(
                CustomPalette(
                    id = "test-overflow",
                    name = "Style 11",
                    primaryHex = 0xFF112233L,
                    secondaryHex = 0xFF445566L,
                    backgroundHex = 0xFF000000L,
                    surfaceHex = 0xFF101010L,
                ),
            )
            assertFalse("Expected 11th preset to be rejected", overflow)
            assertEquals(10, AppSettings.customPalettes.value.size)
        } finally {
            AppSettings.customPalettes.value.forEach { AppSettings.deleteCustomPalette(it.id) }
            initial.forEach { AppSettings.saveCustomPalette(it) }
        }
    }
}
