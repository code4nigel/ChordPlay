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
package com.music.bitchord.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.music.bitchord.BuildConfig
import com.music.bitchord.data.innertube.InnerTubeXResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings' "Enable logging": manages ChordPlay's opt-in [TrackLog] diagnostic session.
 *
 * When explicitly enabled by the listener, every logged event plus device, OS,
 * and build metadata is written into `cache/logs/`. It remains active across app
 * restarts until switched off so launch-time issues can be captured, while older
 * sessions beyond [MAX_CARRIED_BYTES] rotate automatically. When disabled, no
 * session file is written.
 */
object LogRecorder {

    private const val FOLDER = "logs"
    private const val PREFS = "chordplay_logging"
    private const val KEY_ENABLED = "enabled"

    /** Past this, a session carried over a launch starts a new file. */
    private const val MAX_CARRIED_BYTES = 16L * 1024 * 1024

    val recording: StateFlow<Boolean> get() = TrackLog.recording

    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    fun start(context: Context) {
        val folder = File(context.cacheDir, FOLDER)
        folder.listFiles()?.forEach { it.delete() }
        begin(newFile(folder), append = false)
        remember(context, true)
        TrackLog.i("Log", "logging enabled")
    }

    /**
     * Carries the session on if logging was left on when the app last ran.
     * Called early in `Application.onCreate()` so startup issues are captured.
     */
    fun resumeAtLaunch(context: Context) {
        val enabled = runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)
        }.getOrDefault(false)
        if (!enabled) return
        runCatching {
            val folder = File(context.cacheDir, FOLDER)
            val last = folder.listFiles()?.filter { it.isFile }?.maxByOrNull { it.lastModified() }
            val carryOn = last != null && last.length() < MAX_CARRIED_BYTES
            if (!carryOn) folder.listFiles()?.forEach { it.delete() }
            begin(if (carryOn) last!! else newFile(folder), append = carryOn)
            TrackLog.i("Log", "app started, logging still enabled")
        }
    }

    private fun newFile(folder: File): File {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        return File(folder, "chordplay-log-$stamp.txt")
    }

    private fun begin(file: File, append: Boolean) {
        TrackLog.startSession(
            file,
            header = listOf(
                "ChordPlay log",
                "build: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}, ${BuildConfig.FLAVOR}${BuildConfig.BUILD_TYPE})",
                "device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE}), Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                "abi: ${Build.SUPPORTED_ABIS.joinToString()}",
            ),
            append = append,
        )
        InnerTubeXResolver.debugLogging = true
        val current = Thread.getDefaultUncaughtExceptionHandler()
        if (previousHandler == null) {
            previousHandler = current
            Thread.setDefaultUncaughtExceptionHandler { thread, error ->
                TrackLog.toSession('E', "Crash", "uncaught on ${thread.name}", error)
                previousHandler?.uncaughtException(thread, error)
            }
        }
    }

    private fun remember(context: Context, enabled: Boolean) {
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).commit()
        }
    }

    /** Ends the session and returns its file, or null if nothing was running. */
    fun stop(context: Context): File? {
        remember(context, false)
        TrackLog.i("Log", "logging disabled")
        InnerTubeXResolver.debugLogging = BuildConfig.DEBUG
        return TrackLog.stopSession()?.takeIf { it.exists() }
    }

    /** A share sheet for [file], through the app's FileProvider. */
    fun shareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, null).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    /** Copies [file] to [target], a document the listener picked. */
    suspend fun saveTo(resolver: ContentResolver, file: File, target: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            resolver.openOutputStream(target)?.use { out -> file.inputStream().use { it.copyTo(out) } } != null
        }.getOrDefault(false)
    }
}
