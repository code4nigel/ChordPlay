package com.music.bitchord.ui.player

import com.music.bitchord.sharedui.resources.*

import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import org.jetbrains.compose.resources.painterResource
import kotlin.math.absoluteValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import com.music.bitchord.ui.components.optimizedHazeEffect
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import com.music.bitchord.data.canvas.CanvasArtwork
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.LandscapeFontScale
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.icons.BitChordIcons
import org.jetbrains.compose.resources.stringResource

/**
 * Below this height the landscape player tightens its right column — smaller
 * gaps — so a phone on its side or widescreen head unit fits comfortably.
 */
private val LANDSCAPE_COMPACT_HEIGHT = 440.dp

private const val LANDSCAPE_PANE_FADE_IN_MS = 220
private const val LANDSCAPE_PANE_FADE_IN_DELAY_MS = 80
private const val LANDSCAPE_PANE_FADE_OUT_MS = 140

/** Which of the content panes the landscape right side is showing. */
internal enum class PlayerPane { Main, Lyrics, Queue }

/** The landscape sleeve's corner radius, at rest. */
internal val LANDSCAPE_ART_CORNER = 0.dp

/**
 * Modernized ChordPlay Landscape / In-Car / Android Auto Player Layout.
 *
 * Left side: Full-bleed edge-to-edge artwork / animated canvas dissolving into the
 * dynamic mesh gradient, with a floating action pill (Like, Add to Playlist, More) at bottom-left.
 *
 * Right side: Centered player deck with large legible typography, waveform scrubber,
 * glowing ripple transport orb deck, and far-right vertical floating glass rail.
 *
 * Top bar: Left minimize button (chevron down), center empty, and right Spotify Canvas toggle.
 *
 * Supports vertical drag gestures on the right side to seamlessly flip between:
 * Main ↕ Lyrics ↕ Queue.
 */
