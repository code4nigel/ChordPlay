package com.music.bitchord.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.ChordPlayPalette

/**
 * Extended tactile and atmospheric colors for the ChordPlay Modern design system.
 */
@Immutable
data class ChordPlayExtendedColors(
    val glow: Color,
    val surfaceGlass: Color,
    val borderHighlight: Color,
    val isAtmosphericDark: Boolean,
)

val LocalChordPlayExtendedColors = staticCompositionLocalOf {
    ChordPlayExtendedColors(
        glow = Color(0xFF00D2D3),
        surfaceGlass = Color(0xFF141622),
        borderHighlight = Color.White.copy(alpha = 0.08f),
        isAtmosphericDark = true,
    )
}

// -------------------------------------------------------------------------
// 0. CHORDPLAY CRIMSON (App Launcher Icon Signature)
// -------------------------------------------------------------------------
val ChordPlayCrimsonDarkColors = darkColorScheme(
    primary = Color(0xFFFD3F69),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF38101C),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFFF5277),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF421523),
    onSecondaryContainer = Color(0xFFFFD8E4),
    tertiary = Color(0xFFFF7597),
    onTertiary = Color.White,
    background = Color(0xFF0F080B),
    onBackground = Color(0xFFFCE0E8),
    surface = Color(0xFF160D12),
    onSurface = Color(0xFFFCE0E8),
    surfaceVariant = Color(0xFF26141D),
    onSurfaceVariant = Color(0xFFE2B6C3),
    outline = Color(0xFF452230),
    outlineVariant = Color(0xFF2D1520),
)

val ChordPlayCrimsonLightColors = lightColorScheme(
    primary = Color(0xFFD60237),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3B000C),
    secondary = Color(0xFFB81D47),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD8E4),
    onSecondaryContainer = Color(0xFF3E0013),
    tertiary = Color(0xFF9E2C52),
    onTertiary = Color.White,
    background = Color(0xFFFFF8F9),
    onBackground = Color(0xFF22191C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF22191C),
    surfaceVariant = Color(0xFFFCE0E8),
    onSurfaceVariant = Color(0xFF554148),
    outline = Color(0xFFD3BEC5),
    outlineVariant = Color(0xFFEFE0E4),
)

// -------------------------------------------------------------------------
// 1. ELECTRIC VIOLET (Atmospheric Indigo)
// -------------------------------------------------------------------------
val ElectricVioletDarkColors = darkColorScheme(
    primary = Color(0xFF7C4DFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E1C59),
    onPrimaryContainer = Color(0xFFE2D6FF),
    secondary = Color(0xFF00D2D3),
    onSecondary = Color(0xFF003638),
    secondaryContainer = Color(0xFF004F52),
    onSecondaryContainer = Color(0xFF70F5F6),
    tertiary = Color(0xFF6C5CE7),
    onTertiary = Color.White,
    background = Color(0xFF0B0C10),
    onBackground = Color(0xFFF1F1F5),
    surface = Color(0xFF141622),
    onSurface = Color(0xFFF1F1F5),
    surfaceVariant = Color(0xFF1E2235),
    onSurfaceVariant = Color(0xFFA6ACCD),
    outline = Color(0xFF2E334D),
    outlineVariant = Color(0xFF1C1F30),
)

val ElectricVioletLightColors = lightColorScheme(
    primary = Color(0xFF673AB7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7F6),
    onPrimaryContainer = Color(0xFF311B92),
    secondary = Color(0xFF0097A7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF006064),
    tertiary = Color(0xFF5E35B1),
    onTertiary = Color.White,
    background = Color(0xFFF8F9FD),
    onBackground = Color(0xFF141622),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141622),
    surfaceVariant = Color(0xFFECEEF8),
    onSurfaceVariant = Color(0xFF595D78),
    outline = Color(0xFFD0D3E5),
    outlineVariant = Color(0xFFE4E6F2),
)

