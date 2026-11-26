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
package com.music.bitchord.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

private class GlassAlertEntry {
    var title by mutableStateOf("")
    var onDismiss by mutableStateOf({})
    var footerActions by mutableStateOf<(@Composable ColumnScope.() -> Unit)?>(null)
    var content by mutableStateOf<@Composable ColumnScope.() -> Unit>({})
}

private var shownGlassAlert by mutableStateOf<GlassAlertEntry?>(null)

private val GLASS_ALERT_WIDTH = 300.dp
private val GLASS_ALERT_BODY_MAX_HEIGHT = 460.dp

/**
 * Frosted glass options alert hosted at activity root via [GlassAlertHost]
 * so it blurs the real app backdrop behind it.
 */
@Composable
fun GlassAlert(
    title: String,
    onDismiss: () -> Unit,
    footerActions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val entry = remember { GlassAlertEntry() }
    SideEffect {
        entry.title = title
        entry.onDismiss = onDismiss
        entry.footerActions = footerActions
        entry.content = content
    }
    DisposableEffect(entry) {
        shownGlassAlert = entry
        onDispose { if (shownGlassAlert === entry) shownGlassAlert = null }
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassAlertHost(hazeState: HazeState) {
    val entry = shownGlassAlert ?: return
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val useLiquidGlass = LocalLiquidGlassEnabled.current && isGlassSupported()
    BackHandler { entry.onDismiss() }
    val shape = RoundedCornerShape(ALERT_CORNER)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SCRIM_COLOR)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { entry.onDismiss() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val contentColor = MaterialTheme.colorScheme.onSurface
        Column(
            modifier = Modifier
                .width(GLASS_ALERT_WIDTH)
                .clip(shape)
                .then(
                    when {
                        reduceDynamicBlur -> Modifier.background(MaterialTheme.colorScheme.surface)
                        useLiquidGlass -> Modifier.liquidGlass(shape = shape)
                        else -> Modifier.optimizedHazeEffect(
                            state = hazeState,
                            style = HazeMaterials.regular(MaterialTheme.colorScheme.surface),
                        )
                    },
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = GLASS_ALERT_BODY_MAX_HEIGHT)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 19.dp),
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.W600),
                        color = contentColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    )
                    entry.content(this)
                }
            }
            AlertRule()
            val customFooter = entry.footerActions
            if (customFooter != null) {
                customFooter(this)
            } else {
                AlertAction(stringResource(R.string.done), emphasised = true, onClick = { entry.onDismiss() })
            }
        }
    }
}
