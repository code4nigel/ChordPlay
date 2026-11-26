/*
 * Copyright (C) 2024-2026 Kushagra Singh and BitChord contributors <https://github.com/kushagrasinghx/BitChord>
 * Copyright (C) 2026 Shivanshu (code4nigel) <https://github.com/code4nigel/ChordPlay>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.music.bitchord.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.music.bitchord.R
import com.music.bitchord.data.model.Account
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.ic_logo
import com.music.bitchord.ui.screens.ReplayYearBadge
import com.music.bitchord.ui.screens.ReplayYearButton
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The bar's own height, above whatever inset it is sitting under.
 *
 * The single source of truth for it: the bar lays itself out to this, and
 * everything that has to clear the bar — page content padding, either top
 * backdrop, fixed headers that sit directly beneath it — measures from here
 * rather than from a copy of the number.
 */
val TopBarContentHeight = 52.dp

/**
 * The breathing room between the bar's bottom edge and the first thing under
 * it, so content rests below the glass instead of against it.
 */
val TopBarContentGap = 12.dp

/**
 * How much of each end of the bar is spoken for, so a long title truncates
 * instead of running under what sits there.
 *
 * Only ever consumed through the larger of the two — see the title's padding.
 * They are kept apart rather than collapsed into one number because they
 * describe two different things, and the wordmark is the one that changes when
 * the logo or the Dev badge does.
 */
private val BackInset = 54.dp
private val WordmarkInset = 96.dp
private val ActionsInset = 56.dp

/** Between a filling accessory and the actions pill. */
private val ACCESSORY_GAP = 8.dp

/** What the leading end of the bar needs: a back button, or the wordmark. */
private fun leadingInset(hasBack: Boolean): Dp = if (hasBack) BackInset else WordmarkInset

/**
 * How far down the window the bar actually ends: the status bar inset it is
 * pinned under, plus its own height.
 *
 * This has to be read at composition rather than baked in as a constant — the
 * inset is a property of the device and of the window, not of the app. A phone
 * with a cutout, one without, and a freeform window with no status bar at all
 * are all different numbers, and a fixed guess is wrong on all but one of them:
 * too tight and content is clipped under the bar, too loose and every page
 * opens on a band of empty space.
 */
