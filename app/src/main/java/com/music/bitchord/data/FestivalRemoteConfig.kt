package com.music.bitchord.data

import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.utils.FestivalTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
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

            val isActive = root["active"]?.jsonPrimitive?.booleanOrNull
                ?: root["diwali_active"]?.jsonPrimitive?.booleanOrNull
                ?: false

            val festivalId = root["festival_id"]?.jsonPrimitive?.contentOrNull ?: "diwali_2026"
            val themeType = root["theme_type"]?.jsonPrimitive?.contentOrNull ?: "diwali"
            val rawDecorationUrl = root["logo_decoration_url"]?.jsonPrimitive?.contentOrNull
            val quote = root["quote"]?.jsonPrimitive?.contentOrNull ?: ""

            // Security check: only allow HTTPS URLs from trusted domains
            val sanitizedDecorationUrl = rawDecorationUrl?.takeIf { url ->
                url.startsWith("https://", ignoreCase = true) &&
                    (url.contains("githubusercontent.com/code4nigel/", ignoreCase = true) ||
                        url.contains("github.com/code4nigel/", ignoreCase = true))
            }

            // Persist to local disk so next cold launch starts with correct state in 0ms
            AppSettings.setFestivalConfig(
                active = isActive,
                id = festivalId,
                themeType = themeType,
                logoDecorationUrl = sanitizedDecorationUrl,
                quote = quote,
            )
        }
    }
}
