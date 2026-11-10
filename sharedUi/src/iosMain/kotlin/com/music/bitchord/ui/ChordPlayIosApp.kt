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

// ---------------------------------------------------------------------------
// ChordPlay Signature Theme Colors (Matching Android ChordPlay Crimson)
// ---------------------------------------------------------------------------
val ChordPlayCrimson = Color(0xFFFD3F69)
val ChordPlayCrimsonDark = Color(0xFFD60237)
val ChordPlayCrimsonGlow = Color(0xFFFF5277)
val BackgroundDeep = Color(0xFF09080B)
val SurfaceCard = Color(0xFF141318)
val SurfaceCardElevated = Color(0xFF1E1C24)
val SurfaceBorder = Color(0x22FFFFFF)
val TextMuted = Color(0xFF8E8E93)
val TextSubtitle = Color(0xFFB0B0B8)

data class IosTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationText: String,
    val totalSeconds: Float,
    val artworkUrl: String,
    val streamUrl: String,
    val qualityBadge: String = "LOSSLESS 24-BIT",
    val lyrics: List<String> = emptyList(),
)

private val REAL_TRACKS = listOf(
    IosTrack(
        id = "1",
        title = "Blinding Lights",
        artist = "The Weeknd",
        album = "After Hours",
        durationText = "3:20",
        totalSeconds = 200f,
        artworkUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&auto=format&fit=crop&q=80",
        streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
        qualityBadge = "HI-RES LOSSLESS",
        lyrics = listOf(
            "I've been on my own for long enough",
            "Maybe you can show me how to love, maybe",
            "I'm going through withdrawals",
            "You don't even have to do too much",
            "You can turn me on with just a touch, baby",
            "I look around and Sin City's cold and empty",
            "No one's around to judge me",
            "I can't see clearly when you're gone",
            "I said, ooh, I'm blinded by the lights",
        )
    ),
    IosTrack(
        id = "2",
        title = "Starboy",
        artist = "The Weeknd ft. Daft Punk",
        album = "Starboy",
        durationText = "3:50",
        totalSeconds = 230f,
        artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
        streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
        qualityBadge = "DOLBY ATMOS",
        lyrics = listOf(
            "I'm tryna put you in the worst mood, ah",
            "P1 cleaner than your church shoes, ah",
            "Milli point two just to hurt you, ah",
            "All red Lamb' just to tease you, ah",
            "None of these toys on lease too, ah",
            "Made your whole year in a week too, yah",
            "Main bitch out your league too, ah",
            "Side bitch out of your league too, ah",
            "Look what you've done, I'm a motherfuckin' starboy",
        )
    ),
    IosTrack(
        id = "3",
        title = "Levitating",
        artist = "Dua Lipa",
        album = "Future Nostalgia",
        durationText = "3:23",
        totalSeconds = 203f,
        artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
        streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
        qualityBadge = "LOSSLESS 24-BIT",
        lyrics = listOf(
            "If you wanna run away with me, I know a galaxy",
            "And I can take you for a ride",
            "I had a premonition that we fell into a rhythm",
            "Where the music don't stop for life",
            "Glitter in the sky, glitter in my eyes",
            "Shining just the way I like",
            "If you're feeling like you need a little bit of company",
            "You met me at the perfect time",
            "You want me, I want you, baby",
            "My sugarboo, I'm levitating",
        )
    ),
    IosTrack(
        id = "4",
        title = "Shape of You",
        artist = "Ed Sheeran",
        album = "÷ (Divide)",
        durationText = "3:53",
        totalSeconds = 233f,
        artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
        streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
        qualityBadge = "HI-RES LOSSLESS",
        lyrics = listOf(
            "The club isn't the best place to find a lover",
            "So the bar is where I go",
            "Me and my friends at the table doing shots",
            "Drinking fast and then we talk slow",
            "Come over and start up a conversation with just me",
            "And trust me I'll give it a chance now",
            "Take my hand, stop, put Van the Man on the jukebox",
            "And then we start to dance",
            "I'm in love with the shape of you",
        )
    ),
    IosTrack(
        id = "5",
        title = "Bad Guy",
        artist = "Billie Eilish",
        album = "WHEN WE ALL FALL ASLEEP",
        durationText = "3:14",
        totalSeconds = 194f,
        artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80",
        streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
        qualityBadge = "DOLBY ATMOS",
        lyrics = listOf(
            "White shirt now red, my bloody nose",
            "Sleepin', you're on your tippy toes",
            "Creepin' around like no one knows",
            "Think you're so criminal",
            "Bruises on both my knees for you",
            "Don't say thank you or please",
            "I do what I want when I'm wanting to",
            "My soul? So cynical",
            "So you're a tough guy, like it really rough guy",
            "I'm that bad type, make-your-mama-sad type",
            "I'm the bad guy, duh",
        )
    ),
    IosTrack(
        id = "6",
        title = "Midnight City",
        artist = "M83",
        album = "Hurry Up, We're Dreaming",
        durationText = "4:03",
        totalSeconds = 243f,
        artworkUrl = "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=600&auto=format&fit=crop&q=80",
        streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
        qualityBadge = "HI-RES LOSSLESS",
        lyrics = listOf(
            "Waiting in a car",
            "Waiting for a ride in the dark",
            "The night city grows",
            "Look and see her eyes, they glow",
            "Waiting in a car",
            "Waiting for a ride in the dark",
            "The night city grows",
            "Look and see her eyes, they glow",
            "The city is my church",
            "It wraps me in the blinding twilight",
        )
    ),
)

