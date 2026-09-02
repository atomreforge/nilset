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
    STANDARD,
    DYNAMIC,
}

enum class ThemePreset(
    val id: String,
    val label: String,
    val colors: ThemeColors?,
) {
    DEFAULT_DARK(
        "default-dark",
        "默认深色",
        ThemeColors(
            primary = "#D87C5F",
            secondary = "#7D5C4F",
            background = "#16161E",
            surface = "#1E1E28",
        ),
    ),
    MAPLE(
        "maple",
        "枫糖",
        ThemeColors(
            primary = "#D87C5F",
            secondary = "#7D5C4F",
            background = "#FFF8F5",
            surface = "#FFF8F5",
        ),
    ),
    CHERRY(
        "cherry",
        "落樱",
        ThemeColors(
            primary = "#F5C2D7",
            secondary = "#7A5A68",
            background = "#FFF8FA",
            surface = "#FFF8FA",
        ),
    ),
    JADE(
        "jade",
        "青碧",
        ThemeColors(
            primary = "#009999",
            secondary = "#F5C2D7",
            background = "#F4FBFA",
            surface = "#F4FBFA",
        ),
    ),
    JADE_ORANGE(
        "jade-orange",
        "青橙",
        ThemeColors(
            primary = "#009999",
            secondary = "#D87C5F",
            background = "#F7F5F2",
            surface = "#F7F5F2",
        ),
    ),
    CUSTOM("custom", "自定义主题", null);

    companion object {
        fun fromId(id: String): ThemePreset =
            entries.firstOrNull { it.id == id } ?: DEFAULT_DARK
    }
}

@Serializable
data class UserThemeSettings(
    val mode: ThemeMode = ThemeMode.STANDARD,
    val paletteId: String = ThemePreset.DEFAULT_DARK.id,
    val customColors: ThemeColors? = null,
) {
    val palette: ThemePreset
        get() = ThemePreset.fromId(paletteId)

    val isDynamic: Boolean
        get() = mode == ThemeMode.DYNAMIC

    fun effectiveColors(): ThemeColors {
        return if (palette == ThemePreset.CUSTOM) {
            customColors ?: DEFAULT_COLORS
        } else {
            palette.colors ?: DEFAULT_COLORS
        }
    }

    companion object {
        val DEFAULT_COLORS = ThemePreset.DEFAULT_DARK.colors!!
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
