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
