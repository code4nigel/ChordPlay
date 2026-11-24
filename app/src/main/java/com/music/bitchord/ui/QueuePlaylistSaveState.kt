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