@Composable
fun topBarHeight(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopBarContentHeight

/**
 * Where page content should start: clear of the bar, plus [TopBarContentGap].
 */
@Composable
fun topBarContentPadding(): Dp = topBarHeight() + TopBarContentGap

data class HomeBrandLockupConfig(
    val collapseProgress: Float,
    val primaryGlowColor: Color? = null,
    val isFestivalActive: Boolean = false,
    val festivalThemeType: String = "",
    val onFestivalClick: (() -> Unit)? = null,
    val replayYear: ReplayYearBadge? = null,
    val onCollapsedClick: () -> Unit = {},
)

/**
 * The top bar's content — title, back affordance, actions — over no blur of its
 * own.
 *
 * The app mounts it with [transparentBackdrop] and [artworkPageChrome] on every
 * page, in either material: no full-width pane, just floating circles over the
 * shared top gradient — liquid glass when that is on, the navbar's blur when it
 * is not. The bounded-pane path ([TopBarBlur] underneath, lower hairline, solid
 * fill under Reduce dynamic blur) is kept for previews only.
 *
 * Apple Music behaviour: the big in-list header owns the title at rest;
 * once the list scrolls, the small centered title fades in.
 */
@Composable
fun FrostedTopBar(
    title: String,
    scrolled: Boolean,
    modifier: Modifier = Modifier,
    /** Leaves the whole bar transparent so the page's own top gradient is the backdrop. */
    transparentBackdrop: Boolean = false,
    /** Circular back surface and no collapsing title, for artwork-led pages. */
    artworkPageChrome: Boolean = false,
    /** Source sampled by floating top-bar surfaces when liquid glass is off. */
    backButtonHazeState: HazeState? = null,
    trailingTitle: String? = null,
    onBack: (() -> Unit)? = null,
    refreshing: Boolean = false,
    // A lambda, not a value: the drag changes every frame, and reading it in
    // the caller would recompose the whole app on each one.
    pullFraction: () -> Float = { 0f },
    /** Optional morphing stacked-hero -> compact-pill ChordPlay lockup for Home. */
    homeBrandLockup: HomeBrandLockupConfig? = null,
    /**
     * A page control filling the bar from the leading gutter to [actions], on
     * no surface of its own — the Search tab's field, which brings its own.
     * Root tabs have no back button there for it to collide with.
     */
    accessory: (@Composable () -> Unit)? = null,
    /** False on Settings, where the docking search field runs to the trailing edge. */
    actionsShown: Boolean = true,
    /** True when [accessory] paints its own pill surface (so resizing it resizes its glass). */
    accessoryHasOwnSurface: Boolean = false,
    actions: @Composable () -> Unit = {},
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val useFloatingChrome = artworkPageChrome ||
        (LocalLiquidGlassEnabled.current && isGlassSupported())
    val titleAlpha by animateFloatAsState(
        targetValue = if (scrolled || trailingTitle != null) 1f else 0f,
        animationSpec = tween(220),
        label = "topBarTitleAlpha",
    )
    // Every bounded bar uses the same hairline, whether its pane is blurred or
    // solid. A transparent artwork page has no pane edge for a line to mark.
    val dividerColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.outline.copy(
            alpha = when {
                transparentBackdrop -> 0f
                else -> 0.6f
            },
        ),
        animationSpec = tween(220),
        label = "topBarDivider",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (reduceDynamicBlur && !transparentBackdrop) {
                    Modifier.background(MaterialTheme.colorScheme.surface)
                } else Modifier,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(TopBarContentHeight),
        ) {
            /* Temporarily hidden: keep the scrolling center-title implementation
             * intact so it can be restored without rebuilding its transitions.
             *
            // Artwork pages keep their large in-page heading and never create a
            // duplicate title in the status bar, including in accessibility.
            if (!artworkPageChrome) {
                AnimatedContent(
                    targetState = trailingTitle,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(260)) + slideInHorizontally(animationSpec = tween(260)) { it / 3 }) togetherWith
                            (fadeOut(animationSpec = tween(200)) + slideOutHorizontally(animationSpec = tween(200)) { -it / 3 })
                    },
                    label = "topBarTitleAnimation",
                    modifier = Modifier
                        .align(Alignment.Center)
                        // Reserve room for whatever flanks the title — and reserve
                        // the *same* room on both sides. Equal is the whole point:
                        // the title is centered within this padded box, so an inset
                        // that differs end to end moves it off the bar's centre by
                        // half that difference. Reserving what each side actually
                        // needs (96dp for the wordmark, 56dp for the actions) put
                        // every root tab's title 20dp right of centre, which is
                        // visible against a status bar clock that is not.
                        .padding(horizontal = max(leadingInset(onBack != null), ActionsInset))
                        .fillMaxWidth()
                        .graphicsLayer { alpha = titleAlpha },
                ) { trailing ->
                    if (trailing != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = trailing,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    } else {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
             */
            // On a pushed page the back affordance is always visible, since
            // there is no large in-list header to fall back on.
            if (onBack != null) {
                if (useFloatingChrome) {
                    ArtworkPageBackButton(
                        onClick = onBack,
                        hazeState = backButtonHazeState,
                        // The surface edge aligns with the floating navbar. The
                        // padding belongs outside the circle; its icon remains
                        // centred in the same 44dp control in every material.
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = BAR_GUTTER),
                    )
                } else {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            if (onBack == null && accessory == null && homeBrandLockup != null) {
                HomeMorphingBrandLockup(
                    config = homeBrandLockup,
                    hazeState = backButtonHazeState,
                    refreshing = refreshing,
                    pullFraction = pullFraction,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = BAR_GUTTER, top = 4.dp),
                )
            }
            // No wordmark at the leading end: the root tabs carry it above
            // their large heading instead (LargePageTitle in :sharedUi).
            // The accessory takes everything from the leading gutter up to the
            // actions, which keep their own width at the trailing end.
            val edge = if (useFloatingChrome) BAR_GUTTER else 4.dp
            val accessoryStart = when {
                onBack == null -> edge
                useFloatingChrome -> BAR_GUTTER + TOP_CONTROL_SIZE + ACCESSORY_GAP
                else -> 4.dp + 48.dp + ACCESSORY_GAP
            }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .then(if (accessory != null) Modifier.fillMaxWidth().padding(start = accessoryStart) else Modifier)
                    // Same outer edge as the navbar; PILL_INSET below is
                    // internal padding around the icons, not extra margin.
                    .padding(end = edge),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (accessory != null) {
                    if (accessoryHasOwnSurface) {
                        val glass = useFloatingChrome && LocalLiquidGlassEnabled.current && isGlassSupported() && !reduceDynamicBlur
                        Box(modifier = Modifier.weight(1f)) {
                            CompositionLocalProvider(LocalSearchFieldOnSurface provides true) {
                                if (glass) {
                                    val glassContent = glassContentColor()
                                    MaterialTheme(
                                        colorScheme = MaterialTheme.colorScheme.copy(
                                            onBackground = glassContent,
                                            onSurfaceVariant = glassContent.copy(alpha = ACCESSORY_HINT_ALPHA),
                                            primary = glassContent,
                                        ),
                                        content = accessory,
                                    )
                                } else {
                                    accessory()
                                }
                            }
                        }
                    } else {
                        AccessoryPill(
                            floating = useFloatingChrome,
                            hazeState = backButtonHazeState,
                            modifier = Modifier.weight(1f),
                            content = accessory,
                        )
                    }
                    if (actionsShown) {
                        Spacer(Modifier.width(ACCESSORY_GAP))
                    }
                }
                if (actionsShown) {
                    if (useFloatingChrome) {
                        ArtworkPageActions(hazeState = backButtonHazeState, content = actions)
                    } else {
                        actions()
                    }
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
        if (homeBrandLockup == null) {
            RefreshPuck(refreshing = refreshing, pullFraction = pullFraction)
        }
    }
}

/**
 * The bar's accessory — the Search tab's field — as a squircle pill at the
 * height of the controls beside it. On floating chrome it is cut from the same
 * surface as those controls, its glyphs and words in the glass's content
 * colour; on the plain bar it keeps the field's own fill. Same shape and size
 * either way, so turning Liquid Glass off changes the material and nothing else.
 */
@Composable
private fun AccessoryPill(
    floating: Boolean,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val glass = floating && LocalLiquidGlassEnabled.current && isGlassSupported() && !reduceDynamicBlur
    val shape = remember { SquirclePillShape() }
    Box(
        modifier = modifier
            .height(TOP_CONTROL_SIZE)
            .then(
                if (floating) {
                    artworkPageSurface(shape = shape, hazeState = hazeState)
                } else {
                    Modifier
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                },
            ),
    ) {
        CompositionLocalProvider(LocalSearchFieldOnSurface provides true) {
            if (glass) {
                val glassContent = glassContentColor()
                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        onBackground = glassContent,
                        onSurfaceVariant = glassContent.copy(alpha = ACCESSORY_HINT_ALPHA),
                        primary = glassContent,
                    ),
                    content = content,
                )
            } else {
                content()
            }
        }
    }
}

