/*
 * Copyright (C) 2026 Shivanshu (code4nigel) <https://github.com/code4nigel/ChordPlay>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.music.bitchord.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.music.bitchord.data.canvas.CanvasArtwork
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.data.lyrics.LyricsSource
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.KeyedMix
import com.music.bitchord.data.model.LiveStationCategory
import com.music.bitchord.data.model.MoodGenre
import com.music.bitchord.data.model.MoodGenreSection
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.settings.AppDesignStyle
import com.music.bitchord.data.settings.AudioQuality
import com.music.bitchord.data.settings.FullscreenArtworkLyricsStyle
import com.music.bitchord.data.settings.LastPlayerScreen
import com.music.bitchord.data.settings.LibrarySort
import com.music.bitchord.data.settings.LibraryViewType
import com.music.bitchord.data.settings.MixBlend
import com.music.bitchord.data.settings.PlayerLayoutMode
import com.music.bitchord.data.settings.SmartAnalysis
import com.music.bitchord.data.settings.TransitionWindow
import com.music.bitchord.ui.AppUi
import com.music.bitchord.ui.AppUiHost
import com.music.bitchord.ui.components.AudioPopupCard
import com.music.bitchord.ui.components.GitHubIconBadge
import com.music.bitchord.ui.components.LocalAppHazeState
import com.music.bitchord.ui.components.RedditIconBadge
import com.music.bitchord.ui.onboarding.ChordPlayIdentityCard
import com.music.bitchord.ui.player.AudioOutputDevice
import com.music.bitchord.ui.player.CanvasVideoSpec
import com.music.bitchord.ui.player.LyricsRomanizationResult
import com.music.bitchord.ui.player.LyricsTranslationResult
import com.music.bitchord.ui.player.OutputFormatUi
import com.music.bitchord.ui.player.PartyUi
import com.music.bitchord.ui.player.PlayerHost
import com.music.bitchord.ui.player.PlayerPlatform
import com.music.bitchord.ui.player.PlayerSettingsSource
import com.music.bitchord.ui.player.ChordPlayPipelineBottomPill
import com.music.bitchord.ui.player.SystemVolume
import com.music.bitchord.ui.screens.HomeScreen
import com.music.bitchord.ui.screens.RadioScreen
import com.music.bitchord.ui.screens.ReplayYearBadge
import com.music.bitchord.ui.screens.settings.sections.ShelvesSection
import com.music.bitchord.ui.theme.BitChordTheme
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private object PreviewAppUiHost : AppUiHost {
    override val appDesignStyle: StateFlow<AppDesignStyle> = MutableStateFlow(AppDesignStyle.CHORDPLAY)
    override val isDynamicTheme: StateFlow<Boolean> = MutableStateFlow(true)
    override val swipeToPlayNext: StateFlow<Boolean> = MutableStateFlow(true)
    override val homeRecentsViewType: StateFlow<LibraryViewType> = MutableStateFlow(LibraryViewType.LIST)
    override fun setHomeRecentsViewType(value: LibraryViewType) = Unit
    override val pinnedPlaylists: StateFlow<List<String>> = MutableStateFlow(emptyList())
    override val librarySort: StateFlow<LibrarySort> = MutableStateFlow(LibrarySort.DEFAULT)
    @Composable
    override fun downloadedIds(): Set<String> = emptySet()
}

private object PreviewPlayerSettings : PlayerSettingsSource {
    override val animatedCanvas = MutableStateFlow(true)
    override val audioQualityCellular = MutableStateFlow(AudioQuality.HIGH)
    override val audioQualityWifi = MutableStateFlow(AudioQuality.LOSSLESS)
    override val canvasOverCellular = MutableStateFlow(true)
    override val fullBleedArtwork = MutableStateFlow(false)
    override val hideSongStatus = MutableStateFlow(false)
    override val hideVolumeBar = MutableStateFlow(false)
    override val lastPlayerScreen = MutableStateFlow(LastPlayerScreen.MAIN)
    override val legacyMeshGradient = MutableStateFlow(false)
    override val lyricsBlur = MutableStateFlow(true)
    override val lyricsOffsetMs = MutableStateFlow(0)
    override val lyricsSourceOrder = MutableStateFlow(LyricsSource.offered)
    override val meteredConnection = MutableStateFlow<Boolean?>(null)
    override val preferUsbDac = MutableStateFlow(false)
    override val prioritizeSpotifyCanvas = MutableStateFlow(true)
    override val reduceAnimation = MutableStateFlow(false)
    override val reduceDynamicBlur = MutableStateFlow(false)
    override val showNerdStats = MutableStateFlow(false)
    override val smartAnalysis = MutableStateFlow(SmartAnalysis())
    override val smartFadeEnabled = MutableStateFlow(false)
    override val smartMixInProgress = MutableStateFlow(false)
    override val smartMixBlend = MutableStateFlow<MixBlend?>(null)
    override val smartTransitionWindow = MutableStateFlow<TransitionWindow?>(null)
    override val spotifyCanvasAutoHide = MutableStateFlow(false)
    override val syncedLyrics = MutableStateFlow(true)
    override val translationLanguage = MutableStateFlow("")
    override val versionAlignmentInProgress = MutableStateFlow(false)
    override val appDesignStyle = MutableStateFlow(AppDesignStyle.CHORDPLAY)
    override val playerLayoutMode = MutableStateFlow(PlayerLayoutMode.MODERN_AMBIENT)
    override val fullscreenArtworkLyricsStyle = MutableStateFlow(FullscreenArtworkLyricsStyle.PLAYER_STRIP)
    override val enableBreathingAura = MutableStateFlow(true)
    override fun setLastPlayerScreen(value: LastPlayerScreen) = Unit
    override fun setLyricsOffsetMs(value: Int) = Unit
    override fun setFullscreenArtworkLyricsStyle(value: FullscreenArtworkLyricsStyle) = Unit
}

private object PreviewPlayerHost : PlayerHost {
    override val settings: PlayerSettingsSource = PreviewPlayerSettings
    override fun cachedCanvas(song: Song): CanvasArtwork? = null
    override suspend fun canvasFor(song: Song): CanvasArtwork? = null
    @Composable
    override fun CanvasVideo(spec: CanvasVideoSpec, modifier: Modifier) = Unit
    @Composable
    override fun rememberRemoteArtworkUrl(song: Song?): String? = song?.thumbnailUrl
    override val volume: SystemVolume = object : SystemVolume {
        override val level = MutableStateFlow(0.68f)
        override fun set(level: Float) = Unit
        override fun refresh() = Unit
    }
    @Composable
    override fun rememberAudioOutputs(): List<AudioOutputDevice> = emptyList()
    @Composable
    override fun rememberOutputPicker(onOpen: () -> Unit): () -> Unit = onOpen
    override fun selectAudioOutput(id: Int?) = Unit
    override val outputFormat = MutableStateFlow(OutputFormatUi("24-bit PCM · 48 kHz", carriesHiRes = true))
    override val party = MutableStateFlow(PartyUi())
    override suspend fun translateLyrics(
        trackId: String,
        lines: List<LyricLine>,
        targetLanguageTag: String,
    ) = LyricsTranslationResult.Unavailable
    override suspend fun romanizeLyrics(
        trackId: String,
        lines: List<LyricLine>,
        targetLanguageTag: String,
    ) = LyricsRomanizationResult.Unavailable
    override fun showMessage(message: String) = Unit
    @Composable
    override fun AudioPipelineDialog(hazeState: HazeState, isPlaying: Boolean, onDismiss: () -> Unit, accentColor: Color?) = Unit
}

@Composable
private fun PreviewHostWrapper(content: @Composable (HazeState) -> Unit) {
    remember {
        if (runCatching { AppUi.host }.isFailure) {
            AppUi.install(PreviewAppUiHost)
        }
        if (runCatching { PlayerPlatform.host }.isFailure) {
            PlayerPlatform.install(PreviewPlayerHost)
        }
        true
    }
    val hazeState = remember { HazeState() }
    BitChordTheme(darkTheme = true) {
        CompositionLocalProvider(LocalAppHazeState provides hazeState) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                content(hazeState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Home · Redesign",
    device = "id:pixel_8",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeRedesignPreview() {
    PreviewHostWrapper {
        val sampleShelves = listOf(
            HomeShelf(
                title = "Top Picks for You",
                items = listOf(
                    ShelfItem("Midnight Radio", "Made for You • Ambient Synth", null, null, "VL_top1"),
                    ShelfItem("Late Night Echoes", "Album • The Midnight", null, null, "MPREb_top2"),
                ),
            ),
            HomeShelf(
                title = "Quick picks",
                items = listOf(
                    ShelfItem("Somebody You Hate", "MICO", null, "vid1", null),
                    ShelfItem("lofi hip hop radio", "Lofi Girl • 18K watching", null, "vidLive1", null, isVideo = true, isLive = true),
                    ShelfItem("Leave You (Never)", "ELIO", null, "vid2", null),
                    ShelfItem("Outside", "Mike Demero", null, "vid3", null),
                ),
            ),
            HomeShelf(
                title = "Albums and singles",
                items = listOf(
                    ShelfItem("The Life of a Showgirl", "Album • Taylor Swift", null, null, "MPREb_alb1"),
                    ShelfItem("Grand Theft Auto VI Mix", "Single • Skyline", null, null, "MPREb_alb2"),
                ),
            ),
            HomeShelf(
                title = "Music videos",
                items = listOf(
                    ShelfItem("YUME (Official Video)", "Raul • 747K views", null, "vidMv1", null, isVideo = true),
                    ShelfItem("Alfaaz (Acoustic Film)", "Alan Walker", null, "vidMv2", null, isVideo = true),
                ),
            ),
        )
        val speedDial = listOf(
            ShelfItem("lofi hip hop radio", "Lofi Girl • 18K watching", null, "vidLive1", null, isVideo = true, isLive = true),
            ShelfItem("Chill Mix 1", "Made for You", null, null, "VL_mix1"),
            ShelfItem("Tame Impala", "Artist", null, null, "UC_artist1"),
            ShelfItem("Midnight Memories", "Album", null, null, "MPREb_a1"),
            ShelfItem("Liked Music", "Playlist • 184 tracks", null, null, "VLLM"),
        )
        HomeScreen(
            state = UiState.Success(sampleShelves),
            listState = rememberLazyListState(),
            currentSong = Song("vidLive1", "lofi hip hop radio", "Lofi Girl", thumbnailUrl = null, isLive = true, isVideo = true),
            isPlaying = true,
            onItemClick = { _, _ -> },
            onRetry = {},
            refreshing = false,
            onRefresh = {},
            pullState = rememberPullToRefreshState(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            title = "Listen now",
            precomputedSpeedDialBase = speedDial,
            replayYear = ReplayYearBadge(
                year = 2026,
                artworkUrl = null,
                contentDescription = "Open your Replay 2026",
                onOpen = {},
            ),
            onFindMoodClick = {},
            onFindMoodShowAll = {},
        )
    }
}

@Preview(
    name = "Player · Headphone Pill",
    widthDp = 360,
    heightDp = 120,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PlayerBottomPillPreview() {
    PreviewHostWrapper {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF141822))
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            ChordPlayPipelineBottomPill(
                song = Song("vid1", "Midnight City", "M83", thumbnailUrl = null),
                isLoading = false,
                onOpenPipeline = {},
                onOpenOutput = {},
            )
        }
    }
}

@Preview(
    name = "Settings · Your Preferences",
    device = "id:pixel_8",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun YourPreferencesSectionPreview() {
    PreviewHostWrapper {
        ShelvesSection(
            onBack = {},
            contentPadding = PaddingValues(16.dp),
            initialSearchQuery = "",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Radio · Mixes & Stations",
    device = "id:pixel_8",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RadioRedesignPreview() {
    PreviewHostWrapper {
        val genres = listOf(
            MoodGenre("Chill", "FEmusic_moods_and_genres_category", "p1", stripeColor = 0xFF3A7BD5),
            MoodGenre("Focus", "FEmusic_moods_and_genres_category", "p2", stripeColor = 0xFF1D976C),
            MoodGenre("Energy", "FEmusic_moods_and_genres_category", "p3", stripeColor = 0xFF1DB954),
            MoodGenre("Workout", "FEmusic_moods_and_genres_category", "p4", stripeColor = 0xFFFF4B4B),
            MoodGenre("Sleep", "FEmusic_moods_and_genres_category", "p5", stripeColor = 0xFF302B63),
            MoodGenre("Party", "FEmusic_moods_and_genres_category", "p6", stripeColor = 0xFFB83280),
            MoodGenre("Jazz", "FEmusic_moods_and_genres_category", "p7", stripeColor = 0xFF9C4221),
        )
        val live = listOf(
            ShelfItem(
                title = "lofi hip hop radio - beats to relax/study to",
                subtitle = "Lofi Girl • 18K watching",
                thumbnailUrl = null,
                videoId = "jfKfPfyJRdk",
                browseId = null,
                isVideo = true,
                isLive = true,
                liveViewersText = "18K watching",
                liveCategory = LiveStationCategory.LOFI_CHILL,
            ),
        )
        val mixes = listOf(
            KeyedMix(ShelfItem("My Supermix", "Mix", null, null, "VL_q1"), "My Supermix"),
            KeyedMix(ShelfItem("Discover Mix", "Mix", null, null, "VL_q2"), "Discover Mix"),
            KeyedMix(ShelfItem("New Release Mix", "Mix", null, null, "VL_q3"), "New Release Mix"),
            KeyedMix(ShelfItem("Replay Mix", "Mix", null, null, "VL_q4"), "Replay Mix"),
            KeyedMix(ShelfItem("Archive Mix", "Mix", null, null, "VL_q5"), "Archive Mix"),
            KeyedMix(ShelfItem("Liked Music", "Mix", null, null, "VL_q6"), "Liked Music"),
            KeyedMix(ShelfItem("My Mix 1", "The Weeknd, MICO, Drake", null, null, "VL_m1"), "My Mix 1"),
            KeyedMix(ShelfItem("Sleep Supermix", "Ambient & Piano", null, null, "VL_s1"), "Sleep Supermix"),
            KeyedMix(ShelfItem("Sleep Mix 1", "Øneheart, Antent", null, null, "VL_s2"), "Sleep Mix 1"),
        )
        RadioScreen(
            state = UiState.Success(listOf(MoodGenreSection("Moods", genres))),
            listState = rememberLazyListState(),
            melodyItem = ShelfItem("Melody", "Endless personal mix", null, null, "chordplay:melody"),
            onMelodyClick = {},
            liveStations = UiState.Success(live),
            mixes = UiState.Success(mixes),
            seeds = emptyList(),
            onSeedClick = {},
            onStationClick = {},
            onStartStation = {},
            onGenresClick = {},
            onRetry = {},
            refreshing = false,
            onRefresh = {},
            pullState = rememberPullToRefreshState(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        )
    }
}

@Preview(name = "Welcome · ColorHunt Identity & Badges", widthDp = 400, heightDp = 420, showBackground = true)
@Composable
private fun WelcomeIdentityCardPreview() {
    PreviewHostWrapper {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0E121C))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ChordPlayIdentityCard(
                name = "Amit",
                onName = {},
                onRestoreClick = {},
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GitHubIconBadge(size = 44.dp)
                RedditIconBadge(size = 44.dp)
            }
        }
    }
}

@Preview(name = "Player Modal · Glass & Tinted", widthDp = 400, heightDp = 380, showBackground = true)
@Composable
private fun PlayerModalSurfacesPreview() {
    PreviewHostWrapper { haze ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF172336))
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            AudioPopupCard(
                hazeState = haze,
                title = "Audio Pipeline",
                subtitle = "Live signal path, decoder to output",
                doneLabel = "Done",
                onDismiss = {},
                accentColor = Color(0xFF6B7CFF),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "FORMAT & SAMPLE RATE",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                    Text(
                        text = "Opus · 154 kbps · 48 kHz Stereo",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "OUTPUT ROUTE",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                    Text(
                        text = "24-bit PCM · Hardware Mixer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}
