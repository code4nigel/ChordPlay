package com.music.bitchord.ui.screens.settings.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.player.LyricsStyleManager
import com.music.bitchord.ui.player.rememberLyricsFontFamily

/**
 * Authentic live interactive lyrics card showing real-time feedback
 * as the user adjusts font family, weight, scale, alignment, blur, and sync,
 * without CPU-churning infinite transitions and with instant collapse toggle.
 */
@Composable
fun LyricsLivePreview(
    modifier: Modifier = Modifier,
) {
    val showLivePreviews by AppSettings.showSettingsLivePreviews.collectAsStateWithLifecycle()
    val customStyle by LyricsStyleManager.style.collectAsStateWithLifecycle()
    val syncedLyrics by AppSettings.syncedLyrics.collectAsStateWithLifecycle()
    val lyricsBlur by AppSettings.lyricsBlur.collectAsStateWithLifecycle()
    val activeTitle by AppSettings.activeTrackTitle.collectAsStateWithLifecycle()

    val fontFamily = rememberLyricsFontFamily(customStyle)
    val scale = customStyle.sizeScale.scale
    val baseFontSize = 17.sp * scale
    val baseLineHeight = 25.sp * scale
    val textAlign = if (customStyle.centerAligned) TextAlign.Center else TextAlign.Start

    val activePrimary = MaterialTheme.colorScheme.primary
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Collapsed State: Lightweight single-row pill (0 GPU / recomposition overhead)
    if (!showLivePreviews) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .clickable { AppSettings.setShowSettingsLivePreviews(true) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        tint = subtextColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.preview) + " • Tap to expand",
                        style = MaterialTheme.typography.bodySmall,
                        color = subtextColor,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = activePrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        return
    }

    // Expanded State: Elegant, authentic lyrics presentation
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (customStyle.centerAligned) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            // Header Row: Subtle status + Typography summary + Collapse toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(activePrimary),
                    )
                    Text(
                        text = "PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        ),
                        color = activePrimary,
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = subtextColor.copy(alpha = 0.5f),
                    )
                    Text(
                        text = "${customStyle.customFontName ?: customStyle.fontChoice.label} • ${customStyle.sizeScale.label} • ${if (customStyle.centerAligned) "Center" else "Left"}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = subtextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(
                    onClick = { AppSettings.setShowSettingsLivePreviews(false) },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Collapse preview",
                        tint = subtextColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Line 1: Preceding line (dimmed according to lyricsBlur)
            Text(
                text = "Hold on to what you love",
                fontFamily = fontFamily,
                fontSize = baseFontSize * 0.88f,
                lineHeight = baseLineHeight * 0.88f,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
                color = Color.White.copy(alpha = if (lyricsBlur) 0.30f else 0.50f),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(6.dp))

            // Line 2: Active singing line with true typography and sync highlight
            val activeLineColor = if (syncedLyrics) activePrimary else Color.White
            Text(
                text = "Before the moment fades away",
                fontFamily = fontFamily,
                fontSize = baseFontSize,
                lineHeight = baseLineHeight,
                fontWeight = customStyle.weightChoice.weight,
                textAlign = textAlign,
                color = activeLineColor,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(6.dp))

            // Line 3: Upcoming line (dimmed according to lyricsBlur)
            Text(
                text = "And the music takes you home",
                fontFamily = fontFamily,
                fontSize = baseFontSize * 0.88f,
                lineHeight = baseLineHeight * 0.88f,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
                color = Color.White.copy(alpha = if (lyricsBlur) 0.25f else 0.45f),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
