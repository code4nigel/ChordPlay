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

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.music.bitchord.data.stats.Backup
import com.music.bitchord.data.stats.ListeningStats
import com.music.bitchord.data.stats.PeerListening
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class BackupFrequency(val days: Long) {
    OFF(0),
    DAILY(1),
    WEEKLY(7),
    MONTHLY(30),
}

object DriveBackup {

    data class Settings(
        val account: String? = null,
        val accountName: String? = null,
        val frequency: BackupFrequency = BackupFrequency.DAILY,
        val overCellular: Boolean = false,
        val lastSyncAt: Long = 0L,
        val lastError: String? = null,
        val needsSignIn: Boolean = false,
        val identity: DeviceIdentity = DeviceIdentity(installId = ""),
        val forkedTo: String? = null,
    ) {
        val needsDevice: Boolean get() = account != null && identity.deviceId == null
    }

    sealed interface Progress {
        data object Idle : Progress
        data class Running(val step: ReplaySync.Step) : Progress
    }

    sealed interface Devices {
        data object Unknown : Devices
        data object Checking : Devices
        data class Loaded(val list: List<RemoteDevice>) : Devices
        data class Failed(val message: String) : Devices
    }

    sealed interface Result {
        data class Synced(val uploaded: Int, val downloaded: Int) : Result
        data class Restored(val summary: Backup.Summary, val device: String) : Result
        data class Failed(val message: String, val restoring: Boolean) : Result
    }

    class NeedsSignIn : Exception("Sign in to Google Drive again to continue")

    private const val PREFS = "bitchord_drive_backup"
    private const val KEY_ACCOUNT = "account"
    private const val KEY_ACCOUNT_NAME = "account_name"
    private const val KEY_FREQUENCY = "frequency"
    private const val KEY_CELLULAR = "over_cellular"
    private const val KEY_LAST_SYNC = "last_sync_at"
    private const val KEY_LAST_PULL = "last_pull_at"
    private const val KEY_LAST_ERROR = "last_error"
    private const val KEY_NEEDS_SIGN_IN = "needs_sign_in"
    private const val KEY_INSTALL_ID = "install_id"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_DEVICE_NAME = "device_name"
    private const val KEY_ADOPTED_AT = "adopted_manifest_at"
    private const val KEY_FORKED_TO = "forked_to"

    private const val PERIODIC_WORK = "drive_sync"
    private const val CATCH_UP_WORK = "drive_sync_catch_up"
    internal const val KEY_PUSH = "push"

    private val CATCH_UP_MILLIS = TimeUnit.HOURS.toMillis(12)

    private lateinit var prefs: SharedPreferences
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val running = Mutex()

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _progress = MutableStateFlow<Progress>(Progress.Idle)
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    private val _devices = MutableStateFlow<Devices>(Devices.Unknown)
    val devices: StateFlow<Devices> = _devices.asStateFlow()

    private val _result = MutableStateFlow<Result?>(null)
    val result: StateFlow<Result?> = _result.asStateFlow()

