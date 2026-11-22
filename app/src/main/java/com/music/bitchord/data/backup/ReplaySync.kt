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
package com.music.bitchord.data.backup

import android.content.Context
import com.music.bitchord.BuildConfig
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.SearchHistory
import com.music.bitchord.data.stats.Backup
import com.music.bitchord.data.stats.BucketMath
import com.music.bitchord.data.stats.ListeningStats
import com.music.bitchord.data.stats.PeerListening
import com.music.bitchord.data.stats.StoredBucket
import com.music.bitchord.data.stats.WaveIdentitySignature
import com.music.bitchord.ui.onboarding.OnboardingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

data class DeviceIdentity(
    val installId: String,
    val deviceId: String? = null,
    val deviceName: String = "",
    val adoptedManifestAt: String? = null,
)

/**
 * One device's record in Google Drive, storing ChordPlay unique identity (`identityCode`,
 * `listenerName`), typed `AppSettings`, and month SHA-256 hashes.
 */
@Serializable
data class DeviceManifest(
    val schema: Int = SCHEMA,
    val deviceId: String,
    val name: String = "",
    val ownerInstall: String = "",
    val updatedAt: Long = 0L,
    val versionName: String = "",
    val identityCode: String = "",
    val listenerName: String = "",
    val months: Map<String, String> = emptyMap(),
    val settings: Map<String, Backup.PrefValue> = emptyMap(),
) {
    companion object {
        const val SCHEMA = 1
    }
}

data class RemoteDevice(
    val deviceId: String,
    val name: String,
    val updatedAt: Long,
    val months: Int,
    val sizeBytes: Long,
    val manifest: DeviceManifest,
)

internal object SyncPlanner {

    data class Push(val upload: List<String>, val delete: List<String>)

    fun push(local: Map<String, String>, synced: Map<String, String>, remote: Set<String>): Push = Push(
        upload = local.filter { (month, hash) -> synced[month] != hash || month !in remote }.keys.sorted(),
        delete = (remote - local.keys).sorted(),
    )

    data class Pull(val download: List<String>, val drop: List<String>)

    fun pull(listed: Set<String>, remoteMd5: Map<String, String?>, held: Map<String, String>): Pull {
        val available = listed.filter { it in remoteMd5 }.toSet()
        return Pull(
            download = available.filter { held[it] == null || held[it] != remoteMd5[it] }.sorted(),
            drop = (held.keys - available).sorted(),
        )
    }

    fun supersededBy(identity: DeviceIdentity, manifest: DeviceManifest?, manifestAt: String?): Boolean =
        manifest != null &&
            manifest.ownerInstall.isNotEmpty() &&
            manifest.ownerInstall != identity.installId &&
            manifestAt != identity.adoptedManifestAt
}

object ReplaySync {

    private const val KIND = "kind"
    private const val KIND_DEVICE = "device"
    private const val KIND_REPLAY = "replay"
    private const val DEVICE_ID = "deviceId"
    private const val MONTH = "month"
    private const val SYNCED_DIR = "listening-synced"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    sealed interface Step {
        data object Preparing : Step
        data class Uploading(val done: Int, val total: Int) : Step
        data class Downloading(val device: String, val done: Int, val total: Int) : Step
        data object Restoring : Step
    }

    data class Report(
        val identity: DeviceIdentity,
        val uploaded: Int,
        val downloaded: Int,
        val devices: List<RemoteDevice>,
        val forkedTo: String? = null,
    )

    private class Listing(files: List<DriveApi.DriveFile>) {
        val manifests: Map<String, DriveApi.DriveFile> = files
            .filter { it.properties[KIND] == KIND_DEVICE }
            .mapNotNull { file -> file.properties[DEVICE_ID]?.let { it to file } }
            .toMap()

        val months: Map<String, Map<String, DriveApi.DriveFile>> = files
            .filter { it.properties[KIND] == KIND_REPLAY }
            .groupBy { it.properties[DEVICE_ID].orEmpty() }
            .filterKeys { it.isNotEmpty() }
            .mapValues { (_, list) -> list.mapNotNull { f -> f.properties[MONTH]?.let { it to f } }.toMap() }
    }

    private suspend fun readManifest(token: String, file: DriveApi.DriveFile): DeviceManifest =
        json.decodeFromString(DeviceManifest.serializer(), gunzip(DriveApi.download(token, file.id)).decodeToString())

    suspend fun devices(token: String): List<RemoteDevice> {
        val listing = Listing(DriveApi.listAll(token))
        return devicesOf(token, listing)
    }

    private suspend fun devicesOf(token: String, listing: Listing): List<RemoteDevice> =
        listing.manifests.mapNotNull { (id, file) ->
            val manifest = runCatching { readManifest(token, file) }.getOrNull() ?: return@mapNotNull null
            val files = listing.months[id].orEmpty()
            RemoteDevice(
                deviceId = id,
                name = manifest.name,
                updatedAt = manifest.updatedAt,
                months = manifest.months.keys.count { it in files },
                sizeBytes = files.values.sumOf { it.sizeBytes } + file.sizeBytes,
                manifest = manifest,
            )
        }.sortedByDescending { it.updatedAt }

