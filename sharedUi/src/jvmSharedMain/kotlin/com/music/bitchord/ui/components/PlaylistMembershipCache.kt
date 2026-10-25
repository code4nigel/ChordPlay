package com.music.bitchord.ui.components

import com.music.bitchord.data.model.UserPlaylist
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache of playlist membership across sheets to eliminate redundant network hits,
 * track additions/removals, and badge songs present in playlists.
 */
object PlaylistMembershipCache {
    private val membership = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()
    private val playlistTitles = ConcurrentHashMap<String, String>()

    fun contains(playlistId: String, videoId: String): Boolean {
        val cleanId = playlistId.removePrefix("VL")
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
        membership.computeIfAbsent(cleanId) { ConcurrentHashMap() }[videoId] = setVideoId ?: ""
    }

    fun remove(playlistId: String, videoId: String) {
        val cleanId = playlistId.removePrefix("VL")
        membership[cleanId]?.remove(videoId)
        membership[playlistId]?.remove(videoId)
    }

    fun addAll(playlistId: String, entries: Map<String, String?>) {
        val cleanId = playlistId.removePrefix("VL")
        val map = membership.computeIfAbsent(cleanId) { ConcurrentHashMap() }
        entries.forEach { (videoId, setVideoId) ->
            map[videoId] = setVideoId ?: ""
        }
    }

    fun addAllIds(playlistId: String, videoIds: Collection<String>) {
        val cleanId = playlistId.removePrefix("VL")
        val map = membership.computeIfAbsent(cleanId) { ConcurrentHashMap() }
        videoIds.forEach { vid -> map[vid] = "" }
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
        membership.forEach { (playlistId, videos) ->
            if (videos.containsKey(videoId)) {
                val cleanId = playlistId.removePrefix("VL")
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
