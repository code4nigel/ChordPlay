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
package com.music.bitchord.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbDownOffAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.VideoQuality
import com.music.bitchord.data.webdav.WebDavUploads
import com.music.bitchord.download.DownloadState
import com.music.bitchord.download.Downloads
import com.music.bitchord.playback.SleepTimer
import com.music.bitchord.ui.components.thumbnailBorder
import com.music.bitchord.ui.icons.BitChordIcons
import com.music.bitchord.ui.player.ThinSlider
import com.music.bitchord.ui.theme.ArtworkPalette
import com.music.bitchord.ui.theme.rememberArtworkPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Long-press & 3-dot floating menu for a track, styled in the modern iOS-like frosted popover layout.
 *
 * Structure:
 * - Quick Actions: 3-tile horizontal row with Like, Dislike, Share.
 * - Group 1: Version actions (Revert/Upgrade quality, Convert to video/audio, Video quality).
 * - Group 2: Playlist & Speed dial (Add to playlist, Pin to Your Space).
 * - Group 3: Offline & Cache (Download, Remove from cache, WebDAV upload).
 * - Group 4: Queue & Radio (Start radio, AI Mix, Play next, Add to queue).
 * - Group 5: Navigation (Open album with album name, Open artist with artist name).
 * - Group 6: Utilities (Song Info dialog, Sleep timer, Lyrics offset, Copy log).
 */
