package com.music.bitchord

import androidx.media3.common.Player
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.playback.PlaybackService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsOffsetResetTest {

    @After
    fun resetOffset() {
        AppSettings.setLyricsOffsetMs(0)
    }

    @Test
    fun `changing to next song resets lyrics offset`() {
        assertTrue(
            PlaybackService.shouldResetLyricsOffset(
                previousSongId = "songA",
                nextSongId = "songB",
                previousEnded = true,
                reason = Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
            ),
        )
        assertTrue(
            PlaybackService.shouldResetLyricsOffset(
                previousSongId = "songA",
                nextSongId = "songB",
                previousEnded = false,
                reason = Player.MEDIA_ITEM_TRANSITION_REASON_SEEK,
            ),
        )
    }

    @Test
    fun `same song retry or repeat keeps lyrics offset`() {
        assertFalse(
            PlaybackService.shouldResetLyricsOffset(
                previousSongId = "songA",
                nextSongId = "songA",
                previousEnded = false,
                reason = Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED,
            ),
        )
        assertFalse(
            PlaybackService.shouldResetLyricsOffset(
                previousSongId = "songA",
                nextSongId = "songA",
                previousEnded = true,
                reason = Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT,
            ),
        )
    }

    @Test
    fun `consecutive identical song finishing auto-advances and resets`() {
        assertTrue(
            PlaybackService.shouldResetLyricsOffset(
                previousSongId = "songA",
                nextSongId = "songA",
                previousEnded = true,
                reason = Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
            ),
        )
    }

    @Test
    fun `setLyricsOffsetMs clamps and resets in memory`() {
        AppSettings.setLyricsOffsetMs(1_500)
        assertEquals(1_500, AppSettings.lyricsOffsetMs.value)

        AppSettings.setLyricsOffsetMs(99_000)
        assertEquals(AppSettings.MAX_LYRICS_OFFSET_MS, AppSettings.lyricsOffsetMs.value)

        AppSettings.setLyricsOffsetMs(0)
        assertEquals(0, AppSettings.lyricsOffsetMs.value)
    }
}
