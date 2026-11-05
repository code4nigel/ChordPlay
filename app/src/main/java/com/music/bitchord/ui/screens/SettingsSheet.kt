package com.music.bitchord.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.BuildConfig
import com.music.bitchord.R
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.model.Account
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.spotify_logo
import com.music.bitchord.ui.components.isGlassSupported
import com.music.bitchord.ui.components.lightweightLiquidGlass
import com.music.bitchord.ui.player.LyricsStyleManager
import com.music.bitchord.ui.screens.settings.SettingsSection
import com.music.bitchord.ui.screens.settings.sections.AboutSection
import com.music.bitchord.ui.screens.settings.sections.AdvancedPerformanceSection
import com.music.bitchord.ui.screens.settings.sections.AppearanceSection
import com.music.bitchord.ui.screens.settings.sections.AudioSection
import com.music.bitchord.ui.screens.settings.sections.DownloadsSection
import com.music.bitchord.ui.screens.settings.sections.IntegrationsSection
import com.music.bitchord.ui.screens.settings.sections.LyricsSection
import com.music.bitchord.ui.screens.settings.sections.PlayerSection
import com.music.bitchord.ui.screens.settings.sections.ShelvesSection
import com.music.bitchord.ui.screens.settings.sections.StorageSection
import com.music.bitchord.ui.screens.settings.sections.SupportSection
import com.music.bitchord.ui.screens.settings.sections.YourDataSection
import org.jetbrains.compose.resources.painterResource

/**
 * Root Settings screen featuring a modern, aesthetic category hub
 * and focused sub-screens with fluid deceleration transitions.
 */
@Composable
fun SettingsScreen(
    windowWidth: Dp,
    signedIn: Boolean,
    account: Account?,
    channelName: String? = null,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSwitchChannel: () -> Unit = {},
    onAccountScrobbling: () -> Unit = {},
    onOpenListenBrainzLogin: () -> Unit = {},
    onOpenLastfmLogin: () -> Unit = {},
    onOpenDiscord: () -> Unit = {},
    onOpenSpotify: () -> Unit = {},
    onEqualizer: () -> Unit,
    onOpenReplay: () -> Unit,
    onLyricsSources: () -> Unit,
    onTranslationLanguage: () -> Unit,
    onSources: () -> Unit,
    onListenTogether: () -> Unit,
    onSpotifyCanvasAuth: () -> Unit,
    onAppLanguage: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var currentSection by rememberSaveable { mutableStateOf<SettingsSection?>(null) }

    BackHandler(enabled = currentSection != null) {
        currentSection = null
    }

    AnimatedContent(
        targetState = currentSection,
        transitionSpec = {
            if (targetState != null && initialState == null) {
                (slideInHorizontally(
                    animationSpec = tween(durationMillis = 260, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)),
                    initialOffsetX = { fullWidth -> (fullWidth * 0.12f).toInt() },
                ) + fadeIn(animationSpec = tween(200))) togetherWith
                (slideOutHorizontally(
                    animationSpec = tween(durationMillis = 200, easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)),
                    targetOffsetX = { fullWidth -> -(fullWidth * 0.12f).toInt() },
                ) + fadeOut(animationSpec = tween(150)))
            } else if (targetState == null && initialState != null) {
                (slideInHorizontally(
                    animationSpec = tween(durationMillis = 240, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)),
                    initialOffsetX = { fullWidth -> -(fullWidth * 0.12f).toInt() },
                ) + fadeIn(animationSpec = tween(200))) togetherWith
                (slideOutHorizontally(
                    animationSpec = tween(durationMillis = 200, easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)),
                    targetOffsetX = { fullWidth -> (fullWidth * 0.12f).toInt() },
                ) + fadeOut(animationSpec = tween(150)))
            } else {
                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(150))
            }
        },
        label = "SettingsNavigation",
        modifier = modifier.fillMaxSize(),
    ) { section ->
        when (section) {
            null -> SettingsHub(
                signedIn = signedIn,
                account = account,
                channelName = channelName,
                onSignIn = onSignIn,
                onSelectSection = { currentSection = it },
                onListenTogether = onListenTogether,
                onOpenSpotify = onOpenSpotify,
                contentPadding = contentPadding,
            )
            SettingsSection.APPEARANCE -> AppearanceSection(
                onBack = { currentSection = null },
                onAppLanguage = onAppLanguage,
                contentPadding = contentPadding,
            )
            SettingsSection.SHELVES -> ShelvesSection(
                onBack = { currentSection = null },
                contentPadding = contentPadding,
            )
            SettingsSection.PLAYER -> PlayerSection(
                windowWidth = windowWidth,
                onSpotifyCanvasAuth = onSpotifyCanvasAuth,
                onBack = { currentSection = null },
                contentPadding = contentPadding,
            )
            SettingsSection.LYRICS -> LyricsSection(
                onLyricsSources = onLyricsSources,
                onTranslationLanguage = onTranslationLanguage,
                onBack = { currentSection = null },
                contentPadding = contentPadding,
            )
            SettingsSection.AUDIO -> AudioSection(
                onSources = onSources,
                onEqualizer = onEqualizer,
                onBack = { currentSection = null },
                contentPadding = contentPadding,
            )
            SettingsSection.DOWNLOADS -> DownloadsSection(
                onBack = { currentSection = null },
                contentPadding = contentPadding,
            )
            SettingsSection.STORAGE -> StorageSection(
                onBack = { currentSection = null },
                contentPadding = contentPadding,
            )
            SettingsSection.YOUR_DATA -> YourDataSection(
                onOpenReplay = onOpenReplay,
                onBack = { currentSection = null },
                contentPadding = contentPadding,
            )
            SettingsSection.ADVANCED_PERFORMANCE -> AdvancedPerformanceSection(
                onBack = { currentSection = null },
                contentPadding = contentPadding,
            )
            SettingsSection.SUPPORT -> SupportSection(
                onBack = { currentSection = null },
                contentPadding = contentPadding,
            )
            SettingsSection.INTEGRATIONS -> IntegrationsSection(
                signedIn = signedIn,
                account = account,
                channelName = channelName,
                onSignIn = onSignIn,
                onSignOut = onSignOut,
                onSwitchChannel = onSwitchChannel,
                onOpenListenBrainzLogin = onOpenListenBrainzLogin,
                onOpenLastfmLogin = onOpenLastfmLogin,
                onOpenDiscord = onOpenDiscord,
                onOpenSpotify = onOpenSpotify,
                onListenTogether = onListenTogether,
                onBack = { currentSection = null },
                contentPadding = contentPadding,
            )
            SettingsSection.ABOUT -> AboutSection(
                onOpenReplay = onOpenReplay,
                onBack = { currentSection = null },
                contentPadding = contentPadding,
            )
        }
    }
}

