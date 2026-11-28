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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.model.PlaylistPrivacy
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.playback.QueueHistoryRange
import com.music.bitchord.ui.QueuePlaylistSaveState
import com.music.bitchord.ui.QueuePlaylistSaveState.Status
import java.util.Locale

@Composable
internal fun QueuePlaylistSaveDialog(
    state: QueuePlaylistSaveState,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    onCreate: (String, PlaylistPrivacy, QueueHistoryRange?, Boolean) -> Unit,
    onVerify: () -> Unit,
    onFinish: () -> Unit,
    onOpen: (String, String) -> Unit,
) {
    val title = stringResource(R.string.queue_playlist_title)
    when (state) {
        QueuePlaylistSaveState.SignIn -> FrostedQueueDialog(
            title = title,
            onDismiss = onDismiss,
            body = {
                Text(
                    text = stringResource(R.string.queue_playlist_sign_in),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            },
            actions = {
                AlertAction(label = stringResource(R.string.sign_in), emphasised = true, onClick = onSignIn)
                AlertRule()
                AlertAction(label = stringResource(R.string.cancel), emphasised = false, onClick = onDismiss)
            },
        )
        is QueuePlaylistSaveState.Form -> {
            var history by remember(state.capture) { mutableStateOf<QueueHistoryRange?>(null) }
            var review by remember(state.capture) { mutableStateOf<Pair<String, PlaylistPrivacy>?>(null) }
            val snapshot = remember(state.capture, history) { state.capture.select(history) }
            val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
            val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
            val glassSupported = isGlassSupported() && !reduceDynamicBlur
            val formShape = RoundedCornerShape(24.dp)

            Dialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .clip(formShape)
                        .then(
                            if (liquidGlass && glassSupported) {
                                Modifier.lightweightLiquidGlass(
                                    shape = formShape,
                                    fallbackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                )
                            } else {
                                Modifier
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                                    .border(GLASS_EDGE_WIDTH, Color.White.copy(alpha = 0.14f), formShape)
                            },
                        ),
                ) {
                    NewPlaylistForm(
                        onBack = null,
                        onCreate = { name, privacy ->
                            if (snapshot.skipped.isEmpty() && snapshot.upload.videoIds.isNotEmpty()) {
                                onCreate(name, privacy, history, false)
                            } else {
                                review = name to privacy
                            }
                        },
                        onDismiss = onDismiss,
                        title = title,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        additionalOptions = {
                            QueueHistoryOptions(state.capture.historySize, state.capture.historyCount(history), history) {
                                history = it
                            }
                        },
                    )
                }
            }
            review?.let { (name, privacy) ->
                val available = snapshot.upload.videoIds.isNotEmpty()
                val message = if (available) R.string.queue_playlist_skip else R.string.queue_playlist_none_available
                val missing = snapshot.skipped.joinToString("\n") { "• ${it.title} · ${it.artist}" }
                FrostedQueueDialog(
                    title = title,
                    onDismiss = { review = null },
                    body = {
                        Text(
                            text = stringResource(message) + if (missing.isEmpty()) "" else "\n\n$missing",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                        )
                    },
                    actions = {
                        if (available) {
                            AlertAction(
                                label = stringResource(R.string.queue_playlist_save_available),
                                emphasised = true,
                                onClick = {
                                    review = null
                                    onCreate(name, privacy, history, true)
                                },
                            )
                            AlertRule()
                            AlertAction(
                                label = stringResource(R.string.cancel),
                                emphasised = false,
                                onClick = { review = null },
                            )
                        } else {
                            AlertAction(
                                label = stringResource(R.string.close),
                                emphasised = true,
                                onClick = { review = null },
                            )
                        }
                    },
                )
            }
        }
        QueuePlaylistSaveState.Creating -> QueueSaveProgress(title, R.string.queue_playlist_creating)
        QueuePlaylistSaveState.CreateUnconfirmed, QueuePlaylistSaveState.ProfileChanged -> FrostedQueueDialog(
            title = title,
            onDismiss = onDismiss,
            body = {
                Text(
                    text = stringResource(
                        if (state == QueuePlaylistSaveState.ProfileChanged) {
                            R.string.queue_playlist_account_changed
                        } else {
                            R.string.queue_playlist_creation_unconfirmed
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            },
            actions = {
                AlertAction(label = stringResource(R.string.close), emphasised = true, onClick = onDismiss)
            },
        )
        is QueuePlaylistSaveState.Receipt -> {
            if (state.status == Status.VERIFYING) {
                QueueSaveProgress(state.pending.title, R.string.queue_playlist_verifying)
                return
            }
            val message = when (state.status) {
                Status.VERIFIED -> R.string.queue_playlist_saved
                Status.MISMATCH -> R.string.queue_playlist_mismatch
                Status.MIX_SAVE_FAILED -> R.string.queue_playlist_mix_failed
                else -> R.string.queue_playlist_verify_failed
            }
            FrostedQueueDialog(
                title = state.pending.title,
                onDismiss = onDismiss,
                body = {
                    Text(
                        text = stringResource(message),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                },
                actions = {
                    if (state.status == Status.VERIFIED) {
                        AlertAction(
                            label = stringResource(R.string.open_playlist),
                            emphasised = true,
                            onClick = { onOpen(state.pending.browseId, state.pending.title) },
                        )
                    } else {
                        AlertAction(
                            label = stringResource(R.string.queue_playlist_retry_verify),
                            emphasised = true,
                            onClick = onVerify,
                        )
                    }
                    AlertRule()
                    AlertAction(label = stringResource(R.string.done), emphasised = false, onClick = onFinish)
                },
            )
        }
    }
}

@Composable
private fun QueueSaveProgress(title: String, message: Int) {
    FrostedQueueDialog(
        title = title,
        onDismiss = {},
        dismissable = false,
        body = {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(message),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        },
        actions = null,
    )
}

@Composable
private fun FrostedQueueDialog(
    title: String,
    onDismiss: () -> Unit,
    dismissable: Boolean = true,
    body: @Composable ColumnScope.() -> Unit,
    actions: (@Composable ColumnScope.() -> Unit)?,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val glassSupported = isGlassSupported() && !reduceDynamicBlur
    val shape = RoundedCornerShape(18.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = dismissable,
            dismissOnClickOutside = dismissable,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .then(
                        if (liquidGlass && glassSupported) {
                            Modifier.lightweightLiquidGlass(
                                shape = shape,
                                fallbackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            )
                        } else {
                            Modifier
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                                .border(GLASS_EDGE_WIDTH, Color.White.copy(alpha = 0.14f), shape)
                        },
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.W600,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    body()
                }
                if (actions != null) {
                    AlertRule()
                    actions()
                }
            }
        }
    }
}

@Composable
private fun QueueHistoryOptions(
    historySize: Int,
    included: Int,
    selected: QueueHistoryRange?,
    onSelect: (QueueHistoryRange?) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.queue_history_title).uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.queue_history_count, included, historySize),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QueueHistoryRange.entries.forEach { option ->
                HistoryOptionPill(
                    label = if (option == QueueHistoryRange.ALL) {
                        stringResource(R.string.queue_history_all)
                    } else {
                        stringResource(R.string.queue_history_last, option.limit)
                    },
                    selected = selected == option,
                    enabled = historySize > 0,
                    onClick = { onSelect(option.takeUnless { selected == it }) },
                )
            }
        }
    }
}

@Composable
private fun HistoryOptionPill(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val glassSupported = isGlassSupported() && !reduceDynamicBlur
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(MaterialTheme.colorScheme.primary)
                } else if (liquidGlass && glassSupported) {
                    Modifier.lightweightLiquidGlass(
                        shape = CircleShape,
                        fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    )
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
