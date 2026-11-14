package com.music.bitchord.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.auth.GoogleAccountSession
import com.music.bitchord.auth.YouTubeProfile
import com.music.bitchord.data.settings.AppSettings
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.music.bitchord.data.settings.CustomPalette
import com.music.bitchord.ui.theme.rememberArtworkPalette

/** Shared compact selector for every visible YouTube account avatar. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun AccountProfileSelector(
    accounts: List<GoogleAccountSession>,
    activeAccountId: String?,
    activeProfileId: String?,
    hazeState: HazeState,
    onSelect: (GoogleAccountSession, YouTubeProfile) -> Unit,
    onAddAccount: () -> Unit,
    onRemoveAccount: (GoogleAccountSession) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var managing by remember { mutableStateOf(false) }
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val activeArtworkUrl by AppSettings.activeTrackArtworkUrl.collectAsStateWithLifecycle()
    val customPalettes by AppSettings.customPalettes.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val isDark = isSystemInDarkTheme()
    val artPalette = rememberArtworkPalette(imageUrl = activeArtworkUrl, dark = isDark)
    val activeAccent = artPalette?.accent ?: MaterialTheme.colorScheme.primary
    val activeSurfaceVariant = artPalette?.elevated ?: MaterialTheme.colorScheme.surfaceVariant
    val activeBackground = artPalette?.background ?: MaterialTheme.colorScheme.background
    val activeSecondary = artPalette?.wash ?: MaterialTheme.colorScheme.secondary

    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }

    val shape = MaterialTheme.shapes.extraLarge
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = if (isLandscape) Alignment.Center else Alignment.TopCenter,
    ) {
        Surface(
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = shape,
            modifier = Modifier
                .padding(
                    top = if (isLandscape) 16.dp else 56.dp,
                    bottom = if (isLandscape) 16.dp else 24.dp,
                    start = 20.dp,
                    end = 20.dp,
                )
                .widthIn(max = if (isLandscape) 560.dp else 420.dp)
                .fillMaxWidth()
                .clip(shape)
                .then(
                    if (reduceDynamicBlur) {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    } else if (liquidGlass && isGlassSupported()) {
                        Modifier.liquidGlass(shape = shape)
                    } else {
                        Modifier.optimizedHazeEffect(
                            state = hazeState,
                            style = HazeMaterials.thin(MaterialTheme.colorScheme.surface),
                        )
                    },
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            if (isLandscape) {
                // Two-column layout in landscape: Left = Accounts & Profiles, Right = Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Left Column: Accounts & Profiles
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .heightIn(max = 280.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.switch_account),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                            accounts.forEach { account ->
                                item {
                                    Text(
                                        account.email.ifBlank { account.name.ifBlank { stringResource(R.string.accounts) } },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                    )
                                }
                                items(account.profiles.size) { index ->
                                    val profile = account.profiles[index]
                                    ProfileRow(
                                        profile,
                                        account.accountId == activeAccountId && profile.profileId == activeProfileId,
                                        managing,
                                        { onSelect(account, profile); onDismiss() },
                                        { onRemoveAccount(account) },
                                    )
                                }
                            }
                        }
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(240.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    )

                    // Right Column: Actions
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Actions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        SelectorAction(Icons.Rounded.Add, stringResource(R.string.add_account), onAddAccount)
                        SelectorAction(Icons.Rounded.ManageAccounts, stringResource(R.string.manage_accounts), onClick = { managing = !managing })
                        SelectorAction(
                            icon = Icons.Rounded.Brush,
                            label = stringResource(R.string.preserve_style),
                            onClick = {
                                if (customPalettes.size >= 5) {
                                    Toast.makeText(
                                        context,
                                        "Preset not saved: 5 custom presets limit reached. Delete one in Settings → Appearance.",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                } else {
                                    presetNameInput = "Style ${customPalettes.size + 1}"
                                    showSavePresetDialog = true
                                }
                            },
                            trailing = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(activeAccent))
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(activeSurfaceVariant))
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(activeBackground))
                                }
                            },
                        )
                        SelectorAction(Icons.Rounded.Settings, stringResource(R.string.settings), onOpenSettings)
                    }
                }
            } else {
                LazyColumn {
                    item { Text(stringResource(R.string.switch_account), style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(20.dp, 18.dp, 20.dp, 8.dp)) }
                    accounts.forEach { account ->
                    item {
                        Text(account.email.ifBlank { account.name.ifBlank { stringResource(R.string.accounts) } },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp, 12.dp, 20.dp, 4.dp))
                    }
                    items(account.profiles.size) { index ->
                        val profile = account.profiles[index]
                        ProfileRow(profile, account.accountId == activeAccountId && profile.profileId == activeProfileId,
                            managing, { onSelect(account, profile); onDismiss() }, { onRemoveAccount(account) })
                    }
                    }
                    item { SelectorAction(Icons.Rounded.Add, stringResource(R.string.add_account), onAddAccount) }
                    item { SelectorAction(Icons.Rounded.ManageAccounts, stringResource(R.string.manage_accounts), onClick = { managing = !managing }) }
                    item {
                        SelectorAction(
                            icon = Icons.Rounded.Brush,
                            label = stringResource(R.string.preserve_style),
                            onClick = {
                                if (customPalettes.size >= 5) {
                                    Toast.makeText(
                                        context,
                                        "Preset not saved: 5 custom presets limit reached. Delete one in Settings → Appearance.",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                } else {
                                    presetNameInput = "Style ${customPalettes.size + 1}"
                                    showSavePresetDialog = true
                                }
                            },
                            trailing = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(activeAccent))
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(activeSurfaceVariant))
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(activeBackground))
                                }
                            },
                        )
                    }
                    item { SelectorAction(Icons.Rounded.Settings, stringResource(R.string.settings), onOpenSettings) }
                }
            }
        }
    }

    if (showSavePresetDialog) {
        Dialog(
            onDismissRequest = { showSavePresetDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            val dialogShape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .clip(dialogShape)
                        .then(
                            if (reduceDynamicBlur) {
                                Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                            } else {
                                Modifier
                                    .optimizedHazeEffect(
                                        state = hazeState,
                                        style = HazeMaterials.regular(MaterialTheme.colorScheme.surface),
                                    )
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                            }
                        )
                        .border(1.dp, activeAccent.copy(alpha = 0.35f), dialogShape)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.preserve_current_style),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Freeze active song's colorway as a custom preset.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = activeBackground,
                        border = BorderStroke(1.dp, activeAccent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Preview",
                                style = MaterialTheme.typography.labelMedium,
                                color = activeAccent,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(16.dp).clip(CircleShape).background(activeAccent))
                                Box(Modifier.size(16.dp).clip(CircleShape).background(activeSurfaceVariant))
                                Box(Modifier.size(16.dp).clip(CircleShape).background(activeBackground))
                            }
                        }
                    }
                    OutlinedTextField(
                        value = presetNameInput,
                        onValueChange = { presetNameInput = it },
                        label = { Text("Preset Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { showSavePresetDialog = false }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                val finalName = presetNameInput.trim().ifBlank { "Style ${customPalettes.size + 1}" }
                                val newPalette = CustomPalette(
                                    name = finalName,
                                    primaryHex = activeAccent.toArgb().toLong() and 0xFFFFFFFFL,
                                    secondaryHex = activeSecondary.toArgb().toLong() and 0xFFFFFFFFL,
                                    backgroundHex = activeBackground.toArgb().toLong() and 0xFFFFFFFFL,
                                    surfaceHex = activeSurfaceVariant.toArgb().toLong() and 0xFFFFFFFFL,
                                    artworkUrl = activeArtworkUrl,
                                )
                                AppSettings.saveCustomPalette(newPalette)
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(
                                    context,
                                    "Style \"${finalName}\" saved! Apply it in Settings → Appearance → Color palette",
                                    Toast.LENGTH_LONG,
                                ).show()
                                showSavePresetDialog = false
                                onDismiss()
                            },
                        ) {
                            Text("Save", color = activeAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun ProfileRow(profile: YouTubeProfile, selected: Boolean, managing: Boolean,
    onClick: () -> Unit, onRemove: () -> Unit) {
    val description = stringResource(if (selected) R.string.selected_account else R.string.switch_account)
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.RadioButton, onClick = if (managing) onRemove else onClick)
        .semantics { contentDescription = description }
        .padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (profile.avatar != null) AsyncImage(profile.avatar, null, Modifier.size(38.dp).clip(MaterialTheme.shapes.extraLarge))
        else Icon(Icons.Rounded.Person, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) {
            Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(profile.handle.ifBlank { stringResource(if (profile.isBrandAccount) R.string.brand_account else R.string.personal) },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected && !managing) Icon(Icons.Rounded.Check, stringResource(R.string.selected_account), tint = MaterialTheme.colorScheme.primary)
        if (managing) Text(stringResource(R.string.sign_out), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun SelectorAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            trailing()
        }
    }
}
