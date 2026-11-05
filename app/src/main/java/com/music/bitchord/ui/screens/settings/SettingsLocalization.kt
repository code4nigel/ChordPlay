package com.music.bitchord.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppDesignStyle
import com.music.bitchord.data.settings.HomeShelfKey
import com.music.bitchord.data.settings.PlayerInterfaceStyle
import com.music.bitchord.data.settings.PlayerLayoutMode
import com.music.bitchord.data.settings.SongSwipeAction
import com.music.bitchord.ui.player.LyricsFontChoice
import com.music.bitchord.ui.player.LyricsSizeScale
import com.music.bitchord.ui.player.LyricsWeightChoice

@Composable
fun HomeShelfKey.localizedLabel(): String = when (this) {
    HomeShelfKey.SPEED_DIAL -> stringResource(R.string.shelf_speed_dial_label)
    HomeShelfKey.QUICK_PICKS -> stringResource(R.string.shelf_quick_picks_label)
    HomeShelfKey.TOP_PICKS -> stringResource(R.string.shelf_top_picks_label)
    HomeShelfKey.SIMILAR_TO -> stringResource(R.string.shelf_similar_to_label)
    HomeShelfKey.COMMUNITY_PLAYLISTS -> stringResource(R.string.shelf_community_playlists_label)
    HomeShelfKey.FEATURED_PLAYLISTS -> stringResource(R.string.shelf_featured_playlists_label)
    HomeShelfKey.CHARTS -> stringResource(R.string.shelf_charts_label)
    HomeShelfKey.REMIXES -> stringResource(R.string.shelf_remixes_label)
    HomeShelfKey.MUSIC_VIDEOS -> stringResource(R.string.shelf_music_videos_label)
    HomeShelfKey.MIXED_FOR_YOU -> stringResource(R.string.shelf_mixed_for_you_label)
    HomeShelfKey.LISTEN_AGAIN -> stringResource(R.string.shelf_listen_again_label)
    HomeShelfKey.NEW_RELEASES -> stringResource(R.string.shelf_new_releases_label)
    HomeShelfKey.OTHER -> stringResource(R.string.shelf_other_label)
}

@Composable
fun HomeShelfKey.localizedSubtitle(): String = when (this) {
    HomeShelfKey.SPEED_DIAL -> stringResource(R.string.shelf_speed_dial_desc)
    HomeShelfKey.QUICK_PICKS -> stringResource(R.string.shelf_quick_picks_desc)
    HomeShelfKey.TOP_PICKS -> stringResource(R.string.shelf_top_picks_desc)
    HomeShelfKey.SIMILAR_TO -> stringResource(R.string.shelf_similar_to_desc)
    HomeShelfKey.COMMUNITY_PLAYLISTS -> stringResource(R.string.shelf_community_playlists_desc)
    HomeShelfKey.FEATURED_PLAYLISTS -> stringResource(R.string.shelf_featured_playlists_desc)
    HomeShelfKey.CHARTS -> stringResource(R.string.shelf_charts_desc)
    HomeShelfKey.REMIXES -> stringResource(R.string.shelf_remixes_desc)
    HomeShelfKey.MUSIC_VIDEOS -> stringResource(R.string.shelf_music_videos_desc)
    HomeShelfKey.MIXED_FOR_YOU -> stringResource(R.string.shelf_mixed_for_you_desc)
    HomeShelfKey.LISTEN_AGAIN -> stringResource(R.string.shelf_listen_again_desc)
    HomeShelfKey.NEW_RELEASES -> stringResource(R.string.shelf_new_releases_desc)
    HomeShelfKey.OTHER -> stringResource(R.string.shelf_other_desc)
}

@Composable
fun AppDesignStyle.localizedLabel(): String = when (this) {
    AppDesignStyle.CHORDPLAY -> stringResource(R.string.design_style_chordplay)
    AppDesignStyle.CLASSIC_BITCHORD -> stringResource(R.string.design_style_classic)
}

