package com.music.bitchord.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.request.ImageRequest
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.icons.BitChordIcons
import com.music.bitchord.ui.theme.HideSystemBars
import com.music.bitchord.ui.components.optimizedHazeEffect
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.delay

/** How long the controls stay over a playing video untouched. */
private const val VIDEO_CONTROLS_IDLE_MS = 3_500L

/**
 * A music video on a phone held sideways: the picture is the whole screen,
 * letterboxed in black, with the system bars away. A tap brings the controls
 * up over it — the title, the scrubber and the transport — and they stand
 * down again a few seconds after the last touch while it plays, as in every
 * video player. [onExitFullscreen] goes back to the ordinary landscape player.
 */
@Composable
internal fun FullscreenVideoPlayer(
    song: Song,
    isPlaying: Boolean,
    artRequest: ImageRequest,
    onRenderedChanged: (Boolean) -> Unit,
    rendered: Boolean,
    onExitFullscreen: () -> Unit,
    onOpenQuality: () -> Unit,
    scrubber: @Composable () -> Unit,
    transport: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    HideSystemBars(true)
    var controlsShown by remember { mutableStateOf(true) }
    // Bumped by every touch on the controls, so the idle timer starts over.
    var touches by remember { mutableIntStateOf(0) }
    LaunchedEffect(controlsShown, isPlaying, touches) {
        if (controlsShown && isPlaying) {
            delay(VIDEO_CONTROLS_IDLE_MS)
            controlsShown = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { controlsShown = !controlsShown },
    ) {
        // Black with a spinner until the picture's first frame lands.
        if (!rendered) VideoLoadingBackdrop(Modifier.fillMaxSize())
        MusicVideoSurface(
            videoId = song.videoId,
            fit = true,
            onRenderedChanged = onRenderedChanged,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = controlsShown,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.62f),
                            0.30f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.78f),
                        ),
                    )
                    // Any touch on the controls starts the idle timer over.
                    // Watched, not taken: a tap on bare picture still reaches
                    // the box below and puts them away.
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                touches++
                            }
                        }
                    }
                    .windowInsetsPadding(WindowInsets.displayCutout)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = song.artist,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    VideoCornerControls(
                        fullscreen = true,
                        onToggleFullscreen = onExitFullscreen,
                        onOpenQuality = onOpenQuality,
                        onEnterPip = { PlayerPlatform.host.enterPip() },
                        pipSupported = PlayerPlatform.host.pipSupported,
                        showFullBleedToggle = false,
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .widthIn(max = 420.dp),
                ) {
                    transport()
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    scrubber()
                }
            }
        }
    }
}

/**
 * What stands in for a music video until its first frame lands: black, with a
 * spinner, rather than the still artwork. Shared by the portrait card and the
 * fullscreen landscape player.
 */
@Composable
internal fun VideoLoadingBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 3.dp,
            modifier = Modifier.size(36.dp),
        )
    }
}

/**
 * Frosted glass capsule dock combining Video Quality selector, Aspect Ratio Fit/Fill toggle,
 * Edge-to-Edge full-bleed toggle, Picture-in-Picture, and Landscape Fullscreen into one
 * cohesive translucent UI matching ChordPlay's glass design language.
 */
/**
 * Shared frosted glass pill styling used by [VideoControlsDock], [GlassyLyricsActions],
 * player header action capsules, and the landscape minimize button.
 */
@Composable
internal fun Modifier.videoDockGlassPill(
    hazeState: HazeState? = null,
): Modifier {
    val dockGradient = remember {
        Brush.horizontalGradient(
            0.0f to Color.White.copy(alpha = 0.12f),
            0.48f to Color.White.copy(alpha = 0.06f),
            1.0f to Color.White.copy(alpha = 0.14f),
        )
    }
    val dockSheen = remember {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.04f),
                Color.Transparent,
                Color.White.copy(alpha = 0.04f),
            ),
        )
    }
    val borderBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.08f),
            ),
        )
    }
    return this
        .clip(CircleShape)
        .then(
            if (hazeState != null) {
                Modifier.optimizedHazeEffect(
                    state = hazeState,
                    style = HazeMaterials.ultraThin(),
                )
            } else {
                Modifier.background(Color.Black.copy(alpha = 0.42f), CircleShape)
            },
        )
        .background(dockGradient)
        .background(dockSheen)
        .border(
            width = 0.6.dp,
            brush = borderBrush,
            shape = CircleShape,
        )
}

