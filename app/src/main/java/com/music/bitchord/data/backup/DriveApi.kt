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

import com.music.bitchord.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Plain OkHttp REST client against Google Drive v3 `appDataFolder`.
 */
object DriveApi {

    private const val MIME = "application/gzip"
    private const val API = "https://www.googleapis.com/drive/v3"
    private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
    private const val FIELDS = "id,name,md5Checksum,modifiedTime,size,appProperties"

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonType: MediaType = "application/json; charset=UTF-8".toMediaType()

    private val client: OkHttpClient by lazy {
        Http.client.newBuilder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    class Unauthorized : IOException("Google Drive refused the sign-in")

    class DriveError(val code: Int, message: String) : IOException(message)

    data class DriveFile(
        val id: String,
        val name: String,
        val md5: String?,
        val modifiedAt: String,
        val sizeBytes: Long,
        val properties: Map<String, String>,
    )

    data class Profile(val email: String?, val name: String?)

    suspend fun profile(token: String): Profile = withContext(Dispatchers.IO) {
        val url = "$API/about".toHttpUrl().newBuilder()
            .addQueryParameter("fields", "user(emailAddress,displayName)")
            .build()
        val body = execute(Request.Builder().url(url).authorized(token).build()).use { it.bodyJson() }
        val user = body["user"]?.jsonObject
        Profile(
            email = user?.get("emailAddress")?.jsonPrimitive?.contentOrNull,
            name = user?.get("displayName")?.jsonPrimitive?.contentOrNull,
        )
    }

    suspend fun listAll(token: String): List<DriveFile> = withContext(Dispatchers.IO) {
        val files = mutableListOf<DriveFile>()
        var page: String? = null
        do {
            ensureActive()
            val url = "$API/files".toHttpUrl().newBuilder()
                .addQueryParameter("spaces", "appDataFolder")
                .addQueryParameter("q", "trashed = false")
                .addQueryParameter("fields", "nextPageToken,files($FIELDS)")
                .addQueryParameter("pageSize", "1000")
                .apply { page?.let { addQueryParameter("pageToken", it) } }
                .build()
            val body = execute(Request.Builder().url(url).authorized(token).build()).use { it.bodyJson() }
            body["files"]?.jsonArray?.mapNotNullTo(files) { it.jsonObject.toDriveFile() }
            page = body["nextPageToken"]?.jsonPrimitive?.contentOrNull
        } while (page != null)
        files
    }

    suspend fun put(
        token: String,
        name: String,
        bytes: ByteArray,
        properties: Map<String, String>,
        existingId: String?,
    ): DriveFile = withContext(Dispatchers.IO) {
        val metadata = buildJsonObject {
            put("name", name)
            put("mimeType", MIME)
            put("appProperties", buildJsonObject { properties.forEach { (k, v) -> put(k, v) } })
            if (existingId == null) put("parents", buildJsonArray { add(JsonPrimitive("appDataFolder")) })
        }.toString().toRequestBody(jsonType)
        val body = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(metadata)
            .addPart(bytes.toRequestBody(MIME.toMediaType()))
            .build()
        val base = if (existingId == null) "$UPLOAD/files" else "$UPLOAD/files/$existingId"
        val url = base.toHttpUrl().newBuilder()
            .addQueryParameter("uploadType", "multipart")
            .addQueryParameter("fields", FIELDS)
            .build()
        val request = Request.Builder()
            .url(url)
            .authorized(token)
            .method(if (existingId == null) "POST" else "PATCH", body)
            .build()
        execute(request).use { it.bodyJson().toDriveFile() ?: error("Drive's reply had no file in it") }
    }

    suspend fun download(
        token: String,
        id: String,
        expectedBytes: Long = 0L,
        onProgress: (received: Long, total: Long) -> Unit = { _, _ -> },
    ): ByteArray = withContext(Dispatchers.IO) {
        val url = "$API/files/$id".toHttpUrl().newBuilder().addQueryParameter("alt", "media").build()
        execute(Request.Builder().url(url).authorized(token).build()).use { response ->
            val body = response.body
            val total = body.contentLength().takeIf { it > 0 } ?: expectedBytes
            val out = ByteArrayOutputStream(total.coerceIn(0, Int.MAX_VALUE.toLong()).toInt())
            var received = 0L
            onProgress(0, total)
            body.byteStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                    received += read
                    onProgress(received, total)
                }
            }
            out.toByteArray()
        }
    }

    suspend fun delete(token: String, id: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$API/files/$id").authorized(token).delete().build()
        val response = client.newCall(request).execute()
        response.use {
            when {
                it.isSuccessful || it.code == 404 -> Unit
                it.code == 401 -> throw Unauthorized()
                it.code in 500..599 -> throw IOException("Google Drive is having trouble (HTTP ${it.code})")
                else -> throw DriveError(it.code, it.errorMessage() ?: "Google Drive said no (HTTP ${it.code})")
            }
        }
    }

    private fun Request.Builder.authorized(token: String) = header("Authorization", "Bearer $token")

    private fun execute(request: Request): Response {
        val response = client.newCall(request).execute()
        if (response.isSuccessful) return response
        response.use {
            when (it.code) {
                401 -> throw Unauthorized()
                in 500..599 -> throw IOException("Google Drive is having trouble (HTTP ${it.code})")
                else -> throw DriveError(it.code, it.errorMessage() ?: "Google Drive said no (HTTP ${it.code})")
            }
        }
    }

    private fun Response.bodyJson(): JsonObject = json.parseToJsonElement(body.string()).jsonObject

    private fun Response.errorMessage(): String? = runCatching {
        bodyJson()["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
    }.getOrNull()

    private fun JsonObject.toDriveFile(): DriveFile? {
        val id = get("id")?.jsonPrimitive?.contentOrNull ?: return null
        return DriveFile(
            id = id,
            name = get("name")?.jsonPrimitive?.contentOrNull.orEmpty(),
            md5 = get("md5Checksum")?.jsonPrimitive?.contentOrNull,
            modifiedAt = get("modifiedTime")?.jsonPrimitive?.contentOrNull.orEmpty(),
            sizeBytes = get("size")?.jsonPrimitive?.longOrNull ?: 0L,
            properties = get("appProperties")?.jsonObject
                ?.mapNotNull { (k, v) -> v.jsonPrimitive.contentOrNull?.let { k to it } }
                ?.toMap()
                .orEmpty(),
        )
    }
}
