package net.atomreforge.nilset.core.theme

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModelsTest {

    @Test
    fun `four presets have separate light and dark source colors`() {
        val presets = listOf(
            ThemePreset.MAPLE,
            ThemePreset.CHERRY,
            ThemePreset.JADE,
            ThemePreset.DYNAMIC,
        )

        assertEquals(presets + ThemePreset.CUSTOM, ThemePreset.entries)
        assertEquals(ThemePreset.MAPLE.id, UserThemeSettings().paletteId)
        presets.forEach { preset ->
            assertEquals(preset.lightColors, preset.colors(false))
            assertEquals(preset.darkColors, preset.colors(true))
            listOf(preset.lightColors, preset.darkColors).forEach { colors ->
                assertEquals(false, colors!!.background == colors.surface)
            }
        }
        assertEquals(true, UserThemeSettings().showCardBorders)
    }

    @Test
    fun `mode controls light and dark source colors`() {
        val light = UserThemeSettings(mode = ThemeMode.LIGHT)
        val dark = UserThemeSettings(mode = ThemeMode.DARK)

        assertEquals(false, light.usesDarkTheme())
        assertEquals(true, dark.usesDarkTheme())
        assertEquals(true, UserThemeSettings(paletteId = ThemePreset.DYNAMIC.id).isDynamic)
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
            mode = ThemeMode.DARK,
            paletteId = ThemePreset.JADE.id,
            showCardBorders = false,
            customLightColors = ThemeColors("#123456", "#654321", "#EEEEEE", "#FFFFFF"),
            customDarkColors = ThemeColors("#ABCDEF", "#123456", "#111111", "#222222"),
        )
        val serialized = Json.encodeToString(UserThemeSettings.serializer(), settings)

        assertEquals(settings, Json.decodeFromString(UserThemeSettings.serializer(), serialized))
    }
}
