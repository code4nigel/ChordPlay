package com.music.bitchord.ui.screens.settings.sections

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.CommunitySupportDialog
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.AppUpdateChecker
import com.music.bitchord.ui.screens.GROUP_INSET
import com.music.bitchord.ui.screens.RowDivider
import com.music.bitchord.ui.screens.SettingsGroup
import com.music.bitchord.ui.screens.SettingsRow
import com.music.bitchord.ui.screens.SettingsSearchEmpty
import com.music.bitchord.ui.screens.SettingsSectionDivider
import com.music.bitchord.ui.screens.SettingsSubPageHeader
import com.music.bitchord.ui.screens.matchesSearch
import kotlinx.coroutines.launch

/**
 * Dedicated sub-page for ChordPlay support, community discussion,
 * project repository, developer profile, and update management.
 */
@Composable
fun SupportSection(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    initialSearchQuery: String = "",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val updateScope = rememberCoroutineScope()
    var checkingUpdate by remember { mutableStateOf(false) }
    var searchQuery by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }
    var showCommunityDialog by remember { mutableStateOf(false) }
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

        val showUpdateCheck = matchesSearch(
            searchQuery,
            "Check for Updates", "Install Update", "Version $version", "ChordPlay",
            "update", "version", "download",
        )
        val showReddit = matchesSearch(
            searchQuery,
            stringResource(R.string.reddit_community),
            stringResource(R.string.reddit_community_subtitle),
            "reddit", "community", "social",
        )
        val showGithub = matchesSearch(
            searchQuery,
            stringResource(R.string.github_repository),
            "github.com/code4nigel/ChordPlay",
            "source", "github", "issues", "bugs", "repo",
        )
        val showDevProfile = matchesSearch(
            searchQuery,
            stringResource(R.string.developer_profile),
            "github.com/code4nigel",
            "developer", "profile", "author", "nigel",
        )
        val showReleases = matchesSearch(
            searchQuery,
            stringResource(R.string.latest_releases),
            stringResource(R.string.latest_releases_subtitle),
            "releases", "changelog", "download",
        )
        val showUpstreamRepo = matchesSearch(
            searchQuery,
            stringResource(R.string.bitchord_upstream_repo),
            stringResource(R.string.bitchord_upstream_subtitle),
            "upstream", "bitchord", "kushagra",
        )

        val anySupportMatch = showUpdateCheck || showReddit || showGithub || showDevProfile || showReleases || showUpstreamRepo

        // Hero Card: ChordPlay Branding (shown when not searching, or when update matches)
        if (searchQuery.isBlank() || showUpdateCheck) {
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
        }

        if (searchQuery.isNotBlank() && !anySupportMatch) {
            SettingsSearchEmpty(query = searchQuery)
        } else {
            // Dedicated Developer Section
            if (showDevProfile) {
                SettingsGroup(
                    header = "DEVELOPER",
                ) {
                    DeveloperProfileRow(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/code4nigel")))
                            }
                        },
                    )
                    RowDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, "Absolute madman building ChordPlay! 🚀", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "💡 Legend has it Nigel spent his time building ChordPlay instead of prepping for exams... Absolute madman! 💀",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            lineHeight = 16.sp,
                        )
                    }
                }
            }

            val hasCommunityLinks = showReddit || showGithub
            val hasReleasesOrUpstream = showReleases || showUpstreamRepo

            if (hasCommunityLinks || hasReleasesOrUpstream) {
                // Unified Support & Community Block
                SettingsGroup(
                    header = stringResource(R.string.links_and_community),
                    footer = "Join our community on Reddit, report bugs, and follow development on GitHub.",
                ) {
                    SettingsRow(
                        iconPainter = painterResource(R.drawable.ic_chordplay_squircle),
                        title = "ChordPlay Community Hub",
                        subtitle = "Follow developer, view Reddit & GitHub repositories",
                        trailing = {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        onClick = { showCommunityDialog = true },
                    )
                    RowDivider()

                    var linkPreceding = false
                    if (showReddit) {
                        SettingsRow(
                            iconPainter = painterResource(R.drawable.ic_reddit),
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
                        linkPreceding = true
                    }

                    if (showGithub) {
                        if (linkPreceding) RowDivider()
                        SettingsRow(
                            iconPainter = painterResource(R.drawable.ic_github),
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
                        linkPreceding = true
                    }

                    // Releases & Upstream Credit Section Divider
                    if (hasReleasesOrUpstream) {
                        if (hasCommunityLinks) {
                            SettingsSectionDivider(title = stringResource(R.string.latest_releases))
                        }

                        var relPreceding = false
                        if (showReleases) {
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
                            relPreceding = true
                        }

                        if (showUpstreamRepo) {
                            if (relPreceding) RowDivider()
                            SettingsRow(
                                iconPainter = painterResource(R.drawable.ic_github),
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
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showCommunityDialog) {
        CommunitySupportDialog(
            onDismiss = {
                showCommunityDialog = false
                AppSettings.recordCommunityPopupDismissed()
            },
        )
    }
}

@Composable
private fun DeveloperProfileRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeveloperAvatarWithFloatingNotes()

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "NigelWeb",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "• Shivanshu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = "Lead Architect & Developer • ChordPlay",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(5.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_github),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "github.com/code4nigel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Rounded.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun DeveloperAvatarWithFloatingNotes(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "DevAvatarAnimation")

    val bobbingY by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bobbingY",
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathingScale",
    )

    val note1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "note1",
    )
    val note2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, delayMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "note2",
    )
    val note3Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2900, delayMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "note3",
    )
    val note4Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, delayMillis = 1950, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "note4",
    )

    Box(
        modifier = modifier.size(72.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Floating Music Notes emitting outwards
        FloatingMusicNote(
            symbol = "♫",
            progress = note1Progress,
            startX = 16f,
            startY = -10f,
            driftX = 14f,
            driftY = -24f,
            color = MaterialTheme.colorScheme.primary,
        )

        FloatingMusicNote(
            symbol = "♪",
            progress = note2Progress,
            startX = -18f,
            startY = -8f,
            driftX = -12f,
            driftY = -26f,
            color = Color(0xFF8B5CF6),
        )

        FloatingMusicNote(
            symbol = "♬",
            progress = note3Progress,
            startX = -20f,
            startY = 12f,
            driftX = -10f,
            driftY = -22f,
            color = MaterialTheme.colorScheme.secondary,
        )

        FloatingMusicNote(
            symbol = "♪",
            progress = note4Progress,
            startX = 18f,
            startY = 10f,
            driftX = 12f,
            driftY = -20f,
            color = Color(0xFF38BDF8),
        )

        // Subtle glow backdrop ring
        Box(
            modifier = Modifier
                .size(54.dp)
                .graphicsLayer {
                    translationY = bobbingY * density
                    scaleX = breathingScale * 1.05f
                    scaleY = breathingScale * 1.05f
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        )

        // Central Avatar Image
        Image(
            painter = painterResource(R.drawable.dev_avatar),
            contentDescription = "Developer Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer {
                    translationY = bobbingY * density
                    scaleX = breathingScale
                    scaleY = breathingScale
                }
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
        )
    }
}

@Composable
private fun FloatingMusicNote(
    symbol: String,
    progress: Float,
    startX: Float,
    startY: Float,
    driftX: Float,
    driftY: Float,
    color: Color,
) {
    val alpha = when {
        progress < 0.22f -> (progress / 0.22f)
        progress > 0.68f -> (1f - (progress - 0.68f) / 0.32f).coerceIn(0f, 1f)
        else -> 1f
    }
    val currentX = startX + (driftX * progress)
    val currentY = startY + (driftY * progress)
    val scale = 0.75f + (0.45f * progress)
    val rotation = (progress * 30f) - 15f

    Text(
        text = symbol,
        color = color.copy(alpha = alpha * 0.95f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.graphicsLayer {
            translationX = currentX * density
            translationY = currentY * density
            scaleX = scale
            scaleY = scale
            rotationZ = rotation
            this.alpha = alpha
        },
    )
}