private val MOOD_PILLS = listOf("Trending", "Energize", "Workout", "Relax", "Focus", "Commute", "Party")

@Composable
fun ChordPlayIosApp() {
    val audioPlayer = remember { IosAudioPlayer() }
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val position by audioPlayer.currentPosition.collectAsState()
    val duration by audioPlayer.duration.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var currentTrack by remember { mutableStateOf<IosTrack?>(REAL_TRACKS.first()) }
    var showPlayerModal by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var likedTrackIds by remember { mutableStateOf(setOf("1", "2", "5")) }

    // Audio progress polling loop
    LaunchedEffect(Unit) {
        while (true) {
            audioPlayer.updateProgress()
            delay(400)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ChordPlay Signature Frosted Top Bar
            ChordPlayTopHeader(
                onSearchClick = { selectedTab = 2 }
            )

            // Scrollable Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> ChordPlayHomeTab(
                        tracks = REAL_TRACKS,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onTrackSelected = { track ->
                            currentTrack = track
                            audioPlayer.playUrl(track.streamUrl)
                        }
                    )
                    1 -> ChordPlayExploreTab(
                        tracks = REAL_TRACKS.reversed(),
                        onTrackSelected = { track ->
                            currentTrack = track
                            audioPlayer.playUrl(track.streamUrl)
                        }
                    )
                    2 -> ChordPlaySearchTab(
                        tracks = REAL_TRACKS,
                        searchQuery = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onTrackSelected = { track ->
                            currentTrack = track
                            audioPlayer.playUrl(track.streamUrl)
                        }
                    )
                    3 -> ChordPlayLibraryTab(
                        likedCount = likedTrackIds.size,
                        recentTracks = REAL_TRACKS.take(4),
                        onTrackSelected = { track ->
                            currentTrack = track
                            audioPlayer.playUrl(track.streamUrl)
                        }
                    )
                }
            }
        }

        // Floating Liquid Glass Navigation Stack (MiniPlayer + GlassNavBar)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Floating MiniPlayer Capsule
            if (currentTrack != null) {
                ChordPlayFloatingMiniPlayer(
                    track = currentTrack!!,
                    isPlaying = isPlaying,
                    progress = if (duration > 0f) position / duration else 0f,
                    onTogglePlay = { audioPlayer.togglePlayPause() },
                    onExpand = { showPlayerModal = true }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Floating Frosted Glass Bottom Navigation Bar
            ChordPlayGlassNavBar(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it }
            )
        }

        // Full Screen Now Playing Player Modal (The Look For Play!)
        AnimatedVisibility(
            visible = showPlayerModal && currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(380)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(320)) + fadeOut(),
        ) {
            ChordPlayNowPlayingScreen(
                track = currentTrack!!,
                isPlaying = isPlaying,
                position = position,
                duration = if (duration > 0f) duration else currentTrack!!.totalSeconds,
                isLiked = likedTrackIds.contains(currentTrack?.id),
                onToggleLike = {
                    val id = currentTrack?.id ?: return@ChordPlayNowPlayingScreen
                    likedTrackIds = if (likedTrackIds.contains(id)) likedTrackIds - id else likedTrackIds + id
                },
                onTogglePlay = { audioPlayer.togglePlayPause() },
                onSeek = { audioPlayer.seekTo(it) },
                onSkipNext = {
                    val nextIdx = (REAL_TRACKS.indexOf(currentTrack) + 1) % REAL_TRACKS.size
                    currentTrack = REAL_TRACKS[nextIdx]
                    audioPlayer.playUrl(REAL_TRACKS[nextIdx].streamUrl)
                },
                onSkipPrev = {
                    val prevIdx = if (REAL_TRACKS.indexOf(currentTrack) > 0) {
                        REAL_TRACKS.indexOf(currentTrack) - 1
                    } else {
                        REAL_TRACKS.size - 1
                    }
                    currentTrack = REAL_TRACKS[prevIdx]
                    audioPlayer.playUrl(REAL_TRACKS[prevIdx].streamUrl)
                },
                onDismiss = { showPlayerModal = false }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 1. Top Header Bar
// ---------------------------------------------------------------------------
@Composable
private fun ChordPlayTopHeader(onSearchClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 50.dp, start = 20.dp, end = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // ChordPlay Crimson Logo Emblem
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(ChordPlayCrimson, ChordPlayCrimsonGlow)
                        )
                    )
                    .shadow(12.dp, RoundedCornerShape(12.dp), spotColor = ChordPlayCrimson),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "▶",
                    style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                BasicText(
                    text = "ChordPlay",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                )
                BasicText(
                    text = "MUSIC & AUDIO",
                    style = TextStyle(
                        color = ChordPlayCrimsonGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                )
            }
        }

        // Quick Search pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceCardElevated)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                .clickable(onClick = onSearchClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(text = "🔍", style = TextStyle(fontSize = 13.sp))
                Spacer(modifier = Modifier.width(6.dp))
                BasicText(
                    text = "Search",
                    style = TextStyle(color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. Home Tab (Quick Picks, Mood Chips, Featured Songs)
// ---------------------------------------------------------------------------
@Composable
private fun ChordPlayHomeTab(
    tracks: List<IosTrack>,
    currentTrack: IosTrack?,
    isPlaying: Boolean,
    onTrackSelected: (IosTrack) -> Unit
) {
    var selectedMood by remember { mutableStateOf("Trending") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 160.dp)
    ) {
        // Mood / Category Pills
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MOOD_PILLS) { mood ->
                    val isSelected = mood == selectedMood
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(listOf(ChordPlayCrimson, ChordPlayCrimsonGlow))
                                } else {
                                    SolidColor(SurfaceCardElevated)
                                }
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color.Transparent else SurfaceBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedMood = mood }
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                    ) {
                        BasicText(
                            text = mood,
                            style = TextStyle(
                                color = if (isSelected) Color.White else TextSubtitle,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        // Quick Picks Section (Card Carousel)
        item {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)) {
                BasicText(
                    text = "Quick Picks",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    )
                )
                BasicText(
                    text = "START RADIO FROM A SONG",
                    style = TextStyle(
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(tracks) { track ->
                    ChordPlayQuickPickCard(
                        track = track,
                        isCurrent = track.id == currentTrack?.id,
                        onClick = { onTrackSelected(track) }
                    )
                }
            }
        }

        // Listen Again Section
        item {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)) {
                BasicText(
                    text = "Listen Again",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    )
                )
                BasicText(
                    text = "FREQUENTLY PLAYED BY YOU",
                    style = TextStyle(
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
        }

        items(tracks) { track ->
            ChordPlayTrackRow(
                track = track,
                isCurrent = track.id == currentTrack?.id,
                isPlaying = track.id == currentTrack?.id && isPlaying,
                onClick = { onTrackSelected(track) }
            )
        }
    }
}

