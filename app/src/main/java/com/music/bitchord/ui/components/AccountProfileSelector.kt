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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
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
                                if (customPalettes.size >= AppSettings.MAX_CUSTOM_PALETTES) {
                                    Toast.makeText(
                                        context,
                                        "Preset not saved: ${AppSettings.MAX_CUSTOM_PALETTES} custom presets limit reached. Delete one in Settings → Appearance.",
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
                                if (customPalettes.size >= AppSettings.MAX_CUSTOM_PALETTES) {
                                    Toast.makeText(
                                        context,
                                        "Preset not saved: ${AppSettings.MAX_CUSTOM_PALETTES} custom presets limit reached. Delete one in Settings → Appearance.",
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
        val useLiquidGlass = LocalLiquidGlassEnabled.current && isGlassSupported() && !reduceDynamicBlur
        val dialogShape = RoundedCornerShape(22.dp)
        val submitPreset = {
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
        }

        Dialog(
            onDismissRequest = { showSavePresetDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 330.dp)
                        .fillMaxWidth()
                        .clip(dialogShape)
                        .then(
                            when {
                                reduceDynamicBlur -> Modifier.background(MaterialTheme.colorScheme.surface)
                                useLiquidGlass -> Modifier.liquidGlass(shape = dialogShape)
                                else -> Modifier.optimizedHazeEffect(
                                    state = hazeState,
                                    style = HazeMaterials.regular(MaterialTheme.colorScheme.surface),
                                )
                            },
                        )
                        .border(GLASS_EDGE_WIDTH, activeAccent.copy(alpha = 0.28f), dialogShape),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Artwork-tinted orb header badge
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(activeAccent, activeSecondary, activeBackground, activeAccent),
                                    ),
                                )
                                .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Brush,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.preserve_current_style),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = stringResource(R.string.preserve_style_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(14.dp))

                        // Live colorway swatch preview pill
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            activeBackground,
                                            activeSurfaceVariant.copy(alpha = 0.85f),
                                        ),
                                    ),
                                )
                                .border(0.75.dp, activeAccent.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.saved_presets_count,
                                    customPalettes.size,
                                    AppSettings.MAX_CUSTOM_PALETTES,
                                ),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = activeAccent,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(14.dp).clip(CircleShape).background(activeAccent))
                                Box(Modifier.size(14.dp).clip(CircleShape).background(activeSecondary))
                                Box(Modifier.size(14.dp).clip(CircleShape).background(activeSurfaceVariant))
                                Box(Modifier.size(14.dp).clip(CircleShape).background(activeBackground))
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Frosted text field for preset name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                .border(
                                    0.75.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                                    RoundedCornerShape(12.dp),
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (presetNameInput.isEmpty()) {
                                    Text(
                                        text = "Preset name",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    )
                                }
                                BasicTextField(
                                    value = presetNameInput,
                                    onValueChange = { presetNameInput = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    cursorBrush = SolidColor(activeAccent),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { submitPreset() }),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            if (presetNameInput.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .clickable { presetNameInput = "" },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.clear_name),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }

                    AlertRule()
                    AlertAction(
                        label = stringResource(R.string.save),
                        emphasised = true,
                        onClick = submitPreset,
                    )
                    AlertRule()
                    AlertAction(
                        label = stringResource(R.string.cancel),
                        emphasised = false,
                        onClick = { showSavePresetDialog = false },
                    )
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