private const val ACCESSORY_HINT_ALPHA = 0.6f
private val TOP_CONTROL_SIZE = 44.dp

/**
 * Continuous scroll-driven lockup for Home:
 * - Expanded (`collapse = 0`): large glowing `ic_logo` aligned with the right profile avatar,
 *   with `"ChordPlay"` stacked cleanly below it.
 * - Collapsed (`collapse = 1`): compacts into a frosted glass `[ic_logo ChordPlay]` capsule
 *   horizontally aligned with the right profile selector.
 */
@Composable
private fun HomeMorphingBrandLockup(
    config: HomeBrandLockupConfig,
    hazeState: HazeState?,
    refreshing: Boolean = false,
    pullFraction: () -> Float = { 0f },
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val t = config.collapseProgress.coerceIn(0f, 1f)
    val eased = t * t * (3f - 2f * t)
    val pillAlpha = ((t - 0.35f) / 0.65f).coerceIn(0f, 1f)

    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val useLiquidGlass = LocalLiquidGlassEnabled.current && isGlassSupported()
    val collapsedContentColor = if (useLiquidGlass && !reduceDynamicBlur) {
        glassContentColor()
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val expandedTextColor = MaterialTheme.colorScheme.onBackground
    val titleColor = lerp(expandedTextColor, collapsedContentColor, pillAlpha)

    val expandedIconSize = TOP_CONTROL_SIZE // 44.dp aligned with right-side TopBarAccountButton
    val collapsedIconSize = 22.dp
    val iconScale = 1f + ((collapsedIconSize / expandedIconSize) - 1f) * eased
    val glowAlpha = 1f - eased * 0.78f
    val primaryColor = MaterialTheme.colorScheme.primary
    val haloColor = config.primaryGlowColor ?: primaryColor

    val expandedFontSize = 26.sp
    val collapsedFontSize = 16.5.sp
    val textScaleRatio = collapsedFontSize.value / expandedFontSize.value
    val textScale = 1f + (textScaleRatio - 1f) * eased

    var measuredTextWidthPx by remember { mutableIntStateOf(0) }
    var measuredTextHeightPx by remember { mutableIntStateOf(0) }

    val startPadPx = with(density) { 12.dp.toPx() }
    val endPadPx = with(density) { 14.dp.toPx() }
    val collapsedIconPx = with(density) { collapsedIconSize.toPx() }
    val gapPx = with(density) { 7.dp.toPx() }
    val halfTopBoxPx = with(density) { (TOP_CONTROL_SIZE / 2f).toPx() }
    val expandedIconPx = with(density) { expandedIconSize.toPx() }
    val titleTopGapPx = with(density) { 6.dp.toPx() }

    val fallbackWidthPx = with(density) { 122.dp.toPx() }.roundToInt()
    val effectiveTextW = if (measuredTextWidthPx > 0) measuredTextWidthPx else fallbackWidthPx
    val fallbackHeightPx = with(density) { 30.sp.toPx() }.roundToInt()
    val effectiveTextH = if (measuredTextHeightPx > 0) measuredTextHeightPx else fallbackHeightPx

    val collapsedTextW = effectiveTextW * textScaleRatio
    val collapsedPillWidthDp = with(density) {
        (startPadPx + collapsedIconPx + gapPx + collapsedTextW + endPadPx).toDp()
    }
    val expandedTextTransY = (expandedIconPx + titleTopGapPx) - (halfTopBoxPx - effectiveTextH * 0.5f)
    val collapsedTextTransX = startPadPx + collapsedIconPx + gapPx

    val badgeExtraWidthDp = if (config.isFestivalActive && config.festivalThemeType == "diwali") 28.dp else 0.dp
    val replayBadgeOffsetDp = with(density) {
        val expandedX = expandedIconPx + with(density) { 10.dp.toPx() } + with(density) { badgeExtraWidthDp.toPx() }
        val collapsedX = collapsedPillWidthDp.toPx() + with(density) { 8.dp.toPx() }
        (expandedX + (collapsedX - expandedX) * eased).toDp()
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier = modifier.height(TOP_CONTROL_SIZE)) {
        // Frosted glass pill surface behind collapsed lockup
        Box(
            modifier = Modifier
                .width(collapsedPillWidthDp)
                .height(TOP_CONTROL_SIZE)
                .graphicsLayer { alpha = pillAlpha }
                .then(artworkPageSurface(shape = CircleShape, hazeState = hazeState))
                .then(
                    if (t > 0.35f) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = config.onCollapsedClick,
                        )
                    } else {
                        Modifier
                    },
                ),
        )

        // Morphing ChordPlay icon mark + ambient artwork aura
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(expandedIconSize)
                .graphicsLayer {
                    translationX = startPadPx * eased
                    scaleX = iconScale
                    scaleY = iconScale
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .then(
                    if (t <= 0.35f && config.isFestivalActive && config.onFestivalClick != null) {
                        Modifier
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = config.onFestivalClick,
                            )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            ChordPlayPullRefreshIcon(
                iconSize = expandedIconSize,
                primaryColor = primaryColor,
                haloColor = haloColor,
                glowAlpha = glowAlpha,
                refreshing = refreshing,
                pullFraction = pullFraction,
                reduceAnimation = reduceAnimation,
            )
        }

        // Morphing ChordPlay wordmark: stacked beneath icon at rest, tucked beside icon when scrolled
        Text(
            text = "ChordPlay",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = expandedFontSize,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
            ),
            color = titleColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .onSizeChanged {
                    measuredTextWidthPx = it.width
                    measuredTextHeightPx = it.height
                }
                .graphicsLayer {
                    translationX = collapsedTextTransX * eased
                    translationY = expandedTextTransY * (1f - eased)
                    scaleX = textScale
                    scaleY = textScale
                    transformOrigin = TransformOrigin(0f, 0.5f)
                },
        )

        // Optional festival diya or Replay year badge stays interactive on the top row
        if (config.isFestivalActive && config.festivalThemeType == "diwali") {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = expandedIconSize + 8.dp)
                    .graphicsLayer {
                        alpha = 1f - eased
                    },
            ) {
                AnimatedDiya(size = 22.dp)
            }
        }
        config.replayYear?.let { badge ->
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = replayBadgeOffsetDp),
            ) {
                ReplayYearButton(badge = badge, height = 22.dp)
            }
        }
    }
}

