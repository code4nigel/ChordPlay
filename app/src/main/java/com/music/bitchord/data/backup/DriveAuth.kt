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

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.music.bitchord.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Permission to keep one hidden app folder (`drive.appdata`) in the listener's Google Drive
 * via Google Play Services Identity AuthorizationClient.
 */
object DriveAuth {

    private const val SCOPE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
    private const val ACCOUNT_TYPE = "com.google"

    sealed interface Outcome {
        data class Granted(val accessToken: String) : Outcome
        data class NeedsConsent(val intent: PendingIntent) : Outcome
    }

    fun available(context: Context): Boolean =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    suspend fun authorize(context: Context, account: String?): Outcome {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(SCOPE_APPDATA)))
            .apply { if (account != null) setAccount(Account(account, ACCOUNT_TYPE)) }
            .build()
        val result = Identity.getAuthorizationClient(context).authorize(request).await()
        return result.toOutcome()
    }

    fun fromIntent(context: Context, data: Intent?): Outcome {
        val result = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)
        return result.toOutcome()
    }

    suspend fun clearToken(context: Context, token: String) = withContext(Dispatchers.IO) {
        runCatching { GoogleAuthUtil.clearToken(context, token) }
        Unit
    }

    suspend fun revoke(context: Context, account: String) = withContext(Dispatchers.IO) {
        runCatching {
            val token = (authorize(context, account) as? Outcome.Granted)?.accessToken ?: return@runCatching
            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/revoke")
                .post(FormBody.Builder().add("token", token).build())
                .build()
            Http.client.newCall(request).execute().close()
            GoogleAuthUtil.clearToken(context, token)
        }
        Unit
    }

    private fun AuthorizationResult.toOutcome(): Outcome {
        val pending = pendingIntent
        if (hasResolution() && pending != null) return Outcome.NeedsConsent(pending)
        val token = accessToken ?: error("Google didn't return an access token")
        return Outcome.Granted(token)
    }
}

internal suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
