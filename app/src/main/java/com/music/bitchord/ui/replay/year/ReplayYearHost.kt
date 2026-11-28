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
package com.music.bitchord.ui.replay.year

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * The year story and its share sheet, as the app mounts them over everything.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplayYearHost(year: ReplayYear?, open: Boolean, onClose: () -> Unit) {
    var sharing by rememberSaveable { mutableStateOf<YearSlide?>(null) }
    year ?: return

    BackHandler(enabled = open && sharing == null, onBack = onClose)
    BackHandler(enabled = sharing != null) { sharing = null }

    if (open) {
        ReplayYearStories(
            year = year,
            onClose = onClose,
            onShare = { slide -> sharing = slide },
            paused = sharing != null,
        )
    }
    sharing?.let { slide ->
        ModalBottomSheet(
            onDismissRequest = { sharing = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            ReplayYearShareSheet(year = year, slide = slide, onDismiss = { sharing = null })
        }
    }
}
