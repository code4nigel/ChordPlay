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
package com.music.bitchord.ui.theme

import androidx.compose.runtime.Composable

/**
 * Turns the screen for a music video, as YouTube does: the full-screen button
 * asks for landscape, leaving full screen asks for portrait. Either is only a
 * nudge — once the phone is physically held that way the screen follows the
 * sensor again, so turning it back undoes it. Let go of, and the screen handed
 * back to the sensor, when this leaves composition.
 */
interface ScreenOrientation {
    fun landscape()
    fun portrait()
}

@Composable
expect fun rememberScreenOrientation(): ScreenOrientation
