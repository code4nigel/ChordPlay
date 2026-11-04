package com.music.bitchord.desktop

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.icons.BitChordIcons

private val DOCK_BORDER_COLOR = Color.White.copy(alpha = 0.08f)
private val DOCK_SURFACE_COLOR = Color(0xFF101014).copy(alpha = 0.95f)

/**
 * ChordPlay Desktop Bottom Dock.
 *
 * Dedicated Spotify/Apple Music-grade bottom player dock:
 * - Left: High-res artwork thumbnail, track title, artist, lossless quality badge.
 * - Center: Primary playback controls, shuffle/repeat toggles, precision seekbar.
 * - Right: Volume slider, output device switcher, lyrics & queue panel toggles.
 */
@Composable
internal fun DesktopBottomDock(
    song: Song?,
    isPlaying: Boolean,
    isLoading: Boolean,
    progress: Float,
    positionMs: Long,
    durationMs: Long,
    volume: Float,
    shuffle: Boolean,
    repeatMode: DesktopRepeatMode,
    sidePanel: DesktopSidePanel?,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onRepeatModeChange: (DesktopRepeatMode) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenAudioOutput: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    isLiked: Boolean = false,
    onToggleLike: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = rememberDesktopArtworkPalette(song?.thumbnailUrl)

    Box(
        modifier
            .fillMaxWidth()
            .height(82.dp)
            .background(DOCK_SURFACE_COLOR)
            .border(width = 1.dp, color = DOCK_BORDER_COLOR)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: Song Info & Artwork
            Row(
                modifier = Modifier
                    .width(260.dp)
                    .clickable(enabled = song != null, onClick = onOpenNowPlaying),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (song != null) {
                    Box(
                        Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, DOCK_BORDER_COLOR, RoundedCornerShape(8.dp)),
                    ) {
                        DesktopArtwork(
                            url = song.thumbnailUrl,
                            contentDescription = song.title,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = song.title,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (onToggleLike != null) {
                                Spacer(Modifier.width(6.dp))
                                IconButton(
                                    onClick = onToggleLike,
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) BitChordIcons.HeartFilled else BitChordIcons.Heart,
                                        contentDescription = if (isLiked) "Liked" else "Like",
                                        tint = if (isLiked) palette.primaryGlow else DesktopSecondary,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = song.artist,
                                color = DesktopSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(palette.primaryGlow.copy(alpha = 0.20f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    text = song.sourceQuality?.ifBlank { "LOSSLESS" } ?: "320K",
                                    color = palette.primaryGlow,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No track playing",
                        color = DesktopSecondary.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // Center: Playback & Scrubbing Controls
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Shuffle
                    IconButton(
                        onClick = { onShuffleChange(!shuffle) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffle) palette.primaryGlow else DesktopSecondary,
                            modifier = Modifier.size(17.dp),
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = onPrevious,
                        enabled = song != null,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (song != null) Color.White else DesktopSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // Play / Pause Button with ChordPlay vibrant glow
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(palette.primaryGlow)
                            .clickable(enabled = song != null, onClick = onPlayPause),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    // Next
                    IconButton(
                        onClick = onNext,
                        enabled = song != null,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = if (song != null) Color.White else DesktopSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // Repeat
                    IconButton(
                        onClick = {
                            val next = when (repeatMode) {
                                DesktopRepeatMode.OFF -> DesktopRepeatMode.ALL
                                DesktopRepeatMode.ALL -> DesktopRepeatMode.ONE
                                DesktopRepeatMode.ONE -> DesktopRepeatMode.OFF
                            }
                            onRepeatModeChange(next)
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (repeatMode == DesktopRepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (repeatMode != DesktopRepeatMode.OFF) palette.primaryGlow else DesktopSecondary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                // Scrubber with Elapsed / Remaining Time
                Row(
                    modifier = Modifier.fillMaxWidth().height(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatTime(positionMs),
                        color = DesktopSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.width(38.dp),
                    )

                    DesktopThinSlider(
                        value = progress.coerceIn(0f, 1f),
                        onValueChange = onSeek,
                        idleHeight = 3.dp,
                        activeHeight = 6.dp,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )

                    Text(
                        text = formatTime(durationMs),
                        color = DesktopSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.width(38.dp),
                    )
                }
            }

            // Right: Volume & Panel Actions
            Row(
                modifier = Modifier.width(260.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.VolumeUp,
                    contentDescription = "Volume",
                    tint = DesktopSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                DesktopThinSlider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    idleHeight = 3.dp,
                    activeHeight = 6.dp,
                    modifier = Modifier.width(72.dp),
                )
                Spacer(Modifier.width(8.dp))

                // Device Switcher
                DesktopToolbarButton(onClick = onOpenAudioOutput, size = 30.dp) {
                    Icon(
                        imageVector = Icons.Rounded.Headphones,
                        contentDescription = "Audio output",
                        tint = DesktopSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                // Synced Lyrics Toggle
                DesktopToolbarButton(onClick = onOpenLyrics, size = 30.dp) {
                    Icon(
                        imageVector = BitChordIcons.LyricsQuote,
                        contentDescription = "Lyrics",
                        tint = if (sidePanel == DesktopSidePanel.LYRICS) palette.primaryGlow else DesktopSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                // Queue Toggle
                DesktopToolbarButton(onClick = onOpenQueue, size = 30.dp) {
                    Icon(
                        imageVector = BitChordIcons.Queue,
                        contentDescription = "Queue",
                        tint = if (sidePanel == DesktopSidePanel.QUEUE) palette.primaryGlow else DesktopSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                // Cinema Stage / Fullscreen Expand
                DesktopToolbarButton(onClick = onOpenNowPlaying, size = 30.dp) {
                    Icon(
                        imageVector = Icons.Rounded.FitScreen,
                        contentDescription = "Cinema Mode (F11)",
                        tint = palette.primaryGlow,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
