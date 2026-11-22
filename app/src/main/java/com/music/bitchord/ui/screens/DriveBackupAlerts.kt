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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.music.bitchord.R
import com.music.bitchord.data.backup.DriveBackup
import com.music.bitchord.data.backup.RemoteDevice
import com.music.bitchord.ui.components.ConfirmationAlert
import com.music.bitchord.ui.components.TextValueAlert
import dev.chrisbanes.haze.HazeState

@Stable
class DriveBackupAlerts {
    var disconnect by mutableStateOf(false)
    var remove by mutableStateOf<RemoteDevice?>(null)
    var restore by mutableStateOf<RemoteDevice?>(null)
    var renaming by mutableStateOf<String?>(null)
}

@Composable
fun DriveBackupAlertsHost(
    alerts: DriveBackupAlerts,
    hazeState: HazeState,
    onRestoreStarted: () -> Unit,
) {
    val context = LocalContext.current

    if (alerts.disconnect) {
        ConfirmationAlert(
            hazeState = hazeState,
            title = stringResource(R.string.drive_disconnect_confirm_title),
            description = stringResource(R.string.drive_disconnect_confirm_body),
            confirmLabel = stringResource(R.string.disconnect),
            onConfirm = {
                DriveBackup.clearResult()
                DriveBackup.disconnect(context)
                alerts.disconnect = false
            },
            onDismiss = { alerts.disconnect = false },
        )
    }

    alerts.remove?.let { device ->
        val mine = device.deviceId == DriveBackup.settings.value.identity.deviceId
        ConfirmationAlert(
            hazeState = hazeState,
            title = stringResource(R.string.drive_remove_confirm_title, device.name),
            description = stringResource(
                if (mine) R.string.drive_delete_mine_body else R.string.drive_remove_confirm_body,
            ),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                DriveBackup.remove(context, device)
                alerts.remove = null
            },
            onDismiss = { alerts.remove = null },
        )
    }

    alerts.restore?.let { device ->
        ConfirmationAlert(
            hazeState = hazeState,
            title = stringResource(R.string.drive_restore_confirm_title, device.name),
            description = stringResource(R.string.drive_restore_confirm_body, device.name),
            confirmLabel = stringResource(R.string.drive_restore_action),
            onConfirm = {
                DriveBackup.clearResult()
                DriveBackup.restoreAs(context, device)
                alerts.restore = null
                onRestoreStarted()
            },
            onDismiss = { alerts.restore = null },
        )
    }

    alerts.renaming?.let { name ->
        TextValueAlert(
            hazeState = hazeState,
            title = stringResource(R.string.drive_rename_title),
            message = stringResource(R.string.drive_rename_message),
            placeholder = DriveBackup.deviceName(),
            value = name,
            onValueChange = { alerts.renaming = it.take(60) },
            onSave = {
                DriveBackup.rename(name)
                alerts.renaming = null
            },
            onDismiss = { alerts.renaming = null },
            saveEnabled = name.isNotBlank(),
        )
    }
}
