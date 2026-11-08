package com.music.bitchord.data

import com.music.bitchord.ui.utils.FestivalTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request

/**
 * Checks a remote configuration JSON on GitHub to dynamically trigger
 * seasonal festival themes, announcements, and popups without needing an APK update.
 */
object FestivalRemoteConfig {

    private const val CONFIG_URL =
        "https://raw.githubusercontent.com/code4nigel/ChordPlay/main/app/version/festivals.json"

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check() = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(CONFIG_URL).build()
            val body = Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()
            } ?: return@runCatching
            val root = json.parseToJsonElement(body) as? JsonObject ?: return@runCatching

            // Allow enabling/disabling Diwali remotely
            root["diwali_active"]?.jsonPrimitive?.booleanOrNull?.let { isEnabled ->
                FestivalTheme.forceDiwaliActive = isEnabled
            }
        }
    }
}

