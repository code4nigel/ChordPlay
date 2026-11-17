package com.music.bitchord.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOff
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.ChordPlayPalette
import com.music.bitchord.ui.components.isGlassSupported
import com.music.bitchord.ui.player.MeshGradientBackground
import com.music.bitchord.ui.player.MeshPalette
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// 1. Welcome, Community Links & Local Name
// ---------------------------------------------------------------------------

@Composable
internal fun WelcomeStep(
    name: String,
    palette: MeshPalette,
    onName: (String) -> Unit,
    footer: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    val tilt = rememberDeviceTilt()
    OnboardingPage(
        badge = { PageBadge(Icons.Rounded.GraphicEq) },
        title = stringResource(R.string.onboarding_name_title),
        subtitle = stringResource(R.string.onboarding_welcome_subtitle),
        footer = footer,
    ) {
        ChordPlayCommunityHero(palette = palette, tilt = tilt)
        Spacer(Modifier.height(14.dp))
        OnbSectionLabel("COMMUNITY & OPEN SOURCE")
        OnboardingLinkCard(
            iconRes = R.drawable.ic_github,
            title = "GitHub",
            subtitle = "code4nigel/ChordPlay • Releases & @code4nigel",
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/code4nigel/ChordPlay")),
                    )
                }
            },
        )
        Spacer(Modifier.height(9.dp))
        OnboardingLinkCard(
            iconRes = R.drawable.ic_reddit,
            title = "Reddit",
            subtitle = "r/ChordPlay • Community, feedback & requests",
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/r/ChordPlay/")),
                    )
                }
            },
        )
        Spacer(Modifier.height(12.dp))
        OnbField(
            value = name,
            onValueChange = onName,
            placeholder = stringResource(R.string.onboarding_name_placeholder),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OnboardingLinkCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val cardShape = remember { RoundedCornerShape(22.dp) }
    OnbCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.75.dp, Onb.Hairline, cardShape),
        shape = cardShape,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Onb.SurfaceHigh.copy(alpha = 0.85f))
                    .border(0.75.dp, Onb.Hairline, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = Onb.Title,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Onb.Title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Onb.Subtitle,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Onb.Faint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 2. Design & Visual Style (Color Palette + Black OLED + Liquid Glass / Blur)
// ---------------------------------------------------------------------------

@Composable
internal fun DesignStyleStep(
    song: Song?,
    palette: MeshPalette,
    colorPalette: ChordPlayPalette,
    onColorPalette: (ChordPlayPalette) -> Unit,
    amoledDarkMode: Boolean,
    onAmoledDarkMode: (Boolean) -> Unit,
    liquidGlass: Boolean,
    onLiquidGlass: (Boolean) -> Unit,
    reduceBlur: Boolean,
    onReduceBlur: (Boolean) -> Unit,
    reduceAnimation: Boolean,
    onReduceAnimation: (Boolean) -> Unit,
    intensity: Float,
    onIntensity: (Float) -> Unit,
    pace: Float?,
    footer: @Composable ColumnScope.() -> Unit,
) {
    val glassAvailable = isGlassSupported()

    var live by remember { mutableFloatStateOf(intensity) }
    val publish = rememberUpdatedState(onIntensity)
    val published = remember { mutableStateOf(intensity) }
    LaunchedEffect(live) {
        if (live == published.value) return@LaunchedEffect
        delay(GLASS_SETTLE_MS)
        publish.value(live)
        published.value = live
    }
    DisposableEffect(Unit) {
        onDispose {
            if (live != published.value) publish.value(live)
        }
    }

    OnboardingPage(
        badge = { PageBadge(Icons.Rounded.AutoAwesome) },
        title = stringResource(R.string.onboarding_style_title),
        subtitle = stringResource(R.string.onboarding_style_subtitle),
        footer = footer,
    ) {
        NowPlayingStylePreview(
            song = song,
            palette = palette,
            colorPalette = colorPalette,
            amoledDarkMode = amoledDarkMode,
            liquidGlass = liquidGlass && glassAvailable,
            reduceBlur = reduceBlur,
        )
        Spacer(Modifier.height(14.dp))
        OnbSectionLabel(stringResource(R.string.color_palette))
        PaletteSwatchStrip(
            selected = colorPalette,
            onSelect = onColorPalette,
        )
        Spacer(Modifier.height(12.dp))
        OnbToggle(
            icon = Icons.Rounded.DarkMode,
            title = stringResource(R.string.onboarding_black_oled),
            description = stringResource(R.string.pure_black_amoled_desc),
            checked = amoledDarkMode,
            onCheckedChange = onAmoledDarkMode,
        )
        Spacer(Modifier.height(9.dp))
        OnbToggle(
            icon = Icons.Rounded.Animation,
            title = stringResource(R.string.liquid_glass),
            description = if (glassAvailable) {
                stringResource(R.string.onboarding_glass_desc)
            } else {
                stringResource(R.string.onboarding_glass_unsupported)
            },
            checked = liquidGlass && glassAvailable,
            onCheckedChange = onLiquidGlass,
            enabled = glassAvailable,
        )
        if (liquidGlass && glassAvailable) {
            Spacer(Modifier.height(9.dp))
            OnbCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.onboarding_glass_strength),
                            color = Onb.Title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${(live * 100).toInt()}%",
                            color = Onb.Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    GlassStrengthBar(value = live, onChange = { live = it })
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        OnbToggle(
            icon = Icons.Rounded.BlurOff,
            title = stringResource(R.string.reduce_dynamic_blur),
            description = stringResource(R.string.onboarding_reduce_blur_desc),
            checked = reduceBlur,
            onCheckedChange = onReduceBlur,
        )
        Spacer(Modifier.height(9.dp))
        OnbToggle(
            icon = Icons.Rounded.MotionPhotosOff,
            title = stringResource(R.string.reduce_animation),
            description = stringResource(R.string.reduce_animation_subtitle),
            checked = reduceAnimation,
            onCheckedChange = onReduceAnimation,
        )
        if (pace != null && pace > 0.18f && liquidGlass && !reduceBlur) {
            Spacer(Modifier.height(10.dp))
            OnbCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Onb.AccentCopper,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(
                            R.string.onboarding_frame_drop_hint,
                            (pace * 100).toInt(),
                        ),
                        color = Onb.Subtitle,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteSwatchStrip(
    selected: ChordPlayPalette,
    onSelect: (ChordPlayPalette) -> Unit,
) {
    val pillShape = remember { RoundedCornerShape(18.dp) }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(ChordPlayPalette.entries, key = { it.name }) { item ->
            val isSelected = item == selected
            val primaryColor = Color(item.primaryColorHex)
            val secondaryColor = Color(item.secondaryColorHex)
            OnbCard(
                modifier = Modifier.border(
                    width = if (isSelected) 1.5.dp else 0.75.dp,
                    color = if (isSelected) Onb.Title else Onb.Hairline,
                    shape = pillShape,
                ),
                shape = pillShape,
                onClick = { onSelect(item) },
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            if (isSelected) Onb.SurfaceHigh.copy(alpha = 0.90f)
                            else Color.Transparent,
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Canvas(Modifier.size(18.dp)) {
                        drawArc(
                            color = primaryColor,
                            startAngle = -90f,
                            sweepAngle = 180f,
                            useCenter = true,
                        )
                        drawArc(
                            color = secondaryColor,
                            startAngle = 90f,
                            sweepAngle = 180f,
                            useCenter = true,
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = if (isSelected) 0.55f else 0.22f),
                            radius = size.minDimension / 2f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                        )
                    }
                    Text(
                        text = item.shortOnboardingLabel(),
                        color = if (isSelected) Onb.Title else Onb.Subtitle,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun ChordPlayPalette.shortOnboardingLabel(): String = when (this) {
    ChordPlayPalette.CHORDPLAY_CRIMSON -> "Crimson"
    ChordPlayPalette.DYNAMIC_ARTWORK -> "Dynamic Art"
    ChordPlayPalette.ELECTRIC_VIOLET -> "Violet"
    ChordPlayPalette.SUNSET_AMBER -> "Amber"
    ChordPlayPalette.EMERALD_NOIR -> "Emerald"
    ChordPlayPalette.MONOCHROME -> "Mono"
    ChordPlayPalette.DYNAMIC_MONET -> "Material You"
}

@Composable
private fun GlassStrengthBar(value: Float, onChange: (Float) -> Unit) {
    val clamped = value.coerceIn(0.1f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    onChange((change.position.x / size.width).coerceIn(0.1f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onChange((offset.x / size.width).coerceIn(0.1f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Onb.Hairline),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Onb.PillPrimary.copy(alpha = 0.9f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(24.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Onb.PillPrimary),
            )
        }
    }
}

@Composable
private fun NowPlayingStylePreview(
    song: Song?,
    palette: MeshPalette,
    colorPalette: ChordPlayPalette,
    amoledDarkMode: Boolean,
    liquidGlass: Boolean,
    reduceBlur: Boolean,
) {
    val shape = remember { RoundedCornerShape(26.dp) }
    var playing by remember { mutableStateOf(false) }

    val (accentPrimary, accentSecondary) = when (colorPalette) {
        ChordPlayPalette.DYNAMIC_MONET ->
            MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.secondary
        ChordPlayPalette.DYNAMIC_ARTWORK ->
            (palette.colors.getOrNull(0) ?: Color(colorPalette.primaryColorHex)) to
                (palette.colors.getOrNull(1) ?: Color(colorPalette.secondaryColorHex))
        else ->
            Color(colorPalette.primaryColorHex) to Color(colorPalette.secondaryColorHex)
    }
    val previewMeshPalette = remember(colorPalette, palette, accentPrimary, accentSecondary) {
        if (colorPalette == ChordPlayPalette.DYNAMIC_ARTWORK) {
            palette
        } else {
            MeshPalette(
                listOf(
                    accentPrimary,
                    accentSecondary,
                    accentPrimary.copy(alpha = 0.65f),
                    Color(0xFF14141C),
                ),
            )
        }
    }
    val playIconTint = if (accentPrimary.luminance() > 0.55f) Onb.Ink else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(248.dp)
            .onbSurface(shape = shape, elevated = true)
            .clip(shape)
            .border(1.dp, Onb.Hairline, shape),
    ) {
        when {
            amoledDarkMode -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                )
            }
            !reduceBlur -> {
                MeshGradientBackground(
                    palette = previewMeshPalette,
                    modifier = Modifier.fillMaxSize(),
                    trackKey = "${colorPalette.name}:${song?.thumbnailUrl.orEmpty()}",
                    animated = false,
                    blurRadius = 32.dp,
                )
            }
            else -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accentPrimary.copy(alpha = 0.24f),
                                    Color(0xFF13131A),
                                ),
                            ),
                        ),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = colorPalette.label.uppercase(),
                    color = Onb.Title.copy(alpha = 0.72f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.14.em,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (amoledDarkMode) {
                    PreviewBadgePill("OLED")
                }
                PreviewBadgePill(
                    when {
                        reduceBlur -> "LOW BLUR"
                        liquidGlass -> "LIQUID GLASS"
                        else -> "ADAPTIVE"
                    },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Onb.SurfaceHigh)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                ) {
                    if (song?.thumbnailUrl != null) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = song?.title ?: stringResource(R.string.onboarding_preview_loading),
                        color = Onb.Title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = OnbSubtitleTracking,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = song?.artist ?: "ChordPlay WavePlay",
                        color = Onb.Title.copy(alpha = 0.68f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Onb.Title.copy(alpha = 0.18f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.36f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accentPrimary.copy(alpha = 0.94f)),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = Onb.Title.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(26.dp))
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(accentPrimary)
                            .clickable { playing = !playing },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = playIconTint,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(26.dp))
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = null,
                        tint = Onb.Title.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewBadgePill(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Onb.Surface.copy(alpha = 0.68f))
            .border(0.5.dp, Onb.Hairline, RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            color = Onb.Title.copy(alpha = 0.88f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.08.em,
            maxLines = 1,
        )
    }
}

private const val GLASS_SETTLE_MS = 280L
