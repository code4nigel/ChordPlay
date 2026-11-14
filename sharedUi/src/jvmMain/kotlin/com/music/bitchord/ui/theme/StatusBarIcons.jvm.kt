package com.music.bitchord.ui.theme

import androidx.compose.runtime.Composable

// A desktop window has no status bar to tint.
@Composable
actual fun StatusBarIcons(dark: Boolean) = Unit

@Composable
actual fun HideSystemBars(hidden: Boolean) = Unit
