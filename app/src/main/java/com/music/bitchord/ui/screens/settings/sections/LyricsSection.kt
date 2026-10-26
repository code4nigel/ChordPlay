package com.music.bitchord.ui.screens.settings.sections

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.lyrics.translationLanguageName
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.FullscreenArtworkLyricsStyle
import com.music.bitchord.ui.player.LyricsCustomStyle
import com.music.bitchord.ui.player.LyricsFontChoice
import com.music.bitchord.ui.player.LyricsSizeScale
import com.music.bitchord.ui.player.LyricsStyleManager
import com.music.bitchord.ui.player.LyricsWeightChoice
import com.music.bitchord.ui.player.rememberLyricsFontFamily
import com.music.bitchord.ui.screens.*
import com.music.bitchord.ui.screens.settings.previews.LyricsLivePreview
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Lyrics & Typography customization sub-page with a pinned live interactive preview card.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LyricsSection(
    onLyricsSources: () -> Unit,
    onTranslationLanguage: () -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val customStyle by LyricsStyleManager.style.collectAsStateWithLifecycle()
    val syncedLyrics by AppSettings.syncedLyrics.collectAsStateWithLifecycle()
    val lyricsBlur by AppSettings.lyricsBlur.collectAsStateWithLifecycle()
    val lyricsSources by AppSettings.lyricsSources.collectAsStateWithLifecycle()
    val translationLanguage by AppSettings.translationLanguage.collectAsStateWithLifecycle()
    val fullscreenArtworkLyricsStyle by AppSettings.fullscreenArtworkLyricsStyle.collectAsStateWithLifecycle()
    var showFullscreenLyricsDialog by remember { mutableStateOf(false) }

    val fontFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val filename = "custom_lyrics_font_${System.currentTimeMillis()}.ttf"
            val destFile = File(context.filesDir, filename)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            val displayName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Custom Font"
            LyricsStyleManager.setCustomFont(displayName.substringBeforeLast('.'), destFile.absolutePath)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.synced_lyrics),
            onBack = onBack,
        )

        // Pinned Live Preview
        LyricsLivePreview()

        // Typography: Font Family Picker
        SettingsGroup(header = "Typography & Fonts") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ROW_INSET, vertical = 12.dp),
            ) {
                Text(
                    text = "Font Family",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Choose from handcrafted typefaces or load your own custom font file",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LyricsFontChoice.entries.filter { it != LyricsFontChoice.CUSTOM }.forEach { font ->
                        val isSelected = customStyle.fontChoice == font
                        val chipFontFamily = rememberLyricsFontFamily(customStyle.copy(fontChoice = font))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface,
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable { LyricsStyleManager.updateFont(font) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = font.label,
                                fontFamily = chipFontFamily,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }

                    // Custom Font File Chip
                    val isCustom = customStyle.fontChoice == LyricsFontChoice.CUSTOM
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isCustom) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface,
                            )
                            .border(
                                1.dp,
                                if (isCustom) MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(10.dp),
                            )
                            .clickable {
                                if (customStyle.customFontPath != null) {
                                    LyricsStyleManager.updateFont(LyricsFontChoice.CUSTOM)
                                } else {
                                    fontFileLauncher.launch(arrayOf("font/*", "application/octet-stream", "*/*"))
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCustom) Icons.Rounded.Check else Icons.Rounded.Add,
                                contentDescription = null,
                                tint = if (isCustom) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = customStyle.customFontName ?: "Custom File",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCustom) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }
        }

        // Scale, Weight, and Alignment Group
        SettingsGroup(header = "Size & Alignment") {
            // Scale Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ROW_INSET, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Size Scale",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = customStyle.sizeScale.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val scaleIndex = LyricsSizeScale.entries.indexOf(customStyle.sizeScale).coerceAtLeast(0)
                Slider(
                    value = scaleIndex.toFloat(),
                    onValueChange = {
                        LyricsStyleManager.updateScale(LyricsSizeScale.entries[it.roundToInt()])
                    },
                    valueRange = 0f..(LyricsSizeScale.entries.size - 1).toFloat(),
                    steps = LyricsSizeScale.entries.size - 2,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }

            // Weight selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ROW_INSET, vertical = 8.dp),
            ) {
                Text(
                    text = "Font Weight",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                SegmentedControl(
                    options = LyricsWeightChoice.entries.map { it.label },
                    selectedIndex = LyricsWeightChoice.entries.indexOf(customStyle.weightChoice).coerceAtLeast(0),
                    onSelect = { LyricsStyleManager.updateWeight(LyricsWeightChoice.entries[it]) },
                )
            }

            // Alignment toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ROW_INSET, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Text Alignment",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.outline)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (!customStyle.centerAligned) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                            )
                            .clickable { LyricsStyleManager.updateCenterAligned(false) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FormatAlignLeft,
                            contentDescription = "Left",
                            tint = if (!customStyle.centerAligned) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (customStyle.centerAligned) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                            )
                            .clickable { LyricsStyleManager.updateCenterAligned(true) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FormatAlignCenter,
                            contentDescription = "Center",
                            tint = if (customStyle.centerAligned) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        // Lyrics Sync & Sources Group
        SettingsGroup(header = stringResource(R.string.synced_lyrics)) {
            val syncedLyricsTitle = stringResource(R.string.synced_lyrics)
            SettingsRow(
                icon = Icons.AutoMirrored.Rounded.Notes,
                title = syncedLyricsTitle,
                subtitle = stringResource(R.string.synced_lyrics_subtitle),
                trailing = {
                    Switch(
                        checked = syncedLyrics,
                        onCheckedChange = AppSettings::setSyncedLyrics,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setSyncedLyrics(!syncedLyrics) },
            )

            if (syncedLyrics) {
                val lyricsBlurTitle = stringResource(R.string.blur_unfocused_lyrics)
                SettingsRow(
                    icon = Icons.Rounded.BlurOn,
                    title = lyricsBlurTitle,
                    subtitle = stringResource(R.string.blur_unfocused_lyrics_subtitle),
                    trailing = {
                        Switch(
                            checked = lyricsBlur,
                            onCheckedChange = AppSettings::setLyricsBlur,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    },
                    onClick = { AppSettings.setLyricsBlur(!lyricsBlur) },
                )

                val lyricsSourcesTitle = stringResource(R.string.lyrics_sources)
                SettingsRow(
                    icon = Icons.Rounded.Language,
                    title = lyricsSourcesTitle,
                    subtitle = lyricsSources
                        .sortedBy { it.ordinal }
                        .joinToString(", ") { it.label }
                        .ifEmpty { stringResource(R.string.no_lyrics_sources_enabled) },
                    trailing = { Chevron() },
                    onClick = onLyricsSources,
                )

                val translationLanguageTitle = stringResource(R.string.translation_language)
                SettingsRow(
                    icon = Icons.Rounded.Translate,
                    title = translationLanguageTitle,
                    subtitle = if (translationLanguage.isBlank()) {
                        stringResource(R.string.translation_language_subtitle)
                    } else {
                        translationLanguageName(
                            translationLanguage,
                            AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault(),
                        )
                    },
                    trailing = { Chevron() },
                    onClick = onTranslationLanguage,
                )

                SettingsRow(
                    icon = Icons.Rounded.Fullscreen,
                    title = "Fullscreen Artwork Lyrics",
                    subtitle = fullscreenArtworkLyricsStyle.label,
                    trailing = { Chevron() },
                    onClick = { showFullscreenLyricsDialog = true },
                )
            }
        }

        if (showFullscreenLyricsDialog) {
            AlertDialog(
                onDismissRequest = { showFullscreenLyricsDialog = false },
                title = {
                    Text(
                        text = "Fullscreen Artwork Lyrics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        FullscreenArtworkLyricsStyle.entries.forEach { style ->
                            val isSelected = style == fullscreenArtworkLyricsStyle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        AppSettings.setFullscreenArtworkLyricsStyle(style)
                                        showFullscreenLyricsDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = style.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFullscreenLyricsDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}
