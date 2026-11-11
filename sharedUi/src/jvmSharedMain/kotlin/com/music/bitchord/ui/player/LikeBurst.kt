package com.music.bitchord.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

val LocalLikeBurstState = staticCompositionLocalOf<LikeBurstState?> { null }

private val LikeRed = Color(0xFFFF2D55)
private val LikePink = Color(0xFFFF7597)

private val HeartWidth = 88.dp
private val ButtonGlyphWidth = 20.dp
private val ParticleWidth = 12.dp
private const val ParticleCount = 10

private const val TravelMs = 420
private const val FloatMs = 520
private const val HomeMs = 460

private val TravelEasing = CubicBezierEasing(0.3f, 1.22f, 0.5f, 1f)
private val PopEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val HomeEasing = CubicBezierEasing(0.5f, -0.18f, 0.7f, 1f)

private const val MaxFlights = 4

/**
 * Manages the interactive like animation:
 * A glowing heart launches from the like button (or double-tapped cover),
 * arcs to center screen, thumps in vibrant red with a burst of particles and expanding ring,
 * floats gracefully, and dives back into the like button which bounces with spring physics.
 */
@Stable
class LikeBurstState {
    internal class Flight(val id: Long, val origin: Offset, val fromButton: Boolean, val seed: Int)

    internal val flights = mutableStateListOf<Flight>()
    private var nextId = 0L

    internal var buttonBounds by mutableStateOf<Rect?>(null)
    internal val landing = Animatable(1f)

    var likeCount by mutableIntStateOf(0)
        private set

    fun launchFromButton(liked: Boolean, onToggleLike: () -> Unit) {
        if (!liked) {
            val origin = buttonBounds?.center ?: Offset.Zero
            launch(origin, fromButton = true)
            likeCount++
        }
        onToggleLike()
    }

    fun launchFromDoubleTap(rootPosition: Offset, liked: Boolean, onToggleLike: () -> Unit) {
        launch(rootPosition, fromButton = false)
        if (!liked) {
            likeCount++
            onToggleLike()
        }
    }

    fun clear() = flights.clear()

    private fun launch(origin: Offset, fromButton: Boolean) {
        if (flights.size >= MaxFlights) flights.removeAt(0)
        flights += Flight(nextId++, origin, fromButton, Random.nextInt())
    }
}

/**
 * Marks the like button as the origin and landing target for hearts.
 */
fun Modifier.likeButtonTarget(state: LikeBurstState?): Modifier =
    if (state == null) {
        this
    } else {
        this
            .onGloballyPositioned { coordinates ->
                state.buttonBounds = coordinates.boundsInRoot()
            }
            .graphicsLayer {
                val s = state.landing.value
                scaleX = s
                scaleY = s
            }
    }

/**
 * Fullscreen overlay that renders hearts flying through the air and bursting into particles.
 */
@Composable
fun LikeBurstOverlay(state: LikeBurstState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    if (state.flights.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOffset = it.positionInRoot() },
    ) {
        for (flight in state.flights) {
            key(flight.id) {
                HeartFlight(
                    state = state,
                    flight = flight,
                    rootOffset = { rootOffset },
                    bounceScope = scope,
                    onDone = { state.flights.remove(flight) },
                )
            }
        }
    }
}

private class Particle(
    val dx: Float,
    val dy: Float,
    val reach: Dp,
    val scale: Float,
    val spin: Float,
    val color: Color,
)

