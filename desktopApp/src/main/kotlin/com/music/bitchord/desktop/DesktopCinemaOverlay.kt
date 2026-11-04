package com.music.bitchord.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.data.lyrics.LyricsSource
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.PlaybackPosition
import com.music.bitchord.ui.LyricsProviderState
import com.music.bitchord.ui.icons.BitChordIcons
import com.music.bitchord.ui.player.LyricsSidePanel
import com.music.bitchord.ui.player.QueueSidePanel

private val CINEMA_BG = Color(0xFF09090C)
private val CINEMA_BORDER = Color.White.copy(alpha = 0.08f)
private val CINEMA_CARD_BG = Color(0xFF14141A).copy(alpha = 0.85f)

private enum class CinemaTab {
    LYRICS,
    QUEUE,
}

/**
 * ChordPlay Desktop Panoramic Full-Screen / Cinema Mode.
 *
 * Replaces mobile modal sheet with a true desktop-first panoramic presentation:
 * - Left column (45%): Large high-res artwork with vibrant dynamic ambient aura,
 *   lossless playback badge, bold typography, transport controls, precision scrubber.
 * - Right column (55%): Synchronized karaoke lyrics with active-line glow or full queue.
 * - Top right: Desktop exit button (✕ / Esc), strictly eliminating mobile drag pill (—).
 */
