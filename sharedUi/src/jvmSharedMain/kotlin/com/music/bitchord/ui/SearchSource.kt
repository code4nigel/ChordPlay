package com.music.bitchord.ui

/**
 * Split for the Search tab:
 * - [YOUTUBE]: Online YouTube Music catalog
 * - [LIBRARY]: User's saved library (saved playlists, albums, artists, tracks)
 * - [LOCAL]: Offline device storage (Local Music folder)
 * - [ADDONS]: Configured music source addons
 */
enum class SearchSource {
    YOUTUBE,
    LIBRARY,
    LOCAL,
    ADDONS,
}
