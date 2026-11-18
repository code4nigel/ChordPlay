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
package com.music.bitchord.util

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Passes the system's `onTrimMemory` on to the app's own in-memory caches.
 *
 * Listeners say for themselves what they let go of, because only they know
 * what a later rebuild costs. Each one is called on the main thread. A
 * listener tied to something shorter-lived than the process (a ViewModel, a
 * service) has to [unregister] when that thing ends, or this object becomes
 * what keeps it alive.
 */
object MemoryPressure {

    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    /**
     * Calls [listener] when the app is running but memory is short
     * (`RUNNING_LOW`, `RUNNING_CRITICAL`, `onLowMemory`), or when this process
     * is cached and can be killed (`BACKGROUND` and above). It should drop
     * whatever is cheap to rebuild.
     */
    fun register(listener: () -> Unit) {
        listeners += listener
    }

    fun unregister(listener: () -> Unit) {
        listeners -= listener
    }

    /** Call once from [Application.onCreate]. */
    fun install(app: Application) {
        app.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                if (shouldTrim(level)) dispatch()
            }

            @Deprecated("Deprecated in Java")
            override fun onLowMemory() = dispatch()

            override fun onConfigurationChanged(newConfig: Configuration) = Unit
        })
    }

    /**
     * `UI_HIDDEN` only means the user went to another app. Playback, widgets
     * and the notification are often still running then, and the caches are
     * serving them, so that level and `RUNNING_MODERATE` change nothing.
     */
    @Suppress("DEPRECATION") // RUNNING_* levels are still sent below API 34.
    private fun shouldTrim(level: Int): Boolean = when {
        level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> true
        level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> false
        else -> level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
    }

    private fun dispatch() {
        listeners.forEach { listener -> runCatching { listener() } }
    }
}