private const val HOME_REFRESH_MIN_CYCLE_MS = 1150L

/**
 * Animated Home brand mark for pull-to-refresh:
 * As the user pulls down, the ChordPlay emblem winds up with a progressive orbital arc;
 * during refresh it morphs cleanly into a circling harmonic wave icon, then settles back
 * into the ChordPlay emblem once complete.
 */
@Composable
private fun ChordPlayPullRefreshIcon(
    iconSize: Dp,
    primaryColor: Color,
    haloColor: Color,
    glowAlpha: Float,
    refreshing: Boolean,
    pullFraction: () -> Float,
    reduceAnimation: Boolean,
) {
    val currentPull by rememberUpdatedState(pullFraction)
    var visualRefreshing by remember { mutableStateOf(refreshing) }
    var refreshStartedAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(refreshing, reduceAnimation) {
        if (refreshing) {
            refreshStartedAt = System.currentTimeMillis()
            visualRefreshing = true
        } else if (visualRefreshing) {
            if (!reduceAnimation) {
                val remaining = HOME_REFRESH_MIN_CYCLE_MS - (System.currentTimeMillis() - refreshStartedAt)
                if (remaining > 0L) delay(remaining)
            }
            visualRefreshing = false
        }
    }

    val pullProgress by remember {
        derivedStateOf { currentPull().coerceIn(0f, 1.35f) }
    }
    val pullClamped = pullProgress.coerceIn(0f, 1f)
    val refreshMix by animateFloatAsState(
        targetValue = if (visualRefreshing) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 260f),
        label = "chordPlayPullRefreshMix",
    )

    val waveActive = (refreshMix > 0.02f || visualRefreshing) && !reduceAnimation
    val orbitDeg = if (waveActive) {
        val transition = rememberInfiniteTransition(label = "chordPlayRefreshOrbit")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "orbitDeg",
        ).value
    } else {
        0f
    }

    val wavePhase = if (waveActive) {
        val transition = rememberInfiniteTransition(label = "chordPlayRefreshWave")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "wavePhase",
        ).value
    } else {
        0f
    }

    val logoAlpha = (1f - refreshMix * 1.25f).coerceIn(0f, 1f)
    val logoScale = 1f - 0.32f * refreshMix - (if (!visualRefreshing) 0.14f * pullClamped else 0f)
    val logoRotation = if (visualRefreshing) 140f * refreshMix else -24f * pullClamped
    val waveAlpha = ((refreshMix - 0.10f) / 0.90f).coerceIn(0f, 1f)
    val haloBoost = (glowAlpha + 0.30f * maxOf(pullClamped, refreshMix)).coerceIn(0f, 1f)

    Box(
        modifier = Modifier.size(iconSize),
        contentAlignment = Alignment.Center,
    ) {
        if (haloBoost > 0.02f) {
            Canvas(
                modifier = Modifier
                    .size(iconSize + 14.dp)
                    .graphicsLayer { alpha = haloBoost },
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to haloColor.copy(alpha = 0.44f),
                        0.55f to primaryColor.copy(alpha = 0.16f),
                        1f to Color.Transparent,
                    ),
                )
            }
        }

        // Pull progress sweep ring (visible while pulling before refresh settles back)
        if (!visualRefreshing && pullProgress > 0.02f && refreshMix < 0.05f) {
            Canvas(modifier = Modifier.size(iconSize)) {
                val stroke = 2.4.dp.toPx()
                val pad = stroke * 0.9f
                val arcSize = Size(size.width - pad * 2f, size.height - pad * 2f)
                drawArc(
                    color = primaryColor.copy(alpha = (0.30f + 0.60f * pullClamped).coerceIn(0f, 0.9f)),
                    startAngle = -90f + pullProgress * 75f,
                    sweepAngle = (pullClamped * 290f).coerceIn(0f, 300f),
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }

        // Resting / pulling ChordPlay emblem
        if (logoAlpha > 0.01f) {
            Icon(
                painter = painterResource(Res.drawable.ic_logo),
                contentDescription = "ChordPlay",
                tint = primaryColor,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        alpha = logoAlpha
                        scaleX = logoScale
                        scaleY = logoScale
                        rotationZ = logoRotation
                    },
            )
        }

        // Active circling Wave emblem
        if (waveAlpha > 0.01f) {
            Canvas(
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        alpha = waveAlpha
                        val s = 0.72f + 0.28f * waveAlpha
                        scaleX = s
                        scaleY = s
                    },
            ) {
                val w = size.width
                val h = size.height
                val cx = w * 0.5f
                val cy = h * 0.5f
                val stroke = 2.6.dp.toPx()
                val arcPad = stroke * 0.85f
                val arcSize = Size(w - arcPad * 2f, h - arcPad * 2f)

                // Soft luminous center medallion
                drawCircle(
                    color = primaryColor.copy(alpha = 0.16f * waveAlpha),
                    radius = w * 0.42f,
                    center = Offset(cx, cy),
                )

                // Revolving circular orbital arc around the wave bars
                rotate(degrees = orbitDeg, pivot = Offset(cx, cy)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.0f to primaryColor.copy(alpha = 0.06f),
                            0.45f to primaryColor.copy(alpha = 0.55f),
                            0.72f to primaryColor,
                            1.0f to primaryColor.copy(alpha = 0.06f),
                            center = Offset(cx, cy),
                        ),
                        startAngle = -90f,
                        sweepAngle = 260f,
                        useCenter = false,
                        topLeft = Offset(arcPad, arcPad),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    // Orbiting wave crest pearl
                    val r = (w - arcPad * 2f) * 0.5f
                    drawCircle(
                        color = primaryColor,
                        radius = stroke * 0.72f,
                        center = Offset(cx, cy - r),
                    )
                }

                // Centered animated 5-bar sonic wave glyph
                val barCount = 5
                val barW = w * 0.072f
                val gap = w * 0.048f
                val totalBarsW = barCount * barW + (barCount - 1) * gap
                val startX = cx - totalBarsW * 0.5f
                val envelope = floatArrayOf(0.55f, 0.85f, 1.0f, 0.85f, 0.55f)
                for (i in 0 until barCount) {
                    val animFactor = if (reduceAnimation) {
                        0.72f
                    } else {
                        0.50f + 0.50f * sin((wavePhase + i * 0.85f).toDouble()).toFloat()
                    }
                    val barH = (h * 0.42f * envelope[i] * animFactor).coerceAtLeast(barW)
                    val x = startX + i * (barW + gap)
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(x, cy - barH * 0.5f),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW * 0.5f, barW * 0.5f),
                    )
                }
            }
        }
    }
}

