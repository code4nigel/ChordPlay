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
package com.music.bitchord.playback

import com.music.bitchord.data.model.QueueTier
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.smart.AutomixEdit
import com.music.bitchord.playback.smart.AutomixStyle
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedQueueMixesTest {
    private val first = Song("abcdefghijk", "First", "Artist", null)
    private val second = Song("lmnopqrstuv", "Second", "Artist", null)
    private val profile = QueueMixProfile("account", "profile")
    private val playlist = "PL_queue"
    private val fade = AutomixEdit(overlapSeconds = 4.0)
    private val cut = AutomixEdit(style = AutomixStyle.OFF)

    private fun Song.row(id: String) = copy(setVideoId = id, playbackSourceId = "VL$playlist")

    @Test
    fun repeatedTracksBindToDifferentPairsAndSurviveSerialization() {
        val ordered = listOf(first, second, first, second)
        val upload = QueuePlaylistSnapshot(ordered, true, false, mapOf(0 to fade, 2 to cut)).upload
        val rows = ordered.mapIndexed { index, song -> song.row("row-$index") }
        val mix = checkNotNull(upload.verifiedMix(profile, playlist, rows))
        val restored = Json.decodeFromString<SavedQueueMix>(Json.encodeToString(mix))
        assertEquals(profile, restored.profile)
        assertEquals(fade, restored.editFor(rows[0], rows[1]))
        assertEquals(cut, restored.editFor(rows[2], rows[3]))
        assertNull(restored.editFor(rows[0], rows[3]))
        assertNull(restored.editFor(rows[0], rows[1].copy(queueTier = QueueTier.USER_QUEUE)))
        assertNull(restored.editFor(rows[0], rows[1].copy(videoId = first.videoId)))
    }

    @Test
    fun verificationRejectsAMissingRepeatOrAReorderedPlaylist() {
        val ordered = listOf(first, second, first)
        val upload = QueuePlaylistSnapshot(ordered, true, false, emptyMap()).upload
        val rows = ordered.mapIndexed { index, song -> song.row("row-$index") }
        assertNull(upload.verifiedMix(profile, playlist, rows.dropLast(1)))
        assertNull(upload.verifiedMix(profile, playlist, listOf(rows[0], rows[2], rows[1])))
        assertNull(upload.verifiedMix(profile, playlist, listOf(rows[0], rows[1], rows[0])))
    }

    @Test
    fun skippingAnUnsupportedTrackDoesNotReuseItsEditForTheNewJoin() {
        val local = Song("local:1", "Local song", "Artist", null)
        val snapshot = QueuePlaylistSnapshot(listOf(first, local, second, first), false, false, mapOf(0 to fade, 2 to cut))
        assertEquals(listOf(local), snapshot.skipped)
        val rows = listOf(first.row("one"), second.row("two"), first.row("three"))
        val mix = checkNotNull(snapshot.upload.verifiedMix(profile, playlist, rows))
        assertNull(mix.editFor(rows[0], rows[1]))
        assertEquals(cut, mix.editFor(rows[1], rows[2]))
    }

    @Test
    fun historySelectionFreezesTheQueueAndIncludesAvailableOccurrencesInOrder() {
        val past = List(12) { index -> if (index % 2 == 0) first else second }
        val queue = (past + listOf(second, first)).toMutableList()
        val capture = QueuePlaylistCapture(queue, 12, true, false, mapOf(11 to fade, 12 to cut))
        queue.clear()

        for ((range, count) in listOf(
            null to 0,
            QueueHistoryRange.LAST_3 to 3,
            QueueHistoryRange.LAST_5 to 5,
            QueueHistoryRange.LAST_10 to 10,
            QueueHistoryRange.ALL to 12,
        )) {
            val upload = capture.select(range).upload
            assertEquals(count, capture.historyCount(range))
            assertEquals((past.takeLast(count) + listOf(second, first)).map { it.videoId }, upload.videoIds)
            assertEquals(cut, upload.edits[count])
            if (count > 0) assertEquals(fade, upload.edits[count - 1])
        }
    }

    @Test
    fun shortHistoryClampsAndOnlySelectedHistoryReportsUnsupportedTracks() {
        val local = Song("local:1", "Local song", "Artist", null)
        val capture = QueuePlaylistCapture(listOf(local, first, second), 1, false, false, emptyMap())
        assertEquals(1, capture.historyCount(QueueHistoryRange.LAST_10))
        assertEquals(emptyList<Song>(), capture.select(null).skipped)
        assertEquals(listOf(local), capture.select(QueueHistoryRange.LAST_10).skipped)
    }
}
