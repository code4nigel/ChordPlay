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
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.LocalMediaRepository
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.screens.RowDivider
import com.music.bitchord.ui.screens.SettingsGroup
import com.music.bitchord.ui.screens.SettingsRow
import com.music.bitchord.ui.screens.SettingsSearchEmpty
import com.music.bitchord.ui.components.IosSwitch
import com.music.bitchord.ui.screens.SettingsSubPageHeader
import com.music.bitchord.ui.screens.matchesSearch

@Composable
internal fun LocalMusicSection(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    initialSearchQuery: String,
) {
    val context = LocalContext.current
    val folder by AppSettings.localMusicFolderUri.collectAsStateWithLifecycle()
    val filter by AppSettings.filterNonMusicAudio.collectAsStateWithLifecycle()
    var query by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            if (LocalMediaRepository.selectedFolderLabel(uri.toString()) != null) {
                AppSettings.setLocalMusicFolderUri(uri.toString())
            } else {
                Toast.makeText(context, R.string.no_local_audio_found, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val folderTitle = stringResource(R.string.local_music_folder)
    val folderLabel = LocalMediaRepository.selectedFolderLabel(folder) ?: stringResource(R.string.all_audio_folders)
    val allTitle = stringResource(R.string.use_all_audio_folders)
    val allSubtitle = stringResource(R.string.use_all_audio_folders_subtitle)
    val filterTitle = stringResource(R.string.filter_non_music_audio)
    val filterSubtitle = stringResource(R.string.filter_non_music_audio_subtitle)
    val showFolder = matchesSearch(query, folderTitle, folderLabel, "device audio")
    val showAll = matchesSearch(query, allTitle, allSubtitle)
    val showFilter = matchesSearch(query, filterTitle, filterSubtitle)
    val scrollState = rememberScrollState()
    Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(contentPadding)) {
        SettingsSubPageHeader(
            title = stringResource(R.string.local_music),
            onBack = onBack,
            searchQuery = query,
            onSearchQueryChange = { query = it },
            scrollState = scrollState,
        )
        if (!showFolder && !showAll && !showFilter) {
            SettingsSearchEmpty(query)
        } else {
            SettingsGroup {
                if (showFolder) {
                    SettingsRow(
                        icon = Icons.Rounded.Folder,
                        title = folderTitle,
                        subtitle = folderLabel,
                        onClick = { picker.launch(null) },
                    )
                }
                if (showAll) {
                    if (showFolder) RowDivider()
                    SettingsRow(
                        icon = Icons.Rounded.LibraryMusic,
                        title = allTitle,
                        subtitle = allSubtitle,
                        onClick = { AppSettings.setLocalMusicFolderUri("") },
                    )
                }
                if (showFilter) {
                    if (showFolder || showAll) RowDivider()
                    SettingsRow(
                        icon = Icons.Rounded.FilterAlt,
                        title = filterTitle,
                        subtitle = filterSubtitle,
                        trailing = {
                            IosSwitch(
                                checked = filter,
                                onCheckedChange = AppSettings::setFilterNonMusicAudio,
                                modifier = Modifier.semantics { contentDescription = filterTitle },
                            )
                        },
                        onClick = { AppSettings.setFilterNonMusicAudio(!filter) },
                    )
                }
            }
        }
    }
}
