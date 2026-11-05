package com.music.bitchord.ui.screens.settings.sections

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.AppUpdateChecker
import com.music.bitchord.ui.screens.GROUP_INSET
import com.music.bitchord.ui.screens.RowDivider
import com.music.bitchord.ui.screens.SettingsGroup
import com.music.bitchord.ui.screens.SettingsRow
import com.music.bitchord.ui.screens.SettingsSectionDivider
import com.music.bitchord.ui.screens.SettingsSubPageHeader
import kotlinx.coroutines.launch

/**
 * Dedicated sub-page for ChordPlay support, community discussion,
 * project repository, developer profile, and update management.
 */
@Composable
fun SupportSection(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val updateScope = rememberCoroutineScope()
    var checkingUpdate by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val updateInfo by AppUpdateChecker.available.collectAsStateWithLifecycle()
    val downloadState by AppUpdateChecker.download.collectAsStateWithLifecycle()

    val version = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "2.0.4"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.support_community),
            onBack = onBack,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
        )

        // Hero Card: ChordPlay Branding
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
                    text = "Modern Lossless Sound • Fluid Glassmorphism",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = {
                        when (val state = downloadState) {
                            is AppUpdateChecker.DownloadState.Downloading -> {}
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

        // Single Unified Support & Community Block
        SettingsGroup(
            header = stringResource(R.string.links_and_community),
            footer = "Join our community on Reddit, report bugs, and follow development on GitHub.",
        ) {
            // Reddit Subreddit
            SettingsRow(
                icon = Icons.Rounded.Favorite,
                title = stringResource(R.string.reddit_community),
                subtitle = stringResource(R.string.reddit_community_subtitle),
                trailing = {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/r/ChordPlay/")))
                    }
                },
            )

            RowDivider()

            // GitHub Repository
            SettingsRow(
                icon = Icons.Rounded.OpenInNew,
                title = stringResource(R.string.github_repository),
                subtitle = "github.com/code4nigel/ChordPlay • Source code & issues",
                trailing = {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/code4nigel/ChordPlay")))
                    }
                },
            )

            RowDivider()

            // Developer Profile
            SettingsRow(
                icon = Icons.Rounded.Person,
                title = stringResource(R.string.developer_profile),
                subtitle = "github.com/code4nigel • Lead developer profile",
                trailing = {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/code4nigel")))
                    }
                },
            )

            // Releases & Upstream Credit Section Divider
            SettingsSectionDivider(title = stringResource(R.string.latest_releases))

            SettingsRow(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.latest_releases),
                subtitle = stringResource(R.string.latest_releases_subtitle),
                trailing = {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/code4nigel/ChordPlay/releases")))
                    }
                },
            )

            RowDivider()

            SettingsRow(
                icon = Icons.Rounded.OpenInNew,
                title = stringResource(R.string.bitchord_upstream_repo),
                subtitle = stringResource(R.string.bitchord_upstream_subtitle),
                trailing = {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kushagrasinghx/BitChord")))
                    }
                },
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

