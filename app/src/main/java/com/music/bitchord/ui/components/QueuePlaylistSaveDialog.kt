package com.music.bitchord.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.model.PlaylistPrivacy
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.playback.QueueHistoryRange
import com.music.bitchord.ui.QueuePlaylistSaveState
import com.music.bitchord.ui.QueuePlaylistSaveState.Status
import java.util.Locale

@Composable
internal fun QueuePlaylistSaveDialog(
    state: QueuePlaylistSaveState,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    onCreate: (String, PlaylistPrivacy, QueueHistoryRange?, Boolean) -> Unit,
    onVerify: () -> Unit,
    onFinish: () -> Unit,
    onOpen: (String, String) -> Unit,
) {
    val title = stringResource(R.string.queue_playlist_title)
    when (state) {
        QueuePlaylistSaveState.SignIn -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(stringResource(R.string.queue_playlist_sign_in)) },
            confirmButton = { TextButton(onSignIn) { Text(stringResource(R.string.sign_in)) } },
            dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
        )
        is QueuePlaylistSaveState.Form -> {
            var history by remember(state.capture) { mutableStateOf<QueueHistoryRange?>(null) }
            var review by remember(state.capture) { mutableStateOf<Pair<String, PlaylistPrivacy>?>(null) }
            val snapshot = remember(state.capture, history) { state.capture.select(history) }
            Dialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Surface(Modifier.padding(horizontal = 16.dp).widthIn(max = 520.dp).fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface) {
                    NewPlaylistForm(
                        onBack = null,
                        onCreate = { name, privacy ->
                            if (snapshot.skipped.isEmpty() && snapshot.upload.videoIds.isNotEmpty()) {
                                onCreate(name, privacy, history, false)
                            } else {
                                review = name to privacy
                            }
                        },
                        onDismiss = onDismiss,
                        title = title,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        additionalOptions = {
                            QueueHistoryOptions(state.capture.historySize, state.capture.historyCount(history), history) {
                                history = it
                            }
                        },
                    )
                }
            }
            review?.let { (name, privacy) ->
                val available = snapshot.upload.videoIds.isNotEmpty()
                AlertDialog(
                    onDismissRequest = { review = null },
                    title = { Text(title) },
                    text = {
                        val message = if (available) R.string.queue_playlist_skip else R.string.queue_playlist_none_available
                        val missing = snapshot.skipped.joinToString("\n") { "• ${it.title} · ${it.artist}" }
                        Text(stringResource(message) + if (missing.isEmpty()) "" else "\n\n$missing",
                            Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()))
                    },
                    confirmButton = {
                        if (available) TextButton({
                            review = null
                            onCreate(name, privacy, history, true)
                        }) { Text(stringResource(R.string.queue_playlist_save_available)) }
                        else TextButton({ review = null }) { Text(stringResource(R.string.close)) }
                    },
                    dismissButton = if (available) {
                        { TextButton({ review = null }) { Text(stringResource(R.string.cancel)) } }
                    } else null,
                )
            }
        }
        QueuePlaylistSaveState.Creating -> QueueSaveProgress(title, R.string.queue_playlist_creating)
        QueuePlaylistSaveState.CreateUnconfirmed, QueuePlaylistSaveState.ProfileChanged -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(stringResource(if (state == QueuePlaylistSaveState.ProfileChanged)
                R.string.queue_playlist_account_changed else R.string.queue_playlist_creation_unconfirmed)) },
            confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.close)) } },
        )
        is QueuePlaylistSaveState.Receipt -> {
            if (state.status == Status.VERIFYING) {
                QueueSaveProgress(state.pending.title, R.string.queue_playlist_verifying)
                return
            }
            val message = when (state.status) {
                Status.VERIFIED -> R.string.queue_playlist_saved
                Status.MISMATCH -> R.string.queue_playlist_mismatch
                Status.MIX_SAVE_FAILED -> R.string.queue_playlist_mix_failed
                else -> R.string.queue_playlist_verify_failed
            }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(state.pending.title) },
                text = { Text(stringResource(message)) },
                confirmButton = {
                    if (state.status == Status.VERIFIED) {
                        TextButton({ onOpen(state.pending.browseId, state.pending.title) }) {
                            Text(stringResource(R.string.open_playlist))
                        }
                    } else TextButton(onVerify) { Text(stringResource(R.string.queue_playlist_retry_verify)) }
                },
                dismissButton = { TextButton(onFinish) { Text(stringResource(R.string.done)) } },
            )
        }
    }
}

@Composable
private fun QueueSaveProgress(title: String, message: Int) {
    AlertDialog(
        onDismissRequest = {},
        icon = { CircularProgressIndicator() },
        title = { Text(title) },
        text = { Text(stringResource(message)) },
        confirmButton = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    )
}

@Composable
private fun QueueHistoryOptions(
    historySize: Int,
    included: Int,
    selected: QueueHistoryRange?,
    onSelect: (QueueHistoryRange?) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.queue_history_title).uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.queue_history_count, included, historySize),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QueueHistoryRange.entries.forEach { option ->
                HistoryOptionPill(
                    label = if (option == QueueHistoryRange.ALL) {
                        stringResource(R.string.queue_history_all)
                    } else {
                        stringResource(R.string.queue_history_last, option.limit)
                    },
                    selected = selected == option,
                    enabled = historySize > 0,
                    onClick = { onSelect(option.takeUnless { selected == it }) },
                )
            }
        }
    }
}

@Composable
private fun HistoryOptionPill(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val glassSupported = isGlassSupported() && !reduceDynamicBlur
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(MaterialTheme.colorScheme.primary)
                } else if (liquidGlass && glassSupported) {
                    Modifier.lightweightLiquidGlass(
                        shape = CircleShape,
                        fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    )
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
