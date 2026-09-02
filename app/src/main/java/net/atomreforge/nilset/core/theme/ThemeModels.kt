package net.atomreforge.nilset.core.theme

import kotlinx.serialization.Serializable

object ThemeColorFields {
    const val PRIMARY = "primary"
    const val ON_PRIMARY = "onPrimary"
    const val PRIMARY_CONTAINER = "primaryContainer"
    const val ON_PRIMARY_CONTAINER = "onPrimaryContainer"
    const val SECONDARY = "secondary"
    const val ON_SECONDARY = "onSecondary"
    const val SECONDARY_CONTAINER = "secondaryContainer"
    const val ON_SECONDARY_CONTAINER = "onSecondaryContainer"
    const val TERTIARY = "tertiary"
    const val ON_TERTIARY = "onTertiary"
    const val TERTIARY_CONTAINER = "tertiaryContainer"
    const val ON_TERTIARY_CONTAINER = "onTertiaryContainer"
    const val BACKGROUND = "background"
    const val ON_BACKGROUND = "onBackground"
    const val SURFACE = "surface"
    const val ON_SURFACE = "onSurface"
    const val SURFACE_VARIANT = "surfaceVariant"
    const val ON_SURFACE_VARIANT = "onSurfaceVariant"
    const val OUTLINE = "outline"
    const val ERROR = "error"
    const val ON_ERROR = "onError"

    val ALL = listOf(
        PRIMARY,
        ON_PRIMARY,
        PRIMARY_CONTAINER,
        ON_PRIMARY_CONTAINER,
        SECONDARY,
        ON_SECONDARY,
        SECONDARY_CONTAINER,
        ON_SECONDARY_CONTAINER,
        TERTIARY,
        ON_TERTIARY,
        TERTIARY_CONTAINER,
        ON_TERTIARY_CONTAINER,
        BACKGROUND,
        ON_BACKGROUND,
        SURFACE,
        ON_SURFACE,
        SURFACE_VARIANT,
        ON_SURFACE_VARIANT,
        OUTLINE,
        ERROR,
        ON_ERROR,
    )
}

data class ThemeColors(
    val primary: String? = null,
    val onPrimary: String? = null,
    val primaryContainer: String? = null,
    val onPrimaryContainer: String? = null,
    val secondary: String? = null,
    val onSecondary: String? = null,
    val secondaryContainer: String? = null,
    val onSecondaryContainer: String? = null,
    val tertiary: String? = null,
    val onTertiary: String? = null,
    val tertiaryContainer: String? = null,
    val onTertiaryContainer: String? = null,
    val background: String? = null,
    val onBackground: String? = null,
    val surface: String? = null,
    val onSurface: String? = null,
    val surfaceVariant: String? = null,
    val onSurfaceVariant: String? = null,
    val outline: String? = null,
    val error: String? = null,
    val onError: String? = null,
) {
    fun value(field: String): String? = when (field) {
        ThemeColorFields.PRIMARY -> primary
        ThemeColorFields.ON_PRIMARY -> onPrimary
        ThemeColorFields.PRIMARY_CONTAINER -> primaryContainer
        ThemeColorFields.ON_PRIMARY_CONTAINER -> onPrimaryContainer
        ThemeColorFields.SECONDARY -> secondary
        ThemeColorFields.ON_SECONDARY -> onSecondary
        ThemeColorFields.SECONDARY_CONTAINER -> secondaryContainer
        ThemeColorFields.ON_SECONDARY_CONTAINER -> onSecondaryContainer
        ThemeColorFields.TERTIARY -> tertiary
        ThemeColorFields.ON_TERTIARY -> onTertiary
        ThemeColorFields.TERTIARY_CONTAINER -> tertiaryContainer
        ThemeColorFields.ON_TERTIARY_CONTAINER -> onTertiaryContainer
        ThemeColorFields.BACKGROUND -> background
        ThemeColorFields.ON_BACKGROUND -> onBackground
        ThemeColorFields.SURFACE -> surface
        ThemeColorFields.ON_SURFACE -> onSurface
        ThemeColorFields.SURFACE_VARIANT -> surfaceVariant
        ThemeColorFields.ON_SURFACE_VARIANT -> onSurfaceVariant
        ThemeColorFields.OUTLINE -> outline
        ThemeColorFields.ERROR -> error
        ThemeColorFields.ON_ERROR -> onError
        else -> null
    }
}

