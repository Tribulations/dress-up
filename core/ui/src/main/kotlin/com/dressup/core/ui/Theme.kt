package com.dressup.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * dress-up's own look (GUIDELINES.md §4.1): a dark, warm aubergine background so garment photos stand out, a mustard
 * accent for stars and actions and a rose colour for likes. Dark only for now; a light theme can come later.
 */
object DressUpColors {
    val Background = Color(0xFF17141C)
    val Surface = Color(0xFF1F1B25)
    val SurfaceVariant = Color(0xFF2B2632)
    val Mustard = Color(0xFFE9B44C)
    val OnMustard = Color(0xFF2A1E00)
    val Rose = Color(0xFFE5687A)
    val OnRose = Color(0xFF3A0711)
    val Text = Color(0xFFEDE7F1)
    val TextMuted = Color(0xFFB4ACBD)
    val Outline = Color(0xFF4A4352)
    val StarEmpty = Color(0xFF4A4352)
}

private val DarkScheme = darkColorScheme(
    primary = DressUpColors.Mustard,
    onPrimary = DressUpColors.OnMustard,
    primaryContainer = Color(0xFF4A3A12),
    onPrimaryContainer = Color(0xFFFFE0A3),
    secondary = DressUpColors.Rose,
    onSecondary = DressUpColors.OnRose,
    secondaryContainer = Color(0xFF4E2430),
    onSecondaryContainer = Color(0xFFFFD9DE),
    background = DressUpColors.Background,
    onBackground = DressUpColors.Text,
    surface = DressUpColors.Background,
    onSurface = DressUpColors.Text,
    surfaceVariant = DressUpColors.SurfaceVariant,
    onSurfaceVariant = DressUpColors.TextMuted,
    surfaceContainerLowest = DressUpColors.Background,
    surfaceContainerLow = DressUpColors.Surface,
    surfaceContainer = DressUpColors.Surface,
    surfaceContainerHigh = DressUpColors.SurfaceVariant,
    surfaceContainerHighest = Color(0xFF353040),
    outline = DressUpColors.Outline,
    outlineVariant = Color(0xFF3A3441),
)

@Composable
fun DressUpTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
