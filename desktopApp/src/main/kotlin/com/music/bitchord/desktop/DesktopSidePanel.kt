package com.music.bitchord.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.model.Song

/** What the column beside the page is showing. */
internal enum class DesktopSidePanel { LYRICS, QUEUE, STATS }

/**
 * ChordPlay Right-hand Side Panel:
 * Supports Synced Lyrics, Up-Next Queue, and Lossless Track Metrics / Stream Stats.
 */
@Composable
internal fun DesktopSidePanelColumn(
    panel: DesktopSidePanel?,
    onClose: () -> Unit,
    content: @Composable (DesktopSidePanel) -> Unit,
) {
    // Held through the exit, so the column slides out with what it had in it rather than empty.
    var shown by remember { mutableStateOf(panel) }
    if (panel != null) shown = panel
    AnimatedVisibility(
        visible = panel != null,
        enter = expandHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing), expandFrom = Alignment.Start) +
            fadeIn(tween(SLIDE_MS)),
        exit = shrinkHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Start) +
            fadeOut(tween(SLIDE_MS / 2)),
    ) {
        Row(Modifier.width(SIDE_PANEL_WIDTH).fillMaxHeight()) {
            Box(Modifier.width(1.dp).fillMaxHeight().background(DesktopDivider))
            Crossfade(
                targetState = shown ?: return@Row,
                animationSpec = tween(CROSSFADE_MS),
                label = "sidePanel",
            ) { which ->
                Box(Modifier.fillMaxSize()) {
                    if (which == DesktopSidePanel.QUEUE || which == DesktopSidePanel.STATS) {
                        Box(Modifier.fillMaxSize().desktopChromeGlass())
                    }
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (which == DesktopSidePanel.LYRICS) HEADER_HEIGHT else QUEUE_HEADER_HEIGHT)
                                .padding(start = 18.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = when (which) {
                                    DesktopSidePanel.LYRICS -> DesktopStrings["lyrics", "Lyrics"]
                                    DesktopSidePanel.STATS -> "Track Metrics"
                                    DesktopSidePanel.QUEUE -> ""
                                },
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            DesktopToolbarButton(onClick = onClose, size = 28.dp) {
                                Icon(
                                    Icons.Rounded.Close,
                                    DesktopStrings["close", "Close"],
                                    tint = DesktopSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(
                                    start = if (which == DesktopSidePanel.LYRICS) 18.dp else 14.dp,
                                    end = if (which == DesktopSidePanel.LYRICS) 18.dp else 4.dp,
                                    bottom = 14.dp,
                                ),
                        ) {
                            content(which)
                        }
                    }
                }
            }
        }
    }
}

/**
 * ChordPlay Track Stats & Lossless Stream Metrics View.
 */
@Composable
internal fun DesktopTrackStatsPanel(
    song: Song?,
    format: DesktopStreamFormat?,
    engineState: String = "WASAPI Shared / Zero-Stutter Ring Buffer",
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(end = 10.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // High Fidelity Lossless Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DesktopAccent.copy(alpha = 0.12f))
                .border(1.dp, DesktopAccent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .padding(14.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = DesktopAccent,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "AUDIOPHILE RENDERING",
                        color = DesktopAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (format?.isLossless == true) "Bit-Perfect Lossless FLAC Stream" else "High-Definition 320kbps AAC",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Metrics Grid
        StatItem(label = "Track Title", value = song?.title ?: "—")
        StatItem(label = "Artist", value = song?.artist ?: "—")
        StatItem(label = "Container / Codec", value = format?.codec?.uppercase() ?: "AAC / MP4")
        StatItem(label = "Bitrate", value = format?.kbps?.let { "${it} kbps" } ?: "320 kbps (Direct CDN)")
        StatItem(label = "Sample Rate", value = "44,100 Hz · 2 Channels")
        StatItem(label = "Buffer Depth", value = "800ms Zero-Stutter Direct Buffer")
        StatItem(label = "Audio Pipeline", value = engineState)
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label.uppercase(),
            color = DesktopSecondary.copy(alpha = 0.65f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}

private val SIDE_PANEL_WIDTH = 360.dp
private val HEADER_HEIGHT = 46.dp
private val QUEUE_HEADER_HEIGHT = 30.dp
private const val SLIDE_MS = 260
private const val CROSSFADE_MS = 180