@Composable
fun SongActionsMenu(
    song: Song,
    signedIn: Boolean,
    likeStatus: LikeStatus,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onStartRadio: () -> Unit,
    onDownload: () -> Unit,
    onUploadToWebDav: (() -> Unit)? = null,
    onToggleLike: () -> Unit,
    onToggleDislike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onRemoveFromCache: (() -> Unit)? = null,
    onRemoveFromHistory: (() -> Unit)? = null,
    showSleepTimer: Boolean = false,
    onRollbackToOriginal: (() -> Unit)? = null,
    onUpgradeQuality: (() -> Unit)? = null,
    upgradeQualityInProgress: Boolean = false,
    onToggleAudioVersion: (() -> Unit)? = null,
    isAudioVersion: Boolean = false,
    audioVersionMissing: Boolean = false,
    onShare: (() -> Unit)? = null,
    onCopyLog: (() -> Unit)? = null,
    onLyricsOffset: (() -> Unit)? = null,
    onAutomixOverview: (() -> Unit)? = null,
    resolvingLinks: Boolean = false,
    onNeverPlay: (() -> Unit)? = null,
    onMarkArtistAi: (() -> Unit)? = null,
) {
    var showingSongInfo by remember(song.videoId) { mutableStateOf(false) }
    var pickingSleepTimer by remember { mutableStateOf(false) }
    var pickingVideoQuality by remember { mutableStateOf(false) }
    val currentVideoQuality by AppSettings.videoQuality.collectAsStateWithLifecycle()
    val mvQuality by com.music.bitchord.ui.player.PlayerPlatform.host.musicVideoQuality.collectAsStateWithLifecycle()
    val videoQualityText = com.music.bitchord.ui.player.videoQualitySummary(mvQuality, currentVideoQuality)

    if (showingSongInfo) {
        SongInfoDialog(song) { showingSongInfo = false }
    }

    if (pickingVideoQuality) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { pickingVideoQuality = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            com.music.bitchord.ui.player.VideoQualitySheet(
                hazeState = null,
                onDismiss = { pickingVideoQuality = false },
                accentColor = MaterialTheme.colorScheme.primary,
            )
        }
    }

    val artworkPalette = rememberArtworkPalette(imageUrl = song.thumbnailUrl)
    val palette = if (artworkPalette.accent != Color.Unspecified && artworkPalette.background != Color.Unspecified) {
        artworkPalette
    } else {
        neutralMenuPalette()
    }
    val liked = likeStatus == LikeStatus.LIKE
    val disliked = likeStatus == LikeStatus.DISLIKE
    val isOffline = song.localUri != null

    val pinnedSpeedDial by AppSettings.pinnedSpeedDialItems.collectAsStateWithLifecycle()
    val isSpeedDialPinned = remember(pinnedSpeedDial, song.videoId) {
        AppSettings.isSpeedDialPinned(song.videoId)
    }

    val rows: @Composable ColumnScope.() -> Unit = {
        var drawnAny = false
        fun startsGroup(): Boolean = drawnAny.also { drawnAny = true }
        val canRate = isOffline || signedIn
        val shareAction = onShare?.takeIf { !isOffline }

        // Top Quick Actions bar: Like, Dislike, Share
        if (canRate || shareAction != null) {
            if (startsGroup()) IosMenuSeparator()
            IosMenuQuickActions {
                if (canRate) {
                    IosMenuQuickAction(
                        icon = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        label = stringResource(R.string.like),
                        tint = if (liked) palette.accent else null,
                        onClick = onToggleLike,
                    )
                }
                if (signedIn && !isOffline) {
                    IosMenuQuickAction(
                        icon = if (disliked) Icons.Rounded.ThumbDown else Icons.Rounded.ThumbDownOffAlt,
                        label = stringResource(R.string.dislike),
                        tint = if (disliked) palette.accent else null,
                        onClick = onToggleDislike,
                    )
                }
                shareAction?.let {
                    IosMenuQuickAction(
                        icon = Icons.Rounded.Share,
                        label = stringResource(R.string.share),
                        onClick = it,
                    )
                }
            }
        }

        // Group 1: Version actions
        val hasVersionActions = onRollbackToOriginal != null || onUpgradeQuality != null || onToggleAudioVersion != null
        val showVideoQualityAction = !isAudioVersion || song.isVideo || song.isLive
        if (hasVersionActions || showVideoQualityAction) {
            if (startsGroup()) IosMenuSeparator()
            (onRollbackToOriginal ?: onUpgradeQuality)?.let {
                ActionRow(
                    icon = if (onRollbackToOriginal != null) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.HighQuality,
                    label = stringResource(
                        if (onRollbackToOriginal != null) R.string.revert_to_original else R.string.upgrade_quality,
                    ),
                    accent = palette.accent,
                    enabled = onRollbackToOriginal != null || !upgradeQualityInProgress,
                    onClick = it,
                )
            }
            onToggleAudioVersion?.let {
                ActionRow(
                    icon = if (isAudioVersion) Icons.Rounded.Videocam else BitChordIcons.MusicNote,
                    label = stringResource(
                        when {
                            audioVersionMissing && isAudioVersion -> R.string.no_video_version
                            audioVersionMissing -> R.string.no_audio_version
                            isAudioVersion -> R.string.convert_to_video
                            else -> R.string.convert_to_audio
                        },
                    ),
                    accent = palette.accent,
                    enabled = !audioVersionMissing,
                    onClick = it,
                )
            }
            if (showVideoQualityAction) {
                ActionRow(
                    icon = Icons.Rounded.Tune,
                    label = "Video quality",
                    value = videoQualityText,
                    accent = palette.accent,
                    onClick = { pickingVideoQuality = true },
                )
            }
        }

        // Group 2: Playlist & Speed Dial
        if (canRate || onRemoveFromPlaylist != null) {
            if (startsGroup()) IosMenuSeparator()
            if (canRate) ActionRow(
                icon = BitChordIcons.PlaylistAdd,
                label = stringResource(R.string.add_to_playlist),
                accent = palette.accent,
                onClick = onAddToPlaylist,
            )
            onRemoveFromPlaylist?.let {
                ActionRow(
                    icon = Icons.Rounded.PlaylistRemove,
                    label = stringResource(R.string.remove_from_playlist),
                    accent = palette.accent,
                    onClick = it,
                )
            }
            if (canRate) ActionRow(
                icon = Icons.Rounded.PushPin,
                label = if (isSpeedDialPinned) stringResource(R.string.unpin_from_your_space) else stringResource(R.string.pin_to_your_space),
                value = if (isSpeedDialPinned) "Pinned" else null,
                accent = palette.accent,
                onClick = {
                    AppSettings.togglePinSpeedDial(
                        ShelfItem(
                            title = song.title,
                            subtitle = song.artist,
                            thumbnailUrl = song.thumbnailUrl,
                            videoId = song.videoId,
                            browseId = null,
                            isVideo = song.isVideo || song.isLive,
                            isLive = song.isLive,
                            liveViewersText = song.liveViewersText,
                        )
                    )
                },
            )
            if (AppSettings.appDesignStyle.collectAsStateWithLifecycle().value == com.music.bitchord.data.settings.AppDesignStyle.CHORDPLAY) {
                val songShelfItem = remember(song) {
                    ShelfItem(
                        title = song.title,
                        subtitle = song.artist,
                        thumbnailUrl = song.thumbnailUrl,
                        videoId = song.videoId,
                        browseId = null,
                        isVideo = song.isVideo || song.isLive,
                        isLive = song.isLive,
                        liveViewersText = song.liveViewersText,
                    )
                }
                val globalShowLabels by AppSettings.speedDialShowLabels.collectAsStateWithLifecycle()
                val labelOverrides by AppSettings.speedDialLabelOverrides.collectAsStateWithLifecycle()
                val tileId = song.videoId ?: song.title.trim()
                val isTileLabelShown = globalShowLabels != (tileId in labelOverrides)
                if (isSpeedDialPinned) {
                    ActionRow(
                        icon = Icons.Rounded.Edit,
                        label = if (isTileLabelShown) stringResource(R.string.hide_tile_name) else stringResource(R.string.show_tile_name),
                        accent = palette.accent,
                        onClick = { AppSettings.toggleSpeedDialTileLabel(songShelfItem) },
                    )
                }
                ActionRow(
                    icon = Icons.Rounded.VisibilityOff,
                    label = stringResource(R.string.hide_from_your_space),
                    accent = palette.accent,
                    onClick = { AppSettings.hideFromSpeedDial(songShelfItem) },
                )
            }
        }

        // Group 3: Downloads & Offline cache
        val showCacheGroup = !song.isLive || onRemoveFromCache != null || onRemoveFromHistory != null ||
            (onUploadToWebDav != null && isOffline)
        if (showCacheGroup) {
            if (startsGroup()) IosMenuSeparator()
            if (!song.isLive) DownloadRow(song, palette, isOffline, onDownload)
            onRemoveFromCache?.let {
                ActionRow(
                    icon = Icons.Rounded.DeleteOutline,
                    label = stringResource(R.string.remove_from_cache),
                    accent = palette.accent,
                    onClick = it,
                )
            }
            WebDavUploadRow(song, palette, isOffline, onUploadToWebDav)
            onRemoveFromHistory?.let {
                ActionRow(
                    icon = Icons.Rounded.DeleteOutline,
                    label = stringResource(R.string.remove_from_history),
                    accent = palette.accent,
                    onClick = it,
                )
            }
        }

        // Group 4: Queue & Radio
        if (startsGroup()) IosMenuSeparator()
        ActionRow(
            icon = Icons.Rounded.Radio,
            label = stringResource(R.string.start_radio),
            accent = palette.accent,
            onClick = onStartRadio,
        )
        onAutomixOverview?.let {
            ActionRow(
                icon = Icons.Rounded.Headphones,
                label = "AI Mix",
                value = "(beta)",
                accent = palette.accent,
                onClick = it,
            )
        }
        ActionRow(
            icon = BitChordIcons.PlayNext,
            label = stringResource(R.string.play_next),
            accent = palette.accent,
            onClick = onPlayNext,
        )
        ActionRow(
            icon = BitChordIcons.AddToQueue,
            label = stringResource(R.string.add_to_queue),
            accent = palette.accent,
            onClick = onAddToQueue,
        )

        if (onNeverPlay != null || onMarkArtistAi != null) {
            if (startsGroup()) IosMenuSeparator()
            onNeverPlay?.let {
                ActionRow(
                    icon = Icons.Rounded.Block,
                    label = stringResource(R.string.never_play_song),
                    accent = palette.accent,
                    onClick = it,
                )
            }
            onMarkArtistAi?.let {
                ActionRow(
                    icon = Icons.Rounded.SmartToy,
                    label = stringResource(R.string.mark_artist_ai),
                    accent = palette.accent,
                    onClick = it,
                )
            }
        }

        // Group 5: Navigation with subtitle descriptions
        if (startsGroup()) IosMenuSeparator()
        when (val id = song.albumId) {
            null -> if (resolvingLinks) {
                LoadingActionRow(Icons.Rounded.Album, stringResource(R.string.open_album), palette)
            }
            else -> ActionRow(
                icon = Icons.Rounded.Album,
                label = stringResource(R.string.open_album),
                description = song.albumName,
                accent = palette.accent,
            ) { onOpenAlbum(id) }
        }
        when (val id = song.artistId) {
            null -> if (resolvingLinks) {
                LoadingActionRow(Icons.Rounded.Mic, stringResource(R.string.open_artist), palette)
            }
            else -> ActionRow(
                icon = Icons.Rounded.Mic,
                label = stringResource(R.string.open_artist),
                description = song.artist.takeIf { it.isNotBlank() },
                accent = palette.accent,
            ) { onOpenArtist(id) }
        }

        // Group 6: Utilities
        if (startsGroup()) IosMenuSeparator()
        ActionRow(
            icon = Icons.Rounded.Info,
            label = stringResource(R.string.song_info),
            accent = palette.accent,
        ) {
            showingSongInfo = true
        }
        if (showSleepTimer) {
            ActionRow(
                icon = Icons.Rounded.Bedtime,
                label = stringResource(R.string.sleep_timer),
                value = sleepTimerStatus(),
                accent = palette.accent,
            ) { pickingSleepTimer = true }
        }
        onLyricsOffset?.let {
            ActionRow(
                icon = Icons.Rounded.Tune,
                label = stringResource(R.string.lyrics_offset),
                accent = palette.accent,
                onClick = it,
            )
        }
        onCopyLog?.let {
            ActionRow(
                icon = Icons.Rounded.BugReport,
                label = stringResource(R.string.copy_log),
                accent = palette.accent,
                onClick = it,
            )
        }
    }

    CompositionLocalProvider(LocalActionRowStyle provides ActionRowStyle.Menu) {
        Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (pickingSleepTimer) {
                SleepTimerPicker(palette = palette, onBack = { pickingSleepTimer = false })
            } else {
                rows()
            }
        }
    }
}

