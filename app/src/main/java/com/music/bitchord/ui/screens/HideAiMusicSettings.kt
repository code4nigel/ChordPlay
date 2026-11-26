/*
 * Adapted from Dial (https://github.com/steeben/Dial) by steeben.
 * Copyright (C) 2026 Shivanshu (code4nigel) <https://github.com/code4nigel/ChordPlay>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.music.bitchord.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.IosSwitch

/**
 * Settings, Audio: the "Hide AI music" switch (ported from Dial Miscellaneous).
 * Off by default; while enabled, lists the community blocklists beneath it.
 */
@Composable
internal fun HideAiMusicRow(title: String) {
    val hideAiMusic by AppSettings.hideAiMusic.collectAsStateWithLifecycle()
    Column {
        SettingsRow(
            icon = Icons.Rounded.SmartToy,
            title = title,
            subtitle = stringResource(R.string.hide_ai_music_subtitle),
            trailing = {
                IosSwitch(
                    checked = hideAiMusic,
                    onCheckedChange = AppSettings::setHideAiMusic,
                )
            },
            onClick = { AppSettings.setHideAiMusic(!hideAiMusic) },
        )
        if (hideAiMusic) {
            Text(
                text = stringResource(R.string.hide_ai_music_sources),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = TEXT_INSET, end = ROW_INSET, bottom = 14.dp),
            )
        }
    }
}
