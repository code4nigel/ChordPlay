package com.music.bitchord.ui.screens.settings.sections

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import com.music.bitchord.R
import com.music.bitchord.data.LocalMediaRepository
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.stats.Backup
import com.music.bitchord.playback.AudioCache
import com.music.bitchord.ui.screens.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Storage, cache limits, downloads, and backup/restore sub-page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSection(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    initialSearchQuery: String = "",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val backupScope = rememberCoroutineScope()

    val cacheLimitBytes by AppSettings.audioCacheLimitBytes.collectAsStateWithLifecycle()
    val showCacheFolder by AppSettings.showCacheFolder.collectAsStateWithLifecycle()
    val downloadQuality by AppSettings.downloadQuality.collectAsStateWithLifecycle()
    val wifiOnlyDownloads by AppSettings.wifiOnlyDownloads.collectAsStateWithLifecycle()
    val exportDownloads by AppSettings.exportDownloads.collectAsStateWithLifecycle()
    val localMusicFolderUri by AppSettings.localMusicFolderUri.collectAsStateWithLifecycle()
    val filterNonMusicAudio by AppSettings.filterNonMusicAudio.collectAsStateWithLifecycle()

    var pickingDownloadQuality by remember { mutableStateOf(false) }
    var confirmClearSongCache by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var importStatus by remember { mutableStateOf<String?>(null) }

    val localMusicFolderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { folder ->
        if (folder == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                folder,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        AppSettings.setLocalMusicFolderUri(folder.toString())
    }

    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { target ->
        if (target == null) return@rememberLauncherForActivityResult
        backupScope.launch {
            exportStatus = Backup.exportTo(context, target).fold(
                onSuccess = { months ->
                    context.getString(R.string.export_succeeded, context.countOfMonths(months))
                },
                onFailure = {
                    context.getString(R.string.export_failed, it.message ?: context.getString(R.string.unknown_error))
                },
            )
        }
    }

    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { source ->
        if (source == null) return@rememberLauncherForActivityResult
        backupScope.launch {
            importStatus = Backup.importFrom(context, source).fold(
                onSuccess = {
                    context.getString(R.string.import_succeeded, context.countOfMonths(it.months), it.from)
                },
                onFailure = {
                    context.getString(R.string.import_failed, it.message ?: context.getString(R.string.unknown_error))
                },
            )
        }
    }

    val cacheUnlimited = cacheLimitBytes == AppSettings.UNLIMITED_CACHE_LIMIT_BYTES
    val cacheLimitMb = if (cacheUnlimited) CACHE_UNLIMITED_STOP_MB else (cacheLimitBytes / (1024 * 1024)).toInt()
    val unlimitedLabel = stringResource(R.string.song_cache_unlimited)
    val cacheLimitLabel = if (cacheUnlimited) unlimitedLabel else formatCacheSize(cacheLimitMb)

    var searchQuery by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.storage),
            onBack = onBack,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
        )

        val showCacheFolderRow = matchesSearch(
            searchQuery,
            stringResource(R.string.show_cache_folder),
            stringResource(R.string.show_cache_folder_subtitle),
            "cache", "folder",
        )
        val showSongCacheLimit = matchesSearch(
            searchQuery,
            stringResource(R.string.song_cache_limit),
            cacheLimitLabel,
            "cache", "limit", "storage", "megabytes", "mb", "unlimited",
        )
        val showClearSongCache = matchesSearch(
            searchQuery,
            stringResource(R.string.clear_song_cache),
            stringResource(R.string.clear_song_cache_subtitle),
            "clear", "delete", "cache", "songs",
        )
        val showClearImageCache = matchesSearch(
            searchQuery,
            stringResource(R.string.clear_image_cache),
            stringResource(R.string.clear_image_cache_subtitle),
            "clear", "delete", "image", "artwork", "cover",
        )
        val showDownloadQuality = matchesSearch(
            searchQuery,
            stringResource(R.string.download_quality),
            downloadQuality.localizedLabel(),
            "quality", "download", "audio",
        )
        val showDownloadWifiOnly = matchesSearch(
            searchQuery,
            stringResource(R.string.download_wifi_only),
            "wifi", "cellular", "data", "mobile data", "download",
        )
        val showExportDownloads = matchesSearch(
            searchQuery,
            stringResource(R.string.export_compatible_downloads),
            "export", "downloads", "mp3", "folder", "music/chordplay",
        )
        val showLocalMusicFolder = matchesSearch(
            searchQuery,
            stringResource(R.string.local_music_folder),
            stringResource(R.string.local_music),
            LocalMediaRepository.selectedFolderLabel(localMusicFolderUri),
            "local", "folder", "audio", "device",
        )
        val showUseAllAudioFolders = localMusicFolderUri.isNotBlank() && matchesSearch(
            searchQuery,
            stringResource(R.string.use_all_audio_folders),
            stringResource(R.string.use_all_audio_folders_subtitle),
            "all", "folders", "device",
        )
        val showFilterNonMusicAudio = matchesSearch(
            searchQuery,
            stringResource(R.string.filter_non_music_audio),
            stringResource(R.string.filter_non_music_audio_subtitle),
            "filter", "short", "ringtone", "voice notes",
        )
        val showExportData = matchesSearch(
            searchQuery,
            stringResource(R.string.export_data),
            stringResource(R.string.export_data_subtitle),
            "export", "backup", "json", "save",
        )
        val showImportData = matchesSearch(
            searchQuery,
            stringResource(R.string.import_data),
            stringResource(R.string.import_data_subtitle),
            "import", "restore", "backup", "json", "load",
        )

        val hasCacheSection = showCacheFolderRow || showSongCacheLimit || showClearSongCache || showClearImageCache
        val hasDownloadsSection = showDownloadQuality || showDownloadWifiOnly || showExportDownloads
        val hasLocalMusicSection = showLocalMusicFolder || showUseAllAudioFolders || showFilterNonMusicAudio
        val hasBackupSection = showExportData || showImportData
        val anyStorageMatch = hasCacheSection || hasDownloadsSection || hasLocalMusicSection || hasBackupSection

        if (searchQuery.isNotBlank() && !anyStorageMatch) {
            SettingsSearchEmpty(query = searchQuery)
        } else {
            // Single Unified Storage Settings Block (duplicate small header removed)
            SettingsGroup(header = null) {
                var cachePreceding = false
                if (showCacheFolderRow) {
                    val showCacheFolderTitle = stringResource(R.string.show_cache_folder)
                    SettingsRow(
                        icon = Icons.Rounded.Cached,
                        title = showCacheFolderTitle,
                        subtitle = stringResource(R.string.show_cache_folder_subtitle),
                        trailing = {
                            Switch(
                                checked = showCacheFolder,
                                onCheckedChange = AppSettings::setShowCacheFolder,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        },
                        onClick = { AppSettings.setShowCacheFolder(!showCacheFolder) },
                    )
                    cachePreceding = true
                }

                if (showSongCacheLimit) {
                    if (cachePreceding) RowDivider()
                    val songCacheLimitTitle = stringResource(R.string.song_cache_limit)
                    SliderRow(
                        icon = Icons.Rounded.Storage,
                        title = songCacheLimitTitle,
                        subtitle = when {
                            cacheUnlimited -> stringResource(R.string.song_cache_unlimited_subtitle)
                            cacheLimitMb > CACHE_WARNING_MB ->
                                stringResource(R.string.song_cache_large_subtitle, cacheLimitLabel)
                            else -> stringResource(R.string.song_cache_limit_subtitle)
                        },
                        value = cacheLimitLabel,
                        sliderValue = cacheLimitMb.toFloat(),
                        onSliderValue = {
                            AppSettings.setAudioCacheLimitBytes(it.roundToInt().toLong() * 1024 * 1024)
                        },
                        valueRange = (AppSettings.DEFAULT_CACHE_LIMIT_BYTES / (1024 * 1024)).toFloat()..
                            CACHE_UNLIMITED_STOP_MB.toFloat(),
                        steps = 19,
                    )
                    cachePreceding = true
                }

                if (showClearSongCache) {
                    if (cachePreceding) RowDivider()
                    val clearSongCacheTitle = stringResource(R.string.clear_song_cache)
                    SettingsRow(
                        icon = Icons.Rounded.DeleteSweep,
                        title = clearSongCacheTitle,
                        subtitle = stringResource(R.string.clear_song_cache_subtitle),
                        onClick = { confirmClearSongCache = true },
                    )
                    cachePreceding = true
                }

                if (showClearImageCache) {
                    if (cachePreceding) RowDivider()
                    val clearImageCacheTitle = stringResource(R.string.clear_image_cache)
                    SettingsRow(
                        icon = Icons.Rounded.DeleteSweep,
                        title = clearImageCacheTitle,
                        subtitle = stringResource(R.string.clear_image_cache_subtitle),
                        onClick = {
                            val loader = SingletonImageLoader.get(context)
                            loader.memoryCache?.clear()
                            loader.diskCache?.clear()
                            Toast.makeText(context, context.getString(R.string.image_cache_cleared), Toast.LENGTH_SHORT).show()
                        },
                    )
                    cachePreceding = true
                }

                // Downloads Section Divider
                if (hasDownloadsSection) {
                    if (hasCacheSection) {
                        SettingsSectionDivider(title = stringResource(R.string.downloads))
                    }

                    var dlPreceding = false
                    if (showDownloadQuality) {
                        val downloadQualityTitle = stringResource(R.string.download_quality)
                        SettingsRow(
                            icon = Icons.Rounded.Download,
                            title = downloadQualityTitle,
                            value = downloadQuality.localizedLabel(),
                            onClick = { pickingDownloadQuality = true },
                        )
                        dlPreceding = true
                    }

                    if (showDownloadWifiOnly) {
                        if (dlPreceding) RowDivider()
                        val downloadWifiOnlyTitle = stringResource(R.string.download_wifi_only)
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
                        dlPreceding = true
                    }

                    if (showExportDownloads) {
                        if (dlPreceding) RowDivider()
                        val exportDownloadsTitle = stringResource(R.string.export_compatible_downloads)
                        SettingsRow(
                            icon = Icons.Rounded.Folder,
                            title = exportDownloadsTitle,
                            subtitle = "Music/ChordPlay".takeIf { exportDownloads },
                            trailing = {
                                Switch(
                                    checked = exportDownloads,
                                    onCheckedChange = AppSettings::setExportDownloads,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setExportDownloads(!exportDownloads) },
                        )
                    }
                }

                // Local Music Files Section Divider
                if (hasLocalMusicSection) {
                    if (hasCacheSection || hasDownloadsSection) {
                        SettingsSectionDivider(title = stringResource(R.string.local_music))
                    }

                    var localPreceding = false
                    if (showLocalMusicFolder) {
                        val localMusicFolderTitle = stringResource(R.string.local_music_folder)
                        SettingsRow(
                            icon = Icons.Rounded.Folder,
                            title = localMusicFolderTitle,
                            subtitle = LocalMediaRepository.selectedFolderLabel(localMusicFolderUri)
                                ?: stringResource(R.string.all_audio_folders),
                            onClick = { localMusicFolderPicker.launch(null) },
                        )
                        localPreceding = true
                    }

                    if (showUseAllAudioFolders) {
                        if (localPreceding) RowDivider()
                        val useAllAudioFoldersTitle = stringResource(R.string.use_all_audio_folders)
                        SettingsRow(
                            icon = Icons.Rounded.LibraryMusic,
                            title = useAllAudioFoldersTitle,
                            subtitle = stringResource(R.string.use_all_audio_folders_subtitle),
                            onClick = { AppSettings.setLocalMusicFolderUri("") },
                        )
                        localPreceding = true
                    }

                    if (showFilterNonMusicAudio) {
                        if (localPreceding) RowDivider()
                        val filterNonMusicAudioTitle = stringResource(R.string.filter_non_music_audio)
                        SettingsRow(
                            icon = Icons.Rounded.FilterAlt,
                            title = filterNonMusicAudioTitle,
                            subtitle = stringResource(R.string.filter_non_music_audio_subtitle),
                            trailing = {
                                Switch(
                                    checked = filterNonMusicAudio,
                                    onCheckedChange = AppSettings::setFilterNonMusicAudio,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setFilterNonMusicAudio(!filterNonMusicAudio) },
                        )
                    }
                }

                // Backup and Data Restoration Section Divider
                if (hasBackupSection) {
                    if (hasCacheSection || hasDownloadsSection || hasLocalMusicSection) {
                        SettingsSectionDivider(title = stringResource(R.string.your_data))
                    }

                    var backupPreceding = false
                    if (showExportData) {
                        val exportDataTitle = stringResource(R.string.export_data)
                        SettingsRow(
                            icon = Icons.Rounded.FileUpload,
                            title = exportDataTitle,
                            subtitle = exportStatus ?: stringResource(R.string.export_data_subtitle),
                            onClick = { exportPicker.launch(Backup.suggestedName()) },
                        )
                        backupPreceding = true
                    }

                    if (showImportData) {
                        if (backupPreceding) RowDivider()
                        val importDataTitle = stringResource(R.string.import_data)
                        SettingsRow(
                            icon = Icons.Rounded.FileDownload,
                            title = importDataTitle,
                            subtitle = importStatus ?: stringResource(R.string.import_data_subtitle),
                            onClick = { confirmImport = true },
                        )
                    }
                }
            }
        }
    }

    if (pickingDownloadQuality) {
        ModalBottomSheet(
            onDismissRequest = { pickingDownloadQuality = false },
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            DownloadQualitySheet(
                selected = downloadQuality,
                onSelect = { quality ->
                    AppSettings.setDownloadQuality(quality)
                    pickingDownloadQuality = false
                },
            )
        }
    }

    if (confirmClearSongCache) {
        AlertDialog(
            onDismissRequest = { confirmClearSongCache = false },
            title = { Text(stringResource(R.string.clear_song_cache_title)) },
            text = { Text(stringResource(R.string.clear_song_cache_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearSongCache = false
                    AudioCache.clear {
                        Toast.makeText(context, context.getString(R.string.song_cache_cleared), Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearSongCache = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text(stringResource(R.string.import_backup_title)) },
            text = { Text(stringResource(R.string.import_backup_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    importPicker.launch(arrayOf("application/json", "text/plain", "*/*"))
                }) {
                    Text(stringResource(R.string.choose_file))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
