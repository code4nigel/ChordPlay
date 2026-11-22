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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.backup.DriveBackup
import com.music.bitchord.data.backup.RemoteDevice
import kotlinx.coroutines.launch

@Composable
fun DriveDevicesScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onConfirmRestore: (RemoteDevice) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by DriveBackup.settings.collectAsStateWithLifecycle()
    val devices by DriveBackup.devices.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { DriveBackup.refreshDevices(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.drive_devices_title),
            onBack = onBack,
        )

        if (settings.needsDevice) {
            SettingsGroup(topSpacing = 0.dp, footer = stringResource(R.string.drive_new_device_footer)) {
                SettingsRow(
                    icon = Icons.Rounded.CloudUpload,
                    title = stringResource(R.string.drive_new_device),
                    subtitle = DriveBackup.deviceName(),
                    onClick = {
                        DriveBackup.chooseNewDevice(context)
                        onDone()
                    },
                )
            }
        }

        SettingsGroup(
            header = stringResource(R.string.drive_restore_as_header),
            footer = stringResource(R.string.drive_restore_as_footer),
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
                is DriveBackup.Devices.Loaded -> {
                    val others = d.list.filter { it.deviceId != settings.identity.deviceId }
                    if (others.isEmpty()) {
                        SettingsRow(icon = Icons.Rounded.CloudQueue, title = stringResource(R.string.drive_devices_none_other))
                    } else {
                        others.forEachIndexed { index, device ->
                            if (index > 0) RowDivider()
                            SettingsRow(
                                icon = Icons.Rounded.Smartphone,
                                title = device.name,
                                subtitle = describe(device),
                                badge = if (device.manifest.identityCode.isNotBlank()) device.manifest.identityCode else null,
                                value = stringResource(R.string.drive_restore_action),
                                onClick = { onConfirmRestore(device) },
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
