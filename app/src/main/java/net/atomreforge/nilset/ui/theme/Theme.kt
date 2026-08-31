package net.atomreforge.nilset.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

fun defaultDarkColorScheme() = darkColorScheme(
    primary = AccentWarm,
    onPrimary = OnAccentWarm,
    background = DarkBackground,
    onBackground = OnDarkBackground,
    surface = DarkSurface,
    onSurface = OnDarkBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant,
    outline = OutlineDark,
)

@Composable
fun ATOMTheme(
    config: AppThemeConfig = AppThemeConfig.Default,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = config.colorScheme,
        typography = config.typography,
        content = content,
    )
}
