package com.music.bitchord.data.settings

/**
 * Stable persisted ordering for a Library "Show all" grid — playlists or
 * albums. A card there only ever carries a title, so unlike `LocalMusicSort`
 * there is nothing date-based to offer.
 */
enum class LibrarySort(val label: String) {
    /** Whatever order the shelf itself arrived in — YouTube Music's own. */
    DEFAULT("Date Added"),
    TITLE_ASC("Title A-Z"),
    TITLE_DESC("Title Z-A"),
}

/** Display mode for music lists: compact rows or grid cards. */
enum class LibraryViewType {
    LIST,
    GRID,
}

/** Display mode for Library "Show all" page: grid cards or full-width list rows. */
enum class LibraryViewMode {
    GRID,
    LIST,
}

/** Layout mode for Home Speed Dial section. */
enum class SpeedDialLayoutMode(val label: String) {
    TWO_BY_THREE("2×3 Grid"),
    THREE_BY_THREE("3×3 Grid"),
    ONE_BY_FOUR_LIST("1×4 List");

    fun next(): SpeedDialLayoutMode = when (this) {
        TWO_BY_THREE -> THREE_BY_THREE
        THREE_BY_THREE -> ONE_BY_FOUR_LIST
        ONE_BY_FOUR_LIST -> TWO_BY_THREE
    }
}


enum class HomeShelfKey(val id: String, val label: String, val subtitle: String) {
    SPEED_DIAL("speed_dial", "Your Space", "Pinned favorites, mixes & high-frecency grid"),
    QUICK_PICKS("quick_picks", "Quick Picks", "4-row instant radio starter tracks"),
    TOP_PICKS("top_picks", "Top Picks for You", "Artist stations, mixes & spotlight cards"),
    SIMILAR_TO("similar_to", "Similar to Recent Listens", "Recommendations & radios based on recent tracks"),
    COMMUNITY_PLAYLISTS("community_playlists", "Community Playlists", "Trending playlists from the YouTube Music community"),
    FEATURED_PLAYLISTS("featured_playlists", "Featured Playlists", "Editorial spotlights & curated playlist carousels"),
    CHARTS("charts", "Charts & Trending", "Top tracks, global/regional charts & trending hits"),
    REMIXES("remixes", "Remixes & Edits", "Club mixes, VIP edits & reimagined tracks"),
    MUSIC_VIDEOS("music_videos", "Music Videos", "16:9 widescreen music videos & live sessions"),
    MIXED_FOR_YOU("mixed_for_you", "Mixed for You", "YouTube Music personalized mix carousels"),
    LISTEN_AGAIN("listen_again", "Listen Again & Favorites", "Forgotten favorites & heavy rotation shelves"),
    NEW_RELEASES("new_releases", "New Releases", "Latest albums, singles & EPs"),
    OTHER("other", "More Recommendations", "Genre spotlights, moods & other discoveries");

    companion object {
        val DEFAULT_ORDER: List<HomeShelfKey> = entries.toList()
    }
}
