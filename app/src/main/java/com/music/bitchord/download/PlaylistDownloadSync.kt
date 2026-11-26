package com.music.bitchord.download

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal enum class PlaylistDownloadUpdate { UPDATED, UP_TO_DATE, WAIT_FOR_NETWORK, WRONG_PROFILE, STORAGE_PERMISSION, FAILED }

/** Refreshes opted-in download records while the Android activity is in use. */
internal class PlaylistDownloadSync(
    private val context: Context,
    private val scope: CoroutineScope,
    private val activeProfile: () -> PlaylistDownloadProfile,
) {
    private val _refreshing = MutableStateFlow<Set<String>>(emptySet())
    val refreshing = _refreshing.asStateFlow()
    private val lastCheck = mutableMapOf<Pair<PlaylistDownloadProfile, String>, Long>()
    @Volatile private var foreground = false

    fun setForeground(value: Boolean) {
        foreground = value
        if (value) refreshEnabled()
    }

    fun refreshEnabled() {
        scope.launch {
            Downloads.awaitIndexReady()
            val profile = activeProfile()
            for (record in Downloads.collections.value.values) {
                if (!foreground || profile != activeProfile()) break
                if (record.syncProfile == profile) update(record.id, manual = false)
            }
        }
    }

    fun refresh(browseId: String, report: ((PlaylistDownloadUpdate) -> Unit)? = null) {
        scope.launch {
            Downloads.awaitIndexReady()
            val result = update(browseId, manual = report != null)
            if (result != null) report?.invoke(result)
        }
    }

    private suspend fun update(browseId: String, manual: Boolean): PlaylistDownloadUpdate? {
        val record = Downloads.collectionFor(browseId) ?: return null
        if (!record.playlist || !isSyncableYouTubePlaylist(record.id)) return null
        val profile = activeProfile()
        if (!manual && record.syncProfile != profile) return null
        if (record.syncProfile != null && record.syncProfile != profile) return PlaylistDownloadUpdate.WRONG_PROFILE
        if (!foreground || AppSettings.meteredConnection.value == null || !AppSettings.downloadsAllowedNow) {
            return PlaylistDownloadUpdate.WAIT_FOR_NETWORK
        }
        if ((AppSettings.exportDownloads.value || (AppSettings.saveToCustomDownloadLocation.value && !DownloadStore.usesCustomSafTree())) &&
            DownloadStore.needsLegacyPermission() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) return PlaylistDownloadUpdate.STORAGE_PERMISSION

        val id = record.id
        val key = profile to id
        val now = SystemClock.elapsedRealtime()
        if (id in _refreshing.value || (!manual && lastCheck[key]?.let { now - it < REFRESH_INTERVAL_MS } == true)) return null
        _refreshing.value += id
        lastCheck[key] = now
        val canApply = { foreground && profile == activeProfile() && AppSettings.downloadsAllowedNow }
        try {
            val tracks = YtMusicRepository.completePlaylistTracks("VL${id.removePrefix("VL")}", canApply)
                .getOrNull() ?: return PlaylistDownloadUpdate.FAILED
            if (!Downloads.updatePlaylistCollection(id, profile, tracks, canApply)) return PlaylistDownloadUpdate.FAILED
            Downloads.prunePlaylistDownloads(context, id, canApply)
            if (!canApply()) return PlaylistDownloadUpdate.FAILED
            val missing = tracks.distinctBy { it.videoId }.filter { Downloads.savedUri(context, it.videoId) == null }
            if (!canApply() || Downloads.collectionFor(id)?.syncProfile != profile) return PlaylistDownloadUpdate.FAILED
            for (track in missing) {
                val song = if (track.thumbnailUrl == null) track.copy(thumbnailUrl = record.thumbnailUrl) else track
                Downloads.enqueue(context, song, record.title, managedPlaylist = true)
            }
            Downloads.markRequested(id, missing.map { it.videoId })
            return if (missing.isNotEmpty() || tracks.map { it.videoId } != record.videoIds) {
                PlaylistDownloadUpdate.UPDATED
            } else PlaylistDownloadUpdate.UP_TO_DATE
        } finally {
            _refreshing.value -= id
        }
    }

    private companion object { const val REFRESH_INTERVAL_MS = 5 * 60 * 1000L }
}
