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
package com.music.bitchord.ui.graphics

/**
 * Smootherstep, `6t⁵ − 15t⁴ + 10t³`, for a fade across `t` in 0..1.
 *
 * Its slope and its curvature are both zero at either end, so a fade drawn
 * along it eases in from nothing and settles into nothing: there is no line
 * where it starts or where it finishes. A straight ramp turns a corner at both,
 * and the eye picks those corners out as edges across whatever is fading.
 */
fun smootherStep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * x * (x * (x * 6f - 15f) + 10f)
}

/**
 * How many stops a gradient following [smootherStep] is cut into: enough that
 * the straight spans between them neither band nor show the curve as facets
 * over a tall fade.
 */
const val FADE_CURVE_STOPS = 16
