package com.music.bitchord.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.PushPin
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
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

private val UserPlaylist.id: String get() = playlistId

private fun UserPlaylist.isPinned(pinned: List<String>): Boolean =
    id in pinned || browseId in pinned || "VL$id" in pinned || (id.removePrefix("VL") in pinned)

enum class PlaylistSortOrder(val label: String) {
    A_TO_Z("A-Z"),
    Z_TO_A("Z-A"),
    DEFAULT("Default");

    fun next(): PlaylistSortOrder = when (this) {
        A_TO_Z -> Z_TO_A
        Z_TO_A -> DEFAULT
        DEFAULT -> A_TO_Z
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
    pinnedPlaylists: List<String> = emptyList(),
    onDismiss: (() -> Unit)? = null,
    onCommit: ((toAdd: List<UserPlaylist>, toRemove: List<UserPlaylist>) -> Unit)? = null,
) {
    val chordplayPlaylistIds by AppSettings.chordplayPlaylistIds.collectAsStateWithLifecycle()
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val glassSupported = isGlassSupported()
    val haptics = LocalHapticFeedback.current

    var creating by remember { mutableStateOf(startCreating) }
    var playlistSearchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(PlaylistSortOrder.DEFAULT) }
    val listState = rememberLazyListState()

    var isCheckingMembership by remember(song?.videoId) { mutableStateOf(song != null) }

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

    // When playlists finishes loading or changes, update initiallyPresent from cache if user hasn't toggled yet
    LaunchedEffect(playlists, song?.videoId) {
        val vid = song?.videoId ?: return@LaunchedEffect
        if (playlists.isNotEmpty() && initiallyPresent.isEmpty() && userToggledPlaylists.isEmpty()) {
            val fromCache = playlists.filter { pl -> PlaylistMembershipCache.contains(pl.playlistId, vid) }
                .map { it.playlistId }.toSet()
            if (fromCache.isNotEmpty()) {
                initiallyPresent = fromCache
                selected = fromCache
            }
        }
    }

    // Fast, single-call membership check using YouTube Music's native get_add_to_playlist endpoint.
    LaunchedEffect(song?.videoId) {
        val target = song ?: return@LaunchedEffect
        isCheckingMembership = true
        withContext(Dispatchers.IO) {
            val localIds = com.music.bitchord.data.spotify.LocalPlaylistStore.playlists.value
                .filter { pl -> pl.songs.any { it.videoId == target.videoId } }
                .map { it.id }
                .toSet()

            val result = YtMusicRepository.getPlaylistsContainingSong(target.videoId)
            val containingIds = result.getOrNull()
            if (containingIds != null) {
                // Safely reconcile remote with cache without wiping in-flight pending adds!
                PlaylistMembershipCache.reconcileRemote(target.videoId, containingIds, playlists)

                val effectiveContaining = (containingIds + localIds).filter { plId ->
                    PlaylistMembershipCache.contains(plId, target.videoId)
                }.toSet()

                withContext(Dispatchers.Main) {
                    initiallyPresent = effectiveContaining
                    val unmodified = effectiveContaining.filter { it !in userToggledPlaylists }
                    val toggledSelected = selected.filter { it in userToggledPlaylists }
                    selected = (unmodified + toggledSelected).toSet()
                    isCheckingMembership = false
                }
            } else {
                // If offline or on failure, fall back to LocalPlaylistStore + PlaylistMembershipCache
                val cached = playlists
                    .filter { pl ->
                        pl.playlistId in localIds ||
                            pl.playlistId.removePrefix("VL") in localIds ||
                            PlaylistMembershipCache.contains(pl.playlistId, target.videoId)
                    }
                    .map { it.playlistId }
                    .toSet()
                withContext(Dispatchers.Main) {
                    initiallyPresent = cached
                    val unmodified = cached.filter { it !in userToggledPlaylists }
                    val toggledSelected = selected.filter { it in userToggledPlaylists }
                    selected = (unmodified + toggledSelected).toSet()
                    isCheckingMembership = false
                }
            }
        }
    }

    val displayedPlaylists = remember(playlists, playlistSearchQuery) {
        if (playlistSearchQuery.isBlank()) playlists
        else playlists.filter { it.title.contains(playlistSearchQuery, ignoreCase = true) }
    }

