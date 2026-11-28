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
package com.music.bitchord.ui

import com.music.bitchord.playback.QueueMixProfile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistPickerMembershipTest {
    private val firstProfile = QueueMixProfile("account", "first")
    private val secondProfile = QueueMixProfile("account", "second")

    @Test
    fun reopenSharesRefreshAndFailuresKeepKnownBadges() = runBlocking {
        var now = 0L
        var calls = 0
        val store = PlaylistPickerMembership(this, { firstProfile }, { now }) { _, _ ->
            calls++
            if (calls == 1) Result.success(mapOf("one" to true))
            else Result.failure(IllegalStateException("offline"))
        }
        val state = store.observe(firstProfile, "song")
        store.refresh(firstProfile, "song")
        store.refresh(firstProfile, "song")
        yield()
        assertEquals(1, calls)
        assertEquals(setOf("one"), state.value.present.keys)

        now = 59_999
        store.refresh(firstProfile, "song")
        yield()
        assertEquals(1, calls)
        now = 60_000
        store.refresh(firstProfile, "song")
        yield()
        assertEquals(2, calls)
        assertEquals(setOf("one"), state.value.present.keys)
        assertFalse(state.value.checking)
    }

    @Test
    fun oldLookupCannotUndoWriteOrPublishAcrossProfiles() = runBlocking {
        var active = firstProfile
        val remote = CompletableDeferred<Map<String, Boolean>>()
        val store = PlaylistPickerMembership(this, { active }) { _, _ -> Result.success(remote.await()) }
        val first = store.observe(firstProfile, "song")
        store.refresh(firstProfile, "song")
        yield()
        store.added(firstProfile, "song", "VLone", "destination-row")
        remote.complete(mapOf("one" to false))
        yield()
        assertEquals("destination-row", first.value.present["one"])
        assertFalse(first.value.checking)

        active = secondProfile
        store.profileChanged()
        val second = store.observe(secondProfile, "song")
        store.added(firstProfile, "song", "two")
        assertTrue(second.value.present.isEmpty())
        assertEquals(setOf("one"), first.value.present.keys)
    }

    @Test
    fun openedPlaylistSeedsTheBadgeWithoutRestoringARemovedRow() = runBlocking {
        val store = PlaylistPickerMembership(this, { firstProfile })
        val state = store.observe(firstProfile, "song")
        store.seedFromPlaylist(firstProfile, "song", "VLone", "row")
        assertEquals("row", state.value.present["one"])
        store.removed(firstProfile, "song", "one")
        store.seedFromPlaylist(firstProfile, "song", "VLone", "row")
        assertTrue(state.value.present.isEmpty())
    }
}
