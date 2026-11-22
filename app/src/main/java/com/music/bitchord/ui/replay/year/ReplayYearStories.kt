package com.music.bitchord.ui.replay.year

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.player.MeshGradientBackground
import com.music.bitchord.ui.player.MeshPalette
import com.music.bitchord.ui.player.rememberArtworkColors
import com.music.bitchord.ui.replay.BACK_ZONE
import com.music.bitchord.ui.replay.Segment
import com.music.bitchord.ui.replay.StoryFrame
import com.music.bitchord.ui.replay.rotated
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

@Composable
fun ReplayYearStories(
    year: ReplayYear,
    onClose: () -> Unit,
    onShare: (YearSlide) -> Unit,
    paused: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val slides = year.slides
    val reduceMotion by AppSettings.reduceAnimation.collectAsStateWithLifecycle()

    var index by remember { mutableIntStateOf(0) }
    var forward by remember { mutableStateOf(true) }
    var held by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(readMuted(context)) }
    var progress by remember { mutableFloatStateOf(0f) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val onScreen by lifecycle.currentStateFlow.collectAsState()
    val hidden = !onScreen.isAtLeast(Lifecycle.State.STARTED)

    val player = remember { YearClipPlayer(context.applicationContext, scope) }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(muted) { player.muted = muted }
    val starts = remember { mutableStateMapOf<String, BestPartStart>() }

    LaunchedEffect(year) {
        val songs = slides.mapNotNull { year.clips[it] }.distinctBy { it.videoId }
        val gate = Semaphore(LOOKUPS_AT_ONCE)
        songs.forEach { song ->
            BestParts.known(song.videoId)?.let { starts[song.videoId] = it }
            launch {
                if (song.videoId in starts) return@launch
                gate.withPermit { starts[song.videoId] = BestParts.startFor(context.applicationContext, song) }
            }
        }
        val first = year.clips[slides.first()]
        val began = System.currentTimeMillis()
        withTimeoutOrNull(BUILD_MAX_MS) {
            while (first != null && first.videoId !in starts) delay(50)
        }
        clipOf(player, first, starts, slides.first())?.let(player::prime)
        delay((BUILD_MIN_MS - (System.currentTimeMillis() - began)).coerceAtLeast(0))
        ready = true
    }

    val slide = slides[index.coerceIn(0, slides.lastIndex)]

    LaunchedEffect(index, ready) {
        if (!ready) return@LaunchedEffect
        player.play(clipOf(player, year.clips[slide], starts, slide))
        slides.getOrNull(index + 1)?.let { next ->
            delay(PRIME_AFTER_MS)
            player.prime(clipOf(player, year.clips[next], starts, next))
        }
    }
    LaunchedEffect(held, paused, hidden) { player.hold(held || paused || hidden) }

    val beats = remember(index) { HapticBeats() }
    val haptics = rememberHaptics()
    LaunchedEffect(index, ready, beats) {
        if (!ready) return@LaunchedEffect
        var last = -1f
        snapshotFlow { progress * PAGE_MS }.collect { now ->
            val hit = beats.strongestBetween(last, now)
            last = now
            if (hit != null && !(reduceMotion && hit == Haptic.Tick)) haptics.play(hit)
        }
    }

    fun goTo(target: Int) {
        if (target !in slides.indices) return
        forward = target > index
        progress = 0f
        index = target
    }

    fun step(ahead: Boolean) {
        if (ahead && index == slides.lastIndex) {
            onClose()
            return
        }
        goTo(index + if (ahead) 1 else -1)
    }

    LaunchedEffect(index, held, paused, hidden, ready) {
        if (!ready || held || paused || hidden) return@LaunchedEffect
        val remaining = ((1f - progress) * PAGE_MS).toInt().coerceAtLeast(0)
        animate(progress, 1f, animationSpec = tween(remaining, easing = LinearEasing)) { value, _ -> progress = value }
        if (index == slides.lastIndex) onClose() else goTo(index + 1)
    }

    val palette = paletteFor(year, slide)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        StoryFrame {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF17171A))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                held = true
                                tryAwaitRelease()
                                held = false
                            },
                            onLongPress = {},
                            onTap = { offset -> if (ready) step(offset.x >= size.width * BACK_ZONE) },
                        )
                    }
                    .pointerInput(Unit) {
                        var dragged = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragged = 0f },
                            onHorizontalDrag = { change, amount ->
                                dragged += amount
                                change.consume()
                            },
                            onDragEnd = {
                                if (ready && abs(dragged) > SWIPE_PX) step(ahead = dragged < 0)
                            },
                        )
                    },
            ) {
                YearBackdrop(slide, palette)

                if (!ready) {
                    Building()
                } else {
                    AnimatedContent(
                        targetState = index,
                        transitionSpec = { transitionInto(slides[targetState], forward, reduceMotion) },
                        label = "year-slide",
                    ) { shown ->
                        val live = shown == index
                        val clock = remember(live, reduceMotion, beats) {
                            if (live) SlideClock({ progress * PAGE_MS }, reduceMotion, beats)
                            else SlideClock({ Float.MAX_VALUE / 4f }, reduceMotion)
                        }
                        CompositionLocalProvider(LocalSlideClock provides clock) {
                            YearSlideContent(slides[shown], year, palette)
                        }
                    }
                }

                YearChrome(
                    year = year.year,
                    count = slides.size,
                    current = index,
                    progress = { progress },
                    muted = muted,
                    onMute = {
                        muted = !muted
                        writeMuted(context, muted)
                    },
                    onClose = onClose,
                )

                if (ready) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = 18.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .clickable { onShare(slide) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.IosShare,
                            contentDescription = stringResource(R.string.share_my_replay),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun transitionInto(slide: YearSlide, forward: Boolean, reduceMotion: Boolean): ContentTransform {
    if (reduceMotion) return fadeIn(tween(220)) togetherWith fadeOut(tween(180))
    val enterMs = 420
    val exitMs = 320
    return when (slide.ordinal % 3) {
        0 -> (scaleIn(tween(enterMs), initialScale = if (forward) 0.82f else 1.18f) + fadeIn(tween(enterMs))) togetherWith
            (scaleOut(tween(exitMs), targetScale = if (forward) 1.2f else 0.85f) + fadeOut(tween(exitMs)))
        1 -> (slideInVertically(tween(enterMs)) { if (forward) it / 2 else -it / 2 } + fadeIn(tween(enterMs))) togetherWith
            (slideOutVertically(tween(exitMs)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(exitMs)))
        else -> (slideInHorizontally(tween(enterMs)) { if (forward) it else -it } + fadeIn(tween(enterMs / 2))) togetherWith
            (slideOutHorizontally(tween(exitMs)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(exitMs)))
    }
}

private fun clipOf(
    player: YearClipPlayer,
    song: Song?,
    starts: Map<String, BestPartStart>,
    slide: YearSlide,
): YearClipPlayer.Clip? {
    song ?: return null
    val best = starts[song.videoId] ?: BestParts.known(song.videoId)
        ?: BestPartStart(BestPart.fallbackMs(0), BestPartStart.Source.FALLBACK)
    val seek = (best.startMs + SNAP_LEAD_MS - slide.revealAtMs).coerceAtLeast(0)
    return player.clipFor(song, seek)
}

@Composable
private fun Building() {
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) { clock.animateTo(1f, tween(BUILD_MAX_MS.toInt(), easing = LinearEasing)) }
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(
            LocalSlideClock provides SlideClock({ clock.value * BUILD_MAX_MS }, reduceMotion = false),
        ) {
            OrbitRings(start = 0, color = Color.White, modifier = Modifier.size(160.dp))
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.replay_year_building),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W800,
                color = Color.White,
                modifier = Modifier.breathe(LocalSlideClock.current, 0, amount = 0.04f, periodMs = 1_400f),
            )
        }
    }
}

