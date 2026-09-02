package net.atomreforge.nilset.core.theme

import kotlinx.serialization.Serializable

object ThemeColorFields {
    const val PRIMARY = "primary"
    const val SECONDARY = "secondary"
    const val BACKGROUND = "background"
    const val SURFACE = "surface"

    val ALL = listOf(PRIMARY, SECONDARY, BACKGROUND, SURFACE)
}

@Serializable
data class ThemeColors(
    val primary: String,
    val secondary: String,
    val background: String,
    val surface: String,
) {
    fun value(field: String): String? = when (field) {
        ThemeColorFields.PRIMARY -> primary
        ThemeColorFields.SECONDARY -> secondary
        ThemeColorFields.BACKGROUND -> background
        ThemeColorFields.SURFACE -> surface
        else -> null
    }
}

enum class ThemeMode {
    LIGHT,
    DARK,
}

enum class ThemePreset(
    val id: String,
    val label: String,
    val lightColors: ThemeColors?,
    val darkColors: ThemeColors?,
) {
    MAPLE(
        "maple",
        "枫糖",
        ThemeColors(
            primary = "#D87C5F",
            secondary = "#7D5C4F",
            background = "#FFF8F5",
            surface = "#FFEFE9",
        ),
        ThemeColors(
            primary = "#D87C5F",
            secondary = "#7D5C4F",
            background = "#191210",
            surface = "#231A16",
        ),
    ),
    CHERRY(
        "cherry",
        "落樱",
        ThemeColors(
            primary = "#F5C2D7",
            secondary = "#7A5A68",
            background = "#FFF8FA",
            surface = "#FFEDF4",
        ),
        ThemeColors(
            primary = "#F5C2D7",
            secondary = "#B39AA5",
            background = "#201619",
            surface = "#2A1E22",
        ),
    ),
    JADE(
        "jade",
        "青碧",
        ThemeColors(
            primary = "#009999",
            secondary = "#F5C2D7",
            background = "#F4FBFA",
            surface = "#E4F2F0",
        ),
        ThemeColors(
            primary = "#009999",
            secondary = "#D8BAC6",
            background = "#101C1A",
            surface = "#182624",
        ),
    ),
    DYNAMIC(
        "dynamic",
        "动态取色",
        ThemeColors(
            primary = "#6750A4",
            secondary = "#625B71",
            background = "#FFFBFE",
            surface = "#F3EDF7",
        ),
        ThemeColors(
            primary = "#D0BCFF",
            secondary = "#CCC2DC",
            background = "#141218",
            surface = "#211F26",
        ),
    ),
    CUSTOM("custom", "自定义", null, null);

    fun colors(useDark: Boolean): ThemeColors? {
        return if (useDark) darkColors else lightColors
    }

    companion object {
        fun fromId(id: String): ThemePreset =
            entries.firstOrNull { it.id == id } ?: MAPLE
    }
}

@Serializable
data class UserThemeSettings(
    val mode: ThemeMode = ThemeMode.DARK,
    val paletteId: String = ThemePreset.MAPLE.id,
    val customLightColors: ThemeColors? = null,
    val customDarkColors: ThemeColors? = null,
    val showCardBorders: Boolean = true,
    val textScaleEnabled: Boolean = false,
    val textScale: Float = 1f,
    val uiScaleEnabled: Boolean = false,
    val uiScale: Float = 1f,
) {
    val palette: ThemePreset
        get() = ThemePreset.fromId(paletteId)

    val isDynamic: Boolean
        get() = palette == ThemePreset.DYNAMIC

    val effectiveTextScale: Float
        get() = if (textScaleEnabled) textScale.coerceIn(MIN_SCALE, MAX_SCALE) else 1f

    val effectiveUiScale: Float
        get() = if (uiScaleEnabled) uiScale.coerceIn(MIN_SCALE, MAX_SCALE) else 1f

    fun usesDarkTheme(): Boolean {
        return when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    }

    fun effectiveColors(useDark: Boolean = usesDarkTheme()): ThemeColors {
        if (palette == ThemePreset.CUSTOM) {
            return if (useDark) {
                customDarkColors ?: FALLBACK_DARK_COLORS
            } else {
                customLightColors ?: FALLBACK_LIGHT_COLORS
            }
        }
        return palette.colors(useDark) ?: if (useDark) FALLBACK_DARK_COLORS else FALLBACK_LIGHT_COLORS
    }

    companion object {
        const val MIN_SCALE = 0.8f
        const val MAX_SCALE = 1.2f
        const val DEFAULT_SCALE = 1f
        val FALLBACK_LIGHT_COLORS = ThemePreset.MAPLE.lightColors!!
        val FALLBACK_DARK_COLORS = ThemePreset.MAPLE.darkColors!!
    }
}

object ThemeColorParser {
    private val pattern = Regex("^#([0-9A-Fa-f]{6})$")

    fun normalize(value: String?): String? {
        val trimmed = value?.trim().orEmpty()
        val match = pattern.matchEntire(trimmed) ?: return null
        return "#${match.groupValues[1].uppercase()}"
    }

    fun parseArgb(value: String?): Int? {
        val normalized = normalize(value) ?: return null
        return normalized.substring(1).toLong(16).toInt() or 0xFF000000.toInt()
    }
}
