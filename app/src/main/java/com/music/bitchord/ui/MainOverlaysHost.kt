/*
 * Copyright (C) 2024-2026 Kushagra Singh and BitChord contributors <https://github.com/kushagrasinghx/BitChord>
 * Copyright (C) 2026 Shivanshu (code4nigel) <https://github.com/code4nigel/ChordPlay>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.music.bitchord.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.auth.DiscordLoginScreen
import com.music.bitchord.auth.GoogleAccountSession
import com.music.bitchord.auth.SpotifyLoginScreen
import com.music.bitchord.auth.WebSessionMode
import com.music.bitchord.auth.YtMusicLoginScreen
import com.music.bitchord.data.AppUpdateChecker
import com.music.bitchord.data.scrobbling.LastFM
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.SongSort
import com.music.bitchord.data.sources.SourceConfig
import com.music.bitchord.data.sources.SourceKind
import com.music.bitchord.data.sources.SourceRegistry
import com.music.bitchord.data.webdav.WebDavUploads
import com.music.bitchord.ui.components.AccountProfileSelector
import com.music.bitchord.ui.components.AppLanguageDialog
import com.music.bitchord.ui.components.BulkAddProgressDialog
import com.music.bitchord.ui.components.ConfirmationAlert
import com.music.bitchord.ui.components.DiwaliCelebrationDialog
import com.music.bitchord.ui.components.FieldConfig
import com.music.bitchord.ui.components.LastfmLoginAlert
import com.music.bitchord.ui.components.ListenBrainzTokenAlert
import com.music.bitchord.ui.components.LyricsSourcesDialog
import com.music.bitchord.ui.components.ServerEditorHost
import com.music.bitchord.ui.components.TranslationLanguageDialog
import com.music.bitchord.ui.components.UpdateAvailableDialog
import com.music.bitchord.ui.components.WebDavConflictAlert
import com.music.bitchord.ui.components.optimizedHazeEffect
import com.music.bitchord.ui.screens.DiscordDialog
import com.music.bitchord.ui.screens.DiscordDialogHost
import com.music.bitchord.ui.screens.PartyServerEditor
import com.music.bitchord.ui.screens.SourceEditorAlert
import com.music.bitchord.ui.screens.SpotifyCanvasAuthScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Root-level modal dialogs, auth WebViews, and account/sort overlays extracted
 * out of [com.music.bitchord.MainActivity] to keep the activity concise.
 */
