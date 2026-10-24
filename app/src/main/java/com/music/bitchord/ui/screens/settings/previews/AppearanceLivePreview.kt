package com.music.bitchord.ui.screens.settings.previews

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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.ThemeMode

/**
 * Pinned live interactive appearance card showing how the active theme,
 * frosted glass styling, and blur settings affect the UI in real time.
 */
@Composable
fun AppearanceLivePreview(
    modifier: Modifier = Modifier,
) {
    val theme by AppSettings.themeMode.collectAsStateWithLifecycle()
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()

    val isLight = theme == ThemeMode.LIGHT

    val containerBg = when {
        isLight && reduceDynamicBlur -> Color(0xFFF0F2F6)
        isLight -> Color(0xFFF7F8FA).copy(alpha = 0.88f)
        reduceDynamicBlur -> Color(0xFF14161C)
        else -> Color(0xFF161922).copy(alpha = 0.78f)
    }

    val textColor = if (isLight) Color(0xFF1A1C20) else Color.White
    val subtextColor = if (isLight) Color(0xFF5A5E68) else Color.White.copy(alpha = 0.65f)
    val borderColor = when {
        liquidGlass -> MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
        isLight -> Color.Black.copy(alpha = 0.08f)
        else -> Color.White.copy(alpha = 0.12f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(containerBg)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Theme Label + LIVE PREVIEW Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Player Surface Demo",
                    style = MaterialTheme.typography.labelSmall,
                    color = subtextColor,
                )

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

            Spacer(Modifier.height(14.dp))

            // Miniature Player Card Preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isLight) Color.White.copy(alpha = 0.70f)
                        else Color.White.copy(alpha = 0.06f),
                    )
                    .border(
                        1.dp,
                        if (isLight) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(14.dp),
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Mock Artwork Thumbnail
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(Modifier.width(14.dp))

                // Track and Artist Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Midnight Resonance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "ChordPlay Artists • Ambient Glass",
                        style = MaterialTheme.typography.bodySmall,
                        color = subtextColor,
                    )
                }

                // Mini controls
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            // Subtitle status badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isLight) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Mode: ${theme.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = subtextColor,
                )
                Text(
                    text = "Glass: ${if (liquidGlass) "Liquid Enabled" else "Standard"} • Blur: ${if (reduceDynamicBlur) "Reduced" else "Dynamic"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = subtextColor,
                )
            }
        }
    }
}
