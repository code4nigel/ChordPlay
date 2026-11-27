package com.music.bitchord.ui.performance

import android.os.Build
import android.view.Display
import android.view.View
import android.view.Window
import com.music.bitchord.data.settings.RefreshRatePolicy
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
 * Applies the active [RefreshRatePolicy] to [Window]:
 * - [RefreshRatePolicy.PINNED_MAX]: locks min/max to [preferredRate] and disables VRR power-saving downclocking.
 * - [RefreshRatePolicy.ADAPTIVE_VRR]: allows Android's VRR/LTPO scheduler to scale up to [preferredRate] during motion and drop at rest.
 * - [RefreshRatePolicy.CAP_60HZ]: caps refresh rate to 60 Hz with power savings balanced.
 */
fun Window.applyPerformanceRefreshRate(
    display: Display?,
    policy: RefreshRatePolicy,
    preferredRate: Int,
    targetView: View? = null,
) {
    val layoutParams = attributes
    when (policy) {
        RefreshRatePolicy.CAP_60HZ -> {
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
        }
        RefreshRatePolicy.ADAPTIVE_VRR -> {
            val resolvedRate = display.resolvePerformanceRefreshRate(preferredRate)
            val maxRate = resolvedRate.toFloat()

            @Suppress("DEPRECATION")
            layoutParams.preferredRefreshRate = 0f

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    layoutParams.javaClass.getField("preferredMinDisplayRefreshRate").setFloat(layoutParams, 0f)
                    layoutParams.javaClass.getField("preferredMaxDisplayRefreshRate").setFloat(layoutParams, maxRate)
                } catch (_: Throwable) {}
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                setFrameRatePowerSavingsBalanced(true)
                targetView?.requestedFrameRate = View.REQUESTED_FRAME_RATE_CATEGORY_DEFAULT
            }
        }
        RefreshRatePolicy.PINNED_MAX -> {
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
    }
    attributes = layoutParams
}

fun Window.applyPerformanceRefreshRate(
    display: Display?,
    enabled: Boolean,
    preferredRate: Int,
    targetView: View? = null,
) = applyPerformanceRefreshRate(
    display = display,
    policy = if (enabled) RefreshRatePolicy.PINNED_MAX else RefreshRatePolicy.CAP_60HZ,
    preferredRate = preferredRate,
    targetView = targetView,
)

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
