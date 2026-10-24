package com.music.bitchord.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Storage
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
    PLAYER(R.string.playback, Icons.Rounded.SmartDisplay),
    LYRICS(R.string.synced_lyrics, Icons.AutoMirrored.Rounded.Notes),
    AUDIO(R.string.audio_quality, Icons.Rounded.GraphicEq),
    STORAGE(R.string.storage, Icons.Rounded.Storage),
    INTEGRATIONS(R.string.account_integrations, Icons.Rounded.Person),
    ABOUT(R.string.software_update, Icons.Rounded.AutoAwesome);
}
