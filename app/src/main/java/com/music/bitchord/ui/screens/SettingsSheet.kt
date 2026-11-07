package com.music.bitchord.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BlurOff
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.SwipeRight
import androidx.compose.material.icons.rounded.SwitchAccount
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    currentSection: SettingsSection? = null,
    onSectionChange: ((SettingsSection?) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var internalSection by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    var pendingSectionSearchQuery by rememberSaveable { mutableStateOf<String?>(null) }
    val effectiveSection = if (onSectionChange != null) currentSection else internalSection
    val updateSection: (SettingsSection?) -> Unit = { section ->
        if (section == null) {
            pendingSectionSearchQuery = null
        }
        if (onSectionChange != null) {
            onSectionChange(section)
        } else {
            internalSection = section
        }
    }

    BackHandler(enabled = effectiveSection != null) {
        updateSection(null)
    }

    AnimatedContent(
        targetState = effectiveSection,
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
        val query = pendingSectionSearchQuery ?: ""
        when (section) {
            null -> SettingsHub(
                signedIn = signedIn,
                account = account,
                channelName = channelName,
                onSignIn = onSignIn,
                onSelectSection = { targetSection, targetQuery ->
                    pendingSectionSearchQuery = targetQuery
                    updateSection(targetSection)
                },
                onListenTogether = onListenTogether,
                onOpenSpotify = onOpenSpotify,
                contentPadding = contentPadding,
            )
            SettingsSection.APPEARANCE -> AppearanceSection(
                onBack = { updateSection(null) },
                onAppLanguage = onAppLanguage,
                contentPadding = contentPadding,
                initialSearchQuery = query,
            )
            SettingsSection.SHELVES -> ShelvesSection(
                onBack = { updateSection(null) },
                contentPadding = contentPadding,
                initialSearchQuery = query,
            )
            SettingsSection.PLAYER -> PlayerSection(
                windowWidth = windowWidth,
                onSpotifyCanvasAuth = onSpotifyCanvasAuth,
                onBack = { updateSection(null) },
                contentPadding = contentPadding,
                initialSearchQuery = query,
            )
            SettingsSection.LYRICS -> LyricsSection(
                onLyricsSources = onLyricsSources,
                onTranslationLanguage = onTranslationLanguage,
                onBack = { updateSection(null) },
                contentPadding = contentPadding,
                initialSearchQuery = query,
            )
            SettingsSection.AUDIO -> AudioSection(
                onSources = onSources,
                onEqualizer = onEqualizer,
                onBack = { updateSection(null) },
                contentPadding = contentPadding,
                initialSearchQuery = query,
            )
            SettingsSection.DOWNLOADS -> DownloadsSection(
                onBack = { updateSection(null) },
                contentPadding = contentPadding,
                initialSearchQuery = query,
            )
            SettingsSection.STORAGE -> StorageSection(
                onBack = { updateSection(null) },
                contentPadding = contentPadding,
                initialSearchQuery = query,
            )
            SettingsSection.YOUR_DATA -> YourDataSection(
                onOpenReplay = onOpenReplay,
                onBack = { updateSection(null) },
                contentPadding = contentPadding,
                initialSearchQuery = query,
            )
            SettingsSection.ADVANCED_PERFORMANCE -> AdvancedPerformanceSection(
                onBack = { updateSection(null) },
                contentPadding = contentPadding,
                initialSearchQuery = query,
            )
            SettingsSection.SUPPORT -> SupportSection(
                onBack = { updateSection(null) },
                contentPadding = contentPadding,
                initialSearchQuery = query,
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
                onBack = { updateSection(null) },
                contentPadding = contentPadding,
                initialSearchQuery = query,
            )
            SettingsSection.ABOUT -> AboutSection(
                onOpenReplay = onOpenReplay,
                onBack = { updateSection(null) },
                contentPadding = contentPadding,
                initialSearchQuery = query,
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
    onSelectSection: (SettingsSection, String?) -> Unit,
    onListenTogether: () -> Unit,
    onOpenSpotify: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }

    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val liquidGlassIntensity by AppSettings.liquidGlassIntensity.collectAsStateWithLifecycle()
    val animatedCanvas by AppSettings.animatedCanvas.collectAsStateWithLifecycle()
    val spotifyToken by AppSettings.spotifySpdcToken.collectAsStateWithLifecycle()
    val party by ListenTogether.state.collectAsStateWithLifecycle()

    val matchingSections = remember(searchQuery) {
        if (searchQuery.isBlank()) {
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
                    SettingsSection.APPEARANCE -> "appearance theme dark light oled black blur liquid glass animation refresh rate high performance display language swipe"
                    SettingsSection.SHELVES -> "shelves home your space speed dial quick picks top picks remixes videos arrangement reorder customize"
                    SettingsSection.PLAYER -> "playback player artwork sleeve full bleed canvas spotify mesh gradient volume bar song status seek nerd stats"
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

    val allSettings = rememberGlobalSettings()
    val matchingSettings = remember(searchQuery, allSettings) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allSettings.filter { setting ->
                matchesSearch(searchQuery, setting.title, setting.subtitle, *setting.keywords.toTypedArray())
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
                        .clickable { onSelectSection(SettingsSection.INTEGRATIONS, null) }
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

                // Quick Animated Artwork Toggle when Spotify is connected
                AnimatedVisibility(
                    visible = spotifyConnected,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 68.dp),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { AppSettings.setAnimatedCanvas(!animatedCanvas) }
                                .padding(start = 68.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.animated_cover_art),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = stringResource(R.string.animated_cover_art_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = animatedCanvas,
                                onCheckedChange = AppSettings::setAnimatedCanvas,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
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
                                imageVector = Icons.Rounded.WaterDrop,
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

        if (searchQuery.isBlank()) {
            // PREFERENCES Section
            Text(
                text = stringResource(R.string.preferences_header),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )

            Column(modifier = cardModifier) {
                matchingSections.forEachIndexed { index, section ->
                    CleanCategoryRow(
                        section = section,
                        onClick = { onSelectSection(section, null) },
                    )
                    if (index < matchingSections.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 68.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SettingsFooter(version = BuildConfig.VERSION_NAME)
            Spacer(Modifier.height(16.dp))
        } else {
            // Global Search Results: Categories & Individual Settings
            if (matchingSections.isEmpty() && matchingSettings.isEmpty()) {
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
                if (matchingSections.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.categories_header),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )

                    Column(modifier = cardModifier) {
                        matchingSections.forEachIndexed { index, section ->
                            CleanCategoryRow(
                                section = section,
                                onClick = { onSelectSection(section, searchQuery) },
                            )
                            if (index < matchingSections.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 68.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                }

                if (matchingSettings.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_header),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )

                    Column(modifier = cardModifier) {
                        matchingSettings.forEachIndexed { index, item ->
                            CleanSettingItemRow(
                                item = item,
                                onClick = { onSelectSection(item.section, item.queryTarget) },
                            )
                            if (index < matchingSettings.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 68.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

private data class GlobalSettingItem(
    val title: String,
    val subtitle: String?,
    val section: SettingsSection,
    val icon: ImageVector,
    val queryTarget: String,
    val keywords: List<String> = emptyList(),
)

@Composable
private fun CleanSettingItemRow(
    item: GlobalSettingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sectionName = stringResource(item.section.titleRes)
            val detail = if (!item.subtitle.isNullOrBlank()) {
                "$sectionName • ${item.subtitle}"
            } else {
                sectionName
            }
            Text(
                text = detail,
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
}

@Composable
private fun rememberGlobalSettings(): List<GlobalSettingItem> {
    return listOf(
        // Appearance
        GlobalSettingItem(
            title = stringResource(R.string.theme),
            subtitle = stringResource(R.string.pure_black_amoled_desc),
            section = SettingsSection.APPEARANCE,
            icon = Icons.Rounded.Brightness4,
            queryTarget = stringResource(R.string.theme),
            keywords = listOf("dark", "light", "oled", "pure black", "amoled", "system"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.color_palette),
            subtitle = stringResource(R.string.color_palette_subtitle),
            section = SettingsSection.APPEARANCE,
            icon = Icons.Rounded.Palette,
            queryTarget = stringResource(R.string.color_palette),
            keywords = listOf("amber", "ruby", "amethyst", "jade", "sapphire", "emerald", "custom palette", "accent"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.system_typography),
            subtitle = stringResource(R.string.appearance),
            section = SettingsSection.APPEARANCE,
            icon = Icons.Rounded.FontDownload,
            queryTarget = stringResource(R.string.system_typography),
            keywords = listOf("font", "slab", "modern", "serif", "sans"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.display_density),
            subtitle = stringResource(R.string.display_density_subtitle),
            section = SettingsSection.APPEARANCE,
            icon = Icons.Rounded.GridView,
            queryTarget = stringResource(R.string.display_density),
            keywords = listOf("scale", "density", "compact", "grid"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.liquid_glass),
            subtitle = stringResource(R.string.liquid_glass_subtitle),
            section = SettingsSection.APPEARANCE,
            icon = Icons.Rounded.WaterDrop,
            queryTarget = stringResource(R.string.liquid_glass),
            keywords = listOf("liquid", "glass", "intensity", "refraction", "water", "blur"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.reduce_dynamic_blur),
            subtitle = stringResource(R.string.reduce_dynamic_blur_subtitle),
            section = SettingsSection.APPEARANCE,
            icon = Icons.Rounded.BlurOff,
            queryTarget = stringResource(R.string.reduce_dynamic_blur),
            keywords = listOf("blur", "dynamic", "performance"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.reduce_animation),
            subtitle = stringResource(R.string.reduce_animation_subtitle),
            section = SettingsSection.APPEARANCE,
            icon = Icons.Rounded.MotionPhotosOff,
            queryTarget = stringResource(R.string.reduce_animation),
            keywords = listOf("animation", "motion", "reduce"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.language),
            subtitle = stringResource(R.string.app_language),
            section = SettingsSection.APPEARANCE,
            icon = Icons.Rounded.Language,
            queryTarget = stringResource(R.string.language),
            keywords = listOf("translate", "locale", "english"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.swipe_gestures),
            subtitle = stringResource(R.string.right_swipe_action),
            section = SettingsSection.APPEARANCE,
            icon = Icons.Rounded.SwipeRight,
            queryTarget = stringResource(R.string.swipe_gestures),
            keywords = listOf("swipe", "gesture", "right", "left"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.high_performance_mode),
            subtitle = stringResource(R.string.high_performance_subtitle),
            section = SettingsSection.ADVANCED_PERFORMANCE,
            icon = Icons.Rounded.Speed,
            queryTarget = stringResource(R.string.high_performance_mode),
            keywords = listOf("120hz", "90hz", "144hz", "refresh rate", "hz", "fps", "smooth"),
        ),

        // Shelves
        GlobalSettingItem(
            title = stringResource(R.string.home_shelves),
            subtitle = stringResource(R.string.arrange_home_shelves),
            section = SettingsSection.SHELVES,
            icon = Icons.Rounded.ViewAgenda,
            queryTarget = stringResource(R.string.home_shelves),
            keywords = listOf("shelves", "order", "arrange", "reorder", "home", "quick picks", "forgotten favorites", "speed dial"),
        ),

        // Playback (Player)
        GlobalSettingItem(
            title = stringResource(R.string.player_layout),
            subtitle = stringResource(R.string.playback),
            section = SettingsSection.PLAYER,
            icon = Icons.Rounded.GraphicEq,
            queryTarget = stringResource(R.string.player_layout),
            keywords = listOf("modern ambient", "vinyl turntable", "classic", "layout"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.breathing_artwork_aura),
            subtitle = stringResource(R.string.breathing_artwork_aura_desc),
            section = SettingsSection.PLAYER,
            icon = Icons.Rounded.Animation,
            queryTarget = stringResource(R.string.breathing_artwork_aura),
            keywords = listOf("aura", "breathing", "glow", "ambient"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.full_screen_cover_art),
            subtitle = stringResource(R.string.full_screen_cover_art_subtitle),
            section = SettingsSection.PLAYER,
            icon = Icons.Rounded.Fullscreen,
            queryTarget = stringResource(R.string.full_screen_cover_art),
            keywords = listOf("full bleed", "full screen", "cover", "edges"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.legacy_mesh_gradient),
            subtitle = stringResource(R.string.legacy_mesh_gradient_subtitle),
            section = SettingsSection.PLAYER,
            icon = Icons.Rounded.Gradient,
            queryTarget = stringResource(R.string.legacy_mesh_gradient),
            keywords = listOf("mesh", "gradient", "blobs", "v1.5"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.animated_cover_art),
            subtitle = stringResource(R.string.animated_cover_art_subtitle),
            section = SettingsSection.PLAYER,
            icon = Icons.Rounded.Animation,
            queryTarget = stringResource(R.string.animated_cover_art),
            keywords = listOf("canvas", "spotify", "video", "loop"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.animated_cover_cellular),
            subtitle = stringResource(R.string.playback),
            section = SettingsSection.PLAYER,
            icon = Icons.Rounded.SignalCellularAlt,
            queryTarget = stringResource(R.string.animated_cover_cellular),
            keywords = listOf("cellular", "mobile data", "canvas"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.integrate_spotify_canvas),
            subtitle = stringResource(R.string.playback),
            section = SettingsSection.PLAYER,
            icon = Icons.Rounded.Link,
            queryTarget = stringResource(R.string.integrate_spotify_canvas),
            keywords = listOf("spotify", "canvas", "auth", "token", "spdc"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.hide_song_status),
            subtitle = stringResource(R.string.hide_song_status_subtitle),
            section = SettingsSection.PLAYER,
            icon = Icons.Rounded.VisibilityOff,
            queryTarget = stringResource(R.string.hide_song_status),
            keywords = listOf("status", "playing from", "played by"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.play_next_on_swipe),
            subtitle = stringResource(R.string.swipe_plays_next),
            section = SettingsSection.PLAYER,
            icon = Icons.Rounded.PlaylistPlay,
            queryTarget = stringResource(R.string.play_next_on_swipe),
            keywords = listOf("swipe", "queue", "next"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.stop_music_on_close),
            subtitle = stringResource(R.string.stop_music_on_close_subtitle),
            section = SettingsSection.PLAYER,
            icon = Icons.Rounded.MusicOff,
            queryTarget = stringResource(R.string.stop_music_on_close),
            keywords = listOf("recents", "task", "stop", "close"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.smart_version_alignment),
            subtitle = stringResource(R.string.smart_version_alignment_subtitle),
            section = SettingsSection.PLAYER,
            icon = Icons.Rounded.GraphicEq,
            queryTarget = stringResource(R.string.smart_version_alignment),
            keywords = listOf("smart", "audio alignment", "waveform"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.show_nerd_stats),
            subtitle = stringResource(R.string.show_nerd_stats_subtitle),
            section = SettingsSection.PLAYER,
            icon = Icons.Rounded.BarChart,
            queryTarget = stringResource(R.string.show_nerd_stats),
            keywords = listOf("nerd", "stats", "bitrate", "codec"),
        ),

        // Lyrics
        GlobalSettingItem(
            title = stringResource(R.string.font_family),
            subtitle = stringResource(R.string.font_family_subtitle),
            section = SettingsSection.LYRICS,
            icon = Icons.Rounded.FontDownload,
            queryTarget = stringResource(R.string.font_family),
            keywords = listOf("font", "serif", "sans", "mono", "custom font", "ttf"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.size_scale),
            subtitle = stringResource(R.string.size_and_alignment),
            section = SettingsSection.LYRICS,
            icon = Icons.Rounded.FormatSize,
            queryTarget = stringResource(R.string.size_scale),
            keywords = listOf("scale", "size", "font size", "lyrics size"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.font_weight),
            subtitle = stringResource(R.string.size_and_alignment),
            section = SettingsSection.LYRICS,
            icon = Icons.Rounded.FormatBold,
            queryTarget = stringResource(R.string.font_weight),
            keywords = listOf("weight", "bold", "medium", "black", "regular"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.text_alignment),
            subtitle = stringResource(R.string.size_and_alignment),
            section = SettingsSection.LYRICS,
            icon = Icons.Rounded.FormatAlignCenter,
            queryTarget = stringResource(R.string.text_alignment),
            keywords = listOf("alignment", "align", "left", "center"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.synced_lyrics),
            subtitle = stringResource(R.string.synced_lyrics_subtitle),
            section = SettingsSection.LYRICS,
            icon = Icons.AutoMirrored.Rounded.Notes,
            queryTarget = stringResource(R.string.synced_lyrics),
            keywords = listOf("sync", "karaoke", "timing"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.blur_unfocused_lyrics),
            subtitle = stringResource(R.string.blur_unfocused_lyrics_subtitle),
            section = SettingsSection.LYRICS,
            icon = Icons.Rounded.BlurOn,
            queryTarget = stringResource(R.string.blur_unfocused_lyrics),
            keywords = listOf("blur", "focus"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.lyrics_sources),
            subtitle = stringResource(R.string.synced_lyrics),
            section = SettingsSection.LYRICS,
            icon = Icons.Rounded.Language,
            queryTarget = stringResource(R.string.lyrics_sources),
            keywords = listOf("lrclib", "kugou", "genius", "source", "provider"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.translation_language),
            subtitle = stringResource(R.string.translation_language_subtitle),
            section = SettingsSection.LYRICS,
            icon = Icons.Rounded.Translate,
            queryTarget = stringResource(R.string.translation_language),
            keywords = listOf("translate", "language"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.fullscreen_artwork_lyrics),
            subtitle = stringResource(R.string.synced_lyrics),
            section = SettingsSection.LYRICS,
            icon = Icons.Rounded.Fullscreen,
            queryTarget = stringResource(R.string.fullscreen_artwork_lyrics),
            keywords = listOf("fullscreen", "artwork lyrics", "canvas lyrics"),
        ),

        // Audio
        GlobalSettingItem(
            title = stringResource(R.string.on_wifi),
            subtitle = stringResource(R.string.audio_quality),
            section = SettingsSection.AUDIO,
            icon = Icons.Rounded.Wifi,
            queryTarget = stringResource(R.string.on_wifi),
            keywords = listOf("wifi", "quality", "bitrate", "lossless"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.on_mobile_data),
            subtitle = stringResource(R.string.audio_quality),
            section = SettingsSection.AUDIO,
            icon = Icons.Rounded.SignalCellularAlt,
            queryTarget = stringResource(R.string.on_mobile_data),
            keywords = listOf("cellular", "mobile data", "quality", "bitrate"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.dolby_atmos),
            subtitle = stringResource(R.string.dolby_atmos_subtitle),
            section = SettingsSection.AUDIO,
            icon = Icons.Rounded.SurroundSound,
            queryTarget = stringResource(R.string.dolby_atmos),
            keywords = listOf("dolby", "atmos", "spatial", "surround"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.spatial_audio),
            subtitle = stringResource(R.string.spatial_audio_subtitle),
            section = SettingsSection.AUDIO,
            icon = Icons.Rounded.SurroundSound,
            queryTarget = stringResource(R.string.spatial_audio),
            keywords = listOf("spatial", "surround", "3d"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.prefer_music_only),
            subtitle = stringResource(R.string.prefer_music_only_subtitle),
            section = SettingsSection.AUDIO,
            icon = Icons.Rounded.MusicNote,
            queryTarget = stringResource(R.string.prefer_music_only),
            keywords = listOf("music only", "catalogue", "video"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.crossfade),
            subtitle = stringResource(R.string.crossfade_subtitle),
            section = SettingsSection.AUDIO,
            icon = Icons.Rounded.GraphicEq,
            queryTarget = stringResource(R.string.crossfade),
            keywords = listOf("crossfade", "fade", "transition", "overlap"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.automix),
            subtitle = stringResource(R.string.automix_enabled_subtitle),
            section = SettingsSection.AUDIO,
            icon = Icons.Rounded.GraphicEq,
            queryTarget = stringResource(R.string.automix),
            keywords = listOf("automix", "smart fade", "seamless"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.skip_silence),
            subtitle = stringResource(R.string.skip_silence_subtitle),
            section = SettingsSection.AUDIO,
            icon = Icons.Rounded.GraphicEq,
            queryTarget = stringResource(R.string.skip_silence),
            keywords = listOf("skip silence", "gapless"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.equalizer),
            subtitle = stringResource(R.string.equalizer_subtitle),
            section = SettingsSection.AUDIO,
            icon = Icons.Rounded.Tune,
            queryTarget = stringResource(R.string.equalizer),
            keywords = listOf("equalizer", "eq", "bass", "treble"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.source),
            subtitle = stringResource(R.string.sources_subtitle),
            section = SettingsSection.AUDIO,
            icon = Icons.Rounded.Extension,
            queryTarget = stringResource(R.string.source),
            keywords = listOf("source", "piped", "youtube music", "invidious"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.output_precision),
            subtitle = stringResource(R.string.audio_quality),
            section = SettingsSection.AUDIO,
            icon = Icons.Rounded.GraphicEq,
            queryTarget = stringResource(R.string.output_precision),
            keywords = listOf("output pcm", "usb dac", "24-bit", "hi-res"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.loudness_normalization),
            subtitle = stringResource(R.string.loudness_normalization_subtitle),
            section = SettingsSection.AUDIO,
            icon = Icons.Rounded.VolumeUp,
            queryTarget = stringResource(R.string.loudness_normalization),
            keywords = listOf("loudness", "volume", "normalization", "replaygain"),
        ),

        // Downloads
        GlobalSettingItem(
            title = stringResource(R.string.download_quality),
            subtitle = stringResource(R.string.downloads),
            section = SettingsSection.DOWNLOADS,
            icon = Icons.Rounded.Download,
            queryTarget = stringResource(R.string.download_quality),
            keywords = listOf("download", "quality", "bitrate", "lossless"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.download_wifi_only),
            subtitle = stringResource(R.string.downloads),
            section = SettingsSection.DOWNLOADS,
            icon = Icons.Rounded.Wifi,
            queryTarget = stringResource(R.string.download_wifi_only),
            keywords = listOf("download wifi", "cellular", "data"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.save_to_custom_download_location),
            subtitle = stringResource(R.string.save_to_custom_download_location_subtitle),
            section = SettingsSection.DOWNLOADS,
            icon = Icons.Rounded.Folder,
            queryTarget = stringResource(R.string.save_to_custom_download_location),
            keywords = listOf("custom location", "folder", "sd card", "download path"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.download_lyrics_offline),
            subtitle = stringResource(R.string.download_lyrics_offline_subtitle),
            section = SettingsSection.DOWNLOADS,
            icon = Icons.AutoMirrored.Rounded.Notes,
            queryTarget = stringResource(R.string.download_lyrics_offline),
            keywords = listOf("offline lyrics", "lyrics download", "synced"),
        ),

        // Storage
        GlobalSettingItem(
            title = stringResource(R.string.show_cache_folder),
            subtitle = stringResource(R.string.show_cache_folder_subtitle),
            section = SettingsSection.STORAGE,
            icon = Icons.Rounded.Cached,
            queryTarget = stringResource(R.string.show_cache_folder),
            keywords = listOf("cache", "folder", "storage"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.song_cache_limit),
            subtitle = stringResource(R.string.song_cache_limit_subtitle),
            section = SettingsSection.STORAGE,
            icon = Icons.Rounded.Storage,
            queryTarget = stringResource(R.string.song_cache_limit),
            keywords = listOf("cache limit", "megabytes", "unlimited", "gb"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.clear_song_cache),
            subtitle = stringResource(R.string.clear_song_cache_subtitle),
            section = SettingsSection.STORAGE,
            icon = Icons.Rounded.DeleteSweep,
            queryTarget = stringResource(R.string.clear_song_cache),
            keywords = listOf("clear cache", "delete songs"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.clear_image_cache),
            subtitle = stringResource(R.string.clear_image_cache_subtitle),
            section = SettingsSection.STORAGE,
            icon = Icons.Rounded.DeleteSweep,
            queryTarget = stringResource(R.string.clear_image_cache),
            keywords = listOf("clear images", "artwork cache"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.local_music_folder),
            subtitle = stringResource(R.string.local_music),
            section = SettingsSection.STORAGE,
            icon = Icons.Rounded.Folder,
            queryTarget = stringResource(R.string.local_music_folder),
            keywords = listOf("local", "folder", "device audio"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.filter_non_music_audio),
            subtitle = stringResource(R.string.filter_non_music_audio_subtitle),
            section = SettingsSection.STORAGE,
            icon = Icons.Rounded.FilterAlt,
            queryTarget = stringResource(R.string.filter_non_music_audio),
            keywords = listOf("filter", "ringtones", "voice notes"),
        ),

        // Your Data
        GlobalSettingItem(
            title = stringResource(R.string.replay),
            subtitle = stringResource(R.string.replay_subtitle),
            section = SettingsSection.YOUR_DATA,
            icon = Icons.Rounded.BarChart,
            queryTarget = stringResource(R.string.replay),
            keywords = listOf("replay", "stats", "insights", "analytics", "year"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.work_out_genres),
            subtitle = stringResource(R.string.your_data),
            section = SettingsSection.YOUR_DATA,
            icon = Icons.Rounded.LocalOffer,
            queryTarget = stringResource(R.string.work_out_genres),
            keywords = listOf("genres", "replay"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.dont_repeat_songs),
            subtitle = stringResource(R.string.dont_repeat_songs_subtitle),
            section = SettingsSection.YOUR_DATA,
            icon = Icons.Rounded.History,
            queryTarget = stringResource(R.string.dont_repeat_songs),
            keywords = listOf("repeat", "suggestions", "history"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.export_data),
            subtitle = stringResource(R.string.export_data_subtitle),
            section = SettingsSection.YOUR_DATA,
            icon = Icons.Rounded.FileUpload,
            queryTarget = stringResource(R.string.export_data),
            keywords = listOf("export", "backup", "json", "save"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.import_data),
            subtitle = stringResource(R.string.import_data_subtitle),
            section = SettingsSection.YOUR_DATA,
            icon = Icons.Rounded.FileDownload,
            queryTarget = stringResource(R.string.import_data),
            keywords = listOf("import", "restore", "backup", "json"),
        ),

        // Support
        GlobalSettingItem(
            title = "Check for Updates",
            subtitle = stringResource(R.string.support_community),
            section = SettingsSection.SUPPORT,
            icon = Icons.Rounded.Update,
            queryTarget = "Check for Updates",
            keywords = listOf("update", "version", "check", "download"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.reddit_community),
            subtitle = stringResource(R.string.reddit_community_subtitle),
            section = SettingsSection.SUPPORT,
            icon = Icons.Rounded.Favorite,
            queryTarget = stringResource(R.string.reddit_community),
            keywords = listOf("reddit", "community", "social"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.github_repository),
            subtitle = "github.com/code4nigel/ChordPlay",
            section = SettingsSection.SUPPORT,
            icon = Icons.Rounded.OpenInNew,
            queryTarget = stringResource(R.string.github_repository),
            keywords = listOf("github", "source", "issues", "bugs", "repo"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.latest_releases),
            subtitle = stringResource(R.string.latest_releases_subtitle),
            section = SettingsSection.SUPPORT,
            icon = Icons.Rounded.Info,
            queryTarget = stringResource(R.string.latest_releases),
            keywords = listOf("releases", "apk", "changelog"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.developer_profile),
            subtitle = "github.com/code4nigel",
            section = SettingsSection.SUPPORT,
            icon = Icons.Rounded.Person,
            queryTarget = stringResource(R.string.developer_profile),
            keywords = listOf("developer", "profile", "author", "nigel"),
        ),

        // Integrations
        GlobalSettingItem(
            title = stringResource(R.string.account_integrations),
            subtitle = stringResource(R.string.default_youtube_profile),
            section = SettingsSection.INTEGRATIONS,
            icon = Icons.Rounded.SwitchAccount,
            queryTarget = stringResource(R.string.account_integrations),
            keywords = listOf("google", "youtube", "profile", "channel", "account", "sign in"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.spotify),
            subtitle = stringResource(R.string.account_integrations),
            section = SettingsSection.INTEGRATIONS,
            icon = Icons.Rounded.Link,
            queryTarget = stringResource(R.string.spotify),
            keywords = listOf("spotify", "canvas", "spdc", "connect"),
        ),
        GlobalSettingItem(
            title = "Discord Rich Presence",
            subtitle = stringResource(R.string.account_integrations),
            section = SettingsSection.INTEGRATIONS,
            icon = Icons.Rounded.Cloud,
            queryTarget = "Discord",
            keywords = listOf("discord", "rpc", "presence", "rich presence"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.listen_together),
            subtitle = stringResource(R.string.listen_together_subtitle),
            section = SettingsSection.INTEGRATIONS,
            icon = Icons.Rounded.Groups,
            queryTarget = stringResource(R.string.listen_together),
            keywords = listOf("listen together", "party", "room", "share", "friends"),
        ),
        GlobalSettingItem(
            title = "ListenBrainz",
            subtitle = stringResource(R.string.scrobbling),
            section = SettingsSection.INTEGRATIONS,
            icon = Icons.Rounded.Cloud,
            queryTarget = "ListenBrainz",
            keywords = listOf("listenbrainz", "scrobble", "token"),
        ),
        GlobalSettingItem(
            title = "Last.fm",
            subtitle = stringResource(R.string.scrobbling),
            section = SettingsSection.INTEGRATIONS,
            icon = Icons.Rounded.History,
            queryTarget = "Last.fm",
            keywords = listOf("last.fm", "lastfm", "scrobble", "now playing"),
        ),
        GlobalSettingItem(
            title = stringResource(R.string.scrobble_timing),
            subtitle = stringResource(R.string.scrobble_delay_subtitle),
            section = SettingsSection.INTEGRATIONS,
            icon = Icons.Rounded.Tune,
            queryTarget = stringResource(R.string.scrobble_timing),
            keywords = listOf("scrobble timing", "delay", "seconds", "duration"),
        ),
    )
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
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left side: Circular buttons for GitHub and Reddit
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // GitHub circle button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        shape = CircleShape,
                    )
                    .clickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/code4nigel/ChordPlay")))
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_github),
                    contentDescription = "ChordPlay GitHub",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }

            // Reddit circle button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        shape = CircleShape,
                    )
                    .clickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/r/ChordPlay/")))
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_reddit),
                    contentDescription = "ChordPlay Reddit",
                    tint = Color(0xFFFF4500),
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        // Right side: Attribution text in two lines
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "ChordPlay v$version by Shivanshu (code4nigel)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/code4nigel/ChordPlay")))
                    }
                },
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Based on BitChord by Kushagra Singh",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kushagrasinghx/BitChord")))
                    }
                },
            )
        }
    }
}
