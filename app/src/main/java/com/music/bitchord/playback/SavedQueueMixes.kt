package com.music.bitchord.playback

import android.content.Context
import com.music.bitchord.auth.AuthStore
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.QueueTier
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.playback.smart.AutomixEdit
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

@Serializable
internal data class QueueMixProfile(val accountId: String, val profileId: String)

internal fun AuthStore.queueMixProfile(): QueueMixProfile? {
    if (!isSignedIn) return null
    val session = activeSession ?: return null
    val profile = activeProfileId?.takeIf { it.isNotBlank() } ?: session.activeProfileId
        ?: channelPageId ?: channelDataSyncId ?: "personal"
    return QueueMixProfile(session.accountId, profile)
}

internal class QueuePlaylistSnapshot(
    songs: List<Song>,
    automix: Boolean,
    tempoMatching: Boolean,
    edits: Map<Int, AutomixEdit>,
) {
    val skipped: List<Song>
    val upload: QueuePlaylistUpload

    init {
        val (included, unsupported) = songs.withIndex().partition { (_, song) ->
            !song.isLive && YOUTUBE_ID.matches(song.videoId)
        }
        skipped = unsupported.map { it.value }
        upload = QueuePlaylistUpload(
            videoIds = included.map { it.value.videoId },
            automix = automix,
            tempoMatching = tempoMatching,
            edits = included.zipWithNext().mapIndexedNotNull { index, (first, second) ->
                if (second.index != first.index + 1) return@mapIndexedNotNull null
                edits[first.index]?.let { index to it }
            }.toMap(),
        )
    }

    private companion object {
        val YOUTUBE_ID = Regex("[A-Za-z0-9_-]{11}")
    }
}

@Serializable
internal data class QueuePlaylistUpload(
    val videoIds: List<String>,
    val automix: Boolean,
    val tempoMatching: Boolean,
    val edits: Map<Int, AutomixEdit>,
) {

    /** Match uploaded occurrences in order, then bind edits to YouTube's durable row ids. */
    fun verifiedMix(profile: QueueMixProfile, playlistId: String, entries: List<Song>): SavedQueueMix? {
        if (entries.map { it.videoId } != videoIds) return null
        val rowIds = entries.map { it.setVideoId ?: return null }
        if (rowIds.toSet().size != rowIds.size) return null
        val joins = edits.mapNotNull { (index, edit) ->
            val first = rowIds.getOrNull(index) ?: return@mapNotNull null
            val second = rowIds.getOrNull(index + 1) ?: return@mapNotNull null
            SavedQueueJoin(first, second, videoIds[index], videoIds[index + 1], edit)
        }
        return SavedQueueMix(profile, playlistId, automix, tempoMatching, joins)
    }
}

@Serializable
internal data class PendingQueuePlaylist(
    val profile: QueueMixProfile,
    val playlistId: String,
    val title: String,
    val upload: QueuePlaylistUpload,
) {
    val browseId: String get() = "VL$playlistId"
}

@Serializable
internal data class SavedQueueJoin(
    val outgoing: String,
    val incoming: String,
    val outgoingVideo: String,
    val incomingVideo: String,
    val edit: AutomixEdit,
)

@Serializable
internal data class SavedQueueMix(
    val profile: QueueMixProfile,
    val playlistId: String,
    val automix: Boolean,
    val tempoMatching: Boolean,
    val joins: List<SavedQueueJoin>,
) {
    @Transient private val editsByPair = joins.associateBy { it.outgoing to it.incoming }

    fun editFor(outgoing: Song, incoming: Song): AutomixEdit? {
        if (outgoing.queueTier != QueueTier.CONTEXT || incoming.queueTier != QueueTier.CONTEXT ||
            outgoing.playbackSourceId?.removePrefix("VL") != playlistId ||
            incoming.playbackSourceId?.removePrefix("VL") != playlistId) return null
        val left = outgoing.setVideoId ?: return null
        val right = incoming.setVideoId ?: return null
        return editsByPair[left to right]?.takeIf {
            it.outgoingVideo == outgoing.videoId && it.incomingVideo == incoming.videoId
        }?.edit
    }
}

internal class SavedQueueMixes(context: Context) {
    private val prefs = context.getSharedPreferences("saved_queue_mixes", Context.MODE_PRIVATE)

    fun load(profile: QueueMixProfile, source: String?): SavedQueueMix? {
        val playlistId = source?.removePrefix("VL") ?: return null
        val raw = prefs.getString(key(profile, playlistId), null) ?: return null
        return runCatching { Json.decodeFromString<SavedQueueMix>(raw) }.getOrNull()
    }

    fun save(mix: SavedQueueMix): Boolean = prefs.edit()
        .putString(key(mix.profile, mix.playlistId), Json.encodeToString(mix))
        .remove(key(mix.profile, PENDING))
        .commit()

    fun pending(profile: QueueMixProfile): PendingQueuePlaylist? {
        val raw = prefs.getString(key(profile, PENDING), null) ?: return null
        return runCatching { Json.decodeFromString<PendingQueuePlaylist>(raw) }.getOrNull()
    }

    fun remember(pending: PendingQueuePlaylist): Boolean = prefs.edit()
        .putString(key(pending.profile, PENDING), Json.encodeToString(pending))
        .commit()

    fun finish(profile: QueueMixProfile) { prefs.edit().remove(key(profile, PENDING)).apply() }

    private fun key(profile: QueueMixProfile, playlistId: String) = "${profile.accountId}/${profile.profileId}/$playlistId"

    private companion object { const val PENDING = "pending" }
}

internal enum class QueueHistoryRange(val limit: Int) {
    LAST_3(3), LAST_5(5), LAST_10(10), ALL(Int.MAX_VALUE),
}

internal class QueuePlaylistCapture(
    queue: List<Song>,
    currentIndex: Int,
    private val automix: Boolean,
    private val tempoMatching: Boolean,
    private val edits: Map<Int, AutomixEdit>,
) {
    private val songs = if (currentIndex in queue.indices) queue.toList() else emptyList()
    val historySize = if (songs.isEmpty()) 0 else currentIndex

    fun historyCount(range: QueueHistoryRange?) = minOf(historySize, range?.limit ?: 0)

    fun select(range: QueueHistoryRange?): QueuePlaylistSnapshot {
        val start = historySize - historyCount(range)
        val chosenEdits = edits.filterKeys { it >= start }.mapKeys { it.key - start }
        return QueuePlaylistSnapshot(songs.drop(start), automix, tempoMatching, chosenEdits)
    }
}

internal fun snapshotQueuePlaylist(queue: List<Song>, currentIndex: Int): QueuePlaylistCapture =
    QueuePlaylistCapture(
        queue,
        currentIndex,
        AppSettings.activeSmartFadeEnabled.value,
        AppSettings.activeAutomixTempoMatching.value,
        AndroidAutomixStudio.session?.queueEdits(queue).orEmpty(),
    )
