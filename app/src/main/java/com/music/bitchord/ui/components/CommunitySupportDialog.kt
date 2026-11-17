package com.music.bitchord.ui.components

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.onboarding.Onb
import com.music.bitchord.ui.onboarding.OnbCard
import com.music.bitchord.ui.onboarding.OnbPillButton
import com.music.bitchord.ui.onboarding.OnbSectionLabel
import com.music.bitchord.ui.onboarding.OnbSubtitleTracking
import com.music.bitchord.ui.onboarding.OnbTitleTracking
import com.music.bitchord.ui.player.MeshGradientBackground
import com.music.bitchord.ui.player.rememberArtworkColors

private val DialogBaseBg = Color(0xFF0A0A0E)
private val DialogPlumFallback = Brush.linearGradient(
    colors = listOf(Color(0xFF23152C), Color(0xFF14111D), DialogBaseBg),
)

/**
 * Modern frosted community and developer hub dialog matching the ChordPlay onboarding aesthetic.
 * Responsive: single column in Portrait, balanced two-panel layout in Landscape.
 */
@Composable
fun CommunitySupportDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "2.2.3"
    }

    val activeArtworkUrl by AppSettings.activeTrackArtworkUrl.collectAsStateWithLifecycle()
    val reduceBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val artworkPalette = rememberArtworkColors(activeArtworkUrl)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val dialogShape = RoundedCornerShape(28.dp)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = if (isLandscape) 28.dp else 18.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = if (isLandscape) (screenWidth * 0.88f).coerceAtMost(640.dp) else 388.dp)
                    .fillMaxWidth()
                    .heightIn(max = if (isLandscape) (screenHeight * 0.90f).coerceAtMost(420.dp) else screenHeight * 0.86f)
                    .clip(dialogShape)
                    .background(DialogBaseBg)
                    .border(1.dp, Onb.Hairline, dialogShape),
            ) {
                if (!reduceBlur) {
                    MeshGradientBackground(
                        palette = artworkPalette,
                        modifier = Modifier.matchParentSize(),
                        trackKey = activeArtworkUrl,
                        animated = false,
                        blurRadius = 46.dp,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(DialogPlumFallback),
                    )
                }
                // Subtle dark atmospheric veil so frosted cards & typography keep high contrast.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(DialogBaseBg.copy(alpha = 0.42f)),
                )

                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CommunityHeader(
                                versionName = versionName,
                                compact = true,
                            )

                            OnbPillButton(
                                text = "Enjoy Listening",
                                onClick = onDismiss,
                                modifier = Modifier.heightIn(min = 50.dp),
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CommunityContentSections(context = context)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CommunityHeader(versionName = versionName, compact = false)

                        Spacer(Modifier.height(18.dp))

                        CommunityContentSections(context = context)

                        Spacer(Modifier.height(18.dp))

                        OnbPillButton(
                            text = "Enjoy Listening",
                            onClick = onDismiss,
                            modifier = Modifier.heightIn(min = 50.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityHeader(
    versionName: String,
    compact: Boolean,
) {
    val iconShape = RoundedCornerShape(16.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_chordplay_squircle),
            contentDescription = "ChordPlay",
            modifier = Modifier
                .size(if (compact) 56.dp else 64.dp)
                .clip(iconShape)
                .border(1.dp, Onb.Hairline, iconShape),
        )

        Spacer(Modifier.height(if (compact) 10.dp else 12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "ChordPlay",
                color = Onb.Title,
                fontSize = if (compact) 22.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = OnbTitleTracking,
            )

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Onb.SurfaceHigh.copy(alpha = 0.85f))
                    .border(0.75.dp, Onb.Hairline, CircleShape)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "v$versionName",
                    color = Onb.Subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Pure music, open-source & crafted with passion.",
            color = Onb.Subtitle,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = OnbSubtitleTracking,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun CommunityContentSections(
    context: Context,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OnbSectionLabel("DEVELOPER")
            CommunityLinkCard(
                iconPainter = painterResource(R.drawable.ic_github),
                title = "Shivanshu",
                subtitle = "@code4nigel • Lead Developer & Maintainer",
                onClick = { openExternalUrl(context, "https://github.com/code4nigel") },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OnbSectionLabel("COMMUNITY")
            CommunityLinkCard(
                iconPainter = painterResource(R.drawable.ic_reddit),
                title = "Reddit Community",
                subtitle = "r/ChordPlay • Join discussion & requests",
                onClick = { openExternalUrl(context, "https://www.reddit.com/r/ChordPlay/") },
            )
            CommunityLinkCard(
                iconPainter = painterResource(R.drawable.ic_github),
                title = "GitHub Repository",
                subtitle = "code4nigel/ChordPlay • Star & contribute",
                onClick = { openExternalUrl(context, "https://github.com/code4nigel/ChordPlay") },
            )
        }
    }
}

@Composable
private fun CommunityLinkCard(
    iconPainter: Painter,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val cardShape = remember { RoundedCornerShape(22.dp) }
    OnbCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.75.dp, Onb.Hairline, cardShape),
        shape = cardShape,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Onb.SurfaceHigh.copy(alpha = 0.85f))
                    .border(0.75.dp, Onb.Hairline, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    tint = Onb.Title,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    color = Onb.Title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = Onb.Subtitle,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Onb.Faint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun openExternalUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
