/*
 * Copyright (C) 2024-2026 Kushagra Singh and BitChord contributors <https://github.com/kushagrasinghx/BitChord>
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

import android.app.Activity
import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.music.bitchord.ui.components.IosSwitch
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.music.bitchord.R
import com.music.bitchord.data.backup.BackupFrequency
import com.music.bitchord.data.backup.DriveAuth
import com.music.bitchord.data.backup.DriveBackup
import com.music.bitchord.data.backup.RemoteDevice
import com.music.bitchord.data.backup.ReplaySync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun DriveBackupSettingsRow(title: String, onClick: () -> Unit) {
    val settings by DriveBackup.settings.collectAsStateWithLifecycle()
    SettingsRow(
        icon = Icons.Rounded.CloudQueue,
        title = title,
        subtitle = stringResource(R.string.drive_backup_row_subtitle),
        value = when {
            settings.account == null -> stringResource(R.string.off)
            settings.needsSignIn -> stringResource(R.string.drive_sign_in_again)
            settings.needsDevice -> stringResource(R.string.drive_device_not_set)
            else -> settings.frequency.label()
        },
        onClick = onClick,
    )
}

@Composable
fun DriveBackupScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenDevices: () -> Unit,
    onConfirmDisconnect: () -> Unit,
    onConfirmRemove: (RemoteDevice) -> Unit,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by DriveBackup.settings.collectAsStateWithLifecycle()
    val progress by DriveBackup.progress.collectAsStateWithLifecycle()
    val devices by DriveBackup.devices.collectAsStateWithLifecycle()
    val result by DriveBackup.result.collectAsStateWithLifecycle()
    val available = remember { DriveAuth.available(context) }

    var connectError by remember { mutableStateOf<String?>(null) }
    var afterSignIn by remember { mutableStateOf<(() -> Unit)?>(null) }

    val afterConnect: () -> Unit = {
        if (DriveBackup.settings.value.needsDevice) onOpenDevices()
    }

    val consent = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { answer ->
        val then = afterSignIn
        afterSignIn = null
        if (answer.resultCode != Activity.RESULT_OK && answer.data == null) {
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                DriveBackup.finishSignIn(context, answer.data)
                then?.invoke()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.statusCode != CommonStatusCodes.CANCELED) {
                    connectError = e.message ?: context.getString(R.string.unknown_error)
                }
            } catch (e: Exception) {
                connectError = e.message ?: context.getString(R.string.unknown_error)
            }
        }
    }

    fun signIn(then: () -> Unit) {
        connectError = null
        scope.launch {
            try {
                when (val outcome = DriveBackup.beginSignIn(context)) {
                    is DriveAuth.Outcome.Granted -> {
                        DriveBackup.signedIn(context, outcome.accessToken)
                        then()
                    }
                    is DriveAuth.Outcome.NeedsConsent -> {
                        afterSignIn = then
                        consent.launch(IntentSenderRequest.Builder(outcome.intent.intentSender).build())
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                connectError = e.message ?: context.getString(R.string.unknown_error)
            }
        }
    }

    LaunchedEffect(settings.account) {
        if (settings.account != null && !settings.needsSignIn) DriveBackup.refreshDevices(context)
    }

    val running = progress != DriveBackup.Progress.Idle
    val connected = settings.account != null
    val ready = connected && !settings.needsDevice

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.drive_backup_title),
            onBack = onBack,
            scrollState = scrollState,
        )

        if (!available) {
            SettingsGroup(topSpacing = 0.dp) {
                SettingsRow(icon = Icons.Rounded.CloudOff, title = stringResource(R.string.drive_backup_unavailable))
            }
            return@Column
        }

        // ---- Last sync & progress ----
        SettingsGroup(header = stringResource(R.string.drive_sync_status_header)) {
            SettingsRow(
                icon = if (settings.lastSyncAt > 0 && settings.lastError == null) {
                    Icons.Rounded.CloudDone
                } else {
                    Icons.Rounded.CloudQueue
                },
                title = stringResource(R.string.drive_sync_last),
                value = if (settings.lastSyncAt > 0) {
                    formatWhen(context, settings.lastSyncAt)
                } else {
                    stringResource(R.string.drive_backup_never)
                },
                subtitle = if (running) null else when (val r = result) {
                    is DriveBackup.Result.Restored -> stringResource(
                        R.string.drive_restore_as_done,
                        r.device,
                        context.countOfMonths(r.summary.months),
                    )
                    is DriveBackup.Result.Failed -> stringResource(
                        if (r.restoring) R.string.drive_restore_failed else R.string.drive_sync_failed,
                        r.message,
                    )
                    is DriveBackup.Result.Synced -> stringResource(R.string.drive_sync_done)
                    null -> settings.lastError?.let { stringResource(R.string.drive_sync_failed, it) }
                },
            )
            if (running) {
                RowDivider()
                ProgressBlock(progress)
            }
        }

        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
            Button(
                onClick = {
                    DriveBackup.clearResult()
                    if (settings.needsSignIn) {
                        signIn { DriveBackup.syncNow(context) }
                    } else {
                        DriveBackup.syncNow(context)
                    }
                },
                enabled = ready && !running,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    text = stringResource(R.string.drive_sync_now),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // ---- Account ----
        SettingsGroup(
            header = stringResource(R.string.drive_account_header),
            footer = connectError?.let { stringResource(R.string.drive_connect_failed, it) },
        ) {
            val account = settings.account
            when {
                account == null -> SettingsRow(
                    icon = Icons.Rounded.Person,
                    title = stringResource(R.string.drive_connect),
                    subtitle = stringResource(R.string.drive_connect_subtitle),
                    onClick = { signIn(afterConnect) },
                )
                settings.needsSignIn -> SettingsRow(
                    icon = Icons.Rounded.WarningAmber,
                    title = account,
                    subtitle = stringResource(R.string.drive_sign_in_again_subtitle),
                    value = stringResource(R.string.drive_sign_in_again),
                    onClick = { signIn {} },
                )
                else -> SettingsRow(
                    icon = Icons.Rounded.Person,
                    title = settings.accountName ?: account,
                    subtitle = account.takeIf { settings.accountName != null },
                    value = stringResource(R.string.disconnect),
                    enabled = !running,
                    onClick = onConfirmDisconnect,
                )
            }
        }

        // ---- This phone ----
        if (connected) {
            SettingsGroup(
                header = stringResource(R.string.drive_this_phone_header),
                footer = settings.forkedTo?.let { stringResource(R.string.drive_forked_notice, it) },
            ) {
                if (settings.needsDevice) {
                    SettingsRow(
                        icon = Icons.Rounded.WarningAmber,
                        title = stringResource(R.string.drive_choose_device),
                        subtitle = stringResource(R.string.drive_choose_device_subtitle),
                        enabled = !running,
                        onClick = onOpenDevices,
                    )
                } else {
                    SettingsRow(
                        icon = Icons.Rounded.Smartphone,
                        title = settings.identity.deviceName,
                        subtitle = stringResource(R.string.drive_this_phone_subtitle),
                        value = stringResource(R.string.drive_rename),
                        enabled = !running,
                        onClick = {
                            DriveBackup.dismissForkNotice()
                            onRename(settings.identity.deviceName)
                        },
                    )
                }
            }
        }

        // ---- Auto sync Schedule ----
        SettingsGroup(
            header = stringResource(R.string.drive_auto_sync_header),
            footer = stringResource(R.string.drive_auto_sync_footer),
        ) {
            SettingsRow(
                icon = Icons.Rounded.CloudSync,
                title = stringResource(R.string.drive_auto_sync_header),
                subtitle = settings.frequency.label(),
                enabled = ready,
            )
            SegmentedControl(
                options = BackupFrequency.entries.map { it.label() },
                selectedIndex = BackupFrequency.entries.indexOf(settings.frequency),
                onSelect = { DriveBackup.setFrequency(BackupFrequency.entries[it]) },
                enabled = ready,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.SignalCellularAlt,
                title = stringResource(R.string.drive_over_cellular),
                subtitle = stringResource(R.string.drive_over_cellular_subtitle),
                enabled = ready,
                trailing = {
                    IosSwitch(
                        checked = settings.overCellular,
                        onCheckedChange = DriveBackup::setOverCellular,
                        enabled = ready,
                    )
                },
                onClick = if (ready) {
                    { DriveBackup.setOverCellular(!settings.overCellular) }
                } else {
                    null
                },
            )
        }

        // ---- Devices ----
        if (connected && !settings.needsSignIn) {
            SettingsGroup(
                header = stringResource(R.string.drive_devices_header),
                footer = stringResource(R.string.drive_devices_footer),
            ) {
                when (val d = devices) {
                    DriveBackup.Devices.Unknown, DriveBackup.Devices.Checking -> SettingsRow(
                        icon = Icons.Rounded.CloudQueue,
                        title = stringResource(R.string.drive_devices_checking),
                        trailing = { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) },
                    )
                    is DriveBackup.Devices.Failed -> SettingsRow(
                        icon = Icons.Rounded.ErrorOutline,
                        title = stringResource(R.string.drive_devices_failed, d.message),
                        onClick = { scope.launch { DriveBackup.refreshDevices(context) } },
                    )
                    is DriveBackup.Devices.Loaded -> if (d.list.isEmpty()) {
                        SettingsRow(icon = Icons.Rounded.CloudQueue, title = stringResource(R.string.drive_devices_none))
                    } else {
                        d.list.forEachIndexed { index, device ->
                            if (index > 0) RowDivider()
                            val mine = device.deviceId == settings.identity.deviceId
                            SettingsRow(
                                icon = Icons.Rounded.Smartphone,
                                title = device.name,
                                subtitle = describe(device),
                                badge = if (device.manifest.identityCode.isNotBlank()) device.manifest.identityCode else null,
                                value = if (mine) stringResource(R.string.drive_this_phone) else null,
                                enabled = !running,
                                onClick = { onConfirmRemove(device) },
                            )
                        }
                    }
                }
            }
        }

        // ---- Restore ----
        SettingsGroup(
            header = stringResource(R.string.drive_restore_header),
            footer = stringResource(R.string.drive_backup_footer),
        ) {
            SettingsRow(
                icon = Icons.Rounded.FileDownload,
                title = stringResource(R.string.drive_restore_as_title),
                subtitle = stringResource(
                    if (connected) R.string.drive_restore_as_subtitle else R.string.drive_restore_connect_first,
                ),
                enabled = connected && !settings.needsSignIn && !running,
                onClick = onOpenDevices,
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ProgressBlock(progress: DriveBackup.Progress) {
    val step = (progress as? DriveBackup.Progress.Running)?.step
    val (text, fraction) = when (step) {
        null, ReplaySync.Step.Preparing -> stringResource(R.string.drive_sync_preparing) to null
        is ReplaySync.Step.Uploading -> stringResource(
            R.string.drive_sync_uploading,
            (step.done + 1).coerceAtMost(step.total).coerceAtLeast(1),
            step.total.coerceAtLeast(1),
        ) to step.done.fractionOf(step.total)
        is ReplaySync.Step.Downloading -> stringResource(
            R.string.drive_sync_downloading,
            step.device,
            (step.done + 1).coerceAtMost(step.total),
            step.total,
        ) to step.done.fractionOf(step.total)
        ReplaySync.Step.Restoring -> stringResource(R.string.drive_backup_restoring) to null
    }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(
            text = if (fraction != null) "$text · ${(fraction * 100).toInt()}%" else text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        DriveProgressBar(fraction)
    }
}

@Composable
internal fun DriveProgressBar(fraction: Float?) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(4.dp)
    val color = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    if (fraction != null) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = modifier,
            color = color,
            trackColor = track,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    } else {
        LinearProgressIndicator(
            modifier = modifier,
            color = color,
            trackColor = track,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
        )
    }
}

private fun Int.fractionOf(total: Int): Float? =
    if (total > 0) (toFloat() / total).coerceIn(0f, 1f) else null

@Composable
private fun BackupFrequency.label(): String = stringResource(
    when (this) {
        BackupFrequency.OFF -> R.string.off
        BackupFrequency.DAILY -> R.string.drive_frequency_daily
        BackupFrequency.WEEKLY -> R.string.drive_frequency_weekly
        BackupFrequency.MONTHLY -> R.string.drive_frequency_monthly
    },
)

@Composable
internal fun describe(device: RemoteDevice): String {
    val context = LocalContext.current
    return listOfNotNull(
        device.manifest.listenerName.takeIf { it.isNotBlank() },
        device.updatedAt.takeIf { it > 0 }?.let { stringResource(R.string.drive_device_synced, formatWhen(context, it)) },
        pluralStringResource(R.plurals.listening_month_count, device.months, device.months),
        device.sizeBytes.takeIf { it > 0 }?.let { Formatter.formatShortFileSize(context, it) },
    ).joinToString(" · ")
}

internal fun formatWhen(context: Context, millis: Long): String = DateUtils.formatDateTime(
    context,
    millis,
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
)
