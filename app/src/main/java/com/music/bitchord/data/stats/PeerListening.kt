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

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.YearMonth

/**
 * Other devices' listening partitions cached from Google Drive so Replay merges them
 * read-only alongside this phone's own buckets.
 */
object PeerListening {

    @Serializable
    data class Peer(
        val deviceId: String,
        val name: String = "",
        val updatedAt: Long = 0L,
        val months: Map<String, String> = emptyMap(),
    )

    private const val DIRECTORY = "listening-peers"
    private const val INDEX = "index.json"
    private const val TAG = "ChordPlayPeers"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private lateinit var root: File
    private val lock = Any()

    fun init(context: Context) {
        root = File(context.filesDir, DIRECTORY)
    }

    private val ready: Boolean get() = this::root.isInitialized

    fun peers(): List<Peer> = synchronized(lock) {
        if (!ready) return emptyList()
        root.listFiles()?.filter { it.isDirectory }?.mapNotNull { readIndex(it) }.orEmpty()
    }

    fun months(): Set<YearMonth> = peers()
        .flatMap { it.months.keys }
        .mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }
        .toSet()

    fun buckets(month: String): List<StoredBucket> = synchronized(lock) {
        if (!ready) return emptyList()
        root.listFiles()?.filter { it.isDirectory }?.mapNotNull { dir ->
            val file = File(dir, "$month.json")
            if (!file.exists()) return@mapNotNull null
            runCatching { BucketMath.decode(file.readBytes()) }
                .onFailure { Log.w(TAG, "Discarding unreadable ${dir.name}/$month", it) }
                .getOrNull()
        }.orEmpty()
    }

    fun putMonth(deviceId: String, name: String, updatedAt: Long, month: String, bytes: ByteArray, md5: String) {
        synchronized(lock) {
            if (!ready) return
            val dir = dir(deviceId)
            val file = File(dir, "$month.json")
            val temporary = File(dir, "$month.json.tmp")
            temporary.writeBytes(bytes)
            if (!temporary.renameTo(file)) {
                file.delete()
                temporary.renameTo(file)
            }
            val index = readIndex(dir) ?: Peer(deviceId)
            writeIndex(dir, index.copy(name = name, updatedAt = updatedAt, months = index.months + (month to md5)))
        }
    }

    fun touch(deviceId: String, name: String, updatedAt: Long) {
        synchronized(lock) {
            if (!ready) return
            val dir = dir(deviceId)
            val index = readIndex(dir) ?: Peer(deviceId)
            writeIndex(dir, index.copy(name = name, updatedAt = updatedAt))
        }
    }

    fun removeMonth(deviceId: String, month: String) {
        synchronized(lock) {
            if (!ready) return
            val dir = File(root, deviceId)
            File(dir, "$month.json").delete()
            readIndex(dir)?.let { writeIndex(dir, it.copy(months = it.months - month)) }
        }
    }

    fun removeDevice(deviceId: String) {
        synchronized(lock) {
            if (ready) File(root, deviceId).deleteRecursively()
        }
    }

    fun clear() {
        synchronized(lock) {
            if (ready) root.deleteRecursively()
        }
    }

    private fun dir(deviceId: String): File {
        require(deviceId.isNotBlank() && deviceId.none { it == '/' || it == '\\' || it == '.' }) {
            "Not a device id: $deviceId"
        }
        return File(root, deviceId).apply { mkdirs() }
    }

    private fun readIndex(dir: File): Peer? {
        val file = File(dir, INDEX)
        if (!file.exists()) return null
        return runCatching { json.decodeFromString(Peer.serializer(), file.readText()) }.getOrNull()
    }

    private fun writeIndex(dir: File, peer: Peer) {
        File(dir, INDEX).writeText(json.encodeToString(Peer.serializer(), peer))
    }
}