@Composable
private fun HeartFlight(
    state: LikeBurstState,
    flight: LikeBurstState.Flight,
    rootOffset: () -> Offset,
    bounceScope: CoroutineScope,
    onDone: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val glyphScale = ButtonGlyphWidth / HeartWidth

    val travel = remember { Animatable(0f) }
    val scale = remember { Animatable(if (flight.fromButton) glyphScale else 0f) }
    val redness = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val float = remember { Animatable(0f) }
    val home = remember { Animatable(0f) }

    val random = remember { Random(flight.seed) }
    val side = remember { if (random.nextBoolean()) 1f else -1f }
    val particles = remember {
        val palette = listOf(LikeRed, LikePink, Color.White)
        List(ParticleCount) { i ->
            val angle = i * 2f * PI.toFloat() / ParticleCount + (random.nextFloat() - 0.5f) * 0.4f
            Particle(
                dx = cos(angle),
                dy = sin(angle),
                reach = (40 + random.nextInt(32)).dp,
                scale = 0.6f + random.nextFloat() * 0.4f,
                spin = (random.nextFloat() - 0.5f) * 60f,
                color = palette[i % palette.size],
            )
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope {
            if (!flight.fromButton) {
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                } catch (_: Throwable) {}
            }

            // Phase 1: Arcs up to screen center and pops to full size
            val pop = launch { scale.animateTo(1f, tween(TravelMs, easing = PopEasing)) }
            travel.animateTo(1f, tween(TravelMs, easing = TravelEasing))
            pop.join()

            // Phase 2: Heart thump in vibrant red + particle explosion
            try {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            } catch (_: Throwable) {}
            launch { redness.animateTo(1f, tween(100)) }
            launch { burst.animateTo(1f, tween(550, easing = LinearOutSlowInEasing)) }
            scale.animateTo(1.30f, tween(100, easing = LinearOutSlowInEasing))
            launch { scale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 380f)) }

            // Phase 3: Float and bob in the air
            launch {
                delay(140)
                redness.animateTo(0.2f, tween(480))
            }
            float.animateTo(1f, tween(FloatMs, easing = LinearEasing))

            // Phase 4: Dive home into the like button
            home.animateTo(1f, tween(HomeMs, easing = HomeEasing))
            try {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Throwable) {}
            bounceScope.launch {
                state.landing.snapTo(1f)
                state.landing.animateTo(1.35f, tween(80, easing = LinearOutSlowInEasing))
                state.landing.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 480f))
            }
        }
        onDone()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val offset = rootOffset()
        val origin = if (flight.origin == Offset.Zero) Offset(size.width / 2f, size.height * 0.8f) else flight.origin - offset
        val center = Offset(size.width / 2f, size.height * 0.42f)
        val heartW = HeartWidth.toPx()

        // Quad bezier arc for flight into center
        val travelControl = Offset(
            (origin.x + center.x) / 2f + side * 44.dp.toPx(),
            min(origin.y, center.y) - 100.dp.toPx(),
        )
        val t = travel.value
        val inPos = quad(origin, travelControl, center, t)

        // Float bobbing
        val f = float.value
        val bobX = sin(f * 2f * PI.toFloat() * 1.25f) * 6.dp.toPx() * side
        val bobY = -sin(f * 2f * PI.toFloat()) * 8.dp.toPx() - f * 14.dp.toPx()
        val floatPos = center + Offset(bobX, bobY)
        val floatEnd = center + Offset(0f, -14.dp.toPx())

        // Home dive
        val target = state.buttonBounds?.center?.minus(offset) ?: origin
        val h = home.value
        val homeControl = Offset(lerp(floatEnd.x, target.x, 0.25f), floatEnd.y - 80.dp.toPx())
        val pos = when {
            h != 0f -> quad(floatEnd, homeControl, target, h)
            f > 0f -> floatPos
            else -> inPos
        }

        val wobble = sin(f * 2f * PI.toFloat() * 1.5f) * 7f
        val rotation = when {
            h != 0f -> lerp(wobble, 0f, h) + sin(h * PI.toFloat()) * 14f * sign(target.x - floatEnd.x)
            f > 0f -> wobble
            else -> lerp(-18f * side, 0f, t.coerceIn(0f, 1f))
        }

        val homeShrink = lerp(1f, glyphScale, h * h)
        val s = scale.value * homeShrink
        val squash = (scale.value - 1f).coerceAtLeast(0f)
        val sx = s * (1f + squash * 0.25f)
        val sy = s * (1f - squash * 0.12f)
        val alpha = 1f - ((h - 0.82f) / 0.18f).coerceIn(0f, 1f)
        val red = redness.value
        val heartColor = lerp(Color.White, LikeRed, red)

        // Draw particle burst and expanding glow ring
        drawBurst(center, heartW, particles, burst.value)

        if (alpha > 0f && s > 0f) {
            // Soft radial shadow behind heart
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.Black.copy(alpha = 0.25f * alpha), Color.Transparent),
                    center = pos + Offset(0f, 6.dp.toPx() * s),
                    radius = heartW * 0.7f * s,
                ),
                radius = heartW * 0.7f * s,
                center = pos + Offset(0f, 6.dp.toPx() * s),
            )

            // Red aura glow at thump
            if (red > 0.01f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(LikeRed.copy(alpha = 0.40f * red * alpha), Color.Transparent),
                        center = pos,
                        radius = heartW * 0.90f * s,
                    ),
                    radius = heartW * 0.90f * s,
                    center = pos,
                )
            }

            // Draw center heart
            val path = createHeartPath(heartW)
            withTransform({
                translate(pos.x, pos.y)
                rotate(rotation, pivot = Offset.Zero)
                scale(sx, sy, pivot = Offset.Zero)
                translate(-heartW / 2f, -heartW * (76f / 88f) / 2f)
            }) {
                drawPath(
                    path = path,
                    color = heartColor.copy(alpha = alpha),
                    style = Fill,
                )
            }
        }
    }
}

