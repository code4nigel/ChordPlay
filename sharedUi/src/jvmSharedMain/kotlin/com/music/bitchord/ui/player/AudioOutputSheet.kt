package com.music.bitchord.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AutomixPerformanceMode
import com.music.bitchord.sharedui.resources.*
import com.music.bitchord.ui.components.menuFrost
import com.music.bitchord.ui.components.optimizedHazeEffect
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.icons.BitChordIcons
import com.music.bitchord.ui.utils.containSheetGestures
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * Shared floating glass / tinted-dark modal surface used across [AudioOutputSheet]
 * and [VideoQualitySheet] so every player sheet matches Add-to-Playlist's depth and
 * artwork-tinted opaque fallback when Reduce Dynamic Blur is turned on.
 */
@Composable
internal fun playerModalSurfaceModifier(
    shape: Shape,
    hazeState: HazeState?,
    accentColor: Color?,
    reduceDynamicBlur: Boolean,
): Modifier {
    val effectiveAccent = accentColor ?: Color(0xFF4F5D8A)
    val tintedDarkBase = remember(effectiveAccent) {
        lerpColor(Color(0xFF14151B), effectiveAccent, 0.16f)
    }
    val glassGradient = remember(effectiveAccent, tintedDarkBase) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                effectiveAccent.copy(alpha = 0.14f),
                tintedDarkBase.copy(alpha = 0.26f),
                Color.White.copy(alpha = 0.06f),
            ),
        )
    }
    val opaqueTintedGradient = remember(effectiveAccent) {
        Brush.verticalGradient(
            listOf(
                effectiveAccent.copy(alpha = 0.16f),
                Color.Transparent,
            ),
        )
    }
    val rimBrush = remember(reduceDynamicBlur, hazeState, effectiveAccent) {
        val useOpaque = reduceDynamicBlur || hazeState == null
        Brush.verticalGradient(
            listOf(
                if (useOpaque) effectiveAccent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.46f),
                if (useOpaque) Color.White.copy(alpha = 0.08f) else effectiveAccent.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.14f),
            ),
        )
    }

    return if (reduceDynamicBlur || hazeState == null) {
        Modifier
            .clip(shape)
            .background(tintedDarkBase.copy(alpha = 0.95f), shape)
            .background(opaqueTintedGradient, shape)
            .border(0.8.dp, rimBrush, shape)
    } else {
        val frostStyle = remember(tintedDarkBase) { menuFrost(tintedDarkBase, alpha = 0.16f) }
        Modifier
            .clip(shape)
            .optimizedHazeEffect(state = hazeState, style = frostStyle)
            .background(glassGradient, shape)
            .border(1.1.dp, rimBrush, shape)
    }
}
private val AUDIO_SHEET_SHAPE = RoundedCornerShape(30.dp)
private val AUDIO_PILL_SHAPE = RoundedCornerShape(22.dp)
private const val SHEET_DISMISS_FRACTION = 0.25f

