package com.music.bitchord.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.ui.components.optimizedHazeEffect
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.utils.containSheetGestures
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.roundToInt

private val DrawerShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
private val ScrimColor = Color.Black.copy(alpha = 0.5f)
private val DrawerMaxWidth = 640.dp
private const val DismissDragFraction = 0.25f

/**
 * Frosted bottom sheet providing typography and appearance customization for the lyrics view.
 */
@OptIn(ExperimentalHazeMaterialsApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun LyricsStyleSheet(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val customStyle by LyricsStyleManager.style.collectAsStateWithLifecycle()
    val fontPicker = rememberCustomFontPicker { name, path -> LyricsStyleManager.setCustomFont(name, path) }
    val haptics = rememberHaptics()
    var drag by remember { mutableFloatStateOf(0f) }
    var height by remember { mutableIntStateOf(0) }
    val drawerOffset by animateFloatAsState(
        targetValue = drag,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "lyricsStyleDrawerOffset",
    )
    val scrimAlpha = if (height > 0) {
        (1f - drawerOffset / height).coerceIn(0f, 1f)
    } else {
        1f
    }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val reduceDynamicBlur by PlayerSettings.reduceDynamicBlur.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .containSheetGestures()
            .background(ScrimColor.copy(alpha = ScrimColor.alpha * scrimAlpha))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = shown,
            enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it },
            exit = slideOutVertically(tween(180)) { it },
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = DrawerMaxWidth)
                    .fillMaxWidth()
                    .onSizeChanged { height = it.height }
                    .offset { IntOffset(0, drawerOffset.roundToInt()) }
                    .clip(DrawerShape)
                    .then(
                        if (reduceDynamicBlur) {
                            Modifier.background(Color(0xFF121212))
                        } else {
                            Modifier
                                .optimizedHazeEffect(
                                    state = hazeState,
                                    style = HazeMaterials.regular(Color(0xFF141414)),
                                )
                                .background(Color(0xFF121212).copy(alpha = 0.92f))
                        },
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    )
                    .pointerInput(height) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (height > 0 && drag > height * DismissDragFraction) {
                                    onDismiss()
                                } else {
                                    drag = 0f
                                }
                            },
                            onDragCancel = { drag = 0f },
                        ) { _, delta -> drag = (drag + delta).coerceAtLeast(0f) }
                    }
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp)
                    .padding(top = 10.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Top drag handle
                Box(
                    Modifier
                        .padding(bottom = 14.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                )

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Lyrics Font & Styling",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color.White,
                        )
                        Text(
                            text = "Customize typography, scaling, and alignment",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }

                    // Reset button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                haptics.play(Haptic.Tap)
                                LyricsStyleManager.reset()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reset",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Live Preview Card
                val previewFont = rememberLyricsFontFamily(customStyle)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Music feels better with you",
                        fontFamily = previewFont,
                        fontSize = (22.sp * customStyle.sizeScale.scale),
                        fontWeight = customStyle.weightChoice.weight,
                        textAlign = if (customStyle.centerAligned) TextAlign.Center else TextAlign.Start,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Section 1: Font Family
                StyleSectionHeader(title = "FONT FAMILY")
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LyricsFontChoice.values().filter { it != LyricsFontChoice.CUSTOM }.forEach { font ->
                        val selected = customStyle.fontChoice == font
                        val chipFamily = rememberLyricsFontFamily(customStyle.copy(fontChoice = font))
                        StyleChip(
                            label = font.label,
                            selected = selected,
                            fontFamily = chipFamily,
                            onClick = {
                                haptics.play(Haptic.Select)
                                LyricsStyleManager.updateFont(font)
                            },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Custom font picker button / status card
                val isCustomActive = customStyle.fontChoice == LyricsFontChoice.CUSTOM
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isCustomActive) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f)
                        )
                        .clickable {
                            if (customStyle.customFontPath != null) {
                                haptics.play(Haptic.Select)
                                LyricsStyleManager.updateFont(LyricsFontChoice.CUSTOM)
                            } else {
                                fontPicker()
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (isCustomActive) Color.White else Color.White.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (isCustomActive) Icons.Rounded.Check else Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = if (isCustomActive) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = if (customStyle.customFontName != null) {
                                        customStyle.customFontName!!
                                    } else {
                                        "Upload Custom Font"
                                    },
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isCustomActive) FontWeight.Bold else FontWeight.Medium,
                                    ),
                                    maxLines = 1,
                                )
                                Text(
                                    text = if (customStyle.customFontName != null) {
                                        "Custom font active • Tap to select"
                                    } else {
                                        "Import .ttf or .otf font from device"
                                    },
                                    color = Color.White.copy(alpha = 0.55f),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                )
                            }
                        }

                        // Browse / Replace button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.14f))
                            .clickable {
                                haptics.play(Haptic.Tap)
                                fontPicker()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (customStyle.customFontName != null) "Change" else "Browse",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                ),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Section 2: Text Size Scale
                StyleSectionHeader(title = "TEXT SIZE")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LyricsSizeScale.values().forEach { scale ->
                        val selected = customStyle.sizeScale == scale
                        Box(Modifier.weight(1f)) {
                            StyleChip(
                                label = scale.label,
                                selected = selected,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    haptics.play(Haptic.Select)
                                    LyricsStyleManager.updateScale(scale)
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Section 3: Font Weight & Alignment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Font Weight Column
                    Column(Modifier.weight(1f)) {
                        StyleSectionHeader(title = "WEIGHT")
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            LyricsWeightChoice.values().forEach { weight ->
                                val selected = customStyle.weightChoice == weight
                                Box(Modifier.weight(1f)) {
                                    StyleChip(
                                        label = weight.label,
                                        selected = selected,
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            haptics.play(Haptic.Select)
                                            LyricsStyleManager.updateWeight(weight)
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // Alignment Column
                    Column {
                        StyleSectionHeader(title = "ALIGNMENT")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Left align button
                            AlignmentIconPill(
                                icon = Icons.Rounded.FormatAlignLeft,
                                selected = !customStyle.centerAligned,
                                onClick = {
                                    haptics.play(Haptic.Select)
                                    LyricsStyleManager.updateCenterAligned(false)
                                },
                            )
                            // Center align button
                            AlignmentIconPill(
                                icon = Icons.Rounded.FormatAlignCenter,
                                selected = customStyle.centerAligned,
                                onClick = {
                                    haptics.play(Haptic.Select)
                                    LyricsStyleManager.updateCenterAligned(true)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        ),
        color = Color.White.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().padding(start = 2.dp),
    )
}

@Composable
private fun StyleChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    onClick: () -> Unit,
) {
    val bg = if (selected) Color.White else Color.White.copy(alpha = 0.08f)
    val textTint = if (selected) Color.Black else Color.White.copy(alpha = 0.9f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textTint,
            fontFamily = fontFamily,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.5.sp,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun AlignmentIconPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) Color.White else Color.White.copy(alpha = 0.08f)
    val tint = if (selected) Color.Black else Color.White.copy(alpha = 0.9f)
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}
