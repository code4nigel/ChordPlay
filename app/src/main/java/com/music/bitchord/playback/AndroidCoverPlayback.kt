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

import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.player.CoverChange
import com.music.bitchord.ui.player.CoverTrackChange
import com.music.bitchord.ui.player.coverEntryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The playback service and its MediaSession callbacks publish on the main thread. */
internal object AndroidCoverPlayback {
    private data class Skip(val from: String, val to: String, val change: CoverChange, val time: Long)

    private val _change = MutableStateFlow<CoverTrackChange?>(null)
    val change = _change.asStateFlow()
    private var current: Song? = null
    private var skip: Skip? = null

    fun initialize(item: MediaItem?) {
        current = item?.toSong()
        skip = null
        _change.value = null
    }

    fun skipRequested(player: Player, direction: CoverChange) {
        val index = if (direction == CoverChange.NEXT) player.nextMediaItemIndex else player.previousMediaItemIndex
        val from = player.currentMediaItem?.entryKey()
        val to = if (index in 0 until player.mediaItemCount) player.getMediaItemAt(index).entryKey() else null
        skip = if (from != null && to != null && from != to) {
            Skip(from, to, direction, SystemClock.elapsedRealtime())
        } else null
    }

    fun seekWithinEntry(from: MediaItem?, to: MediaItem?) {
        if (from?.entryKey() == to?.entryKey()) skip = null
    }

    fun trackChanged(item: MediaItem?, reason: Int) {
        val next = item?.toSong()
        val previous = current
        current = next
        val requested = skip.also { skip = null }
        if (previous == null || next == null) {
            _change.value = null
            return
        }
        // Resolving or replacing the same MediaItem can report the entry again
        // while its cover is still moving. Keep the confirmed change in flight.
        if (previous.coverEntryKey() == next.coverEntryKey()) return

        val kind = when (reason) {
            Player.MEDIA_ITEM_TRANSITION_REASON_AUTO, Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> CoverChange.AUTOMATIC
            else -> requested
                ?.takeIf {
                    it.from == previous.coverEntryKey() && it.to == next.coverEntryKey() &&
                        SystemClock.elapsedRealtime() - it.time < 1000L
                }
                ?.change ?: CoverChange.SELECTION
        }
        _change.value = CoverTrackChange(previous, next, kind)
    }

    private fun MediaItem.entryKey() = queueEntryId ?: mediaId
}
