/*
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

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.music.bitchord.ui.onboarding.OnboardingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.util.Locale

/**
 * Deterministic acoustic wave & color identity derived from a listener's name.
 * Every name produces a distinct visual signature (hue triad, harmonic frequencies,
 * wave intensity, orbital rings, and 11-bar spectrum fingerprint), akin to a QR
 * identity rendered in ChordPlay's visual language.
 */
@Immutable
data class WaveIdentitySignature(
    val code: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color,
    val waveCount: Int,
    val primaryFreq: Float,
    val secondaryFreq: Float,
    val amplitude: Float,
    val phaseSpeed: Float,
    val ringCount: Int,
    val spectrumBars: List<Float>,
) {
    companion object {
        private data class HarmoniousTriad(
            val primary: Color,
            val secondary: Color,
            val tertiary: Color,
        )

        /**
         * Curated ColorHunt-inspired triads designed to complement dark frosted glass,
         * white typography, and the ChordPlay emblem without clashing neon hues.
         */
        private val CURATED_PALETTES = listOf(
            // 1. Coral Blush & Ocean Slate (ColorHunt pastel coral / mist / slate)
            HarmoniousTriad(Color(0xFFEA7B7B), Color(0xFF7FA1C3), Color(0xFFF5D2D2)),
            // 2. Periwinkle & Peach Coral (Image 8 bottom-left)
            HarmoniousTriad(Color(0xFF8E9BFF), Color(0xFFFF6B7A), Color(0xFFBEE3DB)),
            // 3. Sage Leaf & Warm Sand (Image 8 center-top)
            HarmoniousTriad(Color(0xFF7CA982), Color(0xFFC9A690), Color(0xFFE0EEC6)),
            // 4. Terracotta & Teal Mist (Image 8 top-middle)
            HarmoniousTriad(Color(0xFFF29F8F), Color(0xFF5FA8A5), Color(0xFFFBE2B4)),
            // 5. Sunset Gold & Ocean Blue (Image 8 top-left)
            HarmoniousTriad(Color(0xFFF48436), Color(0xFF799EC0), Color(0xFFFAE29C)),
            // 6. Blush Rose & Sky Steel (Image 8 top-right)
            HarmoniousTriad(Color(0xFFE7A99A), Color(0xFF6B9AC4), Color(0xFFEDDEC9)),
            // 7. Velvet Rose & Honey Amber (Image 8 bottom-center)
            HarmoniousTriad(Color(0xFFD96B78), Color(0xFFF6C667), Color(0xFF9AC1B8)),
            // 8. Olive Moss & Golden Wheat (Image 8 center-right)
            HarmoniousTriad(Color(0xFFA3B170), Color(0xFFC9CC82), Color(0xFF6D7B55)),
            // 9. Warm Copper & Lagoon Teal (Image 8 center-left)
            HarmoniousTriad(Color(0xFFE08546), Color(0xFF9ED5DE), Color(0xFFC45C3E)),
            // 10. Soft Cherry & Vanilla Cream (Image 8 mid-right)
            HarmoniousTriad(Color(0xFFF49FB6), Color(0xFFF7CEB5), Color(0xFFD988A3)),
            // 11. Midnight Indigo & Mint Aurora
            HarmoniousTriad(Color(0xFF7B8CDE), Color(0xFF70D6B4), Color(0xFFB8B8FF)),
            // 12. Crimson Velvet & Champagne Gold
            HarmoniousTriad(Color(0xFFFB4B6E), Color(0xFFF3B778), Color(0xFF8C5383)),
            // 13. Arctic Cyan & Dusk Violet
            HarmoniousTriad(Color(0xFF62C8D8), Color(0xFF9A7BFF), Color(0xFF8EDCE6)),
            // 14. Apricot Sunset & Plum Wine
            HarmoniousTriad(Color(0xFFFF8C69), Color(0xFFB36A8E), Color(0xFFFFD194)),
            // 15. Botanical Jade & Mist Lavender
            HarmoniousTriad(Color(0xFF6EC4A8), Color(0xFFA799E2), Color(0xFFC2ECE1)),
            // 16. Deep Ruby & Rose Quartz
            HarmoniousTriad(Color(0xFFE84A70), Color(0xFFF29CB8), Color(0xFF7C5295)),
            // 17. Cobalt Breeze & Warm Amber
            HarmoniousTriad(Color(0xFF6495ED), Color(0xFFF4A261), Color(0xFFA8DADC)),
            // 18. Lavender Mist & Mauve Pink
            HarmoniousTriad(Color(0xFFB592E8), Color(0xFFE68BB2), Color(0xFF8DB4EA)),
            // 19. Emerald Tide & Seafoam
            HarmoniousTriad(Color(0xFF52B788), Color(0xFF74C69D), Color(0xFFD8F3DC)),
            // 20. Flamingo & Steel Denim
            HarmoniousTriad(Color(0xFFF07167), Color(0xFF6B8EAE), Color(0xFFFED9B7)),
            // 21. Iris Glow & Tangerine Mist
            HarmoniousTriad(Color(0xFF8378E8), Color(0xFFF79D65), Color(0xFFC1BAFA)),
            // 22. Mauve Berry & Soft Celadon
            HarmoniousTriad(Color(0xFFD8839B), Color(0xFF8AB6A9), Color(0xFFF1C0CF)),
            // 23. Peacock Blue &Warm Apricot
            HarmoniousTriad(Color(0xFF5390D9), Color(0xFFEFA87D), Color(0xFF80FFDB)),
            // 24. Saffron Amber & Dusty Teal
            HarmoniousTriad(Color(0xFFF6AE54), Color(0xFF5C9EAD), Color(0xFFFAE09A)),
            // 25. Lilac Cloud & Mint Fizz
            HarmoniousTriad(Color(0xFFC39BF4), Color(0xFF7AD9C0), Color(0xFFEAD4FC)),
            // 26. Crimson Coral & Horizon Blue
            HarmoniousTriad(Color(0xFFFF5E7E), Color(0xFF6DA6E2), Color(0xFFFFB3C1)),
            // 27. Forest Mint & Warm Linen
            HarmoniousTriad(Color(0xFF61A896), Color(0xFFDCC7AA), Color(0xFF8FD1BA)),
            // 28. Orchid Rose & Dusk Sapphire
            HarmoniousTriad(Color(0xFFDB76A8), Color(0xFF6B7FD7), Color(0xFFF5B7D6)),
            // 29. Amber Rose & Slate Aqua
            HarmoniousTriad(Color(0xFFE88D67), Color(0xFF6B9F9F), Color(0xFFF3C6A5)),
            // 30. Glacial Blue & Pastel Mint
            HarmoniousTriad(Color(0xFF73B5E8), Color(0xFF8CE0CE), Color(0xFFC4E1FA)),
            // 31. Velvet Magenta & Sky Mist
            HarmoniousTriad(Color(0xFFDC5E96), Color(0xFF7CA7E8), Color(0xFFF0A5C8)),
            // 32. Warm Ochre & Moss Teal
            HarmoniousTriad(Color(0xFFE6A15C), Color(0xFF609686), Color(0xFFF2D2A9)),
        )

        private val DefaultSignature = WaveIdentitySignature(
            code = "CP-0000",
            primaryColor = Color(0xFFFB4B6E),
            secondaryColor = Color(0xFF7B8CDE),
            tertiaryColor = Color(0xFF70D6B4),
            waveCount = 2,
            primaryFreq = 2.6f,
            secondaryFreq = 1.9f,
            amplitude = 0.72f,
            phaseSpeed = 0.85f,
            ringCount = 3,
            spectrumBars = listOf(0.36f, 0.58f, 0.84f, 0.52f, 0.76f, 0.96f, 0.66f, 0.44f, 0.72f, 0.50f, 0.30f),
        )

        fun fromName(name: String): WaveIdentitySignature {
            val normalized = name.trim().lowercase(Locale.ROOT)
            if (normalized.isEmpty()) return DefaultSignature

            val digest = MessageDigest.getInstance("SHA-256")
                .digest(normalized.toByteArray(Charsets.UTF_8))

            fun byteAt(index: Int): Int = digest[index % digest.size].toInt() and 0xFF
            fun unit(index: Int): Float = byteAt(index) / 255f

            val code = String.format(Locale.US, "CP-%02X%02X", byteAt(0), byteAt(1))
            val paletteIdx = ((byteAt(2) shl 8) or byteAt(3)) % CURATED_PALETTES.size
            val chosen = CURATED_PALETTES[paletteIdx]

            return WaveIdentitySignature(
                code = code,
                primaryColor = chosen.primary,
                secondaryColor = chosen.secondary,
                tertiaryColor = chosen.tertiary,
                waveCount = 2 + (byteAt(12) % 3),
                primaryFreq = 2.2f + unit(13) * 2.8f,
                secondaryFreq = 1.6f + unit(14) * 2.6f,
                amplitude = 0.68f + unit(15) * 0.56f,
                phaseSpeed = 0.78f + unit(16) * 0.78f,
                ringCount = 3 + (byteAt(17) % 3),
                spectrumBars = List(11) { idx ->
                    0.24f + unit(18 + idx) * 0.76f
                },
            )
        }
    }
}