    val sortedPlaylists = remember(displayedPlaylists, pinnedPlaylists, sortOrder, initiallyPresent) {
        val inPlaylistComparator = compareByDescending<UserPlaylist> {
            it.playlistId in initiallyPresent || PlaylistMembershipCache.contains(it.playlistId, song?.videoId.orEmpty())
        }
        val pinnedComparator = compareByDescending<UserPlaylist> {
            it.id in pinnedPlaylists || it.isPinned(pinnedPlaylists)
        }
        when (sortOrder) {
            PlaylistSortOrder.A_TO_Z -> displayedPlaylists.sortedWith(
                pinnedComparator.thenBy { it.title.lowercase(Locale.getDefault()) }
            )
            PlaylistSortOrder.Z_TO_A -> displayedPlaylists.sortedWith(
                pinnedComparator.thenByDescending { it.title.lowercase(Locale.getDefault()) }
            )
            PlaylistSortOrder.DEFAULT -> displayedPlaylists.sortedWith(
                inPlaylistComparator.then(pinnedComparator)
            )
        }
    }

    if (creating) {
        NewPlaylistForm(
            onBack = if (startCreating) null else ({ creating = false }),
            onCreate = onCreate,
            onDismiss = onDismiss,
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ),
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            ),
    ) {
        // Track Header or Sheet Header with Circular Cross button at top right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (song != null) {
                AsyncImage(
                    model = song.artworkAt(ROW_ART_PX),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .thumbnailBorder(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    ExplicitSongTitle(
                        song = song,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.your_playlists).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
            }

            if (onDismiss != null) {
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .then(
                            if (liquidGlass && glassSupported) {
                                Modifier.lightweightLiquidGlass(
                                    shape = CircleShape,
                                    fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                )
                            } else {
                                Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                            }
                        )
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (song != null) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(if (song != null) R.string.add_to_playlist else R.string.your_playlists)
                    .uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            if (isCheckingMembership && song != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.checking),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // Action Bar Row: Circular '+' Button | Pill Search Bar | Circular Sort Icon Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Left: Circular + Button (matches playlist add song round button)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .then(
                        if (liquidGlass && glassSupported) {
                            Modifier.lightweightLiquidGlass(
                                shape = CircleShape,
                                fallbackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            )
                        } else {
                            Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f), CircleShape)
                        }
                    )
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        creating = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.new_playlist),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }

            // Center: Pill-Shaped Search Bar
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(CircleShape)
                    .then(
                        if (liquidGlass && glassSupported) {
                            Modifier.lightweightLiquidGlass(
                                shape = CircleShape,
                                fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            )
                        } else {
                            Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        }
                    )
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (playlistSearchQuery.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_playlists),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                        modifier = Modifier.size(22.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.clear_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }

            // Right: Circular Sort / Filter Icon Button
            val isSorted = sortOrder != PlaylistSortOrder.DEFAULT
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .then(
                        if (liquidGlass && glassSupported) {
                            Modifier.lightweightLiquidGlass(
                                shape = CircleShape,
                                fallbackColor = if (isSorted) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            )
                        } else {
                            Modifier
                                .background(
                                    if (isSorted) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSorted) MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                                    else Color.White.copy(alpha = 0.08f),
                                    shape = CircleShape,
                                )
                        }
                    )
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        sortOrder = sortOrder.next()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Sort,
                    contentDescription = "Sort (${sortOrder.label})",
                    tint = if (isSorted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
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
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                )

                val toAdd = remember(playlists, selected, initiallyPresent) {
                    playlists.filter { it.playlistId in selected && it.playlistId !in initiallyPresent }
                }
                val toRemove = remember(playlists, selected, initiallyPresent) {
                    playlists.filter { it.playlistId !in selected && it.playlistId in initiallyPresent }
                }
                val hasChanges = toAdd.isNotEmpty() || toRemove.isNotEmpty()

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                ) {
                    items(sortedPlaylists, key = { it.playlistId }) { playlist ->
                        val isTicked = playlist.playlistId in selected
                        val wasPresent = playlist.playlistId in initiallyPresent
                        val isChordplay = playlist.playlistId in chordplayPlaylistIds
                        val isPinned = playlist.isPinned(pinnedPlaylists)
                        PlaylistRow(
                            playlist = playlist,
                            selected = isTicked,
                            wasInitiallyThere = wasPresent,
                            isChordPlayPlaylist = isChordplay,
                            isPinned = isPinned,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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

                // Centered Floating Pill Done Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            song?.videoId?.let { vid ->
                                toAdd.forEach { pl -> PlaylistMembershipCache.markPendingAdd(pl.playlistId, vid) }
                                toRemove.forEach { pl -> PlaylistMembershipCache.markPendingRemove(pl.playlistId, vid) }
                            }
                            if (onCommit != null) {
                                onCommit(toAdd, toRemove)
                            } else {
                                onAdd(toAdd)
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp,
                        ),
                        modifier = Modifier
                            .height(46.dp)
                            .widthIn(min = 160.dp, max = 260.dp)
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                            ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
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
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: UserPlaylist,
    selected: Boolean,
    wasInitiallyThere: Boolean,
    isChordPlayPlaylist: Boolean = false,
    isPinned: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = playlist.thumbnailUrl.artworkAt(ROW_ART_PX),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .thumbnailBorder(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isPinned) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                            .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(11.dp),
                            )
                            Text(
                                text = "PINNED",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, letterSpacing = 0.5.sp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                if (isChordPlayPlaylist) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                            .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f), RoundedCornerShape(6.dp))
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
                        .clip(CircleShape)
                        .background(Color(0xFF34C759).copy(alpha = 0.16f))
                        .border(0.5.dp, Color(0xFF34C759).copy(alpha = 0.35f), CircleShape)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.in_playlist),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF34C759),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            !selected && wasInitiallyThere -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.16f))
                        .border(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f), CircleShape)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RemoveCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.will_remove),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            selected && !wasInitiallyThere -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.will_add),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
        }
        Icon(
            imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun NewPlaylistForm(
    onBack: (() -> Unit)?,
    onCreate: (String, PlaylistPrivacy) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf(PlaylistPrivacy.PRIVATE) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val glassSupported = isGlassSupported()

    // The form exists to be typed into; opening it with the keyboard already
    // up saves the tap that would otherwise always follow.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val submit: () -> Unit = {
        if (name.isNotBlank()) {
            focusManager.clearFocus()
            onCreate(name, privacy)
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (onBack != null) 16.dp else 22.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            onBack?.let {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .clickable(onClick = it),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.new_playlist),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.saved_to_youtube_music_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onDismiss != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .then(
                            if (liquidGlass && glassSupported) {
                                Modifier.lightweightLiquidGlass(
                                    shape = CircleShape,
                                    fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                )
                            } else {
                                Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                            }
                        )
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .then(
                    if (liquidGlass && glassSupported) {
                        Modifier.lightweightLiquidGlass(
                            shape = RoundedCornerShape(14.dp),
                            fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        )
                    } else {
                        Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    }
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (name.isEmpty()) {
                    Text(
                        text = stringResource(R.string.playlist_name),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.who_can_see_it).uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 22.dp, top = 4.dp, bottom = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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

        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = submit,
                enabled = name.isNotBlank(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .height(46.dp)
                    .widthIn(min = 180.dp, max = 280.dp),
            ) {
                Text(
                    text = stringResource(R.string.create_playlist),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
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
    onDismiss: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(playlist.title) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val glassSupported = isGlassSupported()

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
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.rename_playlist),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (onDismiss != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .then(
                            if (liquidGlass && glassSupported) {
                                Modifier.lightweightLiquidGlass(
                                    shape = CircleShape,
                                    fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                )
                            } else {
                                Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                            }
                        )
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .then(
                    if (liquidGlass && glassSupported) {
                        Modifier.lightweightLiquidGlass(
                            shape = RoundedCornerShape(14.dp),
                            fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        )
                    } else {
                        Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    }
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = submit,
                enabled = name.isNotBlank() && name != playlist.title,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .height(46.dp)
                    .widthIn(min = 180.dp, max = 280.dp),
            ) {
                Text(
                    text = stringResource(R.string.save_name),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
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
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val glassSupported = isGlassSupported()
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(MaterialTheme.colorScheme.primary)
                } else if (liquidGlass && glassSupported) {
                    Modifier.lightweightLiquidGlass(
                        shape = CircleShape,
                        fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    )
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                }
            )
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
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
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = content,
        )
    }
}
