package com.music.bitchord.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.player.DreamyEaseOutEasing
import com.music.bitchord.ui.utils.containSheetGestures
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val POPUP_CARD_SHAPE = RoundedCornerShape(28.dp)
private val POPUP_WIDTH = 328.dp
private val POPUP_CONTENT_MAX_HEIGHT = 420.dp

/**
 * The frosted card the output drawer's popups open into — matching Add-to-Playlist's
 * balanced glass surface by default and shifting to a rich artwork-tinted dark slab
 * whenever Reduce Dynamic Blur is turned on, with smooth spring enter/exit motion.
 */
@Composable
internal fun AudioPopupCard(
    hazeState: HazeState,
    title: String,
    subtitle: String,
    doneLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val animationSpeed by AppSettings.playerAnimationSpeed.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val slideOffsetPx = with(density) { 36.dp.toPx() }
    val enterProgress = remember { Animatable(0f) }
    var dismissing by remember { mutableStateOf(false) }
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
                    val outDuration = (195f / speed).roundToInt().coerceIn(80, 340)
                    enterProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = outDuration, easing = DreamyEaseOutEasing),
                    )
                    onDismiss()
                }
            }
        }
    }

    BackHandler(enabled = true, onBack = requestDismiss)

    val p = enterProgress.value.coerceIn(0f, 1f)
    val effectiveAccent = accentColor ?: Color(0xFF4F5D8A)
    val tintedDarkBase = remember(effectiveAccent) {
        lerpColor(Color(0xFF14151B), effectiveAccent, 0.16f)
    }
    val glassGradient = remember(effectiveAccent, tintedDarkBase) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.20f),
                effectiveAccent.copy(alpha = 0.14f),
                tintedDarkBase.copy(alpha = 0.24f),
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
    val rimBrush = remember(reduceDynamicBlur, effectiveAccent) {
        Brush.verticalGradient(
            listOf(
                if (reduceDynamicBlur) effectiveAccent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.46f),
                if (reduceDynamicBlur) Color.White.copy(alpha = 0.08f) else effectiveAccent.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.14f),
            ),
        )
    }
    val frostStyle = remember(tintedDarkBase) { menuFrost(tintedDarkBase, alpha = 0.18f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .containSheetGestures()
            .background(Color.Black.copy(alpha = 0.44f * p))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = requestDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(POPUP_WIDTH)
                .graphicsLayer {
                    alpha = p
                    if (!reduceAnimation) {
                        translationY = (1f - p) * slideOffsetPx
                        val s = lerp(0.94f, 1f, p)
                        scaleX = s
                        scaleY = s
                    }
                }
                .shadow(
                    elevation = 24.dp,
                    shape = POPUP_CARD_SHAPE,
                    ambientColor = Color.Black.copy(alpha = 0.34f),
                    spotColor = Color.Black.copy(alpha = 0.34f),
                )
                .clip(POPUP_CARD_SHAPE)
                .then(
                    if (reduceDynamicBlur) {
                        Modifier
                            .background(tintedDarkBase.copy(alpha = 0.95f), POPUP_CARD_SHAPE)
                            .background(opaqueTintedGradient, POPUP_CARD_SHAPE)
                    } else {
                        Modifier
                            .optimizedHazeEffect(state = hazeState, style = frostStyle)
                            .background(glassGradient, POPUP_CARD_SHAPE)
                    },
                )
                .border(width = if (!reduceDynamicBlur) 1.1.dp else 0.8.dp, brush = rimBrush, shape = POPUP_CARD_SHAPE)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 19.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W600,
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                    ),
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                )
            }

            Box(
                modifier = Modifier
                    .heightIn(max = POPUP_CONTENT_MAX_HEIGHT)
                    .verticalScroll(rememberScrollState()),
                content = content,
            )

            PipelineRule(Modifier)
            PipelineDoneAction(label = doneLabel, onClick = requestDismiss)
        }
    }
}