@Composable
internal fun AudioOutputSheet(
    hazeState: HazeState,
    accountName: String?,
    onDismiss: () -> Unit,
    onOpenCast: () -> Unit,
    accentColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val outputs = rememberAudioOutputs()
    val cast by PlayerPlatform.host.castState.collectAsStateWithLifecycle()
    val smartFadeEnabled by PlayerSettings.smartFadeEnabled.collectAsStateWithLifecycle()
    val reduceDynamicBlur by PlayerSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val reduceAnimation by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()
    val animationSpeed by PlayerSettings.playerAnimationSpeed.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val slideOffsetPx = with(density) { 44.dp.toPx() }
    val enterProgress = remember { Animatable(0f) }
    var dismissing by remember { mutableStateOf(false) }

    var dragY by remember { mutableFloatStateOf(0f) }
    var sheetHeight by remember { mutableIntStateOf(0) }
    val animatedDragY by animateFloatAsState(
        targetValue = dragY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "audioSheetDragY",
    )

    val speed = animationSpeed.coerceIn(0.5f, 2.0f)

    LaunchedEffect(Unit) {
        if (reduceAnimation) {
            enterProgress.snapTo(1f)
        } else {
            enterProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = 460f * speed * speed,
                ),
            )
        }
    }

    val requestDismiss: () -> Unit = {
        if (!dismissing) {
            dismissing = true
            if (reduceAnimation) {
                onDismiss()
            } else {
                scope.launch {
                    val outDuration = (160f / speed).roundToInt().coerceIn(70, 260)
                    enterProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = outDuration, easing = FastOutSlowInEasing),
                    )
                    onDismiss()
                }
            }
        }
    }

    PlayerBackHandler(enabled = true, onBack = requestDismiss)

    val p = enterProgress.value.coerceIn(0f, 1f)
    val dragAlpha = if (sheetHeight > 0) (1f - animatedDragY / sheetHeight).coerceIn(0f, 1f) else 1f
    val combinedAlpha = p * dragAlpha

    Box(
        modifier = modifier
            .fillMaxSize()
            .containSheetGestures()
            .background(Color.Black.copy(alpha = 0.44f * combinedAlpha))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = requestDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val surfaceModifier = playerModalSurfaceModifier(
            shape = AUDIO_SHEET_SHAPE,
            hazeState = hazeState,
            accentColor = accentColor,
            reduceDynamicBlur = reduceDynamicBlur,
        )

        Column(
            modifier = Modifier
                .widthIn(max = DRAWER_MAX_WIDTH)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .heightIn(max = 560.dp)
                .onSizeChanged { sheetHeight = it.height }
                .offset { IntOffset(0, animatedDragY.roundToInt()) }
                .graphicsLayer {
                    alpha = p
                    if (!reduceAnimation) {
                        translationY = (1f - p) * slideOffsetPx
                        val s = lerp(0.95f, 1f, p)
                        scaleX = s
                        scaleY = s
                    }
                }
                .shadow(elevation = 24.dp, shape = AUDIO_SHEET_SHAPE, ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.35f))
                .then(surfaceModifier)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                )
                .pointerInput(sheetHeight) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (sheetHeight > 0 && dragY > sheetHeight * SHEET_DISMISS_FRACTION) {
                                requestDismiss()
                            } else {
                                dragY = 0f
                            }
                        },
                        onDragCancel = { dragY = 0f },
                    ) { _, delta ->
                        dragY = (dragY + delta).coerceAtLeast(0f)
                    }
                }
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 18.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .padding(bottom = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.28f)),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 12.dp),
            ) {
                Icon(
                    imageVector = BitChordIcons.Earbuds,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = stringResource(Res.string.audio_output),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                )
            }

            // Volume slider pinned at the very top inside a rounded-rectangle pill container
            VolumeRow(routeKey = outputs, opaque = reduceDynamicBlur)

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                outputs.forEach { device ->
                    OutputRow(
                        device = device,
                        accountName = accountName,
                        opaque = reduceDynamicBlur,
                        onSelect = { PlayerPlatform.host.selectAudioOutput(device.id) },
                    )
                }
                if (cast.supported) {
                    CastRow(cast = cast, opaque = reduceDynamicBlur, onClick = onOpenCast)
                }
                AutomixRow(
                    enabled = smartFadeEnabled,
                    opaque = reduceDynamicBlur,
                    onToggle = { PlayerSettings.setSmartFadeEnabled(it) },
                )
                if (smartFadeEnabled) AutomixPreferences()
            }
        }
    }
}

