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
package com.music.bitchord.ui.screens.settings.sections

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.R
import com.music.bitchord.ui.screens.GroupShape
import com.music.bitchord.ui.screens.ROW_INSET
import java.util.Locale

private data class LicenseCreditEntry(
    val name: String,
    val license: String,
    val copyrightOrAuthor: String? = null,
    val description: String? = null,
    val url: String? = null,
)

private data class LicenseCreditGroup(
    val title: String,
    val entries: List<LicenseCreditEntry>,
)

private val OpenSourceLicenseGroups: List<LicenseCreditGroup> = listOf(
    LicenseCreditGroup(
        title = "Application & Upstream",
        entries = listOf(
            LicenseCreditEntry(
                name = "ChordPlay",
                license = "GPL-3.0-or-later",
                copyrightOrAuthor = "Copyright (C) 2026 Shivanshu (code4nigel)",
                description = "ChordPlay Android music player, UI refresh, Automix Studio, Replay, Melody & Radio.",
                url = "https://github.com/code4nigel/ChordPlay",
            ),
            LicenseCreditEntry(
                name = "BitChord",
                license = "GPL-3.0-or-later",
                copyrightOrAuthor = "Copyright (C) 2024-2026 Kushagra Singh & BitChord contributors",
                description = "Upstream open-source music player project that ChordPlay was originally forked from.",
                url = "https://github.com/kushagrasinghx/BitChord",
            ),
        ),
    ),
    LicenseCreditGroup(
        title = "Vendored & Ported Components",
        entries = listOf(
            LicenseCreditEntry(
                name = "Kyant0/backdrop",
                license = "Apache-2.0",
                copyrightOrAuthor = "Copyright 2025 Kyant0",
                description = "Vendored Compose liquid glass refraction & shader backdrop engine.",
                url = "https://github.com/Kyant0/backdrop",
            ),
            LicenseCreditEntry(
                name = "compose-floating-tab-bar",
                license = "Apache-2.0",
                copyrightOrAuthor = "Elyes Mansour",
                description = "Vendored floating navigation bar layout and transition primitives.",
                url = "https://github.com/elyesmansour/compose-floating-tab-bar",
            ),
            LicenseCreditEntry(
                name = "androidx/media FFmpeg Decoder",
                license = "Apache-2.0",
                copyrightOrAuthor = "Copyright (C) 2016 The Android Open Source Project",
                description = "Vendored Media3 FFmpeg audio decoder & renderer Java/JNI bridge.",
                url = "https://github.com/androidx/media",
            ),
            LicenseCreditEntry(
                name = "librempeg / FFmpeg",
                license = "GPL-3.0-or-later",
                copyrightOrAuthor = "FFmpeg & librempeg contributors",
                description = "Prebuilt native libffmpegJNI.so (--enable-gpl --enable-version3) for ALAC, AC-4, E-AC-3, TrueHD, and DTS decoding.",
                url = "https://github.com/librempeg/librempeg",
            ),
            LicenseCreditEntry(
                name = "Zenith",
                license = "GPL-3.0-or-later",
                copyrightOrAuthor = "rezapranata089-bit/Zenith",
                description = "Ported unified player panel and snapshot backdrop concepts.",
                url = "https://github.com/rezapranata089-bit/Zenith",
            ),
            LicenseCreditEntry(
                name = "Echo-Music",
                license = "GPL-3.0-or-later",
                copyrightOrAuthor = "EchoMusicApp/Echo-Music",
                description = "Adapted liquid glass navigation bar configuration and centered tab pill styling.",
                url = "https://github.com/EchoMusicApp/Echo-Music",
            ),
            LicenseCreditEntry(
                name = "liquid_glass_easy",
                license = "MIT",
                copyrightOrAuthor = "Copyright (c) 2025 Ahmed Gamil",
                description = "Ported lens travel spring, held lift, and acceleration squash motion constants.",
                url = "https://pub.dev/packages/liquid_glass_easy",
            ),
            LicenseCreditEntry(
                name = "SpotiFLAC",
                license = "MIT",
                copyrightOrAuthor = "SpotiFLAC (MornyeGlassWarmup)",
                description = "Background AGSL shader warmup technique to prevent first-gesture frame stalls.",
            ),
            LicenseCreditEntry(
                name = "decent-player",
                license = "MIT",
                copyrightOrAuthor = "Copyright (c) 2026 Ma145",
                description = "Adapted USB DAC descriptor parsing and direct USB audio routing concepts.",
                url = "https://github.com/Ma145/decent-player",
            ),
            LicenseCreditEntry(
                name = "Dial",
                license = "GPL-3.0-or-later",
                copyrightOrAuthor = "steeben",
                description = "Adapted frosted alert, segmented control, iOS switch, and docking search components.",
                url = "https://github.com/steeben/Dial",
            ),
            LicenseCreditEntry(
                name = "Kizzy & InnerTune",
                license = "Credit",
                copyrightOrAuthor = "Copyright (C) 2022 yzziK (Vaibhav) • Modified by Zion Huang",
                description = "Discord Rich Presence gateway and RPC client integration.",
            ),
        ),
    ),
    LicenseCreditGroup(
        title = "Android Libraries",
        entries = listOf(
            LicenseCreditEntry(
                name = "InnerTubeX",
                license = "GPL-3.0-or-later",
                copyrightOrAuthor = "MetrolistGroup",
                description = "YouTube Music Innertube client and stream extraction.",
            ),
            LicenseCreditEntry(
                name = "NewPipeExtractor",
                license = "GPL-3.0-or-later",
                copyrightOrAuthor = "TeamNewPipe",
                description = "Stream URL extraction and signature deobfuscation.",
                url = "https://github.com/TeamNewPipe/NewPipeExtractor",
            ),
            LicenseCreditEntry(
                name = "Haze",
                license = "Apache-2.0",
                copyrightOrAuthor = "Chris Banes",
                description = "Backdrop blur and progressive glass material effects.",
                url = "https://github.com/chrisbanes/haze",
            ),
            LicenseCreditEntry(
                name = "AndroidX Media3 / ExoPlayer",
                license = "Apache-2.0",
                copyrightOrAuthor = "The Android Open Source Project",
                description = "Media playback engine, DASH support, and audio pipeline.",
                url = "https://github.com/androidx/media",
            ),
            LicenseCreditEntry(
                name = "Coil 3",
                license = "Apache-2.0",
                copyrightOrAuthor = "Coil Contributors",
                description = "Asynchronous image loading and artwork decoding.",
                url = "https://coil-kt.github.io/coil/",
            ),
            LicenseCreditEntry(
                name = "OkHttp",
                license = "Apache-2.0",
                copyrightOrAuthor = "Square, Inc.",
                description = "HTTP/2 & TLS networking client.",
                url = "https://square.github.io/okhttp/",
            ),
            LicenseCreditEntry(
                name = "Ktor",
                license = "Apache-2.0",
                copyrightOrAuthor = "JetBrains",
                description = "Multiplatform HTTP client, JSON serialization, and WebSockets.",
                url = "https://ktor.io",
            ),
            LicenseCreditEntry(
                name = "ZXing (core)",
                license = "Apache-2.0",
                copyrightOrAuthor = "ZXing Authors",
                description = "QR matrix generation for Listen Together invites.",
                url = "https://github.com/zxing/zxing",
            ),
            LicenseCreditEntry(
                name = "Jsoup",
                license = "MIT",
                copyrightOrAuthor = "Jonathan Hedley",
                description = "HTML parsing utilities.",
                url = "https://jsoup.org",
            ),
            LicenseCreditEntry(
                name = "ONNX Runtime",
                license = "MIT",
                copyrightOrAuthor = "Microsoft & ONNX Runtime Contributors",
                description = "On-device ML inference engine for Automix analysis.",
                url = "https://onnxruntime.ai",
            ),
            LicenseCreditEntry(
                name = "QuickJS-kt",
                license = "Apache-2.0",
                copyrightOrAuthor = "dokar3",
                description = "Lightweight JavaScript VM for source modules.",
                url = "https://github.com/dokar3/quickjs-kt",
            ),
            LicenseCreditEntry(
                name = "Mozilla Rhino",
                license = "MPL-2.0",
                copyrightOrAuthor = "Mozilla & Rhino Contributors",
                description = "JavaScript engine used by NewPipeExtractor.",
                url = "https://github.com/mozilla/rhino",
            ),
            LicenseCreditEntry(
                name = "SMBJ",
                license = "Apache-2.0",
                copyrightOrAuthor = "Jeroen van Erp (hierynomus)",
                description = "SMB2/SMB3 network share browsing and streaming.",
                url = "https://github.com/hierynomus/smbj",
            ),
            LicenseCreditEntry(
                name = "BouncyCastle",
                license = "MIT",
                copyrightOrAuthor = "Legion of the Bouncy Castle Inc.",
                description = "Cryptographic provider used with SMBJ transport.",
                url = "https://www.bouncycastle.org",
            ),
            LicenseCreditEntry(
                name = "nanojson",
                license = "Unlicense",
                copyrightOrAuthor = "TeamNewPipe / mmaclean",
                description = "Lightweight JSON parser used with NewPipeExtractor.",
                url = "https://github.com/TeamNewPipe/nanojson",
            ),
        ),
    ),
    LicenseCreditGroup(
        title = "Machine Learning Models",
        entries = listOf(
            LicenseCreditEntry(
                name = "Beat This!",
                license = "MIT",
                copyrightOrAuthor = "CPJKU (ISMIR 2024)",
                description = "On-device beat and downbeat tracking model for Automix transitions.",
            ),
            LicenseCreditEntry(
                name = "Open-Unmix",
                license = "MIT",
                copyrightOrAuthor = "F.-R. Stöter & A. Liutkus (Inria / SigSep)",
                description = "On-device vocal-presence tracking model for clean vocal-aware transitions.",
            ),
        ),
    ),
    LicenseCreditGroup(
        title = "Bundled Fonts",
        entries = listOf(
            LicenseCreditEntry(
                name = "Poppins",
                license = "SIL OFL 1.1",
                copyrightOrAuthor = "Copyright 2020 The Poppins Project Authors (Indian Type Foundry)",
                description = "Geometric sans-serif font family.",
                url = "https://github.com/itfoundry/Poppins",
            ),
            LicenseCreditEntry(
                name = "Parkinsans",
                license = "SIL OFL 1.1",
                copyrightOrAuthor = "Copyright 2024 The Parkinsans Project Authors (Red Stone, Indian Type Foundry)",
                description = "Display grotesque font family.",
                url = "https://github.com/redstonedesign/parkinsans",
            ),
        ),
    ),
    LicenseCreditGroup(
        title = "Lyrics & Canvas Providers",
        entries = listOf(
            LicenseCreditEntry(
                name = "Lyrics Sources",
                license = "Providers",
                description = "LRCLIB, Musixmatch, Genius, BetterLyrics, BiniLyrics, SimpMusic, LyricsPlus, PaxSenix, Unison, lrc.red, Megalobiz, and KuGou.",
            ),
            LicenseCreditEntry(
                name = "Animated Cover & Canvas Sources",
                license = "Providers",
                description = "vivimusicanvas (Community Canvas), Apple Music, Tidal, and user-connected Spotify Canvas.",
            ),
        ),
    ),
    LicenseCreditGroup(
        title = "Trademarks & Legal Notices",
        entries = listOf(
            LicenseCreditEntry(
                name = "Dolby & Dolby Atmos",
                license = "Trademark",
                description = "Dolby, Dolby Atmos, and the double-D symbol are trademarks of Dolby Laboratories Licensing Corporation. Shown solely to identify stream format metadata.",
            ),
            LicenseCreditEntry(
                name = "Spotify",
                license = "Trademark",
                description = "Spotify and the Spotify logo are trademarks of Spotify AB. Shown solely to identify optional user-configured account and Canvas integrations.",
            ),
            LicenseCreditEntry(
                name = "GNU General Public License v3.0",
                license = "GPL-3.0",
                description = "ChordPlay is free software distributed under the GNU GPL v3.0 or later, without warranty. Full license texts are available in the project repository.",
                url = "https://www.gnu.org/licenses/gpl-3.0.html",
            ),
            LicenseCreditEntry(
                name = "Apache License 2.0",
                license = "Apache-2.0",
                description = "Applies to Apache-2.0 licensed components and vendored libraries listed above.",
                url = "https://www.apache.org/licenses/LICENSE-2.0",
            ),
            LicenseCreditEntry(
                name = "MIT License",
                license = "MIT",
                description = "Permission is granted, free of charge, to use, copy, modify, merge, publish, and distribute MIT-licensed software and weights subject to copyright and permission notices.",
                url = "https://opensource.org/license/mit",
            ),
            LicenseCreditEntry(
                name = "Mozilla Public License 2.0",
                license = "MPL-2.0",
                description = "Applies to Mozilla Rhino components bundled with NewPipeExtractor.",
                url = "https://www.mozilla.org/en-US/MPL/2.0/",
            ),
            LicenseCreditEntry(
                name = "SIL Open Font License 1.1",
                license = "SIL OFL 1.1",
                description = "Applies to the bundled Poppins and Parkinsans typefaces.",
                url = "https://scripts.sil.org/OFL",
            ),
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OpenSourceLicensesSheet(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.88f).dp
    var filterQuery by remember { mutableStateOf("") }

    val filteredGroups = remember(filterQuery) {
        val q = filterQuery.trim()
        if (q.isEmpty()) {
            OpenSourceLicenseGroups
        } else {
            OpenSourceLicenseGroups.mapNotNull { group ->
                if (group.title.contains(q, ignoreCase = true)) {
                    group
                } else {
                    val matched = group.entries.filter { entry ->
                        entry.name.contains(q, ignoreCase = true) ||
                            entry.license.contains(q, ignoreCase = true) ||
                            (entry.copyrightOrAuthor?.contains(q, ignoreCase = true) == true) ||
                            (entry.description?.contains(q, ignoreCase = true) == true)
                    }
                    if (matched.isEmpty()) null else group.copy(entries = matched)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.open_source_licenses),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.open_source_licenses_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.done),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Compact search field matching Settings styling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (filterQuery.isEmpty()) {
                        Text(
                            text = "Filter libraries, licenses, fonts...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                    BasicTextField(
                        value = filterQuery,
                        onValueChange = { filterQuery = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (filterQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable { filterQuery = "" },
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (filteredGroups.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.settings_search_empty, filterQuery),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 24.dp),
                        )
                    }
                } else {
                    filteredGroups.forEach { group ->
                        item(key = group.title) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = group.title.uppercase(Locale.ROOT),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(GroupShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                ) {
                                    group.entries.forEachIndexed { index, entry ->
                                        if (index > 0) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(start = ROW_INSET),
                                                thickness = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                            )
                                        }
                                        LicenseEntryRow(
                                            entry = entry,
                                            onOpenUrl = entry.url?.let { targetUrl ->
                                                {
                                                    runCatching {
                                                        context.startActivity(
                                                            Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)),
                                                        )
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LicenseEntryRow(
    entry: LicenseCreditEntry,
    onOpenUrl: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onOpenUrl != null) Modifier.clickable(onClick = onOpenUrl) else Modifier)
            .padding(horizontal = ROW_INSET, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .border(
                        0.75.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                        RoundedCornerShape(50),
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = entry.license,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
            if (onOpenUrl != null) {
                Icon(
                    imageVector = Icons.Rounded.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        if (!entry.copyrightOrAuthor.isNullOrBlank()) {
            Text(
                text = entry.copyrightOrAuthor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            )
        }

        if (!entry.description.isNullOrBlank()) {
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!entry.url.isNullOrBlank()) {
            Text(
                text = entry.url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
