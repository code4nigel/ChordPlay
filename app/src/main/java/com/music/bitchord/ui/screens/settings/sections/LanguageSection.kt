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

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Public
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.ContentCountry
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.languageDisplayNameRes
import com.music.bitchord.ui.screens.ContentCountryDialog
import com.music.bitchord.ui.screens.RowDivider
import com.music.bitchord.ui.screens.SettingsGroup
import com.music.bitchord.ui.screens.SettingsRow
import com.music.bitchord.ui.screens.SettingsSearchEmpty
import com.music.bitchord.ui.screens.SettingsSubPageHeader
import com.music.bitchord.ui.screens.countryDisplayLocale
import com.music.bitchord.ui.screens.matchesSearch
import java.util.Locale

/**
 * Dial-style Language & location sub-page offering both App language and Content Country.
 */
@Composable
fun LanguageSection(
    onBack: () -> Unit,
    onAppLanguage: () -> Unit,
    contentPadding: PaddingValues,
    initialSearchQuery: String = "",
    modifier: Modifier = Modifier,
) {
    val contentCountry by AppSettings.contentCountry.collectAsStateWithLifecycle()
    val selectedLanguage = AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag()
        ?: Locale.getDefault().toLanguageTag()
    val appLanguageLabel = stringResource(languageDisplayNameRes(selectedLanguage))

    val countryLabel = contentCountry.trim()
        .takeIf { it.length == 2 }
        ?.let { ContentCountry.displayName(it, countryDisplayLocale()) }
        ?: stringResource(R.string.content_country_automatic)

    var showCountryDialog by remember { mutableStateOf(false) }
    var searchQuery by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }
    val scrollState = rememberScrollState()

    val showAppLanguage = matchesSearch(
        searchQuery,
        stringResource(R.string.app_language),
        appLanguageLabel,
        "language", "locale", "translate",
    )
    val showContentCountry = matchesSearch(
        searchQuery,
        stringResource(R.string.content_country),
        stringResource(R.string.content_country_subtitle),
        countryLabel,
        "country", "location", "region", "recommendations", "charts",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.language_and_location),
            onBack = onBack,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            scrollState = scrollState,
        )

        if (searchQuery.isNotBlank() && !showAppLanguage && !showContentCountry) {
            SettingsSearchEmpty(query = searchQuery)
        } else {
            SettingsGroup(
                header = stringResource(R.string.language_and_location),
                topSpacing = 12.dp,
            ) {
                if (showAppLanguage) {
                    SettingsRow(
                        icon = Icons.Rounded.Language,
                        title = stringResource(R.string.app_language),
                        subtitle = appLanguageLabel,
                        onClick = onAppLanguage,
                    )
                }
                if (showContentCountry) {
                    if (showAppLanguage) RowDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Public,
                        title = stringResource(R.string.content_country),
                        subtitle = stringResource(R.string.content_country_subtitle),
                        value = countryLabel,
                        onClick = { showCountryDialog = true },
                    )
                }
            }
        }
    }

    if (showCountryDialog) {
        ContentCountryDialog(
            title = stringResource(R.string.content_country),
            selected = contentCountry,
            onDismiss = { showCountryDialog = false },
        )
    }
}
