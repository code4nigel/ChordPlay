package com.music.bitchord.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.BrowseItem
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.sharedui.resources.*
import com.music.bitchord.ui.components.SearchField
import com.music.bitchord.ui.components.PillSegmentedControl
import com.music.bitchord.ui.components.SongRow
import com.music.bitchord.ui.screens.BrowseRow
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

internal data class QueueActions(val save: (() -> Unit)?, val search: (() -> Unit)?)
internal val LocalQueueActions = staticCompositionLocalOf<QueueActions?> { null }

class NowPlayingQueueTools(
    val search: suspend (String, SearchFilter) -> Result<List<SearchResult>>,
    val loadAlbum: suspend (BrowseItem) -> Result<List<Song>>,
    val onQueue: (List<Song>, Boolean) -> Unit,
    val onSave: () -> Unit,
    val saveOpen: Boolean,
    val saveContent: @Composable () -> Unit,
)

@Composable
internal fun QueueToolDialogs(
    tools: NowPlayingQueueTools,
    overlays: PlayerOverlayState,
    accent: Color?,
    controlsLocked: Boolean,
) {
    LaunchedEffect(controlsLocked) {
        if (controlsLocked) overlays.queueSearch = false
    }
    if (overlays.queueSearch && !controlsLocked) {
        QueueSearchDialog(accent, tools.search, tools.loadAlbum, tools.onQueue) {
            overlays.queueSearch = false
        }
    }
    tools.saveContent()
}

@Composable
internal fun QueueActionsPill(canSave: Boolean, locked: Boolean) {
    val actions = LocalQueueActions.current ?: return
    if (actions.save == null && actions.search == null) return
    val haptics = rememberHaptics()
    Row(
        Modifier.videoDockGlassPill(hazeState = LocalPlayerHazeState.current).padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.save?.let { save ->
            IconButton(onClick = { haptics.play(Haptic.Tap); save() }, enabled = canSave && !locked,
                modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Save, stringResource(Res.string.queue_save_playlist), Modifier.size(19.dp),
                    tint = Color.White.copy(alpha = if (canSave && !locked) 0.9f else 0.4f))
            }
        }
        actions.search?.let { search ->
            IconButton(onClick = { haptics.play(Haptic.Tap); search() }, enabled = !locked,
                modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Add, stringResource(Res.string.queue_search_title), Modifier.size(18.dp),
                    tint = Color.White.copy(alpha = if (locked) 0.4f else 0.9f))
            }
        }
    }
}

