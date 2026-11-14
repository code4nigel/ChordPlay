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
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Tune
import kotlin.math.roundToInt
import java.util.Locale
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.*
import org.jetbrains.compose.resources.stringResource
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
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
    accentColor: Color? = null,
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
    val scrollState = rememberScrollState()
    val sliderInteractionSource = remember { MutableInteractionSource() }
    val isDraggingFontSlider by sliderInteractionSource.collectIsDraggedAsState()
    val sheetBgAlpha by animateFloatAsState(
        targetValue = if (isDraggingFontSlider) 0.04f else 1f,
        animationSpec = tween(140),
        label = "sheetBgAlpha",
    )
    val otherContentAlpha by animateFloatAsState(
        targetValue = if (isDraggingFontSlider) 0f else 1f,
        animationSpec = tween(140),
        label = "otherContentAlpha",
    )

    val dismissOnRelease: () -> Unit = {
        if (height > 0 && drag > height * DismissDragFraction) onDismiss() else drag = 0f
    }

    val drawerScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (drag <= 0f || available.y >= 0f) return Offset.Zero
                val used = available.y.coerceAtLeast(-drag)
                drag += used
                return Offset(0f, used)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    drag += available.y
                }
                if (drag > 0f && available.y < 0f) {
                    val used = available.y.coerceAtLeast(-drag)
                    drag += used
                    return Offset(0f, used)
                }
                return available
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (drag <= 0f) return Velocity.Zero
                dismissOnRelease()
                return available
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                dismissOnRelease()
                return available
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .containSheetGestures()
            .background(ScrimColor.copy(alpha = ScrimColor.alpha * scrimAlpha * sheetBgAlpha))
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
                        if (reduceDynamicBlur || isDraggingFontSlider) {
                            Modifier.background(
                                if (accentColor != null) {
                                    Brush.verticalGradient(
                                        listOf(
                                            accentColor.copy(alpha = 0.22f * sheetBgAlpha),
                                            Color(0xFF141416).copy(alpha = 0.94f * sheetBgAlpha),
                                        ),
                                    )
                                } else {
                                    SolidColor(Color(0xFF141416).copy(alpha = 0.94f * sheetBgAlpha))
                                },
                            )
                        } else {
                            Modifier
                                .optimizedHazeEffect(
                                    state = hazeState,
                                    style = HazeMaterials.ultraThin(Color(0xFF141416)),
                                )
                                .background(
                                    if (accentColor != null) {
                                        Brush.verticalGradient(
                                            listOf(
                                                accentColor.copy(alpha = 0.28f * sheetBgAlpha),
                                                Color(0xFF141418).copy(alpha = 0.65f * sheetBgAlpha),
                                                Color(0xFF0F0F12).copy(alpha = 0.80f * sheetBgAlpha),
                                            ),
                                        )
                                    } else {
                                        SolidColor(Color(0xFF121215).copy(alpha = 0.72f * sheetBgAlpha))
                                    },
                                )
                        },
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    )
                    .nestedScroll(drawerScroll)
                    .navigationBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 18.dp)
                    .padding(top = 10.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = otherContentAlpha },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Top drag handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(height) {
                                detectVerticalDragGestures(
                                    onDragEnd = dismissOnRelease,
                                    onDragCancel = { drag = 0f },
                                ) { _, delta ->
                                    drag = (drag + delta).coerceAtLeast(0f)
                                }
                            }
                            .padding(bottom = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                        )
                    }

                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.lyrics_options),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color.White,
                            )
                            Text(
                                text = stringResource(Res.string.lyrics_options_subtitle),
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
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF1E2130).copy(alpha = 0.65f),
                                            Color(0xFF12141C).copy(alpha = 0.50f),
                                        ),
                                    ),
                                )
                                .clickable {
                                    haptics.play(Haptic.Tap)
                                    LyricsStyleManager.reset()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(Res.string.lyrics_offset_reset),
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
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(16.dp),
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.music_feels_better),
                            fontFamily = previewFont,
                            fontSize = (22.sp * customStyle.effectiveScale),
                            fontWeight = customStyle.weightChoice.weight,
                            textAlign = if (customStyle.centerAligned) TextAlign.Center else TextAlign.Start,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // Section 1: Font Family
                    StyleSectionHeader(title = stringResource(Res.string.font_family).uppercase())
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LyricsFontChoice.entries.filter { it != LyricsFontChoice.CUSTOM }.forEach { font ->
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
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isCustomActive) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f),
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(16.dp),
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
                                            stringResource(Res.string.upload_custom_font)
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
                                            stringResource(Res.string.import_font_hint)
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
                                    text = if (customStyle.customFontName != null) stringResource(Res.string.change) else stringResource(Res.string.browse),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                    ),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Section 2: Text Size Scale with Live Slider, +/- steppers, and Reset
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            StyleSectionHeader(title = stringResource(Res.string.text_size).uppercase())
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    text = "${(customStyle.effectiveScale * 100).roundToInt()}%",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }

                        // Reset button
                        val isDefaultScale = kotlin.math.abs(customStyle.effectiveScale - 1.0f) < 0.02f
                        TextButton(
                            onClick = {
                                haptics.play(Haptic.Tap)
                                LyricsStyleManager.updateFontScale(1.0f)
                            },
                            enabled = !isDefaultScale,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Reset size",
                                modifier = Modifier.size(15.dp),
                                tint = if (!isDefaultScale) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (!isDefaultScale) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1B1B1F).copy(alpha = if (isDraggingFontSlider) 0.92f else 0.45f))
                            .border(
                                width = 1.dp,
                                color = if (isDraggingFontSlider) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            // Minus button
                            IconButton(
                                onClick = {
                                    haptics.play(Haptic.Tap)
                                    val newScale = ((customStyle.effectiveScale - 0.05f) * 100).roundToInt() / 100f
                                    LyricsStyleManager.updateFontScale(newScale.coerceIn(0.55f, 2.20f))
                                },
                                modifier = Modifier.size(36.dp),
                                enabled = customStyle.effectiveScale > 0.55f,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = "Decrease text size",
                                    tint = if (customStyle.effectiveScale > 0.55f) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            // Slider
                            Slider(
                                value = customStyle.effectiveScale,
                                onValueChange = { newScale ->
                                    LyricsStyleManager.updateFontScale(newScale)
                                },
                                valueRange = 0.55f..2.20f,
                                interactionSource = sliderInteractionSource,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.16f),
                                ),
                                modifier = Modifier.weight(1f),
                            )

                            // Plus button
                            IconButton(
                                onClick = {
                                    haptics.play(Haptic.Tap)
                                    val newScale = ((customStyle.effectiveScale + 0.05f) * 100).roundToInt() / 100f
                                    LyricsStyleManager.updateFontScale(newScale.coerceIn(0.55f, 2.20f))
                                },
                                modifier = Modifier.size(36.dp),
                                enabled = customStyle.effectiveScale < 2.20f,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Increase text size",
                                    tint = if (customStyle.effectiveScale < 2.20f) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = otherContentAlpha },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(16.dp))

                    // Section 3: Font Weight
                    StyleSectionHeader(title = stringResource(Res.string.font_weight).uppercase())
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LyricsWeightChoice.entries.forEach { weightChoice ->
                            val selected = customStyle.weightChoice == weightChoice
                            Box(Modifier.weight(1f)) {
                                StyleChip(
                                    label = weightChoice.label,
                                    selected = selected,
                                    fontWeight = weightChoice.weight,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        haptics.play(Haptic.Select)
                                        LyricsStyleManager.updateWeight(weightChoice)
                                    },
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Section 4: Text Alignment
                    StyleSectionHeader(title = stringResource(Res.string.text_alignment).uppercase())
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Left align option pill
                        Box(Modifier.weight(1f)) {
                            AlignmentOptionPill(
                                icon = Icons.AutoMirrored.Rounded.FormatAlignLeft,
                                label = stringResource(Res.string.align_left),
                                selected = !customStyle.centerAligned,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    haptics.play(Haptic.Select)
                                    LyricsStyleManager.updateCenterAligned(false)
                                },
                            )
                        }

                        // Center align option pill
                        Box(Modifier.weight(1f)) {
                            AlignmentOptionPill(
                                icon = Icons.Rounded.FormatAlignCenter,
                                label = stringResource(Res.string.align_center),
                                selected = customStyle.centerAligned,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    haptics.play(Haptic.Select)
                                    LyricsStyleManager.updateCenterAligned(true)
                                },
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Section 5: Highlight Focus Position
                    StyleSectionHeader(title = "HIGHLIGHT POSITION")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        LyricsHighlightPosition.entries.forEach { pos ->
                            val selected = customStyle.highlightPosition == pos
                            Box(Modifier.weight(1f)) {
                                StyleChip(
                                    label = pos.label,
                                    selected = selected,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        haptics.play(Haptic.Select)
                                        LyricsStyleManager.updateHighlightPosition(pos)
                                    },
                                )
                            }
                        }
                    }

                Spacer(Modifier.height(18.dp))

                // Section: Lyrics Sync Offset
                StyleSectionHeader(title = stringResource(Res.string.sync_offset).uppercase())
                Spacer(Modifier.height(8.dp))
                val offsetMs by PlayerSettings.lyricsOffsetMs.collectAsStateWithLifecycle()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = "Sync Offset",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = "Sync Offset",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%+.1f s", offsetMs / 1_000f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF1E2130).copy(alpha = 0.65f),
                                                Color(0xFF12141C).copy(alpha = 0.50f),
                                            ),
                                        ),
                                    )
                                    .clickable(enabled = offsetMs > MIN_LYRICS_OFFSET_MS) {
                                        haptics.play(Haptic.Select)
                                        PlayerSettings.setLyricsOffsetMs(offsetMs - 100)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("−", color = Color.White.copy(alpha = if (offsetMs > MIN_LYRICS_OFFSET_MS) 0.9f else 0.3f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            ThinSlider(
                                value = (offsetMs - MIN_LYRICS_OFFSET_MS).toFloat() / (MAX_LYRICS_OFFSET_MS - MIN_LYRICS_OFFSET_MS),
                                onValueChange = {
                                    val raw = MIN_LYRICS_OFFSET_MS + it.coerceIn(0f, 1f) * (MAX_LYRICS_OFFSET_MS - MIN_LYRICS_OFFSET_MS)
                                    PlayerSettings.setLyricsOffsetMs((raw / 100).roundToInt() * 100)
                                },
                                idleHeight = 6.dp,
                                activeHeight = 10.dp,
                                modifier = Modifier.weight(1f),
                            )
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF1E2130).copy(alpha = 0.65f),
                                                Color(0xFF12141C).copy(alpha = 0.50f),
                                            ),
                                        ),
                                    )
                                    .clickable(enabled = offsetMs < MAX_LYRICS_OFFSET_MS) {
                                        haptics.play(Haptic.Select)
                                        PlayerSettings.setLyricsOffsetMs(offsetMs + 100)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("+", color = Color.White.copy(alpha = if (offsetMs < MAX_LYRICS_OFFSET_MS) 0.9f else 0.3f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (offsetMs != 0) {
                            Text(
                                text = "Reset offset to 0.0 s",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        haptics.play(Haptic.Tap)
                                        PlayerSettings.setLyricsOffsetMs(0)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                } // End inner column with otherContentAlpha
            }
        }
    }
}

@Composable
private fun StyleSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        ),
        color = Color.White.copy(alpha = 0.5f),
        modifier = modifier.padding(start = 2.dp),
    )
}

@Composable
private fun StyleChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
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
            .padding(horizontal = 6.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textTint,
            fontFamily = fontFamily,
            softWrap = false,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = fontWeight ?: if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.5.sp,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun AlignmentOptionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg = if (selected) Color.White else Color.White.copy(alpha = 0.08f)
    val tint = if (selected) Color.Black else Color.White.copy(alpha = 0.9f)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = tint,
            softWrap = false,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.5.sp,
            ),
            maxLines = 1,
        )
    }
}
