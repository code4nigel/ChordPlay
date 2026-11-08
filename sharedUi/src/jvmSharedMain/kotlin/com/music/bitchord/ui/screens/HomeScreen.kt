package com.music.bitchord.ui.screens

import com.music.bitchord.ui.components.contextClick
import com.music.bitchord.ui.components.AnimatedDiya
import com.music.bitchord.ui.utils.FestivalTheme
import com.music.bitchord.ui.AppUi
import com.music.bitchord.sharedui.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.music.bitchord.ui.components.longPressMenuClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import com.music.bitchord.ui.components.ShelfRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.settings.AppDesignStyle
import com.music.bitchord.ui.components.HomeMood
import com.music.bitchord.ui.components.HomeSpeedDial
import com.music.bitchord.ui.components.SpeedDialSectionHeader
import com.music.bitchord.ui.components.MoodFilterChips
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Storage
import com.music.bitchord.ui.icons.BitChordIcons
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.HEADER_ART_PX
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import java.util.Locale
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.settings.LibraryViewType
import com.music.bitchord.data.settings.HomeShelfKey
import com.music.bitchord.ui.components.HERO_CARD_RATIO
import com.music.bitchord.ui.components.MessageState
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.RowMoreButton
import com.music.bitchord.ui.components.PlayingAccent
import com.music.bitchord.ui.components.PullToRefresh
import com.music.bitchord.ui.components.SHELF_CARD_WIDTH
import com.music.bitchord.ui.components.SearchPlayingBars
import com.music.bitchord.ui.components.SignInBanner
import com.music.bitchord.ui.components.feedMoreSkeleton
import com.music.bitchord.ui.components.feedSkeleton
import com.music.bitchord.ui.components.recentlyPlayedSkeleton
import com.music.bitchord.ui.components.heroCardWidth
import com.music.bitchord.ui.components.thumbnailBorder
import com.music.bitchord.ui.components.trackColumnWidth
import com.music.bitchord.ui.player.MeshGradientBackground
import com.music.bitchord.ui.player.MeshPalette

/** How far the Listen now heading and feed sit above the shared top inset. */
private val HOME_TITLE_LIFT = 28.dp

private const val RECENTS_TITLE = "Recents"
private const val RECENT_TRACKS_PER_COLUMN = 4