@Composable
internal fun QueueSearchDialog(
    accent: Color?,
    search: suspend (String, SearchFilter) -> Result<List<SearchResult>>,
    loadAlbum: suspend (BrowseItem) -> Result<List<Song>>,
    onQueue: (List<Song>, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(SearchFilter.SONGS) }
    var attempt by remember { mutableIntStateOf(0) }
    var response by remember { mutableStateOf<Result<List<SearchResult>>?>(null) }
    var album by remember { mutableStateOf<BrowseItem?>(null) }
    var albumResponse by remember { mutableStateOf<Result<List<Song>>?>(null) }
    var albumAttempt by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(query, category, attempt) {
        response = null
        val term = query.trim()
        if (term.isNotEmpty()) {
            delay(300L)
            response = search(term, category).map { results ->
                when (category) {
                    SearchFilter.ALBUMS -> results.filterIsInstance<SearchResult.Browse>()
                        .filter { it.item.type == BrowseType.ALBUM }.distinctBy { it.item.browseId }
                    else -> results.filterIsInstance<SearchResult.Track>().distinctBy { it.song.videoId }
                }
            }
        }
    }
    LaunchedEffect(album, albumAttempt) {
        albumResponse = null
        album?.let { albumResponse = loadAlbum(it) }
    }

    Dialog(
        onDismissRequest = { if (album == null) onDismiss() else album = null },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier.imePadding().padding(horizontal = 16.dp).widthIn(max = 520.dp).fillMaxWidth()
                .heightIn(max = 560.dp)
                .then(playerModalSurfaceModifier(RoundedCornerShape(28.dp), null, accent, true))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (album != null) {
                    IconButton(onClick = { album = null }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.back))
                    }
                }
                Text(album?.title ?: stringResource(Res.string.queue_search_title), Modifier.weight(1f),
                    color = Color.White, style = MaterialTheme.typography.titleLarge)
                TextButton(onDismiss) { Text(stringResource(Res.string.studio_close)) }
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            val activeAlbum = album
            if (activeAlbum != null) {
                val result = albumResponse
                val songs = result?.getOrNull()
                when {
                    result == null -> QueueSearchLoading()
                    songs == null -> QueueSearchError(Res.string.queue_search_error) { albumAttempt++ }
                    songs.isEmpty() -> Text(stringResource(Res.string.queue_search_empty), color = Color.White)
                    else -> {
                        QueueSearchResultActions(songs, onQueue, wholeAlbum = true)
                        LazyColumn(Modifier.weight(1f, fill = false)) {
                            items(songs) { song -> QueueSearchSong(song, onQueue) }
                        }
                    }
                }
            } else {
                SearchField(query, { query = it; response = null }, { attempt++ }, focusRequester = focusRequester,
                    placeholder = stringResource(Res.string.queue_search_hint))
                PillSegmentedControl(
                    options = listOf(stringResource(Res.string.songs), stringResource(Res.string.albums)),
                    selectedIndex = if (category == SearchFilter.SONGS) 0 else 1,
                    onSelect = { index ->
                        val next = if (index == 0) SearchFilter.SONGS else SearchFilter.ALBUMS
                        if (next != category) {
                            response = null
                            category = next
                        }
                    },
                )
                if (query.isBlank()) return@Column
                val result = response
                val matches = result?.getOrNull()
                when {
                    result == null -> QueueSearchLoading()
                    matches == null -> QueueSearchError(
                        if (category == SearchFilter.ALBUMS) Res.string.queue_search_albums_error else Res.string.queue_search_error,
                    ) { attempt++ }
                    matches.isEmpty() -> Text(
                        stringResource(if (category == SearchFilter.ALBUMS) Res.string.queue_search_no_albums else Res.string.queue_search_empty),
                        color = Color.White,
                    )
                    else -> LazyColumn(Modifier.weight(1f, fill = false)) {
                        items(matches) { match ->
                            when (match) {
                                is SearchResult.Track -> QueueSearchSong(match.song, onQueue)
                                is SearchResult.Browse -> MaterialTheme(
                                    colorScheme = MaterialTheme.colorScheme.copy(
                                        onBackground = Color.White, onSurfaceVariant = Color.White.copy(alpha = 0.75f),
                                    ),
                                ) {
                                    BrowseRow(match.item, onClick = { albumResponse = null; album = match.item })
                                }
                                is SearchResult.TopTrack -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueSearchLoading() {
    Box(Modifier.fillMaxWidth().heightIn(min = 64.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(24.dp))
    }
}

@Composable
private fun QueueSearchError(message: org.jetbrains.compose.resources.StringResource, retry: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(message), Modifier.weight(1f), color = Color.White)
        TextButton(retry) { Text(stringResource(Res.string.retry)) }
    }
}

@Composable
private fun QueueSearchSong(song: Song, onQueue: (List<Song>, Boolean) -> Unit) {
    val tracks = listOf(song)
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(onBackground = Color.White)) {
        SongRow(song, onClick = { onQueue(tracks, false) }, searchPlayingStyle = true,
            subtitleColor = Color.White.copy(alpha = 0.75f))
    }
    QueueSearchResultActions(tracks, onQueue)
}

@Composable
private fun QueueSearchResultActions(songs: List<Song>, onQueue: (List<Song>, Boolean) -> Unit, wholeAlbum: Boolean = false) {
    FlowRow(Modifier.padding(start = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton({ onQueue(songs, true) }, Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(if (wholeAlbum) Res.string.queue_search_play_album_next else Res.string.play_next))
        }
        TextButton({ onQueue(songs, false) }, Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(if (wholeAlbum) Res.string.queue_search_add_album else Res.string.queue_search_add))
        }
    }
}
