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
package com.music.bitchord.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.R
import com.music.bitchord.data.ContentCountry
import com.music.bitchord.data.innertube.Innertube
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.GlassAlert
import java.text.Collator
import java.util.Locale

/** The locale country names are shown in: the app language, else the device locale. */
internal fun countryDisplayLocale(): Locale =
    AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()

/**
 * Searchable country picker in Settings > Language & location using [GlassAlert].
 */
@Composable
internal fun ContentCountryDialog(
    title: String,
    selected: String,
    onDismiss: () -> Unit,
) {
    val displayLocale = remember { countryDisplayLocale() }
    val countries = remember(displayLocale) {
        val collator = Collator.getInstance(displayLocale)
        ContentCountry.CODES
            .map { it to ContentCountry.displayName(it, displayLocale) }
            .sortedWith { a, b -> collator.compare(a.second, b.second) }
    }
    var query by remember { mutableStateOf("") }
    val shown = remember(query, countries) {
        val needle = query.trim()
        if (needle.isEmpty()) {
            countries
        } else {
            countries.filter { (code, name) ->
                name.contains(needle, ignoreCase = true) ||
                    code.equals(needle, ignoreCase = true) ||
                    ContentCountry.displayName(code, Locale.ENGLISH).contains(needle, ignoreCase = true)
            }
        }
    }
    val choose: (String) -> Unit = { code ->
        if (code != selected) AppSettings.setContentCountry(code)
        onDismiss()
    }

    GlassAlert(title = title, onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.content_country_description),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
            color = LocalContentColor.current.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
        )
        CountrySearchField(query = query, onQueryChange = { query = it })
        if (query.isBlank()) {
            CountryRow(
                label = stringResource(R.string.content_country_automatic),
                detail = ContentCountry.displayName(Innertube.automaticRegion, displayLocale),
                selected = selected.isBlank(),
                onClick = { choose("") },
            )
        }
        shown.forEach { (code, name) ->
            CountryRow(
                label = name,
                detail = code,
                selected = selected.equals(code, ignoreCase = true),
                onClick = { choose(code) },
            )
        }
        if (shown.isEmpty()) {
            Text(
                text = stringResource(R.string.no_countries_match),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = LocalContentColor.current.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
            )
        }
    }
}

@Composable
private fun CountrySearchField(query: String, onQueryChange: (String) -> Unit) {
    val ink = LocalContentColor.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ink.copy(alpha = 0.08f))
            .heightIn(min = 40.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = ink.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_countries),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    color = ink.copy(alpha = 0.5f),
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, color = ink),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CountryRow(label: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    val ink = LocalContentColor.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (pressed) ink.copy(alpha = 0.09f) else Color.Transparent)
            .clickable(indication = null, interactionSource = interactionSource, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 15.sp),
                color = ink.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}
