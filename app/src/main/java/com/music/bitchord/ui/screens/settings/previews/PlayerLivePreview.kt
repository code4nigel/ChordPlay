package com.music.bitchord.ui.screens.settings.previews

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.data.settings.AppDesignStyle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.ChordPlayPalette
import com.music.bitchord.data.settings.PlayerLayoutMode
import com.music.bitchord.ui.icons.BitChordIcons

/**
 * Pinned live interactive player preview faithfully mirroring the real ChordPlay
 * Now Playing experience with 1:1 authentic layout, atmosphere, and playback controls.
 */
@Composable
fun PlayerLivePreview(
    modifier: Modifier = Modifier,
) {
    val designStyle by AppSettings.appDesignStyle.collectAsStateWithLifecycle()
    val palette by AppSettings.colorPalette.collectAsStateWithLifecycle()
    val playerLayoutMode by AppSettings.playerLayoutMode.collectAsStateWithLifecycle()
    val enableBreathingAura by AppSettings.enableBreathingAura.collectAsStateWithLifecycle()
    val activeArtworkUrl by AppSettings.activeTrackArtworkUrl.collectAsStateWithLifecycle()

    val isChordPlay = designStyle == AppDesignStyle.CHORDPLAY
    val activePrimary = if (isChordPlay) {
        if (palette == ChordPlayPalette.DYNAMIC_MONET) {
            MaterialTheme.colorScheme.primary
        } else {
            Color(palette.primaryColorHex)
        }
    } else {
        Color.White
    }

    val activeSecondary = if (isChordPlay) {
        if (palette == ChordPlayPalette.DYNAMIC_MONET) {
            MaterialTheme.colorScheme.secondary
        } else {
            Color(palette.secondaryColorHex)
        }
    } else {
        Color(0xFFFA2D48)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "playerLivePreviewTransition")
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "auraPulse",
    )

    val vinylRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vinylRotation",
    )

    val isModern = playerLayoutMode == PlayerLayoutMode.MODERN_AMBIENT
    val isVinyl = playerLayoutMode == PlayerLayoutMode.VINYL_RECORD

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = activePrimary.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF0C0D11))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(26.dp)),
    ) {
        // Full-bleed atmospheric blurred artwork backdrop with dark vignette
        Box(modifier = Modifier.matchParentSize()) {
            if (!activeArtworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = activeArtworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(40.dp)
                        .graphicsLayer { alpha = 0.40f },
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    activePrimary.copy(alpha = 0.35f),
                                    activeSecondary.copy(alpha = 0.20f),
                                    Color(0xFF0C0D11),
                                ),
                            ),
                        ),
                )
            }
            // Vertical dark vignette gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.88f),
                            ),
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Drag Pill Handle
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f)),
            )

            Spacer(Modifier.height(10.dp))

            // Header: "Playing from Lucky Radio" + LIVE PREVIEW badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.60f),
                        modifier = Modifier.size(18.dp),
                    )
                    Column {
                        Text(
                            text = "PLAYING FROM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                            ),
                            color = Color.White.copy(alpha = 0.50f),
                        )
                        Text(
                            text = "Lucky Radio",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Color.White.copy(alpha = 0.90f),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(activePrimary.copy(alpha = 0.20f))
                        .border(1.dp, activePrimary.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "LIVE PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        ),
                        color = activePrimary,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Artwork Stage (Modern Ambient with Breathing Aura vs Vinyl Turntable vs Classic Clean)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isVinyl -> {
                        // Vinyl Record sliding out from Album Jacket
                        Box(contentAlignment = Alignment.Center) {
                            // Spinning Vinyl Record disc sliding out to the right
                            Box(
                                modifier = Modifier
                                    .size(112.dp)
                                    .offset(x = 28.dp)
                                    .shadow(12.dp, CircleShape, spotColor = Color.Black)
                                    .clip(CircleShape)
                                    .background(Color(0xFF101114)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val center = this.center
                                    val maxR = size.minDimension / 2f
                                    val labelR = maxR * 0.38f

                                    // Micro-grooves
                                    var r = labelR + 4f
                                    while (r < maxR - 6f) {
                                        drawCircle(
                                            color = Color(0x18FFFFFF),
                                            radius = r,
                                            center = center,
                                            style = Stroke(width = 1f),
                                        )
                                        r += 4.5f
                                    }

                                    // Lead-in groove
                                    drawCircle(
                                        color = Color(0x30FFFFFF),
                                        radius = maxR - 2f,
                                        center = center,
                                        style = Stroke(width = 1.2f),
                                    )

                                    // Anisotropic sheen
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

                                // Center label with rotating artwork
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .rotate(vinylRotation)
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
                                                .background(Brush.linearGradient(listOf(activePrimary, activeSecondary))),
                                        )
                                    }
                                    // Spindle hole
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
                                    .size(118.dp)
                                    .offset(x = (-26).dp)
                                    .shadow(14.dp, RoundedCornerShape(12.dp), spotColor = Color.Black)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(activePrimary, activeSecondary)))
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
                                        painter = androidx.compose.ui.res.painterResource(com.music.bitchord.R.drawable.ic_logo),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.90f),
                                        modifier = Modifier.size(44.dp),
                                    )
                                }
                            }
                        }
                    }
                    isModern -> {
                        // Modern Ambient with Breathing Aura
                        Box(contentAlignment = Alignment.Center) {
                            if (enableBreathingAura) {
                                val auraScale = 1.05f + auraPulse * 0.12f
                                val auraAlpha = 0.40f + auraPulse * 0.35f
                                Box(
                                    modifier = Modifier
                                        .size((120 * auraScale).dp)
                                        .blur(26.dp)
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(activePrimary.copy(alpha = auraAlpha)),
                                )
                            }
                            // Squircle album cover
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Color.Black)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Brush.linearGradient(listOf(activePrimary, activeSecondary)))
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
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
                                        painter = androidx.compose.ui.res.painterResource(com.music.bitchord.R.drawable.ic_logo),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.90f),
                                        modifier = Modifier.size(46.dp),
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        // Classic Clean Apple Music-style Square Cover
                        Box(
                            modifier = Modifier
                                .size(122.dp)
                                .shadow(12.dp, RoundedCornerShape(8.dp), spotColor = Color.Black)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(activePrimary, activeSecondary)))
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
                                    painter = androidx.compose.ui.res.painterResource(com.music.bitchord.R.drawable.ic_logo),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.90f),
                                    modifier = Modifier.size(46.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Real Song Metadata Row: Title, Artist, Heart (Like), Add to Playlist (+), More (3-dots)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Legends Never Die",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "League of Legends, Against The Current",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = BitChordIcons.HeartFilled,
                        contentDescription = null,
                        tint = activePrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp),
                    )
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Real Lyrics Teaser Snippet: "When the world is callin' you >"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "\"When the world is callin' you\"",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontStyle = FontStyle.Italic,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = activePrimary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Accurate Thin Scrubber with 140K OPUS Audio Badge & Timestamps
            Column(modifier = Modifier.fillMaxWidth()) {
                // Scrubber Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color.White.copy(alpha = 0.20f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.22f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(activePrimary),
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Timestamps and Audio Badge Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "0:17",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White.copy(alpha = 0.65f),
                    )

                    // Audio Fidelity Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "140K OPUS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }

                    Text(
                        text = "-3:37",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Transport Buttons: Rewind (<<), Accent-filled Play Circle, Fast-Forward (>>)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.FastRewind,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )

                Spacer(Modifier.width(28.dp))

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(
                            elevation = 14.dp,
                            shape = CircleShape,
                            spotColor = activePrimary,
                            ambientColor = activePrimary.copy(alpha = 0.5f),
                        )
                        .clip(CircleShape)
                        .background(activePrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(30.dp),
                    )
                }

                Spacer(Modifier.width(28.dp))

                Icon(
                    imageVector = Icons.Rounded.FastForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            // Bottom Frosted Capsule Deck: 4 Icons + "Phone Speaker"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Lyrics bubble
                    Icon(
                        imageVector = BitChordIcons.LyricsQuote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp),
                    )

                    // Headphones
                    Icon(
                        imageVector = Icons.Rounded.Headphones,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp),
                    )

                    // Phone Speaker caption
                    Text(
                        text = "Phone Speaker",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = Color.White.copy(alpha = 0.85f),
                    )

                    // Profile / Party
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp),
                    )

                    // Queue list
                    Icon(
                        imageVector = BitChordIcons.Queue,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