    suspend fun sync(
        context: Context,
        token: String,
        identity: DeviceIdentity,
        push: Boolean,
        onStep: (Step) -> Unit,
    ): Report = withContext(Dispatchers.IO) {
        onStep(Step.Preparing)
        var me = identity
        val deviceId = me.deviceId ?: error("Choose this phone's device first")
        var listing = Listing(DriveApi.listAll(token))
        var forkedTo: String? = null

        val ownManifestFile = listing.manifests[deviceId]
        val ownManifest = ownManifestFile?.let { runCatching { readManifest(token, it) }.getOrNull() }
        if (SyncPlanner.supersededBy(me, ownManifest, ownManifestFile?.modifiedAt)) {
            val unsynced = ListeningStats.exportAll().mapNotNull { month ->
                val before = synced(context, month.month)
                val left = if (before != null) BucketMath.minus(month, before) else month
                left.takeUnless(BucketMath::isEmpty)
            }
            ListeningStats.importAll(unsynced)
            clearSynced(context)
            val name = "${me.deviceName.ifBlank { DriveBackup.deviceName() }} (2)"
            me = me.copy(deviceId = UUID.randomUUID().toString(), deviceName = name, adoptedManifestAt = null)
            forkedTo = name
        }
        val myId = me.deviceId!!

        var uploaded = 0
        if (push) {
            val own = ListeningStats.exportAll()
            val bytes = own.associate { it.month to BucketMath.canonicalBytes(it) }
            val hashes = bytes.mapValues { BucketMath.hash(it.value) }
            val remoteOwn = listing.months[myId].orEmpty()
            val plan = SyncPlanner.push(
                local = hashes,
                synced = hashes.keys.mapNotNull { m -> syncedHash(context, m)?.let { m to it } }.toMap(),
                remote = remoteOwn.keys,
            )
            plan.upload.forEachIndexed { index, month ->
                onStep(Step.Uploading(index, plan.upload.size))
                DriveApi.put(
                    token = token,
                    name = "replay-$myId-$month.json.gz",
                    bytes = gzip(bytes.getValue(month)),
                    properties = mapOf(KIND to KIND_REPLAY, DEVICE_ID to myId, MONTH to month),
                    existingId = remoteOwn[month]?.id,
                )
                writeSynced(context, month, bytes.getValue(month))
                uploaded++
            }
            plan.delete.forEach { month ->
                remoteOwn[month]?.let { DriveApi.delete(token, it.id) }
                File(syncedDir(context), "$month.json").delete()
            }
            onStep(Step.Uploading(plan.upload.size, plan.upload.size))
            val listenerName = OnboardingState.name(context)
            val identityCode = WaveIdentitySignature.fromName(listenerName).code
            val manifest = DeviceManifest(
                deviceId = myId,
                name = me.deviceName,
                ownerInstall = me.installId,
                updatedAt = System.currentTimeMillis(),
                versionName = BuildConfig.VERSION_NAME,
                identityCode = identityCode,
                listenerName = listenerName,
                months = hashes,
                settings = AppSettings.exportPrefs().mapNotNull { (k, v) -> Backup.PrefValue.of(v)?.let { k to it } }.toMap(),
            )
            val written = DriveApi.put(
                token = token,
                name = "device-$myId.json.gz",
                bytes = gzip(json.encodeToString(DeviceManifest.serializer(), manifest).toByteArray()),
                properties = mapOf(KIND to KIND_DEVICE, DEVICE_ID to myId),
                existingId = listing.manifests[myId]?.id,
            )
            me = me.copy(adoptedManifestAt = written.modifiedAt)
            listing = Listing(DriveApi.listAll(token))
        }

        val devices = devicesOf(token, listing)
        val downloaded = pull(context, token, listing, devices, myId, onStep)
        Report(identity = me, uploaded = uploaded, downloaded = downloaded, devices = devices, forkedTo = forkedTo)
    }

