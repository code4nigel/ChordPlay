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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders a portable ChordPlay Wave Identity Pass card to PNG and embeds/extracts the
 * full gzipped [ChordPlayIdentityStore.ChordPlayIdentityFile] JSON payload inside a
 * standard lossless PNG `zTXt` metadata chunk (`"ChordPlayPass"`).
 *
 * Because PNG chunks carry no fixed 2.9 KB QR size cap, the image stays visually
 * clean and crisp while holding complete settings and multi-month listening history.
 */
object ChordPlayPassPng {

    const val CHUNK_KEYWORD = "ChordPlayPass"
    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )
    private const val MAX_DECOMPRESSED_BYTES = 16 * 1024 * 1024

    fun isPng(bytes: ByteArray): Boolean =
        bytes.size >= PNG_SIGNATURE.size &&
            PNG_SIGNATURE.indices.all { i -> bytes[i] == PNG_SIGNATURE[i] }

    /**
     * Renders [signature] and [listenerName] into a high-resolution card bitmap and
     * returns the PNG byte array carrying [payloadJson] in a `zTXt` chunk.
     */
    fun encodePassPng(
        listenerName: String,
        signature: WaveIdentitySignature,
        payloadJson: String,
    ): ByteArray {
        val bitmap = renderPassBitmap(listenerName, signature)
        val rawPng = try {
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
        return injectZtxtChunk(rawPng, payloadJson)
    }

    /**
     * Renders the listener's deterministic Wave Identity Pass card artwork.
     */
    fun renderPassBitmap(
        listenerName: String,
        signature: WaveIdentitySignature,
        widthPx: Int = 1080,
        heightPx: Int = 680,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()

        val c1 = signature.primaryColor.toArgb()
        val c2 = signature.secondaryColor.toArgb()
        val c3 = signature.tertiaryColor.toArgb()

        // Base deep glass obsidian backdrop
        val bgRect = RectF(0f, 0f, w, h)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                w,
                h,
                intArrayOf(0xFF0B0C12.toInt(), 0xFF141724.toInt(), 0xFF0A0B10.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(bgRect, 48f, 48f, bgPaint)

        // Three soft atmospheric radial aura glows from the curated triad
        fun drawAura(cx: Float, cy: Float, radius: Float, argb: Int, alpha: Int) {
            val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx,
                    cy,
                    radius,
                    intArrayOf(withAlpha(argb, alpha), Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawCircle(cx, cy, radius, auraPaint)
        }
        drawAura(w * 0.80f, h * 0.24f, w * 0.52f, c1, 118)
        drawAura(w * 0.20f, h * 0.82f, w * 0.46f, c2, 92)
        drawAura(w * 0.54f, h * 0.56f, w * 0.36f, c3, 56)

        // Orbital resonance rings
        val orbitCx = w * 0.80f
        val orbitCy = h * 0.36f
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
        for (i in 0 until signature.ringCount) {
            val radius = (h * 0.14f) + i * (h * 0.085f)
            ringPaint.color = withAlpha(if (i % 2 == 0) c1 else c2, (56 - i * 10).coerceAtLeast(18))
            ringPaint.strokeWidth = 2.4f
            canvas.drawCircle(orbitCx, orbitCy, radius, ringPaint)
        }
        val dotAngle = 0.72
        val dotRadius = (h * 0.14f) + (signature.ringCount - 1) * (h * 0.085f)
        val dotX = orbitCx + cos(dotAngle).toFloat() * dotRadius
        val dotY = orbitCy + sin(dotAngle).toFloat() * dotRadius
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c3 }
        canvas.drawCircle(dotX, dotY, 7f, dotPaint)

        // Harmonic wave contours across bottom half
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val steps = 160
        for (layer in 0 until signature.waveCount) {
            val path = Path()
            val layerColor = when (layer % 3) {
                0 -> c1
                1 -> c2
                else -> c3
            }
            val freq = signature.primaryFreq + layer * 0.65f
            val baseY = h * (0.58f + layer * 0.08f)
            val amp = h * 0.065f * signature.amplitude * (1f - layer * 0.16f)
            for (s in 0..steps) {
                val t = s / steps.toFloat()
                val x = t * w
                val env = sin(t * PI).toFloat()
                val y = baseY + sin(t * freq * PI * 2.0 + layer * 1.1).toFloat() * amp * env
                if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            wavePaint.color = withAlpha(layerColor, 165 - layer * 28)
            wavePaint.strokeWidth = (4.5f - layer * 0.8f).coerceAtLeast(2f)
            canvas.drawPath(path, wavePaint)
        }

        // Outer hairline border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = withAlpha(0xFFFFFFFF.toInt(), 46)
        }
        canvas.drawRoundRect(RectF(2f, 2f, w - 2f, h - 2f), 48f, 48f, borderPaint)

        // Header pill: "CHORDPLAY PASS"
        val badgeRect = RectF(64f, 58f, 344f, 112f)
        val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(0xFFFFFFFF.toInt(), 30) }
        val badgeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = withAlpha(c1, 140)
        }
        canvas.drawRoundRect(badgeRect, 27f, 27f, badgeBg)
        canvas.drawRoundRect(badgeRect, 27f, 27f, badgeStroke)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 23f
            letterSpacing = 0.12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("CHORDPLAY PASS", 90f, 93f, labelPaint)

        // Identity Code (CP-XXXX)
        val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = c3
            textSize = 30f
            letterSpacing = 0.08f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText(signature.code, 68f, 168f, codePaint)

        // Listener Name
        val displayName = listenerName.trim().ifBlank { "Music Lover" }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = if (displayName.length > 18) 54f else 68f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(displayName, 66f, 278f, namePaint)

        // Subtitle + restore note
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.WHITE, 185)
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText("Unique Harmonic Signature", 66f, 326f, subPaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.WHITE, 148)
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText("Import this image in ChordPlay to restore settings & stats", 66f, h - 62f, footerPaint)

        // 11-bar spectrum fingerprint at bottom-right
        val barWidth = 14f
        val barGap = 10f
        val barBaseY = h - 56f
        val startX = w - 66f - (signature.spectrumBars.size * (barWidth + barGap) - barGap)
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        signature.spectrumBars.forEachIndexed { idx, level ->
            val bh = (22f + level * 92f)
            val bx = startX + idx * (barWidth + barGap)
            barPaint.color = when {
                idx < 4 -> c1
                idx < 8 -> c2
                else -> c3
            }
            barPaint.alpha = 215
            canvas.drawRoundRect(
                RectF(bx, barBaseY - bh, bx + barWidth, barBaseY),
                barWidth / 2f,
                barWidth / 2f,
                barPaint,
            )
        }

        return bitmap
    }

    /**
     * Inserts a zlib-compressed `zTXt` chunk carrying [payloadJson] into [pngBytes]
     * just before the `IEND` chunk, replacing any previous chunk for [keyword].
     */
    fun injectZtxtChunk(
        pngBytes: ByteArray,
        payloadJson: String,
        keyword: String = CHUNK_KEYWORD,
    ): ByteArray {
        require(isPng(pngBytes)) { "Not a valid PNG byte stream" }

        val chunkData = buildZtxtData(keyword, payloadJson)
        val out = ByteArrayOutputStream(pngBytes.size + chunkData.size + 16)
        out.write(PNG_SIGNATURE)

        var pos = PNG_SIGNATURE.size
        var inserted = false
        while (pos + 12 <= pngBytes.size) {
            val length = readIntBE(pngBytes, pos)
            if (length < 0 || pos + 12 + length > pngBytes.size) {
                error("Malformed PNG chunk at offset $pos")
            }
            val typeBytes = pngBytes.copyOfRange(pos + 4, pos + 8)
            val type = typeBytes.decodeToString()
            val dataStart = pos + 8
            val dataEnd = dataStart + length
            val totalChunkLen = 12 + length

            if (type == "zTXt" && matchesKeyword(pngBytes, dataStart, dataEnd, keyword)) {
                pos += totalChunkLen
                continue
            }

            if (type == "IEND" && !inserted) {
                writeChunk(out, "zTXt".toByteArray(Charsets.ISO_8859_1), chunkData)
                inserted = true
            }

            out.write(pngBytes, pos, totalChunkLen)
            pos += totalChunkLen
        }

        if (!inserted) {
            writeChunk(out, "zTXt".toByteArray(Charsets.ISO_8859_1), chunkData)
        }
        return out.toByteArray()
    }

    /**
     * Extracts and inflates the UTF-8 payload stored in the matching `zTXt` chunk,
     * or returns `null` if the PNG has no embedded ChordPlay Pass chunk or fails CRC.
     */
    fun extractPayload(
        pngBytes: ByteArray,
        keyword: String = CHUNK_KEYWORD,
    ): String? {
        if (!isPng(pngBytes)) return null

        var pos = PNG_SIGNATURE.size
        while (pos + 12 <= pngBytes.size) {
            val length = readIntBE(pngBytes, pos)
            if (length < 0 || pos + 12 + length > pngBytes.size) return null

            val typeStart = pos + 4
            val dataStart = pos + 8
            val dataEnd = dataStart + length
            val type = pngBytes.copyOfRange(typeStart, dataStart).decodeToString()

            if (type == "zTXt" && matchesKeyword(pngBytes, dataStart, dataEnd, keyword)) {
                val expectedCrc = readIntBE(pngBytes, dataEnd)
                val actualCrc = crc32(pngBytes, typeStart, 4 + length)
                if (expectedCrc != actualCrc) return null

                val sepIdx = findZeroByte(pngBytes, dataStart, dataEnd)
                if (sepIdx < 0 || sepIdx + 2 > dataEnd) return null
                val compressionMethod = pngBytes[sepIdx + 1].toInt() and 0xFF
                if (compressionMethod != 0) return null

                val compressed = pngBytes.copyOfRange(sepIdx + 2, dataEnd)
                return inflateUtf8(compressed)
            }

            pos += 12 + length
            if (type == "IEND") break
        }
        return null
    }

    private fun buildZtxtData(keyword: String, payloadJson: String): ByteArray {
        val kwBytes = keyword.toByteArray(Charsets.ISO_8859_1)
        val raw = payloadJson.toByteArray(Charsets.UTF_8)
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(raw)
        deflater.finish()
        val deflated = ByteArrayOutputStream(raw.size / 2 + 64)
        val buf = ByteArray(4096)
        while (!deflater.finished()) {
            val n = deflater.deflate(buf)
            if (n <= 0) break
            deflated.write(buf, 0, n)
        }
        deflater.end()

        val compressed = deflated.toByteArray()
        return ByteArray(kwBytes.size + 2 + compressed.size).apply {
            System.arraycopy(kwBytes, 0, this, 0, kwBytes.size)
            this[kwBytes.size] = 0 // Null separator
            this[kwBytes.size + 1] = 0 // Compression method: zlib deflate
            System.arraycopy(compressed, 0, this, kwBytes.size + 2, compressed.size)
        }
    }

    private fun inflateUtf8(compressed: ByteArray): String? {
        val inflater = Inflater()
        return try {
            inflater.setInput(compressed)
            val out = ByteArrayOutputStream(compressed.size * 2)
            val buf = ByteArray(4096)
            var total = 0
            while (!inflater.finished()) {
                val n = inflater.inflate(buf)
                if (n == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) break
                } else {
                    total += n
                    if (total > MAX_DECOMPRESSED_BYTES) return null
                    out.write(buf, 0, n)
                }
            }
            out.toByteArray().toString(Charsets.UTF_8)
        } catch (_: Exception) {
            null
        } finally {
            inflater.end()
        }
    }

    private fun matchesKeyword(
        bytes: ByteArray,
        start: Int,
        end: Int,
        keyword: String,
    ): Boolean {
        val sepIdx = findZeroByte(bytes, start, end)
        if (sepIdx <= start) return false
        val found = bytes.copyOfRange(start, sepIdx).toString(Charsets.ISO_8859_1)
        return found == keyword
    }

    private fun findZeroByte(bytes: ByteArray, start: Int, end: Int): Int {
        for (i in start until end) {
            if (bytes[i] == 0.toByte()) return i
        }
        return -1
    }

    private fun writeChunk(out: ByteArrayOutputStream, type: ByteArray, data: ByteArray) {
        out.write(intBE(data.size))
        out.write(type)
        out.write(data)
        val crc = CRC32().apply {
            update(type)
            update(data)
        }.value.toInt()
        out.write(intBE(crc))
    }

    private fun crc32(bytes: ByteArray, offset: Int, len: Int): Int =
        CRC32().apply { update(bytes, offset, len) }.value.toInt()

    private fun readIntBE(bytes: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.BIG_ENDIAN).int

    private fun intBE(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array()

    private fun withAlpha(argb: Int, alpha: Int): Int =
        (argb and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)
}
