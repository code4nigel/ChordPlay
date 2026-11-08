package com.music.bitchord.ui.utils

import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Utility determining active festival windows and seasonal themes.
 */
object FestivalTheme {

    /**
     * Optional manual override for testing / remote config.
     * When null, evaluates against system calendar date.
     */
    @Volatile
    var forceDiwaliActive: Boolean? = null

    /**
     * Triggers manual re-opening of the Diwali popup (e.g. when tapping the ChordPlay header).
     */
    val requestDiwaliPopup = MutableStateFlow(0)

    fun triggerDiwaliPopup() {
        requestDiwaliPopup.value += 1
    }

    /**
     * Checks if today falls within the Diwali celebration period (November 8–10, 2026).
     * Automatically returns false and turns off after the festival passes.
     */
    fun isDiwaliActive(today: LocalDate = LocalDate.now()): Boolean {
        forceDiwaliActive?.let { return it }
        return (today.year == 2026 && today.monthValue == 11 && today.dayOfMonth in 8..10)
    }
}

