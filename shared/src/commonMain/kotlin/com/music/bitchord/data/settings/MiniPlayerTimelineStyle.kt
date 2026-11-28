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

/** Style of the playback progress indicator drawn on the floating/docked MiniPlayer pill. */
enum class MiniPlayerTimelineStyle {
    /** Classic 3-layer glowing progress stroke tracing the full capsule perimeter. */
    OG_CIRCLE,

    /** Artwork-driven translucent aurora silk tide and starlight shoreline inside the pill. */
    AURORA_WAVE,

    /** Clean frosted glass pill with a multi-layered luminous aurora silk ribbon along the bottom edge. */
    AURORA_SILK,
}