@Composable
private fun AutomixPreferences() {
    val settings = PlayerSettings
    val performanceFlow = settings.automixPerformance ?: return
    val tempoFlow = settings.automixTempoMatching ?: return
    val performance by performanceFlow.collectAsStateWithLifecycle()
    val tempoMatching by tempoFlow.collectAsStateWithLifecycle()
    Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
        Text(stringResource(Res.string.automix_analysis_performance), color = Color.White,
            style = MaterialTheme.typography.titleSmall)
        Text(stringResource(Res.string.automix_analysis_description), color = Color.White.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AutomixPerformanceMode.entries.forEach { mode ->
                val label = when (mode) {
                    AutomixPerformanceMode.EFFICIENT -> Res.string.automix_efficient
                    AutomixPerformanceMode.BALANCED -> Res.string.automix_balanced
                    AutomixPerformanceMode.PERFORMANCE -> Res.string.automix_performance
                }
                FilterChip(performance == mode, { settings.setAutomixPerformance(mode) },
                    label = { Text(stringResource(label)) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        Text(stringResource(Res.string.automix_track_speed), color = Color.White,
            style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(!tempoMatching, { settings.setAutomixTempoMatching(false) },
                label = { Text(stringResource(Res.string.automix_original_speed)) }, modifier = Modifier.heightIn(min = 48.dp))
            FilterChip(tempoMatching, { settings.setAutomixTempoMatching(true) },
                label = { Text(stringResource(Res.string.automix_tempo_matching)) }, modifier = Modifier.heightIn(min = 48.dp))
        }
    }
}

/**
 * Opens the receiver picker. Lit like the playing output while the music is on
 * a receiver, with that receiver's name in place of the generic title.
 */
@Composable
private fun CastRow(cast: CastUi, opaque: Boolean, onClick: () -> Unit) {
    val haptics = rememberHaptics()
    val casting = cast.connectedName != null
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "castRowScale",
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            opaque && casting -> Color.White.copy(alpha = 0.14f)
            opaque -> Color.White.copy(alpha = 0.06f)
            casting -> Color.White.copy(alpha = 0.16f)
            else -> Color.White.copy(alpha = 0.07f)
        },
        animationSpec = tween(220),
        label = "castRowBg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(AUDIO_PILL_SHAPE)
            .background(bgColor)
            .border(
                width = 0.75.dp,
                color = Color.White.copy(alpha = if (casting) 0.22f else 0.09f),
                shape = AUDIO_PILL_SHAPE,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
            ) {
                haptics.play(Haptic.Select)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (casting) 0.18f else 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (casting) Icons.Rounded.CastConnected else Icons.Rounded.Cast,
                contentDescription = null,
                tint = Color.White.copy(alpha = if (casting) 1f else 0.78f),
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = cast.connectedName ?: stringResource(Res.string.cast),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (casting) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = Color.White.copy(alpha = if (casting) 1f else 0.90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(
                    when {
                        cast.connecting -> Res.string.cast_connecting
                        casting -> Res.string.cast_casting
                        else -> Res.string.cast_row_subtitle
                    },
                ),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.60f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.45f),
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Toggles automatic transitions directly from the player's audio output sheet.
 */
@Composable
private fun AutomixRow(
    enabled: Boolean,
    opaque: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val haptics = rememberHaptics()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "automixRowScale",
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            opaque && enabled -> Color.White.copy(alpha = 0.14f)
            opaque -> Color.White.copy(alpha = 0.06f)
            enabled -> Color.White.copy(alpha = 0.16f)
            else -> Color.White.copy(alpha = 0.07f)
        },
        animationSpec = tween(220),
        label = "automixRowBg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(AUDIO_PILL_SHAPE)
            .background(bgColor)
            .border(
                width = 0.75.dp,
                color = Color.White.copy(alpha = if (enabled) 0.22f else 0.09f),
                shape = AUDIO_PILL_SHAPE,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
            ) {
                haptics.play(Haptic.Select)
                onToggle(!enabled)
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (enabled) 0.18f else 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color.White.copy(alpha = if (enabled) 1f else 0.78f),
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.automix_output_label),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (enabled) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = Color.White.copy(alpha = if (enabled) 1f else 0.90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(if (enabled) Res.string.automix_output_on else Res.string.automix_output_off),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.60f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = {
                haptics.play(Haptic.Select)
                onToggle(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color.White.copy(alpha = 0.36f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.78f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.25f),
            ),
        )
    }
}

/**
 * One output. The playing one is lit and says so; the rest are there to be tapped.
 */
@Composable
private fun OutputRow(
    device: AudioOutputDevice,
    accountName: String?,
    opaque: Boolean,
    onSelect: () -> Unit,
) {
    val haptics = rememberHaptics()
    val active = device.isActive
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !active) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "outputRowScale",
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            opaque && active -> Color.White.copy(alpha = 0.14f)
            opaque -> Color.White.copy(alpha = 0.06f)
            active -> Color.White.copy(alpha = 0.16f)
            else -> Color.White.copy(alpha = 0.07f)
        },
        animationSpec = tween(220),
        label = "outputRowBg",
    )
    val borderColor by animateColorAsState(
        targetValue = Color.White.copy(alpha = if (active) 0.24f else 0.09f),
        animationSpec = tween(220),
        label = "outputRowBorder",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(AUDIO_PILL_SHAPE)
            .background(bgColor)
            .border(0.75.dp, borderColor, AUDIO_PILL_SHAPE)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = !active,
            ) {
                haptics.play(Haptic.Select)
                onSelect()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (active) 0.20f else 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconFor(device.kind),
                contentDescription = null,
                tint = Color.White.copy(alpha = if (active) 1f else 0.78f),
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = outputLabel(device, accountName),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = Color.White.copy(alpha = if (active) 1f else 0.90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (active) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(Res.string.audio_output_playing),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.62f),
                )
            }
        }
        if (active) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Media volume inside a rounded-rectangle pill container at the top of the sheet.
 */
