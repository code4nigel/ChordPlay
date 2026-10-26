package com.music.bitchord.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.music.bitchord.data.model.Account
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.player.LyricsStyleManager
import com.music.bitchord.ui.screens.settings.SettingsSection
import com.music.bitchord.ui.screens.settings.sections.AboutSection
import com.music.bitchord.ui.screens.settings.sections.AppearanceSection
import com.music.bitchord.ui.screens.settings.sections.AudioSection
import com.music.bitchord.ui.screens.settings.sections.IntegrationsSection
import com.music.bitchord.ui.screens.settings.sections.LyricsSection
import com.music.bitchord.ui.screens.settings.sections.PlayerSection
import com.music.bitchord.ui.screens.settings.sections.StorageSection

/**
 * Root Settings screen featuring a modular, Prism-inspired category hub
 * and focused sub-screens with fluid, non-bouncy deceleration transitions.
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
                // Forward navigation: smooth deceleration glide into view (no bounce)
                (slideInHorizontally(
                    animationSpec = tween(durationMillis = 260, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)),
                    initialOffsetX = { fullWidth -> (fullWidth * 0.12f).toInt() },
                ) + fadeIn(animationSpec = tween(200))) togetherWith
                (slideOutHorizontally(
                    animationSpec = tween(durationMillis = 200, easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)),
                    targetOffsetX = { fullWidth -> -(fullWidth * 0.12f).toInt() },
                ) + fadeOut(animationSpec = tween(150)))
            } else if (targetState == null && initialState != null) {
                // Backward navigation: smooth pop back to Hub (no bounce)
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
                contentPadding = contentPadding,
            )
            SettingsSection.APPEARANCE -> AppearanceSection(
                onBack = { currentSection = null },
                onAppLanguage = onAppLanguage,
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
            SettingsSection.STORAGE -> StorageSection(
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
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }

    val theme by AppSettings.themeMode.collectAsStateWithLifecycle()
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val fullBleedArtwork by AppSettings.fullBleedArtwork.collectAsStateWithLifecycle()
    val animatedCanvas by AppSettings.animatedCanvas.collectAsStateWithLifecycle()
    val lyricsStyle by LyricsStyleManager.style.collectAsStateWithLifecycle()
    val syncedLyrics by AppSettings.syncedLyrics.collectAsStateWithLifecycle()
    val wifiQuality by AppSettings.audioQualityWifi.collectAsStateWithLifecycle()
    val crossfade by AppSettings.crossfadeSeconds.collectAsStateWithLifecycle()
    val cacheLimitBytes by AppSettings.audioCacheLimitBytes.collectAsStateWithLifecycle()
    val cacheLimitMb = (cacheLimitBytes / (1024L * 1024L)).toInt()

    @Composable
    fun getSubtitle(section: SettingsSection): String = when (section) {
        SettingsSection.APPEARANCE -> "Theme • ${theme.localizedLabel()}, ${if (liquidGlass) "Frosted Glass" else "Solid"}"
        SettingsSection.PLAYER -> "Artwork • ${if (fullBleedArtwork) "Full Bleed" else "Standard Sleeve"}, ${if (animatedCanvas) "Canvas On" else "Canvas Off"}"
        SettingsSection.LYRICS -> "Typography • ${lyricsStyle.fontChoice.label}, ${if (syncedLyrics) "Syllable Sync" else "Standard"}"
        SettingsSection.AUDIO -> "Audio • ${wifiQuality.localizedQualityLabel()}, ${crossfade}s Crossfade"
        SettingsSection.STORAGE -> "Storage • $cacheLimitMb MB Limit"
        SettingsSection.INTEGRATIONS -> "Account • ${if (signedIn) channelName ?: account?.name ?: stringResource(R.string.youtube_music_account) else "Guest"}"
        SettingsSection.ABOUT -> "ChordPlay v${BuildConfig.VERSION_NAME} • Updates, Links & Info"
    }

    val filteredSections = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            SettingsSection.entries
        } else {
            val q = searchQuery.trim().lowercase()
            SettingsSection.entries.filter { section ->
                val keywords = when (section) {
                    SettingsSection.APPEARANCE -> "theme dark light oled black blur liquid glass animation refresh rate high performance display language"
                    SettingsSection.PLAYER -> "player artwork sleeve full bleed canvas spotify mesh gradient volume bar song status seek nerd stats"
                    SettingsSection.LYRICS -> "lyrics typography font style scale size alignment center left sync syllable source translation"
                    SettingsSection.AUDIO -> "audio quality streaming wifi cellular crossfade smart fade automix dolby atmos spatial equalizer usb dac loudness normalization silence"
                    SettingsSection.STORAGE -> "storage cache clear limit download offline backup restore export import local music folder"
                    SettingsSection.INTEGRATIONS -> "account sign in out google youtube music scrobble last.fm lastfm listenbrainz listen together discord rpc"
                    SettingsSection.ABOUT -> "miscellaneous about chordplay version updates release notes github developer licenses bitchord upstream replay"
                }
                keywords.contains(q) || section.name.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        // Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
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

        Spacer(Modifier.height(16.dp))

        // Account Banner Card
        if (searchQuery.isBlank()) {
            AccountHubCard(
                signedIn = signedIn,
                account = account,
                channelName = channelName,
                onSignIn = onSignIn,
                onClick = { onSelectSection(SettingsSection.INTEGRATIONS) },
            )
            Spacer(Modifier.height(16.dp))
        }

        // Category Cards Section
        Text(
            text = if (searchQuery.isBlank()) "PREFERENCES" else "RESULTS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
            ) {
                filteredSections.forEachIndexed { index, section ->
                    CategoryHubRow(
                        section = section,
                        subtitle = getSubtitle(section),
                        onClick = { onSelectSection(section) },
                    )
                    if (index < filteredSections.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 64.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Colophon and attribution footer
        if (searchQuery.isBlank()) {
            SettingsFooter(version = BuildConfig.VERSION_NAME)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AccountHubCard(
    signedIn: Boolean,
    account: Account?,
    channelName: String?,
    onSignIn: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (signedIn && account != null) {
            if (!account.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = account.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channelName ?: account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp),
                    )
                }
                Text(
                    text = if (channelName != null && channelName != account.name) {
                        account.name
                    } else {
                        stringResource(R.string.youtube_music_account)
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(15.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(46.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.youtube_music_account),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.tap_to_sign_in_google),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onSignIn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = stringResource(R.string.sign_in),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun CategoryHubRow(
    section: SettingsSection,
    subtitle: String,
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
                .clip(RoundedCornerShape(10.dp))
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
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(section.titleRes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
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
                    textDecoration = TextDecoration.Underline,
                ),
            )
            append("ChordPlay $version by ")
            withLink(LinkAnnotation.Url("https://github.com/code4nigel", linkStyles)) {
                append("Shivanshu (code4nigel)")
            }
            append("\n")
            withLink(LinkAnnotation.Url("https://github.com/code4nigel/ChordPlay", linkStyles)) {
                append("GitHub")
            }
            append("   ")
            withLink(LinkAnnotation.Url("https://github.com/code4nigel", linkStyles)) {
                append("Developer")
            }
            append("   ")
            withLink(LinkAnnotation.Url("https://github.com/code4nigel/ChordPlay/releases", linkStyles)) {
                append("Releases")
            }
            append("\nBased on BitChord by Kushagra Singh")
        },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
    )
}
