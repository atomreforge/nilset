package net.atomreforge.nilset.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import net.atomreforge.nilset.core.theme.ThemeColorFields
import net.atomreforge.nilset.core.theme.ThemeColorParser
import net.atomreforge.nilset.core.theme.UserThemeSettings

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

@Composable
fun ATOMTheme(
    settings: UserThemeSettings,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = if (settings.materialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        remember(context) { dynamicDarkColorScheme(context) }
    } else {
        remember(settings) { settings.toColorScheme() }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppThemeConfig.Default.typography,
        content = content,
    )
}

private fun UserThemeSettings.toColorScheme(): ColorScheme {
    val base = if (preset.colors == null) defaultDarkColorScheme() else lightColorScheme()
    val presetColors = preset.colors

    return base.copy(
        primary = color(ThemeColorFields.PRIMARY, presetColors?.primary, base.primary),
        onPrimary = color(ThemeColorFields.ON_PRIMARY, presetColors?.onPrimary, base.onPrimary),
        primaryContainer = color(ThemeColorFields.PRIMARY_CONTAINER, presetColors?.primaryContainer, base.primaryContainer),
        onPrimaryContainer = color(ThemeColorFields.ON_PRIMARY_CONTAINER, presetColors?.onPrimaryContainer, base.onPrimaryContainer),
        secondary = color(ThemeColorFields.SECONDARY, presetColors?.secondary, base.secondary),
        onSecondary = color(ThemeColorFields.ON_SECONDARY, presetColors?.onSecondary, base.onSecondary),
        secondaryContainer = color(ThemeColorFields.SECONDARY_CONTAINER, presetColors?.secondaryContainer, base.secondaryContainer),
        onSecondaryContainer = color(ThemeColorFields.ON_SECONDARY_CONTAINER, presetColors?.onSecondaryContainer, base.onSecondaryContainer),
        tertiary = color(ThemeColorFields.TERTIARY, presetColors?.tertiary, base.tertiary),
        onTertiary = color(ThemeColorFields.ON_TERTIARY, presetColors?.onTertiary, base.onTertiary),
        tertiaryContainer = color(ThemeColorFields.TERTIARY_CONTAINER, presetColors?.tertiaryContainer, base.tertiaryContainer),
        onTertiaryContainer = color(ThemeColorFields.ON_TERTIARY_CONTAINER, presetColors?.onTertiaryContainer, base.onTertiaryContainer),
        background = color(ThemeColorFields.BACKGROUND, presetColors?.background, base.background),
        onBackground = color(ThemeColorFields.ON_BACKGROUND, presetColors?.onBackground, base.onBackground),
        surface = color(ThemeColorFields.SURFACE, presetColors?.surface, base.surface),
        onSurface = color(ThemeColorFields.ON_SURFACE, presetColors?.onSurface, base.onSurface),
        surfaceVariant = color(ThemeColorFields.SURFACE_VARIANT, presetColors?.surfaceVariant, base.surfaceVariant),
        onSurfaceVariant = color(ThemeColorFields.ON_SURFACE_VARIANT, presetColors?.onSurfaceVariant, base.onSurfaceVariant),
        outline = color(ThemeColorFields.OUTLINE, presetColors?.outline, base.outline),
        error = color(ThemeColorFields.ERROR, presetColors?.error, base.error),
        onError = color(ThemeColorFields.ON_ERROR, presetColors?.onError, base.onError),
    )
}

private fun UserThemeSettings.color(
    field: String,
    presetValue: String?,
    baseValue: Color,
): Color {
    val argb = ThemeColorParser.parseArgb(resolvedColors()[field])
        ?: ThemeColorParser.parseArgb(presetValue)
        ?: return baseValue

    return Color(argb)
}