@Composable
private fun neutralMenuPalette(): ArtworkPalette {
    val accent = MaterialTheme.colorScheme.primary
    val colors = iosMenuColors()
    return remember(accent, colors) {
        ArtworkPalette(
            background = colors.fill,
            wash = colors.fill,
            elevated = colors.fill,
            accent = accent,
            onBackground = colors.label,
            onBackgroundVariant = colors.secondaryLabel,
            divider = colors.separator,
        )
    }
}

@Composable
private fun DownloadRow(song: Song, palette: ArtworkPalette, isOffline: Boolean, onDownload: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val active by Downloads.active.collectAsStateWithLifecycle()
    val saved by Downloads.saved.collectAsStateWithLifecycle()

    val recorded = saved[song.videoId]
    val file by produceState(recorded?.let(Uri::parse), song.videoId, recorded) {
        value = Downloads.savedUri(context, song.videoId)
    }

    DisposableEffect(song.videoId) {
        onDispose { Downloads.dismissFailure(song.videoId) }
    }

    when (val state = active[song.videoId]) {
        is DownloadState.Queued -> ActionRow(
            icon = Icons.Rounded.Downloading,
            label = stringResource(R.string.queued),
            value = stringResource(R.string.cancel),
            accent = palette.accent,
        ) { Downloads.cancel(song.videoId) }

        is DownloadState.Running -> ActionRow(
            icon = Icons.Rounded.Downloading,
            label = stringResource(R.string.download_notification_title),
            value = if (state.fraction > 0f) "${(state.fraction * 100).toInt()}%" else null,
            tint = palette.accent,
            accent = palette.accent,
        ) { Downloads.cancel(song.videoId) }

        is DownloadState.Failed -> ActionRow(
            icon = Icons.Rounded.ErrorOutline,
            label = state.reason,
            value = stringResource(R.string.retry),
            tint = MaterialTheme.colorScheme.error,
            accent = MaterialTheme.colorScheme.error,
            onClick = onDownload,
        )

        null -> if (file != null) {
            ActionRow(
                icon = Icons.Rounded.DownloadDone,
                label = stringResource(R.string.saved_to_downloads),
                value = stringResource(R.string.delete),
                tint = palette.accent,
                accent = palette.accent,
            ) { scope.launch { Downloads.delete(context, song.videoId) } }
        } else if (!isOffline) {
            ActionRow(
                icon = Icons.Rounded.Download,
                label = stringResource(R.string.download),
                accent = palette.accent,
                onClick = onDownload,
            )
        }
    }
}