// -------------------------------------------------------------------------
// 2. SUNSET AMBER
// -------------------------------------------------------------------------
val SunsetAmberDarkColors = darkColorScheme(
    primary = Color(0xFFFF9F43),
    onPrimary = Color(0xFF2E1500),
    primaryContainer = Color(0xFF4E2600),
    onPrimaryContainer = Color(0xFFFFCC80),
    secondary = Color(0xFFFF6B6B),
    onSecondary = Color(0xFF3E0A0A),
    secondaryContainer = Color(0xFF5C1414),
    onSecondaryContainer = Color(0xFFFFCDD2),
    tertiary = Color(0xFFFFA726),
    onTertiary = Color(0xFF2B1700),
    background = Color(0xFF0E0B09),
    onBackground = Color(0xFFF7ECE6),
    surface = Color(0xFF1A1410),
    onSurface = Color(0xFFF7ECE6),
    surfaceVariant = Color(0xFF2B2018),
    onSurfaceVariant = Color(0xFFD7CCC8),
    outline = Color(0xFF443428),
    outlineVariant = Color(0xFF2E221A),
)

val SunsetAmberLightColors = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF4E1A00),
    secondary = Color(0xFFD84315),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFCCBC),
    onSecondaryContainer = Color(0xFF4B1103),
    tertiary = Color(0xFFF57C00),
    onTertiary = Color.White,
    background = Color(0xFFFDFBF7),
    onBackground = Color(0xFF201A17),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF201A17),
    surfaceVariant = Color(0xFFF6ECE3),
    onSurfaceVariant = Color(0xFF6E6056),
    outline = Color(0xFFDCCFC7),
    outlineVariant = Color(0xFFEFE8E1),
)

// -------------------------------------------------------------------------
// 3. EMERALD NOIR
// -------------------------------------------------------------------------
val EmeraldNoirDarkColors = darkColorScheme(
    primary = Color(0xFF00E676),
    onPrimary = Color(0xFF003816),
    primaryContainer = Color(0xFF005322),
    onPrimaryContainer = Color(0xFF81C784),
    secondary = Color(0xFF00B0FF),
    onSecondary = Color(0xFF002942),
    secondaryContainer = Color(0xFF00446D),
    onSecondaryContainer = Color(0xFF80D8FF),
    tertiary = Color(0xFF1DE9B6),
    onTertiary = Color(0xFF003B2C),
    background = Color(0xFF08100C),
    onBackground = Color(0xFFE8F5E9),
    surface = Color(0xFF0F1D16),
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFF172B21),
    onSurfaceVariant = Color(0xFFA5D6A7),
    outline = Color(0xFF244434),
    outlineVariant = Color(0xFF162A1F),
)

val EmeraldNoirLightColors = lightColorScheme(
    primary = Color(0xFF00897B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2F1),
    onPrimaryContainer = Color(0xFF00332C),
    secondary = Color(0xFF0288D1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1F5FE),
    onSecondaryContainer = Color(0xFF013654),
    tertiary = Color(0xFF00ACC1),
    onTertiary = Color.White,
    background = Color(0xFFF6FBF8),
    onBackground = Color(0xFF142019),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF142019),
    surfaceVariant = Color(0xFFE2EFE7),
    onSurfaceVariant = Color(0xFF4D6556),
    outline = Color(0xFFC0D7CA),
    outlineVariant = Color(0xFFDEEBE3),
)

// -------------------------------------------------------------------------
// 4. MONOCHROME (Pure Obsidian & Stark White)
// -------------------------------------------------------------------------
val MonochromeDarkColors = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF242426),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFD4D4D8),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF2E2E32),
    onSecondaryContainer = Color(0xFFEDEDED),
    tertiary = Color(0xFFA1A1AA),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF09090B),
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF121214),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF1E1E22),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF3F3F46),
    outlineVariant = Color(0xFF27272A),
)

val MonochromeLightColors = lightColorScheme(
    primary = Color(0xFF09090B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4E4E7),
    onPrimaryContainer = Color(0xFF09090B),
    secondary = Color(0xFF27272A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF4F4F5),
    onSecondaryContainer = Color(0xFF18181B),
    tertiary = Color(0xFF52525B),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF09090B),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF09090B),
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFF71717A),
    outline = Color(0xFFD4D4D8),
    outlineVariant = Color(0xFFE4E4E7),
)

