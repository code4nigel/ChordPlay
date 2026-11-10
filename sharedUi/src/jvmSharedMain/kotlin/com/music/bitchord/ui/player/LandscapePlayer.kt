package com.music.bitchord.ui.player

import com.music.bitchord.sharedui.resources.*

import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlaylistAdd
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
 * transport, shorter gaps — so a phone or car head unit fits the player deck
 * without vertical clipping.
 */
private val LANDSCAPE_COMPACT_HEIGHT = 440.dp

private const val LANDSCAPE_PANE_FADE_IN_MS = 220
private const val LANDSCAPE_PANE_FADE_IN_DELAY_MS = 80
private const val LANDSCAPE_PANE_FADE_OUT_MS = 140

/** Which of the content panes the landscape right side is showing. */
internal enum class PlayerPane { Main, Lyrics, Queue }

/** The landscape sleeve's corner radius, at rest. */
internal val LANDSCAPE_ART_CORNER = 16.dp

/** The landscape sleeve's shape at rest. */
private val LandscapeArtShape = RoundedCornerShape(LANDSCAPE_ART_CORNER)

/** Room above the landscape columns for the sheet's drag handle. */
private val LANDSCAPE_HANDLE_STRIP = 20.dp

/**
 * Modernized ChordPlay Landscape / In-Car / Android Auto Player Layout.
 *
 * Left side: Edge-to-edge bleeding artwork / animated canvas dissolving into the
 * dynamic mesh gradient with an overlay audio pipeline bottom pill.
 *
 * Right side: Centered player deck with large legible typography, waveform scrubber,
 * glowing ripple transport orb deck, and far-right vertical floating glass rail.
 *
 * Supports vertical drag gestures on the right side to seamlessly flip between:
 * Main ↕ Lyrics ↕ Queue.
 */
@Composable
internal fun LandscapePlayerLayout(
    pane: PlayerPane,
    background: @Composable (Modifier) -> Unit,
    /** The sleeve/artwork container. Given full height bounds. */
    artwork: @Composable (Modifier) -> Unit,
    actions: @Composable () -> Unit = {},
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

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter,
        ) {
            val compact = maxHeight < LANDSCAPE_COMPACT_HEIGHT

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = LANDSCAPE_HANDLE_STRIP,
                        bottom = if (compact) 8.dp else 16.dp,
                    ),
            ) {
                // 1. Left Hero Area: Edge-to-edge bleeding artwork dissolving into the mesh
                Box(
                    modifier = Modifier
                        .weight(0.46f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    artwork(Modifier.fillMaxSize())
                }

                // 2. Right Deck Area: Player Deck & Far-Right Floating Side Rail
                Row(
                    modifier = Modifier
                        .weight(0.54f)
                        .fillMaxHeight()
                        .padding(
                            start = if (compact) 10.dp else 18.dp,
                            end = if (compact) 10.dp else 20.dp,
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
                    )
                }
            }

            // Top collapse drag handle
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .shadow(2.dp, RoundedCornerShape(2.dp), clip = false)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.60f)),
            )
        }
    }
}

/**
 * Far-Right Floating Frosted Glass Rail for fast 1-tap pane switching and audio pipeline access.
 */
@Composable
internal fun LandscapeSideRail(
    currentPane: PlayerPane,
    onSelectPane: (PlayerPane) -> Unit,
    onOpenPipeline: (() -> Unit)?,
    palette: MeshPalette,
    modifier: Modifier = Modifier,
) {
    val accentColor = palette.colors.firstOrNull() ?: MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.06f),
                    ),
                ),
                shape = CircleShape,
            )
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 1. Now Playing / Player Deck
            SideRailItem(
                icon = Icons.Rounded.Album,
                selected = currentPane == PlayerPane.Main,
                contentDescription = "Now Playing",
                accentColor = accentColor,
                onClick = { onSelectPane(PlayerPane.Main) },
            )

            // 2. Lyrics
            SideRailItem(
                icon = Icons.Rounded.FormatQuote,
                selected = currentPane == PlayerPane.Lyrics,
                contentDescription = "Lyrics",
                accentColor = accentColor,
                onClick = { onSelectPane(PlayerPane.Lyrics) },
            )

            // 3. Queue
            SideRailItem(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                selected = currentPane == PlayerPane.Queue,
                contentDescription = "Queue",
                accentColor = accentColor,
                onClick = { onSelectPane(PlayerPane.Queue) },
            )

            // 4. Audio Pipeline
            if (onOpenPipeline != null) {
                SideRailItem(
                    icon = Icons.Rounded.GraphicEq,
                    selected = false,
                    contentDescription = "Audio Pipeline",
                    accentColor = accentColor,
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
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val animatedBg by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.35f) else Color.Transparent,
        animationSpec = tween(280),
        label = "SideRailBg",
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.75f) else Color.Transparent,
        animationSpec = tween(280),
        label = "SideRailBorder",
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.50f),
        animationSpec = tween(280),
        label = "SideRailIcon",
    )

    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(animatedBg)
            .border(1.dp, animatedBorder, CircleShape)
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
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Modernized Landscape Artwork.
 *
 * Full-height edge-to-edge coverage with horizontal gradient alpha dissolve
 * on the right edge, smoothly fading into the dynamic mesh background.
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
    sleeveShape: () -> Shape = { LandscapeArtShape },
    shadowFraction: () -> Float = { 1f },
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val casts = artLoaded || canvasRendered

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    shadowElevation = if (casts) 14.dp.toPx() * shadowFraction() else 0f
                    shape = sleeveShape()
                    clip = casts
                }
                .graphicsLayer {
                    shape = sleeveShape()
                    clip = true
                }
                .background(Color.Black.copy(alpha = 0.20f)),
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
                    pausedForTransition = pausedForTransition,
                    onRenderedChanged = onCanvasRenderedChange,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Right-side horizontal dissolve gradient mask into the ambient mesh
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.00f to Color.Transparent,
                            0.50f to Color.Transparent,
                            0.82f to Color.Black.copy(alpha = 0.45f),
                            1.00f to Color.Black.copy(alpha = 0.88f),
                        ),
                    ),
            )
        }

        // Overlay elements (e.g. ChordPlayPipelineBottomPill at bottom-left)
        overlay()
    }
}

