package com.music.bitchord.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.settings.CoverAnimationMode

internal class CoverPresentation(
    val outgoing: Painter?,
    val incoming: Painter?,
    val mode: CoverAnimationMode,
    val direction: Int,
    val progress: () -> Float,
)

private data class DisplayedCover(val entry: String, val painter: Painter)

@Composable
internal fun rememberCoverPresentation(
    song: Song,
    currentArt: PlayerArtwork,
    artPx: Int,
    enabled: Boolean,
    upcoming: Song?,
): CoverPresentation? {
    val host = PlayerPlatform.host
    val mode by host.settings.coverAnimation.collectAsStateWithLifecycle()
    if (!enabled || mode == CoverAnimationMode.NONE) return null
    val trackChange by host.coverTrackChange.collectAsStateWithLifecycle()
    val firstChange = remember(host) { host.coverTrackChange.value }
    val entry = song.coverEntryKey()
    var lastShown by remember { mutableStateOf<DisplayedCover?>(null) }
    val change = trackChange?.takeIf { it !== firstChange && it.incoming.coverEntryKey() == entry }
    val timed = remember(change) { Animatable(0f) }
    val finished by remember(timed) { derivedStateOf { timed.value >= 1f } }
    val pending = change?.takeUnless { finished }
    val from = pending?.outgoing
    val to = pending?.incoming ?: upcoming
    val outgoingUrl = rememberRemoteArtworkUrl(from)
    val incomingUrl = rememberRemoteArtworkUrl(to)
    val canBlend = from != null && outgoingUrl?.artworkAt(artPx) != incomingUrl?.artworkAt(artPx)
    val outgoing = rememberCoverPainter(outgoingUrl.takeIf { canBlend }, artPx)
    val incoming = rememberCoverPainter(incomingUrl, artPx)
    val incomingState = incoming?.state?.collectAsStateWithLifecycle()?.value
    val outgoingState = outgoing?.state?.collectAsStateWithLifecycle()?.value
    val steadyPainter = currentArt.loadedPainter
    val outgoingPainter = lastShown?.painter ?: (outgoingState as? AsyncImagePainter.State.Success)?.painter
    val incomingPainter = steadyPainter.takeIf { to?.coverEntryKey() == entry }
        ?: (incomingState as? AsyncImagePainter.State.Success)?.painter
    val incomingReady = incomingPainter != null
    val unavailable = !incomingReady && (incoming == null || incomingState is AsyncImagePainter.State.Error)

    LaunchedEffect(timed, incomingReady, unavailable, canBlend) {
        if (pending == null) return@LaunchedEffect
        when {
            unavailable || !canBlend -> timed.snapTo(1f)
            incomingReady -> timed.animateTo(1f, pending.change.animationSpec)
        }
    }

    if (pending == null || !canBlend || unavailable || outgoingPainter == null && !incomingReady) {
        val held = lastShown?.takeIf {
            it.entry != entry && steadyPainter == null && !currentArt.failed
        }
        if (held != null) return CoverPresentation(held.painter, null, mode, 0) { 0f }
        if (steadyPainter != null) SideEffect { lastShown = DisplayedCover(entry, steadyPainter) }
        return null
    }
    return CoverPresentation(
        outgoing = outgoingPainter,
        incoming = incomingPainter,
        mode = mode,
        direction = pending.change.direction,
        progress = { if (!incomingReady) 0f else if (outgoingPainter == null) 1f else timed.value },
    )
}

@Composable
private fun rememberCoverPainter(url: String?, artPx: Int): AsyncImagePainter? {
    if (url == null) return null
    val art = rememberPlayerArtwork(url, artPx)
    return rememberAsyncImagePainter(art.request, contentScale = ContentScale.Crop, onState = art::onState)
}

@Composable
internal fun CoverMotion(presentation: CoverPresentation) {
    CoverMotionLayers(presentation.mode, presentation.direction, presentation.progress) { entering, modifier ->
        val painter = if (entering) presentation.incoming else presentation.outgoing
        if (painter != null) Image(painter, null, modifier, contentScale = ContentScale.Crop)
    }
}

/** Shared with the settings preview so the selected motion is the one it actually shows. */
@Composable
fun CoverMotionLayers(
    mode: CoverAnimationMode,
    direction: Int,
    progress: () -> Float,
    modifier: Modifier = Modifier,
    cover: @Composable (entering: Boolean, modifier: Modifier) -> Unit,
) {
    Box(modifier.fillMaxSize().clipToBounds()) {
        val directional = direction != 0
        val perspective = mode == CoverAnimationMode.THREE_D_FADE && directional
        val travel = if (perspective) 0.14f else 0.06f
        if (mode != CoverAnimationMode.NONE) {
            cover(false, Modifier.fillMaxSize().graphicsLayer {
                val t = progress().coerceIn(0f, 1f)
                // Keep this layer opaque and overscanned until the arriving cover
                // fills the viewport. It also covers the corners during the turn.
                if (directional) {
                    translationX = direction * size.width * travel * t
                    scaleX = 1f + (if (perspective) 0.53f else 0.13f) * t
                    scaleY = 1f + (if (perspective) 0.13f else 0.035f) * t
                }
                if (perspective) {
                    rotationY = -direction * 21f * t
                    cameraDistance = (size.width * 5f).coerceAtLeast(1f)
                }
            })
        }
        cover(true, Modifier.fillMaxSize().graphicsLayer {
            val t = if (mode == CoverAnimationMode.NONE) 1f else progress().coerceIn(0f, 1f)
            val remaining = 1f - t
            if (directional && mode != CoverAnimationMode.NONE) {
                translationX = -direction * size.width * travel * remaining
            }
            if (perspective) {
                rotationY = direction * 32f * remaining
                cameraDistance = (size.width * 5f).coerceAtLeast(1f)
                scaleX = 1f - 0.06f * remaining
                scaleY = scaleX
            }
            alpha = t
        })
    }
}
