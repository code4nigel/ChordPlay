package com.music.bitchord.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.music.bitchord.R
import com.music.bitchord.data.AppUpdateChecker
import com.music.bitchord.data.settings.AppSettings
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/** UIAlertController's own metrics: fixed narrow width, 14pt corner, 44pt rows. Kept for smaller picker alerts. */
internal val ALERT_WIDTH = 270.dp
internal val ALERT_CORNER = 14.dp
internal val ACTION_HEIGHT = 44.dp

/** Metrics tailored specifically for the update notice card to accommodate banners and changelogs. */
private val UPDATE_DIALOG_MAX_WIDTH = 420.dp
private val UPDATE_DIALOG_CORNER = 20.dp
private val NOTES_MAX_HEIGHT = 360.dp

/**
 * The dim behind the alert. Flat on purpose — the glass is the card, and
 * blurring the wallpaper *behind* it too leaves nothing for the card to be
 * frosted against, which is what made this read as a grey box before.
 */
internal val SCRIM_COLOR = Color.Black.copy(alpha = 0.28f)

private val DOWNLOAD_ROW_HEIGHT = 4.dp

private sealed interface ChangelogBlock {
    data class Text(val markdown: String) : ChangelogBlock
    data class Image(val url: String, val alt: String) : ChangelogBlock
}

private data class ParsedChangelog(
    val bannerImageUrl: String?,
    val blocks: List<ChangelogBlock>,
)

private val IMAGE_TAG_REGEX = Regex(
    """(?:<img[^>]+src=["']([^"']+)["'][^>]*>|!\[([^\]]*)\]\(([^)]+)\))""",
    RegexOption.IGNORE_CASE,
)

private fun parseChangelog(raw: String?): ParsedChangelog {
    if (raw.isNullOrBlank()) return ParsedChangelog(null, emptyList())

    val matches = IMAGE_TAG_REGEX.findAll(raw).toList()
    if (matches.isEmpty()) {
        return ParsedChangelog(null, listOf(ChangelogBlock.Text(raw.trim())))
    }

    var bannerUrl: String? = null
    var startIdx = 0
    val firstMatch = matches.first()

    // If the image is at the very beginning of the notes, treat it as the top Hero banner
    if (raw.substring(0, firstMatch.range.first).isBlank()) {
        bannerUrl = firstMatch.groupValues[1].ifEmpty { firstMatch.groupValues[3] }
        startIdx = firstMatch.range.last + 1
    }

    val remainingMatches = if (bannerUrl != null) matches.drop(1) else matches
    val blocks = mutableListOf<ChangelogBlock>()
    var cursor = startIdx

    for (m in remainingMatches) {
        val textBefore = raw.substring(cursor, m.range.first).trim()
        if (textBefore.isNotEmpty()) {
            blocks.add(ChangelogBlock.Text(textBefore))
        }
        val imgUrl = m.groupValues[1].ifEmpty { m.groupValues[3] }
        val alt = m.groupValues[2]
        if (imgUrl.isNotEmpty()) {
            blocks.add(ChangelogBlock.Image(url = imgUrl, alt = alt))
        }
        cursor = m.range.last + 1
    }

    val trailingText = raw.substring(cursor).trim()
    if (trailingText.isNotEmpty()) {
        blocks.add(ChangelogBlock.Text(trailingText))
    }

    return ParsedChangelog(bannerUrl, blocks)
}

