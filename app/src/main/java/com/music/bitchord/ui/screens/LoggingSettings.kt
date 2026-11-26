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

import android.os.Build
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.BuildConfig
import com.music.bitchord.R
import com.music.bitchord.data.LogRecorder
import com.music.bitchord.ui.components.AlertAction
import com.music.bitchord.ui.components.AlertRule
import com.music.bitchord.ui.components.GlassAlert
import com.music.bitchord.ui.components.IosSwitch
import kotlinx.coroutines.launch
import java.io.File

/**
 * Advanced & Experimental "Enable logging": records diagnostic events, device/OS info,
 * and crash traces into a shareable file ONLY while explicitly enabled. Turning logging ON
 * opens an explicit confirmation dialog explaining what is logged before starting.
 */
@Composable
internal fun EnableLoggingRow(title: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recording by LogRecorder.recording.collectAsStateWithLifecycle()
    var showEnableConfirm by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf<File?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { target ->
        val file = finished ?: return@rememberLauncherForActivityResult
        if (target == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = LogRecorder.saveTo(context.contentResolver, file, target)
            Toast.makeText(
                context,
                context.getString(if (saved) R.string.log_saved else R.string.log_save_failed),
                Toast.LENGTH_SHORT,
            ).show()
            if (saved) finished = null
        }
    }

    val requestToggle: (Boolean) -> Unit = { targetOn ->
        if (targetOn) {
            if (!recording) showEnableConfirm = true
        } else {
            finished = LogRecorder.stop(context)
        }
    }

    SettingsRow(
        icon = Icons.Rounded.BugReport,
        title = title,
        subtitle = stringResource(if (recording) R.string.enable_logging_running else R.string.enable_logging_subtitle),
        trailing = {
            IosSwitch(
                checked = recording,
                onCheckedChange = requestToggle,
            )
        },
        onClick = { requestToggle(!recording) },
    )

    if (showEnableConfirm) {
        GlassAlert(
            title = stringResource(R.string.enable_logging_confirm_title),
            onDismiss = { showEnableConfirm = false },
            footerActions = {
                AlertAction(
                    label = stringResource(R.string.enable_logging_confirm_action),
                    emphasised = true,
                    onClick = {
                        showEnableConfirm = false
                        LogRecorder.start(context)
                    },
                )
                AlertRule()
                AlertAction(
                    label = stringResource(R.string.cancel),
                    emphasised = false,
                    onClick = { showEnableConfirm = false },
                )
            },
        ) {
            val ink = LocalContentColor.current
            Text(
                text = stringResource(
                    R.string.enable_logging_confirm_message,
                    "${Build.MANUFACTURER} ${Build.MODEL}",
                    "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                    "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                ),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
                color = ink.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
        }
    }

    finished?.let { file ->
        GlassAlert(
            title = stringResource(R.string.log_ready_title),
            onDismiss = { finished = null },
            footerActions = {
                AlertAction(
                    label = stringResource(R.string.share_log),
                    emphasised = true,
                    onClick = {
                        context.startActivity(LogRecorder.shareIntent(context, file))
                        finished = null
                    },
                )
                AlertRule()
                AlertAction(
                    label = stringResource(R.string.save_log),
                    emphasised = false,
                    onClick = { saveLauncher.launch(file.name) },
                )
                AlertRule()
                AlertAction(
                    label = stringResource(R.string.done),
                    emphasised = false,
                    onClick = { finished = null },
                )
            },
        ) {
            Text(
                text = stringResource(
                    R.string.log_ready_message,
                    Formatter.formatShortFileSize(context, file.length()),
                ),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
                color = LocalContentColor.current.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