enum class ThemePreset(
    val id: String,
    val label: String,
    val colors: ThemeColors?,
) {
    DEFAULT_DARK("default-dark", "默认深色", null),
    MAPLE("maple", "枫糖", ThemeColors(
        primary = "#D87C5F",
        onPrimary = "#FFFFFF",
        primaryContainer = "#FFDBCF",
        onPrimaryContainer = "#5C1300",
        secondary = "#7D5C4F",
        onSecondary = "#FFFFFF",
        secondaryContainer = "#FFDBCF",
        onSecondaryContainer = "#5C1300",
        tertiary = "#F5C2D7",
        onTertiary = "#3E1A2C",
        tertiaryContainer = "#FFD9E8",
        onTertiaryContainer = "#3E1A2C",
        background = "#FFF8F5",
        onBackground = "#241915",
        surface = "#FFF8F5",
        onSurface = "#241915",
        surfaceVariant = "#FFDBCF",
        onSurfaceVariant = "#5C1300",
        outline = "#7D5C4F",
        error = "#B3261E",
        onError = "#FFFFFF",
    )),
    CHERRY("cherry", "落樱", ThemeColors(
        primary = "#F5C2D7",
        onPrimary = "#3E1A2C",
        primaryContainer = "#FFD9E8",
        onPrimaryContainer = "#3E1A2C",
        secondary = "#7A5A68",
        onSecondary = "#FFFFFF",
        secondaryContainer = "#FFD9E8",
        onSecondaryContainer = "#3E1A2C",
        tertiary = "#7A5A68",
        onTertiary = "#FFFFFF",
        tertiaryContainer = "#FFDBCF",
        onTertiaryContainer = "#5C1300",
        background = "#FFF8FA",
        onBackground = "#2A1A20",
        surface = "#FFF8FA",
        onSurface = "#2A1A20",
        surfaceVariant = "#FFD9E8",
        onSurfaceVariant = "#3E1A2C",
        outline = "#7A5A68",
        error = "#B3261E",
        onError = "#FFFFFF",
    )),
    JADE("jade", "青碧", ThemeColors(
        primary = "#009999",
        onPrimary = "#FFFFFF",
        primaryContainer = "#003737",
        onPrimaryContainer = "#B2FFFF",
        secondary = "#F5C2D7",
        onSecondary = "#3E1A2C",
        secondaryContainer = "#FFD9E8",
        onSecondaryContainer = "#3E1A2C",
        tertiary = "#D87C5F",
        onTertiary = "#FFFFFF",
        tertiaryContainer = "#FFDBCF",
        onTertiaryContainer = "#5C1300",
        background = "#F4FBFA",
        onBackground = "#14201E",
        surface = "#F4FBFA",
        onSurface = "#14201E",
        surfaceVariant = "#D6F0EE",
        onSurfaceVariant = "#14201E",
        outline = "#009999",
        error = "#B3261E",
        onError = "#FFFFFF",
    )),
    JADE_ORANGE("jade-orange", "青橙", ThemeColors(
        primary = "#009999",
        onPrimary = "#FFFFFF",
        primaryContainer = "#FFDBCF",
        onPrimaryContainer = "#5C1300",
        secondary = "#D87C5F",
        onSecondary = "#FFFFFF",
        secondaryContainer = "#FFDBCF",
        onSecondaryContainer = "#5C1300",
        tertiary = "#F5C2D7",
        onTertiary = "#3E1A2C",
        tertiaryContainer = "#FFD9E8",
        onTertiaryContainer = "#3E1A2C",
        background = "#F7F5F2",
        onBackground = "#1F1A17",
        surface = "#F7F5F2",
        onSurface = "#1F1A17",
        surfaceVariant = "#F0E7E0",
        onSurfaceVariant = "#1F1A17",
        outline = "#009999",
        error = "#B3261E",
        onError = "#FFFFFF",
    ));

    companion object {
        fun fromId(id: String): ThemePreset =
            entries.firstOrNull { it.id == id } ?: DEFAULT_DARK
    }
}

@Serializable
data class UserThemeSettings(
    val presetId: String = ThemePreset.DEFAULT_DARK.id,
    val materialYou: Boolean = false,
    val colorOverrides: Map<String, String> = emptyMap(),
) {
    val preset: ThemePreset
        get() = ThemePreset.fromId(presetId)

    fun resolvedColors(): Map<String, String> {
        val base = preset.colors
        return buildMap {
            ThemeColorFields.ALL.forEach { field ->
                val override = ThemeColorParser.normalize(colorOverrides[field])
                val presetColor = base?.value(field)?.let(ThemeColorParser::normalize)
                (override ?: presetColor)?.let { color -> put(field, color) }
            }
        }
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
