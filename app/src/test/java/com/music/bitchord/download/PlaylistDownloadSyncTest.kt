package com.music.bitchord.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistDownloadSyncTest {
    private val profile = PlaylistDownloadProfile("account", "profile")

    @Test
    fun refreshKeepsOrderAndOccurrencesAndOnlyRetiresMissingTracks() {
        val legacy = SavedCollection("VLPLtest", "Playlist", playlist = true, videoIds = listOf("a", "b", "a"))
        val refreshed = legacy.withSyncedTracks(profile, listOf("c", "a", "c"))
        assertEquals(listOf("c", "a", "c"), refreshed.videoIds)
        assertEquals(setOf("b"), refreshed.retiredVideoIds)
        assertEquals(profile, refreshed.syncProfile)
        assertEquals(setOf("c"), refreshed.withSyncedTracks(profile, listOf("a", "b")).retiredVideoIds)
        assertEquals(setOf("a", "b", "c"), refreshed.withSyncedTracks(profile, emptyList()).retiredVideoIds)
    }

    @Test
    fun pruningRequiresKnownOwnershipForEveryAliasAndNoRemainingCollection() {
        val aliases = setOf("one", "alias")
        val metadata = aliases.associateWith { id ->
            SavedSongMetadata(videoId = id, title = "Song", artist = "Artist", uri = "file:///song", playlistManaged = true)
        }
        assertTrue(canPrunePlaylistDownload(aliases, metadata, emptyList()))
        assertFalse(canPrunePlaylistDownload(aliases, metadata - "alias", emptyList()))
        assertFalse(canPrunePlaylistDownload(aliases, metadata + ("one" to metadata.getValue("one").copy(playlistManaged = false)), emptyList()))
        val album = SavedCollection("MPREalbum", "Album", videoIds = listOf("alias"))
        assertFalse(canPrunePlaylistDownload(aliases, metadata, listOf(album)))
    }
}
