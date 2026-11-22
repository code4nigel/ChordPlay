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
package com.music.bitchord.data.backup

import com.music.bitchord.data.stats.Backup
import com.music.bitchord.data.stats.BucketMath
import com.music.bitchord.data.stats.DayStats
import com.music.bitchord.data.stats.NameEntry
import com.music.bitchord.data.stats.StoredBucket
import com.music.bitchord.data.stats.TrackEntry
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveBackupMathTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `bucket math plus and minus round-trip cleanly`() {
        val a = StoredBucket(
            month = "2026-11",
            tracks = listOf(
                TrackEntry(
                    id = "v1",
                    title = "Song 1",
                    artist = "Artist A",
                    ms = 120_000L,
                    plays = 2,
                    days = mutableMapOf("2026-11-10" to DayStats(ms = 120_000L, plays = 2)),
                ),
            ),
            artists = listOf(NameEntry(name = "Artist A", ms = 120_000L, plays = 2)),
            hours = List(24) { if (it == 20) 120_000L else 0L },
            days = mapOf(10 to 120_000L),
        )
        val b = StoredBucket(
            month = "2026-11",
            tracks = listOf(
                TrackEntry(
                    id = "v1",
                    title = "Song 1",
                    artist = "Artist A",
                    ms = 60_000L,
                    plays = 1,
                    days = mutableMapOf("2026-11-11" to DayStats(ms = 60_000L, plays = 1)),
                ),
            ),
            artists = listOf(NameEntry(name = "Artist A", ms = 60_000L, plays = 1)),
            hours = List(24) { if (it == 21) 60_000L else 0L },
            days = mapOf(11 to 60_000L),
        )

        val sum = BucketMath.plus(a, b)
        assertEquals(180_000L, sum.tracks.single().ms)
        assertEquals(3, sum.tracks.single().plays)
        assertEquals(120_000L, sum.hours[20])
        assertEquals(60_000L, sum.hours[21])

        val diff = BucketMath.minus(sum, a)
        assertEquals(60_000L, diff.tracks.single().ms)
        assertEquals(1, diff.tracks.single().plays)
        assertEquals(0L, diff.hours[20])
        assertEquals(60_000L, diff.hours[21])

        val zeroed = BucketMath.minus(a, a)
        assertTrue(BucketMath.isEmpty(zeroed))
    }

    @Test
    fun `device manifest preserves ChordPlay identity and settings`() {
        val manifest = DeviceManifest(
            deviceId = "dev-1",
            name = "Pixel 9",
            ownerInstall = "install-abc",
            updatedAt = 123456789L,
            versionName = "2.2.6",
            identityCode = "CP-A1B2",
            listenerName = "Amit",
            months = mapOf("2026-11" to "abc123"),
            settings = mapOf("liquid_glass" to Backup.PrefValue("bool", value = "true")),
        )
        val encoded = json.encodeToString(DeviceManifest.serializer(), manifest)
        val decoded = json.decodeFromString(DeviceManifest.serializer(), encoded)
        assertEquals("CP-A1B2", decoded.identityCode)
        assertEquals("Amit", decoded.listenerName)
        assertEquals("true", decoded.settings["liquid_glass"]?.value)
    }
}
