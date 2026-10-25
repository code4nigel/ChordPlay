package com.music.bitchord.ui.screens.settings.previews

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.player.LyricsStyleManager
import com.music.bitchord.ui.player.rememberLyricsFontFamily

/**
 * Pinned live interactive lyrics card showing real-time feedback
 * as the user adjusts font family, scale, alignment, and syllable sync.
 */
@Composable
fun LyricsLivePreview(
    modifier: Modifier = Modifier,
) {
    val customStyle by LyricsStyleManager.style.collectAsStateWithLifecycle()
    val syncedLyrics by AppSettings.syncedLyrics.collectAsStateWithLifecycle()
    val lyricsBlur by AppSettings.lyricsBlur.collectAsStateWithLifecycle()

    val fontFamily = rememberLyricsFontFamily(customStyle)
    val scale = customStyle.sizeScale.scale
    val baseFontSize = 18.sp * scale
    val baseLineHeight = 26.sp * scale
    val textAlign = if (customStyle.centerAligned) TextAlign.Center else TextAlign.Start

    val infiniteTransition = rememberInfiniteTransition(label = "syllableGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    val cardBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        ),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBackground)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (customStyle.centerAligned) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            // Header: Live Preview Pill + Song Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Starlight Echoes • ChordPlay",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Live Preview Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "LIVE PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Line 1: Preceding line
            Text(
                text = "Hold on to what you love",
                fontFamily = fontFamily,
                fontSize = baseFontSize * 0.90f,
                lineHeight = baseLineHeight * 0.90f,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
                color = if (lyricsBlur) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            // Line 2: Active singing line with Syllable Sync Glow
            val activeColor = if (syncedLyrics) {
                MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)
            } else {
                Color.White
            }
            Text(
                text = "Before the moment fades away...",
                fontFamily = fontFamily,
                fontSize = baseFontSize,
                lineHeight = baseLineHeight,
                fontWeight = customStyle.weightChoice.weight,
                textAlign = textAlign,
                color = activeColor,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            // Line 3: Following line
            Text(
                text = "And the music takes you home.",
                fontFamily = fontFamily,
                fontSize = baseFontSize * 0.90f,
                lineHeight = baseLineHeight * 0.90f,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
                color = if (lyricsBlur) Color.White.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.50f),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            // Footer summary of typography settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Font: ${customStyle.customFontName ?: customStyle.fontChoice.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.70f),
                )
                Text(
                    text = "Scale: ${customStyle.sizeScale.label} • ${if (customStyle.centerAligned) "Center" else "Left"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.70f),
                )
            }
        }
    }
}
