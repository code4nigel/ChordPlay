package com.music.bitchord.ui.screens.settings.sections

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
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.stats.Backup
import com.music.bitchord.ui.screens.RowDivider
import com.music.bitchord.ui.screens.SettingsGroup
import com.music.bitchord.ui.screens.SettingsRow
import com.music.bitchord.ui.screens.SettingsSectionDivider
import com.music.bitchord.ui.screens.SettingsSubPageHeader
import com.music.bitchord.ui.screens.countOfMonths
import kotlinx.coroutines.launch

/**
 * Dedicated sub-page for Replay insights, personal listening analytics,
 * and cloud-free JSON backup / restore in a single unified settings block.
 */
@Composable
fun YourDataSection(
    onOpenReplay: () -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val backupScope = rememberCoroutineScope()

    val replayGenres by AppSettings.replayGenres.collectAsStateWithLifecycle()
    val dontRepeatSuggestions by AppSettings.dontRepeatSuggestions.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var importStatus by remember { mutableStateOf<String?>(null) }
    var confirmImport by remember { mutableStateOf(false) }

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

        // Single Unified Block for all user data settings
        SettingsGroup(
            header = stringResource(R.string.your_data),
            footer = stringResource(R.string.export_data_subtitle),
        ) {
            // Replay & Insights
            val replayTitle = stringResource(R.string.replay)
            SettingsRow(
                icon = Icons.Rounded.BarChart,
                title = replayTitle,
                subtitle = stringResource(R.string.replay_subtitle),
                onClick = onOpenReplay,
            )

            RowDivider()

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

            RowDivider()

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

            // Backup & Restore
            SettingsSectionDivider(title = "Backup & Restore")

            val exportTitle = stringResource(R.string.export_data)
            SettingsRow(
                icon = Icons.Rounded.FileUpload,
                title = exportTitle,
                subtitle = exportStatus ?: stringResource(R.string.export_data_subtitle),
                onClick = { exportPicker.launch("chordplay-backup.json") },
            )

            RowDivider()

            val importTitle = stringResource(R.string.import_data)
            SettingsRow(
                icon = Icons.Rounded.FileDownload,
                title = importTitle,
                subtitle = importStatus ?: stringResource(R.string.import_data_subtitle),
                onClick = { confirmImport = true },
            )
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