@Composable
private fun SettingsHub(
    signedIn: Boolean,
    account: Account?,
    channelName: String?,
    onSignIn: () -> Unit,
    onSelectSection: (SettingsSection) -> Unit,
    onListenTogether: () -> Unit,
    onOpenSpotify: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }

    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val liquidGlassIntensity by AppSettings.liquidGlassIntensity.collectAsStateWithLifecycle()
    val spotifyToken by AppSettings.spotifySpdcToken.collectAsStateWithLifecycle()
    val party by ListenTogether.state.collectAsStateWithLifecycle()

    val filteredSections = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            // Under Preferences, Integrations is inside Checkout, so show the remaining sections
            listOf(
                SettingsSection.APPEARANCE,
                SettingsSection.SHELVES,
                SettingsSection.PLAYER,
                SettingsSection.LYRICS,
                SettingsSection.AUDIO,
                SettingsSection.DOWNLOADS,
                SettingsSection.STORAGE,
                SettingsSection.YOUR_DATA,
                SettingsSection.ADVANCED_PERFORMANCE,
                SettingsSection.SUPPORT,
                SettingsSection.ABOUT,
            )
        } else {
            val q = searchQuery.trim().lowercase()
            SettingsSection.entries.filter { section ->
                val keywords = when (section) {
                    SettingsSection.APPEARANCE -> "theme dark light oled black blur liquid glass animation refresh rate high performance display language"
                    SettingsSection.SHELVES -> "shelves home your space speed dial quick picks top picks remixes videos arrangement reorder customize"
                    SettingsSection.PLAYER -> "player artwork sleeve full bleed canvas spotify mesh gradient volume bar song status seek nerd stats"
                    SettingsSection.LYRICS -> "lyrics typography font style scale size alignment center left sync syllable source translation"
                    SettingsSection.AUDIO -> "audio quality streaming wifi cellular crossfade smart fade automix dolby atmos spatial equalizer usb dac loudness normalization silence"
                    SettingsSection.DOWNLOADS -> "downloads save custom location offline lyrics folder quality wifi"
                    SettingsSection.STORAGE -> "storage cache clear limit download offline backup restore export import local music folder"
                    SettingsSection.YOUR_DATA -> "your data replay insights stats listening history backup restore export import cache clear"
                    SettingsSection.ADVANCED_PERFORMANCE -> "advance advanced performance 120hz refresh rate display smoothness reduce blur reduce animations automix cpu nerd stats diagnostics"
                    SettingsSection.SUPPORT -> "support community reddit r/chordplay subreddit feedback github code4nigel repository developer profile releases updates"
                    SettingsSection.INTEGRATIONS -> "account sign in out google youtube music scrobble last.fm lastfm listenbrainz listen together discord rpc spotify"
                    SettingsSection.ABOUT -> "miscellaneous about chordplay version updates release notes github developer licenses bitchord upstream replay"
                }
                keywords.contains(q) || section.name.lowercase().contains(q)
            }
        }
    }

    val cardShape = RoundedCornerShape(20.dp)
    val fallbackSurface = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    val cardModifier = Modifier
        .fillMaxWidth()
        .lightweightLiquidGlass(cardShape, fallbackColor = fallbackSurface)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        // Search Bar styled matching search page
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .lightweightLiquidGlass(CircleShape, fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_search_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { searchQuery = "" },
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // CHECKOUT Section (Hub Highlights)
        if (searchQuery.isBlank()) {
            Text(
                text = stringResource(R.string.checkout).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )

            Column(modifier = cardModifier) {
                // 1. Account & Integrations
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSection(SettingsSection.INTEGRATIONS) }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (signedIn && account != null && !account.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = account.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (signedIn && account != null) {
                                    channelName ?: account.name
                                } else {
                                    stringResource(R.string.account_integrations)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (signedIn && account != null) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                        Text(
                            text = if (signedIn) {
                                stringResource(R.string.youtube_music_account)
                            } else {
                                stringResource(R.string.tap_to_sign_in_google)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp),
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 68.dp),
                )

                // 2. Play Together
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onListenTogether)
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.play_together),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            text = if (party != null) stringResource(R.string.play_together_active_desc) else stringResource(R.string.play_together_inactive_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp),
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 68.dp),
                )

                // 3. Spotify Login / Connect
                val spotifyConnected = !spotifyToken.isNullOrBlank()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenSpotify)
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1DB954).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.spotify_logo),
                            contentDescription = null,
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Spotify",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            text = if (spotifyConnected) stringResource(R.string.spotify_connected_desc) else stringResource(R.string.spotify_connect_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    if (spotifyConnected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1DB954).copy(alpha = 0.18f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1DB954),
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 68.dp),
                )

                // 4. Liquid Glass & Intensity Slider
                val glassSupported = isGlassSupported()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.liquid_glass),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                            Text(
                                text = if (glassSupported) stringResource(R.string.glass_refraction_desc) else stringResource(R.string.liquid_glass_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        Switch(
                            checked = liquidGlass,
                            onCheckedChange = AppSettings::setLiquidGlass,
                            enabled = glassSupported,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }

                    AnimatedVisibility(
                        visible = liquidGlass && glassSupported,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, start = 52.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.glass_intensity),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "${(liquidGlassIntensity * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Slider(
                                value = liquidGlassIntensity,
                                onValueChange = AppSettings::setLiquidGlassIntensity,
                                valueRange = 0.1f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }

        // PREFERENCES / RESULTS Section
        Text(
            text = if (searchQuery.isBlank()) stringResource(R.string.preferences_header) else stringResource(R.string.results_header),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        if (filteredSections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.settings_search_empty, searchQuery),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(modifier = cardModifier) {
                filteredSections.forEachIndexed { index, section ->
                    CleanCategoryRow(
                        section = section,
                        onClick = { onSelectSection(section) },
                    )
                    if (index < filteredSections.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 68.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (searchQuery.isBlank()) {
            SettingsFooter(version = BuildConfig.VERSION_NAME)
        }

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Modern, clean category row: circular icon badge + title only (no verbose description subtext).
 */
@Composable
private fun CleanCategoryRow(
    section: SettingsSection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = section.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(section.titleRes),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun SettingsFooter(version: String, modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            val linkStyles = TextLinkStyles(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.None,
                ),
            )
            append("ChordPlay v$version by shivanshu (code4nigel)\n")
            withLink(LinkAnnotation.Url("https://github.com/code4nigel/ChordPlay", linkStyles)) {
                append("ChordPlay GitHub")
            }
            append("\n\nBased on BitChord\n")
            withLink(LinkAnnotation.Url("https://github.com/kushagrasinghx/BitChord", linkStyles)) {
                append("BitChord GitHub")
            }
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    )
}
