package com.music.bitchord.ui.screens.settings.sections

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.CoverAnimationMode
import com.music.bitchord.ui.player.CoverChange
import com.music.bitchord.ui.player.CoverMotionLayers
import com.music.bitchord.ui.player.fullBleedArtworkAvailable
import com.music.bitchord.ui.screens.Chevron
import com.music.bitchord.ui.screens.RowDivider
import com.music.bitchord.ui.screens.SettingsGroup
import com.music.bitchord.ui.screens.SettingsRow
import com.music.bitchord.ui.screens.SettingsSearchEmpty
import com.music.bitchord.ui.screens.SettingsSubPageHeader
import com.music.bitchord.ui.screens.SettingsSubRow
import com.music.bitchord.ui.screens.matchesSearch

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    var query by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }
    val showMotion = matchesSearch(
        query, stringResource(R.string.cover_animation),
        stringResource(R.string.cover_animation_fade), stringResource(R.string.cover_animation_3d), "motion",
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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding)) {
        SettingsSubPageHeader(
            title = stringResource(R.string.settings_cover),
            onBack = onBack,
            searchQuery = query,
            onSearchQueryChange = { query = it },
        )
        if (!showMotion && !showFullScreen && !showTap && !showCanvas) SettingsSearchEmpty(query)
        if (showMotion) SettingsGroup(header = stringResource(R.string.cover_animation)) {
            CoverPreview(mode, reducedMotion)
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CoverAnimationMode.entries.forEach { option ->
                    val label = when (option) {
                        CoverAnimationMode.NONE -> R.string.cover_animation_none
                        CoverAnimationMode.FADE -> R.string.cover_animation_fade
                        CoverAnimationMode.THREE_D_FADE -> R.string.cover_animation_3d
                    }
                    FilterChip(
                        selected = mode == option,
                        onClick = { AppSettings.setCoverAnimation(option) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
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
        Spacer(Modifier.height(20.dp))
    }
}

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
            progress.animateTo(1f, change.animationSpec)
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF181420))) {
            CoverMotionLayers(previewMode, change.direction, { progress.value }) { entering, modifier ->
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
        Column {
            Text(stringResource(R.string.preview), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = {
                change = CoverChange.NEXT
                replay++
            }) {
                Text(stringResource(R.string.cover_preview_manual))
            }
            TextButton(onClick = {
                change = CoverChange.AUTOMATIC
                replay++
            }) {
                Text(stringResource(R.string.cover_preview_automatic))
            }
        }
    }
}
