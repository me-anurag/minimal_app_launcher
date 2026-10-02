package com.minimal.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object MinimalColors {
    val Background = Color(0xFF000000)
    val Text = Color(0xFFEDEDED)
    val Dim = Color(0xFF8A8A8A)
}

@Composable
fun MinimalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = MinimalColors.Background,
            surface = MinimalColors.Background,
            onBackground = MinimalColors.Text,
            onSurface = MinimalColors.Text,
            primary = MinimalColors.Text,
        ),
        content = content,
    )
}