/**
 * Draws the ring and spray of particles during heart thump.
 */
private fun DrawScope.drawBurst(
    center: Offset,
    heartW: Float,
    particles: List<Particle>,
    progress: Float,
) {
    if (progress <= 0f || progress >= 1f) return
    val out = 1f - (1f - progress) * (1f - progress) * (1f - progress) // cubic ease out
    val fade = 1f - progress

    // Expanding shockwave ring
    drawCircle(
        color = lerp(LikeRed, Color.White, progress * 0.5f).copy(alpha = 0.65f * fade),
        radius = heartW * 0.5f * lerp(0.7f, 1.8f, out),
        center = center,
        style = Stroke(width = lerp(4.dp.toPx(), 0.5.dp.toPx(), progress)),
    )

    // Particles spraying out
    val particleSize = ParticleWidth.toPx()
    val miniHeartPath = createHeartPath(particleSize)
    val startDistance = heartW * 0.35f

    for (p in particles) {
        val distance = startDistance + p.reach.toPx() * out
        val at = center + Offset(p.dx * distance, p.dy * distance)
        val s = p.scale * (1f - progress * 0.4f)
        withTransform({
            translate(at.x, at.y)
            rotate(p.spin * progress, pivot = Offset.Zero)
            scale(s, s, pivot = Offset.Zero)
            translate(-particleSize / 2f, -particleSize * (76f / 88f) / 2f)
        }) {
            drawPath(
                path = miniHeartPath,
                color = p.color.copy(alpha = fade),
                style = Fill,
            )
        }
    }
}

/**
 * Procedurally generates a smooth mathematical heart vector path.
 */
internal fun createHeartPath(size: Float): Path = Path().apply {
    val w = size
    val h = size * (76f / 88f)
    moveTo(w * 0.5f, h * 0.88f)
    cubicTo(w * 0.15f, h * 0.65f, 0f, h * 0.45f, 0f, h * 0.28f)
    cubicTo(0f, h * 0.10f, w * 0.15f, 0f, w * 0.32f, 0f)
    cubicTo(w * 0.42f, 0f, w * 0.48f, h * 0.08f, w * 0.5f, h * 0.16f)
    cubicTo(w * 0.52f, h * 0.08f, w * 0.58f, 0f, w * 0.68f, 0f)
    cubicTo(w * 0.85f, 0f, w, h * 0.10f, w, h * 0.28f)
    cubicTo(w, h * 0.45f, w * 0.85f, h * 0.65f, w * 0.5f, h * 0.88f)
    close()
}

private fun quad(a: Offset, control: Offset, b: Offset, t: Float): Offset {
    val u = 1f - t
    return Offset(
        u * u * a.x + 2f * u * t * control.x + t * t * b.x,
        u * u * a.y + 2f * u * t * control.y + t * t * b.y,
    )
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + fraction * (stop - start)

