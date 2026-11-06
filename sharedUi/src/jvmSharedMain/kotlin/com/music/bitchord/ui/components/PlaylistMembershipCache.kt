package com.music.bitchord.ui.components

import com.music.bitchord.data.model.UserPlaylist
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache of playlist membership across sheets to eliminate redundant network hits,
 * track in-flight additions/removals against eventual consistency lag,
 * and badge songs present in playlists.
 */
object PlaylistMembershipCache {
    private val membership = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()
    private val playlistTitles = ConcurrentHashMap<String, String>()

    // Pending additions: (cleanPlaylistId -> Set<videoId>)
    private val pendingAdds = ConcurrentHashMap<String, MutableSet<String>>()
    // Pending removals: (cleanPlaylistId -> Set<videoId>)
    private val pendingRemoves = ConcurrentHashMap<String, MutableSet<String>>()

    fun contains(playlistId: String, videoId: String): Boolean {
        if (videoId.isEmpty()) return false
        val cleanId = playlistId.removePrefix("VL")
        if (pendingRemoves[cleanId]?.contains(videoId) == true || pendingRemoves[playlistId]?.contains(videoId) == true) {
            return false
        }
        if (pendingAdds[cleanId]?.contains(videoId) == true || pendingAdds[playlistId]?.contains(videoId) == true) {
            return true
        }
        return membership[cleanId]?.containsKey(videoId) == true ||
            membership[playlistId]?.containsKey(videoId) == true
    }

    fun getSetVideoId(playlistId: String, videoId: String): String? {
        val cleanId = playlistId.removePrefix("VL")
        return membership[cleanId]?.get(videoId)?.takeIf { it.isNotEmpty() }
            ?: membership[playlistId]?.get(videoId)?.takeIf { it.isNotEmpty() }
    }

    fun add(playlistId: String, videoId: String, setVideoId: String? = null) {
        val cleanId = playlistId.removePrefix("VL")
        pendingRemoves[cleanId]?.remove(videoId)
        pendingRemoves[playlistId]?.remove(videoId)
        membership.computeIfAbsent(cleanId) { ConcurrentHashMap() }[videoId] = setVideoId ?: ""
    }

    fun remove(playlistId: String, videoId: String) {
        val cleanId = playlistId.removePrefix("VL")
        pendingAdds[cleanId]?.remove(videoId)
        pendingAdds[playlistId]?.remove(videoId)
        membership[cleanId]?.remove(videoId)
        membership[playlistId]?.remove(videoId)
    }

    fun markPendingAdd(playlistId: String, videoId: String) {
        val cleanId = playlistId.removePrefix("VL")
        pendingRemoves[cleanId]?.remove(videoId)
        pendingRemoves[playlistId]?.remove(videoId)
        pendingAdds.computeIfAbsent(cleanId) { ConcurrentHashMap.newKeySet() }.add(videoId)
        add(cleanId, videoId)
    }

    fun clearPendingAdd(playlistId: String, videoId: String) {
        val cleanId = playlistId.removePrefix("VL")
        pendingAdds[cleanId]?.remove(videoId)
        pendingAdds[playlistId]?.remove(videoId)
    }

    fun markPendingRemove(playlistId: String, videoId: String) {
        val cleanId = playlistId.removePrefix("VL")
        pendingAdds[cleanId]?.remove(videoId)
        pendingAdds[playlistId]?.remove(videoId)
        pendingRemoves.computeIfAbsent(cleanId) { ConcurrentHashMap.newKeySet() }.add(videoId)
        membership[cleanId]?.remove(videoId)
        membership[playlistId]?.remove(videoId)
    }

    fun clearPendingRemove(playlistId: String, videoId: String) {
        val cleanId = playlistId.removePrefix("VL")
        pendingRemoves[cleanId]?.remove(videoId)
        pendingRemoves[playlistId]?.remove(videoId)
    }

    fun reconcileRemote(videoId: String, serverContainingPlaylistIds: Set<String>, knownPlaylists: List<UserPlaylist>) {
        if (videoId.isEmpty()) return
        val serverCleanIds = serverContainingPlaylistIds.map { it.removePrefix("VL") }.toSet()

        // For any playlist reported by the server, add it and resolve any pending add
        serverCleanIds.forEach { cleanId ->
            pendingAdds[cleanId]?.remove(videoId)
            add(cleanId, videoId)
        }

        // For all known playlists: if server didn't list it, only remove if NOT pending addition!
        knownPlaylists.forEach { pl ->
            val cleanId = pl.playlistId.removePrefix("VL")
            if (cleanId in serverCleanIds) {
                pendingAdds[cleanId]?.remove(videoId)
                add(cleanId, videoId)
            } else {
                // If there is an active pending add, don't wipe it! Eventual consistency is still catching up.
                if (pendingAdds[cleanId]?.contains(videoId) == true || pendingAdds[pl.playlistId]?.contains(videoId) == true) {
                    add(cleanId, videoId)
                } else {
                    membership[cleanId]?.remove(videoId)
                    membership[pl.playlistId]?.remove(videoId)
                }
            }

            // If the server confirms it's not there, clear pending remove
            if (cleanId !in serverCleanIds) {
                pendingRemoves[cleanId]?.remove(videoId)
            }
        }
    }

    fun addAll(playlistId: String, entries: Map<String, String?>) {
        val cleanId = playlistId.removePrefix("VL")
        val map = membership.computeIfAbsent(cleanId) { ConcurrentHashMap() }
        entries.forEach { (videoId, setVideoId) ->
            map[videoId] = setVideoId ?: ""
            pendingRemoves[cleanId]?.remove(videoId)
        }
    }

    fun addAllIds(playlistId: String, videoIds: Collection<String>) {
        val cleanId = playlistId.removePrefix("VL")
        val map = membership.computeIfAbsent(cleanId) { ConcurrentHashMap() }
        videoIds.forEach { vid ->
            map[vid] = ""
            pendingRemoves[cleanId]?.remove(vid)
        }
    }

    fun setPlaylistTitles(playlists: List<UserPlaylist>) {
        playlists.forEach { playlist ->
            val cleanId = playlist.playlistId.removePrefix("VL")
            playlistTitles[cleanId] = playlist.title
        }
    }

    /**
     * Returns the list of playlist titles containing the given [videoId].
     */
    fun getPlaylistNamesForSong(videoId: String, playlists: List<UserPlaylist>): List<String> {
        if (videoId.isEmpty()) return emptyList()
        if (playlists.isNotEmpty()) {
            setPlaylistTitles(playlists)
        }
        val matchingNames = mutableListOf<String>()
        val allIds = (membership.keys + pendingAdds.keys).toSet()
        allIds.forEach { cleanId ->
            if (contains(cleanId, videoId)) {
                val title = playlistTitles[cleanId]
                    ?: playlists.firstOrNull { it.playlistId.removePrefix("VL") == cleanId }?.title
                if (title != null && !matchingNames.contains(title)) {
                    matchingNames.add(title)
                }
            }
        }
        return matchingNames
    }
}
