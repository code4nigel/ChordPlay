package com.music.bitchord.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

data class IosTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationText: String,
    val artworkUrl: String,
    val streamUrl: String,
)

private val SAMPLE_TRACKS = listOf(
    IosTrack(
        id = "1",
        title = "Blinding Lights",
        artist = "The Weeknd",
        album = "After Hours",
        durationText = "3:20",
        artworkUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=500&auto=format&fit=crop&q=80",
        streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
    ),
    IosTrack(
        id = "2",
        title = "Starboy",
        artist = "The Weeknd ft. Daft Punk",
        album = "Starboy",
        durationText = "3:50",
        artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop&q=80",
        streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
    ),
    IosTrack(
        id = "3",
        title = "Levitating",
        artist = "Dua Lipa",
        album = "Future Nostalgia",
        durationText = "3:23",
        artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&auto=format&fit=crop&q=80",
        streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
    ),
    IosTrack(
        id = "4",
        title = "Shape of You",
        artist = "Ed Sheeran",
        album = "÷ (Divide)",
        durationText = "3:53",
        artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&auto=format&fit=crop&q=80",
        streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
    ),
    IosTrack(
        id = "5",
        title = "Bad Guy",
        artist = "Billie Eilish",
        album = "WHEN WE ALL FALL ASLEEP",
        durationText = "3:14",
        artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500&auto=format&fit=crop&q=80",
        streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
    ),
    IosTrack(
        id = "6",
        title = "Midnight City",
        artist = "M83",
        album = "Hurry Up, We're Dreaming",
        durationText = "4:03",
        artworkUrl = "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=500&auto=format&fit=crop&q=80",
        streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
    ),
)

private val CATEGORIES = listOf("Trending", "Relax", "Workout", "Focus", "Party", "Lo-Fi", "Rock")

@Composable
fun ChordPlayIosApp() {
    val audioPlayer = remember { IosAudioPlayer() }
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val position by audioPlayer.currentPosition.collectAsState()
    val duration by audioPlayer.duration.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var currentTrack by remember { mutableStateOf<IosTrack?>(SAMPLE_TRACKS.first()) }
    var showPlayerModal by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var likedTrackIds by remember { mutableStateOf(setOf("1", "3")) }

    // Audio progress polling loop
    LaunchedEffect(Unit) {
        while (true) {
            audioPlayer.updateProgress()
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            IosTopBar()

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> HomeView(
                        tracks = SAMPLE_TRACKS,
                        currentTrack = currentTrack,
                        onTrackSelected = { track ->
                            currentTrack = track
                            audioPlayer.playUrl(track.streamUrl)
                        }
                    )
                    1 -> SearchView(
                        tracks = SAMPLE_TRACKS,
                        searchQuery = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onTrackSelected = { track ->
                            currentTrack = track
                            audioPlayer.playUrl(track.streamUrl)
                        }
                    )
                    2 -> LibraryView(
                        likedCount = likedTrackIds.size,
                        recentTracks = SAMPLE_TRACKS.take(3),
                        onTrackSelected = { track ->
                            currentTrack = track
                            audioPlayer.playUrl(track.streamUrl)
                        }
                    )
                }
            }

            // Floating Mini-Player Dock (when track selected)
            if (currentTrack != null) {
                IosMiniPlayerDock(
                    track = currentTrack!!,
                    isPlaying = isPlaying,
                    onTogglePlay = { audioPlayer.togglePlayPause() },
                    onExpand = { showPlayerModal = true }
                )
            }

            // Bottom Navigation Bar
            IosBottomNavBar(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it }
            )
        }

        // Full Screen Now Playing Modal Drawer
        AnimatedVisibility(
            visible = showPlayerModal && currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            IosNowPlayingModal(
                track = currentTrack!!,
                isPlaying = isPlaying,
                position = position,
                duration = duration,
                isLiked = likedTrackIds.contains(currentTrack?.id),
                onToggleLike = {
                    val id = currentTrack?.id ?: return@IosNowPlayingModal
                    likedTrackIds = if (likedTrackIds.contains(id)) {
                        likedTrackIds - id
                    } else {
                        likedTrackIds + id
                    }
                },
                onTogglePlay = { audioPlayer.togglePlayPause() },
                onSeek = { audioPlayer.seekTo(it) },
                onSkipNext = {
                    val nextIdx = (SAMPLE_TRACKS.indexOf(currentTrack) + 1) % SAMPLE_TRACKS.size
                    currentTrack = SAMPLE_TRACKS[nextIdx]
                    audioPlayer.playUrl(SAMPLE_TRACKS[nextIdx].streamUrl)
                },
                onSkipPrev = {
                    val prevIdx = if (SAMPLE_TRACKS.indexOf(currentTrack) > 0) {
                        SAMPLE_TRACKS.indexOf(currentTrack) - 1
                    } else {
                        SAMPLE_TRACKS.size - 1
                    }
                    currentTrack = SAMPLE_TRACKS[prevIdx]
                    audioPlayer.playUrl(SAMPLE_TRACKS[prevIdx].streamUrl)
                },
                onDismiss = { showPlayerModal = false }
            )
        }
    }
}

