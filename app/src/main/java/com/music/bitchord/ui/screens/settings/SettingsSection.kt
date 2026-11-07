package com.music.bitchord.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.ui.graphics.vector.ImageVector
import com.music.bitchord.R

/**
 * The modular categories of ChordPlay's Prism-inspired settings hierarchy.
 */
enum class SettingsSection(
    val titleRes: Int,
    val icon: ImageVector,
) {
    APPEARANCE(R.string.appearance, Icons.Rounded.Brightness4),
    SHELVES(R.string.home_shelves, Icons.Rounded.ViewAgenda),
    PLAYER(R.string.playback, Icons.Rounded.SmartDisplay),
    LYRICS(R.string.synced_lyrics, Icons.AutoMirrored.Rounded.Notes),
    AUDIO(R.string.audio_quality, Icons.Rounded.GraphicEq),
    DOWNLOADS(R.string.downloads, Icons.Rounded.Download),
    STORAGE(R.string.storage, Icons.Rounded.Storage),
    YOUR_DATA(R.string.your_data, Icons.Rounded.BarChart),
    ADVANCED_PERFORMANCE(R.string.advanced_and_performance, Icons.Rounded.Tune),
    SUPPORT(R.string.support_community, Icons.Rounded.Favorite),
    INTEGRATIONS(R.string.account_integrations, Icons.Rounded.Person),
    ABOUT(R.string.miscellaneous, Icons.AutoMirrored.Rounded.HelpOutline);
}
