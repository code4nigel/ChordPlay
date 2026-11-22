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

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.stats.Backup
import com.music.bitchord.data.stats.ChordPlayIdentityStore
import com.music.bitchord.data.stats.ChordPlayPassPng
import com.music.bitchord.data.stats.WaveIdentitySignature
import com.music.bitchord.ui.onboarding.OnboardingState
import com.music.bitchord.ui.screens.RowDivider
import com.music.bitchord.ui.screens.SettingsGroup
import com.music.bitchord.ui.screens.SettingsRow
import com.music.bitchord.ui.screens.SettingsSearchEmpty
import com.music.bitchord.ui.screens.SettingsSectionDivider
import com.music.bitchord.ui.screens.SettingsSubPageHeader
import com.music.bitchord.ui.screens.countOfMonths
import com.music.bitchord.ui.screens.matchesSearch
import kotlinx.coroutines.launch

/**
 * Dedicated sub-page for Replay insights, personal listening analytics,
 * and cloud-free JSON backup / restore in a single unified settings block.
 */
@Composable
fun YourDataSection(
    onOpenReplay: () -> Unit,
    onOpenDriveBackup: () -> Unit = {},
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    initialSearchQuery: String = "",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val backupScope = rememberCoroutineScope()

    val replayGenres by AppSettings.replayGenres.collectAsStateWithLifecycle()
    val dontRepeatSuggestions by AppSettings.dontRepeatSuggestions.collectAsStateWithLifecycle()

    var searchQuery by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }
    var listenerName by remember(context) { mutableStateOf(OnboardingState.name(context)) }
    val identityCardBitmap = remember(listenerName) {
        ChordPlayPassPng.renderPassBitmap(
            listenerName = listenerName,
            signature = WaveIdentitySignature.fromName(listenerName),
        ).asImageBitmap()
    }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var importStatus by remember { mutableStateOf<String?>(null) }
    var syncIdentityStatus by remember { mutableStateOf<String?>(null) }
    var restoreIdentityStatus by remember { mutableStateOf<String?>(null) }
    var confirmImport by remember { mutableStateOf(false) }

    val identityRestorePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { source ->
        if (source == null) return@rememberLauncherForActivityResult
        backupScope.launch {
            restoreIdentityStatus = ChordPlayIdentityStore.restoreFromUri(context, source).fold(
                onSuccess = { summary ->
                    listenerName = OnboardingState.name(context)
                    context.getString(R.string.onboarding_identity_restored, summary.name, summary.settings)
                },
                onFailure = {
                    it.message ?: context.getString(R.string.unknown_error)
                },
            )
        }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.your_data),
            onBack = onBack,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
        )

        val showReplay = matchesSearch(
            searchQuery,
            stringResource(R.string.replay),
            stringResource(R.string.replay_subtitle),
            "replay", "stats", "insights", "recap", "year",
        )
        val showWorkOutGenres = matchesSearch(
            searchQuery,
            stringResource(R.string.work_out_genres),
            stringResource(R.string.replay_genres_enabled_subtitle),
            stringResource(R.string.replay_genres_disabled_subtitle),
            "genres", "replay", "analytics",
        )
        val showDontRepeatSongs = matchesSearch(
            searchQuery,
            stringResource(R.string.dont_repeat_songs),
            stringResource(R.string.dont_repeat_songs_subtitle),
            "history", "repeat", "suggestions",
        )
        val showDriveBackup = matchesSearch(
            searchQuery,
            stringResource(R.string.drive_backup_title),
            stringResource(R.string.drive_backup_row_subtitle),
            "google", "drive", "backup", "cloud", "sync", "devices",
        )
        val showExport = matchesSearch(
            searchQuery,
            stringResource(R.string.export_data),
            stringResource(R.string.export_data_subtitle),
            "backup", "export", "json",
        )
        val showImport = matchesSearch(
            searchQuery,
            stringResource(R.string.import_data),
            stringResource(R.string.import_data_subtitle),
            "backup", "import", "restore", "json",
        )
        val showSyncIdentity = matchesSearch(
            searchQuery,
            stringResource(R.string.sync_identity_card),
            stringResource(R.string.sync_identity_card_subtitle),
            "identity", "wave", "profile", "you", "downloads", "chordplay",
        )

        val hasReplaySection = showReplay || showWorkOutGenres || showDontRepeatSongs
        val hasBackupSection = showDriveBackup || showExport || showImport
        val anyYourDataMatch = hasReplaySection || hasBackupSection || showSyncIdentity

        if (searchQuery.isNotBlank() && !anyYourDataMatch) {
            SettingsSearchEmpty(query = searchQuery)
        } else {
            // Visual ChordPlay Identity Pass card + direct Backup/Update & Restore controls
            if (showSyncIdentity) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Image(
                        bitmap = identityCardBitmap,
                        contentDescription = stringResource(R.string.drive_included_identity_header),
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1200f / 680f)
                            .clip(RoundedCornerShape(22.dp)),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                if (listenerName.isBlank()) {
                                    OnboardingState.requestReplay()
                                } else {
                                    backupScope.launch {
                                        syncIdentityStatus = ChordPlayIdentityStore.saveIdentity(
                                            context,
                                            listenerName,
                                        ).fold(
                                            onSuccess = { path ->
                                                context.getString(R.string.sync_identity_saved, path)
                                            },
                                            onFailure = {
                                                context.getString(
                                                    R.string.sync_identity_failed,
                                                    it.message ?: context.getString(R.string.unknown_error),
                                                )
                                            },
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.sync_identity_card), maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                identityRestorePicker.launch(arrayOf("image/png", "application/json", "*/*"))
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.drive_restore_action), maxLines = 1)
                        }
                    }
                    val cardStatus = syncIdentityStatus ?: restoreIdentityStatus
                    if (cardStatus != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = cardStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Single Unified Block for all user data settings
            SettingsGroup(
                header = stringResource(R.string.your_data),
                footer = stringResource(R.string.export_data_subtitle),
            ) {
                var replayPreceding = false
                if (showReplay) {
                    val replayTitle = stringResource(R.string.replay)
                    SettingsRow(
                        icon = Icons.Rounded.BarChart,
                        title = replayTitle,
                        subtitle = stringResource(R.string.replay_subtitle),
                        onClick = onOpenReplay,
                    )
                    replayPreceding = true
                }

                if (showWorkOutGenres) {
                    if (replayPreceding) RowDivider()
                    val workOutGenresTitle = stringResource(R.string.work_out_genres)
                    SettingsRow(
                        icon = Icons.Rounded.LocalOffer,
                        title = workOutGenresTitle,
                        subtitle = if (replayGenres) {
                            stringResource(R.string.replay_genres_enabled_subtitle)
                        } else {
                            stringResource(R.string.replay_genres_disabled_subtitle)
                        },
                        trailing = {
                            Switch(
                                checked = replayGenres,
                                onCheckedChange = AppSettings::setReplayGenres,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        },
                        onClick = { AppSettings.setReplayGenres(!replayGenres) },
                    )
                    replayPreceding = true
                }

                if (showDontRepeatSongs) {
                    if (replayPreceding) RowDivider()
                    val dontRepeatSongsTitle = stringResource(R.string.dont_repeat_songs)
                    SettingsRow(
                        icon = Icons.Rounded.History,
                        title = dontRepeatSongsTitle,
                        subtitle = stringResource(R.string.dont_repeat_songs_subtitle),
                        trailing = {
                            Switch(
                                checked = dontRepeatSuggestions,
                                onCheckedChange = AppSettings::setDontRepeatSuggestions,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        },
                        onClick = { AppSettings.setDontRepeatSuggestions(!dontRepeatSuggestions) },
                    )
                }

                // Backup & Restore
                if (hasBackupSection) {
                    if (hasReplaySection) {
                        SettingsSectionDivider(title = stringResource(R.string.backup_and_restore))
                    }

                    var backupPreceding = false
                    if (showDriveBackup) {
                        com.music.bitchord.ui.screens.DriveBackupSettingsRow(
                            title = stringResource(R.string.drive_backup_title),
                            onClick = onOpenDriveBackup,
                        )
                        backupPreceding = true
                    }

                    if (showExport) {
                        if (backupPreceding) RowDivider()
                        val exportTitle = stringResource(R.string.export_data)
                        SettingsRow(
                            icon = Icons.Rounded.FileUpload,
                            title = exportTitle,
                            subtitle = exportStatus ?: stringResource(R.string.export_data_subtitle),
                            onClick = { exportPicker.launch("chordplay-backup.json") },
                        )
                        backupPreceding = true
                    }

                    if (showImport) {
                        if (backupPreceding) RowDivider()
                        val importTitle = stringResource(R.string.import_data)
                        SettingsRow(
                            icon = Icons.Rounded.FileDownload,
                            title = importTitle,
                            subtitle = importStatus ?: stringResource(R.string.import_data_subtitle),
                            onClick = { confirmImport = true },
                        )
                        backupPreceding = true
                    }
                }
            }
        }
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text(stringResource(R.string.import_backup_title)) },
            text = { Text(stringResource(R.string.import_backup_warning)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmImport = false
                        importPicker.launch(arrayOf("application/json"))
                    },
                ) {
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
