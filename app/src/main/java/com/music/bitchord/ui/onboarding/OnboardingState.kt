package com.music.bitchord.ui.onboarding

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists first-run onboarding state locally without cluttering user preferences.
 *
 * Uses dedicated SharedPreferences so launch checks can run immediately in the
 * activity, and shows automatically only on fresh installs (existing installs
 * upgrading can replay anytime from Settings).
 */
object OnboardingState {

    private const val FILE = "bitchord_onboarding"
    private const val KEY_COMPLETED = "onboarding_completed"
    private const val KEY_NAME = "listener_name"

    private const val SETTINGS_FILE = "bitchord_settings"
    private const val KEY_LAST_VERSION_CODE = "last_version_code"

    /** True once setup has been completed or skipped. */
    fun isCompleted(context: Context): Boolean =
        runCatching {
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_COMPLETED, false)
        }.getOrDefault(true)

    /**
     * Raises welcome setup automatically on fresh installs, while upgrades for
     * existing listeners remain uninterrupted (and can still replay from Settings).
     */
    fun shouldShow(context: Context): Boolean {
        val onboarding = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (onboarding.contains(KEY_COMPLETED)) {
            return !onboarding.getBoolean(KEY_COMPLETED, false)
        }
        return !hasLaunchedBefore(context)
    }

    private fun hasLaunchedBefore(context: Context): Boolean =
        runCatching {
            context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE)
                .contains(KEY_LAST_VERSION_CODE)
        }.getOrDefault(true)

    fun markCompleted(context: Context) {
        runCatching {
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_COMPLETED, true).apply()
        }
    }

    /** Local listener name stored on this device. Blank falls back to a neutral label. */
    fun name(context: Context): String =
        runCatching {
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_NAME, "").orEmpty()
        }.getOrDefault("")

    fun saveName(context: Context, name: String) {
        runCatching {
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit().putString(KEY_NAME, name.trim()).apply()
        }
    }

    fun reset(context: Context) {
        runCatching {
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_COMPLETED, false).apply()
        }
    }

    private val _replay = MutableStateFlow(false)
    val replay: StateFlow<Boolean> = _replay.asStateFlow()

    /** Triggers the interactive setup sheet from Settings (About / Support & Community). */
    fun requestReplay() {
        _replay.value = true
    }

    fun replayHandled() {
        _replay.value = false
    }
}

