package com.music.bitchord.ui

/**
 * Three-way split for the Search tab:
 * - [YOUTUBE]: Online YouTube Music catalog
 * - [LIBRARY]: User's saved library (saved playlists, albums, artists, tracks)
 * - [LOCAL]: Offline device storage (Local Music folder)
 */
enum class SearchSource {
    YOUTUBE,
    LIBRARY,
    LOCAL,
}
