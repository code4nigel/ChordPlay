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
package com.music.bitchord

import android.app.Application
import androidx.annotation.OptIn
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.allowHardware
import coil3.request.crossfade
import com.music.bitchord.auth.AuthStore
import com.music.bitchord.data.DebugLog
import com.music.bitchord.data.lyrics.LyricsTranslation
import androidx.appcompat.app.AppCompatDelegate
import com.music.bitchord.ui.player.AndroidPlayerHost
import com.music.bitchord.ui.player.PlayerPlatform
import com.music.bitchord.data.canvas.CanvasCache
import com.music.bitchord.data.smb.SmbCoverFetcher
import com.music.bitchord.data.webdav.WebDavCoilAuth
import com.music.bitchord.data.canvas.SpotifyToken
import com.music.bitchord.data.spotify.SpotifyLibrary
import com.music.bitchord.playback.AudioCache
import com.music.bitchord.playback.LastPlayed
import com.music.bitchord.playback.OriginalVersion
import com.music.bitchord.data.innertube.Innertube
import com.music.bitchord.data.innertube.AndroidStreamHooks
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.melody.MelodyStore
import com.music.bitchord.data.scrobbling.LastFM
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.SearchHistory
import com.music.bitchord.data.sources.SourceRegistry
import com.music.bitchord.data.stats.ArtistFacts
import com.music.bitchord.data.stats.ListeningStats
import com.music.bitchord.download.Downloads
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import kotlin.concurrent.thread

class BitChordApplication : Application(), SingletonImageLoader.Factory {