@Composable
internal fun LandscapePlayerLayout(
    pane: PlayerPane,
    background: @Composable (Modifier) -> Unit,
    /** The sleeve/artwork container. Given full-bleed bounds. */
    artwork: @Composable (Modifier) -> Unit,
    actions: @Composable () -> Unit = {},
    artworkActions: @Composable () -> Unit = {},
    topBar: @Composable () -> Unit = {},
    mainPane: @Composable (compact: Boolean) -> Unit,
    lyricsPane: @Composable () -> Unit,
    queuePane: @Composable () -> Unit,
    onSelectPane: (PlayerPane) -> Unit = {},
    onOpenPipeline: (() -> Unit)? = null,
    palette: MeshPalette = remember { MeshPalette(emptyList()) },
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()

    Box(modifier = modifier.fillMaxSize()) {
        // Dynamic atmospheric background (Mesh Gradient + Synesthesia Nocturne ribbons)
        background(Modifier.fillMaxSize())

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxHeight < LANDSCAPE_COMPACT_HEIGHT

            // 1. Left Hero Area: Edge-to-edge full bleed artwork (0 padding on left/top/bottom)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.50f)
                    .align(Alignment.CenterStart),
            ) {
                artwork(Modifier.fillMaxSize())

                // Frosted action capsule centered at bottom of album art area
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp),
                ) {
                    artworkActions()
                }
            }

            // 2. Right Deck Area: Player Deck & Far-Right Floating Side Rail
            val deckTopPadding by animateDpAsState(
                targetValue = when (pane) {
                    PlayerPane.Lyrics -> 0.dp
                    PlayerPane.Queue -> if (compact) 8.dp else 14.dp
                    PlayerPane.Main -> if (compact) 40.dp else 48.dp
                },
                animationSpec = tween(280),
                label = "deckTopPadding",
            )

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.52f)
                    .align(Alignment.CenterEnd)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.End + WindowInsetsSides.Bottom,
                        ),
                    )
                    .padding(
                        top = deckTopPadding,
                        bottom = if (compact) 8.dp else 16.dp,
                        end = if (compact) 10.dp else 18.dp,
                        start = if (compact) 8.dp else 16.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Right Content Pane with vertical swipe gesture detection
                var dragAccumulatorY by remember { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(pane) {
                            detectVerticalDragGestures(
                                onDragStart = { dragAccumulatorY = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragAccumulatorY += dragAmount
                                },
                                onDragEnd = {
                                    val swipeThreshold = 55.dp.toPx()
                                    if (dragAccumulatorY < -swipeThreshold) {
                                        // Swiped UP -> Forward
                                        when (pane) {
                                            PlayerPane.Main -> {
                                                haptics.play(Haptic.Tap)
                                                onSelectPane(PlayerPane.Lyrics)
                                            }
                                            PlayerPane.Lyrics -> {
                                                haptics.play(Haptic.Tap)
                                                onSelectPane(PlayerPane.Queue)
                                            }
                                            PlayerPane.Queue -> {}
                                        }
                                    } else if (dragAccumulatorY > swipeThreshold) {
                                        // Swiped DOWN -> Backward
                                        when (pane) {
                                            PlayerPane.Queue -> {
                                                haptics.play(Haptic.Tap)
                                                onSelectPane(PlayerPane.Lyrics)
                                            }
                                            PlayerPane.Lyrics -> {
                                                haptics.play(Haptic.Tap)
                                                onSelectPane(PlayerPane.Main)
                                            }
                                            PlayerPane.Main -> {}
                                        }
                                    }
                                    dragAccumulatorY = 0f
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = pane,
                        transitionSpec = {
                            fadeIn(
                                tween(
                                    LANDSCAPE_PANE_FADE_IN_MS,
                                    delayMillis = LANDSCAPE_PANE_FADE_IN_DELAY_MS,
                                ),
                            ) togetherWith fadeOut(tween(LANDSCAPE_PANE_FADE_OUT_MS))
                        },
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                        label = "landscapePaneTransition",
                    ) { shown ->
                        when (shown) {
                            PlayerPane.Main -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                mainPane(compact)
                            }
                            PlayerPane.Lyrics -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                lyricsPane()
                            }
                            PlayerPane.Queue -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                queuePane()
                            }
                        }
                    }
                }

                Spacer(Modifier.width(if (compact) 8.dp else 14.dp))

                // 3. Far-Right Floating Frosted Glass Rail
                LandscapeSideRail(
                    currentPane = pane,
                    onSelectPane = onSelectPane,
                    onOpenPipeline = onOpenPipeline,
                    palette = palette,
                    compact = compact,
                )
            }

            // 4. Top Bar: Left Minimize Button
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                        ),
                    ),
            ) {
                topBar()
            }
        }
    }
}

/**
 * Far-Right Floating Frosted Glass Rail with adaptive multi-stop gradient (no outer or inner borders)
 * and elastic Apple-style squash-and-stretch spring selection indicator.
 */
