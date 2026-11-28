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
package com.music.bitchord.data.settings

enum class CanvasProvider {
    SPOTIFY,
    APPLE_MUSIC,
    TIDAL,
    COMMUNITY;

    companion object {
        val defaultOrder: List<CanvasProvider> = entries.toList()

        fun normalize(raw: List<CanvasProvider>): List<CanvasProvider> {
            val seen = LinkedHashSet<CanvasProvider>(entries.size)
            for (provider in raw) seen.add(provider)
            for (provider in entries) seen.add(provider)
            return seen.toList()
        }

        fun parse(stored: String?): List<CanvasProvider> {
            if (stored.isNullOrBlank()) return defaultOrder
            val parsed = stored.split(',').mapNotNull { token ->
                entries.firstOrNull { it.name == token.trim() }
            }
            return normalize(parsed)
        }

        fun serialize(order: List<CanvasProvider>): String =
            normalize(order).joinToString(",") { it.name }
    }
}
