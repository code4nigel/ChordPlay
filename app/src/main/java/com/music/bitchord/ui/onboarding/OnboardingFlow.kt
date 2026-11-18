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
package com.music.bitchord.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.player.MeshGradientBackground
import com.music.bitchord.ui.player.rememberArtworkColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val WELCOME_STEP = 0
private const val STYLE_STEP = 1
private const val STEP_COUNT = 2

private const val SHEET_AFTER_BONES_MS = 200L
private const val DEMO_QUERY = "hits"

/**
 * Interactive 2-step Welcome / Setup sheet hovering over the scaled app window.
 * Step 0 introduces ChordPlay's open-source community & local listener name;
 * Step 1 configures Color Palette, Black OLED, Liquid Glass, and Blur live.
 */
@Composable
fun SetupWizard(
    onFinished: () -> Unit,
    onReveal: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by rememberSaveable { mutableIntStateOf(WELCOME_STEP) }
    var name by rememberSaveable { mutableStateOf(OnboardingState.name(context)) }

    val colorPalette by AppSettings.colorPalette.collectAsStateWithLifecycle()
    val amoledDarkMode by AppSettings.amoledDarkMode.collectAsStateWithLifecycle()
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val reduceBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val intensity by AppSettings.liquidGlassIntensity.collectAsStateWithLifecycle()

    var demo by remember { mutableStateOf<Song?>(null) }

    val pace = rememberFramePace(key = step)
    val demoPalette = rememberArtworkColors(demo?.thumbnailUrl)

    LaunchedEffect(Unit) {
        demo = runCatching {
            YtMusicRepository.searchPage(DEMO_QUERY, SearchFilter.SONGS).getOrNull()
        }.getOrNull()?.rows?.firstNotNullOfOrNull { row ->
            when (row) {
                is SearchResult.Track -> row.song
                is SearchResult.TopTrack -> row.song
                else -> null
            }
        }
    }

    val reveal = remember { Animatable(0f) }

    suspend fun animateReveal(to: Float, durationMs: Int) {
        if (reduceAnimation) {
            reveal.snapTo(to)
            onReveal(to)
            return
        }
        coroutineScope {
            val animating = launch {
                reveal.animateTo(to, tween(durationMs, easing = FastOutSlowInEasing))
            }
            val reporting = launch {
                while (isActive) {
                    onReveal(reveal.value)
                    withFrameNanos { }
                }
            }
            animating.join()
            reporting.cancel()
            onReveal(to)
        }
    }

    LaunchedEffect(Unit) {
        delay(SHEET_AFTER_BONES_MS)
        animateReveal(1f, 480)
    }

    fun leave(then: () -> Unit) {
        scope.launch {
            animateReveal(0f, 300)
            then()
        }
    }

    fun finish() {
        OnboardingState.saveName(context, name)
        OnboardingState.markCompleted(context)
        leave(onFinished)
    }

    BackHandler(enabled = true) {
        if (step > 0) step-- else finish()
    }

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.54f * reveal.value }
                .background(Color.Black)
                .clickable(enabled = false, onClick = {}),
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = (1f - reveal.value) * size.height * 1.12f }
                .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                .background(Onb.Surface.copy(alpha = 0.94f)),
        ) {
            if (!reduceBlur) {
                MeshGradientBackground(
                    palette = demoPalette,
                    modifier = Modifier.fillMaxSize(),
                    trackKey = demo?.thumbnailUrl,
                    animated = false,
                    blurRadius = 54.dp,
                )
            }
            Column(Modifier.fillMaxSize()) {
                SheetRail(
                    step = step,
                    steps = STEP_COUNT,
                    onBack = { if (step > 0) step-- },
                    onSkip = ::finish,
                )
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (slideInHorizontally { if (forward) it / 4 else -it / 4 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut(tween(160)))
                    },
                    label = "chordplayOnboardingStep",
                    modifier = Modifier.weight(1f),
                ) { current ->
                    val advance = remember(current) {
                        { step = (current + 1).coerceAtMost(STEP_COUNT - 1) }
                    }
                    val footer: @Composable ColumnScope.() -> Unit = {
                        OnbPillButton(
                            text = if (current == STEP_COUNT - 1) {
                                stringResource(R.string.onboarding_get_started)
                            } else {
                                stringResource(R.string.onboarding_continue)
                            },
                            onClick = { if (current == STEP_COUNT - 1) finish() else advance() },
                        )
                    }
                    when (current) {
                        WELCOME_STEP -> WelcomeStep(
                            name = name,
                            palette = demoPalette,
                            onName = { name = it },
                            footer = footer,
                        )
                        else -> DesignStyleStep(
                            song = demo,
                            palette = demoPalette,
                            colorPalette = colorPalette,
                            onColorPalette = AppSettings::setColorPalette,
                            amoledDarkMode = amoledDarkMode,
                            onAmoledDarkMode = AppSettings::setAmoledDarkMode,
                            liquidGlass = liquidGlass,
                            onLiquidGlass = AppSettings::setLiquidGlass,
                            reduceBlur = reduceBlur,
                            onReduceBlur = AppSettings::setReduceDynamicBlur,
                            reduceAnimation = reduceAnimation,
                            onReduceAnimation = AppSettings::setReduceAnimation,
                            intensity = intensity,
                            onIntensity = AppSettings::setLiquidGlassIntensity,
                            pace = pace.value,
                            footer = footer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetRail(
    step: Int,
    steps: Int,
    onBack: () -> Unit,
    onSkip: () -> Unit,
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topInset + 10.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(38.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Onb.Title.copy(alpha = 0.22f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(enabled = step > 0, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                if (step > 0) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Onb.Title,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                repeat(steps) { index ->
                    Box(
                        modifier = Modifier
                            .width(if (index == step) 18.dp else 6.dp)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index <= step) {
                                    Brush.horizontalGradient(
                                        listOf(Onb.Title.copy(alpha = 0.95f), Onb.Title.copy(alpha = 0.62f)),
                                    )
                                } else {
                                    Brush.horizontalGradient(listOf(Onb.Hairline, Onb.Hairline))
                                },
                            ),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSkip),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.onboarding_skip),
                    tint = Onb.Muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
