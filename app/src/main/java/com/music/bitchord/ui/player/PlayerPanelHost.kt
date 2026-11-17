package com.music.bitchord.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.components.IosMenuPopover
import com.music.bitchord.ui.components.LocalAppBackdrop
import com.music.bitchord.ui.components.backdrop.Backdrop
import com.music.bitchord.ui.components.backdrop.backdrops.rememberSnapshotBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.snapshotBackdropSource
import com.music.bitchord.ui.components.rememberRemoteArtworkUrl
import com.music.bitchord.ui.theme.rememberArtworkPalette
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

/** The dim behind the player while it is part-way open. */
private const val PLAYER_SCRIM_ALPHA = 0.32f

/** Longer than the menu's exit, so it never loses its frost on the way out. */
private const val MENU_STILL_RELEASE_MS = 400L

/**
 * The player, raised: one panel in the app's own window, grown out of the
 * mini player's pill (see [PlayerPanel]), with the dim behind it and the
 * track menu and playlist overlays drawn above it.
 *
 * Not a ModalBottomSheet any more, so what the sheet's window used to give the
 * player is carried out here: back, the drag down to put it away (see
 * [playerPullDown]) and the menu above it. The volume keys reach the activity,
 * which already handles them.
 *
 * Ported from Zenith (rezapranata089-bit/Zenith, GPL-3.0), a sibling fork of
 * BitChord.
 */
@Composable
internal fun PlayerPanelHost(
    song: Song,
    motion: PlayerSheetMotion,
    dock: PlayerDock,
    reducePlayerMotion: Boolean,
    reduceDynamicBlur: Boolean,
    /** Whether the mini player underneath is the liquid glass bar's. */
    glassPill: Boolean,
    hazeState: HazeState,
    isPlaying: Boolean,
    isLoading: Boolean,
    controlsLocked: Boolean,
    onBlockedControl: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    /** The track the player's own ⋯ menu is open on, if it is. */
    menuSong: Song?,
    /** That menu's button, in window coordinates. */
    menuAnchor: Rect?,
    onDismissMenu: () -> Unit,
    songMenuBody: @Composable (Song) -> Unit,
    extraOverlaysActive: Boolean,
    renderExtraOverlays: @Composable (Backdrop, HazeState?) -> Unit,
    nowPlaying: @Composable (Song) -> Unit,
    /** Takes the player straight down, for "reduce animation". */
    onDismiss: () -> Unit,
) {
    val activeDock = dock.takeIf { !reducePlayerMotion }
    DisposableEffect(motion, activeDock) {
        dock.sheetOffset = if (!reducePlayerMotion) motion::position else null
        onDispose {
            dock.sheetOffset = null
            motion.onSheetGone()
        }
    }
    // The panel's plain fill as it grows out of the pill: the cover's own
    // colour, so the first frames of an open are already the player's.
    val panelColor = rememberArtworkPalette(rememberRemoteArtworkUrl(song), dark = true).background
    androidx.compose.runtime.SideEffect { dock.panelColor = panelColor }
    // Back is the app's to carry out, like every other close. Composed ahead
    // of the player, so the player's own handlers — the lyrics and the queue
    // putting themselves away first — are newer and are asked before this one.
    BackHandler { if (reducePlayerMotion) onDismiss() else motion.close() }

    // What the glass menu below blurs: a still of the player, taken as the
    // menu opens, rather than the player live — the page's backdrop layer is
    // under the player and would show through it. Live, every tick of the
    // progress bar, every frame of the canvas and the mesh's drift re-recorded
    // the whole player and re-ran the menu's blur under it; nobody watches the
    // player move behind an open menu. The still is a quarter-scale picture
    // and the blur runs on it only when the menu itself moves. Ported from
    // Zenith (rezapranata089-bit/Zenith, GPL-3.0).
    val playerBackdrop = rememberSnapshotBackdrop()
    val overlayNeedsStill = (menuSong != null || extraOverlaysActive) && !reduceDynamicBlur
    LaunchedEffect(overlayNeedsStill) {
        if (overlayNeedsStill) {
            playerBackdrop.capture()
        } else {
            delay(MENU_STILL_RELEASE_MS)
            playerBackdrop.release()
        }
    }
    PlayerPanel(
        dock = activeDock,
        motion = motion.takeIf { !reducePlayerMotion },
        song = song,
        isPlaying = isPlaying,
        isLoading = isLoading,
        controlsLocked = controlsLocked,
        onBlockedControl = onBlockedControl,
        onPlayPause = onPlayPause,
        onNext = onNext,
        onPrevious = onPrevious,
        scrimAlpha = PLAYER_SCRIM_ALPHA,
        material = when {
            glassPill -> PanelPillMaterial.Glass
            reduceDynamicBlur -> PanelPillMaterial.Solid
            else -> PanelPillMaterial.Frost
        },
        hazeState = hazeState,
        onLaidOut = { size ->
            dock.sheetTravel = size.height.toFloat()
            motion.laidOut = true
        },
        overlay = {
            CompositionLocalProvider(LocalAppBackdrop provides playerBackdrop) {
                IosMenuPopover(
                    item = menuSong,
                    anchor = menuAnchor,
                    onDismiss = onDismissMenu,
                    backdrop = playerBackdrop,
                    hazeState = null,
                ) { target ->
                    songMenuBody(target)
                }
            }
            renderExtraOverlays(playerBackdrop, null)
        },
    ) {
        Box(Modifier.fillMaxSize().snapshotBackdropSource(playerBackdrop)) {
            CompositionLocalProvider(LocalPlayerDock provides activeDock) {
                nowPlaying(song)
            }
        }
    }
}

