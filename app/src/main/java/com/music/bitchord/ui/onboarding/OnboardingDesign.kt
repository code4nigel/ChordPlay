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

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.lightweightLiquidGlass
import kotlinx.coroutines.isActive
import kotlin.math.abs

/**
 * Monochrome chrome palette for ChordPlay onboarding so the live cover-tinted
 * backdrop and preview card own the colour.
 */
internal object Onb {
    val Title = Color.White
    val Subtitle = Color(0xFFB7B7C0)
    val Muted = Color(0xFF8A8A94)
    val Faint = Color(0xFF5E5E68)
    val Ink = Color(0xFF101014)

    val Surface = Color(0xFF17171C)
    val SurfaceHigh = Color(0xFF202027)
    val SurfacePressed = Color(0xFF2A2A33)
    val Hairline = Color.White.copy(alpha = 0.08f)

    val PillPrimary = Color(0xFFEDEDF2)
    val PillSecondary = Color(0xFF26262E)
    val AccentCopper = Color(0xFFE5A97D)

    val MaxContentWidth = 560.dp
    val TopClearance = 36.dp
}

internal val OnbTitleTracking = (-0.04).em
internal val OnbSubtitleTracking = (-0.035).em

@Composable
internal fun Modifier.onbSurface(
    shape: RoundedCornerShape,
    elevated: Boolean = false,
): Modifier = this.lightweightLiquidGlass(
    shape = shape,
    fallbackColor = if (elevated) Onb.SurfaceHigh else Onb.Surface,
)

@Composable
internal fun OnboardingPage(
    badge: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val side = remember(maxWidth) { if (maxWidth < 340.dp) 20.dp else 26.dp }
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = Onb.MaxContentWidth)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = side)
                    .padding(top = Onb.TopClearance, bottom = 16.dp),
            ) {
                badge()
                Spacer(Modifier.height(18.dp))
                Text(
                    text = title,
                    color = Onb.Title,
                    fontSize = 31.sp,
                    lineHeight = 35.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = OnbTitleTracking,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = subtitle,
                        color = Onb.Subtitle,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = OnbSubtitleTracking,
                    )
                }
                Spacer(Modifier.height(20.dp))
                content()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = side)
                    .padding(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                content = footer,
            )
        }
    }
}

@Composable
internal fun PageBadge(icon: ImageVector, size: Dp = 44.dp, tint: Color = Onb.Ink) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFF7F7FA), Color(0xFFCFCFD8)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
internal fun OnbPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = remember { CircleShape }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .graphicsLayer {
                val s = if (pressed && enabled) 0.975f else 1f
                scaleX = s
                scaleY = s
                alpha = if (enabled) 1f else 0.4f
            }
            .onbSurface(shape = RoundedCornerShape(50), elevated = !primary)
            .clip(shape)
            .background(if (primary) Onb.PillPrimary.copy(alpha = 0.94f) else Onb.PillSecondary.copy(alpha = 0.7f))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (primary) Onb.Ink else Onb.Title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun OnbTextAction(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Onb.Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun OnbCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .graphicsLayer {
                val s = if (pressed) 0.988f else 1f
                scaleX = s
                scaleY = s
            }
            .onbSurface(shape = shape)
            .clip(shape)
            .background(if (pressed) Onb.SurfacePressed.copy(alpha = 0.5f) else Color.Transparent)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            ),
        content = { content() },
    )
}

@Composable
internal fun OnbToggle(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    OnbCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (enabled) onCheckedChange(!checked) },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Onb.SurfaceHigh.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (checked) Onb.Title else Onb.Muted,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Onb.Title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(description, color = Onb.Subtitle, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Spacer(Modifier.size(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = { if (enabled) onCheckedChange(it) },
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Onb.PillPrimary,
                    checkedThumbColor = Onb.Ink,
                    uncheckedTrackColor = Onb.SurfaceHigh,
                    uncheckedThumbColor = Onb.Muted,
                ),
            )
        }
    }
}

@Composable
internal fun OnbField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Done,
) {
    val shape = remember { RoundedCornerShape(50) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            color = Onb.Title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = OnbSubtitleTracking,
        ),
        cursorBrush = SolidColor(Onb.Title),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onbSurface(shape = shape)
                    .clip(shape)
                    .padding(horizontal = 18.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.size(10.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = Onb.Faint, fontSize = 16.sp)
                    }
                    inner()
                }
            }
        },
    )
}