@Composable
private fun VolumeRow(routeKey: Any, opaque: Boolean) {
    val system = PlayerPlatform.host.volume
    val haptics = rememberHaptics()
    val systemLevel by system.level.collectAsStateWithLifecycle()
    var level by remember(system) { mutableFloatStateOf(systemLevel) }
    var pressed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    val active = pressed || dragging
    val bgAlpha by animateFloatAsState(
        targetValue = if (active) {
            if (opaque) 0.12f else 0.16f
        } else {
            if (opaque) 0.08f else 0.11f
        },
        label = "volumePillBg",
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (active) 0.30f else 0.14f,
        label = "volumePillBorder",
    )

    LaunchedEffect(systemLevel) {
        if (!dragging) level = systemLevel
    }
    LaunchedEffect(dragging) {
        if (!dragging) {
            system.refresh()
            delay(120L)
            level = system.level.value
        }
    }

    LaunchedEffect(routeKey) {
        repeat(VOLUME_REREADS) {
            if (!dragging) system.refresh()
            delay(VOLUME_REREAD_GAP_MS)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .relativeSlider(
                label = stringResource(Res.string.player_volume),
                value = level,
                steps = 99,
                onPressChange = { isDown ->
                    if (isDown && !pressed) haptics.play(Haptic.Select)
                    pressed = isDown
                },
                onValueChange = {
                    val boundary = (it == 0f || it == 1f) && level != it
                    dragging = true
                    level = it
                    system.set(it)
                    if (boundary) haptics.play(Haptic.Tick)
                },
                onFinished = { dragging = false },
            )
            .clip(AUDIO_PILL_SHAPE)
            .background(Color.White.copy(alpha = bgAlpha))
            .border(0.75.dp, Color.White.copy(alpha = borderAlpha), AUDIO_PILL_SHAPE)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (level > 0f) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.80f),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        ThinSlider(
            value = level,
            onValueChange = null,
            idleHeight = 6.dp,
            activeHeight = 18.dp,
            active = active,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "${(level * 100f).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.72f),
        )
    }
}

private fun iconFor(kind: AudioOutputKind): ImageVector = when (kind) {
    AudioOutputKind.PHONE -> Icons.Rounded.PhoneAndroid
    AudioOutputKind.WIRED -> Icons.Rounded.Headphones
    AudioOutputKind.USB -> Icons.Rounded.Usb
    AudioOutputKind.BLUETOOTH -> Icons.Rounded.Bluetooth
    AudioOutputKind.HDMI -> Icons.Rounded.Tv
    AudioOutputKind.OTHER -> Icons.Rounded.Speaker
}

private const val VOLUME_REREADS = 4
private const val VOLUME_REREAD_GAP_MS = 250L