@Composable
private fun WebDavUploadRow(
    song: Song,
    palette: ArtworkPalette,
    isOffline: Boolean,
    onUpload: (() -> Unit)?,
) {
    onUpload ?: return
    if (!isOffline || !WebDavUploads.isUploadable(song)) return
    val active by WebDavUploads.active.collectAsStateWithLifecycle()
    when (val state = active[song.videoId]) {
        is WebDavUploads.TrackState.Running -> ActionRow(
            icon = Icons.Rounded.FileUpload,
            label = stringResource(R.string.upload_to_webdav),
            value = "${(state.fraction * 100).toInt()}%",
            tint = palette.accent,
            accent = palette.accent,
            onClick = {},
        )
        is WebDavUploads.TrackState.Failed -> ActionRow(
            icon = Icons.Rounded.FileUpload,
            label = state.reason,
            value = stringResource(R.string.try_again),
            tint = MaterialTheme.colorScheme.error,
            accent = MaterialTheme.colorScheme.error,
            onClick = onUpload,
        )
        else -> ActionRow(
            icon = Icons.Rounded.FileUpload,
            label = stringResource(R.string.upload_to_webdav),
            accent = palette.accent,
            onClick = onUpload,
        )
    }
}

@Composable
private fun SleepTimerPicker(palette: ArtworkPalette, onBack: () -> Unit) {
    val chosen by SleepTimer.minutes.collectAsStateWithLifecycle()
    val afterTrack by SleepTimer.afterTrack.collectAsStateWithLifecycle()
    val countdown = sleepTimerCountdown()

    Column(Modifier.fillMaxWidth()) {
        SleepTimerHeader(
            title = stringResource(R.string.sleep_timer),
            subtitle = when {
                countdown != null -> stringResource(R.string.pause_countdown, countdown)
                afterTrack -> stringResource(R.string.pause_after_song)
                else -> stringResource(R.string.pause_after_a_while)
            },
            onBack = onBack,
        )
        HorizontalDivider(thickness = 0.5.dp, color = palette.divider)

        SleepOption(
            label = stringResource(R.string.after_this_song),
            selected = afterTrack,
            accent = palette.accent,
        ) {
            SleepTimer.startAfterTrack()
            onBack()
        }
        SleepTimer.PRESETS.forEach { minutes ->
            SleepOption(
                label = if (minutes == 60) {
                    stringResource(R.string.one_hour)
                } else {
                    pluralStringResource(R.plurals.minute_count, minutes, minutes)
                },
                selected = minutes == chosen,
                accent = palette.accent,
            ) {
                SleepTimer.start(minutes)
                onBack()
            }
        }
        if (chosen != null || afterTrack) {
            ActionRow(Icons.Rounded.Close, stringResource(R.string.turn_off_timer), accent = palette.accent) {
                SleepTimer.cancel()
                onBack()
            }
        }
    }
}

