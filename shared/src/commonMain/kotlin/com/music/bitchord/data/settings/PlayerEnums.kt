package com.music.bitchord.data.settings

/**
 * Stream bitrate ceiling on the YouTube fallback path — MEDIUM, HIGH and
 * LOSSLESS all mean "whatever the best available Opus format is" there; what
 * actually tells them apart is which other sources are allowed to answer
 * *before* YouTube gets asked. That part is [permits], and the rungs read:
 *
 * - [LOSSLESS] — the user's own addons and JioSaavn both asked.
 * - [HIGH] — the addons skipped, JioSaavn asked.
 * - [MEDIUM] and [LOW] — both skipped; YouTube's own Opus ladder is all there
 *   is, capped at [maxKbps].
 *
 * [hourly] is what the ceiling costs in data over an hour of listening, which
 * is the only part of this a user actually cares about on a metered plan.
 */
enum class AudioQuality(
    val maxKbps: Int,
    val label: String,
    val detail: String,
    val hourly: String,
) {
    LOW(64, "Low", "~64 kbps · smallest download", "29 MB/hr"),
    MEDIUM(Int.MAX_VALUE, "Medium", "Best available · ~171 kbps Opus", "77 MB/hr"),
    HIGH(Int.MAX_VALUE, "High", "JioSaavn up to 320kbps, YouTube fallback", "144 MB/hr"),
    LOSSLESS(Int.MAX_VALUE, "Lossless", "Your addons + JioSaavn, bit-exact where available", "300+ MB/hr"),
    ;
}

/** The surface that was last open inside the expanded player. */
enum class LastPlayerScreen {
    MAIN,
    LYRICS,
    QUEUE,
}

/** Global design style aesthetic: ChordPlay Modern is the unified design system. */
enum class AppDesignStyle(val label: String, val detail: String) {
    CHORDPLAY("ChordPlay Modern", "Atmospheric glowing glass, vibrant palettes, tactile controls, and dynamic discovery"),
}

/** Player screen visual layout and audio stage presentation. */
enum class PlayerLayoutMode(val label: String, val detail: String) {
    MODERN_AMBIENT("Modern Ambient", "Floating cover with breathing aura, frosted capsule deck, and quick actions"),
    VINYL_RECORD("Vinyl Turntable", "Spinning vinyl record sliding out from sleeve with moving tonearm"),
    CLASSIC_CLEAN("Classic BitChord", "Original Apple Music-style player layout"),
}

/** Player interface style: WavePlay is the unified player interface. */
enum class PlayerInterfaceStyle(val label: String, val detail: String) {
    WAVEPLAY("WavePlay", "Synesthesia ribbons, dynamic waveform seekbar, and 5-button glowing orb deck"),
}

/** Visual presentation style for one-line lyrics in fullscreen Canvas / artwork mode. */
enum class FullscreenArtworkLyricsStyle(val label: String) {
    FLOATING_CAPSULE("Floating Capsule (Above Title)"),
    PLAYER_STRIP("Player Strip (Below Title)"),
    OFF("Hidden"),
}

/** Configurable swipe action on track rows in playlist, library, and search. */
enum class SongSwipeAction(val label: String) {
    PLAY_NEXT("Play Next"),
    ADD_TO_QUEUE("Add to Queue"),
    TOGGLE_LIKE("Like / Favorite"),
    DOWNLOAD("Download"),
    REMOVE_FROM_PLAYLIST("Remove from Playlist"),
    OPEN_MENU("Open Menu"),
    NONE("None");
}

/** Video stream resolution tiers. */
enum class VideoQuality(val label: String, val badge: String, val maxHeight: Int) {
    Q_360P("360p SD", "SD", 360),
    Q_480P("480p SD", "SD", 480),
    Q_720P("720p HD", "HD", 720),
    Q_1080P("1080p FHD", "FHD", 1080),
}

/** Global typography font choice. */
enum class AppFontChoice(val label: String, val detail: String) {
    APPLE_SF_PRO("SF Pro Display", "Signature Apple Music clean grotesque typography"),
    POPPINS("Poppins", "Modern geometric sans with generous curves"),
    PARKINSANS("Parkinsans", "Playful grotesque with distinctive curves and character"),
    SYSTEM_DEFAULT("System Default", "Standard Android system sans-serif"),
}