/** Circular floating back affordance, material-matched to the navbar. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun ArtworkPageBackButton(
    onClick: () -> Unit,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
) {
    val shape = CircleShape
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val useLiquidGlass = LocalLiquidGlassEnabled.current && isGlassSupported()
    val contentColor = if (useLiquidGlass && !reduceDynamicBlur) {
        glassContentColor()
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .then(artworkPageSurface(shape = shape, hazeState = hazeState)),
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = contentColor,
        )
    }
}

/** One shared pill for every action at the right of a floating top bar. */
@Composable
private fun ArtworkPageActions(
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .then(artworkPageSurface(shape = CircleShape, hazeState = hazeState))
            // Keep the right edge fixed while a new action opens room to its
            // left. The surface itself therefore grows instead of jumping to
            // its new width in a single frame.
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            // One 48dp profile target with no inset is a true 48x48 circle.
            // Once another action exists, restore the navbar's PILL_INSET at
            // both edges. This is layout padding inside the surface, not an
            // outer margin, so BAR_GUTTER remains unchanged.
            .artworkActionEdgePadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

/**
 * Navbar edge padding that collapses only for the profile-only state.
 *
 * Invisible/animating action slots can measure between zero and 48dp, so the
 * natural content width—not the number of emitted composables—is the reliable
 * source of truth. At exactly one icon target the pill stays circular; above
 * that it gains the same inset as the navbar while retaining fully round ends.
 */
private fun Modifier.artworkActionEdgePadding(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0))
    val oneActionWidth = 48.dp.roundToPx()
    val extraActionFraction = ((placeable.width - oneActionWidth).toFloat() / oneActionWidth)
        .coerceIn(0f, 1f)
    val edgePadding = (PILL_INSET.roundToPx() * extraActionFraction).roundToInt()
    val width = (placeable.width + edgePadding * 2).coerceIn(constraints.minWidth, constraints.maxWidth)

    layout(width, placeable.height) {
        placeable.placeRelative(edgePadding, 0)
    }
}

