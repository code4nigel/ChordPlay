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

import org.junit.Assert.assertEquals
import org.junit.Test

class NavTabsOrderTest {
    @Test
    fun visibleNavPagesKeepsOrderAndAppendsSearch() {
        val reordered = listOf(
            OrderedToggle(NavPage.LIBRARY, true),
            OrderedToggle(NavPage.HOME, false),
            OrderedToggle(NavPage.RADIO, true),
        )
        assertEquals(
            listOf(NavPage.LIBRARY, NavPage.RADIO, NavPage.SEARCH),
            AppSettings.visibleNavPages(reordered),
        )
    }

    @Test
    fun setNavTabsPreventsDisablingEveryPillTab() {
        AppSettings.setNavTabs(
            listOf(
                OrderedToggle(NavPage.HOME, false),
                OrderedToggle(NavPage.RADIO, false),
                OrderedToggle(NavPage.LIBRARY, false),
            ),
        )
        assertEquals(
            listOf(NavPage.HOME, NavPage.SEARCH),
            AppSettings.visibleNavPages(),
        )
        AppSettings.setNavTabs(NavPage.defaultOrder.map { OrderedToggle(it, true) })
    }

    @Test
    fun readAndWriteOrderedRoundTripsCustomOrderAndToggles() {
        val input = listOf(
            OrderedToggle(NavPage.LIBRARY, true),
            OrderedToggle(NavPage.HOME, true),
            OrderedToggle(NavPage.RADIO, false),
        )
        val encoded = AppSettings.writeOrdered(input) { it.name }
        val decoded = AppSettings.readOrdered(
            stored = encoded,
            all = NavPage.pillPages,
            defaultOn = NavPage.pillPages.toSet(),
            defaultOrder = NavPage.defaultOrder,
        ) { name -> NavPage.entries.firstOrNull { it.name == name } }
        assertEquals(input, decoded)
    }
}