@Composable
internal fun VideoControlsDock(
    videoFill: Boolean = false,
    onToggleVideoFill: (() -> Unit)? = null,
    videoFullBleed: Boolean = false,
    onToggleVideoFullBleed: (() -> Unit)? = null,
    fullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
    onOpenQuality: () -> Unit = {},
    onEnterPip: (() -> Unit)? = null,
    pipSupported: Boolean = false,
    showFullBleedToggle: Boolean = true,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .videoDockGlassPill(hazeState = hazeState)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. Quality Pill
        VideoQualityBox(onClick = onOpenQuality)

        // Hairline separator
        Box(
            modifier = Modifier
                .width(0.8.dp)
                .height(14.dp)
                .background(Color.White.copy(alpha = 0.22f)),
        )

        // 2. Edge-to-Edge / Fill Video Toggle
        if (showFullBleedToggle && onToggleVideoFill != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleVideoFill,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = BitChordIcons.AspectRatio,
                    contentDescription = if (videoFill) "Fit Video" else "Fill Video",
                    tint = if (videoFill) Color.White else Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        // 4. Picture-in-Picture
        if (pipSupported && onEnterPip != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onEnterPip,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = BitChordIcons.PictureInPicture,
                    contentDescription = "Picture in Picture",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        // 5. Fullscreen Toggle
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleFullscreen,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                contentDescription = if (fullscreen) "Exit Fullscreen" else "Fullscreen",
                tint = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Backward-compatible wrapper delegating to [VideoControlsDock].
 */
@Composable
internal fun VideoCornerControls(
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onOpenQuality: () -> Unit,
    modifier: Modifier = Modifier,
    videoFill: Boolean = false,
    onToggleVideoFill: (() -> Unit)? = null,
    videoFullBleed: Boolean = false,
    onToggleVideoFullBleed: (() -> Unit)? = null,
    onEnterPip: (() -> Unit)? = null,
    pipSupported: Boolean = false,
    showFullBleedToggle: Boolean = true,
    hazeState: HazeState? = null,
) = VideoControlsDock(
    videoFill = videoFill,
    onToggleVideoFill = onToggleVideoFill,
    videoFullBleed = videoFullBleed,
    onToggleVideoFullBleed = onToggleVideoFullBleed,
    fullscreen = fullscreen,
    onToggleFullscreen = onToggleFullscreen,
    onOpenQuality = onOpenQuality,
    onEnterPip = onEnterPip,
    pipSupported = pipSupported,
    showFullBleedToggle = showFullBleedToggle,
    hazeState = hazeState,
    modifier = modifier,
)

/** What the quality box and drawer say is playing: the held height, else the one on screen. */
private fun MusicVideoQualityUi.selected(): Int? =
    if (held == null || held !in heights) activeHeight else held

/**
 * The playing video's quality: 360p, 480p, HD, FullHD, 2K, 4K — what is on
 * screen, which is not always what was picked (4K held, then a video shot in
 * 1080p).
 */
@Composable
private fun VideoQualityBox(onClick: () -> Unit) {
    val selectedQuality by PlayerSettings.videoQuality.collectAsStateWithLifecycle()
    val quality by PlayerPlatform.host.musicVideoQuality.collectAsStateWithLifecycle()
    val label = quality.held?.let { "${it}p" } ?: "${selectedQuality.maxHeight}p"
    Box(
        modifier = Modifier
            .height(28.dp)
            .widthIn(min = 38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = label,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
            label = "videoQualityLabel",
        ) { text ->
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                maxLines = 1,
            )
        }
    }
}

/**
 * The quality list, in the same drawer as the audio output: Auto, then every
 * height this video offers, the one playing marked. Picking one holds it from
 * now on — for the next video too, where that video has it.
 */
@Composable
internal fun MusicVideoQualityDrawer(hazeState: HazeState, onDismiss: () -> Unit) {
    val quality by PlayerPlatform.host.musicVideoQuality.collectAsStateWithLifecycle()
    val selected = quality.selected()
    PlayerDrawer(
        hazeState = hazeState,
        title = "Video Quality",
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            QualityRow(
                name = "Auto",
                detail = if (quality.held == null) selected?.let(::videoQualityLabel) else null,
                active = quality.held == null,
            ) {
                PlayerPlatform.host.setMusicVideoQuality(null)
                onDismiss()
            }
            quality.heights.forEach { height ->
                QualityRow(
                    name = videoQualityLabel(height),
                    detail = "${height}p",
                    active = quality.held != null && selected == height,
                ) {
                    PlayerPlatform.host.setMusicVideoQuality(height)
                    onDismiss()
                }
            }
        }
    }
}

/** One row of [MusicVideoQualityDrawer], drawn as the audio output drawer draws an output. */
@Composable
private fun QualityRow(name: String, detail: String?, active: Boolean, onSelect: () -> Unit) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ROW_SHAPE)
            .background(Color.White.copy(alpha = if (active) 0.10f else 0.05f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !active,
            ) {
                haptics.play(Haptic.Select)
                onSelect()
            }
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = Color.White.copy(alpha = if (active) 1f else 0.85f),
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (active) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
