package com.music.bitchord.ui.screens.settings.previews

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppDesignStyle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.ChordPlayPalette
import com.music.bitchord.data.settings.PlayerInterfaceStyle
import com.music.bitchord.data.settings.PlayerLayoutMode
import com.music.bitchord.ui.icons.BitChordIcons
import com.music.bitchord.ui.screens.settings.localizedLabel

/**
 * Authentic live interactive player preview faithfully mirroring ChordPlay's
 * Now Playing screen: reflects layout mode (Modern Ambient vs Vinyl Turntable vs Classic),
 * interface style (WavePlay vs ChordPlay OG), breathing aura, and current song metadata.
 */
@Composable
fun PlayerLivePreview(
    modifier: Modifier = Modifier,
) {
    val showLivePreviews by AppSettings.showSettingsLivePreviews.collectAsStateWithLifecycle()
    val designStyle by AppSettings.appDesignStyle.collectAsStateWithLifecycle()
    val palette by AppSettings.colorPalette.collectAsStateWithLifecycle()
    val playerLayoutMode by AppSettings.playerLayoutMode.collectAsStateWithLifecycle()
    val playerInterfaceStyle by AppSettings.playerInterfaceStyle.collectAsStateWithLifecycle()
    val enableBreathingAura by AppSettings.enableBreathingAura.collectAsStateWithLifecycle()
    val activeArtworkUrl by AppSettings.activeTrackArtworkUrl.collectAsStateWithLifecycle()
    val activeTitle by AppSettings.activeTrackTitle.collectAsStateWithLifecycle()
    val activeArtist by AppSettings.activeTrackArtist.collectAsStateWithLifecycle()
    val customPalettes by AppSettings.customPalettes.collectAsStateWithLifecycle()
    val activeCustomPaletteId by AppSettings.activeCustomPaletteId.collectAsStateWithLifecycle()
    val activeCustomPalette = activeCustomPaletteId?.let { id ->
        customPalettes.firstOrNull { it.id == id }
    }

    val isChordPlay = designStyle == AppDesignStyle.CHORDPLAY
    val activePrimary = if (activeCustomPalette != null) {
        Color(activeCustomPalette.primaryHex)
    } else if (isChordPlay) {
        if (palette == ChordPlayPalette.DYNAMIC_MONET) {
            MaterialTheme.colorScheme.primary
        } else {
            Color(palette.primaryColorHex)
        }
    } else {
        Color.White
    }

    val activeSecondary = if (activeCustomPalette != null) {
        Color(activeCustomPalette.secondaryHex)
    } else if (isChordPlay) {
        if (palette == ChordPlayPalette.DYNAMIC_MONET) {
            MaterialTheme.colorScheme.secondary
        } else {
            Color(palette.secondaryColorHex)
        }
    } else {
        Color(0xFFFA2D48)
    }

    val animatedPrimary by animateColorAsState(targetValue = activePrimary, animationSpec = tween(300), label = "playerPrimary")

    val isModern = playerLayoutMode == PlayerLayoutMode.MODERN_AMBIENT
    val isVinyl = playerLayoutMode == PlayerLayoutMode.VINYL_RECORD
    val isWavePlay = playerInterfaceStyle == PlayerInterfaceStyle.WAVEPLAY

    val textColor = Color.White
    val subtextColor = Color.White.copy(alpha = 0.65f)

    // Collapsed State: Lightweight single-row pill (0 GPU / recomposition overhead)
    if (!showLivePreviews) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .clickable { AppSettings.setShowSettingsLivePreviews(true) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.preview) + " • Tap to expand",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = animatedPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        return
    }

    // Expanded State: Elegant, refined player stage
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = animatedPrimary.copy(alpha = 0.20f))
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0C0D11))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp)),
    ) {
        // Atmospheric gradient backdrop
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            animatedPrimary.copy(alpha = 0.15f),
                            Color(0xFF0C0D11).copy(alpha = 0.70f),
                            Color(0xFF0C0D11),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header: Status indicator + Mode summary + Collapse toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(animatedPrimary),
                    )
                    Text(
                        text = "PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        ),
                        color = animatedPrimary,
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = subtextColor.copy(alpha = 0.5f),
                    )
                    Text(
                        text = "${playerLayoutMode.localizedLabel()} • ${playerInterfaceStyle.localizedLabel()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = subtextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(
                    onClick = { AppSettings.setShowSettingsLivePreviews(false) },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Collapse preview",
                        tint = subtextColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Artwork Stage (Modern Ambient vs Vinyl Turntable vs Classic Clean)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isVinyl -> {
                        // Vinyl Record sliding out from Album Jacket
                        Box(contentAlignment = Alignment.Center) {
                            // Disc sliding out to the right (static Canvas microgrooves)
                            Box(
                                modifier = Modifier
                                    .size(108.dp)
                                    .offset(x = 26.dp)
                                    .shadow(10.dp, CircleShape, spotColor = Color.Black)
                                    .clip(CircleShape)
                                    .background(Color(0xFF101114)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val center = this.center
                                    val maxR = size.minDimension / 2f
                                    val labelR = maxR * 0.38f
                                    var r = labelR + 4f
                                    while (r < maxR - 6f) {
                                        drawCircle(
                                            color = Color(0x18FFFFFF),
                                            radius = r,
                                            center = center,
                                            style = Stroke(width = 1f),
                                        )
                                        r += 5f
                                    }
                                    drawCircle(
                                        color = Color(0x30FFFFFF),
                                        radius = maxR - 2f,
                                        center = center,
                                        style = Stroke(width = 1.2f),
                                    )
                                    val sheen = Brush.sweepGradient(
                                        0.00f to Color.Transparent,
                                        0.12f to Color(0x20FFFFFF),
                                        0.25f to Color.Transparent,
                                        0.50f to Color.Transparent,
                                        0.62f to Color(0x20FFFFFF),
                                        0.75f to Color.Transparent,
                                        1.00f to Color.Transparent,
                                    )
                                    drawCircle(brush = sheen, radius = maxR, center = center)
                                }

                                // Center label with album artwork
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Color(0x40FFFFFF), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!activeArtworkUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = activeArtworkUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Brush.linearGradient(listOf(animatedPrimary, activeSecondary))),
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF08090C))
                                            .border(1.dp, Color(0xFF6B6E78), CircleShape),
                                    )
                                }
                            }

                            // Album Jacket on the left
                            Box(
                                modifier = Modifier
                                    .size(114.dp)
                                    .offset(x = (-24).dp)
                                    .shadow(12.dp, RoundedCornerShape(12.dp), spotColor = Color.Black)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(animatedPrimary, activeSecondary)))
                                    .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!activeArtworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = activeArtworkUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_logo),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.90f),
                                        modifier = Modifier.size(42.dp),
                                    )
                                }
                            }
                        }
                    }
                    isModern -> {
                        // Modern Ambient with Static Breathing Aura
                        Box(contentAlignment = Alignment.Center) {
                            if (enableBreathingAura) {
                                Box(
                                    modifier = Modifier
                                        .size(126.dp)
                                        .blur(22.dp)
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(animatedPrimary.copy(alpha = 0.38f)),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(116.dp)
                                    .shadow(14.dp, RoundedCornerShape(18.dp), spotColor = Color.Black)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Brush.linearGradient(listOf(animatedPrimary, activeSecondary)))
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!activeArtworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = activeArtworkUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_logo),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.90f),
                                        modifier = Modifier.size(44.dp),
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        // Classic Clean Apple Music-style Square Cover
                        Box(
                            modifier = Modifier
                                .size(116.dp)
                                .shadow(10.dp, RoundedCornerShape(8.dp), spotColor = Color.Black)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(animatedPrimary, activeSecondary)))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!activeArtworkUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = activeArtworkUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_logo),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.90f),
                                    modifier = Modifier.size(44.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Real Track Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeTitle ?: "Legends Never Die",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = textColor,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = activeArtist ?: "Against The Current",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.5.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = subtextColor,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = BitChordIcons.HeartFilled,
                        contentDescription = null,
                        tint = animatedPrimary,
                        modifier = Modifier.size(19.dp),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = null,
                        tint = subtextColor,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Seekbar Area: WavePlay Dynamic Waveform vs Classic Clean Scrubber
            if (isWavePlay) {
                // WavePlay Mini Waveform visualization
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val barHeights = listOf(
                            0.35f, 0.65f, 0.45f, 0.85f, 0.55f, 0.95f, 0.70f, 0.40f,
                            0.75f, 0.90f, 0.60f, 0.80f, 0.50f, 0.35f, 0.65f, 0.45f,
                            0.75f, 0.55f, 0.40f, 0.60f, 0.30f, 0.45f, 0.35f, 0.25f,
                        )
                        barHeights.forEachIndexed { index, heightFactor ->
                            val isPlayed = index < 8
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height((14 * heightFactor).dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(if (isPlayed) animatedPrimary else Color.White.copy(alpha = 0.22f)),
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "1:24",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = subtextColor,
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = "LOSSLESS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }

                        Text(
                            text = "-2:18",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = subtextColor,
                        )
                    }
                }
            } else {
                // ChordPlay OG / Classic Scrubber
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White.copy(alpha = 0.20f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.35f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(animatedPrimary),
                        )
                    }

                    Spacer(Modifier.height(5.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "1:24",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = subtextColor,
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = "LOSSLESS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }

                        Text(
                            text = "-2:18",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = subtextColor,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Transport Row: Authentic Player Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = null,
                    tint = subtextColor,
                    modifier = Modifier.size(18.dp),
                )

                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(24.dp),
                )

                // Play Button
                Box(
                    modifier = Modifier
                        .size(if (isWavePlay) 46.dp else 42.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            spotColor = animatedPrimary,
                        )
                        .clip(CircleShape)
                        .background(animatedPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(24.dp),
                )

                Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = null,
                    tint = subtextColor,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            // Bottom Action Row: Lyrics Quote + Output Device capsule + Queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Icon(
                    imageVector = BitChordIcons.LyricsQuote,
                    contentDescription = null,
                    tint = subtextColor,
                    modifier = Modifier.size(18.dp),
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Phone Speaker",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }

                Icon(
                    imageVector = BitChordIcons.Queue,
                    contentDescription = null,
                    tint = subtextColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
