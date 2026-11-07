package com.music.bitchord.ui.performance

import android.os.Build
import android.view.Display
import android.view.View
import android.view.Window
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MIN_PERFORMANCE_REFRESH_RATE = 50
private const val MAX_REASONABLE_REFRESH_RATE = 240

/** Refresh rates the current display can actually select, normalized for UI labels. */
fun Display?.supportedPerformanceRefreshRates(): List<Int> {
    val fallback = this?.refreshRate?.roundToInt()?.coerceAtLeast(MIN_PERFORMANCE_REFRESH_RATE) ?: 60
    return this?.supportedModes
        ?.asSequence()
        ?.map { it.refreshRate.roundToInt() }
        ?.filter { it in MIN_PERFORMANCE_REFRESH_RATE..MAX_REASONABLE_REFRESH_RATE }
        ?.distinct()
        ?.sorted()
        ?.toList()
        ?.ifEmpty { listOf(fallback) }
        ?: listOf(fallback)
}

/** Maps a saved preference to a real mode, including preferences restored on another device. */
fun Display?.resolvePerformanceRefreshRate(preferred: Int): Int =
    supportedPerformanceRefreshRates().minBy { abs(it - preferred) }

/**
 * Finds the optimal [Display.Mode] matching [targetRate] with the largest resolution
 * (or matching the display's current mode).
 */
fun Display?.findModeForRefreshRate(targetRate: Int): Display.Mode? {
    if (this == null) return null
    val modes = supportedModes ?: return null
    val targetModes = modes.filter { abs(it.refreshRate.roundToInt() - targetRate) <= 1 }
    return if (targetModes.isNotEmpty()) {
        targetModes.maxByOrNull { it.physicalWidth * it.physicalHeight }
    } else {
        modes.minByOrNull { abs(it.refreshRate.roundToInt() - targetRate) }
    }
}

/**
 * Enforces sustained high refresh rate (e.g. 120Hz/unlocked FPS) across all Android versions.
 *
 * Configures [Window.attributes] display mode, minimum and maximum refresh rates, and
 * power savings policies so variable refresh rate (VRR) displays do not downclock to 60Hz.
 */
fun Window.applyPerformanceRefreshRate(
    display: Display?,
    enabled: Boolean,
    preferredRate: Int,
    targetView: View? = null,
) {
    val layoutParams = attributes
    if (!enabled) {
        // Fall back to standard 60Hz / power savings mode
        @Suppress("DEPRECATION")
        layoutParams.preferredRefreshRate = 60f

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                layoutParams.javaClass.getField("preferredMinDisplayRefreshRate").setFloat(layoutParams, 0f)
                layoutParams.javaClass.getField("preferredMaxDisplayRefreshRate").setFloat(layoutParams, 60f)
            } catch (_: Throwable) {}
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            setFrameRatePowerSavingsBalanced(true)
            targetView?.requestedFrameRate = View.REQUESTED_FRAME_RATE_CATEGORY_DEFAULT
        }
    } else {
        // High performance mode: unlock and lock to target refresh rate (e.g. 120Hz)
        val resolvedRate = display.resolvePerformanceRefreshRate(preferredRate)
        val rateFloat = resolvedRate.toFloat()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                layoutParams.javaClass.getField("preferredMinDisplayRefreshRate").setFloat(layoutParams, rateFloat)
                layoutParams.javaClass.getField("preferredMaxDisplayRefreshRate").setFloat(layoutParams, rateFloat)
            } catch (_: Throwable) {}
        }

        @Suppress("DEPRECATION")
        layoutParams.preferredRefreshRate = rateFloat

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            setFrameRatePowerSavingsBalanced(false)
            targetView?.requestedFrameRate = rateFloat
        }
    }
    attributes = layoutParams
}

/** Formats a density scaling float into a human-readable label matching Metrolist styling. */
fun displayDensityLabel(scale: Float): String {
    val percent = (scale * 100).roundToInt()
    val name = when {
        percent <= 55 -> "Ultra Compact"
        percent <= 65 -> "Very Compact"
        percent <= 78 -> "Compact"
        percent <= 92 -> "Slightly Compact"
        percent in 98..102 -> "Native"
        percent <= 118 -> "Slightly Large"
        else -> "Large"
    }
    return "$name ($percent%)"
}
