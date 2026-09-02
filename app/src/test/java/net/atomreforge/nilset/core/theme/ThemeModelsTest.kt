package net.atomreforge.nilset.core.theme

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModelsTest {

    @Test
    fun `four presets have separate light and dark source colors`() {
        val presets = listOf(
            ThemePreset.DEFAULT,
            ThemePreset.MAPLE,
            ThemePreset.CHERRY,
            ThemePreset.JADE,
        )

        assertEquals(presets, ThemePreset.entries.filterNot { it == ThemePreset.CUSTOM })
        presets.forEach { preset ->
            assertEquals(preset.lightColors, preset.colors(false))
            assertEquals(preset.darkColors, preset.colors(true))
        }
    }

    @Test
    fun `mode controls light dark and dynamic system behavior`() {
        val light = UserThemeSettings(mode = ThemeMode.LIGHT)
        val dark = UserThemeSettings(mode = ThemeMode.DARK)
        val dynamic = UserThemeSettings(mode = ThemeMode.DYNAMIC)

        assertEquals(false, light.usesDarkTheme(true))
        assertEquals(true, dark.usesDarkTheme(false))
        assertEquals(false, dynamic.usesDarkTheme(false))
        assertEquals(true, dynamic.usesDarkTheme(true))
    }

    @Test
    fun `color parser accepts only rgb hex`() {
        assertEquals("#D87C5F", ThemeColorParser.normalize("#d87c5f"))
        assertEquals(null, ThemeColorParser.normalize("D87C5F"))
        assertEquals(null, ThemeColorParser.normalize("#D87C5F0"))
        assertEquals(0xFFD87C5F.toInt(), ThemeColorParser.parseArgb("#D87C5F"))
    }

    @Test
    fun `custom colors are separated by light and dark mode`() {
        val lightColors = ThemeColors("#123456", "#654321", "#EEEEEE", "#FFFFFF")
        val darkColors = ThemeColors("#ABCDEF", "#123456", "#111111", "#222222")
        val settings = UserThemeSettings(
            paletteId = ThemePreset.CUSTOM.id,
            customLightColors = lightColors,
            customDarkColors = darkColors,
        )

        assertEquals(lightColors, settings.effectiveColors(useDark = false))
        assertEquals(darkColors, settings.effectiveColors(useDark = true))
    }

    @Test
    fun `preset resolves source colors by mode`() {
        val settings = UserThemeSettings(paletteId = ThemePreset.JADE.id)

        assertEquals(ThemePreset.JADE.lightColors, settings.effectiveColors(useDark = false))
        assertEquals(ThemePreset.JADE.darkColors, settings.effectiveColors(useDark = true))
    }

    @Test
    fun `theme settings serialize and deserialize`() {
        val settings = UserThemeSettings(
            mode = ThemeMode.DYNAMIC,
            paletteId = ThemePreset.JADE.id,
            customLightColors = ThemeColors("#123456", "#654321", "#EEEEEE", "#FFFFFF"),
            customDarkColors = ThemeColors("#ABCDEF", "#123456", "#111111", "#222222"),
        )
        val serialized = Json.encodeToString(UserThemeSettings.serializer(), settings)

        assertEquals(settings, Json.decodeFromString(UserThemeSettings.serializer(), serialized))
    }
}