    @Volatile
    private var imageLoaderBuilt = false

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        // Preload the startup SharedPreferences files off the main thread while
        // the early hooks and AuthStore open run.
        prefetchPreferences()
        val authStoreOpen = FutureTask { AuthStore(this) }
        thread(name = "auth-store-open", block = authStoreOpen::run)
        // The player is drawn by the shared UI module; this is what it reads
        // underneath — settings, the Canvas decoder, outputs, the party.
        AndroidStreamHooks.installEarly()
        PlayerPlatform.install(AndroidPlayerHost(this))
        com.music.bitchord.ui.AppUi.install(com.music.bitchord.ui.AndroidAppUiHost)
        registerBundledLyricsFonts()
        // The shared data layer (lyrics, the YouTube Music client) logs to
        // logcat on debug builds only, as the app's own DebugLog always has.
        if (BuildConfig.DEBUG) {
            DebugLog.sink = DebugLog.Sink { level, tag, message, error ->
                when (level) {
                    'D' -> android.util.Log.d(tag, message, error)
                    'I' -> android.util.Log.i(tag, message, error)
                    'W' -> android.util.Log.w(tag, message, error)
                    else -> android.util.Log.e(tag, message, error)
                }
            }
        } else {
            // R8 strips android.util.Log from a release build (proguard-rules.pro);
            // this stops Media3 composing the lines it would have handed it.
            androidx.media3.common.util.Log.setLogLevel(androidx.media3.common.util.Log.LOG_LEVEL_OFF)
            java.util.logging.LogManager.getLogManager().reset()
            java.util.logging.Logger.getLogger("").level = java.util.logging.Level.OFF
        }
        // Compose re-votes the window's frame rate from dispatchDraw on every
        // frame (API 35+), building stack traces on the main thread per frame.
        // The app manages its refresh rate cleanly via window attributes.
        @kotlin.OptIn(ExperimentalComposeUiApi::class)
        ComposeUiFlags.isAdaptiveRefreshRateEnabled = false
        // The Spotify library and import live in the shared module; the tokens are the WebView's.
        SpotifyLibrary.auth = SpotifyLibrary.Auth {
            SpotifyToken.accessToken()?.let { SpotifyLibrary.Tokens(it, SpotifyToken.clientToken()) }
        }
        SpotifyLibrary.isConnected = { com.music.bitchord.data.settings.AppSettings.spotifySpdcToken.value.isNotBlank() }
        // The per-app language picker, which YouTube Music's `hl` follows.
        // Full tag (zh-Hant / zh-Hans preserved); Innertube maps to hl/gl.
        Innertube.appLanguage = { AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag() }
        migrateScriptOnlyAppLocale()
        LyricsTranslation.cacheDir = cacheDir
        // PlaybackService shares this process, so seeding the cookie here means
        // stream resolution is authenticated from the first play onwards.
        authStore = try {
            authStoreOpen.get()
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
        // Opened off the main thread, alongside everything below: none of these
        // reads a setting or the session, and between them they are the slowest
        // opens at startup — SourceRegistry's encrypted store most of all.
        // Started only once [AuthStore] exists, because both encrypted stores
        // share one keystore master key and a first launch must not have two
        // threads racing to create it. Deliberately not joined on the main thread.
        thread(name = "startup-init") {
            SourceRegistry.init(this)
            AndroidStreamHooks.initInnerTubeX(this)
            // Its own directory: canvas clips are looping video, not audio, and
            // belong in a cache AudioCache's own limit and eviction policy were
            // never sized for. See CanvasCache's doc for why this one exists at
            // all — it is the fix for canvas clips re-fetching the same few
            // seconds of video from the network on every loop.
            CanvasCache.init(this)
        }
        // Migration-safe: an old single cookie becomes the first encrypted
        // session, while newer installs restore the profile the listener chose.
        val restoredSession = authStore.activeSession
        if (restoredSession != null && authStore.activeAccountId == null) {
            authStore.select(restoredSession.accountId, restoredSession.activeProfileId)
        }
        authStore.cookie = restoredSession?.cookie
        Innertube.cookie = restoredSession?.cookie
        // Which account that cookie actually acts as. Read here rather than on
        // demand so the answer is usually in hand before the first request needs
        // it: a play registered under the wrong account is indistinguishable, to
        // the listener, from one that was never registered at all. Fire and
        // forget — every caller works without it, just less precisely.
        if (restoredSession != null) {
            // After the cookie, never before: setting the cookie clears any
            // channel the last session was acting as, so restoring the choice
            // first would restore it into the value about to be wiped.
            restoredSession.profiles.firstOrNull { it.profileId == restoredSession.activeProfileId }
                ?.let { Innertube.selectChannel(it.pageId, it.dataSyncId, it.authUser) }
            CoroutineScope(Dispatchers.IO).launch { Innertube.ensureSessionScope() }
        }
        AppSettings.init(this, authStore)
        // Before anything resolves a track: an addon with `checkValidLossless`
        // is gated on this, and the gate reads "no" until it has looked.
        com.music.bitchord.playback.audio.LosslessOutput.init(this)
        // Restores a party this device is still a member of, so a process death
        // mid-session is something the rest of the party never sees. The socket
        // and the clock offset are not restored — both are re-established on
        // the next connect, which is the only way to be sure they are current.
        ListenTogether.init(this)
        SearchHistory.init(this)
        LastPlayed.init(this)
        com.music.bitchord.playback.PartyPersonalQueueStash.init(this)
        // Which tracks the listener has reverted to YouTube's own upload. Read
        // by [Song.toMediaItem], so it has to be open before the restart
        // snapshot below is turned back into queue items.
        OriginalVersion.init(this)
        // What's already saved to Downloads, so the song menu can say so
        // without a media-store query per row.
        Downloads.init(this)
        // The device's own listening record. Opened here rather than in
        // PlaybackService because the Replay page reads it from the UI side and
        // both live in this process — one owner, one directory.
        ListeningStats.init(this)
        com.music.bitchord.data.stats.PeerListening.init(this)
        com.music.bitchord.data.backup.DriveBackup.init(this)
        // After AppSettings, whose switch decides whether half of it runs.
        ArtistFacts.init(this)
        MelodyStore.init(this)
        // One cache directory can only be opened once per process, and
        // PlaybackService shares this one — so it's opened here, not there.
        AudioCache.init(this)
        // The offscreen WebView that mints a Spotify access token from the
        // listener's own session cookie needs a Context, and nothing in the
        // suspend call chain that reaches it (a track's canvas lookup) has
        // one to hand — see SpotifyToken's doc for why.
        SpotifyToken.init(this)
        // A sideloaded update is just a new APK over the old one, so app data —
        // including whatever the old build left in these caches — survives it
        // untouched. Wipe both on the first launch of a higher versionCode so a
        // format or key change between builds can't serve stale or mismatched
        // bytes from a cache the new code didn't write.
        if (AppSettings.consumeVersionUpdate(BuildConfig.VERSION_CODE)) {
            backgroundScope().launch {
                AudioCache.clear()
                SingletonImageLoader.get(this@BitChordApplication).let { loader ->
                    loader.memoryCache?.clear()
                    loader.diskCache?.clear()
                }
            }
        }
        // Initialize LastFM with saved settings if available
        initLastfm()
        // The public "apps open right now" count; see Presence.
        com.music.bitchord.data.presence.AndroidPresence.install(this)
        // The app's own caches let go when the system asks; see MemoryPressure.
        com.music.bitchord.util.MemoryPressure.install(this)
        val activityManager = getSystemService(ACTIVITY_SERVICE) as? android.app.ActivityManager
        if (activityManager?.isLowRamDevice != true) {
            com.music.bitchord.ui.components.backdrop.GlassShaderWarmup.warmUp()
        }
    }