@Composable
private fun IosTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF10B981)))),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "▶",
                    style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            BasicText(
                text = "ChordPlay",
                style = TextStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1F2937))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            BasicText(
                text = "iOS Native",
                style = TextStyle(color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun HomeView(
    tracks: List<IosTrack>,
    currentTrack: IosTrack?,
    onTrackSelected: (IosTrack) -> Unit
) {
    var selectedCat by remember { mutableStateOf("Trending") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        item {
            // Category Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(CATEGORIES) { cat ->
                    val isSelected = cat == selectedCat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF8B5CF6) else Color(0xFF18181B))
                            .clickable { selectedCat = cat }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        BasicText(
                            text = cat,
                            style = TextStyle(
                                color = if (isSelected) Color.White else Color(0xFFA1A1AA),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        item {
            // Quick Picks Shelf
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                BasicText(
                    text = "Quick Picks",
                    style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                BasicText(
                    text = "Based on your music taste",
                    style = TextStyle(color = Color(0xFF71717A), fontSize = 13.sp)
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(tracks) { track ->
                    TrackCard(track = track, onClick = { onTrackSelected(track) })
                }
            }
        }

        item {
            // Top Songs
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                BasicText(
                    text = "Discover More",
                    style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        items(tracks) { track ->
            TrackListItem(
                track = track,
                isPlaying = track.id == currentTrack?.id,
                onClick = { onTrackSelected(track) }
            )
        }
    }
}

@Composable
private fun TrackCard(track: IosTrack, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF27272A))
        ) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        BasicText(
            text = track.title,
            style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        BasicText(
            text = track.artist,
            style = TextStyle(color = Color(0xFFA1A1AA), fontSize = 12.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TrackListItem(track: IosTrack, isPlaying: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF27272A))
        ) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = track.title,
                style = TextStyle(
                    color = if (isPlaying) Color(0xFF10B981) else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            BasicText(
                text = "${track.artist} • ${track.durationText}",
                style = TextStyle(color = Color(0xFF71717A), fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        BasicText(
            text = if (isPlaying) "Playing" else "▶",
            style = TextStyle(
                color = if (isPlaying) Color(0xFF10B981) else Color(0xFFA1A1AA),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun SearchView(
    tracks: List<IosTrack>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onTrackSelected: (IosTrack) -> Unit
) {
    val filteredTracks = remember(searchQuery) {
        if (searchQuery.isBlank()) tracks else tracks.filter {
            it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(12.dp))
        // Search Input Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF18181B))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (searchQuery.isEmpty()) {
                BasicText(
                    text = "Search songs, artists, albums...",
                    style = TextStyle(color = Color(0xFF71717A), fontSize = 15.sp)
                )
            }
            BasicTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(Color(0xFF8B5CF6)),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredTracks) { track ->
                TrackListItem(track = track, isPlaying = false, onClick = { onTrackSelected(track) })
            }
        }
    }
}

@Composable
private fun LibraryView(
    likedCount: Int,
    recentTracks: List<IosTrack>,
    onTrackSelected: (IosTrack) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            BasicText(
                text = "Your Library",
                style = TextStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Liked Songs Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF4C1D95))))
                    .padding(20.dp)
            ) {
                Column {
                    BasicText(
                        text = "♥ Liked Songs",
                        style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicText(
                        text = "$likedCount favorite tracks",
                        style = TextStyle(color = Color(0xFFDDD6FE), fontSize = 13.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            BasicText(
                text = "Recently Played",
                style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(recentTracks) { track ->
            TrackListItem(track = track, isPlaying = false, onClick = { onTrackSelected(track) })
        }
    }
}

@Composable
private fun IosMiniPlayerDock(
    track: IosTrack,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onExpand: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E24))
            .clickable(onClick = onExpand)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF27272A))
            ) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = track.title,
                    style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    text = track.artist,
                    style = TextStyle(color = Color(0xFFA1A1AA), fontSize = 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF27272A))
                    .clickable { onTogglePlay() },
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = if (isPlaying) "❚❚" else "▶",
                    style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun IosBottomNavBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F12))
            .padding(top = 10.dp, bottom = 26.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tabs = listOf("Home" to "⌂", "Search" to "🔍", "Library" to "📚")
        tabs.forEachIndexed { idx, (label, icon) ->
            val isSelected = selectedTab == idx
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelectTab(idx) }
            ) {
                BasicText(
                    text = icon,
                    style = TextStyle(
                        color = if (isSelected) Color(0xFF8B5CF6) else Color(0xFF71717A),
                        fontSize = 18.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                BasicText(
                    text = label,
                    style = TextStyle(
                        color = if (isSelected) Color.White else Color(0xFF71717A),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

@Composable
private fun IosNowPlayingModal(
    track: IosTrack,
    isPlaying: Boolean,
    position: Float,
    duration: Float,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit,
    onDismiss: () -> Unit
) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(12000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            rotation.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B))
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Modal Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF18181B))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(text = "▼", style = TextStyle(color = Color.White, fontSize = 12.sp))
                }
                BasicText(
                    text = "NOW PLAYING",
                    style = TextStyle(color = Color(0xFFA1A1AA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF18181B))
                        .clickable(onClick = onToggleLike),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = if (isLiked) "♥" else "♡",
                        style = TextStyle(color = if (isLiked) Color(0xFFEF4444) else Color.White, fontSize = 18.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Spinning Vinyl Cover
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF18181B))
                    .border(4.dp, Color(0xFF27272A), CircleShape)
                    .rotate(rotation.value),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
                // Center Vinyl Spindle Hole
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF09090B))
                        .border(3.dp, Color(0xFF3F3F46), CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Title and Artist
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    text = track.title,
                    style = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                BasicText(
                    text = track.artist,
                    style = TextStyle(color = Color(0xFFA1A1AA), fontSize = 16.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Progress Bar / Timeline
            val progressFraction = if (duration > 0f) (position / duration).coerceIn(0f, 1f) else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF27272A))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progressFraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF8B5CF6))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Duration labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BasicText(
                    text = formatSeconds(position),
                    style = TextStyle(color = Color(0xFF71717A), fontSize = 12.sp)
                )
                BasicText(
                    text = if (duration > 0f) formatSeconds(duration) else track.durationText,
                    style = TextStyle(color = Color(0xFF71717A), fontSize = 12.sp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Playback Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF18181B))
                        .clickable(onClick = onSkipPrev),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(text = "⏮", style = TextStyle(color = Color.White, fontSize = 20.sp))
                }

                // Play / Pause
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF10B981))))
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = if (isPlaying) "❚❚" else "▶",
                        style = TextStyle(color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    )
                }

                // Next
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF18181B))
                        .clickable(onClick = onSkipNext),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(text = "⏭", style = TextStyle(color = Color.White, fontSize = 20.sp))
                }
            }
        }
    }
}

private fun formatSeconds(seconds: Float): String {
    val totalSecs = seconds.toInt().coerceAtLeast(0)
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "$mins:${if (secs < 10) "0$secs" else "$secs"}"
}

