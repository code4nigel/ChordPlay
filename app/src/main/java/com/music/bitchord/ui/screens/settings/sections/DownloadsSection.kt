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

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.AlertAction
import com.music.bitchord.ui.components.AlertRule
import com.music.bitchord.ui.components.GlassAlert
import com.music.bitchord.ui.components.IosSwitch
import com.music.bitchord.ui.screens.*

/**
 * Downloads management sub-page: custom storage location, offline synced lyrics,
 * and download connection quality preferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsSection(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    initialSearchQuery: String = "",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val saveToCustomLocation by AppSettings.saveToCustomDownloadLocation.collectAsStateWithLifecycle()
    val customLocationUri by AppSettings.customDownloadLocationUri.collectAsStateWithLifecycle()
    val downloadLyricsOffline by AppSettings.downloadLyricsOffline.collectAsStateWithLifecycle()
    val downloadQuality by AppSettings.downloadQuality.collectAsStateWithLifecycle()
    val wifiOnlyDownloads by AppSettings.wifiOnlyDownloads.collectAsStateWithLifecycle()

    var showLyricsDisclaimerDialog by remember { mutableStateOf(false) }
    var pickingDownloadQuality by remember { mutableStateOf(false) }

    val customFolderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { folder ->
        if (folder == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                folder,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        AppSettings.setCustomDownloadLocationUri(folder.toString())
        AppSettings.setSaveToCustomDownloadLocation(true)
    }

    val customLocationDisplay = remember(customLocationUri) {
        AppSettings.customDownloadLocationDisplay()
    }

    var searchQuery by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.downloads),
            onBack = onBack,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            scrollState = scrollState,
        )

        val saveCustomTitle = stringResource(R.string.save_to_custom_download_location)
        val saveCustomSubtitle = stringResource(R.string.save_to_custom_download_location_subtitle)
        val showSaveCustom = matchesSearch(searchQuery, saveCustomTitle, saveCustomSubtitle, "custom", "location", "folder", "save")

        val locationTitle = stringResource(R.string.download_location)
        val showLocation = saveToCustomLocation && matchesSearch(searchQuery, locationTitle, customLocationDisplay, "location", "folder", "path", "sd card")

        val offlineLyricsTitle = stringResource(R.string.download_lyrics_offline)
        val offlineLyricsSubtitle = stringResource(R.string.download_lyrics_offline_subtitle)
        val showOfflineLyrics = matchesSearch(searchQuery, offlineLyricsTitle, offlineLyricsSubtitle, "offline", "lyrics", "synced")

        val downloadQualityTitle = stringResource(R.string.download_quality)
        val showDownloadQuality = matchesSearch(searchQuery, downloadQualityTitle, downloadQuality.localizedLabel(), "quality", "audio", "lossless", "download")

        val downloadWifiOnlyTitle = stringResource(R.string.download_wifi_only)
        val showWifiOnly = matchesSearch(searchQuery, downloadWifiOnlyTitle, "wifi", "cellular", "data", "mobile data")

        val hasStorageGroup = showSaveCustom || showLocation
        val hasLyricsGroup = showOfflineLyrics
        val hasQualityGroup = showDownloadQuality || showWifiOnly
        val anyDownloadMatch = hasStorageGroup || hasLyricsGroup || hasQualityGroup

        if (searchQuery.isNotBlank() && !anyDownloadMatch) {
            SettingsSearchEmpty(query = searchQuery)
        } else {
            // Single Unified Downloads Block
            SettingsGroup(header = null) {
                if (showSaveCustom) {
                    SettingsRow(
                        icon = Icons.Rounded.FolderSpecial,
                        title = saveCustomTitle,
                        subtitle = saveCustomSubtitle,
                        trailing = {
                            IosSwitch(
                                checked = saveToCustomLocation,
                                onCheckedChange = AppSettings::setSaveToCustomDownloadLocation,
                            )
                        },
                        onClick = { AppSettings.setSaveToCustomDownloadLocation(!saveToCustomLocation) },
                    )
                }

                if (showLocation) {
                    if (showSaveCustom) RowDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Folder,
                        title = locationTitle,
                        subtitle = customLocationDisplay,
                        enabled = saveToCustomLocation,
                        onClick = { customFolderPicker.launch(null) },
                    )
                }

                // Offline Lyrics Section Divider
                if (hasLyricsGroup) {
                    if (hasStorageGroup) {
                        SettingsSectionDivider(title = stringResource(R.string.synced_lyrics))
                    }
                    SettingsRow(
                        icon = Icons.Rounded.Lyrics,
                        title = offlineLyricsTitle,
                        subtitle = offlineLyricsSubtitle,
                        trailing = {
                            IosSwitch(
                                checked = downloadLyricsOffline,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        showLyricsDisclaimerDialog = true
                                    } else {
                                        AppSettings.setDownloadLyricsOffline(false)
                                    }
                                },
                            )
                        },
                        onClick = {
                            if (!downloadLyricsOffline) {
                                showLyricsDisclaimerDialog = true
                            } else {
                                AppSettings.setDownloadLyricsOffline(false)
                            }
                        },
                    )
                }

                // Audio Quality & Network Preferences Section Divider
                if (hasQualityGroup) {
                    if (hasStorageGroup || hasLyricsGroup) {
                        SettingsSectionDivider(title = stringResource(R.string.audio_quality))
                    }

                    if (showDownloadQuality) {
                        SettingsRow(
                            icon = Icons.Rounded.Download,
                            title = downloadQualityTitle,
                            value = downloadQuality.localizedLabel(),
                            onClick = { pickingDownloadQuality = true },
                        )
                    }

                    if (showWifiOnly) {
                        if (showDownloadQuality) RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Wifi,
                            title = downloadWifiOnlyTitle,
                            trailing = {
                                IosSwitch(
                                    checked = wifiOnlyDownloads,
                                    onCheckedChange = AppSettings::setWifiOnlyDownloads,
                                )
                            },
                            onClick = { AppSettings.setWifiOnlyDownloads(!wifiOnlyDownloads) },
                        )
                    }
                }
            }
        }
    }

    // Offline Lyrics Legal Disclaimer Dialog
    if (showLyricsDisclaimerDialog) {
        GlassAlert(
            title = stringResource(R.string.offline_lyrics_disclaimer_title),
            onDismiss = { showLyricsDisclaimerDialog = false },
            footerActions = {
                AlertAction(
                    label = stringResource(R.string.agree_and_enable),
                    emphasised = true,
                    onClick = {
                        AppSettings.setDownloadLyricsOffline(true)
                        showLyricsDisclaimerDialog = false
                    },
                )
                AlertRule()
                AlertAction(
                    label = stringResource(R.string.cancel),
                    emphasised = false,
                    onClick = { showLyricsDisclaimerDialog = false },
                )
            },
        ) {
            Text(
                text = stringResource(R.string.offline_lyrics_disclaimer_message),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
                color = LocalContentColor.current.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
        }
    }

    if (pickingDownloadQuality) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { pickingDownloadQuality = false },
        ) {
            com.music.bitchord.ui.screens.DownloadQualitySheet(
                selected = downloadQuality,
                onSelect = { quality ->
                    AppSettings.setDownloadQuality(quality)
                    pickingDownloadQuality = false
                },
            )
        }
    }
}