    private fun registerBundledLyricsFonts() {
        val sfPro by lazy {
            androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.sf_pro_display_regular, androidx.compose.ui.text.font.FontWeight.Normal),
                androidx.compose.ui.text.font.Font(R.font.sf_pro_display_medium, androidx.compose.ui.text.font.FontWeight.Medium),
                androidx.compose.ui.text.font.Font(R.font.sf_pro_display_semibold, androidx.compose.ui.text.font.FontWeight.SemiBold),
                androidx.compose.ui.text.font.Font(R.font.sf_pro_display_bold, androidx.compose.ui.text.font.FontWeight.Bold),
                androidx.compose.ui.text.font.Font(R.font.sf_pro_display_heavy, androidx.compose.ui.text.font.FontWeight.ExtraBold),
            )
        }
        val poppins by lazy {
            androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.poppins_regular, androidx.compose.ui.text.font.FontWeight.Normal),
                androidx.compose.ui.text.font.Font(R.font.poppins_medium, androidx.compose.ui.text.font.FontWeight.Medium),
                androidx.compose.ui.text.font.Font(R.font.poppins_semibold, androidx.compose.ui.text.font.FontWeight.SemiBold),
                androidx.compose.ui.text.font.Font(R.font.poppins_bold, androidx.compose.ui.text.font.FontWeight.Bold),
                androidx.compose.ui.text.font.Font(R.font.poppins_extrabold, androidx.compose.ui.text.font.FontWeight.ExtraBold),
            )
        }
        val parkinsans by lazy {
            androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(R.font.parkinsans_medium, androidx.compose.ui.text.font.FontWeight.Medium),
                androidx.compose.ui.text.font.Font(R.font.parkinsans_semibold, androidx.compose.ui.text.font.FontWeight.SemiBold),
                androidx.compose.ui.text.font.Font(R.font.parkinsans_bold, androidx.compose.ui.text.font.FontWeight.Bold),
                androidx.compose.ui.text.font.Font(R.font.parkinsans_extrabold, androidx.compose.ui.text.font.FontWeight.ExtraBold),
            )
        }
        com.music.bitchord.ui.player.LyricsStyleManager.setBundledFontResolver { choice ->
            when (choice) {
                com.music.bitchord.ui.player.LyricsFontChoice.DEFAULT -> sfPro
                com.music.bitchord.ui.player.LyricsFontChoice.POPPINS -> poppins
                com.music.bitchord.ui.player.LyricsFontChoice.PARKINSANS -> parkinsans
                else -> androidx.compose.ui.text.font.FontFamily.Default
            }
        }
    }

    private fun prefetchPreferences() {
        getSharedPreferences("bitchord_auth", MODE_PRIVATE)
        getSharedPreferences("bitchord_auth_plain", MODE_PRIVATE)
        for (name in STARTUP_PREFERENCE_FILES) getSharedPreferences(name, MODE_PRIVATE)
    }

    private fun backgroundScope(): CoroutineScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, error ->
            DebugLog.w("BitChord", "background work failed: ${error.message}")
        },
    )

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (!imageLoaderBuilt) return
        val cache = SingletonImageLoader.get(this).memoryCache ?: return
        when {
            level >= TRIM_MEMORY_BACKGROUND -> cache.clear()
            level != TRIM_MEMORY_UI_HIDDEN && level >= TRIM_MEMORY_RUNNING_LOW -> {
                cache.trimToSize(cache.size / 2)
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (!imageLoaderBuilt) return
        SingletonImageLoader.get(this).memoryCache?.clear()
    }

    /**
     * Artwork loading, which was previously left entirely on Coil's defaults.
     *
     * The defaults aren't unreasonable, but the disk cache is sized at 2% of
     * free space — which on a full phone is the 10MB floor, a few screens of
     * covers, and covers are exactly the thing worth still having tomorrow.
     * Naming a directory alongside it keeps that cache somewhere identifiable
     * rather than in the process's temp dir.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        imageLoaderBuilt = true
        val lowRam = getSystemService(android.app.ActivityManager::class.java)
            ?.isLowRamDevice == true
        return ImageLoader.Builder(context)
            // Covers on the WebDAV server need the credential or every one
            // of them 401s — which reads as "this track has no artwork".
            // Coil's own transport never sees Http.client's interceptor, so
            // the header is attached per request instead. See WebDavCoilAuth.
            .components {
                add(WebDavCoilAuth())
                // Covers filed on the SMB share; anything else falls
                // through to Coil's own fetchers. See SmbCoverFetcher.
                add(SmbCoverFetcher.Factory())
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, if (lowRam) 0.15 else 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            // Covers arriving with a hard cut read as the list flickering as
            // it scrolls; a short fade reads as them developing.
            .crossfade(200)
            .allowHardware(true)
            .build()
    }

    private fun initLastfm() {
        val sessionKey = AppSettings.lastfmSessionKey.value
        if (sessionKey.isBlank()) return
        val endpoint = AppSettings.lastfmEndpoint.value.ifBlank { LastFM.DEFAULT_API_ENDPOINT }
        val apiKey = AppSettings.lastfmApiKey.value.trim()
        val secret = AppSettings.lastfmSecret.value.trim()
        if (apiKey.isBlank() || secret.isBlank()) return
        LastFM.configure(
            endpoint = endpoint,
            apiKey = apiKey,
            secret = secret,
            sessionKey = sessionKey,
        )
    }

    companion object {
        lateinit var authStore: AuthStore
            private set

        private val STARTUP_PREFERENCE_FILES = listOf(
            "bitchord_settings",
            "bitchord_local_playlists",
            "bitchord_listen_together",
            "bitchord_last_played",
            "bitchord_party_queue_stash",
            "bitchord_original_versions",
            "bitchord_downloads",
            "audio_cache_state",
            "presence",
            "bitchord_onboarding",
            "innertubex_player_config",
            MelodyStore.PREFS_FILE,
        )

        /**
         * One-time move from region-less script tags to regional ones.
         *
         * Early builds of the Simplified/Traditional picker stored `zh-Hant` /
         * `zh-Hans`. Android Views resolve those (script qualifiers), but the
         * shared Compose resources only match language + region — so Settings
         * read Traditional while the player read Simplified. Regional tags
         * match on both systems. Bare `zh` is left alone: it historically
         * meant Simplified.
         */
        private fun migrateScriptOnlyAppLocale() {
            val current = AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag()
            val replacement = when (current) {
                "zh-Hant" -> "zh-TW"
                "zh-Hans" -> "zh-CN"
                else -> return
            }
            AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags(replacement),
            )
        }
    }
}