    fun init(app: Application) {
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _settings.value = read()
        if (_settings.value.identity.installId.isEmpty()) {
            save { it.copy(identity = it.identity.copy(installId = UUID.randomUUID().toString())) }
        }
        scope.launch {
            var firstLook = true
            settings
                .map { Triple(it.account != null && !it.needsDevice, it.frequency, it.overCellular) }
                .distinctUntilChanged()
                .collect { (ready, frequency, cellular) ->
                    if (ready && frequency != BackupFrequency.OFF) {
                        schedule(app, frequency, cellular, replace = !firstLook)
                    } else if (!firstLook) {
                        WorkManager.getInstance(app).cancelUniqueWork(PERIODIC_WORK)
                    }
                    firstLook = false
                }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                val s = _settings.value
                if (s.account == null || s.needsDevice || s.needsSignIn) return
                if (System.currentTimeMillis() - prefs.getLong(KEY_LAST_PULL, 0L) < CATCH_UP_MILLIS) return
                val request = OneTimeWorkRequestBuilder<DriveBackupWorker>()
                    .setConstraints(constraints(s.overCellular))
                    .setInputData(workDataOf(KEY_PUSH to false))
                    .build()
                WorkManager.getInstance(app).enqueueUniqueWork(CATCH_UP_WORK, ExistingWorkPolicy.KEEP, request)
            }
        })
    }

    fun setFrequency(frequency: BackupFrequency) = save { it.copy(frequency = frequency) }

    fun setOverCellular(on: Boolean) = save { it.copy(overCellular = on) }

    fun rename(name: String) {
        val trimmed = name.trim().take(60)
        if (trimmed.isEmpty()) return
        save { it.copy(identity = it.identity.copy(deviceName = trimmed)) }
    }

    fun clearResult() {
        _result.value = null
    }

    fun dismissForkNotice() = save { it.copy(forkedTo = null) }

    suspend fun beginSignIn(context: Context): DriveAuth.Outcome =
        DriveAuth.authorize(context, _settings.value.account)

    suspend fun finishSignIn(context: Context, data: Intent?) {
        when (val outcome = DriveAuth.fromIntent(context, data)) {
            is DriveAuth.Outcome.Granted -> signedIn(context, outcome.accessToken)
            is DriveAuth.Outcome.NeedsConsent -> error("Google Drive access wasn't granted")
        }
    }

    suspend fun signedIn(context: Context, token: String) {
        val profile = DriveApi.profile(token)
        val email = profile.email ?: error("Couldn't tell which Google account that was")
        save {
            it.copy(
                account = email,
                accountName = profile.name,
                needsSignIn = false,
                lastError = if (it.needsSignIn) null else it.lastError,
            )
        }
        refreshDevices(context)
        val found = (_devices.value as? Devices.Loaded)?.list.orEmpty()
        if (_settings.value.needsDevice && found.isEmpty()) chooseNewDevice(context)
    }

    fun disconnect(context: Context) {
        val account = _settings.value.account ?: return
        save {
            Settings(
                frequency = it.frequency,
                overCellular = it.overCellular,
                identity = it.identity,
            )
        }
        _devices.value = Devices.Unknown
        PeerListening.clear()
        ListeningStats.peersChanged()
        scope.launch { DriveAuth.revoke(context.applicationContext, account) }
    }

    suspend fun refreshDevices(context: Context) {
        if (_settings.value.account == null) {
            _devices.value = Devices.Unknown
            return
        }
        _devices.value = Devices.Checking
        _devices.value = try {
            Devices.Loaded(withToken(context) { ReplaySync.devices(it) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Devices.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    fun chooseNewDevice(context: Context) {
        if (_settings.value.identity.deviceId != null) return
        save {
            it.copy(
                identity = it.identity.copy(
                    deviceId = UUID.randomUUID().toString(),
                    deviceName = it.identity.deviceName.ifBlank { deviceName() },
                    adoptedManifestAt = null,
                ),
            )
        }
        syncNow(context)
    }

    fun restoreAs(context: Context, device: RemoteDevice) {
        val app = context.applicationContext
        scope.launch {
            if (!running.tryLock()) return@launch
            var restored = false
            try {
                val keepLocal = _settings.value.identity.deviceId == null || !ReplaySync.hasSynced(app)
                val (identity, summary) = withToken(app) { token ->
                    ReplaySync.restoreAs(app, token, _settings.value.identity, device, keepLocal) { step ->
                        _progress.value = Progress.Running(step)
                    }
                }
                save { it.copy(identity = identity, forkedTo = null) }
                _result.value = Result.Restored(summary, device.name)
                restored = true
            } catch (e: CancellationException) {
                throw e
            } catch (_: NeedsSignIn) {
                // Handled by settings flags
            } catch (e: Exception) {
                _result.value = Result.Failed(e.message ?: e.javaClass.simpleName, restoring = true)
            } finally {
                _progress.value = Progress.Idle
                running.unlock()
            }
            if (restored) runSync(app, push = true)
        }
    }

    fun remove(context: Context, device: RemoteDevice) {
        val app = context.applicationContext
        val mine = device.deviceId == _settings.value.identity.deviceId
        scope.launch {
            if (!running.tryLock()) return@launch
            try {
                withToken(app) { ReplaySync.remove(it, device) }
                if (mine) {
                    ReplaySync.clearSynced(app)
                    save {
                        it.copy(
                            frequency = BackupFrequency.OFF,
                            lastSyncAt = 0L,
                            lastError = null,
                            identity = it.identity.copy(adoptedManifestAt = null),
                        )
                    }
                }
                refreshDevices(app)
            } catch (e: CancellationException) {
                throw e
            } catch (_: NeedsSignIn) {
            } catch (e: Exception) {
                _result.value = Result.Failed(e.message ?: e.javaClass.simpleName, restoring = false)
            } finally {
                running.unlock()
            }
        }
    }

    fun syncNow(context: Context) {
        val app = context.applicationContext
        scope.launch {
            _result.value = when (val outcome = runSync(app, push = true)) {
                is RunOutcome.Done -> Result.Synced(outcome.uploaded, outcome.downloaded)
                is RunOutcome.Failed -> Result.Failed(outcome.message, restoring = false)
                RunOutcome.Busy, RunOutcome.NeedsSignIn, RunOutcome.NotReady -> null
            }
        }
    }

    internal sealed interface RunOutcome {
        data class Done(val uploaded: Int, val downloaded: Int) : RunOutcome
        data class Failed(val message: String, val retryable: Boolean) : RunOutcome
        data object NeedsSignIn : RunOutcome
        data object NotReady : RunOutcome
        data object Busy : RunOutcome
    }

    internal suspend fun runSync(context: Context, push: Boolean): RunOutcome {
        val s = _settings.value
        if (s.account == null) return RunOutcome.NeedsSignIn
        if (s.needsDevice) return RunOutcome.NotReady
        if (!running.tryLock()) return RunOutcome.Busy
        return try {
            val report = withToken(context) { token ->
                ReplaySync.sync(context, token, _settings.value.identity, push) { step ->
                    _progress.value = Progress.Running(step)
                }
            }
            val now = System.currentTimeMillis()
            prefs.edit { putLong(KEY_LAST_PULL, now) }
            save {
                it.copy(
                    identity = report.identity,
                    lastSyncAt = if (push) now else it.lastSyncAt,
                    lastError = null,
                    forkedTo = report.forkedTo ?: it.forkedTo,
                )
            }
            _devices.value = Devices.Loaded(report.devices)
            RunOutcome.Done(report.uploaded, report.downloaded)
        } catch (e: CancellationException) {
            throw e
        } catch (_: NeedsSignIn) {
            RunOutcome.NeedsSignIn
        } catch (e: Exception) {
            val message = e.message ?: e.javaClass.simpleName
            save { it.copy(lastError = message) }
            RunOutcome.Failed(message, retryable = e is java.io.IOException && e !is DriveApi.DriveError)
        } finally {
            _progress.value = Progress.Idle
            running.unlock()
        }
    }

    internal fun scheduledRunDue(): Boolean {
        val s = _settings.value
        if (s.account == null || s.needsDevice || s.frequency == BackupFrequency.OFF) return false
        val period = TimeUnit.DAYS.toMillis(s.frequency.days)
        return System.currentTimeMillis() - s.lastSyncAt >= period / 2
    }

    private suspend fun <T> withToken(context: Context, block: suspend (String) -> T): T {
        val first = token(context)
        return try {
            block(first)
        } catch (e: DriveApi.Unauthorized) {
            DriveAuth.clearToken(context, first)
            block(token(context))
        }
    }

    private suspend fun token(context: Context): String {
        val account = _settings.value.account ?: throw NeedsSignIn()
        return when (val outcome = DriveAuth.authorize(context, account)) {
            is DriveAuth.Outcome.Granted -> outcome.accessToken
            is DriveAuth.Outcome.NeedsConsent -> {
                save { it.copy(needsSignIn = true) }
                throw NeedsSignIn()
            }
        }
    }

    internal fun deviceName(): String {
        val model = Build.MODEL.orEmpty()
        val maker = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.titlecase() }
        return if (maker.isBlank() || model.startsWith(maker, ignoreCase = true)) model else "$maker $model"
    }

    private fun constraints(cellular: Boolean) = Constraints.Builder()
        .setRequiredNetworkType(if (cellular) NetworkType.CONNECTED else NetworkType.UNMETERED)
        .setRequiresBatteryNotLow(true)
        .setRequiresStorageNotLow(true)
        .build()

    private fun schedule(context: Context, frequency: BackupFrequency, cellular: Boolean, replace: Boolean) {
        val period = frequency.days * 24
        val flex = when (frequency) {
            BackupFrequency.DAILY -> 6L
            BackupFrequency.WEEKLY -> 24L
            else -> 72L
        }
        val request = PeriodicWorkRequestBuilder<DriveBackupWorker>(period, TimeUnit.HOURS, flex, TimeUnit.HOURS)
            .setConstraints(constraints(cellular))
            .setInputData(workDataOf(KEY_PUSH to true))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun read(): Settings = Settings(
        account = prefs.getString(KEY_ACCOUNT, null),
        accountName = prefs.getString(KEY_ACCOUNT_NAME, null),
        frequency = prefs.getString(KEY_FREQUENCY, null)
            ?.let { name -> BackupFrequency.entries.firstOrNull { it.name == name } }
            ?: BackupFrequency.DAILY,
        overCellular = prefs.getBoolean(KEY_CELLULAR, false),
        lastSyncAt = prefs.getLong(KEY_LAST_SYNC, 0L),
        lastError = prefs.getString(KEY_LAST_ERROR, null),
        needsSignIn = prefs.getBoolean(KEY_NEEDS_SIGN_IN, false),
        identity = DeviceIdentity(
            installId = prefs.getString(KEY_INSTALL_ID, null).orEmpty(),
            deviceId = prefs.getString(KEY_DEVICE_ID, null),
            deviceName = prefs.getString(KEY_DEVICE_NAME, null).orEmpty(),
            adoptedManifestAt = prefs.getString(KEY_ADOPTED_AT, null),
        ),
        forkedTo = prefs.getString(KEY_FORKED_TO, null),
    )

    private fun save(change: (Settings) -> Settings) {
        _settings.update(change)
        if (!this::prefs.isInitialized) return
        val s = _settings.value
        prefs.edit {
            putString(KEY_ACCOUNT, s.account)
            putString(KEY_ACCOUNT_NAME, s.accountName)
            putString(KEY_FREQUENCY, s.frequency.name)
            putBoolean(KEY_CELLULAR, s.overCellular)
            putLong(KEY_LAST_SYNC, s.lastSyncAt)
            putString(KEY_LAST_ERROR, s.lastError)
            putBoolean(KEY_NEEDS_SIGN_IN, s.needsSignIn)
            putString(KEY_INSTALL_ID, s.identity.installId)
            putString(KEY_DEVICE_ID, s.identity.deviceId)
            putString(KEY_DEVICE_NAME, s.identity.deviceName)
            putString(KEY_ADOPTED_AT, s.identity.adoptedManifestAt)
            putString(KEY_FORKED_TO, s.forkedTo)
        }
    }
}

class DriveBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val push = inputData.getBoolean(DriveBackup.KEY_PUSH, true)
        if (push && !DriveBackup.scheduledRunDue()) return Result.success()
        return when (val outcome = DriveBackup.runSync(applicationContext, push)) {
            is DriveBackup.RunOutcome.Failed ->
                if (outcome.retryable && runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
            else -> Result.success()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
