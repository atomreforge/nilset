package net.atomreforge.nilset.core.theme

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModelsTest {

    @Test
    fun `preset colors contain four approved source colors`() {
        assertEquals(
            ThemeColors("#D87C5F", "#7D5C4F", "#FFF8F5", "#FFF8F5"),
            ThemePreset.MAPLE.colors,
        )
        assertEquals(
            ThemeColors("#F5C2D7", "#7A5A68", "#FFF8FA", "#FFF8FA"),
            ThemePreset.CHERRY.colors,
        )
        assertEquals(
            ThemeColors("#009999", "#F5C2D7", "#F4FBFA", "#F4FBFA"),
            ThemePreset.JADE.colors,
        )
        assertEquals(
            ThemeColors("#009999", "#D87C5F", "#F7F5F2", "#F7F5F2"),
            ThemePreset.JADE_ORANGE.colors,
        )
    }

    @Test
    fun `color parser accepts only rgb hex`() {
        assertEquals("#D87C5F", ThemeColorParser.normalize("#d87c5f"))
        assertEquals(null, ThemeColorParser.normalize("D87C5F"))
        assertEquals(null, ThemeColorParser.normalize("#D87C5F0"))
        assertEquals(0xFFD87C5F.toInt(), ThemeColorParser.parseArgb("#D87C5F"))
    }

    @Test
    fun `custom colors take precedence over source palette`() {
        val customColors = ThemeColors("#123456", "#654321", "#111111", "#222222")
        val settings = UserThemeSettings(
            paletteId = ThemePreset.CUSTOM.id,
            customColors = customColors,
        )

        assertEquals(customColors, settings.effectiveColors())
    }

    @Test
    fun `preset resolves source colors`() {
        val settings = UserThemeSettings(paletteId = ThemePreset.JADE.id)

        assertEquals(ThemePreset.JADE.colors, settings.effectiveColors())
    }

    @Test
    fun `theme settings serialize and deserialize`() {
        val settings = UserThemeSettings(
            mode = ThemeMode.DYNAMIC,
            paletteId = ThemePreset.JADE.id,
            customColors = ThemeColors("#123456", "#654321", "#111111", "#222222"),
        )
        val serialized = Json.encodeToString(UserThemeSettings.serializer(), settings)

        assertEquals(settings, Json.decodeFromString(UserThemeSettings.serializer(), serialized))
    }
}
