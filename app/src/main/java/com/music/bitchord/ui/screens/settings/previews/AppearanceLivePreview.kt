package com.music.bitchord.ui.screens.settings.previews

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.music.bitchord.data.settings.ThemeMode
import com.music.bitchord.ui.icons.BitChordIcons

/**
 * Authentic live preview faithfully mirroring ChordPlay's home components:
 * Speed Dial tiles and floating MiniPlayer pill, accurately responding to
 * design style (ChordPlay Modern vs Classic BitChord), palettes, and glass surfaces.
 */
@Composable
fun AppearanceLivePreview(
    modifier: Modifier = Modifier,
) {
    val showLivePreviews by AppSettings.showSettingsLivePreviews.collectAsStateWithLifecycle()
    val theme by AppSettings.themeMode.collectAsStateWithLifecycle()
    val amoledDarkMode by AppSettings.amoledDarkMode.collectAsStateWithLifecycle()
    val designStyle by AppSettings.appDesignStyle.collectAsStateWithLifecycle()
    val palette by AppSettings.colorPalette.collectAsStateWithLifecycle()
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val activeArtworkUrl by AppSettings.activeTrackArtworkUrl.collectAsStateWithLifecycle()
    val activeTitle by AppSettings.activeTrackTitle.collectAsStateWithLifecycle()
    val activeArtist by AppSettings.activeTrackArtist.collectAsStateWithLifecycle()
    val customPalettes by AppSettings.customPalettes.collectAsStateWithLifecycle()
    val activeCustomPaletteId by AppSettings.activeCustomPaletteId.collectAsStateWithLifecycle()
    val activeCustomPalette = activeCustomPaletteId?.let { id ->
        customPalettes.firstOrNull { it.id == id }
    }

    val isLight = theme == ThemeMode.LIGHT
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
        if (isLight) Color(0xFF1E1E1E) else Color.White
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

    val targetBg = when {
        !isLight && amoledDarkMode -> Color.Black
        !isChordPlay -> if (isLight) MaterialTheme.colorScheme.surface else Color(0xFF101114)
        activeCustomPalette != null -> {
            Color(activeCustomPalette.surfaceHex).copy(alpha = if (reduceDynamicBlur) 1f else 0.90f)
        }
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

    val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = tween(300), label = "appearanceBg")
    val animatedPrimary by animateColorAsState(targetValue = activePrimary, animationSpec = tween(300), label = "appearancePrimary")

    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant

    val borderColor = when {
        !isChordPlay -> if (isLight) MaterialTheme.colorScheme.outline.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.10f)
        liquidGlass -> animatedPrimary.copy(alpha = 0.35f)
        isLight -> MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
        else -> animatedPrimary.copy(alpha = 0.22f)
    }

    val cardCornerRadius = if (isChordPlay) 22.dp else 14.dp

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
                        tint = subtextColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.preview) + " • Tap to expand",
                        style = MaterialTheme.typography.bodySmall,
                        color = subtextColor,
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

    // Expanded State: Elegant, refined preview card
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(
                elevation = if (isChordPlay) 12.dp else 4.dp,
                shape = RoundedCornerShape(cardCornerRadius),
                spotColor = animatedPrimary.copy(alpha = if (isChordPlay) 0.18f else 0.05f),
            )
            .clip(RoundedCornerShape(cardCornerRadius))
            .background(animatedBg)
            .border(1.dp, borderColor, RoundedCornerShape(cardCornerRadius))
            .padding(14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Subtle status + Active Style summary + Collapse toggle
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
                        text = if (isChordPlay) "ChordPlay Modern" else "Classic BitChord",
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

            Spacer(Modifier.height(10.dp))

            // Speed Dial Row: 2 authentic tiles matching HomeSpeedDial.kt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Tile 1: Current track / favorite
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isLight) 0.75f else 0.45f))
                        .border(1.dp, Color.White.copy(alpha = if (isChordPlay) 0.14f else 0.08f), RoundedCornerShape(12.dp)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
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
                                    painter = painterResource(R.drawable.ic_logo),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                        ) {
                            Text(
                                text = activeTitle ?: "Pal Pal",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = textColor,
                            )
                            Text(
                                text = activeArtist ?: "Arijit Singh",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = subtextColor,
                            )
                        }
                    }
                }

                // Tile 2: Mix / Station with micro badge
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isLight) 0.75f else 0.45f))
                        .border(1.dp, Color.White.copy(alpha = if (isChordPlay) 0.14f else 0.08f), RoundedCornerShape(12.dp)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                                .background(Brush.linearGradient(listOf(activeSecondary, animatedPrimary))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(animatedPrimary.copy(alpha = 0.85f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .align(Alignment.TopStart),
                            ) {
                                Text(
                                    text = "MIX",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = Color.White,
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                        ) {
                            Text(
                                text = "Discover Mix",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = textColor,
                            )
                            Text(
                                text = "Personalized",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = subtextColor,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Floating MiniPlayer Pill faithfully matching MiniPlayer.kt
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        if (isChordPlay) {
                            MaterialTheme.colorScheme.surface.copy(alpha = if (liquidGlass) 0.88f else 0.70f)
                        } else {
                            if (isLight) Color.White.copy(alpha = 0.90f) else Color(0xFF1A1B20)
                        },
                    )
                    .border(
                        1.dp,
                        if (isChordPlay) {
                            animatedPrimary.copy(alpha = if (liquidGlass) 0.45f else 0.28f)
                        } else {
                            Color.White.copy(alpha = 0.12f)
                        },
                        RoundedCornerShape(percent = 50),
                    )
                    .padding(horizontal = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Home glyph matching bottom navigation
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = BitChordIcons.Home,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(15.dp),
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Artwork thumbnail
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(7.dp))
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
                                painter = painterResource(R.drawable.ic_logo),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Active track name
                    Text(
                        text = activeTitle ?: "Pal Pal",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = textColor,
                        modifier = Modifier.weight(1f),
                    )

                    // Play icon
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = if (isChordPlay) animatedPrimary else textColor,
                        modifier = Modifier.size(22.dp),
                    )

                    Spacer(Modifier.width(10.dp))

                    // Search glyph matching GlassNavBar
                    Icon(
                        imageVector = BitChordIcons.TabSearch,
                        contentDescription = null,
                        tint = subtextColor,
                        modifier = Modifier.size(17.dp),
                    )

                    Spacer(Modifier.width(4.dp))
                }
            }
        }
    }
}