@Composable
private fun YearChrome(
    year: Int,
    count: Int,
    current: Int,
    progress: () -> Float,
    muted: Boolean,
    onMute: () -> Unit,
    onClose: () -> Unit,
    still: Boolean = false,
) {
    val controls = if (still) Modifier.alpha(0f) else Modifier
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp).then(controls),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = stringResource(if (muted) R.string.replay_year_unmute else R.string.replay_year_mute),
                tint = Color.White,
                modifier = Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onMute).padding(4.dp),
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.close_replay),
                tint = Color.White,
                modifier = Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onClose).padding(4.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = controls) {
            repeat(count) { i ->
                if (i == current) {
                    Segment(fraction = progress(), modifier = Modifier.weight(1f))
                } else {
                    Segment(fraction = if (i < current) 1f else 0f, modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Replay ${year % 100}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W700,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = "ChordPlay",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W700,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun YearBackdrop(slide: YearSlide, palette: MeshPalette) {
    MeshGradientBackground(palette = palette, trackKey = slide.name, animated = false)
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to Color.Black.copy(alpha = 0.45f),
                    0.35f to Color.Black.copy(alpha = 0.10f),
                    1.0f to Color.Black.copy(alpha = 0.34f),
                ),
            ),
    )
}

@Composable
private fun paletteFor(year: ReplayYear, slide: YearSlide): MeshPalette {
    val artwork = year.clips[slide]?.thumbnailUrl ?: year.data.summary.songs.firstOrNull()?.song?.thumbnailUrl
    return rememberArtworkColors(artwork).rotated(slide.hue)
}

@Composable
internal fun YearCardStill(year: ReplayYear, slide: YearSlide, modifier: Modifier = Modifier) {
    val palette = paletteFor(year, slide)
    val clock = remember { SlideClock({ PAGE_MS }, reduceMotion = false) }
    Box(modifier.background(Color(0xFF17171A))) {
        YearBackdrop(slide, palette)
        CompositionLocalProvider(LocalSlideClock provides clock) {
            YearSlideContent(slide, year, palette)
        }
        YearChrome(
            year = year.year,
            count = year.slides.size,
            current = year.slides.indexOf(slide),
            progress = { 1f },
            muted = false,
            onMute = {},
            onClose = {},
            still = true,
        )
    }
}

private fun prefs(context: Context) = context.getSharedPreferences("replay_year", Context.MODE_PRIVATE)

private fun readMuted(context: Context): Boolean =
    runCatching { prefs(context).getBoolean("muted", false) }.getOrDefault(false)

private fun writeMuted(context: Context, muted: Boolean) {
    runCatching { prefs(context).edit().putBoolean("muted", muted).apply() }
}

private const val PAGE_MS = 15_000f
private const val BUILD_MIN_MS = 1_200L
private const val BUILD_MAX_MS = 2_500L
private const val LOOKUPS_AT_ONCE = 3
private const val PRIME_AFTER_MS = 1_200L
private const val SNAP_LEAD_MS = 250L
private const val SWIPE_PX = 120f
