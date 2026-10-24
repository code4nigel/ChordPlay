package com.music.bitchord.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import kotlinx.coroutines.delay
import com.music.bitchord.data.settings.AppSettings
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.util.concurrent.ConcurrentHashMap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.music.bitchord.data.YtMusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.model.PlaylistPrivacy
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UserPlaylist
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.ui.components.thumbnailBorder
import com.music.bitchord.ui.icons.BitChordIcons
import java.util.Locale

/** Cache of playlist membership across sheets to eliminate redundant network hits and track additions/removals. */
object PlaylistMembershipCache {
    private val membership = ConcurrentHashMap<String, ConcurrentHashMap<String, String?>>()

    fun contains(playlistId: String, videoId: String): Boolean =
        membership[playlistId]?.containsKey(videoId) == true

    fun getSetVideoId(playlistId: String, videoId: String): String? =
        membership[playlistId]?.get(videoId)

    fun add(playlistId: String, videoId: String, setVideoId: String? = null) {
        membership.computeIfAbsent(playlistId) { ConcurrentHashMap() }[videoId] = setVideoId
    }

    fun remove(playlistId: String, videoId: String) {
        membership[playlistId]?.remove(videoId)
    }

    fun addAll(playlistId: String, entries: Map<String, String?>) {
        membership.computeIfAbsent(playlistId) { ConcurrentHashMap() }.putAll(entries)
    }

    fun addAllIds(playlistId: String, videoIds: Collection<String>) {
        val map = membership.computeIfAbsent(playlistId) { ConcurrentHashMap() }
        videoIds.forEach { vid -> map.putIfAbsent(vid, null) }
    }
}

/**
 * Where a track goes: one of the account's playlists, or a new one.
 *
 * Supports multi-selection with explicit Done action:
 * - Playlists already containing the song are initially checked.
 * - Tapping an already-present playlist unchecks it (marks for removal).
 * - Tapping an unchecked playlist checks it (marks for addition).
 * - Clicking Done executes both additions and removals.
 */