@Composable
internal fun LandscapeSideRail(
    currentPane: PlayerPane,
    onSelectPane: (PlayerPane) -> Unit,
    onOpenPipeline: (() -> Unit)?,
    palette: MeshPalette,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // 1. Adaptive Song Palette Colors
    val c1 = palette.colors.getOrNull(0) ?: MaterialTheme.colorScheme.primary
    val accentColor = c1
    val c2 = palette.colors.getOrNull(1) ?: palette.colors.getOrNull(0) ?: MaterialTheme.colorScheme.secondary
    val c3 = palette.colors.getOrNull(2) ?: Color.Black

    val animC1 by animateColorAsState(targetValue = c1, animationSpec = tween(650), label = "railC1")
    val animC2 by animateColorAsState(targetValue = c2, animationSpec = tween(650), label = "railC2")
    val animC3 by animateColorAsState(targetValue = c3, animationSpec = tween(650), label = "railC3")

    val dockAdaptiveGradient = remember(animC1, animC2, animC3) {
        Brush.verticalGradient(
            0.0f to animC1.copy(alpha = 0.40f),
            0.48f to animC2.copy(alpha = 0.22f),
            1.0f to animC3.copy(alpha = 0.45f),
        )
    }
    val dockGlassSheen = remember {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.02f),
                Color.Black.copy(alpha = 0.16f),
            ),
        )
    }

    // 2. Elastic Jelly Indicator Physics
    val selectedIndex = when (currentPane) {
        PlayerPane.Main -> 0
        PlayerPane.Lyrics -> 1
        PlayerPane.Queue -> 2
    }

    val itemSize = if (compact) 38.dp else 42.dp
    val itemSpacing = if (compact) 8.dp else 12.dp
    val targetOffset = (itemSize + itemSpacing) * selectedIndex

    val animatedOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = 0.62f, // Bouncy elastic Apple-like spring
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "elasticIndicatorY",
    )

    // Dynamic squash and stretch: stretches along velocity axis in flight, squashes upon landing
    val deltaY = (targetOffset - animatedOffset).value
    val stretch = (deltaY.absoluteValue * 0.38f).coerceAtMost(16f)
    val indicatorHeight = itemSize + stretch.dp
    val indicatorWidth = (itemSize.value - stretch * 0.25f).coerceAtLeast(32f).dp

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(dockAdaptiveGradient)
            .background(dockGlassSheen)
            .padding(vertical = if (compact) 8.dp else 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Elastic selection pill indicator gliding behind the 3 pane icons
        Box(
            modifier = Modifier
                .offset(y = animatedOffset - (stretch * 0.5f).dp)
                .size(width = indicatorWidth, height = indicatorHeight)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.32f),
                            Color.White.copy(alpha = 0.16f),
                        ),
                    ),
                ),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 1. Now Playing
            SideRailItem(
                icon = Icons.Rounded.Album,
                selected = currentPane == PlayerPane.Main,
                contentDescription = "Now Playing",
                itemSize = itemSize,
                onClick = { onSelectPane(PlayerPane.Main) },
            )

            // 2. Lyrics
            SideRailItem(
                icon = Icons.Rounded.FormatQuote,
                selected = currentPane == PlayerPane.Lyrics,
                contentDescription = "Lyrics",
                itemSize = itemSize,
                onClick = { onSelectPane(PlayerPane.Lyrics) },
            )

            // 3. Queue
            SideRailItem(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                selected = currentPane == PlayerPane.Queue,
                contentDescription = "Queue",
                itemSize = itemSize,
                onClick = { onSelectPane(PlayerPane.Queue) },
            )

            // 4. Audio Pipeline Button
            if (onOpenPipeline != null) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .width(20.dp)
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.15f)),
                )
                SideRailItem(
                    icon = Icons.Rounded.GraphicEq,
                    selected = false,
                    contentDescription = "Audio Pipeline",
                    itemSize = itemSize,
                    onClick = onOpenPipeline,
                )
            }
        }
    }
}

@Composable
private fun SideRailItem(
    icon: ImageVector,
    selected: Boolean,
    contentDescription: String,
    itemSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val iconTint by animateColorAsState(
        targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.48f),
        animationSpec = tween(220),
        label = "SideRailIconTint",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.10f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "SideRailIconScale",
    )

    Box(
        modifier = modifier
            .size(itemSize)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                haptics.play(Haptic.Tap)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier
                .size((itemSize.value * 0.52f).dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
    }
}

/**
 * Modernized Landscape Artwork.
 *
 * Edge-to-edge full bleed coverage on left, top, and bottom, with horizontal gradient
 * alpha dissolve on the right edge, smoothly fading into the dynamic mesh background.
 */