@Composable
internal fun OnbSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = remember { RoundedCornerShape(50) }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cell = (maxWidth - 8.dp) / options.size.coerceAtLeast(1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onbSurface(shape = shape)
                .clip(shape)
                .padding(4.dp),
        ) {
            val offset by animateDpAsState(
                targetValue = cell * selected,
                animationSpec = tween(240),
                label = "onbSegment",
            )
            Box(
                modifier = Modifier
                    .padding(start = offset)
                    .widthIn(min = cell.coerceAtLeast(1.dp))
                    .height(40.dp)
                    .clip(shape)
                    .background(Onb.PillSecondary.copy(alpha = 0.95f)),
            )
            Row(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(shape)
                            .clickable { onSelect(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            color = if (index == selected) Onb.Title else Onb.Muted,
                            fontSize = 13.sp,
                            fontWeight = if (index == selected) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun OnbSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = Onb.Faint,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.12.em,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 4.dp),
    )
}

@Composable
internal fun OnbDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp)
            .height(1.dp)
            .background(Onb.Hairline),
    )
}

/** Low-pass filtered accelerometer tilt, gated when [AppSettings.reduceAnimation] is active. */
@Composable
internal fun rememberDeviceTilt(maxDegrees: Float = 5f): () -> Pair<Float, Float> {
    val context = LocalContext.current
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val tilt = remember { mutableStateOf(0f to 0f) }
    DisposableEffect(context, reduceAnimation) {
        if (reduceAnimation) {
            tilt.value = 0f to 0f
            return@DisposableEffect onDispose { }
        }
        val manager = runCatching {
            context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        }.getOrNull()
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (manager == null || sensor == null) return@DisposableEffect onDispose { }
        val listener = object : SensorEventListener {
            private var lastX = 0f
            private var lastY = 0f

            override fun onSensorChanged(event: SensorEvent) {
                val rawX = (-event.values[0] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
                val rawY = (event.values[1] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
                val (x, y) = tilt.value
                val nextX = x * 0.88f + rawX * 0.12f
                val nextY = y * 0.88f + rawY * 0.12f
                if (abs(nextX - lastX) < TILT_DEADBAND && abs(nextY - lastY) < TILT_DEADBAND) return
                lastX = nextX
                lastY = nextY
                tilt.value = nextX to nextY
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val registered = runCatching {
            manager.registerListener(listener, sensor, TILT_SAMPLE_MICROS)
        }.getOrDefault(false)
        onDispose { if (registered) runCatching { manager.unregisterListener(listener) } }
    }
    return remember(maxDegrees) {
        {
            val (x, y) = tilt.value
            (-y * maxDegrees) to (x * maxDegrees)
        }
    }
}

private const val TILT_SAMPLE_MICROS = 33_000
private const val TILT_DEADBAND = 0.02f

/** Measures real dropped-frame ratio on the Style step so Glass/Blur hints match device reality. */
@Composable
internal fun rememberFramePace(key: Any? = null, sampleMillis: Long = 1_600L): State<Float?> {
    val slowRatio = remember { mutableStateOf<Float?>(null) }
    val context = LocalContext.current
    val refreshHz = remember(context) {
        runCatching {
            @Suppress("DEPRECATION")
            val manager = context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
            @Suppress("DEPRECATION")
            manager?.defaultDisplay?.refreshRate?.takeIf { it > 1f } ?: 60f
        }.getOrDefault(60f)
    }
    LaunchedEffect(key) {
        slowRatio.value = null
        val budget = (1_000f / refreshHz) * 1.6f
        var frames = 0
        var slow = 0
        var last = 0L
        val started = withFrameNanos { it }
        while (isActive) {
            val now = withFrameNanos { it }
            if (last != 0L) {
                val deltaMs = (now - last) / 1_000_000f
                frames++
                if (deltaMs > budget) slow++
            }
            last = now
            if ((now - started) / 1_000_000L >= sampleMillis) break
        }
        slowRatio.value = if (frames > 8) slow.toFloat() / frames else null
    }
    return slowRatio
}

