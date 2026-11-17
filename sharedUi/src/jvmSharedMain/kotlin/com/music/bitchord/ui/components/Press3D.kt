package com.music.bitchord.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val DEFAULT_MAX_TILT = 7f
private const val DEFAULT_PRESSED_SCALE = 0.96f

private val TiltSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 420f,
)

/**
 * Tactile 3D press feedback that tilts an element based on pointer position relative to center.
 * When touched off-center, the element dips down towards the finger with realistic spring damping
 * while scaling down slightly.
 *
 * Uses [PointerEventPass.Initial] so that clicks, long-presses, and gestures pass through unimpeded.
 * Implemented as a [Modifier.Node] so scrolling cards pay no `Modifier.composed` allocation.
 */
fun Modifier.press3D(
    maxTilt: Float = DEFAULT_MAX_TILT,
    pressedScale: Float = DEFAULT_PRESSED_SCALE,
    enabled: Boolean = true,
    origin: TransformOrigin = TransformOrigin.Center,
): Modifier = if (!enabled) {
    this
} else {
    this.then(Press3DElement(maxTilt = maxTilt, pressedScale = pressedScale, origin = origin))
}

private data class Press3DElement(
    val maxTilt: Float,
    val pressedScale: Float,
    val origin: TransformOrigin,
) : ModifierNodeElement<Press3DNode>() {
    override fun create(): Press3DNode = Press3DNode(maxTilt, pressedScale, origin)

    override fun update(node: Press3DNode) {
        node.maxTilt = maxTilt
        node.pressedScale = pressedScale
        node.origin = origin
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "press3D"
        properties["maxTilt"] = maxTilt
        properties["pressedScale"] = pressedScale
        properties["origin"] = origin
    }
}

private class Press3DNode(
    var maxTilt: Float,
    var pressedScale: Float,
    var origin: TransformOrigin,
) : DelegatingNode(), LayoutModifierNode {

    private val press = Animatable(0f)
    private var normalized: Offset = Offset.Zero
    private var measuredSize: IntSize = IntSize.Zero
    private var animJob: Job? = null

    init {
        delegate(
            SuspendingPointerInputModifierNode {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent(PointerEventPass.Initial)
                            .changes.firstOrNull { it.pressed } ?: continue

                        val bounds = this.size
                        if (bounds.width > 0 && bounds.height > 0) {
                            val p = down.position
                            normalized = Offset(
                                x = ((p.x / bounds.width) * 2f - 1f).coerceIn(-1f, 1f),
                                y = ((p.y / bounds.height) * 2f - 1f).coerceIn(-1f, 1f),
                            )
                            measuredSize = bounds
                        }
                        animJob?.cancel()
                        animJob = coroutineScope.launch { press.animateTo(1f, TiltSpring) }

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.none { it.pressed }) break
                        }
                        animJob?.cancel()
                        animJob = coroutineScope.launch { press.animateTo(0f, TiltSpring) }
                    }
                }
            },
        )
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                val t = press.value
                if (t > 0f) {
                    val s = 1f - (1f - pressedScale) * t
                    scaleX = s
                    scaleY = s
                    rotationX = -normalized.y * maxTilt * t
                    rotationY = normalized.x * maxTilt * t
                    cameraDistance = (measuredSize.width.takeIf { it > 0 }?.toFloat() ?: 1000f) * 3f
                    transformOrigin = origin
                }
            }
        }
    }
}
