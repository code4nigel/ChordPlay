/*
 * Copyright (C) 2026 Shivanshu (code4nigel) <https://github.com/code4nigel/ChordPlay>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.music.bitchord.data.settings

import android.content.SharedPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppSettingsPerformancePolicyTest {

    private lateinit var testPrefs: InMemorySharedPreferences
    private var previousPrefs: Any? = null
    private var previousAutoTier: PerformanceTier = PerformanceTier.BALANCED
    private var previousProfile: PerformanceProfile = PerformanceProfile.AUTO
    private var previousLink: Boolean = false
    private var previousAutomixMode: AutomixPerformanceMode = AutomixPerformanceMode.BALANCED
    private var previousAnimatedCanvas: Boolean = true
    private var previousLiveBlur: Boolean = false
    private var previousBlurOverride: Any? = null
    private var previousAnimOverride: Any? = null

    private fun field(name: String) = AppSettings::class.java.getDeclaredField(name).apply {
        isAccessible = true
    }

    @Before
    fun setUp() {
        testPrefs = InMemorySharedPreferences()
        val prefsField = field("prefs")
        previousPrefs = prefsField.get(AppSettings)
        previousAutoTier = AppSettings.autoDeviceTier.value
        previousProfile = AppSettings.performanceProfile.value
        previousLink = AppSettings.linkAutomixAndCanvas.value
        previousAutomixMode = AppSettings.automixPerformanceMode.value
        previousAnimatedCanvas = AppSettings.animatedCanvas.value
        previousLiveBlur = AppSettings.liveBlur.value
        previousBlurOverride = field("reduceBlurOverride").get(AppSettings)
        previousAnimOverride = field("reduceAnimationOverride").get(AppSettings)

        prefsField.set(AppSettings, testPrefs)
        field("reduceBlurOverride").set(AppSettings, null)
        field("reduceAnimationOverride").set(AppSettings, null)
        AppSettings.autoDeviceTier.value = PerformanceTier.BALANCED
        AppSettings.linkAutomixAndCanvas.value = false
        AppSettings.setPerformanceProfile(PerformanceProfile.BALANCED)
    }

    @After
    fun tearDown() {
        field("reduceBlurOverride").set(AppSettings, previousBlurOverride)
        field("reduceAnimationOverride").set(AppSettings, previousAnimOverride)
        AppSettings.autoDeviceTier.value = previousAutoTier
        AppSettings.performanceProfile.value = previousProfile
        AppSettings.linkAutomixAndCanvas.value = previousLink
        AppSettings.automixPerformanceMode.value = previousAutomixMode
        AppSettings.animatedCanvas.value = previousAnimatedCanvas
        AppSettings.liveBlur.value = previousLiveBlur
        val applyMethod = AppSettings::class.java.getDeclaredMethod("applyPerformancePolicy", Boolean::class.javaPrimitiveType)
        applyMethod.isAccessible = true
        applyMethod.invoke(AppSettings, false)
        field("prefs").set(AppSettings, previousPrefs)
    }

    @Test
    fun manualBlurAndMotionOverridesCustomizePolicyUntilProfileReselected() {
        assertFalse(AppSettings.performancePolicy.value.isCustomized)

        AppSettings.setReduceDynamicBlur(true)
        assertTrue(AppSettings.reduceDynamicBlur.value)
        assertTrue(AppSettings.performancePolicy.value.isCustomized)
        assertFalse(AppSettings.disableInfiniteLoops.value)

        AppSettings.setReduceAnimation(true)
        assertTrue(AppSettings.reduceAnimation.value)
        assertTrue(AppSettings.disableInfiniteLoops.value)
        assertTrue(AppSettings.performancePolicy.value.isCustomized)

        // Switching or re-selecting a profile clears manual overrides cleanly.
        AppSettings.setPerformanceProfile(PerformanceProfile.BALANCED)
        assertFalse(AppSettings.performancePolicy.value.isCustomized)
        assertFalse(AppSettings.reduceDynamicBlur.value)
        assertFalse(AppSettings.reduceAnimation.value)
        assertFalse(AppSettings.disableInfiniteLoops.value)
    }

    @Test
    fun standaloneAutomixAndCanvasStayUntouchedUnlessLinked() {
        AppSettings.automixPerformanceMode.value = AutomixPerformanceMode.PERFORMANCE
        AppSettings.animatedCanvas.value = true
        AppSettings.liveBlur.value = true

        AppSettings.setPerformanceProfile(PerformanceProfile.SAVER)
        assertTrue(AppSettings.reduceDynamicBlur.value)
        assertEquals(AutomixPerformanceMode.PERFORMANCE, AppSettings.automixPerformanceMode.value)
        assertTrue(AppSettings.animatedCanvas.value)
        assertTrue(AppSettings.liveBlur.value)
    }

    @Test
    fun linkingAutomixAndCanvasSnapshotsAndRestoresStandaloneChoices() {
        AppSettings.automixPerformanceMode.value = AutomixPerformanceMode.PERFORMANCE
        AppSettings.animatedCanvas.value = true
        AppSettings.liveBlur.value = true

        AppSettings.setLinkAutomixAndCanvas(true)
        assertTrue(AppSettings.linkAutomixAndCanvas.value)
        assertEquals(AutomixPerformanceMode.BALANCED, AppSettings.automixPerformanceMode.value)
        assertTrue(AppSettings.animatedCanvas.value)
        assertTrue(AppSettings.liveBlur.value)

        AppSettings.setPerformanceProfile(PerformanceProfile.SAVER)
        assertEquals(AutomixPerformanceMode.EFFICIENT, AppSettings.automixPerformanceMode.value)
        assertFalse(AppSettings.animatedCanvas.value)
        assertFalse(AppSettings.liveBlur.value)

        AppSettings.setPerformanceProfile(PerformanceProfile.HIGH)
        assertEquals(AutomixPerformanceMode.PERFORMANCE, AppSettings.automixPerformanceMode.value)
        assertTrue(AppSettings.animatedCanvas.value)
        assertTrue(AppSettings.liveBlur.value)

        AppSettings.setPerformanceProfile(PerformanceProfile.SAVER)
        assertEquals(AutomixPerformanceMode.EFFICIENT, AppSettings.automixPerformanceMode.value)

        // Unlinking must restore pre-link values cleanly.
        AppSettings.setLinkAutomixAndCanvas(false)
        assertFalse(AppSettings.linkAutomixAndCanvas.value)
        assertEquals(AutomixPerformanceMode.PERFORMANCE, AppSettings.automixPerformanceMode.value)
        assertTrue(AppSettings.animatedCanvas.value)
        assertTrue(AppSettings.liveBlur.value)
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = data.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (data[key] as? Set<String>)?.toMutableSet() ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor(this)
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class Editor(private val prefs: InMemorySharedPreferences) : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private val removed = mutableSetOf<String>()
            private var clearAll = false

            override fun putString(key: String?, value: String?) = apply { if (key != null) pending[key] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { if (key != null) pending[key] = values }
            override fun putInt(key: String?, value: Int) = apply { if (key != null) pending[key] = value }
            override fun putLong(key: String?, value: Long) = apply { if (key != null) pending[key] = value }
            override fun putFloat(key: String?, value: Float) = apply { if (key != null) pending[key] = value }
            override fun putBoolean(key: String?, value: Boolean) = apply { if (key != null) pending[key] = value }
            override fun remove(key: String?) = apply { if (key != null) removed.add(key) }
            override fun clear() = apply { clearAll = true }
            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearAll) prefs.data.clear()
                removed.forEach { prefs.data.remove(it) }
                prefs.data.putAll(pending)
            }
        }
    }
}
