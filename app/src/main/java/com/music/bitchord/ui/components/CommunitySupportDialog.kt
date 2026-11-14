package com.music.bitchord.ui.components

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.bitchord.R

private val AccentCopper = Color(0xFFE5A97D)
private val DialogBg = Color(0xFF13141B)
private val CardBg = Color(0xFF1A1B24)

/**
 * Modern, informative community and developer dialog for ChordPlay.
 * Responsive: clean single column in Portrait, balanced two-panel layout in Landscape.
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
        }.getOrNull() ?: "2.1.6"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val dialogShape = RoundedCornerShape(22.dp)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = if (isLandscape) 28.dp else 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = if (isLandscape) (screenWidth * 0.88f).coerceAtMost(620.dp) else 370.dp)
                    .fillMaxWidth()
                    .heightIn(max = if (isLandscape) (screenHeight * 0.90f).coerceAtMost(410.dp) else screenHeight * 0.86f)
                    .clip(dialogShape)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), dialogShape),
                shape = dialogShape,
                color = DialogBg,
                tonalElevation = 8.dp,
            ) {
                if (isLandscape) {
                    // ---- LANDSCAPE: TWO-PANEL SPLIT LAYOUT ----
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left Panel: Identity, Hero & Primary Action
                        Column(
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                // App Squircle Clean Logo
                                Image(
                                    painter = painterResource(R.drawable.ic_chordplay_squircle),
                                    contentDescription = "ChordPlay",
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(14.dp)),
                                )

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "ChordPlay",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                    ),
                                    color = Color.White,
                                )

                                Spacer(Modifier.height(4.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                ) {
                                    Text(
                                        text = "v$versionName",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = Color.White.copy(alpha = 0.70f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "Pure music, open-source & crafted with passion.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                    ),
                                    color = Color.White.copy(alpha = 0.65f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                )
                            }

                            // Left Panel Bottom Action
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            ) {
                                Text(
                                    text = "Enjoy Listening",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                    ),
                                )
                            }
                        }

                        // Right Panel: Developer & Community Hub Grouped Actions
                        Column(
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CommunityContentSections(
                                context = context,
                            )
                        }
                    }
                } else {
                    // ---- PORTRAIT: CLEAN INTEGRATED COLUMN ----
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // 1. Top Reddit Hero Banner (Without duplicate icon beneath it!)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(105.dp),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.banner_chordplay_reddit),
                                contentDescription = "ChordPlay Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize(),
                            )
                            // Smooth gradient fade to dialog background
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                DialogBg.copy(alpha = 0.50f),
                                                DialogBg,
                                            ),
                                        ),
                                    ),
                            )
                        }

                        // 2. Branding & Version Header
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "ChordPlay",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                    ),
                                    color = Color.White,
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                ) {
                                    Text(
                                        text = "v$versionName",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = Color.White.copy(alpha = 0.70f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // 3. Unified Inset Grouped Sections
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            CommunityContentSections(
                                context = context,
                            )

                            Spacer(Modifier.height(4.dp))

                            // 4. Primary Dismiss Button (Rounded Rectangle, not harsh pill)
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            ) {
                                Text(
                                    text = "Enjoy Listening",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                    ),
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable sections for Developer and Support & Community,
 * organized into cohesive unified rounded rectangles with internal row dividers.
 */
@Composable
private fun CommunityContentSections(
    context: android.content.Context,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Section 1: Developer
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "DEVELOPER",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                ),
                color = AccentCopper,
                modifier = Modifier.padding(start = 2.dp),
            )

            // Unified rounded container for Developer
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ActionRow(
                    iconPainter = painterResource(R.drawable.ic_github),
                    title = "Shivanshu",
                    subtitle = "@code4nigel • Lead Developer & Maintainer",
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/code4nigel"))
                            )
                        }
                    },
                )
            }
        }

        // Section 2: Support ChordPlay
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "SUPPORT CHORDPLAY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                ),
                color = AccentCopper,
                modifier = Modifier.padding(start = 2.dp),
            )

            // Single unified rounded container holding both Community hubs with hairline divider
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ActionRow(
                        iconPainter = painterResource(R.drawable.ic_reddit),
                        title = "Reddit Community",
                        subtitle = "r/ChordPlay • Join discussion & requests",
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/r/ChordPlay/"))
                                )
                            }
                        },
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.06f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )

                    ActionRow(
                        iconPainter = painterResource(R.drawable.ic_github),
                        title = "GitHub Repository",
                        subtitle = "code4nigel/ChordPlay • Star & contribute",
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/code4nigel/ChordPlay"))
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    iconPainter: Painter,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Flat rounded icon tile
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
                color = Color.White,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Color.White.copy(alpha = 0.60f),
            )
        }

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(18.dp),
        )
    }
}
