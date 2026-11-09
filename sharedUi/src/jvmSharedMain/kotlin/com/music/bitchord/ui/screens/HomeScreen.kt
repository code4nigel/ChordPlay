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
            val shelfKey = shelf.toShelfKey()
            if (shelfKey == HomeShelfKey.QUICK_PICKS || shelf.title.equals(RECENTS_TITLE, ignoreCase = true)) {
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

    val isFestivalActive by FestivalTheme.isFestivalActive.collectAsStateWithLifecycle()
    val festivalThemeType by FestivalTheme.festivalThemeType.collectAsStateWithLifecycle()

    Column(modifier = modifier.padding(horizontal = PAGE_GUTTER, vertical = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .padding(bottom = 8.dp)
                .then(
                    if (isFestivalActive) {
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                FestivalTheme.triggerFestivalPopup()
                            }
                    } else {
                        Modifier
                    }
                ),
        ) {
            if (isFestivalActive && festivalThemeType == "diwali") {
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
            if (isFestivalActive && festivalThemeType == "diwali") {
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
    val lower = trimmed.lowercase(Locale.ROOT)

    // Check "Similar to" across all 18 languages
    val similarPrefixes = listOf(
        "similar to", "tương tự như", "tương tự", "ähnlich wie", "aehnlich wie",
        "similaire à", "similaire a", "similar a", "simile a", "podobne do",
        "похоже на", "похожие на", "semelhante a", "mirip dengan", "คล้ายกับ",
        "בדומה ל-", "בדומה ל", "דומה ל-", "דומה ל", "şuna benzer:", "şuna benzer", "benzer:",
    )
    for (prefix in similarPrefixes) {
        if (lower.startsWith(prefix)) {
            val rest = trimmed.substring(prefix.length).trim().removePrefix(":").removePrefix("-").trim()
            if (rest.isNotEmpty()) return stringResource(Res.string.shelf_similar_to, rest)
        }
    }
    val similarSuffixes = listOf(
        "に似ている曲", "に似ている", " के समान", " से मिलते-जुलते", " से मिलते जुलते", " benzeri",
    )
    for (suffix in similarSuffixes) {
        if (lower.endsWith(suffix.lowercase(Locale.ROOT))) {
            val rest = trimmed.substring(0, trimmed.length - suffix.length).trim()
            if (rest.isNotEmpty()) return stringResource(Res.string.shelf_similar_to, rest)
        }
    }
    if (trimmed.startsWith("与") || trimmed.startsWith("與")) {
        val candidate = trimmed.removePrefix("与").removePrefix("與")
            .removeSuffix("类似").removeSuffix("類似").removeSuffix("相似").trim()
        if (candidate.isNotEmpty()) {
            return stringResource(Res.string.shelf_similar_to, candidate)
        }
    }

    return when {
        trimmed.equals("Recents", ignoreCase = true) ||
            trimmed.equals("Recently played", ignoreCase = true) ||
            trimmed.equals("Gần đây", ignoreCase = true) ||
            trimmed.equals("最近", ignoreCase = true) ||
            trimmed.equals("Recientes", ignoreCase = true) ||
            trimmed.equals("Récents", ignoreCase = true) ||
            trimmed.equals("Zuletzt gehört", ignoreCase = true) ||
            trimmed.equals("Ostatnio odtwarzane", ignoreCase = true) ||
            trimmed.equals("Baru saja diputar", ignoreCase = true) ||
            trimmed.equals("हाल ही में बजाया गया", ignoreCase = true) ||
            trimmed.equals("Недавно воспроизведенные", ignoreCase = true) ||
            trimmed.equals("最近播放", ignoreCase = true) ||
            trimmed.equals("הושמעו לאחרונה", ignoreCase = true) ||
            trimmed.equals("Son çalınanlar", ignoreCase = true) ||
            trimmed.equals("เล่นล่าสุด", ignoreCase = true) ->
            stringResource(Res.string.shelf_recents)

        lower.contains("quick pick") || lower.contains("quick_pick") ||
            lower.contains("schnelle auswahl") || lower.contains("sélection rapide") || lower.contains("selection rapide") ||
            lower.contains("szybki wybór") || lower.contains("szybki wybor") ||
            lower.contains("selección rápida") || lower.contains("seleccion rapida") ||
            lower.contains("escolhas rápidas") || lower.contains("escolhas rapidas") ||
            lower.contains("pilihan instan") || lower.contains("pilihan cepat") ||
            lower.contains("क्विक पिक्स") || lower.contains("त्वरित चयन") ||
            lower.contains("クイック ミックス") || lower.contains("クイックミックス") || lower.contains("クイックピック") ||
            lower.contains("быстрый выбор") || lower.contains("快速选择") || lower.contains("快速選擇") ||
            lower.contains("בחירות מהירות") || lower.contains("בחירה מהירה") ||
            lower.contains("scelte rapide") || lower.contains("scelta rapida") ||
            lower.contains("hızlı seçimler") || lower.contains("hizli secimler") ||
            lower.contains("เลือกด่วน") || lower.contains("ตัวเลือกด่วน") ||
            lower.contains("lựa chọn nhanh") || lower.contains("chọn nhanh đài phát") ->
            stringResource(Res.string.shelf_quick_picks)

        lower.contains("top pick") || lower.contains("top-auswahl") || lower.contains("top auswahl") ||
            lower.contains("meilleures sélections") || lower.contains("meilleure selection") ||
            lower.contains("najlepszy wybór") || lower.contains("najlepszy wybor") ||
            lower.contains("principais escolhas") || lower.contains("pilihan teratas") ||
            lower.contains("टॉप पिक्स") || lower.contains("главный выбор") || lower.contains("лучший выбор") ||
            lower.contains("为你精选") || lower.contains("為你精選") || lower.contains("特别推荐") || lower.contains("特別推薦") ||
            lower.contains("בחירות המובילות") || lower.contains("scelte migliori") || lower.contains("en iyi seçimler") ||
            lower.contains("ตัวเลือกยอดนิยม") || lower.contains("lựa chọn hàng đầu") ->
            stringResource(Res.string.shelf_top_picks_for_you)

        lower.contains("forgotten favorite") || lower.contains("forgotten favourite") ||
            lower.contains("giai điệu quen thuộc") || lower.contains("よく聴いたお気に入りの曲") ||
            lower.contains("favoritos olvidados") || lower.contains("favoris oubliés") ||
            lower.contains("vergangene favoriten") || lower.contains("zapomniane ulubione") ||
            lower.contains("favoritos esquecidos") || lower.contains("भूले-बिसरे पसंदीदा") ||
            lower.contains("забытые шедевры") || lower.contains("曾经常听的歌曲") || lower.contains("曾經常聽的好歌") ||
            lower.contains("מועדפים שנשכחו") || lower.contains("preferiti dimenticati") ||
            lower.contains("unutulan favoriler") || lower.contains("เพลงโปรดที่ลืมไปแล้ว") ->
            stringResource(Res.string.shelf_forgotten_favorites)

        lower.contains("listen again") || lower.contains("play again") ||
            lower.contains("nochmal anhören") || lower.contains("nochmal anhoeren") ||
            lower.contains("écouter à nouveau") || lower.contains("ecouter a nouveau") ||
            lower.contains("volver a escuchar") || lower.contains("posłuchaj jeszcze raz") || lower.contains("posluchaj jeszcze raz") ||
            lower.contains("ouvir novamente") || lower.contains("dengarkan lagi") || lower.contains("फिर से सुनें") ||
            lower.contains("もう一度聴く") || lower.contains("слушать снова") ||
            lower.contains("重新收听") || lower.contains("再次收聽") || lower.contains("再次聆聽") ||
            lower.contains("האזנה חוזרת") || lower.contains("ascolta di nuovo") || lower.contains("tekrar dinle") ||
            lower.contains("ฟังอีกครั้ง") || lower.contains("nghe lại") ->
            stringResource(Res.string.shelf_listen_again)

        (lower.contains("trending") || lower.contains("popul") || lower.contains("thịnh hành") || lower.contains("急上昇") || lower.contains("en alta") || lower.contains("тренд") || lower.contains("热门") || lower.contains("熱門") || lower.contains("มาแรง")) &&
            (lower.contains("community") || lower.contains("cộng đồng") || lower.contains("コミュニティ") || lower.contains("comunidad") || lower.contains("communauté") || lower.contains("communaute") || lower.contains("społeczności") || lower.contains("comunidade") || lower.contains("komunitas") || lower.contains("समुदाय") || lower.contains("сообществ") || lower.contains("社群") || lower.contains("社区") || lower.contains("קהילה") || lower.contains("topluluk") || lower.contains("ชุมชน")) ->
            stringResource(Res.string.shelf_trending_community_playlists)

        lower.contains("featured playlist") || lower.contains("listas destacadas") || lower.contains("playlist destacadas") ||
            lower.contains("playlists à la une") || lower.contains("empfohlene playlists") ||
            lower.contains("polecane playlisty") || lower.contains("playlists em destaque") ||
            lower.contains("playlist unggulan") || lower.contains("विशेष प्लेलिस्ट") ||
            lower.contains("注目の再生リスト") || lower.contains("рекомендуемые плейлисты") ||
            lower.contains("精选播放列表") || lower.contains("精選播放清單") ||
            lower.contains("פלייליסטים נבחרים") || lower.contains("playlist in primo piano") ||
            lower.contains("เพลย์ลิสต์แนะนำ") || lower.contains("danh sách phát nổi bật") || lower.contains("danh sách phát đề xuất") ->
            stringResource(Res.string.shelf_featured_playlists_for_you)

        lower.contains("mixed for you") || lower.contains("mixes para ti") || lower.contains("mix pour vous") ||
            lower.contains("für dich gemixt") || lower.contains("fuer dich gemixt") || lower.contains("miks dla ciebie") ||
            lower.contains("músicas para você") || lower.contains("dibuat khusus untuk anda") ||
            lower.contains("आपके लिए मिक्स") || lower.contains("специально для вас") ||
            lower.contains("量身打造") || lower.contains("为你打造") || lower.contains("為你打造") ||
            lower.contains("מיקסים בשבילך") || lower.contains("mix per te") || lower.contains("sizin için karışık") ||
            lower.contains("มิกซ์สำหรับคุณ") || lower.contains("dành riêng cho bạn") ->
            stringResource(Res.string.shelf_mixed_for_you)

        trimmed.equals("For you", ignoreCase = true) || trimmed.equals("Dành cho bạn", ignoreCase = true) ||
            trimmed.equals("あなたへのおすすめ", ignoreCase = true) || trimmed.equals("Para ti", ignoreCase = true) ||
            trimmed.equals("Pour vous", ignoreCase = true) || trimmed.equals("Für dich", ignoreCase = true) ||
            trimmed.equals("Dla Ciebie", ignoreCase = true) || trimmed.equals("Para você", ignoreCase = true) ||
            trimmed.equals("Untuk Anda", ignoreCase = true) || trimmed.equals("आपके लिए", ignoreCase = true) ||
            trimmed.equals("Для вас", ignoreCase = true) || trimmed.equals("为你推荐", ignoreCase = true) ||
            trimmed.equals("為你推薦", ignoreCase = true) || trimmed.equals("בשבילך", ignoreCase = true) ||
            trimmed.equals("Per te", ignoreCase = true) || trimmed.equals("Sizin için", ignoreCase = true) ||
            trimmed.equals("สำหรับคุณ", ignoreCase = true) ->
            stringResource(Res.string.shelf_for_you)

        lower.contains("remixes & edits") || lower.contains("remixes and edits") ||
            lower.contains("remixes & edições") || lower.contains("remiksy i edycje") ||
            lower.contains("रीमिक्स और एडिट") || lower.contains("remixes et versions") ||
            lower.contains("remixes y ediciones") || lower.contains("重混与编辑") || lower.contains("重混與編輯") ||
            lower.contains("bản phối lại & bản chỉnh sửa") ||
            trimmed.equals("Remixes", ignoreCase = true) || trimmed.equals("Remix", ignoreCase = true) ||
            trimmed.equals("Remiksy", ignoreCase = true) || trimmed.equals("रीमिक्स", ignoreCase = true) ||
            trimmed.equals("リミックス", ignoreCase = true) || trimmed.equals("Ремиксы", ignoreCase = true) ||
            trimmed.equals("混音", ignoreCase = true) || trimmed.equals("重混", ignoreCase = true) ->
            stringResource(Res.string.shelf_remixes_edits)

        lower.contains("covers and remixes") || lower.contains("covers & remixes") ||
            lower.contains("カバーとリミックス") || lower.contains("versiones y remixes") ||
            lower.contains("reprises et remixes") || lower.contains("covery i remiksy") ||
            lower.contains("каверы и ремиксы") || lower.contains("翻唱和重混") || lower.contains("翻唱與重混") ->
            stringResource(Res.string.shelf_covers_remixes)

        lower.contains("music videos for you") || lower.contains("vídeos musicales para ti") ||
            lower.contains("clips musicaux pour vous") || lower.contains("musikvideos für dich") ||
            lower.contains("teledyski dla ciebie") || lower.contains("videoclipes para você") ||
            lower.contains("आपके लिए संगीत वीडियो") || lower.contains("музыкальные клипы для вас") ||
            lower.contains("为您精选音乐短片") || lower.contains("為您精選音樂影片") ->
            stringResource(Res.string.shelf_music_videos_for_you)

        lower.contains("recommended music video") || lower.contains("video âm nhạc đề xuất") ||
            lower.contains("おすすめのミュージック ビデオ") || lower.contains("vídeos musicales recomendados") ||
            lower.contains("clips musicaux recommandés") || lower.contains("empfohlene musikvideos") ||
            lower.contains("polecane teledyski") || lower.contains("videoclipes recomendados") ||
            lower.contains("video musik rekomendasi") || lower.contains("सुझाए गए संगीत वीडियो") ||
            lower.contains("рекомендованные музыкальные клипы") || lower.contains("推荐音乐短片") ||
            lower.contains("推薦音樂影片") || lower.contains("קליפים מומלצים") ||
            lower.contains("video musicali consigliati") || lower.contains("önerilen müzik klipleri") ||
            lower.contains("มิวสิกวิดีโอแนะนำ") ->
            stringResource(Res.string.shelf_recommended_music_videos)

        lower.contains("top music video") || lower.contains("video âm nhạc hàng đầu") ||
            lower.contains("人気のミュージック ビデオ") || lower.contains("vídeos musicales populares") ||
            lower.contains("meilleurs clips musicaux") || lower.contains("top-musikvideos") ||
            lower.contains("najlepsze teledyski") || lower.contains("principais videoclipes") ||
            lower.contains("शीर्ष संगीत वीडियो") || lower.contains("лучшие музыкальные клипы") ||
            lower.contains("热门音乐短片") || lower.contains("熱門音樂影片") ||
            lower.contains("i migliori video musicali") || lower.contains("มิวสิกวิดีโอยอดนิยม") ->
            stringResource(Res.string.shelf_top_music_videos)

        lower.contains("new music video") || lower.contains("nouveaux clips") ||
            lower.contains("neue musikvideos") || lower.contains("nowe teledyski") ||
            lower.contains("novos videoclipes") || lower.contains("नए संगीत वीडियो") ||
            lower.contains("новые музыкальные клипы") || lower.contains("最新音乐短片") ||
            lower.contains("最新音樂影片") || lower.contains("nuovi video musicali") ||
            lower.contains("มิวสิกวิดีโอใหม่") ->
            stringResource(Res.string.shelf_new_music_videos)

        lower.contains("music video") || lower.contains("video âm nhạc") ||
            lower.contains("ミュージック ビデオ") || lower.contains("vídeos musicales") ||
            lower.contains("videos musicales") || lower.contains("clips musicaux") ||
            lower.contains("musikvideos") || lower.contains("teledyski") ||
            lower.contains("videoclipes") || lower.contains("संगीत वीडियो") ||
            lower.contains("музыкальные клипы") || lower.contains("音乐短片") || lower.contains("音樂影片") ||
            lower.contains("video musicali") || lower.contains("müzik klipleri") || lower.contains("มิวสิกวิดีโอ") ->
            stringResource(Res.string.shelf_music_videos)

        lower.startsWith("shorts") || lower.startsWith("short") ||
            trimmed.equals("Krótkie filmy", ignoreCase = true) ||
            trimmed.equals("Shorts nổi bật", ignoreCase = true) ||
            trimmed.equals("ショート", ignoreCase = true) ->
            stringResource(Res.string.shelf_shorts)

        lower.contains("youtube chart") || lower.contains("bảng xếp hạng youtube") ||
            lower.contains("youtube チャート") || lower.contains("listas de youtube") ||
            lower.contains("classements youtube") || lower.contains("youtube-charts") ||
            lower.contains("listy youtube") || lower.contains("paradas do youtube") ||
            lower.contains("यूट्यूब चार्ट") || lower.contains("чарты youtube") ||
            lower.contains("youtube 排行榜") || lower.contains("youtube 發燒影片") ||
            lower.contains("classifiche di youtube") || lower.contains("ชาร์ต youtube") ->
            stringResource(Res.string.shelf_youtube_charts)

        lower.contains("hits today") || lower.contains("today's hits") ||
            lower.contains("bản hit hôm nay") || lower.contains("今日のヒット曲") ||
            lower.contains("éxitos de hoy") || lower.contains("hits d'aujourd'hui") ||
            lower.contains("hits von heute") || lower.contains("hity na dziś") ||
            lower.contains("sucessos de hoje") || lower.contains("आज के हिट") ||
            lower.contains("хиты сегодня") || lower.contains("今日热门") || lower.contains("今日熱門") ||
            lower.contains("le hit di oggi") || lower.contains("günün hitleri") || lower.contains("เพลงฮิตวันนี้") ->
            stringResource(Res.string.shelf_hits_today)

        trimmed.equals("Trending", ignoreCase = true) || trimmed.equals("Thịnh hành", ignoreCase = true) ||
            trimmed.equals("急上昇", ignoreCase = true) || trimmed.equals("Tendencias", ignoreCase = true) ||
            trimmed.equals("Tendances", ignoreCase = true) || trimmed.equals("Trends", ignoreCase = true) ||
            trimmed.equals("Na czasie", ignoreCase = true) || trimmed.equals("Em alta", ignoreCase = true) ||
            trimmed.equals("Sedang tren", ignoreCase = true) || trimmed.equals("ट्रेंडिंग", ignoreCase = true) ||
            trimmed.equals("В тренде", ignoreCase = true) || trimmed.equals("热门", ignoreCase = true) ||
            trimmed.equals("發燒", ignoreCase = true) || trimmed.equals("חם ברשת", ignoreCase = true) ||
            trimmed.equals("Di tendenza", ignoreCase = true) || trimmed.equals("Trendler", ignoreCase = true) ||
            trimmed.equals("มาแรง", ignoreCase = true) ->
            stringResource(Res.string.shelf_trending)

        trimmed.equals("Charts", ignoreCase = true) || trimmed.equals("Chart", ignoreCase = true) ||
            trimmed.equals("Bảng xếp hạng", ignoreCase = true) || trimmed.equals("チャート", ignoreCase = true) ||
            trimmed.equals("Listas de éxitos", ignoreCase = true) || trimmed.equals("Classements", ignoreCase = true) ||
            trimmed.equals("Listy przebojów", ignoreCase = true) || trimmed.equals("Paradas", ignoreCase = true) ||
            trimmed.equals("Tangga lagu", ignoreCase = true) || trimmed.equals("चार्ट", ignoreCase = true) ||
            trimmed.equals("Чарты", ignoreCase = true) || trimmed.equals("排行榜", ignoreCase = true) ||
            trimmed.equals("熱門榜", ignoreCase = true) || trimmed.equals("מצעדים", ignoreCase = true) ||
            trimmed.equals("Classifiche", ignoreCase = true) || trimmed.equals("Listeler", ignoreCase = true) ||
            trimmed.equals("ชาร์ต", ignoreCase = true) ->
            stringResource(Res.string.shelf_charts)

        lower.contains("new albums and singles and top songs") || lower.contains("new albums & singles & top songs") ->
            stringResource(Res.string.shelf_new_albums_singles_top_songs)

        lower.contains("new albums and singles") || lower.contains("new albums & singles") ||
            lower.contains("nouveaux albums et singles") || lower.contains("neue alben und singles") ||
            lower.contains("nowe albumy i single") || lower.contains("novos álbuns e singles") ||
            lower.contains("नए एल्बम और सिंगल") || lower.contains("новые альбомы и синглы") ||
            lower.contains("新专辑与单曲") || lower.contains("新專輯與單曲") || lower.contains("אלבומים וסינגלים חדשים") ||
            lower.contains("nuovi album e singoli") || lower.contains("yeni albümler ve tekliler") ->
            stringResource(Res.string.shelf_new_albums_singles)

        lower.contains("albums and singles") || lower.contains("albums & singles") ->
            stringResource(Res.string.shelf_albums_singles)

        trimmed.equals("Top songs", ignoreCase = true) || trimmed.equals("Top Songs", ignoreCase = true) ||
            trimmed.equals("Top-Titel", ignoreCase = true) || trimmed.equals("Meilleurs titres", ignoreCase = true) ||
            trimmed.equals("Najlepsze utwory", ignoreCase = true) || trimmed.equals("Principais faixas", ignoreCase = true) ||
            trimmed.equals("शीर्ष गीत", ignoreCase = true) || trimmed.equals("Лучшие треки", ignoreCase = true) ||
            trimmed.equals("热门歌曲", ignoreCase = true) || trimmed.equals("熱門歌曲", ignoreCase = true) ->
            stringResource(Res.string.shelf_top_songs)

        lower.contains("new release") || lower.contains("novedades") || lower.contains("nouveautés") ||
            lower.contains("nouveautes") || lower.contains("neuerscheinungen") || lower.contains("nowe wydania") ||
            lower.contains("lançamentos") || lower.contains("lancamentos") || lower.contains("rilis baru") ||
            lower.contains("नई रिलीज़") || lower.contains("नई रिलीज") || lower.contains("最新リリース") ||
            lower.contains("новинки") || lower.contains("新作品") || lower.contains("最新發行") || lower.contains("最新发行") ||
            lower.contains("מהדורות חדשות") || lower.contains("nuove uscite") || lower.contains("yeni çıkanlar") ||
            lower.contains("เพลงออกใหม่") || lower.contains("bản phát hành mới") ->
            stringResource(Res.string.shelf_new_releases)

        lower.contains("artists on the rise") || lower.contains("nghệ sĩ đang lên") ||
            lower.contains("artistas en auge") || lower.contains("artistes émergents") ||
            lower.contains("aufstrebende künstler") || lower.contains("wschodzący artyści") ||
            lower.contains("उभरते कलाकार") || lower.contains("набирающие популярность исполнители") ||
            lower.contains("新星艺人") || lower.contains("新星藝人") || lower.contains("artisti emergenti") ||
            lower.contains("ศิลปินมาแรง") ->
            stringResource(Res.string.shelf_artists_on_the_rise)

        lower.contains("from your library") || lower.contains("từ thư viện của bạn") ||
            lower.contains("ライブラリから") || lower.contains("de tu biblioteca") ||
            lower.contains("de votre bibliothèque") || lower.contains("aus deiner mediathek") ||
            lower.contains("z twojej biblioteki") || lower.contains("da sua biblioteca") ||
            lower.contains("आपकी लाइब्रेरी से") || lower.contains("из вашей библиотеки") ||
            lower.contains("来自媒体库") || lower.contains("來自音樂庫") ||
            lower.contains("dalla tua raccolta") || lower.contains("kitaplığınızdan") ->
            stringResource(Res.string.shelf_from_your_library)

        trimmed.equals("Concerts", ignoreCase = true) || trimmed.equals("Buổi hòa nhạc", ignoreCase = true) ||
            trimmed.equals("コンサート", ignoreCase = true) || trimmed.equals("Conciertos", ignoreCase = true) ||
            trimmed.equals("Konzerte", ignoreCase = true) || trimmed.equals("Koncerty", ignoreCase = true) ||
            trimmed.equals("Shows", ignoreCase = true) || trimmed.equals("Konser", ignoreCase = true) ||
            trimmed.equals("कंसर्ट", ignoreCase = true) || trimmed.equals("Концерты", ignoreCase = true) ||
            trimmed.equals("演唱会", ignoreCase = true) || trimmed.equals("演唱會", ignoreCase = true) ||
            trimmed.equals("הופעות", ignoreCase = true) || trimmed.equals("Concerti", ignoreCase = true) ||
            trimmed.equals("คอนเสิร์ต", ignoreCase = true) ->
            stringResource(Res.string.shelf_concerts)

        trimmed.equals("Playlists", ignoreCase = true) || trimmed.equals("Danh sách phát", ignoreCase = true) ||
            trimmed.equals("再生リスト", ignoreCase = true) || trimmed.equals("Listas de reproducción", ignoreCase = true) ||
            trimmed.equals("Listes de lecture", ignoreCase = true) || trimmed.equals("Playlisten", ignoreCase = true) ||
            trimmed.equals("Playlisty", ignoreCase = true) || trimmed.equals("प्लेलिस्ट", ignoreCase = true) ||
            trimmed.equals("Плейлисты", ignoreCase = true) || trimmed.equals("播放列表", ignoreCase = true) ||
            trimmed.equals("播放清單", ignoreCase = true) ->
            stringResource(Res.string.playlists)

        trimmed.equals("Albums", ignoreCase = true) || trimmed.equals("Album", ignoreCase = true) ||
            trimmed.equals("アルバム", ignoreCase = true) || trimmed.equals("Álbumes", ignoreCase = true) ||
            trimmed.equals("Alben", ignoreCase = true) || trimmed.equals("Albumy", ignoreCase = true) ||
            trimmed.equals("एल्बम", ignoreCase = true) || trimmed.equals("Альбомы", ignoreCase = true) ||
            trimmed.equals("专辑", ignoreCase = true) || trimmed.equals("專輯", ignoreCase = true) ->
            stringResource(Res.string.albums)

        trimmed.equals("Artists", ignoreCase = true) || trimmed.equals("Nghệ sĩ", ignoreCase = true) ||
            trimmed.equals("アーティスト", ignoreCase = true) || trimmed.equals("Artistas", ignoreCase = true) ||
            trimmed.equals("Artistes", ignoreCase = true) || trimmed.equals("Künstler", ignoreCase = true) ||
            trimmed.equals("Artyści", ignoreCase = true) || trimmed.equals("कलाकार", ignoreCase = true) ||
            trimmed.equals("Исполнители", ignoreCase = true) || trimmed.equals("艺人", ignoreCase = true) ||
            trimmed.equals("演出者", ignoreCase = true) ->
            stringResource(Res.string.artists)

        trimmed.equals("Subscriptions", ignoreCase = true) || trimmed.equals("Đã đăng ký", ignoreCase = true) ||
            trimmed.equals("登録チャンネル", ignoreCase = true) || trimmed.equals("Suscripciones", ignoreCase = true) ||
            trimmed.equals("Abonnements", ignoreCase = true) || trimmed.equals("Abos", ignoreCase = true) ||
            trimmed.equals("Subskrypcje", ignoreCase = true) || trimmed.equals("सदस्यताएं", ignoreCase = true) ||
            trimmed.equals("Подписки", ignoreCase = true) || trimmed.equals("订阅", ignoreCase = true) ||
            trimmed.equals("訂閱內容", ignoreCase = true) ->
            stringResource(Res.string.subscriptions)

        else -> title
    }
}

@Composable
internal fun localizeShelfSubtitle(subtitle: String): String {
    val trimmed = subtitle.trim()
    val lower = trimmed.lowercase(Locale.ROOT)
    return when {
        lower.contains("start a radio") || lower.contains("bắt đầu đài phát") ||
            lower.contains("iniciar una radio") || lower.contains("lancer une radio") ||
            lower.contains("radio starten") || lower.contains("rozpocznij radio") ||
            lower.contains("iniciar uma rádio") || lower.contains("mulai radio") ||
            lower.contains("रेडियो शुरू करें") || lower.contains("включить радиостанцию") ||
            lower.contains("电台") || lower.contains("電台") || lower.contains("רדיו") ||
            lower.contains("avvia una radio") || lower.contains("radyo başlat") ||
            lower.contains("เริ่มวิทยุ") || lower.contains("ラジオを開始") ->
            stringResource(Res.string.shelf_start_radio_desc)

        lower.contains("fresh drops") || lower.contains("stations, mixes") ||
            lower.contains("bản phát hành mới và tuyển tập") || lower.contains("estaciones, mezclas") ||
            lower.contains("stations, mix") || lower.contains("sender, mixes") ||
            lower.contains("stacje, miksy") || lower.contains("estações, mixes") ||
            lower.contains("stasiun, campuran") || lower.contains("स्टेशन, मिक्स") ||
            lower.contains("радиостанции, миксы") || lower.contains("电台、合辑") ||
            lower.contains("電台、合輯") || lower.contains("stazioni, mix") ||
            lower.contains("istasyonlar, karışıklar") || lower.contains("สถานี, มิกซ์") ||
            lower.contains("ステーション、ミックス") ->
            stringResource(Res.string.shelf_stations_mixes_desc)

        lower.contains("artist stations") || lower.contains("spotlight cards") ||
            lower.contains("thẻ tiêu điểm") || lower.contains("tarjetas destacadas") ||
            lower.contains("cartes à la une") || lower.contains("spotlight-karten") ||
            lower.contains("karty polecanych") || lower.contains("cartões de destaque") ||
            lower.contains("kartu sorotan") || lower.contains("स्पॉटलाइट कार्ड") ||
            lower.contains("карточки с акцентом") || lower.contains("焦点卡片") ||
            lower.contains("焦點卡片") || lower.contains("schede in primo piano") ||
            lower.contains("öne çıkan kartlar") || lower.contains("การ์ดไฮไลต์") ||
            lower.contains("スポットライト カード") ->
            stringResource(Res.string.shelf_top_picks_desc)

        lower.contains("club mixes") || lower.contains("vips & reimagined") || lower.contains("vip edits") ||
            lower.contains("bản phối câu lạc bộ") || lower.contains("mezclas de club") ||
            lower.contains("mix club") || lower.contains("club-mixes") ||
            lower.contains("klubowe miksy") || lower.contains("mixes de balada") ||
            lower.contains("campuran klub") || lower.contains("क्लब मिक्स") ||
            lower.contains("клубные миксы") || lower.contains("俱乐部混音") ||
            lower.contains("夜店混音") || lower.contains("mix da club") ||
            lower.contains("kulüp miksleri") || lower.contains("คลับมิกซ์") ||
            lower.contains("クラブ ミックス") ->
            stringResource(Res.string.shelf_club_mixes_desc)

        lower.contains("16:9 widescreen") || lower.contains("widescreen official videos") ||
            lower.contains("màn hình rộng 16:9") || lower.contains("pantalla ancha 16:9") ||
            lower.contains("écran large 16:9") || lower.contains("16:9-breitbild") ||
            lower.contains("panoramiczne 16:9") || lower.contains("widescreen 16:9") ||
            lower.contains("layar lebar 16:9") || lower.contains("16:9 वाइडस्क्रीन") ||
            lower.contains("широкоформатные 16:9") || lower.contains("16:9 宽屏") ||
            lower.contains("16:9 寬螢幕") || lower.contains("widescreen 16:9") ||
            lower.contains("16:9 geniş ekran") || lower.contains("จอกว้าง 16:9") ||
            lower.contains("16:9 ワイドスクリーン") ->
            stringResource(Res.string.shelf_widescreen_videos_desc)

        lower.contains("top tunes right now") || lower.contains("giai điệu hàng đầu hiện nay") ||
            lower.contains("canciones destacadas ahora") || lower.contains("meilleurs titres en ce moment") ||
            lower.contains("top-titel im trend") || lower.contains("najlepsze utwory teraz") ||
            lower.contains("as melhores do momento") || lower.contains("lagu terpopuler saat ini") ||
            lower.contains("अभी के शीर्ष गीत") || lower.contains("главные треки прямо сейчас") ||
            lower.contains("当前热门曲目") || lower.contains("熱門歌曲") ||
            lower.contains("i brani più ascoltati del momento") || lower.contains("şu anda en popüler parçalar") ||
            lower.contains("เพลงฮิตติดหูในขณะนี้") || lower.contains("注目の曲") ->
            stringResource(Res.string.shelf_top_tunes_right_now)

        lower.contains("from the community") || lower.contains("từ cộng đồng") ||
            lower.contains("de la comunidad") || lower.contains("de la communauté") || lower.contains("de la communaute") ||
            lower.contains("aus der community") || lower.contains("ze społeczności") || lower.contains("ze spolecznosci") ||
            lower.contains("da comunidade") || lower.contains("dari komunitas") ||
            lower.contains("समुदाय से") || lower.contains("от сообщества") ||
            lower.contains("来自社区") || lower.contains("來自社群") || lower.contains("מהקהילה") ||
            lower.contains("dalla community") || lower.contains("topluluktan") ||
            lower.contains("จากชุมชน") || lower.contains("コミュニティより") ->
            stringResource(Res.string.shelf_from_the_community)

        lower.contains("youtube chart") || lower.contains("bảng xếp hạng youtube") ||
            lower.contains("listas de youtube") || lower.contains("classements youtube") ||
            lower.contains("youtube-charts") || lower.contains("listy youtube") ||
            lower.contains("paradas do youtube") || lower.contains("tangga lagu youtube") ||
            lower.contains("यूट्यूब चार्ट") || lower.contains("чарты youtube") ||
            lower.contains("youtube 排行榜") || lower.contains("youtube 發燒影片") ||
            lower.contains("classifiche di youtube") || lower.contains("youtube listeleri") ||
            lower.contains("ชาร์ต youtube") || lower.contains("youtube チャート") ->
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
                trimmed.equals("Bài hát", ignoreCase = true) || trimmed.equals("曲", ignoreCase = true) ||
                trimmed.equals("Canción", ignoreCase = true) || trimmed.equals("Cancion", ignoreCase = true) ||
                trimmed.equals("Lied", ignoreCase = true) || trimmed.equals("Utwór", ignoreCase = true) ||
                trimmed.equals("Utwor", ignoreCase = true) || trimmed.equals("Música", ignoreCase = true) ||
                trimmed.equals("Musica", ignoreCase = true) || trimmed.equals("Lagu", ignoreCase = true) ||
                trimmed.equals("गीत", ignoreCase = true) || trimmed.equals("Песня", ignoreCase = true) ||
                trimmed.equals("歌曲", ignoreCase = true) || trimmed.equals("שיר", ignoreCase = true) ||
                trimmed.equals("Brano", ignoreCase = true) || trimmed.equals("Şarkı", ignoreCase = true) ||
                trimmed.equals("Sarki", ignoreCase = true) || trimmed.equals("เพลง", ignoreCase = true) -> songLabel

            trimmed.equals("Single", ignoreCase = true) || trimmed.equals("Đĩa đơn", ignoreCase = true) ||
                trimmed.equals("シングル", ignoreCase = true) || trimmed.equals("Sencillo", ignoreCase = true) ||
                trimmed.equals("Singiel", ignoreCase = true) || trimmed.equals("Singel", ignoreCase = true) ||
                trimmed.equals("सिंगल", ignoreCase = true) || trimmed.equals("Сингл", ignoreCase = true) ||
                trimmed.equals("单曲", ignoreCase = true) || trimmed.equals("單曲", ignoreCase = true) ||
                trimmed.equals("סינגל", ignoreCase = true) || trimmed.equals("Tekli", ignoreCase = true) ||
                trimmed.equals("ซิงเกิล", ignoreCase = true) -> singleLabel

            trimmed.equals("Chart", ignoreCase = true) || trimmed.equals("Bảng xếp hạng", ignoreCase = true) ||
                trimmed.equals("チャート", ignoreCase = true) || trimmed.equals("Lista de éxitos", ignoreCase = true) ||
                trimmed.equals("Classement", ignoreCase = true) || trimmed.equals("Charts", ignoreCase = true) ||
                trimmed.equals("Lista przebojów", ignoreCase = true) || trimmed.equals("Parada", ignoreCase = true) ||
                trimmed.equals("Tangga lagu", ignoreCase = true) || trimmed.equals("चार्ट", ignoreCase = true) ||
                trimmed.equals("Чарт", ignoreCase = true) || trimmed.equals("排行榜", ignoreCase = true) ||
                trimmed.equals("熱門榜", ignoreCase = true) || trimmed.equals("מצעד", ignoreCase = true) ||
                trimmed.equals("Classifica", ignoreCase = true) || trimmed.equals("Liste", ignoreCase = true) ||
                trimmed.equals("ชาร์ต", ignoreCase = true) -> chartLabel

            trimmed.equals("Playlist", ignoreCase = true) || trimmed.equals("Danh sách phát", ignoreCase = true) ||
                trimmed.equals("再生リスト", ignoreCase = true) || trimmed.equals("Lista de reproducción", ignoreCase = true) ||
                trimmed.equals("Liste de lecture", ignoreCase = true) || trimmed.equals("Playlista", ignoreCase = true) ||
                trimmed.equals("Daftar putar", ignoreCase = true) || trimmed.equals("प्लेलिस्ट", ignoreCase = true) ||
                trimmed.equals("Плейлист", ignoreCase = true) || trimmed.equals("播放列表", ignoreCase = true) ||
                trimmed.equals("播放清單", ignoreCase = true) || trimmed.equals("רשימת השמעה", ignoreCase = true) ||
                trimmed.equals("Oynatma listesi", ignoreCase = true) || trimmed.equals("เพลย์ลิสต์", ignoreCase = true) -> playlistLabel

            lower.contains("view") || lower.contains("lượt xem") || lower.contains("回視聴") ||
                lower.contains("vistas") || lower.contains("vues") || lower.contains("aufrufe") ||
                lower.contains("wyświetleń") || lower.contains("wyswietlen") || lower.contains("visualizações") ||
                lower.contains("visualizacoes") || lower.contains("ditonton") || lower.contains("देखा गया") ||
                lower.contains("просмотр") || lower.contains("次观看") || lower.contains("次觀看") ||
                lower.contains("צפיות") || lower.contains("visualizzazioni") || lower.contains("görüntüleme") ||
                lower.contains("goruntuleme") || lower.contains("ครั้ง") -> {
                val count = trimmed.takeWhile { it.isDigit() || it == '.' || it == ',' || it == ' ' || it in "KkMmBb" }.trim()
                if (count.any { it.isDigit() }) stringResource(Res.string.card_views_format, count) else part
            }

            lower.contains("play") || lower.contains("lượt phát") || lower.contains("回再生") ||
                lower.contains("reproducciones") || lower.contains("écoutes") || lower.contains("ecoutes") ||
                lower.contains("wiedergaben") || lower.contains("odtworzeń") || lower.contains("odtworzen") ||
                lower.contains("reproduções") || lower.contains("reproducoes") || lower.contains("diputar") ||
                lower.contains("बजाया गया") || lower.contains("прослушиван") || lower.contains("次播放") ||
                lower.contains("השמעות") || lower.contains("riproduzioni") || lower.contains("oynatma") -> {
                val count = trimmed.takeWhile { it.isDigit() || it == '.' || it == ',' || it == ' ' || it in "KkMmBb" }.trim()
                if (count.any { it.isDigit() }) stringResource(Res.string.card_plays_format, count) else part
            }

            lower.contains("song") || lower.contains("bài hát") || lower.contains("曲") ||
                lower.contains("canciones") || lower.contains("titres") || lower.contains("titel") ||
                lower.contains("utworów") || lower.contains("utworow") || lower.contains("músicas") ||
                lower.contains("musicas") || lower.contains("lagu") || lower.contains("गाने") ||
                lower.contains("трек") || lower.contains("首歌曲") || lower.contains("שירים") ||
                lower.contains("brani") || lower.contains("şarkı") || lower.contains("sarki") ||
                lower.contains("เพลง") -> {
                val count = trimmed.takeWhile { it.isDigit() || it == '.' || it == ',' || it == ' ' || it in "KkMmBb" }.trim()
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
    val isVideoShelf = shelf.toShelfKey() == HomeShelfKey.MUSIC_VIDEOS ||
        shelf.title.contains("video", ignoreCase = true) ||
        shelf.items.any { it.isVideo }
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

fun clearShelfKeyCache() {
    HomeShelfKey.clearCache()
}

internal fun HomeShelf.toShelfKey(): HomeShelfKey = HomeShelfKey.fromTitle(title)
