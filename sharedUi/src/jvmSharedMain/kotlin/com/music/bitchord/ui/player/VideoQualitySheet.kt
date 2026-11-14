package com.music.bitchord.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.VideoQuality
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState

/**
 * Modern ChordPlay Video Streaming Quality Selector.
 * Allows switching between 1080p FHD, 720p HD, 480p SD, and 360p SD.
 */
@Composable
internal fun VideoQualitySheet(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
) {
    val haptics = rememberHaptics()
    val activeQuality by PlayerSettings.videoQuality.collectAsStateWithLifecycle()
    val effectiveAccent = accentColor ?: Color(0xFFFF5722)

    PlayerDrawer(
        hazeState = hazeState,
        title = "Video Quality",
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Select streaming resolution for music videos",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )

            VideoQuality.entries.forEach { quality ->
                val isSelected = quality == activeQuality
                val subtitle = when (quality) {
                    VideoQuality.Q_1080P -> "1080p Full HD • Crisp details, best for Wi-Fi"
                    VideoQuality.Q_720P -> "720p High Definition • Standard & smooth"
                    VideoQuality.Q_480P -> "480p Standard Definition • Balanced data usage"
                    VideoQuality.Q_360P -> "360p Data Saver • Lowest data consumption"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ROW_SHAPE)
                        .background(
                            if (isSelected) effectiveAccent.copy(alpha = 0.16f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 1.dp,
                                    color = effectiveAccent.copy(alpha = 0.45f),
                                    shape = ROW_SHAPE,
                                )
                            } else Modifier
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            haptics.play(Haptic.Select)
                            PlayerPlatform.host.settings.setVideoQuality(quality)
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) effectiveAccent.copy(alpha = 0.25f)
                                else Color.White.copy(alpha = 0.08f)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = when (quality) {
                                VideoQuality.Q_1080P -> Icons.Rounded.HighQuality
                                VideoQuality.Q_720P -> Icons.Rounded.Hd
                                else -> Icons.Rounded.VideoLibrary
                            },
                            contentDescription = null,
                            tint = if (isSelected) effectiveAccent else Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = quality.label,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                ),
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.88f),
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isSelected) effectiveAccent.copy(alpha = 0.28f)
                                        else Color.White.copy(alpha = 0.10f)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    text = quality.badge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) effectiveAccent else Color.White.copy(alpha = 0.65f),
                                )
                            }
                        }

                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }

                    if (isSelected) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(effectiveAccent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = Color.Black,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

