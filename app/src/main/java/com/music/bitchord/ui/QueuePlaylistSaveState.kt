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

import com.music.bitchord.playback.PendingQueuePlaylist
import com.music.bitchord.playback.QueueMixProfile
import com.music.bitchord.playback.QueuePlaylistCapture

internal sealed interface QueuePlaylistSaveState {
    data object SignIn : QueuePlaylistSaveState
    data class Form(val profile: QueueMixProfile, val capture: QueuePlaylistCapture) : QueuePlaylistSaveState
    data object Creating : QueuePlaylistSaveState
    data object CreateUnconfirmed : QueuePlaylistSaveState
    data object ProfileChanged : QueuePlaylistSaveState
    data class Receipt(val pending: PendingQueuePlaylist, val status: Status) : QueuePlaylistSaveState

    enum class Status { VERIFYING, VERIFIED, MISMATCH, VERIFY_FAILED, MIX_SAVE_FAILED }
}
