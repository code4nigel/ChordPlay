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
