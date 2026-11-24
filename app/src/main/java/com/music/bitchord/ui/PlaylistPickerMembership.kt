package com.music.bitchord.ui

import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.playback.QueueMixProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal data class PlaylistPickerMembershipState(
    val present: Map<String, String?> = emptyMap(),
    val checking: Boolean = false,
)

/** Profile-scoped, in-memory membership for the single-song picker. */
internal class PlaylistPickerMembership(
    private val scope: CoroutineScope,
    private val activeProfile: () -> QueueMixProfile?,
    private val now: () -> Long = { System.nanoTime() / 1_000_000 },
    private val lookup: suspend (String, () -> Boolean) -> Result<Map<String, Boolean>> =
        YtMusicRepository::getPlaylistMembership,
) {
    private data class SongKey(val profile: QueueMixProfile, val songId: String)

    private class Entry {
        val state = MutableStateFlow(PlaylistPickerMembershipState())
        var refreshedAt: Long? = null
        var revision = 0L
        var job: Job? = null
        val recentWrites = mutableMapOf<String, Long>()
        val knownAbsent = mutableSetOf<String>()
    }

    private val entries = mutableMapOf<SongKey, Entry>()
    private val signedOut = MutableStateFlow(PlaylistPickerMembershipState())
    @Volatile private var profileGeneration = 0L

    fun profileChanged() {
        profileGeneration++
        entries.values.forEach {
            it.job?.cancel()
            it.refreshedAt = null
            it.state.value = it.state.value.copy(checking = false)
        }
    }

    fun observe(profile: QueueMixProfile?, songId: String): StateFlow<PlaylistPickerMembershipState> =
        if (profile == null) signedOut else entry(profile, songId).state

    fun refresh(profile: QueueMixProfile?, songId: String) {
        if (profile == null || activeProfile() != profile) return
        val entry = entry(profile, songId)
        if (entry.state.value.checking || entry.refreshedAt?.let { now() - it < FRESH_MS } == true) return
        val revision = entry.revision
        val generation = profileGeneration
        val current = { profileGeneration == generation && activeProfile() == profile }
        entry.state.value = entry.state.value.copy(checking = true)
        entry.job = scope.launch {
            try {
                val resolved = lookup(songId, current).getOrNull()
                if (resolved == null || !current() || entry.revision != revision) return@launch
                val present = entry.state.value.present.toMutableMap()
                val timestamp = now()
                entry.recentWrites.values.removeAll { timestamp - it >= FRESH_MS }
                resolved.forEach { (id, contains) ->
                    if (id in entry.recentWrites && contains != present.containsKey(id)) return@forEach
                    entry.recentWrites.remove(id)
                    if (contains) {
                        entry.knownAbsent.remove(id)
                        present.putIfAbsent(id, null)
                    } else {
                        entry.knownAbsent.add(id)
                        present.remove(id)
                    }
                }
                entry.refreshedAt = timestamp
                entry.state.value = entry.state.value.copy(present = present)
            } finally {
                if (profileGeneration == generation) entry.state.value = entry.state.value.copy(checking = false)
            }
        }
    }

    fun added(profile: QueueMixProfile?, songId: String, playlistId: String, rowId: String? = null) {
        if (profile == null || activeProfile() != profile) return
        val entry = entry(profile, songId)
        val id = playlistId.removePrefix("VL")
        entry.revision++
        entry.recentWrites[id] = now()
        entry.knownAbsent.remove(id)
        entry.state.value = entry.state.value.copy(
            present = entry.state.value.present + (id to (rowId ?: entry.state.value.present[id])),
        )
    }

    fun removed(profile: QueueMixProfile?, songId: String, playlistId: String) {
        if (profile == null || activeProfile() != profile) return
        val entry = entry(profile, songId)
        val id = playlistId.removePrefix("VL")
        entry.revision++
        entry.recentWrites[id] = now()
        entry.knownAbsent.add(id)
        entry.state.value = entry.state.value.copy(present = entry.state.value.present - id)
    }

    fun rowId(profile: QueueMixProfile?, songId: String, playlistId: String): String? {
        if (profile == null) return null
        return entries[SongKey(profile, songId)]?.state?.value?.present?.get(playlistId.removePrefix("VL"))
    }

    /** An opened destination's own rows can establish presence before the picker lookup. */
    fun seedFromPlaylist(profile: QueueMixProfile, songId: String, playlistId: String, rowId: String) {
        if (activeProfile() != profile) return
        val entry = entry(profile, songId)
        val id = playlistId.removePrefix("VL")
        val state = entry.state.value
        if (id !in entry.knownAbsent && state.present[id] == null) {
            entry.state.value = state.copy(present = state.present + (id to rowId))
        }
    }

    private fun entry(profile: QueueMixProfile, songId: String): Entry =
        entries.getOrPut(SongKey(profile, songId), ::Entry)

    private companion object { const val FRESH_MS = 60_000L }
}