@Composable
internal fun DesktopCinemaOverlay(
    visible: Boolean,
    song: Song?,
    isPlaying: Boolean,
    isLoading: Boolean,
    position: PlaybackPosition,
    durationMs: Long,
    format: DesktopStreamFormat?,
    isLiked: Boolean,
    shuffle: Boolean,
    repeatMode: DesktopRepeatMode,
    volume: Float,
    sharedLyrics: List<LyricLine>?,
    sharedLyricsSource: LyricsSource?,
    providerStates: Map<LyricsSource, LyricsProviderState>,
    sharedLyricsUnavailable: Boolean,
    queue: List<Song>,
    queueIndex: Int,
    autoplayEnabled: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleLike: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSelectLyricsProvider: (LyricsSource) -> Unit,
    onJumpToQueue: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onMoveInQueue: (from: Int, to: Int) -> Unit,
    onClearQueue: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible && song != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        if (song == null) return@AnimatedVisibility

        val palette = rememberDesktopArtworkPalette(song.thumbnailUrl)
        var selectedTab by remember { mutableStateOf(CinemaTab.LYRICS) }
        val positionMs = position.positionMs

        Box(
            Modifier
                .fillMaxSize()
                .background(CINEMA_BG)
        ) {
            // Skia hardware-accelerated ambient aura illumination
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                palette.ambientAura.copy(alpha = 0.35f),
                                palette.primaryGlow.copy(alpha = 0.12f),
                                Color.Transparent,
                            ),
                            radius = 1200f,
                        )
                    )
            )

            Column(Modifier.fillMaxSize()) {
                // Top Desktop Navigation Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CHORDPLAY",
                            color = palette.primaryGlow,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "·  CINEMA STAGE",
                            color = DesktopSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                        )
                    }

                    // Desktop Collapse / Close Button (Esc)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, CINEMA_BORDER, CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Exit Cinema Mode (Esc)",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // Panoramic Two-Column Layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 32.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    // Left Column (45% width): Large Art, Meta, Specs, Transport
                    Column(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        // High-Resolution Artwork with Ambient Glow Border
                        Box(
                            modifier = Modifier
                                .size(340.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, CINEMA_BORDER, RoundedCornerShape(16.dp)),
                        ) {
                            DesktopArtwork(
                                url = song.thumbnailUrl,
                                contentDescription = song.title,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        // Title & Artist
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = song.artist,
                            color = DesktopSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(Modifier.height(12.dp))

                        // Lossless Stats Pill & Heart Action
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // Favorite toggle
                            IconButton(
                                onClick = onToggleLike,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = if (isLiked) BitChordIcons.HeartFilled else BitChordIcons.Heart,
                                    contentDescription = if (isLiked) "Liked" else "Like",
                                    tint = if (isLiked) palette.primaryGlow else DesktopSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            // Lossless Audio Stats Card Badge
                            val specs = format?.summary ?: if (!song.sourceQuality.isNullOrBlank()) {
                                "${song.sourceQuality} · Lossless Engine"
                            } else {
                                "ChordPlay Lossless Engine · 320 kbps"
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(palette.primaryGlow.copy(alpha = 0.15f))
                                    .border(1.dp, palette.primaryGlow.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = specs.uppercase(),
                                    color = palette.primaryGlow,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Transport Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            IconButton(onClick = onToggleShuffle, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (shuffle) palette.primaryGlow else DesktopSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp),
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(palette.primaryGlow)
                                    .clickable(onClick = onPlayPause),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.5.dp,
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(30.dp),
                                    )
                                }
                            }

                            IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp),
                                )
                            }

                            IconButton(onClick = onCycleRepeat, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = if (repeatMode == DesktopRepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                    contentDescription = "Repeat",
                                    tint = if (repeatMode != DesktopRepeatMode.OFF) palette.primaryGlow else DesktopSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Precision Scrubber
                        val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = formatTime(positionMs),
                                color = DesktopSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.width(42.dp),
                            )

                            DesktopThinSlider(
                                value = progress,
                                onValueChange = { fraction ->
                                    if (durationMs > 0) onSeek((fraction * durationMs).toLong())
                                },
                                idleHeight = 3.dp,
                                activeHeight = 7.dp,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            )

                            Text(
                                text = formatTime(durationMs),
                                color = DesktopSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.width(42.dp),
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Volume Row
                        Row(
                            modifier = Modifier.fillMaxWidth(0.6f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeUp,
                                contentDescription = "Volume",
                                tint = DesktopSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            DesktopThinSlider(
                                value = volume,
                                onValueChange = onVolumeChange,
                                idleHeight = 3.dp,
                                activeHeight = 6.dp,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // Right Column (55% width): Synchronized Karaoke Lyrics or Queue Pane
                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CINEMA_CARD_BG)
                            .border(1.dp, CINEMA_BORDER, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        // Tab Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CinemaTabButton(
                                title = "Synchronized Lyrics",
                                selected = selectedTab == CinemaTab.LYRICS,
                                accent = palette.primaryGlow,
                                onClick = { selectedTab = CinemaTab.LYRICS },
                            )
                            CinemaTabButton(
                                title = "Up Next (${queue.size})",
                                selected = selectedTab == CinemaTab.QUEUE,
                                accent = palette.primaryGlow,
                                onClick = { selectedTab = CinemaTab.QUEUE },
                            )
                        }

                        Box(Modifier.fillMaxSize()) {
                            when (selectedTab) {
                                CinemaTab.LYRICS -> {
                                    LyricsSidePanel(
                                        song = song,
                                        isPlaying = isPlaying,
                                        position = position,
                                        lyrics = sharedLyrics,
                                        lyricsSource = sharedLyricsSource,
                                        lyricsProviderStates = providerStates,
                                        onSelectLyricsProvider = onSelectLyricsProvider,
                                        lyricsUnavailable = sharedLyricsUnavailable,
                                        onSeek = onSeek,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                CinemaTab.QUEUE -> {
                                    QueueSidePanel(
                                        queue = queue,
                                        queueIndex = queueIndex,
                                        autoplayEnabled = autoplayEnabled,
                                        onJumpTo = onJumpToQueue,
                                        onRemove = onRemoveFromQueue,
                                        onMove = onMoveInQueue,
                                        onClear = onClearQueue,
                                        modifier = Modifier.fillMaxSize(),
                                    )
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
private fun CinemaTabButton(
    title: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val bg = if (selected) accent.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.05f)
    val textCol = if (selected) accent else DesktopSecondary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = title,
            color = textCol,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
