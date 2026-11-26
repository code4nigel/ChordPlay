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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.music.bitchord.R
import com.music.bitchord.ui.components.SearchField
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

internal val SETTINGS_SEARCH_SLOT_HEIGHT = 44.dp

/**
 * What Settings' one search field needs to know to sit in the active page at rest
 * and dock beside the back button as the page scrolls.
 *
 * The field itself lives in the top bar, so there is only ever one of it.
 * Each Settings page keeps an empty space of the field's size under its title
 * and reports where that space is; the bar reports where the field docks.
 */
@Stable
class SettingsSearchDockState {
    /** The page's space for the field, in root coordinates, as at scroll 0. */
    var restBounds by mutableStateOf<Rect?>(null)

    /** Where the field docks in the bar, in root coordinates. */
    var slotBounds by mutableStateOf<Rect?>(null)

    /** How far the page has scrolled. Read only while laying out, so scroll relays out without recomposing. */
    var scroll: () -> Float by mutableStateOf({ 0f })

    /** The scroll at which the field reaches the bar's row, or null before both have been laid out. */
    val dockScroll: Float?
        get() {
            val rest = restBounds ?: return null
            val slot = slotBounds ?: return null
            return (rest.center.y - slot.center.y).coerceAtLeast(0f)
        }
}

@Stable
class SettingsSearchHostState {
    val dock = SettingsSearchDockState()
    var query by mutableStateOf("")
    var onQueryChange: (String) -> Unit by mutableStateOf({})
}

internal val LocalSettingsSearchHost = compositionLocalOf<SettingsSearchHostState?> { null }
internal val LocalSettingsSearchPageActive = compositionLocalOf { true }

/**
 * Reserves the resting slot under the page heading for [SettingsDockingSearchField]
 * and binds the active page's scroll and query state to [searchHost].
 */
@Composable
internal fun SettingsSearchSlot(
    searchHost: SettingsSearchHostState?,
    isActivePage: Boolean,
    scrollState: ScrollState,
    query: String,
    onQueryChange: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (isActivePage && searchHost != null) {
        searchHost.query = query
        searchHost.onQueryChange = onQueryChange ?: {}
        searchHost.dock.scroll = { scrollState.value.toFloat() }
    }
    LaunchedEffect(query, isActivePage, searchHost) {
        if (!isActivePage || searchHost == null) return@LaunchedEffect
        val dockAt = searchHost.dock.dockScroll ?: return@LaunchedEffect
        val current = scrollState.value.toFloat()
        if (current > dockAt) {
            snapshotFlow { scrollState.maxValue }.first()
            scrollState.scrollTo(
                dockAt.roundToInt().coerceAtMost(scrollState.maxValue),
            )
        }
    }
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(SETTINGS_SEARCH_SLOT_HEIGHT)
            .onGloballyPositioned { coords ->
                if (!isActivePage || searchHost == null) return@onGloballyPositioned
                val pos = coords.positionInRoot()
                val s = scrollState.value.toFloat()
                searchHost.dock.restBounds = Rect(
                    left = pos.x,
                    top = pos.y + s,
                    right = pos.x + coords.size.width,
                    bottom = pos.y + s + coords.size.height,
                )
            },
    )
}

/**
 * How far Settings' search field has shrunk toward its place in the top bar:
 * 0 at rest under the title, 1 once it has risen to the bar's row.
 */
internal fun settingsSearchDockProgress(scroll: Float, dockScroll: Float): Float {
    if (dockScroll <= 0f) return if (scroll >= dockScroll) 1f else 0f
    return (scroll / dockScroll).coerceIn(0f, 1f)
}

/**
 * Where one of the field's edges sits at [progress], between where it rests
 * in the page and where the bar's slot has it.
 */
internal fun settingsSearchDockEdge(rest: Float, slot: Float, progress: Float): Float =
    rest + (slot - rest) * progress.coerceIn(0f, 1f)

/**
 * Settings' search field, composed in the bar's slot but drawn wherever the
 * active page's space for it is. It rises with the page; as it does, its left
 * edge draws in to clear the back button and it stays docked in the top bar.
 */
@Composable
fun SettingsDockingSearchField(
    state: SettingsSearchDockState,
    query: String,
    onQueryChange: (String) -> Unit,
    surface: Modifier,
    modifier: Modifier = Modifier,
    shown: () -> Float = { 1f },
) {
    SearchField(
        query = query,
        onQueryChange = onQueryChange,
        onSubmit = {},
        placeholder = stringResource(R.string.settings_search_hint),
        containerColor = Color.Transparent,
        shape = CircleShape,
        height = SETTINGS_SEARCH_SLOT_HEIGHT,
        modifier = modifier
            .layout { measurable, constraints ->
                val slotWidth = constraints.maxWidth
                val rest = state.restBounds
                val slot = state.slotBounds
                val dockScroll = state.dockScroll
                if (rest == null || slot == null || dockScroll == null) {
                    val placeable = measurable.measure(constraints)
                    return@layout layout(slotWidth, placeable.height) {}
                }
                val scroll = state.scroll()
                val progress = settingsSearchDockProgress(scroll, dockScroll)
                val left = settingsSearchDockEdge(rest.left, slot.left, progress)
                val right = settingsSearchDockEdge(rest.right, slot.right, progress)
                val width = (right - left).roundToInt().coerceAtLeast(0)
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = width,
                        maxWidth = width,
                        minHeight = constraints.minHeight,
                        maxHeight = constraints.maxHeight,
                    ),
                )
                val drop = (dockScroll - scroll).coerceAtLeast(0f)
                layout(slotWidth, placeable.height) {
                    placeable.place((left - slot.left).roundToInt(), drop.roundToInt())
                }
            }
            .graphicsLayer {
                val s = shown().coerceIn(0f, 1f)
                scaleX = s
                scaleY = s
                alpha = s
            }
            .then(surface),
    )
}
