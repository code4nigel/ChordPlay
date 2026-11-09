package com.music.bitchord.ui.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reactive state and configuration for seasonal festival themes and animations.
 */
object FestivalTheme {

    /**
     * Reactive StateFlow reflecting whether the seasonal festival theme is active.
     * Initialized from local disk storage at cold start, then updated live via remote config.
     */
    val isFestivalActive = MutableStateFlow(false)

    /**
     * Backwards-compatible alias for Diwali checks.
     */
    val isDiwaliActive = isFestivalActive

    /**
     * Theme type identifier (e.g. "diwali", "christmas", "new_year", "generic").
     */
    val festivalThemeType = MutableStateFlow("diwali")

    /**
     * Optional remote decoration image/animation URL (e.g. Santa hat, snowman, campfire)
     * displayed over or flanking the ChordPlay header logo.
     */
    val festivalLogoDecorationUrl = MutableStateFlow<String?>(null)

    /**
     * Request counter to trigger re-opening the celebration dialog.
     */
    val requestFestivalPopup = MutableStateFlow(0)
    val requestDiwaliPopup = requestFestivalPopup

    fun triggerFestivalPopup() {
        if (isFestivalActive.value) {
            requestFestivalPopup.value += 1
        }
    }

    fun triggerDiwaliPopup() {
        triggerFestivalPopup()
    }

    /**
     * Updates reactive theme state from local disk cache or remote config payload.
     */
    fun updateFromSettings(
        active: Boolean,
        themeType: String = "diwali",
        logoDecorationUrl: String? = null,
    ) {
        isFestivalActive.value = active
        festivalThemeType.value = themeType
        festivalLogoDecorationUrl.value = logoDecorationUrl
    }
}
