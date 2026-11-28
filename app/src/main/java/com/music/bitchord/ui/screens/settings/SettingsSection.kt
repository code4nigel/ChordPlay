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
package com.music.bitchord.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.ui.graphics.vector.ImageVector
import com.music.bitchord.R
import com.music.bitchord.ui.icons.BitChordIcons

/**
 * The modular categories of ChordPlay's settings hierarchy.
 */
enum class SettingsSection(
    val titleRes: Int,
    val icon: ImageVector,
) {
    APPEARANCE(R.string.appearance, BitChordIcons.Appearance),
    COVER(R.string.settings_cover, Icons.Rounded.Image),
    MINIPLAYER(R.string.settings_miniplayer, Icons.Rounded.Waves),
    PLAYER_STYLE(R.string.settings_player_style, Icons.Rounded.GraphicEq),
    SHELVES(R.string.your_preferences, Icons.Rounded.ViewAgenda),
    PLAYER(R.string.playback, Icons.Rounded.SmartDisplay),
    LYRICS(R.string.synced_lyrics, Icons.AutoMirrored.Rounded.Notes),
    AUDIO(R.string.audio_quality, Icons.Rounded.GraphicEq),
    DOWNLOADS(R.string.downloads, Icons.Rounded.Download),
    LOCAL_MUSIC(R.string.local_music, Icons.Rounded.LibraryMusic),
    STORAGE(R.string.storage, Icons.Rounded.Storage),
    YOUR_DATA(R.string.your_data, Icons.Rounded.BarChart),
    DRIVE_BACKUP(R.string.drive_backup_title, Icons.Rounded.CloudQueue),
    DRIVE_DEVICES(R.string.drive_devices_title, Icons.Rounded.CloudQueue),
    ADVANCED_PERFORMANCE(R.string.advanced_and_performance, Icons.Rounded.Speed),
    SUPPORT(R.string.support_community, Icons.Rounded.Favorite),
    LANGUAGE(R.string.language_and_location, Icons.Rounded.Language),
    INTEGRATIONS(R.string.account_integrations, Icons.Rounded.Person),
    ABOUT(R.string.miscellaneous, Icons.AutoMirrored.Rounded.HelpOutline);

    fun parentSection(): SettingsSection? = when (this) {
        COVER, MINIPLAYER, PLAYER_STYLE -> APPEARANCE
        DRIVE_DEVICES -> DRIVE_BACKUP
        DRIVE_BACKUP -> YOUR_DATA
        else -> null
    }
}