// -------------------------------------------------------------------------
// Modern Elevated Typography
// -------------------------------------------------------------------------
fun chordPlayTypography(family: androidx.compose.ui.text.font.FontFamily = SFProDisplay) = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.W800, fontSize = 34.sp, letterSpacing = (-0.8).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.W800, fontSize = 28.sp, letterSpacing = (-0.6).sp),
    displaySmall = TextStyle(fontWeight = FontWeight.W700, fontSize = 24.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.W800, fontSize = 30.sp, letterSpacing = (-0.7).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.W700, fontSize = 22.sp, letterSpacing = (-0.45).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.W700, fontSize = 18.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.W700, fontSize = 20.sp, letterSpacing = (-0.35).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.W600, fontSize = 16.sp, letterSpacing = (-0.2).sp),
    titleSmall = TextStyle(fontWeight = FontWeight.W600, fontSize = 14.sp, letterSpacing = (-0.15).sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.W400, fontSize = 16.sp, letterSpacing = (-0.1).sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.W400, fontSize = 14.sp, letterSpacing = (-0.1).sp),
    bodySmall = TextStyle(fontWeight = FontWeight.W400, fontSize = 12.sp, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.W600, fontSize = 13.sp, letterSpacing = (-0.1).sp),
    labelMedium = TextStyle(fontWeight = FontWeight.W600, fontSize = 12.sp, letterSpacing = 0.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.W600, fontSize = 11.sp, letterSpacing = 0.sp),
).withFamily(family)

val ChordPlayTypography = chordPlayTypography()

/**
 * ChordPlay Modern design system theme provider.
 * Renders atmospheric surfaces, luminous accents, and tactile design elements.
 */