@Composable
fun AppDesignStyle.localizedDetail(): String = when (this) {
    AppDesignStyle.CHORDPLAY -> stringResource(R.string.design_style_chordplay_desc)
    AppDesignStyle.CLASSIC_BITCHORD -> stringResource(R.string.design_style_classic_desc)
}

@Composable
fun PlayerInterfaceStyle.localizedLabel(): String = when (this) {
    PlayerInterfaceStyle.WAVEPLAY -> stringResource(R.string.player_interface_waveplay)
    PlayerInterfaceStyle.CHORDPLAY_OG -> stringResource(R.string.player_interface_og)
}

@Composable
fun PlayerInterfaceStyle.localizedDetail(): String = when (this) {
    PlayerInterfaceStyle.WAVEPLAY -> stringResource(R.string.player_interface_waveplay_desc)
    PlayerInterfaceStyle.CHORDPLAY_OG -> stringResource(R.string.player_interface_og_desc)
}

@Composable
fun PlayerLayoutMode.localizedLabel(): String = when (this) {
    PlayerLayoutMode.MODERN_AMBIENT -> stringResource(R.string.player_layout_modern)
    PlayerLayoutMode.VINYL_RECORD -> stringResource(R.string.player_layout_vinyl)
    PlayerLayoutMode.CLASSIC_CLEAN -> stringResource(R.string.player_layout_classic)
}

@Composable
fun PlayerLayoutMode.localizedDetail(): String = when (this) {
    PlayerLayoutMode.MODERN_AMBIENT -> stringResource(R.string.player_layout_modern_desc)
    PlayerLayoutMode.VINYL_RECORD -> stringResource(R.string.player_layout_vinyl_desc)
    PlayerLayoutMode.CLASSIC_CLEAN -> stringResource(R.string.player_layout_classic_desc)
}

@Composable
fun LyricsFontChoice.localizedLabel(): String = when (this) {
    LyricsFontChoice.DEFAULT -> stringResource(R.string.font_family_default)
    LyricsFontChoice.SERIF -> stringResource(R.string.font_family_serif)
    LyricsFontChoice.SANS_SERIF -> stringResource(R.string.font_family_sans_serif)
    LyricsFontChoice.MONOSPACE -> stringResource(R.string.font_family_monospace)
    LyricsFontChoice.CURSIVE -> stringResource(R.string.font_family_script)
    LyricsFontChoice.CUSTOM -> stringResource(R.string.custom_font_file)
    else -> this.label // Poppins, Parkinsans are typeface proper names
}

@Composable
fun LyricsSizeScale.localizedLabel(): String = when (this) {
    LyricsSizeScale.COMPACT -> stringResource(R.string.font_size_compact)
    LyricsSizeScale.STANDARD -> stringResource(R.string.font_size_standard)
    LyricsSizeScale.LARGE -> stringResource(R.string.font_size_large)
    LyricsSizeScale.EXTRA_LARGE -> stringResource(R.string.font_size_huge)
}

@Composable
fun LyricsWeightChoice.localizedLabel(): String = when (this) {
    LyricsWeightChoice.MEDIUM -> stringResource(R.string.font_weight_medium)
    LyricsWeightChoice.SEMI_BOLD -> stringResource(R.string.font_weight_semibold)
    LyricsWeightChoice.BOLD -> stringResource(R.string.font_weight_bold)
    LyricsWeightChoice.HEAVY -> stringResource(R.string.font_weight_heavy)
}

@Composable
fun SongSwipeAction.localizedLabel(): String = when (this) {
    SongSwipeAction.PLAY_NEXT -> stringResource(R.string.play_next)
    SongSwipeAction.ADD_TO_QUEUE -> stringResource(R.string.add_to_queue)
    SongSwipeAction.TOGGLE_LIKE -> stringResource(R.string.action_like_favorite)
    SongSwipeAction.DOWNLOAD -> stringResource(R.string.download)
    SongSwipeAction.REMOVE_FROM_PLAYLIST -> stringResource(R.string.action_remove_from_playlist)
    SongSwipeAction.OPEN_MENU -> stringResource(R.string.action_open_menu)
    SongSwipeAction.NONE -> stringResource(R.string.action_none)
}

