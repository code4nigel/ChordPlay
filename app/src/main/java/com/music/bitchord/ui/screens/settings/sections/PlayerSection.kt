package com.music.bitchord.ui.screens.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.PlayerLayoutMode
import com.music.bitchord.data.settings.LandscapeFontScale
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.player.fullBleedArtworkAvailable
import com.music.bitchord.ui.screens.*
import com.music.bitchord.ui.screens.settings.localizedLabel
import com.music.bitchord.ui.screens.settings.localizedDetail
import com.music.bitchord.ui.screens.settings.previews.PlayerLivePreview

/**
 * Player & Visuals customization sub-page with a pinned live interactive preview card.
 */
@Composable
fun PlayerSection(
    windowWidth: Dp,
    onSpotifyCanvasAuth: () -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    initialSearchQuery: String = "",
    modifier: Modifier = Modifier,
) {
    val playerLayoutMode by AppSettings.playerLayoutMode.collectAsStateWithLifecycle()
    val enableBreathingAura by AppSettings.enableBreathingAura.collectAsStateWithLifecycle()
    val fullBleedArtwork by AppSettings.fullBleedArtwork.collectAsStateWithLifecycle()
    val animatedCanvas by AppSettings.animatedCanvas.collectAsStateWithLifecycle()
    val canvasOverCellular by AppSettings.canvasOverCellular.collectAsStateWithLifecycle()
    val legacyMeshGradient by AppSettings.legacyMeshGradient.collectAsStateWithLifecycle()
    val hideSongStatus by AppSettings.hideSongStatus.collectAsStateWithLifecycle()
    val swipeToPlayNext by AppSettings.swipeToPlayNext.collectAsStateWithLifecycle()
    val stopOnTaskRemoved by AppSettings.stopOnTaskRemoved.collectAsStateWithLifecycle()
    val smartVersionAlignment by AppSettings.smartVersionAlignment.collectAsStateWithLifecycle()
    val nerdStats by AppSettings.showNerdStats.collectAsStateWithLifecycle()
    val landscapeFontScale by AppSettings.landscapeFontScale.collectAsStateWithLifecycle()

    var searchQuery by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.playback),
            onBack = onBack,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
        )

        val showPlayerLayout = matchesSearch(
            searchQuery,
            stringResource(R.string.player_layout),
            playerLayoutMode.localizedDetail(),
            "layout", "compact", "normal",
        )
        val showBreathingAura = matchesSearch(
            searchQuery,
            stringResource(R.string.breathing_artwork_aura),
            stringResource(R.string.breathing_artwork_aura_desc),
            "aura", "breathing", "glow",
        )
        val showFullScreenCoverArt = fullBleedArtworkAvailable(windowWidth) && matchesSearch(
            searchQuery,
            stringResource(R.string.full_screen_cover_art),
            stringResource(R.string.full_screen_cover_art_subtitle),
            "full bleed", "full screen", "artwork",
        )
        val showLegacyMeshGradient = matchesSearch(
            searchQuery,
            stringResource(R.string.legacy_mesh_gradient),
            stringResource(R.string.legacy_mesh_gradient_subtitle),
            "mesh", "gradient", "backdrop",
        )
        val showAnimatedCoverArt = matchesSearch(
            searchQuery,
            stringResource(R.string.animated_cover_art),
            stringResource(R.string.animated_cover_art_subtitle),
            stringResource(R.string.animated_cover_cellular),
            stringResource(R.string.integrate_spotify_canvas),
            "canvas", "spotify", "video", "loop", "cellular",
        )
        val showHideSongStatus = matchesSearch(
            searchQuery,
            stringResource(R.string.hide_song_status),
            stringResource(R.string.hide_song_status_subtitle),
            "song status", "playing from", "played by",
        )
        val showPlayNextOnSwipe = matchesSearch(
            searchQuery,
            stringResource(R.string.play_next_on_swipe),
            stringResource(R.string.swipe_plays_next),
            stringResource(R.string.swipe_adds_to_queue),
            "swipe", "queue", "next",
        )
        val showStopMusicOnClose = matchesSearch(
            searchQuery,
            stringResource(R.string.stop_music_on_close),
            stringResource(R.string.stop_music_on_close_subtitle),
            "stop", "recents", "close", "task",
        )
        val showSmartAlignment = matchesSearch(
            searchQuery,
            stringResource(R.string.smart_version_alignment),
            stringResource(R.string.smart_version_alignment_subtitle),
            "smart", "audio", "alignment", "waveform",
        )
        val showNerdStats = matchesSearch(
            searchQuery,
            stringResource(R.string.show_nerd_stats),
            stringResource(R.string.show_nerd_stats_subtitle),
            "nerd", "stats", "codec", "bitrate",
        )
        val showLandscapeFontScale = matchesSearch(
            searchQuery,
            stringResource(R.string.landscape_font_size),
            landscapeFontScale.detail,
            "landscape", "car", "in-car", "font", "size", "auto", "text", "auto",
        )

        val anyPlayerMatch = showPlayerLayout || showBreathingAura ||
            showFullScreenCoverArt || showLegacyMeshGradient || showAnimatedCoverArt ||
            showHideSongStatus || showPlayNextOnSwipe || showStopMusicOnClose ||
            showLandscapeFontScale || showSmartAlignment || showNerdStats

        // Pinned Live Preview (hidden during active search)
        if (searchQuery.isBlank()) {
            PlayerLivePreview()
        }

        if (searchQuery.isNotBlank() && !anyPlayerMatch) {
            SettingsSearchEmpty(query = searchQuery)
        } else {
            // Single Unified Playback Settings Block
            SettingsGroup(header = null) {
                if (showPlayerLayout) {
                    SettingsRow(
                        icon = Icons.Rounded.GraphicEq,
                        title = stringResource(R.string.player_layout),
                        subtitle = playerLayoutMode.localizedDetail(),
                    )
                    SegmentedControl(
                        options = PlayerLayoutMode.entries.map { it.localizedLabel() },
                        selectedIndex = PlayerLayoutMode.entries.indexOf(playerLayoutMode),
                        onSelect = { AppSettings.setPlayerLayoutMode(PlayerLayoutMode.entries[it]) },
                        modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
                    )
                }

                if (showBreathingAura) {
                    SettingsRow(
                        icon = Icons.Rounded.Animation,
                        title = stringResource(R.string.breathing_artwork_aura),
                        subtitle = stringResource(R.string.breathing_artwork_aura_desc),
                        trailing = {
                            Switch(
                                checked = enableBreathingAura,
                                onCheckedChange = AppSettings::setEnableBreathingAura,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        },
                        onClick = { AppSettings.setEnableBreathingAura(!enableBreathingAura) },
                    )
                }

                // Artwork & Video Visuals Section Divider
                val hasTopVisuals = showPlayerLayout || showBreathingAura
                val hasArtworkVisuals = showFullScreenCoverArt || showLegacyMeshGradient || showAnimatedCoverArt
                if (hasArtworkVisuals) {
                    if (hasTopVisuals) {
                        SettingsSectionDivider(title = stringResource(R.string.artwork_and_visuals))
                    }

                    if (showFullScreenCoverArt) {
                        val fullScreenCoverArtTitle = stringResource(R.string.full_screen_cover_art)
                        SettingsRow(
                            icon = Icons.Rounded.Fullscreen,
                            title = fullScreenCoverArtTitle,
                            subtitle = stringResource(R.string.full_screen_cover_art_subtitle),
                            trailing = {
                                Switch(
                                    checked = fullBleedArtwork,
                                    onCheckedChange = AppSettings::setFullBleedArtwork,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setFullBleedArtwork(!fullBleedArtwork) },
                        )
                    }

                    if (showLegacyMeshGradient) {
                        val legacyMeshGradientTitle = stringResource(R.string.legacy_mesh_gradient)
                        SettingsRow(
                            icon = Icons.Rounded.Gradient,
                            title = legacyMeshGradientTitle,
                            subtitle = stringResource(R.string.legacy_mesh_gradient_subtitle),
                            trailing = {
                                Switch(
                                    checked = legacyMeshGradient,
                                    onCheckedChange = AppSettings::setLegacyMeshGradient,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setLegacyMeshGradient(!legacyMeshGradient) },
                        )
                    }

                    if (showAnimatedCoverArt) {
                        val animatedCoverArtTitle = stringResource(R.string.animated_cover_art)
                        SettingsRow(
                            icon = Icons.Rounded.Animation,
                            title = animatedCoverArtTitle,
                            subtitle = stringResource(R.string.animated_cover_art_subtitle),
                            trailing = {
                                Switch(
                                    checked = animatedCanvas,
                                    onCheckedChange = AppSettings::setAnimatedCanvas,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setAnimatedCanvas(!animatedCanvas) },
                        )

                        if (animatedCanvas) {
                            val coverCellularTitle = stringResource(R.string.animated_cover_cellular)
                            SettingsSubRow(
                                title = coverCellularTitle,
                                checked = canvasOverCellular,
                                onCheckedChange = AppSettings::setCanvasOverCellular,
                            )

                            val spotifyCanvasTitle = stringResource(R.string.integrate_spotify_canvas)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onSpotifyCanvasAuth)
                                    .padding(start = ROW_INSET, end = ROW_INSET, top = 4.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = spotifyCanvasTitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Chevron()
                            }
                        }
                    }
                }

                // Preferences Section Divider
                val hasPreferences = showHideSongStatus || showPlayNextOnSwipe || showStopMusicOnClose
                if (hasPreferences) {
                    if (hasTopVisuals || hasArtworkVisuals) {
                        SettingsSectionDivider(title = stringResource(R.string.preferences_header))
                    }

                    if (showHideSongStatus) {
                        val hideSongStatusTitle = stringResource(R.string.hide_song_status)
                        SettingsRow(
                            icon = Icons.Rounded.VisibilityOff,
                            title = hideSongStatusTitle,
                            subtitle = stringResource(R.string.hide_song_status_subtitle),
                            trailing = {
                                Switch(
                                    checked = hideSongStatus,
                                    onCheckedChange = AppSettings::setHideSongStatus,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setHideSongStatus(!hideSongStatus) },
                        )
                    }

                    if (showPlayNextOnSwipe) {
                        val playNextOnSwipeTitle = stringResource(R.string.play_next_on_swipe)
                        SettingsRow(
                            icon = Icons.Rounded.PlaylistPlay,
                            title = playNextOnSwipeTitle,
                            subtitle = if (swipeToPlayNext) {
                                stringResource(R.string.swipe_plays_next)
                            } else {
                                stringResource(R.string.swipe_adds_to_queue)
                            },
                            trailing = {
                                Switch(
                                    checked = swipeToPlayNext,
                                    onCheckedChange = AppSettings::setSwipeToPlayNext,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setSwipeToPlayNext(!swipeToPlayNext) },
                        )
                    }

                    if (showStopMusicOnClose) {
                        val stopMusicOnCloseTitle = stringResource(R.string.stop_music_on_close)
                        SettingsRow(
                            icon = Icons.Rounded.MusicOff,
                            title = stopMusicOnCloseTitle,
                            subtitle = stringResource(R.string.stop_music_on_close_subtitle),
                            trailing = {
                                Switch(
                                    checked = stopOnTaskRemoved,
                                    onCheckedChange = AppSettings::setStopOnTaskRemoved,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setStopOnTaskRemoved(!stopOnTaskRemoved) },
                        )
                    }
                }

                // Landscape & In-Car Display Section
                if (showLandscapeFontScale) {
                    if (hasTopVisuals || hasArtworkVisuals || hasPreferences) {
                        SettingsSectionDivider(title = stringResource(R.string.landscape_display_header))
                    }
                    SettingsRow(
                        icon = Icons.Rounded.FormatSize,
                        title = stringResource(R.string.landscape_font_size),
                        subtitle = landscapeFontScale.detail,
                    )
                    SegmentedControl(
                        options = LandscapeFontScale.entries.map { it.label },
                        selectedIndex = LandscapeFontScale.entries.indexOf(landscapeFontScale),
                        onSelect = { AppSettings.setLandscapeFontScale(LandscapeFontScale.entries[it]) },
                        modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
                    )
                }

                // Advanced & Stats Section Divider
                val hasAdvanced = showSmartAlignment || showNerdStats
                if (hasAdvanced) {
                    if (hasTopVisuals || hasArtworkVisuals || hasPreferences) {
                        SettingsSectionDivider(title = stringResource(R.string.advanced_options))
                    }

                    if (showSmartAlignment) {
                        val smartAlignmentTitle = stringResource(R.string.smart_version_alignment)
                        SettingsRow(
                            icon = Icons.Rounded.GraphicEq,
                            title = smartAlignmentTitle,
                            subtitle = stringResource(R.string.smart_version_alignment_subtitle),
                            trailing = {
                                Switch(
                                    checked = smartVersionAlignment,
                                    onCheckedChange = AppSettings::setSmartVersionAlignment,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setSmartVersionAlignment(!smartVersionAlignment) },
                        )
                    }

                    if (showNerdStats) {
                        val showNerdStatsTitle = stringResource(R.string.show_nerd_stats)
                        SettingsRow(
                            icon = Icons.Rounded.BarChart,
                            title = showNerdStatsTitle,
                            subtitle = stringResource(R.string.show_nerd_stats_subtitle),
                            trailing = {
                                Switch(
                                    checked = nerdStats,
                                    onCheckedChange = AppSettings::setShowNerdStats,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setShowNerdStats(!nerdStats) },
                        )
                    }
                }
            }
        }
    }
}
