package com.music.bitchord.ui.player

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import kotlin.math.roundToInt

/** Touches leave the value alone until they turn into a horizontal drag. */
@Composable
internal fun Modifier.relativeSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit,
): Modifier {
    val currentValue by rememberUpdatedState(value.coerceIn(range))
    val update by rememberUpdatedState(onValueChange)
    val finish by rememberUpdatedState(onFinished)
    val span = range.endInclusive - range.start
    fun snap(raw: Float): Float {
        val clamped = raw.coerceIn(range)
        if (steps == 0) return clamped
        val interval = span / (steps + 1)
        return (range.start + ((clamped - range.start) / interval).roundToInt() * interval).coerceIn(range)
    }
    fun setValue(raw: Float): Boolean {
        val next = snap(raw)
        if (next == currentValue) return false
        update(next)
        finish()
        return true
    }
    return semantics(mergeDescendants = true) {
        contentDescription = label
        progressBarRangeInfo = ProgressBarRangeInfo(currentValue, range, steps)
        if (enabled) {
            setProgress(action = ::setValue)
        } else {
            disabled()
        }
    }.onKeyEvent { event ->
        if (!enabled || event.type != KeyEventType.KeyDown) return@onKeyEvent false
        val step = span / if (steps == 0) 100 else steps + 1
        when (event.key) {
            Key.DirectionLeft, Key.DirectionDown -> setValue(currentValue - step)
            Key.DirectionRight, Key.DirectionUp -> setValue(currentValue + step)
            else -> false
        }
    }.focusable(enabled).then(
        if (!enabled) Modifier else Modifier.pointerInput(range, steps) {
            var dragValue = currentValue
            var sentValue = currentValue
            detectHorizontalDragGestures(
                onDragStart = {
                    dragValue = currentValue
                    sentValue = currentValue
                },
                onDragEnd = { finish() },
                onDragCancel = { finish() },
            ) { change, distance ->
                change.consume()
                dragValue = (dragValue + distance * span / size.width.coerceAtLeast(1)).coerceIn(range)
                val next = snap(dragValue)
                if (next != sentValue) {
                    sentValue = next
                    update(next)
                }
            }
        },
    )
}
