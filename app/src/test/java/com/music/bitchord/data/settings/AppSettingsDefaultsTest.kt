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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsDefaultsTest {
    @Test
    fun defaultsKeepAuroraSilkMiniPlayerQuickPicksOffAndLivePreviewsOn() {
        assertEquals(MiniPlayerTimelineStyle.AURORA_SILK, AppSettings.miniPlayerTimelineStyle.value)
        assertTrue("Expected Quick picks hidden by default", "Quick picks" in AppSettings.hiddenHomeShelves.value)
        assertTrue("Expected Settings live previews enabled by default", AppSettings.showSettingsLivePreviews.value)
        assertFalse("Expected offline lyrics disabled by default", AppSettings.downloadLyricsOffline.value)
        assertFalse("Expected offline lyrics never-ask flag false by default", AppSettings.neverAskDownloadLyricsOffline.value)
        assertFalse("Expected keepScreenOn false by default", AppSettings.keepScreenOn.value)
        assertEquals(KeepScreenOnDuration.FIVE_MINUTES, AppSettings.keepScreenOnDuration.value)
        assertFalse("Expected hideAiMusic false by default", AppSettings.hideAiMusic.value)
        assertEquals(0.8f, AppSettings.DEFAULT_PLAYER_ANIMATION_SPEED, 0.0001f)
        assertEquals(0.7f, AppSettings.DEFAULT_MINIPLAYER_ANIMATION_SPEED, 0.0001f)
        assertEquals(AppSettings.DEFAULT_PLAYER_ANIMATION_SPEED, AppSettings.playerAnimationSpeed.value, 0.0001f)
        assertEquals(AppSettings.DEFAULT_MINIPLAYER_ANIMATION_SPEED, AppSettings.miniplayerAnimationSpeed.value, 0.0001f)
    }

    @Test
    fun miniplayerAndPlayerAnimationSpeedsAreIndependentAndClamped() {
        val originalPlayer = AppSettings.playerAnimationSpeed.value
        val originalMini = AppSettings.miniplayerAnimationSpeed.value
        try {
            AppSettings.setPlayerAnimationSpeed(1.6f)
            AppSettings.setMiniplayerAnimationSpeed(0.7f)
            assertEquals(1.6f, AppSettings.playerAnimationSpeed.value, 0.001f)
            assertEquals(0.7f, AppSettings.miniplayerAnimationSpeed.value, 0.001f)

            AppSettings.setMiniplayerAnimationSpeed(9.9f)
            assertEquals(AppSettings.MAX_MINIPLAYER_ANIMATION_SPEED, AppSettings.miniplayerAnimationSpeed.value, 0.001f)
            assertEquals(1.6f, AppSettings.playerAnimationSpeed.value, 0.001f)

            AppSettings.setMiniplayerAnimationSpeed(0.1f)
            assertEquals(AppSettings.MIN_MINIPLAYER_ANIMATION_SPEED, AppSettings.miniplayerAnimationSpeed.value, 0.001f)
        } finally {
            AppSettings.setPlayerAnimationSpeed(originalPlayer)
            AppSettings.setMiniplayerAnimationSpeed(originalMini)
        }
    }
}