    private suspend fun pull(
        context: Context,
        token: String,
        listing: Listing,
        devices: List<RemoteDevice>,
        myId: String,
        onStep: (Step) -> Unit,
    ): Int {
        var downloaded = 0
        var changed = false
        val held = PeerListening.peers().associateBy { it.deviceId }
        held.keys.filter { id -> id == myId || devices.none { it.deviceId == id } }.forEach {
            PeerListening.removeDevice(it)
            changed = true
        }
        devices.filter { it.deviceId != myId }.forEach { device ->
            val files = listing.months[device.deviceId].orEmpty()
            val plan = SyncPlanner.pull(
                listed = device.manifest.months.keys,
                remoteMd5 = files.mapValues { it.value.md5 },
                held = held[device.deviceId]?.months.orEmpty(),
            )
            PeerListening.touch(device.deviceId, device.name, device.updatedAt)
            plan.download.forEachIndexed { index, month ->
                onStep(Step.Downloading(device.name, index, plan.download.size))
                val file = files.getValue(month)
                val bytes = gunzip(DriveApi.download(token, file.id, file.sizeBytes))
                if (runCatching { BucketMath.decode(bytes) }.isSuccess) {
                    PeerListening.putMonth(device.deviceId, device.name, device.updatedAt, month, bytes, file.md5.orEmpty())
                    downloaded++
                    changed = true
                }
            }
            plan.drop.forEach {
                PeerListening.removeMonth(device.deviceId, it)
                changed = true
            }
        }
        if (changed) ListeningStats.peersChanged()
        return downloaded
    }

    suspend fun restoreAs(
        context: Context,
        token: String,
        identity: DeviceIdentity,
        device: RemoteDevice,
        keepLocal: Boolean,
        onStep: (Step) -> Unit,
    ): Pair<DeviceIdentity, Backup.Summary> = withContext(Dispatchers.IO) {
        val listing = Listing(DriveApi.listAll(token))
        val manifestFile = listing.manifests[device.deviceId] ?: error("That device is no longer in Google Drive")
        val manifest = readManifest(token, manifestFile)
        val files = listing.months[device.deviceId].orEmpty()
        val months = manifest.months.keys.filter { it in files }.sorted()

        val remote = months.mapIndexed { index, month ->
            onStep(Step.Downloading(manifest.name, index, months.size))
            val bytes = gunzip(DriveApi.download(token, files.getValue(month).id))
            month to BucketMath.decode(bytes)
        }.toMap()
        onStep(Step.Restoring)

        val local = if (keepLocal) ListeningStats.exportAll().associateBy { it.month } else emptyMap()
        val combined: List<StoredBucket> = (remote.keys + local.keys).sorted().map { month ->
            val a = remote[month]
            val b = local[month]
            when {
                a != null && b != null -> BucketMath.plus(a, BucketMath.minus(b, a))
                else -> a ?: b!!
            }
        }
        ListeningStats.importAll(combined)
        AppSettings.importPrefs(manifest.settings.mapValues { it.value.decoded() })
        if (manifest.listenerName.isNotBlank()) {
            OnboardingState.saveName(context, manifest.listenerName)
        }
        SearchHistory.reload()

        clearSynced(context)
        remote.forEach { (month, bucket) -> writeSynced(context, month, BucketMath.canonicalBytes(bucket)) }
        PeerListening.removeDevice(device.deviceId)
        ListeningStats.peersChanged()

        val adopted = identity.copy(
            deviceId = device.deviceId,
            deviceName = manifest.name,
            adoptedManifestAt = manifestFile.modifiedAt,
        )
        adopted to Backup.Summary(
            months = combined.size,
            settings = manifest.settings.size,
            from = manifest.versionName,
            at = manifest.updatedAt.toString(),
        )
    }

    suspend fun remove(token: String, device: RemoteDevice) {
        val listing = Listing(DriveApi.listAll(token))
        listing.months[device.deviceId].orEmpty().values.forEach { DriveApi.delete(token, it.id) }
        listing.manifests[device.deviceId]?.let { DriveApi.delete(token, it.id) }
        PeerListening.removeDevice(device.deviceId)
        ListeningStats.peersChanged()
    }

    fun clearSynced(context: Context) {
        syncedDir(context).deleteRecursively()
    }

    fun hasSynced(context: Context): Boolean = syncedDir(context).listFiles()?.isNotEmpty() == true

    private fun syncedDir(context: Context) = File(context.filesDir, SYNCED_DIR)

    private fun synced(context: Context, month: String): StoredBucket? =
        File(syncedDir(context), "$month.json").takeIf { it.exists() }
            ?.let { runCatching { BucketMath.decode(it.readBytes()) }.getOrNull() }

    private fun syncedHash(context: Context, month: String): String? =
        File(syncedDir(context), "$month.json").takeIf { it.exists() }?.let { BucketMath.hash(it.readBytes()) }

    private fun writeSynced(context: Context, month: String, bytes: ByteArray) {
        val dir = syncedDir(context).apply { mkdirs() }
        val temporary = File(dir, "$month.json.tmp")
        temporary.writeBytes(bytes)
        val file = File(dir, "$month.json")
        if (!temporary.renameTo(file)) {
            file.delete()
            temporary.renameTo(file)
        }
    }

    private fun gzip(bytes: ByteArray): ByteArray =
        ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(bytes) } }.toByteArray()

    private fun gunzip(bytes: ByteArray): ByteArray = GZIPInputStream(bytes.inputStream()).use { it.readBytes() }
}