/** The navbar's exact material choice, reusable by every floating top control. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
internal fun artworkPageSurface(
    shape: CornerBasedShape,
    hazeState: HazeState?,
): Modifier {
    val container = MaterialTheme.colorScheme.surface
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val useLiquidGlass = LocalLiquidGlassEnabled.current && isGlassSupported()

    return Modifier
        .clip(shape)
        .then(
            when {
                useLiquidGlass -> Modifier.liquidGlass(shape)
                reduceDynamicBlur || hazeState == null -> Modifier.background(container)
                else -> Modifier.optimizedHazeEffect(
                    state = hazeState,
                    style = HazeMaterials.regular(container),
                )
            },
        )
        // Keep the same explicit hairline as FloatingBottomBar. Liquid glass
        // also has its refractive highlight, but the navbar retains this edge
        // so these surfaces do as well.
        .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
}

/**
 * The account affordance at the right end of the bar.
 *
 * It is the signed-in Google account's own photo — the same one YouTube Music
 * shows there — and tapping it opens Settings, where the account lives. Signed
 * out, or before the account menu has come back, it falls back to a person
 * glyph on a filled circle so the tap target never disappears.
 *
 * The hairline ring is what keeps a photo with light edges from dissolving into
 * the bar's glass; it is the same one thumbnails elsewhere carry.
 */
