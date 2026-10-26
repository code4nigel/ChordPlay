package com.music.bitchord.ui.screens.settings.previews

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import com.music.bitchord.data.settings.ThemeMode

/**
 * Authentic 1:1 live dashboard preview showing how theme mode, ChordPlay color palette,
 * glass effects, and blur interact on the actual home screen components.
 */
@Composable
fun AppearanceLivePreview(
    modifier: Modifier = Modifier,
) {
    val theme by AppSettings.themeMode.collectAsStateWithLifecycle()
    val designStyle by AppSettings.appDesignStyle.collectAsStateWithLifecycle()
    val palette by AppSettings.colorPalette.collectAsStateWithLifecycle()
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val activeArtworkUrl by AppSettings.activeTrackArtworkUrl.collectAsStateWithLifecycle()

    val isLight = theme == ThemeMode.LIGHT
    val isChordPlay = designStyle == AppDesignStyle.CHORDPLAY

    val activePrimary = if (isChordPlay) {
        if (palette == ChordPlayPalette.DYNAMIC_MONET) {
            MaterialTheme.colorScheme.primary
        } else {
            Color(palette.primaryColorHex)
        }
    } else {
        if (isLight) Color.Black else Color.White
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

    val targetBg = when {
        !isChordPlay -> if (isLight) MaterialTheme.colorScheme.surface else Color(0xFF0F1012)
        palette == ChordPlayPalette.DYNAMIC_MONET -> {
            if (reduceDynamicBlur) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
            }
        }
        isLight && reduceDynamicBlur -> Color(0xFFF0F2F6)
        isLight -> Color(0xFFF7F8FA).copy(alpha = 0.95f)
        reduceDynamicBlur -> when (palette) {
            ChordPlayPalette.CHORDPLAY_CRIMSON -> Color(0xFF160D12)
            ChordPlayPalette.ELECTRIC_VIOLET -> Color(0xFF141622)
            ChordPlayPalette.SUNSET_AMBER -> Color(0xFF1A1410)
            ChordPlayPalette.EMERALD_NOIR -> Color(0xFF0F1D16)
            ChordPlayPalette.MONOCHROME -> Color(0xFF121214)
            ChordPlayPalette.DYNAMIC_ARTWORK -> MaterialTheme.colorScheme.surface
            ChordPlayPalette.DYNAMIC_MONET -> MaterialTheme.colorScheme.surfaceVariant
        }
        else -> when (palette) {
            ChordPlayPalette.CHORDPLAY_CRIMSON -> Color(0xFF160D12).copy(alpha = 0.90f)
            ChordPlayPalette.ELECTRIC_VIOLET -> Color(0xFF141622).copy(alpha = 0.90f)
            ChordPlayPalette.SUNSET_AMBER -> Color(0xFF1A1410).copy(alpha = 0.90f)
            ChordPlayPalette.EMERALD_NOIR -> Color(0xFF0F1D16).copy(alpha = 0.90f)
            ChordPlayPalette.MONOCHROME -> Color(0xFF121214).copy(alpha = 0.90f)
            ChordPlayPalette.DYNAMIC_ARTWORK -> MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
            ChordPlayPalette.DYNAMIC_MONET -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f)
        }
    }

    val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = tween(300), label = "bg")
    val animatedPrimary by animateColorAsState(targetValue = activePrimary, animationSpec = tween(300), label = "primary")

    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant

    val borderColor = when {
        !isChordPlay -> if (isLight) MaterialTheme.colorScheme.outline.copy(alpha = 0.30f) else Color(0xFF2C2C2E)
        liquidGlass -> animatedPrimary.copy(alpha = 0.45f)
        isLight -> MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        else -> animatedPrimary.copy(alpha = 0.25f)
    }

    val cardCornerRadius = if (isChordPlay) 24.dp else 16.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(12.dp, RoundedCornerShape(cardCornerRadius), spotColor = animatedPrimary.copy(alpha = 0.20f))
            .clip(RoundedCornerShape(cardCornerRadius))
            .background(animatedBg)
            .border(1.dp, borderColor, RoundedCornerShape(cardCornerRadius))
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: ChordPlay Logo + LIVE PREVIEW badge
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
                        painter = painterResource(R.drawable.ic_logo),
                        contentDescription = null,
                        tint = animatedPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "ChordPlay",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = animatedPrimary,
                        letterSpacing = 0.5.sp,
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(animatedPrimary.copy(alpha = 0.16f))
                        .border(1.dp, animatedPrimary.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "LIVE PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        ),
                        color = animatedPrimary,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Greeting line
            Text(
                text = "Back in the mix.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )

            Spacer(Modifier.height(12.dp))

            // Mini Category Filter Pills: All, Chill, Energy, Focus
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val moods = listOf(
                    "All" to true,
                    "Chill" to false,
                    "Energy" to false,
                    "Focus" to false,
                )
                moods.forEach { (mood, isSelected) ->
                    val pillBg = if (isSelected) {
                        animatedPrimary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isLight) 0.8f else 0.45f)
                    }
                    val pillTextColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        subtextColor
                    }
                    val pillBorder = if (isSelected) {
                        BorderStrokeModifier(animatedPrimary)
                    } else {
                        Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(pillBg)
                            .then(pillBorder)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = mood,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = pillTextColor,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 2 Mini Speed Dial Cards (Legends Never Die & Starboy)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Speed Dial Card 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isLight) 0.8f else 0.5f))
                        .border(1.dp, animatedPrimary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .padding(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(listOf(animatedPrimary, activeSecondary)),
                                ),
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
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Legends Never Die",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = textColor,
                            )
                            Text(
                                text = "League of Legends",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = subtextColor,
                            )
                        }
                    }
                }

                // Speed Dial Card 2
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isLight) 0.8f else 0.5f))
                        .border(1.dp, animatedPrimary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .padding(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(listOf(activeSecondary, animatedPrimary)),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Starboy",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = textColor,
                            )
                            Text(
                                text = "The Weeknd",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = subtextColor,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Mini Floating Player Pill with Real Playback Controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (liquidGlass) 0.85f else 0.65f),
                    )
                    .border(
                        1.dp,
                        if (liquidGlass) animatedPrimary.copy(alpha = 0.40f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                        RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Mini artwork
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(animatedPrimary, activeSecondary))),
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
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    // Track & Artist
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Legends Never Die",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = textColor,
                        )
                        Text(
                            text = "League of Legends",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = subtextColor,
                        )
                    }

                    // Like Heart
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = animatedPrimary,
                        modifier = Modifier.size(18.dp),
                    )

                    Spacer(Modifier.width(10.dp))

                    // Play Button Circle
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .shadow(4.dp, CircleShape, spotColor = animatedPrimary)
                            .clip(CircleShape)
                            .background(animatedPrimary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Skip Next
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = null,
                        tint = subtextColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

private fun BorderStrokeModifier(color: Color): Modifier =
    Modifier.border(1.dp, color, RoundedCornerShape(16.dp))
