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
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.SwipeLeft
import androidx.compose.material.icons.rounded.SwipeRight
import com.music.bitchord.data.settings.SongSwipeAction
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import com.music.bitchord.data.settings.CustomPalette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
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
import com.music.bitchord.data.settings.AppFontChoice
import com.music.bitchord.data.settings.ThemeMode
import com.music.bitchord.ui.components.isGlassSupported
import com.music.bitchord.ui.components.languageDisplayNameRes
import com.music.bitchord.ui.performance.displayDensityLabel
import com.music.bitchord.ui.performance.resolvePerformanceRefreshRate
import com.music.bitchord.ui.performance.supportedPerformanceRefreshRates
import com.music.bitchord.ui.screens.*
import com.music.bitchord.ui.screens.settings.localizedLabel
import com.music.bitchord.ui.screens.settings.localizedDetail
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
    val haptics = LocalHapticFeedback.current

    val theme by AppSettings.themeMode.collectAsStateWithLifecycle()
    val amoledDarkMode by AppSettings.amoledDarkMode.collectAsStateWithLifecycle()
    val appFontChoice by AppSettings.appFontChoice.collectAsStateWithLifecycle()
    val designStyle by AppSettings.appDesignStyle.collectAsStateWithLifecycle()
    val colorPalette by AppSettings.colorPalette.collectAsStateWithLifecycle()
    val customPalettes by AppSettings.customPalettes.collectAsStateWithLifecycle()
    val activeCustomPaletteId by AppSettings.activeCustomPaletteId.collectAsStateWithLifecycle()
    val playerInterfaceStyle by AppSettings.playerInterfaceStyle.collectAsStateWithLifecycle()
    val displayDensityScale by AppSettings.displayDensityScale.collectAsStateWithLifecycle()

    var showRenameDialog by remember { mutableStateOf(false) }
    var paletteToRename by remember { mutableStateOf<CustomPalette?>(null) }
    var renameInput by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var paletteToDelete by remember { mutableStateOf<CustomPalette?>(null) }
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val liquidGlassSupported = isGlassSupported()

    val songSwipeRightAction by AppSettings.songSwipeRightAction.collectAsStateWithLifecycle()
    val songSwipeLeftAction by AppSettings.songSwipeLeftAction.collectAsStateWithLifecycle()
    var showRightSwipePicker by remember { mutableStateOf(false) }
    var showLeftSwipePicker by remember { mutableStateOf(false) }

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

    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.appearance),
            onBack = onBack,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
        )

        // Pinned Live Preview
        AppearanceLivePreview()

        // Single Unified Big Block for all Appearance Settings matching Image 1
        SettingsGroup(header = stringResource(R.string.appearance)) {
            val designStyleTitle = stringResource(R.string.design_style)
            SettingsRow(
                icon = Icons.Rounded.Style,
                title = designStyleTitle,
                subtitle = stringResource(R.string.design_style_subtitle),
            )
            SegmentedControl(
                options = AppDesignStyle.entries.map { it.localizedLabel() },
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
                    text = designStyle.localizedDetail(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Player Interface Switcher (WavePlay vs ChordPlay OG)
            SettingsSectionDivider(title = stringResource(R.string.player_interface))

            SettingsRow(
                icon = Icons.Rounded.GraphicEq,
                title = stringResource(R.string.player_interface),
                subtitle = stringResource(R.string.player_interface_desc),
            )
            SegmentedControl(
                options = com.music.bitchord.data.settings.PlayerInterfaceStyle.entries.map { it.localizedLabel() },
                selectedIndex = com.music.bitchord.data.settings.PlayerInterfaceStyle.entries.indexOf(playerInterfaceStyle),
                onSelect = { AppSettings.setPlayerInterfaceStyle(com.music.bitchord.data.settings.PlayerInterfaceStyle.entries[it]) },
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
                    text = playerInterfaceStyle.localizedDetail(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Curated Atmospheric Color Palettes (When ChordPlay Modern is active)
            if (designStyle == AppDesignStyle.CHORDPLAY) {
                SettingsSectionDivider(title = stringResource(R.string.color_palette))

                SettingsRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.color_palette),
                    subtitle = stringResource(R.string.color_palette_subtitle),
                )
                PaletteCarousel(
                    selectedPalette = if (activeCustomPaletteId != null) null else colorPalette,
                    onSelectPalette = { AppSettings.setColorPalette(it) },
                    modifier = Modifier.padding(bottom = 14.dp),
                )

                // Custom Saves Subsection
                SettingsSectionDivider(title = stringResource(R.string.custom_saves))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ROW_INSET, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.saved_presets),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.saved_presets_count, customPalettes.size, 5),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (customPalettes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(14.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.preserve_style_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        customPalettes.forEach { palette ->
                            val isSelected = activeCustomPaletteId == palette.id
                            val primary = Color(palette.primaryHex)
                            val secondary = Color(palette.secondaryHex)
                            val bg = Color(palette.backgroundHex)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        AppSettings.selectCustomPalette(palette.id)
                                    }
                                    .padding(horizontal = ROW_INSET, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Active radio indicator
                                Icon(
                                    imageVector = if (isSelected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = if (isSelected) "Selected" else "Unselected",
                                    tint = if (isSelected) primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(12.dp))

                                // Swatch preview pill [● ● ●]
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(Modifier.size(10.dp).clip(CircleShape).background(primary))
                                        Box(Modifier.size(10.dp).clip(CircleShape).background(secondary))
                                        Box(Modifier.size(10.dp).clip(CircleShape).background(bg))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))

                                // Name
                                Text(
                                    text = palette.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )

                                // Rename button
                                IconButton(
                                    onClick = {
                                        paletteToRename = palette
                                        renameInput = palette.name
                                        showRenameDialog = true
                                    },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Rename preset",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }

                                // Delete button
                                IconButton(
                                    onClick = {
                                        paletteToDelete = palette
                                        showDeleteDialog = true
                                    },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete preset",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Theme Mode (Dark, Light, System)
            SettingsSectionDivider(title = stringResource(R.string.theme))

            val themeTitle = stringResource(R.string.theme)
            SettingsRow(icon = Icons.Rounded.Brightness4, title = themeTitle)
            SegmentedControl(
                options = ThemeMode.entries.map { it.localizedLabel() },
                selectedIndex = ThemeMode.entries.indexOf(theme),
                onSelect = { AppSettings.setThemeMode(ThemeMode.entries[it]) },
                modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
            )
            SettingsRow(
                icon = Icons.Rounded.Brightness4,
                title = stringResource(R.string.pure_black_amoled),
                subtitle = stringResource(R.string.pure_black_amoled_desc),
                trailing = {
                    Switch(
                        checked = amoledDarkMode,
                        onCheckedChange = AppSettings::setAmoledDarkMode,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setAmoledDarkMode(!amoledDarkMode) },
            )

            // System Typography
            SettingsSectionDivider(title = stringResource(R.string.system_typography))

            SettingsRow(
                icon = Icons.Rounded.Style,
                title = appFontChoice.localizedLabel(),
                subtitle = appFontChoice.localizedDetail(),
            )
            SegmentedControl(
                options = AppFontChoice.entries.map { it.localizedLabel() },
                selectedIndex = AppFontChoice.entries.indexOf(appFontChoice),
                onSelect = { AppSettings.setAppFontChoice(AppFontChoice.entries[it]) },
                modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
            )

            // Display Density
            SettingsSectionDivider(title = stringResource(R.string.display_density))

            val displayDensityTitle = stringResource(R.string.display_density)
            SliderRow(
                icon = Icons.Rounded.GridView,
                title = displayDensityTitle,
                subtitle = stringResource(R.string.display_density_subtitle),
                value = displayDensityLabel(displayDensityScale),
                sliderValue = displayDensityScale,
                onSliderValue = { AppSettings.setDisplayDensityScale(it) },
                valueRange = AppSettings.MIN_DISPLAY_DENSITY_SCALE..AppSettings.MAX_DISPLAY_DENSITY_SCALE,
                steps = 14,
            )

            // Glass & Animation Effects
            SettingsSectionDivider(title = stringResource(R.string.liquid_glass))

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

            val showLivePreviews by AppSettings.showSettingsLivePreviews.collectAsStateWithLifecycle()
            SettingsRow(
                icon = Icons.Rounded.Visibility,
                title = stringResource(R.string.settings_live_previews),
                subtitle = stringResource(R.string.settings_live_previews_desc),
                trailing = {
                    Switch(
                        checked = showLivePreviews,
                        onCheckedChange = AppSettings::setShowSettingsLivePreviews,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setShowSettingsLivePreviews(!showLivePreviews) },
            )

            // Swipe Gestures
            SettingsSectionDivider(title = stringResource(R.string.swipe_gestures))

            SettingsRow(
                icon = Icons.Rounded.SwipeRight,
                title = stringResource(R.string.right_swipe_action),
                subtitle = songSwipeRightAction.localizedLabel(),
                onClick = { showRightSwipePicker = true },
            )
            SettingsRow(
                icon = Icons.Rounded.SwipeLeft,
                title = stringResource(R.string.left_swipe_action),
                subtitle = songSwipeLeftAction.localizedLabel(),
                onClick = { showLeftSwipePicker = true },
            )

            // Performance & Refresh Rates
            SettingsSectionDivider(title = stringResource(R.string.performance))

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

            // App Language
            SettingsSectionDivider(title = stringResource(R.string.language))

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

    if (showRightSwipePicker) {
        SwipeActionPickerDialog(
            title = stringResource(R.string.right_swipe_action),
            currentAction = songSwipeRightAction,
            onSelect = { action ->
                AppSettings.setSongSwipeRightAction(action)
                if (action == SongSwipeAction.PLAY_NEXT) {
                    AppSettings.setSwipeToPlayNext(true)
                } else if (action == SongSwipeAction.ADD_TO_QUEUE) {
                    AppSettings.setSwipeToPlayNext(false)
                }
            },
            onDismiss = { showRightSwipePicker = false },
        )
    }

    if (showLeftSwipePicker) {
        SwipeActionPickerDialog(
            title = stringResource(R.string.left_swipe_action),
            currentAction = songSwipeLeftAction,
            onSelect = { action ->
                AppSettings.setSongSwipeLeftAction(action)
            },
            onDismiss = { showLeftSwipePicker = false },
        )
    }

    if (showRenameDialog && paletteToRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Preset") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newName = renameInput.trim()
                        if (newName.isNotBlank()) {
                            AppSettings.renameCustomPalette(paletteToRename!!.id, newName)
                        }
                        showRenameDialog = false
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showDeleteDialog && paletteToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Preset?") },
            text = {
                Text("Are you sure you want to delete \"${paletteToDelete!!.name}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppSettings.deleteCustomPalette(paletteToDelete!!.id)
                        showDeleteDialog = false
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SwipeActionPickerDialog(
    title: String,
    currentAction: SongSwipeAction,
    onSelect: (SongSwipeAction) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SongSwipeAction.entries.forEach { action ->
                    val isSelected = action == currentAction
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelect(action)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = action.localizedLabel(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * Curated color palette carousel with rich live checkmarks and atmospheric swatches.
 */
@Composable
private fun PaletteCarousel(
    selectedPalette: ChordPlayPalette?,
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

            val isDynamicArt = item == ChordPlayPalette.DYNAMIC_ARTWORK
            val goldColor = Color(0xFFFFD700)
            val goldBrush = Brush.sweepGradient(
                listOf(Color(0xFFFFD700), Color(0xFFFFA000), Color(0xFFFFE082), Color(0xFFFFD700)),
            )

            Column(
                modifier = Modifier
                    .width(112.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) primaryColor.copy(alpha = 0.16f)
                        else if (isDynamicArt) goldColor.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    )
                    .border(
                        width = if (isSelected) 2.dp else if (isDynamicArt) 1.5.dp else 1.dp,
                        color = if (isSelected) primaryColor else if (isDynamicArt) goldColor.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectPalette(item)
                    }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Swatch orb with prominent golden circle for Dynamic Song Art
                Box(
                    modifier = Modifier
                        .size(if (isDynamicArt) 48.dp else 44.dp)
                        .then(
                            if (isDynamicArt) {
                                Modifier
                                    .border(2.5.dp, goldBrush, CircleShape)
                                    .padding(3.dp)
                            } else Modifier
                        )
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
                                tint = if (isDynamicArt) goldColor else Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected || isDynamicArt) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else if (isDynamicArt) {
                        goldColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
