package com.music.bitchord.ui.screens.settings.sections

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BlurOff
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.AutomixPerformanceMode
import com.music.bitchord.ui.performance.displayDensityLabel
import com.music.bitchord.ui.performance.resolvePerformanceRefreshRate
import com.music.bitchord.ui.performance.supportedPerformanceRefreshRates
import com.music.bitchord.ui.screens.AutomixPerformanceSheet
import com.music.bitchord.ui.screens.ROW_INSET
import com.music.bitchord.ui.screens.RowDivider
import com.music.bitchord.ui.screens.SegmentedControl
import com.music.bitchord.ui.screens.SettingsGroup
import com.music.bitchord.ui.screens.SettingsRow
import com.music.bitchord.ui.screens.SliderRow
import com.music.bitchord.ui.screens.SettingsSectionDivider
import com.music.bitchord.ui.screens.SettingsSubPageHeader
import com.music.bitchord.ui.screens.localizedLabel

/**
 * Dedicated sub-page for high refresh rate display settings, dynamic blur controls,
 * animation reduction, Automix CPU performance, and Nerd Stats diagnostics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedPerformanceSection(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val highPerformanceMode by AppSettings.highPerformanceMode.collectAsStateWithLifecycle()
    val performanceRefreshRate by AppSettings.performanceRefreshRate.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val automixPerformance by AppSettings.automixPerformanceMode.collectAsStateWithLifecycle()
    val stopOnTaskRemoved by AppSettings.stopOnTaskRemoved.collectAsStateWithLifecycle()
    val nerdStats by AppSettings.showNerdStats.collectAsStateWithLifecycle()
    val displayDensityScale by AppSettings.displayDensityScale.collectAsStateWithLifecycle()

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
    var pickingAutomixPerformance by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

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
            title = stringResource(R.string.advanced_and_performance),
            onBack = onBack,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
        )

        // Single Unified Advanced & Performance Settings Block
        SettingsGroup(
            header = stringResource(R.string.advanced_and_performance),
            footer = stringResource(R.string.show_nerd_stats_subtitle),
        ) {
            val highPerformanceModeTitle = stringResource(R.string.high_performance_mode)
            SettingsRow(
                icon = Icons.Rounded.Speed,
                title = highPerformanceModeTitle,
                subtitle = if (highPerformanceMode) {
                    stringResource(R.string.high_performance_active, selectedPerformanceRefreshRate ?: 60)
                } else {
                    stringResource(R.string.high_performance_subtitle)
                },
                trailing = {
                    Switch(
                        checked = highPerformanceMode,
                        onCheckedChange = { targetState ->
                            if (targetState) {
                                showPerformanceWarning = true
                            } else {
                                AppSettings.setHighPerformanceMode(false)
                            }
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
                RowDivider()
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

            RowDivider()

            val reduceBlurTitle = stringResource(R.string.reduce_dynamic_blur)
            SettingsRow(
                icon = Icons.Rounded.BlurOff,
                title = reduceBlurTitle,
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

            RowDivider()

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

            RowDivider()

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

            // Automix CPU Processing Section Divider
            SettingsSectionDivider(title = stringResource(R.string.automix))

            val automixPerformanceTitle = stringResource(R.string.automix_performance)
            SettingsRow(
                icon = Icons.Rounded.Tune,
                title = automixPerformanceTitle,
                subtitle = stringResource(R.string.automix_performance_subtitle),
                value = automixPerformance.localizedLabel(),
                onClick = { pickingAutomixPerformance = true },
            )

            RowDivider()

            val stopMusicOnCloseTitle = stringResource(R.string.stop_music_on_close)
            SettingsRow(
                icon = Icons.Rounded.MusicOff,
                title = stopMusicOnCloseTitle,
                subtitle = stringResource(R.string.stop_music_on_close_subtitle),
                trailing = {
                    Switch(
                        checked = stopOnTaskRemoved,
                        onCheckedChange = AppSettings::setStopOnTaskRemoved,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setStopOnTaskRemoved(!stopOnTaskRemoved) },
            )

            // Nerd Stats & Diagnostics Section Divider
            SettingsSectionDivider(title = stringResource(R.string.advanced_options))

            val nerdStatsTitle = stringResource(R.string.show_nerd_stats)
            SettingsRow(
                icon = Icons.Rounded.BarChart,
                title = nerdStatsTitle,
                subtitle = stringResource(R.string.show_nerd_stats_subtitle),
                trailing = {
                    Switch(
                        checked = nerdStats,
                        onCheckedChange = AppSettings::setShowNerdStats,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setShowNerdStats(!nerdStats) },
            )
        }
    }

    if (pickingAutomixPerformance) {
        ModalBottomSheet(
            onDismissRequest = { pickingAutomixPerformance = false },
        ) {
            AutomixPerformanceSheet(
                selected = automixPerformance,
                onSelect = {
                    AppSettings.setAutomixPerformanceMode(it)
                    pickingAutomixPerformance = false
                },
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
