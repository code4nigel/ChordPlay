/*
 * Copyright (C) 2026 Shivanshu (code4nigel) <https://github.com/code4nigel/ChordPlay>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.music.bitchord.ui.screens.settings.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BlurOff
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.KeepScreenOnDuration
import com.music.bitchord.data.settings.PerformanceProfile
import com.music.bitchord.data.settings.PerformanceTier
import com.music.bitchord.ui.components.AlertAction
import com.music.bitchord.ui.components.AlertRule
import com.music.bitchord.ui.components.GlassAlert
import com.music.bitchord.ui.components.IosSwitch
import com.music.bitchord.ui.performance.displayDensityLabel
import com.music.bitchord.ui.performance.resolvePerformanceRefreshRate
import com.music.bitchord.ui.performance.supportedPerformanceRefreshRates
import com.music.bitchord.ui.screens.AutomixPerformanceSheet
import com.music.bitchord.ui.screens.EnableLoggingRow
import com.music.bitchord.ui.screens.ROW_INSET
import com.music.bitchord.ui.screens.RowDivider
import com.music.bitchord.ui.screens.SegmentedControl
import com.music.bitchord.ui.screens.SettingsGroup
import com.music.bitchord.ui.screens.SettingsRow
import com.music.bitchord.ui.screens.SettingsSearchEmpty
import com.music.bitchord.ui.screens.SliderRow
import com.music.bitchord.ui.screens.SettingsSectionDivider
import com.music.bitchord.ui.screens.SettingsSubPageHeader
import com.music.bitchord.ui.screens.localizedLabel
import com.music.bitchord.ui.screens.matchesSearch
import com.music.bitchord.ui.screens.settings.localizedLabel

/**
 * Dedicated sub-page for Performance Profile selection, refresh-rate policy,
 * blur/motion overrides, optional Automix/Canvas linking, and diagnostics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedPerformanceSection(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    initialSearchQuery: String = "",
    modifier: Modifier = Modifier,
) {
    val performancePolicy by AppSettings.performancePolicy.collectAsStateWithLifecycle()
    val performanceRefreshRate by AppSettings.performanceRefreshRate.collectAsStateWithLifecycle()
    val automixPerformance by AppSettings.automixPerformanceMode.collectAsStateWithLifecycle()
    val stopOnTaskRemoved by AppSettings.stopOnTaskRemoved.collectAsStateWithLifecycle()
    val showLivePreviews by AppSettings.showSettingsLivePreviews.collectAsStateWithLifecycle()
    val keepScreenOn by AppSettings.keepScreenOn.collectAsStateWithLifecycle()
    val keepScreenOnDuration by AppSettings.keepScreenOnDuration.collectAsStateWithLifecycle()
    val nerdStats by AppSettings.showNerdStats.collectAsStateWithLifecycle()
    val displayDensityScale by AppSettings.displayDensityScale.collectAsStateWithLifecycle()

    val currentDisplay = LocalView.current.display
    val supportedRefreshRates = remember(currentDisplay) {
        currentDisplay.supportedPerformanceRefreshRates()
    }
    val selectedPerformanceRefreshRate = remember(currentDisplay, performanceRefreshRate) {
        currentDisplay.resolvePerformanceRefreshRate(performanceRefreshRate)
    }

    LaunchedEffect(supportedRefreshRates, selectedPerformanceRefreshRate, performanceRefreshRate) {
        if (supportedRefreshRates.size > 1 && selectedPerformanceRefreshRate != performanceRefreshRate) {
            AppSettings.setPerformanceRefreshRate(selectedPerformanceRefreshRate)
        }
    }

    var showHighPerformanceConfirm by remember { mutableStateOf(false) }
    var pickingAutomixPerformance by remember { mutableStateOf(false) }
    var searchQuery by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.advanced_and_performance),
            onBack = onBack,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            scrollState = scrollState,
        )

        val showPerformanceProfile = matchesSearch(
            searchQuery,
            stringResource(R.string.performance_profile),
            stringResource(R.string.performance_profile_auto),
            stringResource(R.string.performance_profile_balanced),
            stringResource(R.string.performance_profile_saver),
            stringResource(R.string.performance_profile_high),
            stringResource(R.string.performance_profile_tier_saver),
            stringResource(R.string.refresh_rate),
            "refresh", "rate", "hz", "120hz", "90hz", "144hz", "vrr", "fps", "performance", "profile", "battery", "solid", "light motion",
        )
        val showLinkAutomixCanvas = matchesSearch(
            searchQuery,
            stringResource(R.string.link_automix_canvas),
            stringResource(R.string.link_automix_canvas_subtitle),
            "link", "automix", "canvas", "live blur", "profile",
        )
        val showReduceBlur = matchesSearch(
            searchQuery,
            stringResource(R.string.reduce_dynamic_blur),
            stringResource(R.string.reduce_dynamic_blur_subtitle),
            "blur", "dynamic", "performance",
        )
        val showReduceAnimation = matchesSearch(
            searchQuery,
            stringResource(R.string.reduce_animation),
            stringResource(R.string.reduce_animation_subtitle),
            "motion", "animation", "animations",
        )
        val showLivePreviewsSetting = matchesSearch(
            searchQuery,
            stringResource(R.string.settings_live_previews),
            stringResource(R.string.settings_live_previews_desc),
            "live", "preview", "previews", "cover", "miniplayer", "player style",
        )
        val showKeepScreenOn = matchesSearch(
            searchQuery,
            stringResource(R.string.keep_screen_on),
            stringResource(R.string.keep_screen_on_subtitle),
            stringResource(R.string.keep_screen_on_1_min),
            stringResource(R.string.keep_screen_on_5_min),
            stringResource(R.string.keep_screen_on_15_min),
            stringResource(R.string.keep_screen_on_indefinitely),
            "screen", "awake", "timeout", "display", "sleep", "indefinitely", "minutes",
        )
        val showDisplayDensity = matchesSearch(
            searchQuery,
            stringResource(R.string.display_density),
            stringResource(R.string.display_density_subtitle),
            "density", "scale", "grid", "compact",
        )
        val showAutomixPerf = matchesSearch(
            searchQuery,
            stringResource(R.string.automix),
            stringResource(R.string.automix_performance),
            stringResource(R.string.automix_performance_subtitle),
            automixPerformance.localizedLabel(),
            "cpu", "threads", "processing",
        )
        val showStopOnClose = matchesSearch(
            searchQuery,
            stringResource(R.string.stop_music_on_close),
            stringResource(R.string.stop_music_on_close_subtitle),
            "close", "recents", "task", "stop",
        )
        val showNerdStats = matchesSearch(
            searchQuery,
            stringResource(R.string.show_nerd_stats),
            stringResource(R.string.show_nerd_stats_subtitle),
            "nerd", "stats", "bitrate", "codec", "diagnostics",
        )
        val showEnableLogging = matchesSearch(
            searchQuery,
            stringResource(R.string.enable_logging),
            stringResource(R.string.enable_logging_subtitle),
            "log", "logging", "debug", "bug", "report", "crash", "share", "save", "experimental",
        )

        val hasDisplayPerf = showPerformanceProfile || showLinkAutomixCanvas || showReduceBlur ||
            showReduceAnimation || showLivePreviewsSetting || showKeepScreenOn || showDisplayDensity
        val hasAutomix = showAutomixPerf || showStopOnClose
        val hasDiagnostics = showNerdStats || showEnableLogging
        val anyAdvancedMatch = hasDisplayPerf || hasAutomix || hasDiagnostics

        if (searchQuery.isNotBlank() && !anyAdvancedMatch) {
            SettingsSearchEmpty(query = searchQuery)
        } else {
            // Single Unified Advanced & Performance Settings Block
            SettingsGroup(
                header = stringResource(R.string.advanced_and_performance),
                footer = stringResource(R.string.show_nerd_stats_subtitle),
            ) {
                var perfPreceding = false
                if (showPerformanceProfile) {
                    val profiles = PerformanceProfile.entries
                    val profileLabels = profiles.map { it.localizedLabel() }
                    val tierDescription = when (performancePolicy.resolvedTier) {
                        PerformanceTier.SAVER -> stringResource(R.string.performance_profile_saver_desc)
                        PerformanceTier.BALANCED -> stringResource(R.string.performance_profile_balanced_desc)
                        PerformanceTier.HIGH -> stringResource(
                            R.string.performance_profile_high_desc,
                            selectedPerformanceRefreshRate,
                        )
                    }
                    val profileSubtitle = if (performancePolicy.selectedProfile == PerformanceProfile.AUTO) {
                        "${stringResource(R.string.performance_profile_auto_prefix, performancePolicy.resolvedTier.localizedLabel())} — $tierDescription"
                    } else {
                        tierDescription
                    }
                    SettingsRow(
                        icon = Icons.Rounded.Speed,
                        title = stringResource(R.string.performance_profile),
                        subtitle = profileSubtitle,
                        badge = if (performancePolicy.isCustomized) {
                            stringResource(R.string.performance_profile_customized)
                        } else {
                            null
                        },
                    )
                    SegmentedControl(
                        options = profileLabels,
                        selectedIndex = profiles.indexOf(performancePolicy.selectedProfile).coerceAtLeast(0),
                        onSelect = { index ->
                            val chosen = profiles[index]
                            if (chosen == PerformanceProfile.HIGH &&
                                performancePolicy.selectedProfile != PerformanceProfile.HIGH
                            ) {
                                showHighPerformanceConfirm = true
                            } else {
                                AppSettings.setPerformanceProfile(chosen)
                            }
                        },
                        modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
                    )
                    perfPreceding = true

                    if (supportedRefreshRates.size > 1 && performancePolicy.resolvedTier != PerformanceTier.SAVER) {
                        RowDivider()
                        val refreshRateSubtitle = if (performancePolicy.resolvedTier == PerformanceTier.HIGH) {
                            stringResource(R.string.refresh_rate_pinned_subtitle, selectedPerformanceRefreshRate)
                        } else {
                            stringResource(R.string.refresh_rate_adaptive_subtitle, selectedPerformanceRefreshRate)
                        }
                        SettingsRow(
                            icon = Icons.Rounded.Speed,
                            title = stringResource(R.string.refresh_rate),
                            subtitle = refreshRateSubtitle,
                        )
                        SegmentedControl(
                            options = supportedRefreshRates.map { "$it Hz" },
                            selectedIndex = supportedRefreshRates.indexOf(selectedPerformanceRefreshRate).coerceAtLeast(0),
                            onSelect = { AppSettings.setPerformanceRefreshRate(supportedRefreshRates[it]) },
                            modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
                        )
                    }
                }

                if (showLinkAutomixCanvas) {
                    if (perfPreceding) RowDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Link,
                        title = stringResource(R.string.link_automix_canvas),
                        subtitle = stringResource(R.string.link_automix_canvas_subtitle),
                        trailing = {
                            IosSwitch(
                                checked = performancePolicy.linkAutomixAndCanvas,
                                onCheckedChange = AppSettings::setLinkAutomixAndCanvas,
                            )
                        },
                        onClick = { AppSettings.setLinkAutomixAndCanvas(!performancePolicy.linkAutomixAndCanvas) },
                    )
                    perfPreceding = true
                }

                if (showReduceBlur) {
                    if (perfPreceding) RowDivider()
                    val reduceBlurTitle = stringResource(R.string.reduce_dynamic_blur)
                    SettingsRow(
                        icon = Icons.Rounded.BlurOff,
                        title = reduceBlurTitle,
                        subtitle = stringResource(R.string.reduce_dynamic_blur_subtitle),
                        trailing = {
                            IosSwitch(
                                checked = performancePolicy.reduceDynamicBlur,
                                onCheckedChange = AppSettings::setReduceDynamicBlur,
                            )
                        },
                        onClick = { AppSettings.setReduceDynamicBlur(!performancePolicy.reduceDynamicBlur) },
                    )
                    perfPreceding = true
                }

                if (showReduceAnimation) {
                    if (perfPreceding) RowDivider()
                    val reduceAnimationTitle = stringResource(R.string.reduce_animation)
                    SettingsRow(
                        icon = Icons.Rounded.MotionPhotosOff,
                        title = reduceAnimationTitle,
                        subtitle = stringResource(R.string.reduce_animation_subtitle),
                        trailing = {
                            IosSwitch(
                                checked = performancePolicy.reduceAnimation,
                                onCheckedChange = AppSettings::setReduceAnimation,
                            )
                        },
                        onClick = { AppSettings.setReduceAnimation(!performancePolicy.reduceAnimation) },
                    )
                    perfPreceding = true
                }

                if (showLivePreviewsSetting) {
                    if (perfPreceding) RowDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Visibility,
                        title = stringResource(R.string.settings_live_previews),
                        subtitle = stringResource(R.string.settings_live_previews_desc),
                        trailing = {
                            IosSwitch(
                                checked = showLivePreviews,
                                onCheckedChange = AppSettings::setShowSettingsLivePreviews,
                            )
                        },
                        onClick = { AppSettings.setShowSettingsLivePreviews(!showLivePreviews) },
                    )
                    perfPreceding = true
                }

                if (showKeepScreenOn) {
                    if (perfPreceding) RowDivider()
                    SettingsRow(
                        icon = Icons.Rounded.WbSunny,
                        title = stringResource(R.string.keep_screen_on),
                        subtitle = stringResource(R.string.keep_screen_on_subtitle),
                        trailing = {
                            IosSwitch(
                                checked = keepScreenOn,
                                onCheckedChange = AppSettings::setKeepScreenOn,
                            )
                        },
                        onClick = { AppSettings.setKeepScreenOn(!keepScreenOn) },
                    )
                    if (keepScreenOn) {
                        val durationLabels = listOf(
                            stringResource(R.string.keep_screen_on_1_min),
                            stringResource(R.string.keep_screen_on_5_min),
                            stringResource(R.string.keep_screen_on_15_min),
                            stringResource(R.string.keep_screen_on_indefinitely),
                        )
                        val durations = KeepScreenOnDuration.entries
                        SegmentedControl(
                            options = durationLabels,
                            selectedIndex = durations.indexOf(keepScreenOnDuration).coerceAtLeast(0),
                            onSelect = { index -> AppSettings.setKeepScreenOnDuration(durations[index]) },
                            modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
                        )
                    }
                    perfPreceding = true
                }

                if (showDisplayDensity) {
                    if (perfPreceding) RowDivider()
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
                }

                // Automix CPU Processing Section Divider
                if (hasAutomix) {
                    if (hasDisplayPerf) {
                        SettingsSectionDivider(title = stringResource(R.string.automix))
                    }

                    var automixPreceding = false
                    if (showAutomixPerf) {
                        val automixPerformanceTitle = stringResource(R.string.automix_performance)
                        SettingsRow(
                            icon = Icons.Rounded.Tune,
                            title = automixPerformanceTitle,
                            subtitle = stringResource(R.string.automix_performance_subtitle),
                            value = automixPerformance.localizedLabel(),
                            onClick = { pickingAutomixPerformance = true },
                        )
                        automixPreceding = true
                    }

                    if (showStopOnClose) {
                        if (automixPreceding) RowDivider()
                        val stopMusicOnCloseTitle = stringResource(R.string.stop_music_on_close)
                        SettingsRow(
                            icon = Icons.Rounded.MusicOff,
                            title = stopMusicOnCloseTitle,
                            subtitle = stringResource(R.string.stop_music_on_close_subtitle),
                            trailing = {
                                IosSwitch(
                                    checked = stopOnTaskRemoved,
                                    onCheckedChange = AppSettings::setStopOnTaskRemoved,
                                )
                            },
                            onClick = { AppSettings.setStopOnTaskRemoved(!stopOnTaskRemoved) },
                        )
                    }
                }

                // Nerd Stats, Logging & Diagnostics Section Divider
                if (hasDiagnostics) {
                    if (hasDisplayPerf || hasAutomix) {
                        SettingsSectionDivider(title = stringResource(R.string.advanced_options))
                    }

                    if (showNerdStats) {
                        val nerdStatsTitle = stringResource(R.string.show_nerd_stats)
                        SettingsRow(
                            icon = Icons.Rounded.BarChart,
                            title = nerdStatsTitle,
                            subtitle = stringResource(R.string.show_nerd_stats_subtitle),
                            trailing = {
                                IosSwitch(
                                    checked = nerdStats,
                                    onCheckedChange = AppSettings::setShowNerdStats,
                                )
                            },
                            onClick = { AppSettings.setShowNerdStats(!nerdStats) },
                        )
                    }

                    if (showEnableLogging) {
                        if (showNerdStats) RowDivider()
                        EnableLoggingRow(title = stringResource(R.string.enable_logging))
                    }
                }
            }
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

    if (showHighPerformanceConfirm) {
        GlassAlert(
            title = stringResource(R.string.performance_profile_high_confirm_title),
            onDismiss = { showHighPerformanceConfirm = false },
            footerActions = {
                AlertAction(
                    label = stringResource(R.string.enable),
                    emphasised = true,
                    onClick = {
                        showHighPerformanceConfirm = false
                        AppSettings.setPerformanceProfile(PerformanceProfile.HIGH)
                    },
                )
                AlertRule()
                AlertAction(
                    label = stringResource(R.string.cancel),
                    emphasised = false,
                    onClick = { showHighPerformanceConfirm = false },
                )
            },
        ) {
            val ink = LocalContentColor.current
            Text(
                text = stringResource(
                    R.string.performance_profile_high_confirm_body,
                    selectedPerformanceRefreshRate,
                ),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
                color = ink.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}