@Composable
fun TopBarAccountButton(
    account: Account?,
    onClick: () -> Unit,
    onSwipeProfile: ((forward: Boolean) -> Boolean)? = null,
    modifier: Modifier = Modifier,
) {
    val translation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    // Wrapped in an IconButton so it keeps the 48dp target, the ripple and the
    // spacing every other action in this bar has.
    IconButton(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer { translationY = translation.value }
            .pointerInput(onSwipeProfile) {
                if (onSwipeProfile == null) return@pointerInput
                var drag = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { change, amount -> change.consume(); drag += amount },
                    onDragEnd = {
                        if (kotlin.math.abs(drag) < 28f) return@detectVerticalDragGestures
                        if (!onSwipeProfile.invoke(drag > 0f)) scope.launch {
                            translation.snapTo(if (drag > 0f) 9f else -9f)
                            translation.animateTo(0f, spring())
                        }
                    },
                )
            },
    ) {
        val photo = account?.thumbnailUrl
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = stringResource(R.string.switch_account),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
                    .thumbnailBorder(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .thumbnailBorder(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = stringResource(R.string.switch_account),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * The refresh indicator: a round puck that slides out from under the bar's
 * bottom edge as the list is pulled, Chrome-style.
 *
 * It is drawn by the bar rather than by the pull-to-refresh box, so it lands on
 * top of the glass instead of behind it, and it is clipped to the band just
 * below the bar so it emerges from the edge instead of appearing over the
 * status bar. Its position is the pull state's own distance fraction — which
 * the pull box holds at 1 while refreshing and animates back to 0 when done —
 * so the slide down, the rest while loading and the slide back up are all one
 * value. The arc fills with the drag, then spins once the refresh is away.
 *
 * Laid out at zero height, so the bar keeps its size and the puck overhangs
 * the content without taking any touches from it.
 */
@Composable
private fun RefreshPuck(refreshing: Boolean, pullFraction: () -> Float, modifier: Modifier = Modifier) {
    val currentFraction by rememberUpdatedState(pullFraction)
    val pulling by remember { derivedStateOf { currentFraction() > 0f } }
    if (!refreshing && !pulling) return

    val travel = with(LocalDensity.current) { (PUCK_REST + PUCK_SIZE).toPx() }
    Box(
        modifier = modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minHeight = 0))
                layout(placeable.width, 0) { placeable.place(0, 0) }
            }
            .fillMaxWidth()
            .height(PUCK_REST + PUCK_SIZE + PUCK_OVERSHOOT + 12.dp)
            .clipToBounds(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val f = currentFraction().coerceIn(0f, 2f)
                    // Past the threshold the puck keeps following, but
                    // reluctantly, like the list under it.
                    val eased = if (f <= 1f) f else 1f + (f - 1f) * 0.35f
                    translationY = travel * eased - PUCK_SIZE.toPx()
                    alpha = (f * 3f).coerceAtMost(1f)
                }
                .shadow(6.dp, CircleShape)
                .size(PUCK_SIZE)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val indicatorModifier = Modifier.size(22.dp)
            if (refreshing) {
                CircularProgressIndicator(
                    modifier = indicatorModifier,
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp,
                    strokeCap = StrokeCap.Round,
                )
            } else {
                CircularProgressIndicator(
                    progress = { (currentFraction() * 0.8f).coerceIn(0f, 0.8f) },
                    modifier = indicatorModifier.graphicsLayer {
                        rotationZ = currentFraction().coerceIn(0f, 2f) * 180f
                    },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                    strokeWidth = 2.5.dp,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                )
            }
        }
    }
}

private val PUCK_SIZE = 40.dp

/** How far below the bar's edge the puck rests while a refresh runs. */
private val PUCK_REST = 16.dp

/** Headroom for the damped travel past the threshold. */
private val PUCK_OVERSHOOT = 20.dp

/**
 * The account photo's diameter.
 *
 * Smaller than an icon's 24dp box: a filled circle carries more weight than a
 * glyph does, and at 24 it sat heavier in the bar than the wordmark opposite it.
 */
private val AVATAR_SIZE = 28.dp
