package com.music.bitchord.ui.screens.settings.sections

import android.os.Build

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOff
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppDesignStyle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.ChordPlayPalette
import com.music.bitchord.data.settings.ThemeMode
import com.music.bitchord.ui.components.isGlassSupported
import com.music.bitchord.ui.components.languageDisplayNameRes
import com.music.bitchord.ui.performance.resolvePerformanceRefreshRate
import com.music.bitchord.ui.performance.supportedPerformanceRefreshRates
import com.music.bitchord.ui.screens.*
import com.music.bitchord.ui.screens.settings.previews.AppearanceLivePreview
import java.util.Locale

/**
 * Appearance & Glass customization sub-page with a pinned live interactive preview card.
 */
@Composable
fun AppearanceSection(
    onBack: () -> Unit,
    onAppLanguage: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val theme by AppSettings.themeMode.collectAsStateWithLifecycle()
    val designStyle by AppSettings.appDesignStyle.collectAsStateWithLifecycle()
    val colorPalette by AppSettings.colorPalette.collectAsStateWithLifecycle()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val liquidGlassSupported = isGlassSupported()

    val highPerformanceMode by AppSettings.highPerformanceMode.collectAsStateWithLifecycle()
    val performanceRefreshRate by AppSettings.performanceRefreshRate.collectAsStateWithLifecycle()
    val currentDisplay = LocalView.current.display
    val supportedRefreshRates = remember(currentDisplay) {
        currentDisplay.supportedPerformanceRefreshRates()
    }
    val selectedPerformanceRefreshRate = remember(currentDisplay, performanceRefreshRate) {
        currentDisplay.resolvePerformanceRefreshRate(performanceRefreshRate)
    }

    LaunchedEffect(selectedPerformanceRefreshRate, performanceRefreshRate) {
        if (selectedPerformanceRefreshRate != performanceRefreshRate) {
            AppSettings.setPerformanceRefreshRate(selectedPerformanceRefreshRate)
        }
    }

    var showPerformanceWarning by remember { mutableStateOf(false) }
    var showPerformanceConfirmation by remember { mutableStateOf(false) }

    val batterySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        showPerformanceConfirmation = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.appearance),
            onBack = onBack,
        )

        // Pinned Live Preview
        AppearanceLivePreview()

        // 1. Design System & Style Switcher Group
        SettingsGroup(header = stringResource(R.string.design_style)) {
            val designStyleTitle = stringResource(R.string.design_style)
            SettingsRow(
                icon = Icons.Rounded.Style,
                title = designStyleTitle,
                subtitle = stringResource(R.string.design_style_subtitle),
            )
            SegmentedControl(
                options = AppDesignStyle.entries.map { it.label },
                selectedIndex = AppDesignStyle.entries.indexOf(designStyle),
                onSelect = { AppSettings.setAppDesignStyle(AppDesignStyle.entries[it]) },
                modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 12.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp),
            ) {
                Text(
                    text = designStyle.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 2. Curated Atmospheric Color Palettes (When ChordPlay Modern is active)
        if (designStyle == AppDesignStyle.CHORDPLAY) {
            SettingsGroup(header = stringResource(R.string.color_palette)) {
                SettingsRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.color_palette),
                    subtitle = stringResource(R.string.color_palette_subtitle),
                )
                PaletteCarousel(
                    selectedPalette = colorPalette,
                    onSelectPalette = { AppSettings.setColorPalette(it) },
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            }
        }

        // 3. Theme Mode (Dark, Light, System)
        SettingsGroup(header = stringResource(R.string.theme)) {
            val themeTitle = stringResource(R.string.theme)
            SettingsRow(icon = Icons.Rounded.Brightness4, title = themeTitle)
            SegmentedControl(
                options = ThemeMode.entries.map { it.localizedLabel() },
                selectedIndex = ThemeMode.entries.indexOf(theme),
                onSelect = { AppSettings.setThemeMode(ThemeMode.entries[it]) },
                modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
            )
        }

        // 4. Glass & Animation Effects Group
        SettingsGroup(header = stringResource(R.string.liquid_glass)) {
            val liquidGlassTitle = stringResource(R.string.liquid_glass)
            SettingsRow(
                icon = Icons.Rounded.AutoAwesome,
                title = liquidGlassTitle,
                subtitle = stringResource(
                    if (liquidGlassSupported) R.string.liquid_glass_subtitle
                    else R.string.liquid_glass_unavailable,
                ),
                enabled = liquidGlassSupported,
                trailing = {
                    Switch(
                        checked = liquidGlass,
                        onCheckedChange = AppSettings::setLiquidGlass,
                        enabled = liquidGlassSupported,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setLiquidGlass(!liquidGlass) },
            )

            val reduceDynamicBlurTitle = stringResource(R.string.reduce_dynamic_blur)
            SettingsRow(
                icon = Icons.Rounded.BlurOff,
                title = reduceDynamicBlurTitle,
                subtitle = stringResource(R.string.reduce_dynamic_blur_subtitle),
                trailing = {
                    Switch(
                        checked = reduceDynamicBlur,
                        onCheckedChange = AppSettings::setReduceDynamicBlur,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setReduceDynamicBlur(!reduceDynamicBlur) },
            )

            val reduceAnimationTitle = stringResource(R.string.reduce_animation)
            SettingsRow(
                icon = Icons.Rounded.MotionPhotosOff,
                title = reduceAnimationTitle,
                subtitle = stringResource(R.string.reduce_animation_subtitle),
                trailing = {
                    Switch(
                        checked = reduceAnimation,
                        onCheckedChange = AppSettings::setReduceAnimation,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setReduceAnimation(!reduceAnimation) },
            )
        }

        // 5. Performance & Refresh Rates Group
        SettingsGroup(header = stringResource(R.string.performance)) {
            val highPerformanceModeTitle = stringResource(R.string.high_performance_mode)
            SettingsRow(
                icon = Icons.Rounded.Speed,
                title = highPerformanceModeTitle,
                subtitle = if (highPerformanceMode) {
                    stringResource(R.string.high_performance_active, selectedPerformanceRefreshRate ?: 120)
                } else {
                    stringResource(R.string.high_performance_subtitle)
                },
                trailing = {
                    Switch(
                        checked = highPerformanceMode,
                        onCheckedChange = { enable ->
                            if (enable) showPerformanceWarning = true
                            else AppSettings.setHighPerformanceMode(false)
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = {
                    if (!highPerformanceMode) showPerformanceWarning = true
                    else AppSettings.setHighPerformanceMode(false)
                },
            )

            if (highPerformanceMode && supportedRefreshRates.isNotEmpty()) {
                val refreshRateTitle = stringResource(R.string.refresh_rate)
                val labels = supportedRefreshRates.map { rate ->
                    if (rate == null) stringResource(R.string.system)
                    else "${rate.toInt()} Hz"
                }
                val selectedIndex = supportedRefreshRates.indexOf(selectedPerformanceRefreshRate).coerceAtLeast(0)
                SettingsRow(
                    icon = Icons.Rounded.Speed,
                    title = refreshRateTitle,
                )
                SegmentedControl(
                    options = labels,
                    selectedIndex = selectedIndex,
                    onSelect = { AppSettings.setPerformanceRefreshRate(supportedRefreshRates[it]) },
                    modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
                )
            }
        }

        // 6. App Language Group
        SettingsGroup(header = stringResource(R.string.language)) {
            val appLanguageTitle = stringResource(R.string.app_language)
            val selectedLanguage = AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag()
                ?: Locale.getDefault().toLanguageTag()
            SettingsRow(
                icon = Icons.Rounded.Language,
                title = appLanguageTitle,
                subtitle = stringResource(languageDisplayNameRes(selectedLanguage)),
                onClick = onAppLanguage,
            )
        }
    }

    if (showPerformanceWarning) {
        AlertDialog(
            onDismissRequest = { showPerformanceWarning = false },
            title = { Text(stringResource(R.string.high_performance_before_enabling)) },
            text = { Text(stringResource(R.string.high_performance_battery_warning)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPerformanceWarning = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        val settingsIntent = intent.takeIf {
                            it.resolveActivity(context.packageManager) != null
                        } ?: Intent(Settings.ACTION_SETTINGS)
                        batterySettingsLauncher.launch(settingsIntent)
                    },
                ) {
                    Text(stringResource(R.string.open_battery_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPerformanceWarning = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showPerformanceConfirmation) {
        AlertDialog(
            onDismissRequest = { showPerformanceConfirmation = false },
            title = { Text(stringResource(R.string.enable_high_performance_title)) },
            text = { Text(stringResource(R.string.enable_high_performance_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPerformanceConfirmation = false
                        AppSettings.setHighPerformanceMode(true)
                    },
                ) {
                    Text(stringResource(R.string.enable))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPerformanceConfirmation = false }) {
                    Text(stringResource(R.string.not_yet))
                }
            },
        )
    }
}

/**
 * Curated color palette carousel with rich live checkmarks and atmospheric swatches.
 */
@Composable
private fun PaletteCarousel(
    selectedPalette: ChordPlayPalette,
    onSelectPalette: (ChordPlayPalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = ROW_INSET),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(ChordPlayPalette.entries) { item ->
            val isSelected = item == selectedPalette
            val primaryColor = if (item == ChordPlayPalette.DYNAMIC_MONET && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MaterialTheme.colorScheme.primary
            } else {
                Color(item.primaryColorHex)
            }
            val secondaryColor = if (item == ChordPlayPalette.DYNAMIC_MONET && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MaterialTheme.colorScheme.secondary
            } else {
                Color(item.secondaryColorHex)
            }

            Column(
                modifier = Modifier
                    .width(112.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) primaryColor.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectPalette(item)
                    }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Two-color orb swatch
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(primaryColor, secondaryColor, primaryColor),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.60f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
