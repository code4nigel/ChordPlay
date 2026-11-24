package com.music.bitchord.ui.screens.settings.sections

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.CanvasProvider
import com.music.bitchord.data.settings.CoverAnimationMode
import com.music.bitchord.ui.player.CoverChange
import com.music.bitchord.ui.player.CoverMotionLayers
import com.music.bitchord.ui.player.fullBleedArtworkAvailable
import com.music.bitchord.ui.screens.Chevron
import com.music.bitchord.ui.screens.RowDivider
import com.music.bitchord.ui.screens.SegmentedControl
import com.music.bitchord.ui.screens.SettingsGroup
import com.music.bitchord.ui.screens.SettingsRow
import com.music.bitchord.ui.screens.SettingsSearchEmpty
import com.music.bitchord.ui.screens.SettingsSubPageHeader
import com.music.bitchord.ui.screens.SettingsSubRow
import com.music.bitchord.ui.screens.matchesSearch

@Composable
internal fun CoverSection(
    windowWidth: Dp,
    onSpotifyCanvasAuth: () -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    initialSearchQuery: String,
) {
    val mode by AppSettings.coverAnimation.collectAsStateWithLifecycle()
    val reducedMotion by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val fullBleed by AppSettings.fullBleedArtwork.collectAsStateWithLifecycle()
    val tapToToggle by AppSettings.tapArtworkToToggleFullScreen.collectAsStateWithLifecycle()
    val canvas by AppSettings.animatedCanvas.collectAsStateWithLifecycle()
    val canvasOnCellular by AppSettings.canvasOverCellular.collectAsStateWithLifecycle()
    val canvasOrder by AppSettings.canvasProviderOrder.collectAsStateWithLifecycle()
    val spotifyToken by AppSettings.spotifySpdcToken.collectAsStateWithLifecycle()
    var draggingPriority by remember { mutableStateOf(false) }
    var query by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }
    val showMotion = matchesSearch(
        query, stringResource(R.string.cover_animation),
        stringResource(R.string.cover_animation_none), stringResource(R.string.cover_animation_sweep),
        stringResource(R.string.cover_animation_disintegrate), "sweep", "disintegrate", "thanos", "3d", "motion", "fade",
    )
    val canFullScreen = fullBleedArtworkAvailable(windowWidth)
    val showFullScreen = canFullScreen && matchesSearch(
        query, stringResource(R.string.full_screen_cover_art), "full screen", "full bleed",
    )
    val showTap = canFullScreen && matchesSearch(
        query, stringResource(R.string.tap_artwork_toggle_fullscreen), "full screen", "tap artwork",
    )
    val showCanvas = matchesSearch(
        query, stringResource(R.string.animated_cover_art), stringResource(R.string.animated_cover_cellular),
        stringResource(R.string.integrate_spotify_canvas), "canvas", "spotify",
    )
    val showPriority = matchesSearch(
        query,
        stringResource(R.string.cover_priority),
        stringResource(R.string.cover_priority_subtitle),
        stringResource(R.string.cover_provider_spotify),
        stringResource(R.string.cover_provider_apple),
        stringResource(R.string.cover_provider_tidal),
        stringResource(R.string.cover_provider_community),
        "priority",
        "order",
        "spotify",
        "apple",
        "tidal",
        "community",
        "drag",
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), enabled = !draggingPriority)
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.settings_cover),
            onBack = onBack,
            searchQuery = query,
            onSearchQueryChange = { query = it },
        )
        if (!showMotion && !showFullScreen && !showTap && !showCanvas && !(canvas && showPriority)) {
            SettingsSearchEmpty(query)
        }
        if (showMotion) SettingsGroup(header = stringResource(R.string.cover_animation)) {
            CoverPreview(mode, reducedMotion)
            val modes = CoverAnimationMode.entries
            SegmentedControl(
                options = modes.map { option ->
                    stringResource(
                        when (option) {
                            CoverAnimationMode.NONE -> R.string.cover_animation_none
                            CoverAnimationMode.DIRECTIONAL_SWEEP -> R.string.cover_animation_sweep
                            CoverAnimationMode.DISINTEGRATE -> R.string.cover_animation_disintegrate
                        },
                    )
                },
                selectedIndex = modes.indexOf(mode).coerceAtLeast(0),
                onSelect = { index -> AppSettings.setCoverAnimation(modes[index]) },
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            )
        }
        if (showFullScreen || showTap || showCanvas) SettingsGroup {
            if (showFullScreen) {
                SettingsRow(
                    icon = Icons.Rounded.Fullscreen,
                    title = stringResource(R.string.full_screen_cover_art),
                    subtitle = stringResource(R.string.full_screen_cover_art_subtitle),
                    onClick = { AppSettings.setFullBleedArtwork(!fullBleed) },
                    trailing = { Switch(fullBleed, AppSettings::setFullBleedArtwork) },
                )
            }
            if (showTap) {
                if (showFullScreen) RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.TouchApp,
                    title = stringResource(R.string.tap_artwork_toggle_fullscreen),
                    subtitle = stringResource(R.string.tap_artwork_toggle_fullscreen_subtitle),
                    onClick = { AppSettings.setTapArtworkToToggleFullScreen(!tapToToggle) },
                    trailing = { Switch(tapToToggle, AppSettings::setTapArtworkToToggleFullScreen) },
                )
            }
            if (showCanvas) {
                if (showFullScreen || showTap) RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.Animation,
                    title = stringResource(R.string.animated_cover_art),
                    subtitle = stringResource(R.string.animated_cover_art_subtitle),
                    onClick = { AppSettings.setAnimatedCanvas(!canvas) },
                    trailing = { Switch(canvas, AppSettings::setAnimatedCanvas) },
                )
                if (canvas) {
                    SettingsSubRow(
                        title = stringResource(R.string.animated_cover_cellular),
                        checked = canvasOnCellular,
                        onCheckedChange = AppSettings::setCanvasOverCellular,
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Link,
                        title = stringResource(R.string.integrate_spotify_canvas),
                        onClick = onSpotifyCanvasAuth,
                        trailing = { Chevron() },
                    )
                }
            }
        }
        if (canvas && showPriority) {
            SettingsGroup(header = stringResource(R.string.cover_priority)) {
                CoverPriorityHeader(
                    canReset = canvasOrder != CanvasProvider.defaultOrder,
                    onReset = AppSettings::resetCanvasProviderOrder,
                )
                CoverPriorityList(
                    order = canvasOrder,
                    spotifyConfigured = spotifyToken.isNotBlank(),
                    onReorder = AppSettings::setCanvasProviderOrder,
                    onDraggingChange = { draggingPriority = it },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

private const val PRIORITY_SWAP_THRESHOLD = 0.55f

@Composable
private fun CoverPriorityHeader(
    canReset: Boolean,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.cover_priority_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
        )
        if (canReset) {
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onReset)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.cover_priority_reset),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun CoverPriorityList(
    order: List<CanvasProvider>,
    spotifyConfigured: Boolean,
    onReorder: (List<CanvasProvider>) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var liveOrder by remember { mutableStateOf(order) }
    var draggedProvider by remember { mutableStateOf<CanvasProvider?>(null) }
    var totalDrag by remember { mutableFloatStateOf(0f) }
    var startIndex by remember { mutableIntStateOf(0) }
    var pitchPx by remember { mutableFloatStateOf(0f) }
    var lockedPitchPx by remember { mutableFloatStateOf(0f) }

    val currentOnReorder by rememberUpdatedState(onReorder)
    val currentOnDraggingChange by rememberUpdatedState(onDraggingChange)

    LaunchedEffect(order, draggedProvider) {
        if (draggedProvider == null) {
            liveOrder = order
        }
    }

    fun advanceDrag(delta: Float) {
        val provider = draggedProvider ?: return
        val pitch = lockedPitchPx
        if (pitch <= 0f) return
        var index = liveOrder.indexOf(provider)
        if (index < 0) return

        totalDrag = (totalDrag + delta).coerceIn(
            -startIndex * pitch,
            (liveOrder.lastIndex - startIndex) * pitch,
        )

        while (true) {
            val travelled = totalDrag / pitch
            val moved = (index - startIndex).toFloat()
            if (travelled > moved + PRIORITY_SWAP_THRESHOLD && index < liveOrder.lastIndex) {
                liveOrder = liveOrder.toMutableList().apply { add(index + 1, removeAt(index)) }
                index++
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } else if (travelled < moved - PRIORITY_SWAP_THRESHOLD && index > 0) {
                liveOrder = liveOrder.toMutableList().apply { add(index - 1, removeAt(index)) }
                index--
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } else {
                break
            }
        }
    }

    fun finishDrag() {
        val committed = liveOrder
        draggedProvider = null
        totalDrag = 0f
        currentOnDraggingChange(false)
        if (committed != order) {
            currentOnReorder(committed)
        }
    }

    Column(Modifier.fillMaxWidth()) {
        liveOrder.forEach { provider ->
            key(provider) {
                val dragging = provider == draggedProvider
                val rank = liveOrder.indexOf(provider) + 1
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (dragging) 1f else 0f)
                        .onSizeChanged { if (it.height > 0) pitchPx = it.height.toFloat() }
                        .graphicsLayer {
                            translationY = if (dragging) {
                                totalDrag - (liveOrder.indexOf(provider) - startIndex) * lockedPitchPx
                            } else {
                                0f
                            }
                            if (dragging) {
                                scaleX = 1.015f
                                scaleY = 1.015f
                            }
                        }
                        .then(
                            if (dragging) {
                                Modifier.background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                    RoundedCornerShape(12.dp),
                                )
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    RowDivider()
                    CoverPriorityRow(
                        rank = rank,
                        provider = provider,
                        spotifyConfigured = spotifyConfigured,
                        dragging = dragging,
                        onDragStart = {
                            draggedProvider = provider
                            totalDrag = 0f
                            startIndex = liveOrder.indexOf(provider).coerceAtLeast(0)
                            lockedPitchPx = pitchPx
                            currentOnDraggingChange(true)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragDelta = ::advanceDrag,
                        onDragFinish = ::finishDrag,
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverPriorityRow(
    rank: Int,
    provider: CanvasProvider,
    spotifyConfigured: Boolean,
    dragging: Boolean,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragFinish: () -> Unit,
) {
    val titleRes = when (provider) {
        CanvasProvider.SPOTIFY -> R.string.cover_provider_spotify
        CanvasProvider.APPLE_MUSIC -> R.string.cover_provider_apple
        CanvasProvider.TIDAL -> R.string.cover_provider_tidal
        CanvasProvider.COMMUNITY -> R.string.cover_provider_community
    }
    val subtitleRes = when (provider) {
        CanvasProvider.SPOTIFY -> if (spotifyConfigured) {
            R.string.cover_provider_spotify_subtitle
        } else {
            R.string.cover_provider_spotify_setup_hint
        }
        CanvasProvider.APPLE_MUSIC -> R.string.cover_provider_apple_subtitle
        CanvasProvider.TIDAL -> R.string.cover_provider_tidal_subtitle
        CanvasProvider.COMMUNITY -> R.string.cover_provider_community_subtitle
    }
    val subtitleHighlight = provider == CanvasProvider.SPOTIFY && !spotifyConfigured
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragDelta by rememberUpdatedState(onDragDelta)
    val currentOnDragFinish by rememberUpdatedState(onDragFinish)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    if (rank == 1 || dragging) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (rank == 1 || dragging) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(subtitleRes),
                style = MaterialTheme.typography.bodySmall,
                color = if (subtitleHighlight) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { currentOnDragStart() },
                        onDrag = { change, delta ->
                            change.consume()
                            currentOnDragDelta(delta.y)
                        },
                        onDragEnd = { currentOnDragFinish() },
                        onDragCancel = { currentOnDragFinish() },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = stringResource(R.string.drag_to_reorder),
                tint = if (dragging) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                },
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CoverPreview(mode: CoverAnimationMode, reducedMotion: Boolean) {
    val progress = remember { Animatable(1f) }
    var replay by remember { mutableIntStateOf(0) }
    var change by remember { mutableStateOf(CoverChange.NEXT) }
    val previewMode = if (reducedMotion) CoverAnimationMode.NONE else mode
    LaunchedEffect(previewMode, replay) {
        if (previewMode == CoverAnimationMode.NONE) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(1f, change.animationSpecFor(previewMode))
        }
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val outgoingRose = replay % 2 != 0
        val outgoingColors = if (outgoingRose) {
            listOf(Color(0xFFD685AE), Color(0xFF9F4B7E), Color(0xFF432040))
        } else {
            listOf(Color(0xFF76CBD6), Color(0xFF3E8FA3), Color(0xFF153B50))
        }
        Box(
            Modifier
                .size(184.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF181420))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
        ) {
            CoverMotionLayers(
                mode = previewMode,
                direction = change.direction,
                progress = { progress.value },
                outgoingColors = outgoingColors,
                seed = replay,
            ) { entering, modifier ->
                val rose = entering == (replay % 2 == 0)
                Canvas(modifier) {
                    val accent = if (rose) Color(0xFFD685AE) else Color(0xFF76CBD6)
                    val base = if (rose) Color(0xFF432040) else Color(0xFF153B50)
                    drawRect(Brush.verticalGradient(listOf(base, Color(0xFF181420))))
                    val center = Offset(size.width * (if (rose) 0.68f else 0.32f), size.height * 0.44f)
                    drawCircle(accent.copy(alpha = 0.24f), size.minDimension * 0.42f, center)
                    drawCircle(accent, size.minDimension * 0.24f, center)
                    drawCircle(base, size.minDimension * 0.095f, center)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CoverPreviewButton(
                icon = Icons.Rounded.SkipPrevious,
                label = stringResource(R.string.cover_preview_previous),
                onClick = {
                    change = CoverChange.PREVIOUS
                    replay++
                },
            )
            CoverPreviewButton(
                icon = Icons.Rounded.SkipNext,
                label = stringResource(R.string.cover_preview_next),
                onClick = {
                    change = CoverChange.NEXT
                    replay++
                },
            )
            CoverPreviewButton(
                icon = Icons.Rounded.PlayArrow,
                label = stringResource(R.string.cover_preview_auto),
                onClick = {
                    change = CoverChange.AUTOMATIC
                    replay++
                },
            )
        }
        if (reducedMotion) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.reduce_animation),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CoverPreviewButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