private fun ShelfItem.matchesCurrentlyPlaying(song: Song?): Boolean {
    song ?: return false
    if (videoId == null) return false
    return videoId == song.videoId || (title == song.title && subtitle == song.artist)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState<List<HomeShelf>>,
    listState: LazyListState,
    /** The tapped item and the recommendation shelf it came from. */
    onItemClick: (ShelfItem, String) -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    /** The page's heading, or null for none — the desktop's pages carry no heading. */
    title: String?,
    signedIn: Boolean = true,
    onSignIn: (() -> Unit)? = null,
    /**
     * Holding a card rather than tapping it. Every card on the feed answers,
     * whichever kind it is: the caller reads the item the same way it does for
     * a tap, so a track card opens the track menu and a card that points at a
     * collection opens the album / playlist one.
     */
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    // Explore doesn't page — only Home has a continuation worth following.
    onLoadMore: (() -> Unit)? = null,
    loadingMore: Boolean = false,
    recentlyPlayedLoading: Boolean = false,
    /**
     * Whether the feed's first shelf gets the big full-bleed cards. The phone
     * leads with them; on a window as wide as a desktop's they are posters, so
     * there every shelf is the ordinary row.
     */
    leadHero: Boolean = true,
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    onLuckyDialClick: (() -> Unit)? = null,
    onPlayMixClick: ((ShelfItem) -> Unit)? = null,
) {
    val recentsViewType by AppUi.host.homeRecentsViewType.collectAsStateWithLifecycle()
    val designStyle by AppUi.host.appDesignStyle.collectAsStateWithLifecycle()
    val speedDialLayoutMode by AppUi.host.speedDialLayoutMode.collectAsStateWithLifecycle()
    val homeShelfOrder by AppUi.host.homeShelfOrder.collectAsStateWithLifecycle()
    val disabledHomeShelves by AppUi.host.disabledHomeShelves.collectAsStateWithLifecycle()
    var selectedMood by remember { mutableStateOf(HomeMood.ALL) }

    val pinnedSpeedDial by AppUi.host.pinnedSpeedDial.collectAsStateWithLifecycle()
    val pinnedIds = remember(pinnedSpeedDial) {
        (pinnedSpeedDial.mapNotNull { it.videoId } + pinnedSpeedDial.mapNotNull { it.browseId }).toSet()
    }

    val speedDialItems = remember(state, designStyle, selectedMood, pinnedSpeedDial) {
        if (designStyle == AppDesignStyle.CHORDPLAY && state is UiState.Success) {
            val shelves = state.data
            val shelfMixes = shelves.flatMap { shelf ->
                shelf.items.filter { item ->
                    val bId = item.browseId.orEmpty()
                    bId.startsWith("RD") || bId.startsWith("VLRD") ||
                        shelf.title.contains("mix", ignoreCase = true) ||
                        item.subtitle.contains("mix", ignoreCase = true) ||
                        item.subtitle.contains("radio", ignoreCase = true)
                }
            }
            val seedTracks = shelves
                .filter { it.title.contains("listen again", true) || it.title.contains("quick", true) || it.title.equals(RECENTS_TITLE, true) }
                .flatMap { it.items }
                .filter { it.videoId != null }
            val synthMixes = seedTracks.take(4).mapNotNull { t ->
                val vid = t.videoId ?: return@mapNotNull null
                val artist = t.subtitle.substringBefore(" • ").trim().ifBlank { t.title }
                ShelfItem(
                    title = "$artist Mix",
                    subtitle = "Mix • Based on ${t.title}",
                    thumbnailUrl = t.thumbnailUrl,
                    videoId = null,
                    browseId = "RDAMVM$vid",
                )
            }
            val allMixes = (shelfMixes + synthMixes).distinctBy { it.browseId ?: it.title }
            val favTracks = shelves.filter { !it.title.equals(RECENTS_TITLE, ignoreCase = true) }.flatMap { it.items }.filter { it.videoId != null }
            val recentTracks = shelves.firstOrNull { it.title.equals(RECENTS_TITLE, ignoreCase = true) }?.items.orEmpty()
            val rankedTracks = (favTracks + recentTracks).distinctBy { it.videoId ?: it.title }
            val collections = shelves.flatMap { it.items }.filter { it.browseId != null && !it.browseId!!.startsWith("RD") && !it.browseId!!.startsWith("VLRD") }.distinctBy { it.browseId }

            val interleaved = buildList {
                addAll(pinnedSpeedDial)
                var mIdx = 0; var tIdx = 0; var cIdx = 0
                while (size < 26 && (mIdx < allMixes.size || tIdx < rankedTracks.size || cIdx < collections.size)) {
                    if (mIdx < allMixes.size) add(allMixes[mIdx++])
                    if (tIdx < rankedTracks.size) add(rankedTracks[tIdx++])
                    if (cIdx < collections.size) add(collections[cIdx++])
                    if (tIdx < rankedTracks.size) add(rankedTracks[tIdx++])
                }
            }.distinctBy { it.videoId ?: it.browseId ?: it.title }

            if (selectedMood == HomeMood.ALL || selectedMood.keywords.isEmpty()) interleaved
            else {
                val moodFiltered = interleaved.filter { item ->
                    selectedMood.keywords.any { kw -> item.title.contains(kw, true) || item.subtitle.contains(kw, true) }
                }
                if (moodFiltered.size >= 5) (pinnedSpeedDial + moodFiltered).distinctBy { it.videoId ?: it.browseId ?: it.title } else interleaved
            }
        } else emptyList()
    }

    val showSpeedDial = speedDialItems.isNotEmpty() && HomeShelfKey.SPEED_DIAL !in disabledHomeShelves
    val speedDialIndex = homeShelfOrder.indexOf(HomeShelfKey.SPEED_DIAL)

    val orderedShelves = remember(state, designStyle, selectedMood, disabledHomeShelves, homeShelfOrder) {
        if (state !is UiState.Success) emptyList()
        else {
            val base = if (designStyle == AppDesignStyle.CHORDPLAY) selectedMood.filterShelves(state.data) else state.data
            base
                .filter { it.toShelfKey() !in disabledHomeShelves }
                .sortedBy { shelf ->
                    val idx = homeShelfOrder.indexOf(shelf.toShelfKey())
                    if (idx == -1) 999 else idx
                }
        }
    }

    val beforeSpeedDialShelves = remember(orderedShelves, speedDialIndex, showSpeedDial, homeShelfOrder) {
        if (speedDialIndex == -1 || !showSpeedDial) emptyList()
        else orderedShelves.filter {
            val idx = homeShelfOrder.indexOf(it.toShelfKey())
            idx in 0 until speedDialIndex
        }
    }

    val afterSpeedDialShelves = remember(orderedShelves, beforeSpeedDialShelves, speedDialIndex, showSpeedDial) {
        if (speedDialIndex == -1 || !showSpeedDial) orderedShelves
        else orderedShelves.filterNot { it in beforeSpeedDialShelves }
    }

    PullToRefresh(
        refreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // The heading sits higher than the other tabs' first row: pull the
            // whole feed up by [HOME_TITLE_LIFT] with it.
            contentPadding = if (title != null) {
                PaddingValues(
                    top = (contentPadding.calculateTopPadding() - HOME_TITLE_LIFT).coerceAtLeast(0.dp),
                    bottom = contentPadding.calculateBottomPadding(),
                )
            } else {
                contentPadding
            },
        ) {
            if (title != null) {
                if (designStyle == AppDesignStyle.CHORDPLAY) {
                    item(key = "chordplay_header") {
                        ChordPlayHomeHeader()
                    }
                } else {
                    item(key = "home_title") { HomeTitle(title) }
                }
            }
            if (designStyle == AppDesignStyle.CHORDPLAY) {
                item(key = "mood_chips") {
                    MoodFilterChips(
                        selectedMood = selectedMood,
                        onMoodSelect = { selectedMood = it },
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            if (!signedIn && onSignIn != null) {
                item {
                    SignInBanner(onSignIn = onSignIn, modifier = Modifier.padding(bottom = 8.dp))
                }
            }
            when (state) {
                is UiState.Loading -> {
                    if (recentlyPlayedLoading) {
                        recentlyPlayedSkeleton(listLayout = recentsViewType == LibraryViewType.LIST)
                        // Recents owns the leading layout while its request is
                        // pending, so the feed behind it starts with ordinary
                        // shelf placeholders rather than another hero card.
                        feedSkeleton(firstIsHero = false)
                    } else {
                        feedSkeleton(firstIsHero = leadHero)
                    }
                }
                is UiState.Error -> item {
                    MessageState(state.message, actionLabel = stringResource(Res.string.retry), onAction = onRetry)
                }
                is UiState.Success -> {
                    if (recentlyPlayedLoading) {
                        recentlyPlayedSkeleton(listLayout = recentsViewType == LibraryViewType.LIST)
                    }
                    // The loading skeleton already owns the hero slot. Until
                    // Recently Played lands, every real shelf must retain its
                    // compact-card layout instead of briefly becoming a hero.

                    if (beforeSpeedDialShelves.isNotEmpty()) {
                        itemsIndexedShelves(
                            shelves = beforeSpeedDialShelves,
                            onItemClick = onItemClick,
                            onItemLongPress = onItemLongPress,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            firstIsHero = false,
                            recentsViewType = recentsViewType,
                            onRecentsViewTypeToggle = {
                                AppUi.host.setHomeRecentsViewType(
                                    if (recentsViewType == LibraryViewType.LIST) LibraryViewType.GRID else LibraryViewType.LIST
                                )
                            },
                        )
                    }

                    if (showSpeedDial) {
                        item(key = "home_speed_dial_header") {
                            SpeedDialSectionHeader(
                                layoutMode = speedDialLayoutMode,
                                onToggleLayoutMode = {
                                    AppUi.host.setSpeedDialLayoutMode(speedDialLayoutMode.next())
                                },
                            )
                        }
                        item(key = "home_speed_dial") {
                            HomeSpeedDial(
                                items = speedDialItems,
                                pinnedIds = pinnedIds,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                layoutMode = speedDialLayoutMode,
                                onItemClick = { item -> onItemClick(item, "Your Space") },
                                onPlayMixClick = onPlayMixClick,
                                onLuckyDialClick = {
                                    if (onLuckyDialClick != null) {
                                        onLuckyDialClick()
                                    } else {
                                        val candidate = (speedDialItems.filter { it.videoId != null }.shuffled().firstOrNull()
                                            ?: speedDialItems.shuffled().firstOrNull())
                                        if (candidate != null) {
                                            onItemClick(candidate, "Roll a Dice")
                                        }
                                    }
                                },
                                onItemLongPress = onItemLongPress,
                                modifier = Modifier.padding(bottom = 6.dp),
                            )
                        }
                    }

                    if (afterSpeedDialShelves.isNotEmpty()) {
                        itemsIndexedShelves(
                            shelves = afterSpeedDialShelves,
                            onItemClick = onItemClick,
                            onItemLongPress = onItemLongPress,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            firstIsHero = false,
                            recentsViewType = recentsViewType,
                            onRecentsViewTypeToggle = {
                                AppUi.host.setHomeRecentsViewType(
                                    if (recentsViewType == LibraryViewType.LIST) LibraryViewType.GRID else LibraryViewType.LIST
                                )
                            },
                        )
                    }
                    if (loadingMore) feedMoreSkeleton()
                }
            }
        }
    }

    if (onLoadMore != null && state is UiState.Success) {
        val loadMore by rememberUpdatedState(onLoadMore)
        // Re-checked on every layout change, rather than on the rising edge of
        // "the tail is in view". A page that appends only a shelf or two leaves
        // the list still near its end, so an edge-triggered effect would never
        // fire a second time: the feed dead-ended at the bottom with no
        // skeleton and no request in flight to explain it. Restarting on
        // [loadingMore] re-checks the moment a page settles, so the next one is
        // asked for while the tail is still on screen to show it loading.
        LaunchedEffect(listState, loadingMore) {
            snapshotFlow {
                val layout = listState.layoutInfo
                (layout.visibleItemsInfo.lastOrNull()?.index ?: -1) to layout.totalItemsCount
            }.collect { (lastVisible, total) ->
                if (!loadingMore && total > 0 && lastVisible >= total - 3) loadMore()
            }
        }
    }
}

/**
 * Recents mirrors the artist page's top-tracks pager. Any other lead shelf keeps
 * Apple's full-bleed treatment, while the remaining shelves use square cards.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedShelves(
    shelves: List<HomeShelf>,
    onItemClick: (ShelfItem, String) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    currentSong: Song?,
    isPlaying: Boolean,
    firstIsHero: Boolean = true,
    recentsViewType: LibraryViewType,
    onRecentsViewTypeToggle: () -> Unit,
) {
    shelves.forEachIndexed { index, shelf ->
        item(key = shelf.title + index) {
            val openItem: (ShelfItem) -> Unit = { item -> onItemClick(item, shelf.title) }
            if (shelf.title.equals(RECENTS_TITLE, ignoreCase = true) || shelf.title.equals("Quick picks", ignoreCase = true)) {
                RecentShelf(
                    shelf = shelf,
                    onItemClick = openItem,
                    onItemLongPress = onItemLongPress,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    viewType = recentsViewType,
                    onViewTypeToggle = onRecentsViewTypeToggle,
                )
            } else if (index == 0 && firstIsHero) {
                HeroShelf(
                    shelf = shelf,
                    onItemClick = openItem,
                    onItemLongPress = onItemLongPress,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                )
            } else {
                Shelf(
                    shelf = shelf,
                    onItemClick = openItem,
                    onItemLongPress = onItemLongPress,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                )
            }
        }
    }
}

/** The same four-rows-per-page treatment used by an artist's Top songs. */
@Composable
private fun RecentShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    currentSong: Song?,
    isPlaying: Boolean,
    viewType: LibraryViewType,
    onViewTypeToggle: () -> Unit,
) {
    Column(Modifier.padding(bottom = 26.dp)) {
        RecentSectionHeader(
            title = shelf.title,
            subtitle = shelf.subtitle,
            viewType = viewType,
            onViewTypeToggle = onViewTypeToggle,
        )
        if (viewType == LibraryViewType.LIST) {
            BoxWithConstraints {
                val columnWidth = trackColumnWidth(maxWidth)
                ShelfRow(
                    contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(shelf.items.chunked(RECENT_TRACKS_PER_COLUMN)) { column ->
                        Column(Modifier.width(columnWidth)) {
                            column.forEach { item ->
                                RecentTrackRow(
                                    item = item,
                                    onClick = { onItemClick(item) },
                                    onLongPress = onItemLongPress?.let { { it(item) } },
                                    isCurrent = item.matchesCurrentlyPlaying(currentSong),
                                    isPlaying = item.matchesCurrentlyPlaying(currentSong) && isPlaying,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            BoxWithConstraints {
                val cardWidth = heroCardWidth(maxWidth)
                ShelfRow(
                    contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(shelf.items) { item ->
                        HeroCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            onLongPress = onItemLongPress?.let { { it(item) } },
                            modifier = Modifier.width(cardWidth),
                            isCurrent = item.matchesCurrentlyPlaying(currentSong),
                            isPlaying = item.matchesCurrentlyPlaying(currentSong) && isPlaying,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSectionHeader(
    title: String,
    subtitle: String,
    viewType: LibraryViewType,
    onViewTypeToggle: () -> Unit,
) {
    val displayTitle = localizeShelfTitle(title)
    val displaySubtitle = localizeShelfSubtitle(subtitle)
    Row(
        modifier = Modifier
            .padding(horizontal = PAGE_GUTTER, vertical = 10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (displaySubtitle.isNotBlank()) {
                Text(
                    text = displaySubtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onViewTypeToggle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (viewType == LibraryViewType.LIST) {
                    BitChordIcons.GridView
                } else {
                    BitChordIcons.ListView
                },
                contentDescription = stringResource(
                    if (viewType == LibraryViewType.LIST) {
                        Res.string.switch_to_grid_view
                    } else {
                        Res.string.switch_to_list_view
                    },
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun RecentTrackRow(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    isCurrent: Boolean,
    isPlaying: Boolean,
) {
    CompactTrackRow(
        title = item.title,
        subtitle = item.subtitle,
        thumbnailUrl = item.thumbnailUrl,
        onClick = onClick,
        onLongPress = onLongPress,
        isCurrent = isCurrent,
        isPlaying = isPlaying,
    )
}

/**
 * One row of Recents' four-to-a-column pager. Public so the desktop artist page
 * can list its top songs the same way.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactTrackRow(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .contextClick(onLongPress)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp)) {
            AsyncImage(
                model = thumbnailUrl.artworkAt(ROW_ART_PX),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .thumbnailBorder(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            if (isCurrent && isPlaying) SearchPlayingBars(Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) PlayingAccent else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onLongPress != null) {
            RowMoreButton(onClick = onLongPress)
        }
    }
}

/**
 * Listen now's large heading, with the wordmark stacked above it.
 *
 * Only the home tab carries it — the top bar no longer shows the logo, so this
 * is where the app's mark lives. Its height is taken from the heading's own
 * font size rather than a fixed dp, so the two keep their proportion under the
 * system font scale.
 */
/**
 * Dynamic witty musical greeting based on device local time (Option B):
 * - 05:00 - 11:59: "Ears first, questions later."
 * - 12:00 - 16:59: "Back in the mix, $userName." / "Back in the groove."
 * - 17:00 - 21:59: "Your soundtrack awaits."
 * - 22:00 - 04:59: "Turn it up."
 */
internal fun getMusicalGreeting(userName: String? = null): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Ears first, questions later."
        in 12..16 -> if (!userName.isNullOrBlank()) "Back in the mix, $userName." else "Back in the groove."
        in 17..21 -> "Your soundtrack awaits."
        else -> "Turn it up."
    }
}

/**
 * Dynamic witty musical greetings:
 * 1. "Back in the mix, $userName." / "Back in the mix."
 * 2. "Ears first, questions later."
 * 3. "Your soundtrack awaits."
 * 4. "Turn it up."
 */
private fun getGreetingList(userName: String? = null): List<String> = listOf(
    if (!userName.isNullOrBlank()) "Back in the mix, $userName." else "Back in the mix.",
    "Ears first, questions later.",
    "Your soundtrack awaits.",
    "Turn it up.",
)

/**
 * ChordPlay Living Home Screen dynamic greeting header with cycling greetings.
 */
@Composable
private fun ChordPlayHomeHeader(
    userName: String? = null,
    modifier: Modifier = Modifier,
) {
    val titleStyle = MaterialTheme.typography.headlineMedium
    val logoHeight = with(LocalDensity.current) { (MaterialTheme.typography.titleLarge.fontSize * LOGO_TO_FONT).toDp() }
    val initialGreeting = remember(userName) { getMusicalGreeting(userName) }
    val greetings = remember(userName) {
        listOf(initialGreeting) + (getGreetingList(userName) - initialGreeting)
    }
    var greetingIndex by remember { mutableIntStateOf(0) }

    // Rotate slowly (every 60 seconds) so the greeting stays calm, readable, and premium
    LaunchedEffect(greetings) {
        while (true) {
            delay(60_000L)
            greetingIndex = (greetingIndex + 1) % greetings.size
        }
    }

    val isDiwali = remember { FestivalTheme.isDiwaliActive() }

    Column(modifier = modifier.padding(horizontal = PAGE_GUTTER, vertical = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    FestivalTheme.triggerDiwaliPopup()
                },
        ) {
            if (isDiwali) {
                AnimatedDiya(size = 22.dp)
            }
            Icon(
                painter = painterResource(Res.drawable.ic_logo),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = "ChordPlay",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            if (isDiwali) {
                AnimatedDiya(size = 22.dp)
            }
        }
        Box(
            modifier = Modifier.clickable {
                greetingIndex = (greetingIndex + 1) % greetings.size
            }
        ) {
            AnimatedContent(
                targetState = greetings[greetingIndex % greetings.size],
                transitionSpec = {
                    (slideInVertically(animationSpec = tween(350)) { height -> height / 2 } + fadeIn(animationSpec = tween(350)))
                        .togetherWith(slideOutVertically(animationSpec = tween(250)) { height -> -height / 2 } + fadeOut(animationSpec = tween(250)))
                },
                label = "HomeGreetingRotation",
            ) { greeting ->
                Text(
                    text = greeting,
                    style = titleStyle,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
private fun HomeTitle(title: String, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.displayLarge
    val logoHeight = with(LocalDensity.current) { (style.fontSize * LOGO_TO_FONT).toDp() }
    Column(modifier = modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp)) {
        Icon(
            painter = painterResource(Res.drawable.ic_logo),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(bottom = 14.dp)
                .height(logoHeight)
                .aspectRatio(LOGO_ASPECT),
        )
        Text(
            text = title,
            style = style,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** The wordmark's height against the heading's font size. */
private const val LOGO_TO_FONT = 1.15f

/** ic_logo's viewport, 1024 x 1024 (1:1 aspect ratio). */
private const val LOGO_ASPECT = 1.0f

/**
 * Shared by the home feed, Explore and Library so headings line up across tabs.
 *
 * [onShowAll] is only ever set on Library, whose rows stop at five cards
 * rather than running the shelf's whole length — see [LibraryGridShelf].
 * Home and Explore never pass it, so their heading is unchanged.
 */
@Composable
internal fun SectionHeader(title: String, subtitle: String = "", onShowAll: (() -> Unit)? = null) {
    val displayTitle = localizeShelfTitle(title)
    val displaySubtitle = localizeShelfSubtitle(subtitle)
    Row(
        modifier = Modifier
            .padding(horizontal = PAGE_GUTTER, vertical = 10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (displaySubtitle.isNotBlank()) {
                Text(
                    text = displaySubtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onShowAll != null) {
            Text(
                text = stringResource(Res.string.show_all),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onShowAll)
                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
            )
        }
    }
}
@Composable
internal fun localizeShelfTitle(title: String): String {
    val trimmed = title.trim()
    return when {
        trimmed.equals("Recents", ignoreCase = true) ||
            trimmed.equals("Recently played", ignoreCase = true) ||
            trimmed.equals("Gần đây", ignoreCase = true) ||
            trimmed.equals("最近", ignoreCase = true) ->
            stringResource(Res.string.shelf_recents)
        trimmed.equals("Playlists", ignoreCase = true) ||
            trimmed.equals("Danh sách phát", ignoreCase = true) ||
            trimmed.equals("再生リスト", ignoreCase = true) ->
            stringResource(Res.string.playlists)
        trimmed.equals("Albums", ignoreCase = true) ||
            trimmed.equals("Album", ignoreCase = true) ||
            trimmed.equals("アルバム", ignoreCase = true) ->
            stringResource(Res.string.albums)
        trimmed.equals("Artists", ignoreCase = true) ||
            trimmed.equals("Nghệ sĩ", ignoreCase = true) ||
            trimmed.equals("アーティスト", ignoreCase = true) ->
            stringResource(Res.string.artists)
        trimmed.equals("Subscriptions", ignoreCase = true) ||
            trimmed.equals("Đã đăng ký", ignoreCase = true) ||
            trimmed.equals("登録チャンネル", ignoreCase = true) ->
            stringResource(Res.string.subscriptions)
        trimmed.equals("Trending community playlists", ignoreCase = true) ||
            trimmed.equals("Danh sách phát cộng đồng thịnh hành", ignoreCase = true) ||
            trimmed.equals("Danh sách phát thịnh hành trong cộng đồng người dùng", ignoreCase = true) ||
            trimmed.equals("急上昇のコミュニティ再生リスト", ignoreCase = true) ->
            stringResource(Res.string.shelf_trending_community_playlists)
        trimmed.equals("Featured playlists for you", ignoreCase = true) ||
            trimmed.equals("Danh sách phát đề xuất cho bạn", ignoreCase = true) ||
            trimmed.equals("Danh sách phát nổi bật dành cho bạn", ignoreCase = true) ||
            trimmed.equals("おすすめの再生リスト", ignoreCase = true) ->
            stringResource(Res.string.shelf_featured_playlists_for_you)
        trimmed.equals("Quick picks", ignoreCase = true) ||
            trimmed.equals("Lựa chọn nhanh", ignoreCase = true) ||
            trimmed.equals("Chọn nhanh đài phát", ignoreCase = true) ||
            trimmed.equals("クイック ミックス", ignoreCase = true) ->
            stringResource(Res.string.shelf_quick_picks)
        trimmed.equals("Listen again", ignoreCase = true) ||
            trimmed.equals("Nghe lại", ignoreCase = true) ||
            trimmed.equals("もう一度聴く", ignoreCase = true) ->
            stringResource(Res.string.shelf_listen_again)
        trimmed.equals("Mixed for you", ignoreCase = true) ||
            trimmed.equals("Dành riêng cho bạn", ignoreCase = true) ||
            trimmed.equals("ミックス", ignoreCase = true) ->
            stringResource(Res.string.shelf_mixed_for_you)
        trimmed.startsWith("Similar to", ignoreCase = true) -> {
            val rest = trimmed.substring(10).trim()
            stringResource(Res.string.shelf_similar_to, rest)
        }
        trimmed.startsWith("Tương tự như", ignoreCase = true) -> {
            val rest = trimmed.substring(12).trim()
            stringResource(Res.string.shelf_similar_to, rest)
        }
        trimmed.equals("Forgotten favorites", ignoreCase = true) ||
            trimmed.equals("Giai điệu quen thuộc", ignoreCase = true) ||
            trimmed.equals("よく聴いたお気に入りの曲", ignoreCase = true) ->
            stringResource(Res.string.shelf_forgotten_favorites)
        trimmed.equals("Recommended music videos", ignoreCase = true) ||
            trimmed.equals("Video âm nhạc đề xuất", ignoreCase = true) ||
            trimmed.equals("おすすめのミュージック ビデオ", ignoreCase = true) ->
            stringResource(Res.string.shelf_recommended_music_videos)
        trimmed.equals("From your library", ignoreCase = true) ||
            trimmed.equals("Từ thư viện của bạn", ignoreCase = true) ||
            trimmed.equals("ライブラリから", ignoreCase = true) ->
            stringResource(Res.string.shelf_from_your_library)
        trimmed.equals("Charts", ignoreCase = true) ||
            trimmed.equals("Bảng xếp hạng", ignoreCase = true) ||
            trimmed.equals("チャート", ignoreCase = true) ->
            stringResource(Res.string.shelf_charts)
        trimmed.equals("New releases", ignoreCase = true) ||
            trimmed.equals("Bản phát hành mới", ignoreCase = true) ||
            trimmed.equals("最新リリース", ignoreCase = true) ->
            stringResource(Res.string.shelf_new_releases)
        trimmed.equals("Top music videos", ignoreCase = true) ||
            trimmed.equals("Video âm nhạc hàng đầu", ignoreCase = true) ||
            trimmed.equals("人気のミュージック ビデオ", ignoreCase = true) ->
            stringResource(Res.string.shelf_top_music_videos)
        trimmed.equals("For you", ignoreCase = true) ||
            trimmed.equals("Dành cho bạn", ignoreCase = true) ||
            trimmed.equals("あなたへのおすすめ", ignoreCase = true) ->
            stringResource(Res.string.shelf_for_you)
        trimmed.equals("Hits today", ignoreCase = true) ||
            trimmed.equals("Today's Hits", ignoreCase = true) ||
            trimmed.equals("Bản hit hôm nay", ignoreCase = true) ||
            trimmed.equals("今日のヒット曲", ignoreCase = true) ->
            stringResource(Res.string.shelf_hits_today)
        trimmed.equals("Artists on the rise", ignoreCase = true) ||
            trimmed.equals("Nghệ sĩ đang lên", ignoreCase = true) ->
            stringResource(Res.string.shelf_artists_on_the_rise)
        trimmed.equals("Concerts", ignoreCase = true) ||
            trimmed.equals("Buổi hòa nhạc", ignoreCase = true) ||
            trimmed.equals("コンサート", ignoreCase = true) ->
            stringResource(Res.string.shelf_concerts)
        trimmed.startsWith("Shorts", ignoreCase = true) ||
            trimmed.equals("Krótkie filmy", ignoreCase = true) ||
            trimmed.equals("Shorts nổi bật", ignoreCase = true) ||
            trimmed.equals("ショート", ignoreCase = true) ->
            stringResource(Res.string.shelf_shorts)
        trimmed.equals("Trending", ignoreCase = true) ||
            trimmed.equals("Thịnh hành", ignoreCase = true) ||
            trimmed.equals("急上昇", ignoreCase = true) ->
            stringResource(Res.string.shelf_trending)
        trimmed.equals("Top Picks for You", ignoreCase = true) ||
            trimmed.equals("Top picks for you", ignoreCase = true) ||
            trimmed.equals("Top picks", ignoreCase = true) ->
            stringResource(Res.string.shelf_top_picks_for_you)
        trimmed.equals("Remixes & Edits", ignoreCase = true) ||
            trimmed.equals("Remixes & edits", ignoreCase = true) ||
            trimmed.equals("Remixes and edits", ignoreCase = true) ||
            trimmed.equals("Remixes and edit", ignoreCase = true) ||
            trimmed.equals("Remixes", ignoreCase = true) ->
            stringResource(Res.string.shelf_remixes_edits)
        trimmed.equals("Covers and remixes", ignoreCase = true) ||
            trimmed.equals("Covers & remixes", ignoreCase = true) ->
            stringResource(Res.string.shelf_covers_remixes)
        trimmed.equals("Music Videos for You", ignoreCase = true) ||
            trimmed.equals("Music videos for you", ignoreCase = true) ->
            stringResource(Res.string.shelf_music_videos_for_you)
        trimmed.equals("Music videos", ignoreCase = true) ||
            trimmed.equals("Music Videos", ignoreCase = true) ->
            stringResource(Res.string.shelf_music_videos)
        trimmed.equals("New music videos", ignoreCase = true) ||
            trimmed.equals("New Music Videos", ignoreCase = true) ->
            stringResource(Res.string.shelf_new_music_videos)
        trimmed.equals("Albums and singles", ignoreCase = true) ||
            trimmed.equals("Albums & singles", ignoreCase = true) ->
            stringResource(Res.string.shelf_albums_singles)
        trimmed.equals("New albums and singles and top songs", ignoreCase = true) ||
            trimmed.equals("New albums & singles & top songs", ignoreCase = true) ->
            stringResource(Res.string.shelf_new_albums_singles_top_songs)
        trimmed.equals("New albums and singles", ignoreCase = true) ||
            trimmed.equals("New albums & singles", ignoreCase = true) ->
            stringResource(Res.string.shelf_new_albums_singles)
        trimmed.equals("Top songs", ignoreCase = true) ->
            stringResource(Res.string.shelf_top_songs)
        else -> title
    }
}
@Composable
internal fun localizeShelfSubtitle(subtitle: String): String {
    val trimmed = subtitle.trim()
    return when {
        trimmed.contains("start a radio", ignoreCase = true) ->
            stringResource(Res.string.shelf_start_radio_desc)
        trimmed.contains("fresh drops", ignoreCase = true) ||
            trimmed.contains("Stations, mixes", ignoreCase = true) ->
            stringResource(Res.string.shelf_stations_mixes_desc)
        trimmed.contains("Artist stations", ignoreCase = true) ||
            trimmed.contains("spotlight cards", ignoreCase = true) ->
            stringResource(Res.string.shelf_top_picks_desc)
        trimmed.contains("club mixes", ignoreCase = true) ||
            trimmed.contains("VIPs & reimagined", ignoreCase = true) ||
            trimmed.contains("VIP edits", ignoreCase = true) ->
            stringResource(Res.string.shelf_club_mixes_desc)
        trimmed.contains("16:9 widescreen", ignoreCase = true) ||
            trimmed.contains("widescreen official videos", ignoreCase = true) ->
            stringResource(Res.string.shelf_widescreen_videos_desc)
        trimmed.equals("TOP TUNES RIGHT NOW", ignoreCase = true) ||
            trimmed.equals("Top tunes right now", ignoreCase = true) ||
            trimmed.equals("Giai điệu hàng đầu hiện nay", ignoreCase = true) ||
            trimmed.equals("注目の曲", ignoreCase = true) ->
            stringResource(Res.string.shelf_top_tunes_right_now)
        trimmed.equals("From the community", ignoreCase = true) ||
            trimmed.equals("Từ cộng đồng", ignoreCase = true) ||
            trimmed.equals("コミュニティより", ignoreCase = true) ->
            stringResource(Res.string.shelf_from_the_community)
        trimmed.equals("YouTube Charts", ignoreCase = true) ||
            trimmed.equals("Bảng xếp hạng YouTube", ignoreCase = true) ||
            trimmed.equals("YouTube チャート", ignoreCase = true) ->
            stringResource(Res.string.shelf_youtube_charts)
        else -> subtitle
    }
}

@Composable
internal fun localizeCardSubtitle(subtitle: String): String {
    if (subtitle.isBlank()) return subtitle
    val delimiter = " • "
    val parts = subtitle.split(delimiter)
    val songLabel = stringResource(Res.string.shelf_song_item)
    val singleLabel = stringResource(Res.string.shelf_single)
    val chartLabel = stringResource(Res.string.shelf_chart)
    val playlistLabel = stringResource(Res.string.playlist)
    val localizedParts = parts.map { part ->
        val trimmed = part.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        when {
            trimmed.equals("Song", ignoreCase = true) || trimmed.equals("Titre", ignoreCase = true) ||
                trimmed.equals("Bài hát", ignoreCase = true) || trimmed.equals("曲", ignoreCase = true) -> songLabel
            trimmed.equals("Single", ignoreCase = true) || trimmed.equals("Đĩa đơn", ignoreCase = true) ||
                trimmed.equals("シングル", ignoreCase = true) -> singleLabel
            trimmed.equals("Chart", ignoreCase = true) || trimmed.equals("Bảng xếp hạng", ignoreCase = true) ||
                trimmed.equals("チャート", ignoreCase = true) -> chartLabel
            trimmed.equals("Playlist", ignoreCase = true) || trimmed.equals("Danh sách phát", ignoreCase = true) ||
                trimmed.equals("再生リスト", ignoreCase = true) -> playlistLabel
            lower.endsWith(" views") || lower.endsWith(" view") || lower.endsWith(" lượt xem") ||
                lower.endsWith(" 回視聴") || lower.endsWith("回視聴") -> {
                val count = trimmed.substringBeforeLast(' ', "").trim()
                if (count.any { it.isDigit() }) stringResource(Res.string.card_views_format, count) else part
            }
            lower.endsWith(" plays") || lower.endsWith(" play") || lower.endsWith(" lượt phát") ||
                lower.endsWith(" 回再生") || lower.endsWith("回再生") -> {
                val count = trimmed.substringBeforeLast(' ', "").trim()
                if (count.any { it.isDigit() }) stringResource(Res.string.card_plays_format, count) else part
            }
            lower.endsWith(" songs") || lower.endsWith(" song") || lower.endsWith(" bài hát") ||
                lower.endsWith(" 曲") || lower.endsWith("曲") -> {
                val count = trimmed.substringBeforeLast(' ', "").trim()
                if (count.any { it.isDigit() }) stringResource(Res.string.card_songs_format, count) else part
            }
            else -> part
        }
    }
    return localizedParts.joinToString(delimiter)
}

@Composable
private fun HeroShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    currentSong: Song? = null,
    isPlaying: Boolean = false,
) {
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(shelf.title, shelf.subtitle)
        // Measured rather than taken as a share of the parent, because the card
        // has a ceiling as well as a fraction — see [heroCardWidth]. A fixed
        // width is also the only one of the two the aspect ratio below can turn
        // into a height, so the card keeps its shape however it was arrived at.
        BoxWithConstraints {
            val cardWidth = heroCardWidth(maxWidth)
            ShelfRow(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(shelf.items) { item ->
                    HeroCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        onLongPress = onItemLongPress?.let { { it(item) } },
                        modifier = Modifier.width(cardWidth),
                        isCurrent = item.matchesCurrentlyPlaying(currentSong),
                        isPlaying = item.matchesCurrentlyPlaying(currentSong) && isPlaying,
                    )
                }
            }
        }
    }
}

/** Big card: artwork with the caption laid over a scrim, as on Listen Now. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCard(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
) {
    Box(
        modifier = modifier
            .aspectRatio(HERO_CARD_RATIO)

            .clip(RoundedCornerShape(if (AppUi.host.appDesignStyle.value == AppDesignStyle.CHORDPLAY) 20.dp else 18.dp))
            .thumbnailBorder(RoundedCornerShape(if (AppUi.host.appDesignStyle.value == AppDesignStyle.CHORDPLAY) 20.dp else 18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .contextClick(onLongPress),
    ) {
        AsyncImage(
            model = item.thumbnailUrl.artworkAt(HEADER_ART_PX),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isCurrent && isPlaying) SearchPlayingBars(Modifier.align(Alignment.Center))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                    ),
                )
                .padding(start = 16.dp, end = 16.dp, top = 34.dp, bottom = 14.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                color = if (isCurrent) PlayingAccent else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.subtitle.isNotBlank()) {
                val displaySubtitle = localizeCardSubtitle(item.subtitle)
                Text(
                    text = displaySubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * [leadingCard] rides at the head of the row, ahead of the content — the
 * Library tab's "New playlist" tile, which belongs among the playlists rather
 * than in a bar somewhere above them. [onItemLongPress] opens the album /
 * playlist menu, and is null only where a card points at something with no
 * track list behind it to act on.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Shelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    leadingCard: (@Composable () -> Unit)? = null,
    currentSong: Song? = null,
    isPlaying: Boolean = false,
) {
    val isVideoShelf = shelf.title.contains("video", ignoreCase = true) || shelf.items.any { it.isVideo }
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(shelf.title, shelf.subtitle)
        ShelfRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            leadingCard?.let { card -> item(key = "leading") { card() } }
            items(shelf.items) { item ->
                ShelfCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onLongPress = onItemLongPress?.let { { it(item) } },
                    isCurrent = item.matchesCurrentlyPlaying(currentSong),
                    isPlaying = item.matchesCurrentlyPlaying(currentSong) && isPlaying,
                    isVideo = isVideoShelf || item.isVideo,
                )
            }
        }
    }
}

/**
 * A card that isn't a thing yet — the dashed "New playlist" tile at the head
 * of the Library's playlist row, sized to sit in line with the covers beside
 * it rather than as a button bolted above them.
 */
@Composable
internal fun NewShelfCard(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(SHELF_CARD_WIDTH),
    /** A full-colour mark drawn in place of [icon], for a card that stands for a brand. */
    logo: DrawableResource? = null,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(if (AppUi.host.appDesignStyle.value == AppDesignStyle.CHORDPLAY) 20.dp else 12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (logo != null) {
                // The mark is a disc with the bars cut out, so tinting it
                // leaves the bars clear: white on dark, black on light.
                Icon(
                    painter = painterResource(logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(44.dp),
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * A device-folder card: mesh gradient in the folder's colours with its glyph
 * on top. One composable for local music, WebDAV and SMB rather than three
 * copies of the same box.
 */
@Composable
private fun ServiceCard(colors: List<Color>, trackKey: String, icon: ImageVector) {
    val palette = remember { MeshPalette(colors) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(if (AppUi.host.appDesignStyle.value == AppDesignStyle.CHORDPLAY) 20.dp else 12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        MeshGradientBackground(
            palette = palette,
            trackKey = trackKey,
            continuous = true,
            blurRadius = 24.dp,
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(40.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ShelfCard(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    isVideo: Boolean = false,
    modifier: Modifier = if (isVideo) Modifier.width(220.dp) else Modifier.width(SHELF_CARD_WIDTH),
    /** Set on a Library playlist card that's in [AppSettings.pinnedPlaylists][com.music.bitchord.data.settings.AppSettings.pinnedPlaylists]. */
    isPinned: Boolean = false,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
) {
    Column(
        modifier = modifier
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .contextClick(onLongPress),
    ) {
        when (item.browseId) {
            "local:downloads" -> {
                val palette = remember { MeshPalette(listOf(Color(0xFF1E3C72), Color(0xFF2A5298))) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(if (AppUi.host.appDesignStyle.value == AppDesignStyle.CHORDPLAY) 20.dp else 12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    MeshGradientBackground(
                        palette = palette,
                        trackKey = "local:downloads",
                        continuous = true,
                        blurRadius = 24.dp,
                    )
                    Icon(
                        imageVector = BitChordIcons.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            "local:all" -> ServiceCard(
                colors = listOf(Color(0xFF134E5E), Color(0xFF71B280)),
                trackKey = "local:all",
                icon = Icons.Rounded.LibraryMusic,
            )
            "local:cache" -> ServiceCard(
                colors = listOf(Color(0xFF42275A), Color(0xFF734B6D)),
                trackKey = "local:cache",
                icon = Icons.Rounded.Cached,
            )
            "local:webdav" -> ServiceCard(
                colors = listOf(Color(0xFF3A1C71), Color(0xFFD76D77)),
                trackKey = "local:webdav",
                icon = Icons.Rounded.Folder,
            )
            "local:smb" -> ServiceCard(
                colors = listOf(Color(0xFF0F2027), Color(0xFF2C5364)),
                trackKey = "local:smb",
                icon = Icons.Rounded.Storage,
            )
            else -> {
                val cardShape = RoundedCornerShape(if (isVideo) 16.dp else if (AppUi.host.appDesignStyle.value == AppDesignStyle.CHORDPLAY) 20.dp else 12.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (isVideo) 16f / 9f else 1f)

                        .clip(cardShape)
                        .thumbnailBorder(cardShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (isCurrent && isPlaying) SearchPlayingBars(Modifier.align(Alignment.Center))
                    if (isVideo) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "VIDEO",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 10.sp,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isPinned) {
                Icon(
                    imageVector = BitChordIcons.Pin,
                    contentDescription = stringResource(Res.string.pinned),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) PlayingAccent else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        val displaySubtitle = localizeCardSubtitle(item.subtitle)
        Text(
            text = displaySubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val shelfKeyCache = java.util.concurrent.ConcurrentHashMap<String, HomeShelfKey>()

private fun HomeShelf.toShelfKey(): HomeShelfKey = shelfKeyCache.getOrPut(title) {
    val t = title.lowercase()
    when {
        t.contains("quick pick") || t.equals(RECENTS_TITLE, ignoreCase = true) -> HomeShelfKey.QUICK_PICKS
        t.contains("top pick") -> HomeShelfKey.TOP_PICKS
        t.contains("similar to") || t.contains("similar") || t.contains("tương tự") -> HomeShelfKey.SIMILAR_TO
        t.contains("community") || t.contains("cộng đồng") || t.contains("コミュニティ") -> HomeShelfKey.COMMUNITY_PLAYLISTS
        t.contains("featured playlist") -> HomeShelfKey.FEATURED_PLAYLISTS
        t.contains("chart") || t.contains("trending") || t.contains("bảng xếp hạng") || t.contains("thịnh hành") || t.contains("hits today") || t.contains("today's hits") -> HomeShelfKey.CHARTS
        t.contains("remix") -> HomeShelfKey.REMIXES
        t.contains("video") -> HomeShelfKey.MUSIC_VIDEOS
        t.contains("mixed for you") || t.contains("my mix") || t.contains("mixes") -> HomeShelfKey.MIXED_FOR_YOU
        t.contains("listen again") || t.contains("favorite") || t.contains("rotation") || t.contains("nghe lại") || t.contains("quen thuộc") -> HomeShelfKey.LISTEN_AGAIN
        t.contains("new release") || t.contains("bản phát hành mới") || t.contains("mới") -> HomeShelfKey.NEW_RELEASES
        else -> HomeShelfKey.OTHER
    }
}