/**
 * Once-per-launch nudge that a newer build is on GitHub Releases — the top
 * bar's [Icons.Rounded.Upgrade][androidx.compose.material.icons.rounded.Upgrade]
 * icon is the quiet, always-there version of this; this is the one-time,
 * hard-to-miss version shown the moment the check comes back.
 *
 * Shaped like an iOS system alert, which is the same lineage as the rest of the
 * app's Apple Music styling: frosted card, hairline rules, full-width actions
 * stacked under the message rather than a Material button pair in the corner.
 *
 * The update round trip happens here rather than in a browser: Download pulls
 * the release's APK into the app cache (progress fills the hairline under the
 * message), then Install hands it to the system installer. Where the release
 * carries no APK at all, the actions fall back to opening the releases page.
 *
 * Sits over the whole app as an overlay rather than an Android [Dialog][androidx.compose.ui.window.Dialog]
 * so its glass can sample the same [HazeState] the rest of the app's frosted
 * surfaces use, the way [FrostedTopBar] and [MiniPlayer] already do.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun UpdateAvailableDialog(
    version: String,
    notes: String?,
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpenReleasePage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val state by AppUpdateChecker.download.collectAsStateWithLifecycle()
    val shape = RoundedCornerShape(UPDATE_DIALOG_CORNER)
    val parsed = remember(notes) { parseChangelog(notes) }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary

    val changelogRichTextStyle = remember(onSurface, primary) {
        RichTextStyle(
            paragraphSpacing = 6.sp,
            headingStyle = { level, _ ->
                when (level) {
                    0 -> TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 21.sp,
                        color = onSurface,
                    )
                    1 -> TextStyle(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 19.sp,
                        color = onSurface,
                    )
                    2 -> TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp,
                        color = primary,
                    )
                    else -> TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp,
                        color = onSurface,
                    )
                }
            },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SCRIM_COLOR)
            // Tapping the scrim reads the same as Remind Me Later — nothing
            // about this update is mandatory, so backing out of it should be as
            // easy as getting into it. Mid-download it only closes the sheet;
            // the download keeps going and the top-bar icon reopens this.
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = UPDATE_DIALOG_MAX_WIDTH)
                .fillMaxWidth()
                .clip(shape)
                .then(
                    if (reduceDynamicBlur) {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    } else {
                        Modifier.optimizedHazeEffect(
                            state = hazeState,
                            style = HazeMaterials.regular(MaterialTheme.colorScheme.surface),
                        )
                    },
                )
                // Swallows the tap before it reaches the scrim behind, so
                // touching the card itself never dismisses it.
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Top Hero Banner image extracted from release changelog
                if (parsed.bannerImageUrl != null) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 14.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                    ) {
                        AsyncImage(
                            model = parsed.bannerImageUrl,
                            contentDescription = stringResource(R.string.software_update),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp, max = 150.dp),
                            contentScale = ContentScale.FillWidth,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.software_update),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.W600,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "v$version",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Text(
                    text = when (state) {
                        is AppUpdateChecker.DownloadState.Downloading ->
                            stringResource(R.string.update_downloading_body, version)
                        is AppUpdateChecker.DownloadState.Ready ->
                            stringResource(R.string.update_ready_body, version)
                        is AppUpdateChecker.DownloadState.Failed ->
                            stringResource(R.string.update_failed_body, version)
                        else ->
                            stringResource(R.string.update_available_body, version)
                    },
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                )

                // The download's progress, drawn as a thin fill across a
                // hairline track — same weight as [AlertRule], so it reads as
                // part of the card rather than a widget bolted onto it.
                val downloading = state as? AppUpdateChecker.DownloadState.Downloading
                if (downloading != null || state is AppUpdateChecker.DownloadState.Failed) {
                    Box(
                        Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                            .height(DOWNLOAD_ROW_HEIGHT)
                            .clip(RoundedCornerShape(DOWNLOAD_ROW_HEIGHT / 2))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
                    ) {
                        if (downloading != null && downloading.fraction > 0f) {
                            Box(
                                Modifier
                                    .fillMaxWidth(downloading.fraction)
                                    .height(DOWNLOAD_ROW_HEIGHT)
                                    .clip(RoundedCornerShape(DOWNLOAD_ROW_HEIGHT / 2))
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }
                }
                if (state is AppUpdateChecker.DownloadState.Failed) {
                    Text(
                        text = (state as AppUpdateChecker.DownloadState.Failed).message,
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }

                // The release's own notes, rendered as Markdown rather than
                // dumped as raw text — GitHub release bodies lean on headings,
                // bullet lists and bold for the changelog, and those are the
                // whole point of reading this before installing.
                if (parsed.blocks.isNotEmpty()) {
                    AlertRule(modifier = Modifier.padding(top = 14.dp))
                    Text(
                        text = stringResource(R.string.whats_new),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 6.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W600,
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Start,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = NOTES_MAX_HEIGHT)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (block in parsed.blocks) {
                                when (block) {
                                    is ChangelogBlock.Text -> {
                                        // RichText's Material3 Text leans on LocalContentColor,
                                        // which nothing here provides — this card is a plain
                                        // Column.background(...), not a Surface, so without
                                        // this the notes render at LocalContentColor's black
                                        // default regardless of theme.
                                        CompositionLocalProvider(
                                            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                                        ) {
                                            ProvideTextStyle(
                                                MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.5.sp,
                                                    lineHeight = 17.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                                                ),
                                            ) {
                                                RichText(style = changelogRichTextStyle) {
                                                    Markdown(block.markdown)
                                                }
                                            }
                                        }
                                    }
                                    is ChangelogBlock.Image -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                                        ) {
                                            AsyncImage(
                                                model = block.url,
                                                contentDescription = block.alt.ifEmpty { null },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 200.dp),
                                                contentScale = ContentScale.FillWidth,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AlertRule()
            when (state) {
                is AppUpdateChecker.DownloadState.Downloading -> {
                    AlertAction(label = stringResource(R.string.cancel), emphasised = false, onClick = onCancelDownload)
                }
                is AppUpdateChecker.DownloadState.Ready -> {
                    AlertAction(label = stringResource(R.string.install_now), emphasised = true, onClick = onInstall)
                    AlertRule()
                    AlertAction(label = stringResource(R.string.later), emphasised = false, onClick = onDismiss)
                }
                is AppUpdateChecker.DownloadState.Failed -> {
                    AlertAction(label = stringResource(R.string.try_again), emphasised = true, onClick = onDownload)
                    AlertRule()
                    AlertAction(label = stringResource(R.string.open_releases_page), emphasised = false, onClick = onOpenReleasePage)
                }
                else -> {
                    AlertAction(label = stringResource(R.string.download_now), emphasised = true, onClick = onDownload)
                    AlertRule()
                    AlertAction(label = stringResource(R.string.remind_me_later), emphasised = false, onClick = onDismiss)
                }
            }
        }
    }
}

/**
 * Full-bleed action row. Tinted rather than filled, so the two read as equals
 * in weight and only the font differentiates the default action — the alert's
 * whole point is that neither choice is a trap.
 *
 * [destructive] is the one exception to that: an action that deletes something
 * is red in this lineage, because the point there *is* to break the symmetry.
 * It stays a plain row rather than a filled button, so it reads as a warning
 * and not as the thing to press.
 */
@Composable
internal fun AlertAction(
    label: String,
    emphasised: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ACTION_HEIGHT)
            // iOS washes the whole row instead of drawing a ripple inside it.
            .background(
                if (pressed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f) else Color.Transparent,
            )
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                fontWeight = if (emphasised) FontWeight.W600 else FontWeight.W400,
            ),
            color = (if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                .copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}

/** Hairline separator — [HorizontalDivider][androidx.compose.material3.HorizontalDivider]'s 1dp reads as a bar at this scale. */
@Composable
internal fun AlertRule(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
    )
}

