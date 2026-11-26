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
    }
}