@Composable
fun PlaylistPickerSheet(
    playlists: List<UserPlaylist>,
    loading: Boolean,
    onAdd: (List<UserPlaylist>) -> Unit,
    onCreate: (String, PlaylistPrivacy) -> Unit,
    modifier: Modifier = Modifier,
    song: Song? = null,
    startCreating: Boolean = false,
    onCommit: ((toAdd: List<UserPlaylist>, toRemove: List<UserPlaylist>) -> Unit)? = null,
) {
    val chordplayPlaylistIds by AppSettings.chordplayPlaylistIds.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(startCreating) }
    var playlistSearchQuery by remember { mutableStateOf("") }

    // Seed initiallyPresent from local memory cache
    var initiallyPresent by remember(song?.videoId) {
        mutableStateOf(
            song?.videoId?.let { vid ->
                playlists.filter { pl -> PlaylistMembershipCache.contains(pl.playlistId, vid) }
                    .map { it.playlistId }.toSet()
            } ?: emptySet()
        )
    }

    // Selected state begins identical to initiallyPresent:
    // Any playlist containing the song is checked; others are unchecked.
    var selected by remember(song?.videoId) {
        mutableStateOf(
            song?.videoId?.let { vid ->
                playlists.filter { pl -> PlaylistMembershipCache.contains(pl.playlistId, vid) }
                    .map { it.playlistId }.toSet()
            } ?: emptySet()
        )
    }

    val userToggledPlaylists = remember(song?.videoId) { mutableSetOf<String>() }

    // Non-blocking, gentle background check for playlists not yet in memory.
    // Checks sequentially with delays so there is zero UI stutter, thread starvation or network flooding.
    LaunchedEffect(song?.videoId) {
        val target = song ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            for (pl in playlists) {
                if (pl.playlistId in initiallyPresent) continue
                if (PlaylistMembershipCache.contains(pl.playlistId, target.videoId)) {
                    withContext(Dispatchers.Main) {
                        initiallyPresent = initiallyPresent + pl.playlistId
                        if (pl.playlistId !in userToggledPlaylists) {
                            selected = selected + pl.playlistId
                        }
                    }
                    continue
                }

                delay(80)
                runCatching {
                    val entries = YtMusicRepository.browseSongs(pl.browseId).getOrNull()?.songs
                    if (entries != null) {
                        PlaylistMembershipCache.addAll(
                            pl.playlistId,
                            entries.associate { it.videoId to it.setVideoId },
                        )
                        val match = entries.firstOrNull { it.videoId == target.videoId }
                        if (match != null) {
                            withContext(Dispatchers.Main) {
                                initiallyPresent = initiallyPresent + pl.playlistId
                                if (pl.playlistId !in userToggledPlaylists) {
                                    selected = selected + pl.playlistId
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val displayedPlaylists = remember(playlists, playlistSearchQuery) {
        if (playlistSearchQuery.isBlank()) playlists
        else playlists.filter { it.title.contains(playlistSearchQuery, ignoreCase = true) }
    }

    if (creating) {
        NewPlaylistForm(
            onBack = if (startCreating) null else ({ creating = false }),
            onCreate = onCreate,
            modifier = modifier,
        )
        return
    }

    Column(modifier.fillMaxWidth()) {
        if (song != null) {
            SheetTrackHeader(song)
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
        }
        SheetHeading(
            stringResource(if (song != null) R.string.add_to_playlist else R.string.your_playlists)
                .uppercase(Locale.getDefault()),
        )

        ActionRow(
            icon = BitChordIcons.Plus,
            label = stringResource(R.string.new_playlist),
            onClick = { creating = true },
        )

        if (playlists.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (playlistSearchQuery.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_playlists),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                    BasicTextField(
                        value = playlistSearchQuery,
                        onValueChange = { playlistSearchQuery = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (playlistSearchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { playlistSearchQuery = "" },
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.clear_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        when {
            loading && playlists.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(26.dp),
                )
            }

            playlists.isEmpty() -> Text(
                text = stringResource(R.string.no_playlists_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            )

            else -> {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                )

                val toAdd = remember(playlists, selected, initiallyPresent) {
                    playlists.filter { it.playlistId in selected && it.playlistId !in initiallyPresent }
                }
                val toRemove = remember(playlists, selected, initiallyPresent) {
                    playlists.filter { it.playlistId !in selected && it.playlistId in initiallyPresent }
                }
                val hasChanges = toAdd.isNotEmpty() || toRemove.isNotEmpty()

                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(displayedPlaylists, key = { it.playlistId }) { playlist ->
                        val isTicked = playlist.playlistId in selected
                        val wasPresent = playlist.playlistId in initiallyPresent
                        val isChordplay = playlist.playlistId in chordplayPlaylistIds
                        PlaylistRow(
                            playlist = playlist,
                            selected = isTicked,
                            wasInitiallyThere = wasPresent,
                            isChordPlayPlaylist = isChordplay,
                            onClick = {
                                userToggledPlaylists.add(playlist.playlistId)
                                selected = if (isTicked) {
                                    selected - playlist.playlistId
                                } else {
                                    selected + playlist.playlistId
                                }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        song?.videoId?.let { vid ->
                            toAdd.forEach { pl -> PlaylistMembershipCache.add(pl.playlistId, vid) }
                            toRemove.forEach { pl -> PlaylistMembershipCache.remove(pl.playlistId, vid) }
                        }
                        if (onCommit != null) {
                            onCommit(toAdd, toRemove)
                        } else {
                            onAdd(toAdd)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                ) {
                    Text(
                        text = when {
                            hasChanges && toAdd.isNotEmpty() && toRemove.isNotEmpty() ->
                                "${stringResource(R.string.done)} (+${toAdd.size}, -${toRemove.size})"
                            hasChanges && toAdd.isNotEmpty() ->
                                "${stringResource(R.string.done)} (+${toAdd.size})"
                            hasChanges && toRemove.isNotEmpty() ->
                                "${stringResource(R.string.done)} (-${toRemove.size})"
                            else -> stringResource(R.string.done)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PlaylistRow(
    playlist: UserPlaylist,
    selected: Boolean,
    wasInitiallyThere: Boolean,
    isChordPlayPlaylist: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = playlist.thumbnailUrl.artworkAt(ROW_ART_PX),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(7.dp))
                .thumbnailBorder(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isChordPlayPlaylist) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "ChordPlay",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            if (playlist.subtitle.isNotBlank()) {
                Text(
                    text = playlist.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            selected && wasInitiallyThere -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF34C759).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.in_playlist),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF34C759),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            !selected && wasInitiallyThere -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RemoveCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.will_remove),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            selected && !wasInitiallyThere -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.will_add),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
        }
        Icon(
            imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp),
        )
    }
}
@Composable
private fun NewPlaylistForm(
    onBack: (() -> Unit)?,
    onCreate: (String, PlaylistPrivacy) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf(PlaylistPrivacy.PRIVATE) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // The form exists to be typed into; opening it with the keyboard already
    // up saves the tap that would otherwise always follow.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val submit: () -> Unit = {
        if (name.isNotBlank()) {
            focusManager.clearFocus()
            onCreate(name, privacy)
        }
    }

    // As above: the keyboard is up from the moment this opens, and "Create
    // playlist" is below the fold without this.
    Column(
        modifier
            .fillMaxWidth()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (onBack != null) 8.dp else 22.dp, end = 22.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            onBack?.let {
                IconButton(onClick = it) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.new_playlist),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.saved_to_youtube_music_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(11.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (name.isEmpty()) {
                    Text(
                        text = stringResource(R.string.playlist_name),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }
            if (name.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { name = "" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.clear_name),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        SheetHeading(stringResource(R.string.who_can_see_it).uppercase(Locale.getDefault()))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlaylistPrivacy.entries.forEach { option ->
                PrivacyPill(
                    icon = option.icon,
                    label = option.label,
                    selected = option == privacy,
                    onClick = { privacy = option },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = submit,
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
        ) {
            Text(stringResource(R.string.create_playlist))
        }
        Spacer(Modifier.height(28.dp))
    }
}

/**
 * The rename panel of [BrowseActionsSheet], which swaps itself out for this
 * rather than opening a dialog over itself — same reason the create form lives
 * inside [PlaylistPickerSheet].
 */
@Composable
internal fun RenamePlaylistForm(
    playlist: UserPlaylist,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf(playlist.title) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val submit: () -> Unit = {
        if (name.isNotBlank()) {
            focusManager.clearFocus()
            onRename(name)
        }
    }

    // The form opens with the keyboard already up, which on a bottom sheet
    // would otherwise sit over the button the form exists to reach.
    Column(
        modifier
            .fillMaxWidth()
            .imePadding(),
    ) {
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
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = stringResource(R.string.rename_playlist),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(11.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
            )
        }
        Button(
            onClick = submit,
            enabled = name.isNotBlank() && name != playlist.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
        ) {
            Text(stringResource(R.string.save_name))
        }
        Spacer(Modifier.height(28.dp))
    }
}

/**
 * The glyph that says what a visibility actually means — a padlock, a shared
 * link, a globe. Three words that all sound like degrees of the same thing
 * read much faster as three different shapes.
 */
private val PlaylistPrivacy.icon: ImageVector
    get() = when (this) {
        PlaylistPrivacy.PRIVATE -> Icons.Rounded.Lock
        PlaylistPrivacy.UNLISTED -> Icons.Rounded.Link
        PlaylistPrivacy.PUBLIC -> Icons.Rounded.Public
    }

/** The search filters' pill, carrying an icon ahead of its label. */
@Composable
private fun PrivacyPill(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .clickable(onClick = onClick)
            .padding(start = 11.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = content,
        )
    }
}
