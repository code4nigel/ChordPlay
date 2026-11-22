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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordPlayPassPngTest {

    // Minimal valid 1x1 transparent PNG
    private val minimalPng = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
        0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4.toByte(), 0x89.toByte(),
        0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54,
        0x78, 0x9C.toByte(), 0x63, 0x00, 0x01, 0x00, 0x00, 0x05,
        0x00, 0x01, 0x0D, 0x0A, 0x2D, 0xB4.toByte(),
        0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
        0xAE.toByte(), 0x42, 0x60, 0x82.toByte(),
    )

    @Test
    fun `zTXt chunk inject and extract round-trips large identity json`() {
        val sampleJson = buildString {
            append("""{"app":"chordplay_identity","version":1,"identityCode":"CP-A1B2","listenerName":"Amit",""")
            append(""""settings":{"liquid_glass":{"type":"bool","value":"true"}}""")
            append(""","listening":[""")
            repeat(50) { idx ->
                if (idx > 0) append(',')
                append("""{"month":"2026-0${(idx % 9) + 1}","plays":${idx + 12}}""")
            }
            append("]}")
        }

        val embedded = ChordPlayPassPng.injectZtxtChunk(minimalPng, sampleJson)
        assertTrue(ChordPlayPassPng.isPng(embedded))

        // Re-injecting replaces cleanly without duplication
        val updatedJson = sampleJson.replace("CP-A1B2", "CP-FF00")
        val reEmbedded = ChordPlayPassPng.injectZtxtChunk(embedded, updatedJson)

        val extracted = ChordPlayPassPng.extractPayload(reEmbedded)
        assertEquals(updatedJson, extracted)
    }

    @Test
    fun `extractPayload returns null for stripped png or non-png bytes`() {
        assertTrue(ChordPlayPassPng.isPng(minimalPng))
        assertNull(ChordPlayPassPng.extractPayload(minimalPng))

        val plainJson = """{"app":"chordplay_identity"}""".toByteArray()
        assertFalse(ChordPlayPassPng.isPng(plainJson))
        assertNull(ChordPlayPassPng.extractPayload(plainJson))
    }
}
