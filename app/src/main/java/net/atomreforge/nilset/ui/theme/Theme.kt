package net.atomreforge.nilset.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

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
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        remember(context) { dynamicDarkColorScheme(context) }
    } else {
        config.colorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = config.typography,
        content = content,
    )
}