@Composable
internal fun LandscapeArtwork(
    artRequest: ImageRequest,
    artLoaded: Boolean,
    onArtState: (AsyncImagePainter.State) -> Unit,
    canvas: CanvasArtwork?,
    canvasRendered: Boolean,
    isPlaying: Boolean,
    onCanvasRenderedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pausedForTransition: Boolean = false,
    positionMs: Long = 0L,
    sleeveShape: () -> Shape = { RoundedCornerShape(0.dp) },
    shadowFraction: () -> Float = { 0f },
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(sleeveShape())
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    // Dissolve right edge smoothly into transparency
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0.00f to Color.Black,
                            0.40f to Color.Black,
                            0.96f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (!artLoaded && !canvasRendered) {
                Icon(
                    imageVector = BitChordIcons.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxSize(0.28f),
                )
            }
            AsyncImage(
                model = artRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onState = onArtState,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent { if (artLoaded || !canvasRendered) drawContent() },
            )
            canvas?.let { clip ->
                CanvasArtworkPlayer(
                    canvas = clip,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    pausedForTransition = pausedForTransition,
                    onRenderedChanged = onCanvasRenderedChange,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Overlay elements (e.g. nerd stats)
        overlay()
    }
}

/**
 * Modernized ChordPlay Landscape Main Player Deck.
 */
@Composable
internal fun LandscapeMainPane(
    compact: Boolean,
    credits: @Composable () -> Unit,
    lyricStrip: (@Composable () -> Unit)?,
    scrubber: @Composable () -> Unit,
    transport: @Composable () -> Unit,
    volume: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .padding(horizontal = if (compact) 4.dp else 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        credits()
        Spacer(Modifier.height(if (compact) 4.dp else 10.dp))

        lyricStrip?.invoke()
        Spacer(Modifier.height(if (compact) 4.dp else 12.dp))

        scrubber()
        Spacer(Modifier.height(if (compact) 6.dp else 14.dp))

        transport()

        if (volume != null) {
            Spacer(Modifier.height(if (compact) 4.dp else 10.dp))
            volume()
        }
    }
}

/**
 * Modernized Title and Artist credits taking 100% of the horizontal space on the player deck.
 */
@Composable
internal fun LandscapeCredits(
    song: Song,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (browseId: String?, name: String) -> Unit,
    fontScale: LandscapeFontScale = LandscapeFontScale.LARGE,
    modifier: Modifier = Modifier,
) {
    val scale = fontScale.scaleFactor
    val titleFontSize = (24 * scale).sp
    val artistFontSize = (17 * scale).sp

    Crossfade(
        targetState = song.title to song.artist,
        animationSpec = tween(durationMillis = 300),
        modifier = modifier.fillMaxWidth(),
        label = "landscapeCredits",
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            var titleOverflowing by remember { mutableStateOf(false) }
            MarqueeText(
                text = song.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    lineHeight = (titleFontSize.value * 1.2f).sp,
                ),
                color = Color.White,
                onOverflowChange = { titleOverflowing = it },
                leading = if (song.isExplicit == true) {
                    { ExplicitBadge(Color.White) }
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .opensPage(song.albumId, onOpenAlbum),
            )
            Spacer(Modifier.height(3.dp))
            ArtistCreditsMarquee(
                text = song.artist,
                credits = remember(song.artist, song.artists, song.artistId) {
                    artistCredits(song.artist, song.artists, song.artistId)
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = artistFontSize,
                    fontWeight = FontWeight.Medium,
                    lineHeight = (artistFontSize.value * 1.25f).sp,
                ),
                color = Color.White.copy(alpha = 0.65f),
                startDelayMillis = if (titleOverflowing) MARQUEE_ARTIST_STAGGER_MS else 0L,
                onOpenArtist = onOpenArtist,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Frosted Glass Action Capsule positioned at the bottom-center of the album artwork.
 * Houses: Spotify Canvas toggle, Heart (Like), Add to Playlist, Three Dots menu,
 * and dynamically adds Shuffle & Repeat (when not on Main pane) and Autoplay (when on Queue pane).
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
internal fun LandscapeArtworkActions(
    song: Song,
    signedIn: Boolean,
    likeStatus: LikeStatus,
    showRevertCue: Boolean,
    animatedCanvas: Boolean,
    onToggleAnimatedCanvas: () -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    onOpenMenu: () -> Unit,
    palette: MeshPalette = remember { MeshPalette(emptyList()) },
    currentPane: PlayerPane = PlayerPane.Main,
    shuffleEnabled: Boolean = false,
    repeatMode: Int = RepeatModes.OFF,
    autoplayEnabled: Boolean = false,
    onToggleShuffle: (() -> Unit)? = null,
    onCycleRepeat: (() -> Unit)? = null,
    onToggleAutoplay: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    val haptics = rememberHaptics()
    val haze = hazeState ?: LocalPlayerHazeState.current

    val c1 = palette.colors.getOrNull(0) ?: MaterialTheme.colorScheme.primary
    val accentColor = c1
    val c2 = palette.colors.getOrNull(1) ?: palette.colors.getOrNull(0) ?: MaterialTheme.colorScheme.secondary
    val c3 = palette.colors.getOrNull(2) ?: Color.Black

    val animC1 by animateColorAsState(targetValue = c1, animationSpec = tween(650), label = "artActionsC1")
    val animC2 by animateColorAsState(targetValue = c2, animationSpec = tween(650), label = "artActionsC2")
    val animC3 by animateColorAsState(targetValue = c3, animationSpec = tween(650), label = "artActionsC3")

    val dockAdaptiveGradient = remember(animC1, animC2, animC3) {
        Brush.horizontalGradient(
            0.0f to animC1.copy(alpha = 0.40f),
            0.48f to animC2.copy(alpha = 0.22f),
            1.0f to animC3.copy(alpha = 0.45f),
        )
    }
    val dockGlassSheen = remember {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.02f),
                Color.Black.copy(alpha = 0.16f),
            ),
        )
    }

    Row(
        modifier = modifier
            .animateContentSize()
            .clip(CircleShape)
            .then(
                if (haze != null) {
                    Modifier.optimizedHazeEffect(
                        state = haze,
                        style = HazeMaterials.ultraThin(Color.White.copy(alpha = 0.04f)),
                    )
                } else {
                    Modifier
                }
            )
            .background(dockAdaptiveGradient)
            .background(dockGlassSheen)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. Spotify Canvas Toggle Button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        haptics.play(Haptic.Select)
                        onToggleAnimatedCanvas()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            val spotifyGreen = Color(0xFF1DB954)
            val iconTint = if (animatedCanvas) spotifyGreen else Color.White.copy(alpha = 0.45f)
            Icon(
                painter = painterResource(Res.drawable.spotify_logo),
                contentDescription = if (animatedCanvas) "Turn off Spotify Canvas" else "Turn on Spotify Canvas",
                tint = iconTint,
                modifier = Modifier
                    .size(20.dp)
                    .then(
                        if (!animatedCanvas) {
                            Modifier.drawWithContent {
                                drawContent()
                                val pad = 2.dp.toPx()
                                val strokeWidth = 1.8.dp.toPx()
                                drawLine(
                                    color = Color.White.copy(alpha = 0.55f),
                                    start = androidx.compose.ui.geometry.Offset(pad, pad),
                                    end = androidx.compose.ui.geometry.Offset(size.width - pad, size.height - pad),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round,
                                )
                            }
                        } else {
                            Modifier
                        },
                    ),
            )
        }

        // 2. Heart (Like / Unlike)
        if (signedIn && song.localUri == null) {
            val liked = likeStatus == LikeStatus.LIKE
            val likeBurst = LocalLikeBurstState.current
            Box(
                modifier = Modifier
                    .likeButtonTarget(likeBurst)
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            val isLiked = likeStatus == LikeStatus.LIKE
                            haptics.play(if (isLiked) Haptic.ToggleOff else Haptic.ToggleOn)
                            if (likeBurst != null) {
                                likeBurst.launchFromButton(isLiked, onToggleLike)
                            } else {
                                onToggleLike()
                            }
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (liked) BitChordIcons.HeartFilled else BitChordIcons.Heart,
                    contentDescription = stringResource(
                        if (liked) Res.string.remove_from_liked else Res.string.like,
                    ),
                    tint = if (liked) accentColor else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // 3. Add to Playlist
        if (onAddToPlaylist != null && song.localUri == null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            haptics.play(Haptic.Tap)
                            onAddToPlaylist()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = stringResource(Res.string.add_to_playlist),
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // 4. Shuffle & Repeat buttons (visible on Lyrics & Queue panes)
        AnimatedVisibility(
            visible = currentPane != PlayerPane.Main,
            enter = fadeIn(tween(200)) + expandHorizontally(),
            exit = fadeOut(tween(160)) + shrinkHorizontally(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Shuffle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                haptics.play(if (shuffleEnabled) Haptic.ToggleOff else Haptic.ToggleOn)
                                onToggleShuffle?.invoke()
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = BitChordIcons.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) accentColor else Color.White.copy(alpha = 0.50f),
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Repeat
                val isRepeatActive = repeatMode != RepeatModes.OFF
                val repeatIcon = if (repeatMode == RepeatModes.ONE) Icons.Rounded.RepeatOne else BitChordIcons.Repeat
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                haptics.play(Haptic.Select)
                                onCycleRepeat?.invoke()
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = if (isRepeatActive) accentColor else Color.White.copy(alpha = 0.50f),
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Autoplay (visible on Queue pane)
                AnimatedVisibility(
                    visible = currentPane == PlayerPane.Queue,
                    enter = fadeIn(tween(200)) + expandHorizontally(),
                    exit = fadeOut(tween(160)) + shrinkHorizontally(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    haptics.play(if (autoplayEnabled) Haptic.ToggleOff else Haptic.ToggleOn)
                                    onToggleAutoplay?.invoke()
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = BitChordIcons.Infinity,
                            contentDescription = stringResource(
                                if (autoplayEnabled) Res.string.autoplay_on else Res.string.autoplay_off,
                            ),
                            tint = if (autoplayEnabled) accentColor else Color.White.copy(alpha = 0.50f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        // 5. Three dots (More / Undo)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        onOpenMenu()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (showRevertCue) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.MoreHoriz,
                contentDescription = stringResource(Res.string.more),
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * Landscape Lyrics Pane: full scrolling synced lyrics sheet with status and language options.
 */
@Composable
internal fun LandscapeLyricsPane(
    hasLyrics: Boolean,
    placeholder: String,
    status: String,
    onStatusClick: () -> Unit,
    picking: Boolean = false,
    pickBar: @Composable () -> Unit = {},
    showingRomanization: Boolean = false,
    romanizationState: LyricsTranslationUiState = LyricsTranslationUiState.Idle,
    onToggleRomanization: () -> Unit = {},
    showingTranslation: Boolean = false,
    translationState: LyricsTranslationUiState = LyricsTranslationUiState.Idle,
    onToggleTranslation: () -> Unit = {},
    onOpenStyle: () -> Unit = {},
    palette: MeshPalette = remember { MeshPalette(emptyList()) },
    modifier: Modifier = Modifier,
    panel: @Composable (Modifier) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    val topFade = 44.dp.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startY = 0f,
                            endY = topFade,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            if (hasLyrics) {
                panel(Modifier.fillMaxSize())
            } else {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
        if (picking) {
            Box(Modifier.fillMaxWidth()) { pickBar() }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Bottom-left: Lyrics provider attribution / status
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    LyricsStatusWithChange(
                        status = status,
                        onStatusClick = onStatusClick,
                    )
                }
                // Bottom-right: Language (Globe), Style (TT), and Translation (文A) in unified glassy capsule
                if (hasLyrics) {
                    GlassyLyricsActions(
                        showingRomanization = showingRomanization,
                        romanizationState = romanizationState,
                        onToggleRomanization = onToggleRomanization,
                        showingTranslation = showingTranslation,
                        translationState = translationState,
                        onToggleTranslation = onToggleTranslation,
                        onOpenStyle = onOpenStyle,
                        palette = palette,
                    )
                }
            }
        }
    }
}