@Composable
fun ChordPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: ChordPlayPalette = ChordPlayPalette.DYNAMIC_ARTWORK,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val amoledDarkMode by AppSettings.amoledDarkMode.collectAsStateWithLifecycle()
    val appFontChoice by AppSettings.appFontChoice.collectAsStateWithLifecycle()
    val activeArtworkUrl by AppSettings.activeTrackArtworkUrl.collectAsStateWithLifecycle()
    val customPalettes by AppSettings.customPalettes.collectAsStateWithLifecycle()
    val activeCustomPaletteId by AppSettings.activeCustomPaletteId.collectAsStateWithLifecycle()
    val activeCustomPalette = activeCustomPaletteId?.let { id ->
        customPalettes.firstOrNull { it.id == id }
    }

    val dynamicArtworkPalette = if (activeCustomPalette == null && palette == ChordPlayPalette.DYNAMIC_ARTWORK) {
        rememberArtworkPalette(imageUrl = activeArtworkUrl, dark = darkTheme)
    } else null

    val colorScheme = when {
        activeCustomPalette != null -> {
            val primary = Color(activeCustomPalette.primaryHex)
            val secondary = Color(activeCustomPalette.secondaryHex)
            val bg = Color(activeCustomPalette.backgroundHex)
            val surf = Color(activeCustomPalette.surfaceHex)

            if (darkTheme) {
                darkColorScheme(
                    primary = primary,
                    onPrimary = Color.White,
                    primaryContainer = surf,
                    onPrimaryContainer = Color.White,
                    secondary = secondary,
                    onSecondary = Color.White,
                    secondaryContainer = surf,
                    onSecondaryContainer = Color.White,
                    tertiary = secondary,
                    onTertiary = Color.White,
                    background = bg,
                    onBackground = Color(0xFFF1F1F5),
                    surface = surf,
                    onSurface = Color(0xFFF1F1F5),
                    surfaceVariant = surf,
                    onSurfaceVariant = Color(0xFFA6ACCD),
                    outline = primary.copy(alpha = 0.28f),
                    outlineVariant = primary.copy(alpha = 0.12f),
                )
            } else {
                lightColorScheme(
                    primary = primary,
                    onPrimary = Color.White,
                    primaryContainer = surf,
                    onPrimaryContainer = Color(0xFF141622),
                    secondary = secondary,
                    onSecondary = Color.White,
                    secondaryContainer = surf,
                    onSecondaryContainer = Color(0xFF141622),
                    tertiary = secondary,
                    onTertiary = Color.White,
                    background = bg,
                    onBackground = Color(0xFF141622),
                    surface = surf,
                    onSurface = Color(0xFF141622),
                    surfaceVariant = surf,
                    onSurfaceVariant = Color(0xFF595D78),
                    outline = primary.copy(alpha = 0.28f),
                    outlineVariant = primary.copy(alpha = 0.12f),
                )
            }
        }
        palette == ChordPlayPalette.DYNAMIC_ARTWORK && dynamicArtworkPalette != null -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = dynamicArtworkPalette.accent,
                    onPrimary = Color.White,
                    primaryContainer = dynamicArtworkPalette.elevated,
                    onPrimaryContainer = dynamicArtworkPalette.onBackground,
                    secondary = dynamicArtworkPalette.accent,
                    onSecondary = Color.White,
                    secondaryContainer = dynamicArtworkPalette.elevated,
                    onSecondaryContainer = dynamicArtworkPalette.onBackground,
                    tertiary = dynamicArtworkPalette.accent,
                    onTertiary = Color.White,
                    background = dynamicArtworkPalette.background,
                    onBackground = dynamicArtworkPalette.onBackground,
                    surface = dynamicArtworkPalette.wash,
                    onSurface = dynamicArtworkPalette.onBackground,
                    surfaceVariant = dynamicArtworkPalette.elevated,
                    onSurfaceVariant = dynamicArtworkPalette.onBackgroundVariant,
                    outline = dynamicArtworkPalette.divider,
                )
            } else {
                lightColorScheme(
                    primary = dynamicArtworkPalette.accent,
                    onPrimary = Color.White,
                    primaryContainer = dynamicArtworkPalette.elevated,
                    onPrimaryContainer = dynamicArtworkPalette.onBackground,
                    secondary = dynamicArtworkPalette.accent,
                    onSecondary = Color.White,
                    secondaryContainer = dynamicArtworkPalette.elevated,
                    onSecondaryContainer = dynamicArtworkPalette.onBackground,
                    tertiary = dynamicArtworkPalette.accent,
                    onTertiary = Color.White,
                    background = dynamicArtworkPalette.background,
                    onBackground = dynamicArtworkPalette.onBackground,
                    surface = dynamicArtworkPalette.wash,
                    onSurface = dynamicArtworkPalette.onBackground,
                    surfaceVariant = dynamicArtworkPalette.elevated,
                    onSurfaceVariant = dynamicArtworkPalette.onBackgroundVariant,
                    outline = dynamicArtworkPalette.divider,
                )
            }
        }
        else -> when (palette) {
            ChordPlayPalette.CHORDPLAY_CRIMSON, ChordPlayPalette.DYNAMIC_ARTWORK ->
                if (darkTheme) ChordPlayCrimsonDarkColors else ChordPlayCrimsonLightColors
            ChordPlayPalette.ELECTRIC_VIOLET -> if (darkTheme) ElectricVioletDarkColors else ElectricVioletLightColors
            ChordPlayPalette.SUNSET_AMBER -> if (darkTheme) SunsetAmberDarkColors else SunsetAmberLightColors
            ChordPlayPalette.EMERALD_NOIR -> if (darkTheme) EmeraldNoirDarkColors else EmeraldNoirLightColors
            ChordPlayPalette.MONOCHROME -> if (darkTheme) MonochromeDarkColors else MonochromeLightColors
            ChordPlayPalette.DYNAMIC_MONET -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (darkTheme) {
                        val dynamic = dynamicDarkColorScheme(context)
                        dynamic.copy(
                            background = dynamic.background.copy(alpha = 1f),
                            surface = dynamic.surface,
                            surfaceVariant = dynamic.surfaceVariant,
                            outline = dynamic.outline.copy(alpha = 0.35f),
                        )
                    } else {
                        val dynamic = dynamicLightColorScheme(context)
                        dynamic.copy(
                            background = dynamic.background.copy(alpha = 1f),
                            surface = dynamic.surface,
                            surfaceVariant = dynamic.surfaceVariant,
                            outline = dynamic.outline.copy(alpha = 0.35f),
                        )
                    }
                } else {
                    if (darkTheme) ChordPlayCrimsonDarkColors else ChordPlayCrimsonLightColors
                }
            }
        }
    }

    val extendedColors = when {
        activeCustomPalette != null -> {
            val primary = Color(activeCustomPalette.primaryHex)
            val secondary = Color(activeCustomPalette.secondaryHex)
            val surf = Color(activeCustomPalette.surfaceHex)
            ChordPlayExtendedColors(
                glow = secondary,
                surfaceGlass = surf.copy(alpha = 0.85f),
                borderHighlight = primary.copy(alpha = 0.25f),
                isAtmosphericDark = darkTheme,
            )
        }
        palette == ChordPlayPalette.DYNAMIC_ARTWORK && dynamicArtworkPalette != null -> {
            ChordPlayExtendedColors(
                glow = dynamicArtworkPalette.accent,
                surfaceGlass = dynamicArtworkPalette.wash.copy(alpha = 0.85f),
                borderHighlight = dynamicArtworkPalette.accent.copy(alpha = 0.25f),
                isAtmosphericDark = darkTheme,
            )
        }
        else -> when (palette) {
            ChordPlayPalette.CHORDPLAY_CRIMSON, ChordPlayPalette.DYNAMIC_ARTWORK -> ChordPlayExtendedColors(
                glow = Color(0xFFFF2A6D),
                surfaceGlass = if (darkTheme) Color(0xFF1E1017) else Color(0xFFFAF0F3),
                borderHighlight = Color(0xFFFD3F69).copy(alpha = 0.22f),
                isAtmosphericDark = darkTheme,
            )
            ChordPlayPalette.ELECTRIC_VIOLET -> ChordPlayExtendedColors(
                glow = Color(0xFF00D2D3),
                surfaceGlass = if (darkTheme) Color(0xFF141622) else Color(0xFFF0F1FA),
                borderHighlight = if (darkTheme) Color(0xFF7C4DFF).copy(alpha = 0.20f) else Color(0xFF7C4DFF).copy(alpha = 0.12f),
                isAtmosphericDark = darkTheme,
            )
            ChordPlayPalette.SUNSET_AMBER -> ChordPlayExtendedColors(
                glow = Color(0xFFFF6B6B),
                surfaceGlass = if (darkTheme) Color(0xFF1A1410) else Color(0xFFFAF2EB),
                borderHighlight = if (darkTheme) Color(0xFFFF9F43).copy(alpha = 0.20f) else Color(0xFFFF9F43).copy(alpha = 0.12f),
                isAtmosphericDark = darkTheme,
            )
            ChordPlayPalette.EMERALD_NOIR -> ChordPlayExtendedColors(
                glow = Color(0xFF00B0FF),
                surfaceGlass = if (darkTheme) Color(0xFF0F1D16) else Color(0xFFEBF6F0),
                borderHighlight = if (darkTheme) Color(0xFF00E676).copy(alpha = 0.20f) else Color(0xFF00E676).copy(alpha = 0.12f),
                isAtmosphericDark = darkTheme,
            )
            ChordPlayPalette.MONOCHROME -> ChordPlayExtendedColors(
                glow = if (darkTheme) Color(0xFFFFFFFF).copy(alpha = 0.45f) else Color(0xFF000000).copy(alpha = 0.25f),
                surfaceGlass = if (darkTheme) Color(0xFF141416).copy(alpha = 0.88f) else Color(0xFFFAFAFA).copy(alpha = 0.90f),
                borderHighlight = if (darkTheme) Color(0xFFFFFFFF).copy(alpha = 0.18f) else Color(0xFF000000).copy(alpha = 0.12f),
                isAtmosphericDark = darkTheme,
            )
            ChordPlayPalette.DYNAMIC_MONET -> ChordPlayExtendedColors(
                glow = colorScheme.secondary,
                surfaceGlass = colorScheme.surfaceVariant.copy(alpha = 0.75f),
                borderHighlight = colorScheme.primary.copy(alpha = 0.25f),
                isAtmosphericDark = darkTheme,
            )
        }
    }

    val effectiveColorScheme = if (darkTheme && amoledDarkMode) {
        colorScheme.copy(
            background = Color.Black,
            surface = Color(0xFF050507),
            surfaceVariant = Color(0xFF0E0E12),
            outline = colorScheme.primary.copy(alpha = 0.28f),
        )
    } else {
        colorScheme
    }

    val effectiveExtendedColors = if (darkTheme && amoledDarkMode) {
        extendedColors.copy(
            surfaceGlass = Color(0xFF08080C).copy(alpha = 0.88f),
            borderHighlight = colorScheme.primary.copy(alpha = 0.24f),
        )
    } else {
        extendedColors
    }

    val typography = remember(appFontChoice) {
        chordPlayTypography(fontFamilyFor(appFontChoice))
    }

    CompositionLocalProvider(
        LocalChordPlayExtendedColors provides effectiveExtendedColors,
    ) {
        MaterialTheme(
            colorScheme = effectiveColorScheme,
            typography = typography,
            content = content,
        )
    }
}