@Composable
internal fun MainDialogsHost(
    hazeState: HazeState,
    scope: CoroutineScope,
    context: Context,
    viewModel: MainViewModel,
    webSession: WebSessionMode?,
    onWebSessionChange: (WebSessionMode?) -> Unit,
    onSelectTab: (Int) -> Unit,
    googleAccounts: List<GoogleAccountSession>,
    activeAccountId: String?,
    activeProfileId: String?,
    bulkAddProgress: BulkAddProgress?,
    onDismissBulkAdd: () -> Unit,
    showDiwaliDialog: Boolean,
    onDismissDiwaliDialog: () -> Unit,
    showUpdateDialog: Boolean,
    onDismissUpdateDialog: () -> Unit,
    updateNotice: AppUpdateChecker.UpdateInfo?,
    showLyricsSources: Boolean,
    onDismissLyricsSources: () -> Unit,
    songSortMenuOpen: Boolean,
    onDismissSongSortMenu: () -> Unit,
    songSort: SongSort,
    detailBrowseId: String?,
    showAccountSelector: Boolean,
    onDismissAccountSelector: () -> Unit,
    onOpenSettings: () -> Unit,
    showAppLanguage: Boolean,
    onDismissAppLanguage: () -> Unit,
    showTranslationLanguage: Boolean,
    onDismissTranslationLanguage: () -> Unit,
    showListenBrainzLogin: Boolean,
    onDismissListenBrainzLogin: () -> Unit,
    listenBrainzToken: String,
    showLastfmLogin: Boolean,
    onDismissLastfmLogin: () -> Unit,
    showWebDavEditor: Boolean,
    onDismissWebDavEditor: () -> Unit,
    showSmbEditor: Boolean,
    onDismissSmbEditor: () -> Unit,
    showDiscordLogin: Boolean,
    onDismissDiscordLogin: () -> Unit,
    showSpotifyLogin: Boolean,
    onDismissSpotifyLogin: () -> Unit,
    showSpotifyCanvasAuth: Boolean,
    onDismissSpotifyCanvasAuth: () -> Unit,
    discordDialog: DiscordDialog?,
    onDismissDiscordDialog: () -> Unit,
    editingPartyServer: Boolean,
    onDismissPartyServer: () -> Unit,
    editingSource: SourceConfig?,
    onDismissEditingSource: () -> Unit,
    confirmJioSaavn: Boolean,
    onDismissConfirmJioSaavn: () -> Unit,
) {
    // ---- Google sign-in (full screen WebView) ----
    webSession?.let { mode ->
        BackHandler { onWebSessionChange(null) }
        var captureRequest by remember(mode) { mutableIntStateOf(0) }
        var captureFailed by remember(mode) { mutableStateOf(false) }
        var pageReady by remember(mode) { mutableStateOf(false) }
        var confirmingProfile by remember(mode) { mutableStateOf(false) }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { onWebSessionChange(null) }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = when (mode) {
                                WebSessionMode.SIGN_IN -> stringResource(R.string.sign_in_youtube_music)
                                WebSessionMode.SWITCH_CHANNEL -> stringResource(R.string.choose_profile)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        if (pageReady) {
                            Text(
                                text = if (captureFailed) {
                                    stringResource(R.string.profile_unavailable)
                                } else {
                                    stringResource(R.string.switch_profile_hint)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                    }
                    if (pageReady) {
                        TextButton(onClick = {
                            captureFailed = false
                            confirmingProfile = true
                            captureRequest++
                        }, enabled = !confirmingProfile) {
                            Text(
                                if (confirmingProfile) stringResource(R.string.checking)
                                else stringResource(R.string.use_this_profile),
                            )
                        }
                    }
                }
                YtMusicLoginScreen(
                    mode = mode,
                    initialCookie = if (mode == WebSessionMode.SWITCH_CHANNEL) {
                        googleAccounts.firstOrNull { it.accountId == activeAccountId }?.cookie
                    } else null,
                    captureRequest = captureRequest,
                    onPageReady = { pageReady = it },
                    onCaptureUnavailable = {
                        confirmingProfile = false
                        captureFailed = true
                    },
                    onCaptured = { session ->
                        viewModel.onWebSession(session, mode) { accepted ->
                            confirmingProfile = false
                            if (accepted) {
                                onWebSessionChange(null)
                                if (mode == WebSessionMode.SIGN_IN) onSelectTab(2)
                            } else {
                                captureFailed = true
                            }
                        }
                    },
                )
            }
        }
    }

    bulkAddProgress?.let { progress ->
        BulkAddProgressDialog(
            progress = progress,
            hazeState = hazeState,
            onDismiss = onDismissBulkAdd,
        )
    }

    if (showDiwaliDialog) {
        DiwaliCelebrationDialog(onDismiss = onDismissDiwaliDialog)
    }

    if (showUpdateDialog) {
        updateNotice?.let { update ->
            UpdateAvailableDialog(
                version = update.version,
                notes = update.notes,
                hazeState = hazeState,
                onDismiss = onDismissUpdateDialog,
                onDownload = {
                    if (update.apkUrl != null) {
                        scope.launch { AppUpdateChecker.downloadApk(context) }
                    } else {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl)))
                        onDismissUpdateDialog()
                    }
                },
                onCancelDownload = { AppUpdateChecker.cancelDownload() },
                onInstall = {
                    val ready = AppUpdateChecker.download.value as? AppUpdateChecker.DownloadState.Ready
                    ready?.let { AppUpdateChecker.installApk(context, it.file) }
                },
                onOpenReleasePage = {
                    AppUpdateChecker.resetDownload()
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl)))
                    onDismissUpdateDialog()
                },
            )
        }
    }

    if (showLyricsSources) {
        BackHandler(onBack = onDismissLyricsSources)
        LyricsSourcesDialog(hazeState = hazeState, onDismiss = onDismissLyricsSources)
    }

    if (songSortMenuOpen) {
        BackHandler(onBack = onDismissSongSortMenu)
        FrostedSortMenu(
            hazeState = hazeState,
            selected = songSort,
            onSelect = { option ->
                detailBrowseId?.let { AppSettings.setDetailSongSort(it, option) }
                onDismissSongSortMenu()
            },
            onFlipDateDirection = {
                detailBrowseId?.let { browseId ->
                    val next = when (songSort) {
                        SongSort.DATE_ADDED_DESC -> SongSort.DATE_ADDED_ASC
                        SongSort.DATE_ADDED_ASC -> SongSort.DATE_ADDED_DESC
                        else -> SongSort.DATE_ADDED_DESC
                    }
                    AppSettings.setDetailSongSort(browseId, next)
                }
            },
            onDismiss = onDismissSongSortMenu,
        )
    }

    if (showAccountSelector) {
        BackHandler(onBack = onDismissAccountSelector)
    }

    AnimatedVisibility(
        visible = showAccountSelector,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(150)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.48f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissAccountSelector,
                ),
        )
    }

    AnimatedVisibility(
        visible = showAccountSelector,
        enter = fadeIn(animationSpec = tween(180)) +
            scaleIn(
                initialScale = 0.75f,
                transformOrigin = TransformOrigin(0.88f, 0.05f),
                animationSpec = tween(250, easing = FastOutSlowInEasing),
            ),
        exit = fadeOut(animationSpec = tween(150)) +
            scaleOut(
                targetScale = 0.80f,
                transformOrigin = TransformOrigin(0.88f, 0.05f),
                animationSpec = tween(180, easing = FastOutLinearInEasing),
            ),
    ) {
        AccountProfileSelector(
            accounts = googleAccounts,
            activeAccountId = activeAccountId,
            activeProfileId = activeProfileId,
            hazeState = hazeState,
            onSelect = { selected, profile -> viewModel.selectProfile(selected.accountId, profile.profileId) },
            onAddAccount = {
                onDismissAccountSelector()
                onWebSessionChange(WebSessionMode.SIGN_IN)
            },
            onRemoveAccount = { selected ->
                viewModel.removeAccount(selected.accountId)
                onDismissAccountSelector()
            },
            onOpenSettings = {
                onDismissAccountSelector()
                onOpenSettings()
            },
            onDismiss = onDismissAccountSelector,
        )
    }

    if (showAppLanguage) {
        BackHandler(onBack = onDismissAppLanguage)
        AppLanguageDialog(hazeState = hazeState, onDismiss = onDismissAppLanguage)
    }

    if (showTranslationLanguage) {
        BackHandler(onBack = onDismissTranslationLanguage)
        TranslationLanguageDialog(hazeState = hazeState, onDismiss = onDismissTranslationLanguage)
    }

    if (showListenBrainzLogin) {
        var tokenInput by remember { mutableStateOf(listenBrainzToken) }
        ListenBrainzTokenAlert(
            hazeState = hazeState,
            tokenInput = tokenInput,
            onTokenInputChange = { tokenInput = it },
            onSave = {
                AppSettings.setListenBrainzToken(tokenInput.trim())
                onDismissListenBrainzLogin()
            },
            onDismiss = onDismissListenBrainzLogin,
        )
    }

    if (showLastfmLogin) {
        var usernameInput by remember { mutableStateOf("") }
        var passwordInput by remember { mutableStateOf("") }
        var lastfmError by remember { mutableStateOf<String?>(null) }
        var lastfmLoading by remember { mutableStateOf(false) }
        LastfmLoginAlert(
            hazeState = hazeState,
            usernameInput = usernameInput,
            onUsernameInputChange = { usernameInput = it },
            passwordInput = passwordInput,
            onPasswordInputChange = { passwordInput = it },
            error = lastfmError,
            loading = lastfmLoading,
            onSignIn = {
                lastfmLoading = true
                lastfmError = null
                scope.launch {
                    try {
                        LastFM.initialize(
                            apiKey = AppSettings.lastfmApiKey.value,
                            secret = AppSettings.lastfmSecret.value,
                        )
                        LastFM.getMobileSession(usernameInput.trim(), passwordInput)
                            .onSuccess { auth ->
                                AppSettings.setLastfmSessionKey(auth.session.key)
                                AppSettings.setLastfmUsername(auth.session.name)
                                AppSettings.setLastfmEnabled(true)
                                onDismissLastfmLogin()
                            }
                            .onFailure { e ->
                                lastfmError = e.message ?: context.getString(R.string.login_failed)
                            }
                    } catch (e: Exception) {
                        lastfmError = e.message ?: context.getString(R.string.login_failed)
                    } finally {
                        lastfmLoading = false
                    }
                }
            },
            onDismiss = { if (!lastfmLoading) onDismissLastfmLogin() },
        )
    }

    if (showWebDavEditor) {
        BackHandler(onBack = onDismissWebDavEditor)
        ServerEditorHost(
            hazeState = hazeState,
            title = stringResource(R.string.webdav),
            description = stringResource(R.string.webdav_description),
            fields = listOf(
                FieldConfig(
                    initial = AppSettings.webdavUrl.value,
                    placeholder = stringResource(R.string.webdav_server_url_hint),
                    keyboardType = KeyboardType.Uri,
                ),
                FieldConfig(
                    initial = AppSettings.webdavUsername.value,
                    placeholder = stringResource(R.string.username),
                ),
                FieldConfig(
                    initial = AppSettings.webdavPassword.value,
                    placeholder = stringResource(R.string.password),
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                ),
            ),
            canSubmit = { it[0].isNotBlank() },
            testFailedRes = R.string.webdav_test_failed,
            onTest = { (url, username, password) ->
                com.music.bitchord.data.webdav.WebDavRepository.testConnection(
                    url.trim(),
                    username.trim(),
                    password,
                )
            },
            onSave = { (url, username, password) ->
                AppSettings.setWebDavUrl(url.trim())
                AppSettings.setWebDavUsername(username.trim())
                AppSettings.setWebDavPassword(password)
                onDismissWebDavEditor()
            },
            onDismiss = onDismissWebDavEditor,
        )
    }

    if (showSmbEditor) {
        BackHandler(onBack = onDismissSmbEditor)
        ServerEditorHost(
            hazeState = hazeState,
            title = stringResource(R.string.smb),
            description = stringResource(R.string.smb_description),
            fields = listOf(
                FieldConfig(
                    initial = AppSettings.smbHost.value,
                    placeholder = stringResource(R.string.smb_server_hint),
                    keyboardType = KeyboardType.Uri,
                ),
                FieldConfig(
                    initial = AppSettings.smbShare.value,
                    placeholder = stringResource(R.string.smb_share_hint),
                ),
                FieldConfig(
                    initial = AppSettings.smbBasePath.value,
                    placeholder = stringResource(R.string.smb_folder_hint),
                ),
                FieldConfig(
                    initial = AppSettings.smbUsername.value,
                    placeholder = stringResource(R.string.username),
                ),
                FieldConfig(
                    initial = AppSettings.smbPassword.value,
                    placeholder = stringResource(R.string.password),
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                ),
            ),
            canSubmit = { it[0].isNotBlank() && it[1].isNotBlank() },
            testFailedRes = R.string.smb_test_failed,
            onTest = { (host, share, folder, username, password) ->
                com.music.bitchord.data.smb.SmbRepository.testConnection(
                    host.trim(),
                    share.trim(),
                    folder.trim(),
                    username.trim(),
                    password,
                )
            },
            onSave = { (host, share, folder, username, password) ->
                AppSettings.setSmbHost(host.trim())
                AppSettings.setSmbShare(share.trim())
                AppSettings.setSmbBasePath(folder.trim())
                AppSettings.setSmbUsername(username.trim())
                AppSettings.setSmbPassword(password)
                onDismissSmbEditor()
            },
            onDismiss = onDismissSmbEditor,
        )
    }

    val uploadConflict by WebDavUploads.conflict.collectAsStateWithLifecycle()
    uploadConflict?.let { req ->
        var applyToAll by remember(req) { mutableStateOf(false) }
        val answer: (WebDavUploads.Choice) -> Unit = { choice ->
            req.answer.complete(WebDavUploads.Resolution(choice, applyToAll))
        }
        BackHandler { answer(WebDavUploads.Choice.SKIP) }
        WebDavConflictAlert(
            hazeState = hazeState,
            fileName = req.fileName,
            showApplyToAll = req.remaining > 0,
            applyToAll = applyToAll,
            onApplyToAllChange = { applyToAll = it },
            onOverwrite = { answer(WebDavUploads.Choice.OVERWRITE) },
            onKeepBoth = { answer(WebDavUploads.Choice.KEEP_BOTH) },
            onSkip = { answer(WebDavUploads.Choice.SKIP) },
            onDismiss = { answer(WebDavUploads.Choice.SKIP) },
        )
    }

    if (showDiscordLogin) {
        BackHandler(onBack = onDismissDiscordLogin)
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismissDiscordLogin) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Text(
                        "Sign in to Discord",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                DiscordLoginScreen(
                    onTokenCaptured = { token ->
                        AppSettings.setDiscordToken(token)
                        onDismissDiscordLogin()
                    },
                )
            }
        }
    }

    if (showSpotifyLogin) {
        BackHandler(onBack = onDismissSpotifyLogin)
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismissSpotifyLogin) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Text(
                        stringResource(R.string.spotify_sign_in),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                SpotifyLoginScreen(
                    modifier = Modifier.navigationBarsPadding(),
                    onConnected = { token ->
                        AppSettings.setSpotifySpdcToken(token)
                        onDismissSpotifyLogin()
                    },
                )
            }
        }
    }

    if (showSpotifyCanvasAuth) {
        BackHandler(onBack = onDismissSpotifyCanvasAuth)
        SpotifyCanvasAuthScreen(onNavigateUp = onDismissSpotifyCanvasAuth)
    }

    discordDialog?.let { which ->
        DiscordDialogHost(
            which = which,
            hazeState = hazeState,
            onDismiss = onDismissDiscordDialog,
        )
    }

    if (editingPartyServer) {
        PartyServerEditor(
            hazeState = hazeState,
            onDismiss = onDismissPartyServer,
        )
    }

    editingSource?.let { config ->
        SourceEditorAlert(
            hazeState = hazeState,
            config = config,
            onDismiss = onDismissEditingSource,
            onSaved = onDismissEditingSource,
            onDelete = {
                SourceRegistry.remove(config.id)
                onDismissEditingSource()
            },
            scope = scope,
        )
    }

    if (confirmJioSaavn) {
        ConfirmationAlert(
            hazeState = hazeState,
            title = stringResource(R.string.enable_jiosaavn),
            description = stringResource(R.string.jiosaavn_mismatch_warning),
            confirmLabel = stringResource(R.string.enable_anyway),
            onConfirm = {
                SourceRegistry.configs.value
                    .firstOrNull { it.kind == SourceKind.JIOSAAVN }
                    ?.let { SourceRegistry.setEnabled(it.id, true) }
                onDismissConfirmJioSaavn()
            },
            onDismiss = onDismissConfirmJioSaavn,
        )
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
internal fun FrostedSortMenu(
    hazeState: HazeState,
    selected: SongSort,
    onSelect: (SongSort) -> Unit,
    onFlipDateDirection: () -> Unit,
    onDismiss: () -> Unit,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val shape = MaterialTheme.shapes.extraLarge
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = .48f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.TopEnd,
    ) {
        Surface(
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = shape,
            modifier = Modifier
                .padding(top = 56.dp, end = 20.dp)
                .width(IntrinsicSize.Max)
                .clip(shape)
                .then(
                    if (reduceDynamicBlur) {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    } else {
                        Modifier.optimizedHazeEffect(
                            state = hazeState,
                            style = HazeMaterials.thin(MaterialTheme.colorScheme.surface),
                        )
                    },
                )
                .clickable(onClick = {}),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                val dateActive = selected == SongSort.DATE_ADDED_ASC ||
                    selected == SongSort.DATE_ADDED_DESC
                val arrowRotation by animateFloatAsState(
                    targetValue = if (selected == SongSort.DATE_ADDED_ASC) 180f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "dateAddedArrow",
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button) { onFlipDateDirection() }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.sort_date_added_toggle),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (dateActive) {
                        Icon(
                            Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.rotate(arrowRotation),
                        )
                    }
                }
                SongSort.entries
                    .filter { it != SongSort.DATE_ADDED_ASC && it != SongSort.DATE_ADDED_DESC }
                    .forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .clickable(role = Role.Button) { onSelect(option) }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                option.localizedLabel(),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            if (option == selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
            }
        }
    }
}

@Composable
internal fun SongSort.localizedLabel(): String = when (this) {
    SongSort.DEFAULT -> stringResource(R.string.sort_default)
    SongSort.TITLE_ASC -> stringResource(R.string.sort_title_ascending)
    SongSort.TITLE_DESC -> stringResource(R.string.sort_title_descending)
    SongSort.DATE_ADDED_ASC -> stringResource(R.string.sort_date_added_oldest)
    SongSort.DATE_ADDED_DESC -> stringResource(R.string.sort_date_added)
}

