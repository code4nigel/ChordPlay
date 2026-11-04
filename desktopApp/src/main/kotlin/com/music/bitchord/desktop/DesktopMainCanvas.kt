package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource

/**
 * ChordPlay Desktop Main Canvas (Center Pane).
 *
 * Scaffolds the center viewport of the 3-pane architecture:
 * - Scoped recomposition boundary to insulate browsing feeds from fast player ticks.
 * - Hardware-accelerated multi-stop radial gradient backdrop.
 * - Host for hero banners, mood chips, curated shelves, and album/artist detail pages.
 */
@Composable
internal fun DesktopMainCanvas(
    compact: Boolean,
    glass: Boolean,
    materialBehindApp: Boolean,
    containerColor: Color,
    backdrop: @Composable (transparentBase: Boolean) -> Unit,
    bottomBar: @Composable (compact: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(
        modifier
            .fillMaxHeight()
            .then(
                if (glass && !materialBehindApp) {
                    Modifier.background(containerColor)
                } else {
                    Modifier
                },
            ),
    ) {
        val haze = LocalDesktopHaze.current
        val hazeModifier = if (haze != null) Modifier.hazeSource(haze) else Modifier

        Box(Modifier.fillMaxSize().then(hazeModifier)) {
            if (glass) {
                backdrop(materialBehindApp)
            }
            content(
                PaddingValues(
                    start = 0.dp,
                    end = 0.dp,
                    bottom = if (compact) 128.dp else 16.dp,
                ),
            )
        }

        if (compact) {
            Box(Modifier.align(Alignment.BottomCenter)) {
                bottomBar(true)
            }
        }
    }
}
