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
package com.music.bitchord.data.stats

import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.Locale

/**
 * Arithmetic on months of listening, for moving partitions across devices cleanly
 * without double-counting plays.
 */
object BucketMath {

    private val canonical = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** [a] and [b] added together. Both must be the same month. */
    fun plus(a: StoredBucket, b: StoredBucket): StoredBucket {
        require(a.month == b.month) { "Can't add ${a.month} to ${b.month}" }
        val tracks = LinkedHashMap<String, TrackEntry>()
        (a.tracks + b.tracks).forEach { entry ->
            tracks.merge(entry.id, entry.deepCopy()) { x, y -> x.also { it.absorb(y) } }
        }
        val artists = LinkedHashMap<String, NameEntry>()
        (a.artists + b.artists).forEach { entry ->
            artists.merge(artistKey(entry), entry.deepCopy()) { x, y -> x.also { it.absorb(y) } }
        }
        val albums = LinkedHashMap<String, NameEntry>()
        (a.albums + b.albums).forEach { entry ->
            albums.merge(albumKey(entry), entry.deepCopy()) { x, y -> x.also { it.absorb(y) } }
        }
        val mergedDays = LinkedHashMap<Int, Long>(a.days)
        b.days.forEach { (day, ms) -> mergedDays[day] = (mergedDays[day] ?: 0L) + ms }
        return StoredBucket(
            version = maxOf(a.version, b.version),
            month = a.month,
            tracks = tracks.values.toList(),
            artists = artists.values.toList(),
            albums = albums.values.toList(),
            hours = List(24) { a.hours.getOrElse(it) { 0L } + b.hours.getOrElse(it) { 0L } },
            days = mergedDays,
        )
    }

    /**
     * What [a] has that [b] doesn't: every counter of [b] subtracted from [a], clamped to zero.
     */
    fun minus(a: StoredBucket, b: StoredBucket): StoredBucket {
        require(a.month == b.month) { "Can't subtract ${b.month} from ${a.month}" }
        val oldTracks = b.tracks.associateBy { it.id }
        val oldArtists = b.artists.associateBy(::artistKey)
        val oldAlbums = b.albums.associateBy(::albumKey)
        return StoredBucket(
            version = a.version,
            month = a.month,
            tracks = a.tracks.mapNotNull { entry ->
                val old = oldTracks[entry.id] ?: return@mapNotNull entry.deepCopy()
                entry.deepCopy().apply {
                    ms = (ms - old.ms).coerceAtLeast(0)
                    plays = (plays - old.plays).coerceAtLeast(0)
                    days = subtractDays(days, old.days)
                }.takeIf { it.ms > 0 || it.plays > 0 }
            },
            artists = a.artists.mapNotNull { it.minus(oldArtists[artistKey(it)]) },
            albums = a.albums.mapNotNull { it.minus(oldAlbums[albumKey(it)]) },
            hours = List(24) { (a.hours.getOrElse(it) { 0L } - b.hours.getOrElse(it) { 0L }).coerceAtLeast(0) },
            days = a.days.mapNotNull { (day, ms) ->
                val left = ms - (b.days[day] ?: 0L)
                if (left > 0) day to left else null
            }.toMap(),
        )
    }

    fun isEmpty(bucket: StoredBucket): Boolean =
        bucket.tracks.isEmpty() && bucket.hours.all { it == 0L } && bucket.days.values.all { it == 0L }

    fun canonicalBytes(bucket: StoredBucket): ByteArray =
        canonical.encodeToString(StoredBucket.serializer(), bucket).toByteArray()

    fun decode(bytes: ByteArray): StoredBucket =
        canonical.decodeFromString(StoredBucket.serializer(), bytes.decodeToString())

    fun hash(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun artistKey(entry: NameEntry): String =
        (ListeningStats.primaryArtist(entry.name) ?: entry.name).lowercase(Locale.ROOT)

    private fun albumKey(entry: NameEntry): String =
        entry.id ?: (entry.name.lowercase(Locale.ROOT) + SEPARATOR + entry.sub.orEmpty().lowercase(Locale.ROOT))

    private val SEPARATOR = Char(31).toString()

    private fun TrackEntry.deepCopy() = copy(
        days = days.mapValuesTo(mutableMapOf()) { it.value.copy() },
    )

    private fun NameEntry.deepCopy() = copy()

    private fun NameEntry.minus(old: NameEntry?): NameEntry? {
        if (old == null) return deepCopy()
        return deepCopy().apply {
            ms = (ms - old.ms).coerceAtLeast(0)
            plays = (plays - old.plays).coerceAtLeast(0)
        }.takeIf { it.ms > 0 || it.plays > 0 }
    }

    private fun subtractDays(
        from: MutableMap<String, DayStats>,
        take: Map<String, DayStats>,
    ): MutableMap<String, DayStats> {
        val out = mutableMapOf<String, DayStats>()
        from.forEach { (day, stats) ->
            val old = take[day]
            val left = DayStats(
                ms = (stats.ms - (old?.ms ?: 0L)).coerceAtLeast(0),
                plays = (stats.plays - (old?.plays ?: 0)).coerceAtLeast(0),
            )
            if (left.ms > 0 || left.plays > 0) out[day] = left
        }
        return out
    }
}