/**
 * Modernized ChordPlay Landscape Main Player Deck.
 */
@Composable
internal fun LandscapeMainPane(
    compact: Boolean,
    caption: String?,
    onOpenCaption: () -> Unit,
    credits: @Composable () -> Unit,
    lyricStrip: (@Composable () -> Unit)?,
    scrubber: @Composable () -> Unit,
    transport: @Composable () -> Unit,
    volume: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth()
            .padding(horizontal = if (compact) 4.dp else 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        if (caption != null) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                    .clickable(onClick = onOpenCaption)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.70f),
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
        }

        credits()
        Spacer(Modifier.height(if (compact) 4.dp else 10.dp))

        lyricStrip?.invoke()
        Spacer(Modifier.height(if (compact) 4.dp else 10.dp))

        scrubber()
        Spacer(Modifier.height(if (compact) 6.dp else 14.dp))

        transport()

        if (volume != null) {
            Spacer(Modifier.height(if (compact) 6.dp else 12.dp))
            volume()
        }
    }
}

/**
 * Modernized Title, Artist, Like, Add to Playlist, and More menu with scalable typography
 * optimized for in-car and landscape readability.
 */
@Composable
internal fun LandscapeCredits(
    song: Song,
    signedIn: Boolean,
    likeStatus: LikeStatus,
    showRevertCue: Boolean,
    onToggleLike: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    onOpenMenu: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (browseId: String?, name: String) -> Unit,
    fontScale: LandscapeFontScale = LandscapeFontScale.LARGE,
    modifier: Modifier = Modifier,
) {
    val scale = fontScale.scaleFactor
    val titleFontSize = (22 * scale).sp
    val artistFontSize = (16 * scale).sp

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Crossfade(
            targetState = song.title to song.artist,
            animationSpec = tween(durationMillis = 300),
            modifier = Modifier.weight(1f),
            label = "landscapeCredits",
        ) {
            Column {
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
                    modifier = Modifier.opensPage(song.albumId, onOpenAlbum),
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
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        if (signedIn && song.localUri == null) {
            val liked = likeStatus == LikeStatus.LIKE
            CircleGlyph(
                icon = if (liked) BitChordIcons.HeartFilled else BitChordIcons.Heart,
                contentDescription = stringResource(
                    if (liked) Res.string.remove_from_liked else Res.string.like,
                ),
                onClick = onToggleLike,
                active = liked,
                haptic = if (liked) Haptic.ToggleOff else Haptic.ToggleOn,
            )
            Spacer(Modifier.width(6.dp))
        }
        if (onAddToPlaylist != null && song.localUri == null) {
            CircleGlyph(
                icon = Icons.Rounded.PlaylistAdd,
                contentDescription = stringResource(Res.string.add_to_playlist),
                onClick = onAddToPlaylist,
                haptic = Haptic.Tap,
            )
            Spacer(Modifier.width(6.dp))
        }
        CircleGlyph(
            icon = if (showRevertCue) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.MoreHoriz,
            contentDescription = stringResource(Res.string.more),
            onClick = onOpenMenu,
        )
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
    romanizationToggle: @Composable () -> Unit,
    translationToggle: @Composable () -> Unit,
    styleToggle: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    panel: @Composable (Modifier) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
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
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(34.dp)) { if (hasLyrics) romanizationToggle() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LyricsStatusWithChange(
                        status = status,
                        onStatusClick = onStatusClick,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (hasLyrics) {
                        styleToggle()
                        translationToggle()
                    }
                }
            }
        }
    }
}
