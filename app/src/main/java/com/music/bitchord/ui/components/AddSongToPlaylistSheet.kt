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

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import kotlinx.coroutines.delay

private val playlistSongId = Regex("[A-Za-z0-9_-]{11}")

@Composable
internal fun AddSongToPlaylistSheet(
    playlistTitle: String,
    thumbnailUrl: String?,
    initialSongs: List<Song>,
    suggestedSongs: List<Song>,
    onAddSong: (Song, (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Song>?>(null) }
    var fallback by remember { mutableStateOf<List<Song>?>(null) }
    var searchFailed by remember { mutableStateOf(false) }
    var confirmed by remember { mutableStateOf(emptySet<String>()) }
    var pending by remember { mutableStateOf(emptySet<String>()) }
    // Snapshot initial membership for the sheet session so tapping + transitions
    // the row to a checkmark in place rather than removing it and shifting the list.
    val initialPresentIds = remember { initialSongs.mapTo(HashSet(), Song::videoId) }
    val present = remember(initialSongs, confirmed) {
        initialSongs.mapTo(confirmed.toMutableSet(), Song::videoId)
    }
    val suggestions = remember(suggestedSongs, initialPresentIds) {
        suggestedSongs.filter {
            playlistSongId.matches(it.videoId) && !it.isLive && !it.isVideo && it.videoId !in initialPresentIds
        }.distinctBy(Song::videoId)
    }

    LaunchedEffect(suggestions.isEmpty()) {
        if (suggestions.isEmpty() && fallback == null) {
            fallback = YtMusicRepository.quickPicks(initialPresentIds, songsOnly = true)
                .getOrDefault(emptyList())
                .take(20)
        }
    }
    LaunchedEffect(query.trim()) {
        val text = query.trim()
        if (text.isBlank()) return@LaunchedEffect
        delay(300)
        YtMusicRepository.searchPage(text, SearchFilter.SONGS, isLibrary = false).fold(
            onSuccess = { page ->
                results = page.rows.mapNotNull {
                    when (it) {
                        is SearchResult.Track -> it.song
                        is SearchResult.TopTrack -> it.song
                        else -> null
                    }
                }.filter { playlistSongId.matches(it.videoId) && !it.isLive }.distinctBy(Song::videoId)
            },
            onFailure = { searchFailed = true; results = emptyList() },
        )
    }

    Column(Modifier.fillMaxWidth().height(540.dp).padding(horizontal = 14.dp)) {
        PlaylistSheetHeader(
            title = stringResource(R.string.add_songs_to_playlist),
            subtitle = playlistTitle,
            thumbnailUrl = thumbnailUrl,
            onDismiss = onDismiss,
        )
        SearchField(
            query = query,
            onQueryChange = {
                if (it.trim() != query.trim()) {
                    results = null
                    searchFailed = false
                }
                query = it
            },
            onSubmit = {},
            placeholder = stringResource(R.string.search_songs_to_add),
        )
        val searching = query.isNotBlank()
        val songs = if (searching) {
            results
        } else {
            suggestions.ifEmpty {
                fallback?.filterNot { it.videoId in initialPresentIds }
            }
        }
        val bodyMode = when {
            songs == null -> "loading"
            songs.isEmpty() -> "empty"
            else -> "list"
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            AnimatedContent(
                targetState = bodyMode,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                },
                label = "addSongSheetBody",
            ) { mode ->
                when (mode) {
                    "loading" -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.searching_songs),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    "empty" -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(if (searchFailed) R.string.failed else R.string.no_songs_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    else -> {
                        val visibleSongs = songs.orEmpty()
                        LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp, bottom = 8.dp)) {
                            if (!searching && visibleSongs.isNotEmpty()) {
                                item(key = "recommended_heading") {
                                    SheetHeading(stringResource(R.string.recommended_songs))
                                }
                            }
                            items(visibleSongs, key = Song::videoId) { song ->
                                val id = song.videoId
                                val add = {
                                    if (id !in present && id !in pending) {
                                        pending = pending + id
                                        onAddSong(song) { success ->
                                            pending = pending - id
                                            if (success) {
                                                confirmed = confirmed + id
                                            } else {
                                                Toast.makeText(context, R.string.failed, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.animateItem(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    SongRow(
                                        song = song,
                                        onClick = add,
                                        modifier = Modifier.weight(1f),
                                        searchPlayingStyle = true,
                                        downloadedTint = null,
                                    )
                                    val actionState = when {
                                        id in pending -> 0
                                        id in present -> 1
                                        else -> 2
                                    }
                                    Crossfade(targetState = actionState, label = "addSongAction") { state ->
                                        when (state) {
                                            0 -> CircularProgressIndicator(
                                                modifier = Modifier.padding(14.dp).size(20.dp),
                                                strokeWidth = 2.dp,
                                            )
                                            1 -> Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = stringResource(R.string.in_playlist),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(14.dp).size(20.dp),
                                            )
                                            else -> IconButton(onClick = add) {
                                                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_to_playlist), tint = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlaylistSheetHeader(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    onDismiss: () -> Unit,
) {
    Column {
        Box(
            modifier = Modifier.fillMaxWidth().height(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(width = 34.dp, height = 4.dp).background(
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape,
            ))
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                val shape = RoundedCornerShape(10.dp)
                AsyncImage(
                    model = thumbnailUrl.artworkAt(ROW_ART_PX),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(shape).thumbnailBorder(shape),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close), tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}
