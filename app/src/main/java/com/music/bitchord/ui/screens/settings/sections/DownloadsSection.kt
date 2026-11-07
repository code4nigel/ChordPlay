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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.downloads),
            onBack = onBack,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
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
                            Switch(
                                checked = saveToCustomLocation,
                                onCheckedChange = AppSettings::setSaveToCustomDownloadLocation,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                                ),
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
                            Switch(
                                checked = downloadLyricsOffline,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        showLyricsDisclaimerDialog = true
                                    } else {
                                        AppSettings.setDownloadLyricsOffline(false)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                                ),
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
                                Switch(
                                    checked = wifiOnlyDownloads,
                                    onCheckedChange = AppSettings::setWifiOnlyDownloads,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
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
        AlertDialog(
            onDismissRequest = { showLyricsDisclaimerDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.offline_lyrics_disclaimer_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.offline_lyrics_disclaimer_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppSettings.setDownloadLyricsOffline(true)
                        showLyricsDisclaimerDialog = false
                    },
                ) {
                    Text(
                        text = stringResource(R.string.agree_and_enable),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLyricsDisclaimerDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
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