@Composable
private fun ChordPlayQuickPickCard(
    track: IosTrack,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceCard)
                .border(
                    width = if (isCurrent) 2.dp else 1.dp,
                    color = if (isCurrent) ChordPlayCrimson else SurfaceBorder,
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize()
            )
            // Quality Tag Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                BasicText(
                    text = track.qualityBadge,
                    style = TextStyle(
                        color = ChordPlayCrimsonGlow,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        BasicText(
            text = track.title,
            style = TextStyle(
                color = if (isCurrent) ChordPlayCrimsonGlow else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        BasicText(
            text = track.artist,
            style = TextStyle(color = TextMuted, fontSize = 13.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChordPlayTrackRow(
    track: IosTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceCard)
                .border(
                    width = if (isCurrent) 2.dp else 1.dp,
                    color = if (isCurrent) ChordPlayCrimson else SurfaceBorder,
                    shape = RoundedCornerShape(12.dp)
                )
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
                    color = if (isCurrent) ChordPlayCrimsonGlow else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = "${track.artist} • ${track.durationText}",
                    style = TextStyle(color = TextMuted, fontSize = 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Playing indicator or play glyph
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isPlaying) ChordPlayCrimson.copy(alpha = 0.2f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = if (isPlaying) "❚❚" else "▶",
                style = TextStyle(
                    color = if (isCurrent) ChordPlayCrimson else TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 3. Explore Tab
// ---------------------------------------------------------------------------
@Composable
private fun ChordPlayExploreTab(
    tracks: List<IosTrack>,
    onTrackSelected: (IosTrack) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 160.dp, start = 20.dp, end = 20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            BasicText(
                text = "Explore & Charts",
                style = TextStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            )
            BasicText(
                text = "TOP STREAMED MUSIC TODAY",
                style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Featured Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF2E0854), Color(0xFFFD3F69))
                        )
                    )
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(22.dp))
                    .clickable { onTrackSelected(tracks.first()) }
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x55000000))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        BasicText(
                            text = "GLOBAL TOP 50",
                            style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    BasicText(
                        text = "Global Hits & Viral Sounds",
                        style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    )
                    BasicText(
                        text = "Updated hourly with the hottest tracks",
                        style = TextStyle(color = Color(0xFFDDD6FE), fontSize = 12.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            BasicText(
                text = "Trending Releases",
                style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(tracks) { track ->
            ChordPlayTrackRow(
                track = track,
                isCurrent = false,
                isPlaying = false,
                onClick = { onTrackSelected(track) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 4. Search Tab
// ---------------------------------------------------------------------------
@Composable
private fun ChordPlaySearchTab(
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        // Modern Frosted Search Pill Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceCardElevated)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(text = "🔍", style = TextStyle(fontSize = 16.sp))
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        BasicText(
                            text = "Search songs, artists, albums...",
                            style = TextStyle(color = TextMuted, fontSize = 15.sp)
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(ChordPlayCrimson),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clickable { onQueryChange("") }
                            .padding(4.dp)
                    ) {
                        BasicText(
                            text = "✕",
                            style = TextStyle(color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 160.dp)
        ) {
            items(filteredTracks) { track ->
                ChordPlayTrackRow(
                    track = track,
                    isCurrent = false,
                    isPlaying = false,
                    onClick = { onTrackSelected(track) }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 5. Library Tab
// ---------------------------------------------------------------------------
@Composable
private fun ChordPlayLibraryTab(
    likedCount: Int,
    recentTracks: List<IosTrack>,
    onTrackSelected: (IosTrack) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 160.dp, start = 20.dp, end = 20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            BasicText(
                text = "Your Library",
                style = TextStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            )
            BasicText(
                text = "SAVED PLAYLISTS & OFFLINE CACHE",
                style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            )
            Spacer(modifier = Modifier.height(18.dp))

            // Liked Songs Card with ChordPlay Crimson Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(ChordPlayCrimsonDark, ChordPlayCrimsonGlow)
                        )
                    )
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0x33000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = "♥",
                            style = TextStyle(color = Color.White, fontSize = 24.sp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        BasicText(
                            text = "Liked Songs",
                            style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        BasicText(
                            text = "$likedCount favorite tracks • Auto-Downloaded",
                            style = TextStyle(color = Color(0xFFFFD8E4), fontSize = 13.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            BasicText(
                text = "Recently Played",
                style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(recentTracks) { track ->
            ChordPlayTrackRow(
                track = track,
                isCurrent = false,
                isPlaying = false,
                onClick = { onTrackSelected(track) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 6. Floating Liquid Glass MiniPlayer (ChordPlay Signature Floating Pill)
// ---------------------------------------------------------------------------
@Composable
private fun ChordPlayFloatingMiniPlayer(
    track: IosTrack,
    isPlaying: Boolean,
    progress: Float,
    onTogglePlay: () -> Unit,
    onExpand: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xF01A1822))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Color.Black)
            .clickable(onClick = onExpand)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork thumbnail with glowing border
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCardElevated)
                        .border(1.dp, ChordPlayCrimson.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
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
                        style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    BasicText(
                        text = track.artist,
                        style = TextStyle(color = TextMuted, fontSize = 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Play / Pause Circle Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(ChordPlayCrimson, ChordPlayCrimsonGlow)
                            )
                        )
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = if (isPlaying) "❚❚" else "▶",
                        style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Slim glowing progress bar on bottom edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color(0x22FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(ChordPlayCrimson)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 7. Floating Frosted Glass Bottom Navigation Bar (ChordPlay GlassNavBar)
// ---------------------------------------------------------------------------
@Composable
private fun ChordPlayGlassNavBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    val tabs = listOf(
        "Home" to "⌂",
        "Explore" to "🧭",
        "Search" to "🔍",
        "Library" to "📚"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xF0121218))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp))
            .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = Color.Black)
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { idx, (label, icon) ->
                val isSelected = selectedTab == idx
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) ChordPlayCrimson.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { onSelectTab(idx) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    BasicText(
                        text = icon,
                        style = TextStyle(
                            color = if (isSelected) ChordPlayCrimsonGlow else TextMuted,
                            fontSize = 18.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    BasicText(
                        text = label,
                        style = TextStyle(
                            color = if (isSelected) Color.White else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 8. Full-Screen NowPlaying Screen (The "Look For Play"!)
// ---------------------------------------------------------------------------
@Composable
private fun ChordPlayNowPlayingScreen(
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
    var useVinylMode by remember { mutableStateOf(true) }
    var showLyricsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(14000, easing = LinearEasing),
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
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF200A18),
                        Color(0xFF0F080F),
                        BackgroundDeep
                    )
                )
            )
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(52.dp))

            // Player Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minimize Chevron
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceCardElevated)
                        .border(1.dp, SurfaceBorder, CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(text = "▼", style = TextStyle(color = Color.White, fontSize = 14.sp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicText(
                        text = "PLAYING FROM CHORDPLAY",
                        style = TextStyle(
                            color = ChordPlayCrimsonGlow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    )
                    BasicText(
                        text = track.album,
                        style = TextStyle(color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Vinyl / Cover toggle button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceCardElevated)
                        .border(1.dp, SurfaceBorder, CircleShape)
                        .clickable { useVinylMode = !useVinylMode },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = if (useVinylMode) "💿" else "🖼",
                        style = TextStyle(fontSize = 16.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Artwork Section: Either Spinning Vinyl or High-Res Cover
            if (useVinylMode) {
                // Realistic ChordPlay Spinning Vinyl Record
                Box(
                    modifier = Modifier
                        .size(290.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A0A0A))
                        .border(5.dp, Color(0xFF1E1E22), CircleShape)
                        .border(15.dp, Color(0xFF141416), CircleShape)
                        .border(25.dp, Color(0xFF0F0F11), CircleShape)
                        .shadow(28.dp, CircleShape, spotColor = ChordPlayCrimson)
                        .rotate(rotation.value)
                        .clickable { useVinylMode = false },
                    contentAlignment = Alignment.Center
                ) {
                    // Center Album Label
                    Box(
                        modifier = Modifier
                            .size(125.dp)
                            .clip(CircleShape)
                            .border(3.dp, ChordPlayCrimson, CircleShape)
                    ) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = track.title,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }

                    // Spindle Hole
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF000000))
                            .border(2.dp, Color(0xFF555555), CircleShape)
                    )
                }
            } else {
                // Square Cover Card with Ambient Glow
                Box(
                    modifier = Modifier
                        .size(290.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(SurfaceCard)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(26.dp))
                        .shadow(28.dp, RoundedCornerShape(26.dp), spotColor = ChordPlayCrimson)
                        .clickable { useVinylMode = true }
                ) {
                    AsyncImage(
                        model = track.artworkUrl,
                        contentDescription = track.title,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Track Title & Favorite Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        text = track.title,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.4).sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicText(
                            text = track.artist,
                            style = TextStyle(color = ChordPlayCrimsonGlow, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Lossless badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x33FD3F69))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            BasicText(
                                text = track.qualityBadge,
                                style = TextStyle(color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            )
                        }
                    }
                }

                // Heart / Like button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isLiked) ChordPlayCrimson.copy(alpha = 0.2f) else SurfaceCardElevated)
                        .border(1.dp, if (isLiked) ChordPlayCrimson else SurfaceBorder, CircleShape)
                        .clickable(onClick = onToggleLike),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = if (isLiked) "♥" else "♡",
                        style = TextStyle(
                            color = if (isLiked) ChordPlayCrimson else Color.White,
                            fontSize = 22.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Interactive Progress Scrubber Slider
            val progressFraction = if (duration > 0f) (position / duration).coerceIn(0f, 1f) else 0f
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x2EFFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressFraction)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(ChordPlayCrimsonDark, ChordPlayCrimsonGlow)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BasicText(
                        text = formatSeconds(position),
                        style = TextStyle(color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    )
                    BasicText(
                        text = formatSeconds(duration),
                        style = TextStyle(color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Playback Transport Deck (Shuffle, Prev, Big Play, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceCardElevated)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(text = "🔀", style = TextStyle(fontSize = 18.sp))
                }

                // Prev Track
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(SurfaceCardElevated)
                        .border(1.dp, SurfaceBorder, CircleShape)
                        .clickable(onClick = onSkipPrev),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(text = "⏮", style = TextStyle(color = Color.White, fontSize = 22.sp))
                }

                // Main Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(ChordPlayCrimson, ChordPlayCrimsonGlow)
                            )
                        )
                        .border(2.dp, Color(0x55FFFFFF), CircleShape)
                        .shadow(20.dp, CircleShape, spotColor = ChordPlayCrimson)
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = if (isPlaying) "❚❚" else "▶",
                        style = TextStyle(color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    )
                }

                // Next Track
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(SurfaceCardElevated)
                        .border(1.dp, SurfaceBorder, CircleShape)
                        .clickable(onClick = onSkipNext),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(text = "⏭", style = TextStyle(color = Color.White, fontSize = 22.sp))
                }

                // Repeat Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceCardElevated)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(text = "🔁", style = TextStyle(fontSize = 18.sp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lyrics Drawer Trigger Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x33FD3F69))
                    .border(1.dp, ChordPlayCrimson.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .clickable { showLyricsSheet = !showLyricsSheet }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(text = "💬", style = TextStyle(fontSize = 14.sp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicText(
                        text = if (showLyricsSheet) "Hide Lyrics" else "View Synchronized Lyrics",
                        style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (showLyricsSheet && track.lyrics.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceCardElevated)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(track.lyrics) { line ->
                        BasicText(
                            text = line,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
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