/**
 * Local portable identity file saved under `Downloads/Music/ChordPlay/you/`.
 * Bundles the listener's name, deterministic wave code, [AppSettings], and
 * [ListeningStats] so reinstalling or opening on this device restores their full setup.
 */
object ChordPlayIdentityStore {

    const val RELATIVE_SUBDIR = "Music/ChordPlay/you"
    const val DISPLAY_FOLDER = "Downloads/Music/ChordPlay/you"
    private const val IDENTITY_TAG = "chordplay_identity"
    private const val SCHEMA_VERSION = 1
    private const val MIME_JSON = "application/json"
    private const val MIME_PNG = "image/png"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    @Serializable
    data class ChordPlayIdentityFile(
        val app: String = IDENTITY_TAG,
        val version: Int = SCHEMA_VERSION,
        val identityCode: String,
        val listenerName: String = "",
        val updatedAt: String = "",
        val backup: Backup.BackupFile,
    )

    data class DiscoveredIdentity(
        val name: String,
        val code: String,
        val updatedAt: String,
        val settingsCount: Int,
        val monthsCount: Int,
        val uri: Uri,
    )

    data class RestoredIdentitySummary(
        val name: String,
        val code: String,
        val months: Int,
        val settings: Int,
    )

    internal fun encodeIdentity(file: ChordPlayIdentityFile): String =
        json.encodeToString(ChordPlayIdentityFile.serializer(), file)

