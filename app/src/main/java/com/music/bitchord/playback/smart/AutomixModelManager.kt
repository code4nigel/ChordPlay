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
package com.music.bitchord.playback.smart

import android.content.Context
import com.music.bitchord.data.DebugLog
import com.music.bitchord.data.Http
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Downloads and manages the on-device Automix ONNX models on demand so they do not bloat the base APK.
 */
object AutomixModelManager {

    private const val TAG = "AutomixModelManager"
    private const val BASE_URL = "https://raw.githubusercontent.com/code4nigel/ChordPlay/main/models/"

    private val requiredModels = listOf(
        BeatTracker.MODEL_ASSET,
        VocalTracker.MODEL_ASSET,
    )

    sealed interface ModelState {
        data object NotDownloaded : ModelState
        data class Downloading(val progress: Float) : ModelState
        data object Ready : ModelState
        data class Error(val message: String) : ModelState
    }

    private val _state = MutableStateFlow<ModelState>(ModelState.NotDownloaded)
    val state: StateFlow<ModelState> = _state.asStateFlow()

    private val downloading = AtomicBoolean(false)
    private val syncLock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun areModelsReady(context: Context): Boolean {
        val dir = context.applicationContext.filesDir
        return requiredModels.all { name ->
            val file = File(dir, name)
            file.exists() && file.length() > 0L
        }
    }

    fun refreshState(context: Context) {
        if (downloading.get()) return
        _state.value = if (areModelsReady(context)) ModelState.Ready else ModelState.NotDownloaded
    }

    /**
     * Triggers an asynchronous download of any missing Automix models when the user enables Automix.
     */
    fun ensureModelsDownloaded(context: Context) {
        val appContext = context.applicationContext
        if (areModelsReady(appContext)) {
            _state.value = ModelState.Ready
            return
        }
        if (!downloading.compareAndSet(false, true)) return

        _state.value = ModelState.Downloading(0f)
        scope.launch {
            try {
                val missing = requiredModels.filter { name ->
                    val file = File(appContext.filesDir, name)
                    !file.exists() || file.length() == 0L
                }
                val totalCount = missing.size.coerceAtLeast(1)
                missing.forEachIndexed { index, name ->
                    downloadModel(appContext, name) { fileFraction ->
                        val overall = ((index + fileFraction) / totalCount).coerceIn(0f, 1f)
                        _state.value = ModelState.Downloading(overall)
                    }
                }
                _state.value = if (areModelsReady(appContext)) {
                    ModelState.Ready
                } else {
                    ModelState.Error("Incomplete model download")
                }
            } catch (t: Throwable) {
                DebugLog.w(TAG, "Automix model download failed: ${t.message}")
                _state.value = ModelState.Error(t.message ?: "Download failed")
            } finally {
                downloading.set(false)
            }
        }
    }

    /**
     * Resolves [name] on disk for [BeatTracker] / [VocalTracker], unpacking from assets if present
     * or downloading on demand if missing.
     */
    fun ensureModelSync(context: Context, name: String): String {
        val appContext = context.applicationContext
        val target = File(appContext.filesDir, name)
        if (target.exists() && target.length() > 0L) return target.absolutePath

        synchronized(syncLock) {
            if (target.exists() && target.length() > 0L) return target.absolutePath
            val copiedFromAssets = runCatching {
                appContext.assets.open(name).use { input ->
                    val tmp = File(appContext.filesDir, "$name.tmp")
                    tmp.outputStream().use { output -> input.copyTo(output) }
                    if (!tmp.renameTo(target)) {
                        tmp.copyTo(target, overwrite = true)
                        tmp.delete()
                    }
                }
                target.exists() && target.length() > 0L
            }.getOrDefault(false)

            if (copiedFromAssets) return target.absolutePath

            runCatching { downloadModel(appContext, name) }
                .onFailure {
                    DebugLog.w(TAG, "On-demand download of $name failed: ${it.message}")
                }
            if (target.exists() && target.length() > 0L) return target.absolutePath
            throw FileNotFoundException("Automix model '$name' is not downloaded")
        }
    }

    private fun downloadModel(
        context: Context,
        name: String,
        onProgress: (Float) -> Unit = {},
    ) {
        val dir = context.filesDir
        val target = File(dir, name)
        if (target.exists() && target.length() > 0L) {
            onProgress(1f)
            return
        }
        val tmp = File(dir, "$name.tmp")
        val request = Request.Builder().url("$BASE_URL$name").build()
        Http.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} downloading $name")
            }
            val body = response.body ?: throw IOException("Empty response body for $name")
            val totalBytes = body.contentLength()
            var copied = 0L
            val buffer = ByteArray(16 * 1024)
            body.byteStream().use { input ->
                tmp.outputStream().use { output ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (totalBytes > 0L) {
                            onProgress((copied.toFloat() / totalBytes).coerceIn(0f, 1f))
                        }
                    }
                }
            }
        }
        if (tmp.length() <= 0L) {
            tmp.delete()
            throw IOException("Downloaded empty file for $name")
        }
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
        onProgress(1f)
    }
}
