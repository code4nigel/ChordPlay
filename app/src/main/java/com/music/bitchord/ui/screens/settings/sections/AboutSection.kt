package com.music.bitchord.ui.screens.settings.sections

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.OpenInNew
import android.widget.Toast
import com.music.bitchord.data.AppUpdateChecker
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.screens.*

/**
 * About ChordPlay sub-page containing app info, replay, repository links, and credits.
 */
@Composable
fun AboutSection(
    onOpenReplay: () -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    initialSearchQuery: String = "",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var searchQuery by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }
    val replayGenres by AppSettings.replayGenres.collectAsStateWithLifecycle()
    val dontRepeatSuggestions by AppSettings.dontRepeatSuggestions.collectAsStateWithLifecycle()

    val version = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.8"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.miscellaneous),
            onBack = onBack,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
        )

        val showUpdate = matchesSearch(
            searchQuery,
            "Check for Updates", "Install Update", "Version $version", "ChordPlay",
            "update", "version", "download",
        )
        val showReplay = matchesSearch(
            searchQuery,
            stringResource(R.string.replay),
            stringResource(R.string.replay_subtitle),
            "replay", "stats", "insights", "analytics",
        )
        val showWorkOutGenres = matchesSearch(
            searchQuery,
            stringResource(R.string.work_out_genres),
            stringResource(R.string.replay_genres_enabled_subtitle),
            stringResource(R.string.replay_genres_disabled_subtitle),
            "genres", "replay",
        )
        val showDontRepeatSongs = matchesSearch(
            searchQuery,
            stringResource(R.string.dont_repeat_songs),
            stringResource(R.string.dont_repeat_songs_subtitle),
            "repeat", "suggestions", "history",
        )
        val showReddit = matchesSearch(
            searchQuery,
            stringResource(R.string.reddit_community),
            stringResource(R.string.reddit_community_subtitle),
            "reddit", "community",
        )
        val showGithub = matchesSearch(
            searchQuery,
            stringResource(R.string.github_repository),
            "code4nigel/ChordPlay",
            "github", "source", "issues",
        )
        val showReleases = matchesSearch(
            searchQuery,
            stringResource(R.string.latest_releases),
            stringResource(R.string.latest_releases_subtitle),
            "releases", "download", "apk",
        )
        val showDevProfile = matchesSearch(
            searchQuery,
            stringResource(R.string.developer_profile),
            "code4nigel",
            "developer", "profile", "author", "nigel",
        )
        val showUpstreamRepo = matchesSearch(
            searchQuery,
            stringResource(R.string.bitchord_upstream_repo),
            stringResource(R.string.bitchord_upstream_subtitle),
            "bitchord", "kushagra", "upstream",
        )

        val anyAboutMatch = showUpdate || showReplay || showWorkOutGenres || showDontRepeatSongs ||
            showReddit || showGithub || showReleases || showDevProfile || showUpstreamRepo

        // Hero Card: ChordPlay Branding & Version (shown when not searching, or when update matches)
        if (searchQuery.isBlank() || showUpdate) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GROUP_INSET, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_logo),
                            contentDescription = "ChordPlay",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "ChordPlay",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    Text(
                        text = "Version $version",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Fluid Glassmorphism • Dynamic Artwork Aura",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Spacer(Modifier.height(14.dp))

                    var checkingUpdate by remember { mutableStateOf(false) }
                    val updateScope = rememberCoroutineScope()
                    val updateInfo by AppUpdateChecker.available.collectAsStateWithLifecycle()
                    val downloadState by AppUpdateChecker.download.collectAsStateWithLifecycle()

                    Button(
                        onClick = {
                            when (val state = downloadState) {
                                is AppUpdateChecker.DownloadState.Downloading -> {
                                    // Already downloading, do nothing
                                }
                                is AppUpdateChecker.DownloadState.Ready -> {
                                    AppUpdateChecker.installApk(context, state.file)
                                }
                                else -> {
                                    if (updateInfo != null) {
                                        if (updateInfo?.apkUrl != null) {
                                            updateScope.launch {
                                                AppUpdateChecker.downloadApk(context)
                                            }
                                        } else {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo?.releaseUrl)))
                                            }
                                        }
                                    } else if (!checkingUpdate) {
                                        checkingUpdate = true
                                        updateScope.launch {
                                            AppUpdateChecker.check()
                                            checkingUpdate = false
                                            if (AppUpdateChecker.available.value == null) {
                                                Toast.makeText(context, "ChordPlay is up to date (v$version)", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (checkingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Checking...")
                        } else when (val state = downloadState) {
                            is AppUpdateChecker.DownloadState.Downloading -> {
                                CircularProgressIndicator(
                                    progress = { state.fraction },
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Downloading ${(state.fraction * 100).toInt()}%")
                            }
                            is AppUpdateChecker.DownloadState.Ready -> {
                                Icon(
                                    imageVector = Icons.Rounded.SystemUpdate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Install Update")
                            }
                            else -> {
                                if (updateInfo != null) {
                                    Icon(
                                        imageVector = Icons.Rounded.SystemUpdate,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Download v${updateInfo?.version}")
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Update,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Check for Updates")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (searchQuery.isNotBlank() && !anyAboutMatch) {
            SettingsSearchEmpty(query = searchQuery)
        } else {
            val hasUserDataSection = showReplay || showWorkOutGenres || showDontRepeatSongs
            val hasLinksSection = showReddit || showGithub || showReleases || showDevProfile || showUpstreamRepo

            if (hasUserDataSection || hasLinksSection) {
                // Single Unified About Block
                SettingsGroup(header = stringResource(R.string.your_data)) {
                    var userPreceding = false
                    if (showReplay) {
                        val replayTitle = stringResource(R.string.replay)
                        SettingsRow(
                            icon = Icons.Rounded.BarChart,
                            title = replayTitle,
                            subtitle = stringResource(R.string.replay_subtitle),
                            onClick = onOpenReplay,
                        )
                        userPreceding = true
                    }

                    if (showWorkOutGenres) {
                        if (userPreceding) RowDivider()
                        val workOutGenresTitle = stringResource(R.string.work_out_genres)
                        SettingsRow(
                            icon = Icons.Rounded.LocalOffer,
                            title = workOutGenresTitle,
                            subtitle = if (replayGenres) {
                                stringResource(R.string.replay_genres_enabled_subtitle)
                            } else {
                                stringResource(R.string.replay_genres_disabled_subtitle)
                            },
                            trailing = {
                                Switch(
                                    checked = replayGenres,
                                    onCheckedChange = AppSettings::setReplayGenres,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setReplayGenres(!replayGenres) },
                        )
                        userPreceding = true
                    }

                    if (showDontRepeatSongs) {
                        if (userPreceding) RowDivider()
                        val dontRepeatSongsTitle = stringResource(R.string.dont_repeat_songs)
                        SettingsRow(
                            icon = Icons.Rounded.History,
                            title = dontRepeatSongsTitle,
                            subtitle = stringResource(R.string.dont_repeat_songs_subtitle),
                            trailing = {
                                Switch(
                                    checked = dontRepeatSuggestions,
                                    onCheckedChange = AppSettings::setDontRepeatSuggestions,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setDontRepeatSuggestions(!dontRepeatSuggestions) },
                        )
                    }

                    // Project Links Section Divider
                    if (hasLinksSection) {
                        if (hasUserDataSection) {
                            SettingsSectionDivider(title = stringResource(R.string.links_and_community))
                        }

                        var linkPreceding = false
                        if (showReddit) {
                            SettingsRow(
                                icon = Icons.Rounded.OpenInNew,
                                title = stringResource(R.string.reddit_community),
                                subtitle = stringResource(R.string.reddit_community_subtitle),
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/r/ChordPlay/")))
                                    }
                                },
                            )
                            linkPreceding = true
                        }

                        if (showGithub) {
                            if (linkPreceding) RowDivider()
                            SettingsRow(
                                icon = Icons.Rounded.OpenInNew,
                                title = stringResource(R.string.github_repository),
                                subtitle = "code4nigel/ChordPlay",
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/code4nigel/ChordPlay")))
                                    }
                                },
                            )
                            linkPreceding = true
                        }

                        if (showReleases) {
                            if (linkPreceding) RowDivider()
                            SettingsRow(
                                icon = Icons.Rounded.OpenInNew,
                                title = stringResource(R.string.latest_releases),
                                subtitle = stringResource(R.string.latest_releases_subtitle),
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/code4nigel/ChordPlay/releases")))
                                    }
                                },
                            )
                            linkPreceding = true
                        }

                        if (showDevProfile) {
                            if (linkPreceding) RowDivider()
                            SettingsRow(
                                icon = Icons.Rounded.OpenInNew,
                                title = stringResource(R.string.developer_profile),
                                subtitle = "github.com/code4nigel",
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/code4nigel")))
                                    }
                                },
                            )
                            linkPreceding = true
                        }

                        if (showUpstreamRepo) {
                            if (linkPreceding) RowDivider()
                            SettingsRow(
                                icon = Icons.Rounded.OpenInNew,
                                title = stringResource(R.string.bitchord_upstream_repo),
                                subtitle = stringResource(R.string.bitchord_upstream_subtitle),
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kushagrasinghx/BitChord")))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        if (searchQuery.isBlank()) {
            // Attribution & Colophon
            Text(
                text = buildAnnotatedString {
                    val linkStyles = TextLinkStyles(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                        ),
                    )
                    append("ChordPlay $version by Shivanshu (code4nigel)\n")
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
                    append("\nBased on BitChord by Kushagra Singh\n")
                    withLink(LinkAnnotation.Url("https://github.com/kushagrasinghx/BitChord", linkStyles)) {
                        append("GitHub")
                    }
                    append("   ")
                    withLink(LinkAnnotation.Url("https://github.com/kushagrasinghx", linkStyles)) {
                        append("Developer")
                    }
                    append("   ")
                    withLink(LinkAnnotation.Url("https://github.com/kushagrasinghx/BitChord/releases", linkStyles)) {
                        append("Releases")
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
            )
        }
    }
}
