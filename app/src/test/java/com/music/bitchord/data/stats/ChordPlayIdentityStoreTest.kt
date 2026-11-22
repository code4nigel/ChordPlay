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
package com.music.bitchord.data.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordPlayIdentityStoreTest {

    @Test
    fun waveIdentitySignatureIsDeterministicAcrossCasingAndWhitespace() {
        val a = WaveIdentitySignature.fromName("Amit")
        val b = WaveIdentitySignature.fromName("  amit  ")
        val c = WaveIdentitySignature.fromName("AMIT")

        assertEquals(a.code, b.code)
        assertEquals(a.code, c.code)
        assertEquals(a.primaryColor, b.primaryColor)
        assertEquals(a.secondaryColor, b.secondaryColor)
        assertEquals(a.tertiaryColor, b.tertiaryColor)
        assertEquals(a.waveCount, b.waveCount)
        assertEquals(a.primaryFreq, b.primaryFreq, 0.0001f)
        assertEquals(a.secondaryFreq, b.secondaryFreq, 0.0001f)
        assertEquals(a.amplitude, b.amplitude, 0.0001f)
        assertEquals(a.spectrumBars, b.spectrumBars)
    }

    @Test
    fun waveIdentitySignatureVariesPerListenerAndStaysWithinBounds() {
        val defaultSig = WaveIdentitySignature.fromName("   ")
        val amit = WaveIdentitySignature.fromName("Amit")
        val shivanshu = WaveIdentitySignature.fromName("Shivanshu")

        assertEquals("CP-0000", defaultSig.code)
        assertNotEquals(defaultSig.code, amit.code)
        assertNotEquals(amit.code, shivanshu.code)
        assertNotEquals(amit.primaryColor, shivanshu.primaryColor)

        listOf(defaultSig, amit, shivanshu).forEach { sig ->
            assertTrue(sig.code.startsWith("CP-"))
            assertTrue(sig.waveCount in 2..4)
            assertTrue(sig.ringCount in 3..5)
            assertEquals(11, sig.spectrumBars.size)
            assertTrue(sig.spectrumBars.all { it in 0f..1f })
        }
    }

    @Test
    fun identityFileJsonRoundTripsAndRejectsForeignTags() {
        val sig = WaveIdentitySignature.fromName("ChordPlayer")
        val original = ChordPlayIdentityStore.ChordPlayIdentityFile(
            identityCode = sig.code,
            listenerName = "ChordPlayer",
            updatedAt = "2026-11-21T16:30:00Z",
            backup = Backup.BackupFile(
                versionName = "1.8.29",
                exportedAt = "2026-11-21T16:30:00Z",
                settings = mapOf(
                    "player_animation_speed" to Backup.PrefValue.of(1.5f)!!,
                    "amoled_dark_mode" to Backup.PrefValue.of(true)!!,
                ),
                listening = emptyList(),
            ),
        )

        val encoded = ChordPlayIdentityStore.encodeIdentity(original)
        val decoded = ChordPlayIdentityStore.decodeIdentity(encoded)
        assertNotNull(decoded)
        assertEquals(original.identityCode, decoded!!.identityCode)
        assertEquals(original.listenerName, decoded.listenerName)
        assertEquals(1.5f, decoded.backup.settings["player_animation_speed"]?.decoded())
        assertEquals(true, decoded.backup.settings["amoled_dark_mode"]?.decoded())

        val wrongApp = original.copy(app = "other_app")
        assertNull(ChordPlayIdentityStore.decodeIdentity(ChordPlayIdentityStore.encodeIdentity(wrongApp)))
    }

    @Test
    fun fileNameForNameSanitizesAndFallsBackCleanly() {
        assertEquals("you.chordplay.json", ChordPlayIdentityStore.fileNameForName("   "))
        assertEquals("amit-meena.chordplay.json", ChordPlayIdentityStore.fileNameForName("Amit Meena!"))
    }

    @Test
    fun waveIdentityPaletteDistinctChannelsYieldHarmoniousNonMonochromeTriad() {
        val sampleNames = listOf("Amit", "Shivanshu", "ChordPlay", "Nora", "Zed", "Maya")
        sampleNames.forEach { name ->
            val sig = WaveIdentitySignature.fromName(name)
            assertNotEquals(sig.primaryColor, sig.secondaryColor)
            assertNotEquals(sig.secondaryColor, sig.tertiaryColor)
        }
    }
}
