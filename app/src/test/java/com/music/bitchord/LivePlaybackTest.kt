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
package com.music.bitchord

import androidx.media3.common.MimeTypes
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.VideoQuality
import com.music.bitchord.data.sources.TrackMatcher
import com.music.bitchord.playback.AudioCache
import com.music.bitchord.playback.playbackMimeType
import com.music.bitchord.playback.playbackUriString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LivePlaybackTest {

    @Test
    fun audioCacheBypassesLiveHlsLocalAndDirectHttpUris() {
        assertTrue(AudioCache.shouldBypassCacheUrl("bitchord://watch?v=abc&m=1&live=1"))
        assertTrue(AudioCache.shouldBypassCacheUrl("https://manifest.googlevideo.com/api/manifest/hls_variant/playlist.m3u8"))
        assertTrue(AudioCache.shouldBypassCacheUrl("bitchord://watch?v=abc&manifest_reopen=hls"))
        assertTrue(AudioCache.shouldBypassCacheUrl("file:///storage/song.flac"))
        assertTrue(AudioCache.shouldBypassCacheUrl("content://media/external/audio/1"))
        assertFalse(AudioCache.shouldBypassCacheUrl("bitchord://watch?v=abc&n=Test&a=Artist"))
    }

    @Test
    fun liveSongBuildsM3u8PlaybackUriIgnoringOfflineCopy() {
        val liveSong = Song(
            videoId = "liveMJ123",
            title = "Michael Jackson: The Station",
            artist = "Michael Jackson",
            thumbnailUrl = null,
            isVideo = true,
            isLive = true,
            liveViewersText = "4.2K watching",
        )

        val uriWithOffline = liveSong.playbackUriString(offlineUri = "file:///storage/emulated/0/offline.m4a")
        assertTrue(uriWithOffline.startsWith("bitchord://watch?v=liveMJ123"))
        assertTrue("&m=1" in uriWithOffline)
        assertTrue("&live=1" in uriWithOffline)
        assertEquals(MimeTypes.APPLICATION_M3U8, liveSong.playbackMimeType())
        assertEquals(MimeTypes.APPLICATION_M3U8, liveSong.playbackMimeType(offlineUri = "file:///storage/emulated/0/offline.m4a"))

        val vodSong = Song(
            videoId = "vodSong1",
            title = "Billie Jean",
            artist = "Michael Jackson",
            thumbnailUrl = null,
        )
        val vodUri = vodSong.playbackUriString()
        assertFalse("&live=1" in vodUri)
        assertNull(vodSong.playbackMimeType())
    }

    @Test
    fun trackMatcherTargetPreservesIsLiveFlag() {
        val liveSong = Song(
            videoId = "liveStation1",
            title = "24/7 Chill Radio",
            artist = "Lofi Girl",
            thumbnailUrl = null,
            isLive = true,
        )
        val vodSong = Song(
            videoId = "vodTrack1",
            title = "Normal Song",
            artist = "Artist",
            thumbnailUrl = null,
            isLive = false,
        )

        assertTrue(TrackMatcher.targetOf(liveSong).isLive)
        assertFalse(TrackMatcher.targetOf(vodSong).isLive)
    }

    @Test
    fun videoQualityIncludesAutoDefaultTier() {
        assertEquals(VideoQuality.AUTO, VideoQuality.entries.first())
        assertTrue(VideoQuality.AUTO.isAuto)
        assertEquals(
            listOf(
                VideoQuality.AUTO,
                VideoQuality.Q_360P,
                VideoQuality.Q_480P,
                VideoQuality.Q_720P,
                VideoQuality.Q_1080P,
            ),
            VideoQuality.entries,
        )
    }
}