    internal fun decodeIdentity(text: String): ChordPlayIdentityFile? =
        runCatching {
            val parsed = json.decodeFromString(ChordPlayIdentityFile.serializer(), text)
            parsed.takeIf { it.app == IDENTITY_TAG && it.version <= SCHEMA_VERSION }
        }.getOrNull()

    internal fun fileNameForName(name: String): String {
        val slug = slugForName(name)
        return if (slug.isNotEmpty()) "$slug.chordplay.json" else "you.chordplay.json"
    }

    internal fun pngFileNameForName(name: String): String {
        val slug = slugForName(name)
        return if (slug.isNotEmpty()) "$slug.chordplay.png" else "you.chordplay.png"
    }

    private fun slugForName(name: String): String =
        name.trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_-]+"), "-")
            .trim('-')
            .take(36)

    internal fun decodeIdentityBytes(bytes: ByteArray): ChordPlayIdentityFile? {
        val text = if (ChordPlayPassPng.isPng(bytes)) {
            ChordPlayPassPng.extractPayload(bytes) ?: return null
        } else {
            bytes.decodeToString()
        }
        return decodeIdentity(text)
    }

    /**
     * Saves the listener's current identity, settings, and listening stats to
     * `Downloads/Music/ChordPlay/you/` as both a `.chordplay.png` Pass card image
     * (embedding the lossless gzipped identity payload) and `.chordplay.json`.
     */
    suspend fun saveIdentity(context: Context, name: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = name.trim()
            val sig = WaveIdentitySignature.fromName(cleanName)
            val payload = ChordPlayIdentityFile(
                identityCode = sig.code,
                listenerName = cleanName,
                updatedAt = Instant.now().toString(),
                backup = Backup.createSnapshot(),
            )
            val jsonText = encodeIdentity(payload)
            val jsonBytes = jsonText.toByteArray(Charsets.UTF_8)
            val pngBytes = ChordPlayPassPng.encodePassPng(cleanName, sig, jsonText)

            val jsonFileName = fileNameForName(cleanName)
            val pngFileName = pngFileNameForName(cleanName)

            writeBytesToDownloads(context, jsonFileName, MIME_JSON, jsonBytes)
            writeBytesToDownloads(context, pngFileName, MIME_PNG, pngBytes)
            "$DISPLAY_FOLDER/$pngFileName"
        }
    }

    /**
     * Saves only the Wave Identity Pass PNG card image (`<slug>.chordplay.png`) carrying
     * the complete lossless settings & stats payload in its `zTXt` chunk.
     */
    suspend fun savePassImage(context: Context, name: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = name.trim()
            val sig = WaveIdentitySignature.fromName(cleanName)
            val payload = ChordPlayIdentityFile(
                identityCode = sig.code,
                listenerName = cleanName,
                updatedAt = Instant.now().toString(),
                backup = Backup.createSnapshot(),
            )
            val pngBytes = ChordPlayPassPng.encodePassPng(cleanName, sig, encodeIdentity(payload))
            val pngFileName = pngFileNameForName(cleanName)
            writeBytesToDownloads(context, pngFileName, MIME_PNG, pngBytes)
            "$DISPLAY_FOLDER/$pngFileName"
        }
    }

    private fun writeBytesToDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val savedViaMediaStore = runCatching {
                saveViaMediaStore(context, fileName, mimeType, bytes)
            }.getOrDefault(false)
            if (!savedViaMediaStore) {
                saveViaDirectFile(fileName, bytes)
            }
        } else {
            saveViaDirectFile(fileName, bytes)
        }
    }

    /**
     * Scans `Downloads/Music/ChordPlay/you` for readable identity files (`.json` or `.png` Pass cards)
     * so onboarding can offer one-tap restoration.
     */
    suspend fun discoverLocalIdentities(context: Context): List<DiscoveredIdentity> =
        withContext(Dispatchers.IO) {
            val found = mutableListOf<DiscoveredIdentity>()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching {
                    queryMediaStoreIdentities(context, found)
                }
            }

            runCatching {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    RELATIVE_SUBDIR,
                )
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles { file ->
                        file.isFile && (
                            file.name.endsWith(".json", ignoreCase = true) ||
                                file.name.endsWith(".chordplay.png", ignoreCase = true) ||
                                file.name.endsWith(".png", ignoreCase = true)
                            )
                    }?.forEach { file ->
                        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return@forEach
                        decodeIdentityBytes(bytes)?.let { parsed ->
                            found.add(
                                DiscoveredIdentity(
                                    name = parsed.listenerName.ifBlank { parsed.identityCode },
                                    code = parsed.identityCode,
                                    updatedAt = parsed.updatedAt,
                                    settingsCount = parsed.backup.settings.size,
                                    monthsCount = parsed.backup.listening.size,
                                    uri = Uri.fromFile(file),
                                ),
                            )
                        }
                    }
                }
            }

            found
                .distinctBy { it.code to it.name.lowercase(Locale.ROOT) }
                .sortedByDescending { it.updatedAt }
        }

    /**
     * Restores a ChordPlay Pass PNG card image, `.chordplay.json` identity file, or standard
     * ChordPlay/BitChord JSON backup from [uri].
     */
    suspend fun restoreFromUri(context: Context, uri: Uri): Result<RestoredIdentitySummary> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = readUriBytes(context, uri) ?: error("Couldn't open identity file")
                val identity = if (ChordPlayPassPng.isPng(bytes)) {
                    val embeddedJson = ChordPlayPassPng.extractPayload(bytes)
                        ?: error("No ChordPlay Pass data found in this image (compressed photos strip Pass metadata)")
                    decodeIdentity(embeddedJson) ?: error("Unreadable ChordPlay Pass data in image")
                } else {
                    decodeIdentity(bytes.decodeToString())
                }
                if (identity != null) {
                    val summary = Backup.applySnapshot(identity.backup)
                    if (identity.listenerName.isNotBlank()) {
                        OnboardingState.saveName(context, identity.listenerName)
                    }
                    return@runCatching RestoredIdentitySummary(
                        name = identity.listenerName.ifBlank { identity.identityCode },
                        code = identity.identityCode,
                        months = summary.months,
                        settings = summary.settings,
                    )
                }

                // Fallback: allow restoring a standard ChordPlay JSON backup too
                val summary = Backup.importFrom(context, uri).getOrThrow()
                val existingName = OnboardingState.name(context)
                val sig = WaveIdentitySignature.fromName(existingName)
                RestoredIdentitySummary(
                    name = existingName.ifBlank { sig.code },
                    code = sig.code,
                    months = summary.months,
                    settings = summary.settings,
                )
            }
        }

    private fun readUriBytes(context: Context, uri: Uri): ByteArray? {
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            return runCatching { File(path).readBytes() }.getOrNull()
        }
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveViaMediaStore(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ): Boolean {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val targetRelPath = "${Environment.DIRECTORY_DOWNLOADS}/$RELATIVE_SUBDIR/"

        val existingUri = resolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
            arrayOf(fileName, "%$RELATIVE_SUBDIR%"),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                ContentUris.withAppendedId(collection, id)
            } else {
                null
            }
        }

        if (existingUri != null) {
            val updated = runCatching {
                resolver.openOutputStream(existingUri, "wt")?.use { out ->
                    out.write(bytes)
                    true
                } ?: false
            }.getOrDefault(false)
            if (updated) return true
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, targetRelPath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val insertedUri = resolver.insert(collection, values) ?: return false
        val wrote = runCatching {
            resolver.openOutputStream(insertedUri, "wt")?.use { out ->
                out.write(bytes)
                true
            } ?: false
        }.getOrDefault(false)

        if (wrote) {
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(insertedUri, done, null, null)
            return true
        } else {
            runCatching { resolver.delete(insertedUri, null, null) }
            return false
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun queryMediaStoreIdentities(context: Context, out: MutableList<DiscoveredIdentity>) {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        resolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME),
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND (${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? OR ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?)",
            arrayOf("%$RELATIVE_SUBDIR%", "%.json", "%.png"),
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val bytes = readUriBytes(context, uri) ?: continue
                decodeIdentityBytes(bytes)?.let { parsed ->
                    out.add(
                        DiscoveredIdentity(
                            name = parsed.listenerName.ifBlank { parsed.identityCode },
                            code = parsed.identityCode,
                            updatedAt = parsed.updatedAt,
                            settingsCount = parsed.backup.settings.size,
                            monthsCount = parsed.backup.listening.size,
                            uri = uri,
                        ),
                    )
                }
            }
        }
    }

    private fun saveViaDirectFile(fileName: String, bytes: ByteArray) {
        @Suppress("DEPRECATION")
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            RELATIVE_SUBDIR,
        )
        if (!dir.exists() && !dir.mkdirs()) {
            error("Could not create $DISPLAY_FOLDER")
        }
        File(dir, fileName).writeBytes(bytes)
    }
}
