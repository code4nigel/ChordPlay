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

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.music.bitchord.R
import com.music.bitchord.data.settings.NavPage
import com.music.bitchord.data.settings.OrderedToggle
import com.music.bitchord.ui.components.GlassAlert
import com.music.bitchord.ui.components.IosSwitch

@Composable
internal fun NavPage.label(): String = stringResource(
    when (this) {
        NavPage.HOME -> R.string.home
        NavPage.RADIO -> R.string.radio
        NavPage.LIBRARY -> R.string.library
        NavPage.SEARCH -> R.string.search
    },
)

/**
 * Dialog holding a [ReorderableToggleList] for ordering and toggling a short
 * list of items (such as Navigation bar tabs). Changes commit immediately.
 */
@Composable
internal fun <T : Any> ReorderDialog(
    title: String,
    note: String?,
    items: List<OrderedToggle<T>>,
    label: @Composable (T) -> String,
    onChange: (List<OrderedToggle<T>>) -> Unit,
    onDismiss: () -> Unit,
    canDisable: (List<OrderedToggle<T>>, T) -> Boolean = { _, _ -> true },
) {
    GlassAlert(title = title, onDismiss = onDismiss) {
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
                color = LocalContentColor.current.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        ReorderableToggleList(items, label, onChange, canDisable)
    }
}

@Composable
internal fun <T : Any> ReorderableToggleList(
    items: List<OrderedToggle<T>>,
    label: @Composable (T) -> String,
    onChange: (List<OrderedToggle<T>>) -> Unit,
    canDisable: (List<OrderedToggle<T>>, T) -> Boolean = { _, _ -> true },
) {
    var working by remember(items) { mutableStateOf(items) }
    var dragging by remember { mutableStateOf<T?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { REORDER_ROW_HEIGHT.toPx() }
    val haptics = LocalHapticFeedback.current
    val currentOnChange by rememberUpdatedState(onChange)

    Column(Modifier.fillMaxWidth()) {
        working.forEach { entry ->
            key(entry.item) {
                val isDragging = dragging == entry.item
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(REORDER_ROW_HEIGHT)
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffset else 0f
                            val lift = if (isDragging) 1.03f else 1f
                            scaleX = lift
                            scaleY = lift
                        },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DragHandle,
                        contentDescription = stringResource(R.string.drag_to_reorder),
                        tint = LocalContentColor.current.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(40.dp)
                            .padding(8.dp)
                            .pointerInput(entry.item) {
                                detectDragGestures(
                                    onDragStart = {
                                        dragging = entry.item
                                        dragOffset = 0f
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                        val list = working
                                        val from = list.indexOfFirst { it.item == entry.item }
                                        val to = when {
                                            dragOffset > rowHeightPx / 2 && from < list.lastIndex -> from + 1
                                            dragOffset < -rowHeightPx / 2 && from > 0 -> from - 1
                                            else -> from
                                        }
                                        if (to != from) {
                                            working = list.toMutableList().apply { add(to, removeAt(from)) }
                                            dragOffset += if (to > from) -rowHeightPx else rowHeightPx
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    },
                                    onDragEnd = {
                                        dragging = null
                                        dragOffset = 0f
                                        currentOnChange(working)
                                    },
                                    onDragCancel = {
                                        dragging = null
                                        dragOffset = 0f
                                        currentOnChange(working)
                                    },
                                )
                            },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = label(entry.item),
                        style = MaterialTheme.typography.bodyLarge,
                        color = LocalContentColor.current,
                        modifier = Modifier.weight(1f),
                    )
                    val allowed = !entry.enabled || canDisable(working, entry.item)
                    IosSwitch(
                        checked = entry.enabled,
                        enabled = allowed,
                        onCheckedChange = { on ->
                            working = working.map { if (it.item == entry.item) it.copy(enabled = on) else it }
                            currentOnChange(working)
                        },
                    )
                }
            }
        }
    }
}

private val REORDER_ROW_HEIGHT = 56.dp