@Composable
private fun SleepTimerHeader(title: String, subtitle: String, onBack: () -> Unit) {
    val colors = iosMenuColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = colors.label,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    letterSpacing = (-0.3).sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.label,
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                color = colors.secondaryLabel,
            )
        }
    }
}

@Composable
private fun SleepOption(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    IosMenuRow(
        label = label,
        icon = if (selected) Icons.Rounded.Check else null,
        onClick = onClick,
        iconTint = accent,
    )
}

@Composable
private fun sleepTimerStatus(): String? {
    val afterTrack by SleepTimer.afterTrack.collectAsStateWithLifecycle()
    return sleepTimerCountdown() ?: stringResource(R.string.after_this_song).takeIf { afterTrack }
}

@Composable
private fun sleepTimerCountdown(): String? {
    val deadline by SleepTimer.deadline.collectAsStateWithLifecycle()
    val remaining by produceState<Long?>(initialValue = SleepTimer.remainingMs(), deadline) {
        while (deadline != null) {
            value = SleepTimer.remainingMs()
            delay(1_000)
        }
        value = null
    }
    return remaining?.let {
        val seconds = it / 1000
        "%d:%02d".format(Locale.ROOT, seconds / 60, seconds % 60)
    }
}

@Composable
internal fun ActionRow(
    icon: ImageVector,
    label: String,
    value: String? = null,
    tint: Color? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    description: String? = null,
    onClick: () -> Unit,
) {
    if (LocalActionRowStyle.current == ActionRowStyle.Menu) {
        IosMenuRow(
            label = label,
            icon = icon,
            onClick = onClick,
            description = description,
            value = value,
            iconTint = tint,
            valueColor = accent,
            enabled = enabled,
        )
        return
    }
    val foreground = MaterialTheme.colorScheme.onBackground
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = (tint ?: foreground).copy(alpha = alpha),
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(18.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = foreground.copy(alpha = alpha),
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = accent.copy(alpha = alpha),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LoadingActionRow(icon: ImageVector, label: String, palette: ArtworkPalette) {
    IosMenuRow(label = label, icon = icon, onClick = null, enabled = false) {
        CircularProgressIndicator(
            color = palette.onBackgroundVariant,
            strokeWidth = 2.dp,
            modifier = Modifier.size(16.dp),
        )
    }
}
