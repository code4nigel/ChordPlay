package com.music.bitchord.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.NerdStats
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.AudioQuality
import com.music.bitchord.sharedui.resources.*
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Open, airy, and immersive transport controls for ChordPlay Modern mode.
 * The buttons float unconstrained directly over the atmospheric background with
 * generous breathing room and an organic, luminous Play/Pause halo aura.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun ChordPlayTransportDeck(
    isPlaying: Boolean,
    isLoading: Boolean,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()

    val prevInteraction = remember { MutableInteractionSource() }
    val isPrevPressed by prevInteraction.collectIsPressedAsState()
    val prevScale by animateFloatAsState(
        targetValue = if (isPrevPressed) 0.88f else 1f,
        animationSpec = tween(120),
        label = "PrevScale",
    )
    val prevAlpha by animateFloatAsState(
        targetValue = if (previousEnabled) 1f else 0.35f,
        label = "PrevAlpha",
    )

    val nextInteraction = remember { MutableInteractionSource() }
    val isNextPressed by nextInteraction.collectIsPressedAsState()
    val nextScale by animateFloatAsState(
        targetValue = if (isNextPressed) 0.88f else 1f,
        animationSpec = tween(120),
        label = "NextScale",
    )
    val nextAlpha by animateFloatAsState(
        targetValue = if (nextEnabled) 1f else 0.35f,
        label = "NextAlpha",
    )

    val playInteraction = remember { MutableInteractionSource() }
    val isPlayPressed by playInteraction.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.92f else 1f,
        animationSpec = tween(120),
        label = "PlayScale",
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    // Open, airy transport row with generous breathing space across the canvas
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Previous Track
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = prevScale
                    scaleY = prevScale
                }
                .clickable(
                    interactionSource = prevInteraction,
                    indication = null,
                    enabled = previousEnabled,
                ) {
                    haptics.play(Haptic.SkipPrevious)
                    onPrevious()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_player_previous),
                contentDescription = stringResource(Res.string.widget_previous),
                tint = Color.White.copy(alpha = prevAlpha),
                modifier = Modifier.size(36.dp),
            )
        }

        // Central Organic Glowing Play/Pause Halo
        Box(
            modifier = Modifier
                .size(70.dp)
                .graphicsLayer {
                    scaleX = playScale
                    scaleY = playScale
                }
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = primaryColor.copy(alpha = 0.75f),
                    ambientColor = primaryColor.copy(alpha = 0.4f),
                )
                .clip(CircleShape)
                .background(primaryColor)
                .clickable(
                    interactionSource = playInteraction,
                    indication = null,
                ) {
                    haptics.play(if (isPlaying) Haptic.Pause else Haptic.Resume)
                    onPlayPause()
                },
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(30.dp),
                )
            } else {
                Icon(
                    painter = painterResource(
                        if (isPlaying) Res.drawable.ic_player_pause else Res.drawable.ic_player_play,
                    ),
                    contentDescription = stringResource(if (isPlaying) Res.string.pause else Res.string.play),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        // Next Track
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = nextScale
                    scaleY = nextScale
                }
                .clickable(
                    interactionSource = nextInteraction,
                    indication = null,
                    enabled = nextEnabled,
                ) {
                    haptics.play(Haptic.SkipNext)
                    onNext()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_player_next),
                contentDescription = stringResource(Res.string.widget_next),
                tint = Color.White.copy(alpha = nextAlpha),
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

/**
 * Compact audio fidelity badge pill designed to sit seamlessly inline inside the seekbar
 * between the start and end timestamps. Clicking it triggers the audio pipeline sheet.
 */
@Composable
fun ChordPlayAudioFidelityBadge(
    song: Song,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val wifiQuality by PlayerSettings.audioQualityWifi.collectAsStateWithLifecycle()
    val cellularQuality by PlayerSettings.audioQualityCellular.collectAsStateWithLifecycle()
    val metered by PlayerSettings.meteredConnection.collectAsStateWithLifecycle()
    val nerdStats by NerdStats.current.collectAsStateWithLifecycle()
    val racingLossless by NerdStats.racingLossless.collectAsStateWithLifecycle()

    val effectiveQuality = if (metered == true) cellularQuality else wifiQuality
    val stillRacing = song.videoId in racingLossless

    val badgeText = when {
        isLoading || stillRacing -> "OPTIMIZING"
        nerdStats?.isHiRes == true -> "HI-RES LOSSLESS"
        nerdStats?.isLossless == true -> "LOSSLESS FLAC"
        nerdStats?.isDolbyAtmos == true -> "DOLBY ATMOS"
        nerdStats?.bitrateKbps != null -> {
            val codec = nerdStats?.mimeType?.substringAfterLast("/")?.uppercase() ?: "AUDIO"
            "${nerdStats?.bitrateKbps}K $codec"
        }
        effectiveQuality == AudioQuality.LOSSLESS -> "HD LOSSLESS"
        else -> "HIGH FIDELITY"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(0.75.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                haptics.play(Haptic.Tap)
                onClick()
            }
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(11.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = badgeText,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
        )
    }
}
