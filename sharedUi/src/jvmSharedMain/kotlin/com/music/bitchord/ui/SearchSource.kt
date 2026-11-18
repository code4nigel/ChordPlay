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
package com.music.bitchord.ui

/**
 * Split for the Search tab:
 * - [YOUTUBE]: Online YouTube Music catalog
 * - [LIBRARY]: User's saved library (saved playlists, albums, artists, tracks)
 * - [LOCAL]: Offline device storage (Local Music folder)
 * - [ADDONS]: Configured music source addons
 */
enum class SearchSource {
    YOUTUBE,
    LIBRARY,
    LOCAL,
    ADDONS,
}
