package com.music.bitchord.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.Song
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.add_to_playlist
import com.music.bitchord.sharedui.resources.like
import com.music.bitchord.sharedui.resources.more
import com.music.bitchord.sharedui.resources.remove_from_liked
import com.music.bitchord.ui.components.menuAnchor
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.Haptics
import com.music.bitchord.ui.icons.BitChordIcons
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.resources.stringResource

/**
 * The portrait player's title and artist, with ChordPlay's frosted action
 * capsule (like, add-to-playlist, menu/revert cue).
 */
@Composable
internal fun PortraitCredits(
    song: Song,
    isWavePlay: Boolean,
    isModernOrVinyl: Boolean,
    signedIn: Boolean,
    likeStatus: LikeStatus,
    showRevertCue: Boolean,
    currentTitleTop: () -> Dp,
    titleStart: () -> Dp,
    p: () -> Float,
    scrolls: () -> Boolean,
    headerHeight: Dp,
    thumbSize: Dp,
    playerHaze: HazeState,
    haptics: Haptics,
    likeBurstState: LikeBurstState,
    onToggleLike: () -> Unit,
    onAddToPlaylist: (() -> Unit)?,
    onOpenMenu: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (browseId: String?, name: String) -> Unit,
    onDismissBandBottom: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset {
                IntOffset(
                    x = 0,
                    y = (
                        currentTitleTop() -
                            lerp(0.dp, (headerHeight - thumbSize) / 2, p())
                        ).roundToPx(),
                )
            }
            .layout { measurable, constraints ->
                val start = titleStart().roundToPx()
                val placeable = measurable.measure(constraints.offset(horizontal = -start))
                layout(
                    constraints.constrainWidth(placeable.width + start),
                    constraints.constrainHeight(placeable.height),
                ) { placeable.placeRelative(start, 0) }
            }
            .height(headerHeight)
            .onGloballyPositioned { onDismissBandBottom(it.boundsInRoot().bottom) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .graphicsLayer {
                    val scale = 1f - 0.2f * p()
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0.5f)
                },
        ) {
            var titleOverflowing by remember { mutableStateOf(false) }
            Crossfade(
                targetState = song.title to song.artist,
                animationSpec = tween(durationMillis = 300),
                label = "playerCredits",
            ) {
                val scrolling = scrolls()
                Column {
                    MarqueeText(
                        text = song.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = if (isWavePlay) 24.sp else 20.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                        enabled = scrolling,
                        leading = if (song.isExplicit == true) {
                            { ExplicitBadge(color = Color.White) }
                        } else {
                            null
                        },
                        onOverflowChange = { titleOverflowing = it },
                        modifier = Modifier.opensPage(song.albumId, onOpenAlbum),
                    )
                    ArtistCreditsMarquee(
                        text = song.artist,
                        credits = remember(song.artist, song.artists, song.artistId) {
                            artistCredits(song.artist, song.artists, song.artistId)
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.W500,
                            fontSize = if (isWavePlay) 16.sp else 20.sp,
                        ),
                        color = Color.White.copy(alpha = if (isWavePlay) 0.65f else 0.55f),
                        enabled = scrolling,
                        startDelayMillis = if (titleOverflowing) MARQUEE_ARTIST_STAGGER_MS else 0L,
                        onOpenArtist = onOpenArtist,
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Row(
            modifier = Modifier
                .videoDockGlassPill(hazeState = playerHaze)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (signedIn && song.localUri == null) {
                val liked = likeStatus == LikeStatus.LIKE
                val heartTint by animateColorAsState(
                    targetValue = if (liked) Color(0xFFFF2D55) else Color.White.copy(alpha = 0.85f),
                    animationSpec = tween(durationMillis = 220),
                    label = "portraitLikeTint",
                )
                val heartScale by animateFloatAsState(
                    targetValue = if (liked) 1.06f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "portraitLikeScale",
                )
                Box(
                    modifier = Modifier
                        .likeButtonTarget(likeBurstState)
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                val currentlyLiked = likeStatus == LikeStatus.LIKE
                                haptics.play(if (currentlyLiked) Haptic.ToggleOff else Haptic.ToggleOn)
                                likeBurstState.launchFromButton(currentlyLiked, onToggleLike)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (liked) BitChordIcons.HeartFilled else BitChordIcons.Heart,
                        contentDescription = stringResource(
                            if (liked) Res.string.remove_from_liked else Res.string.like,
                        ),
                        tint = heartTint,
                        modifier = Modifier
                            .size(19.dp)
                            .graphicsLayer {
                                scaleX = heartScale
                                scaleY = heartScale
                            },
                    )
                }
            }
            if (onAddToPlaylist != null && (song.localUri == null || isModernOrVinyl)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
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
                        imageVector = BitChordIcons.PlaylistAdd,
                        contentDescription = stringResource(Res.string.add_to_playlist),
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .menuAnchor()
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
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
