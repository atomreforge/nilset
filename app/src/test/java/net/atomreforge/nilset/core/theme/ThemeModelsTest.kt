package net.atomreforge.nilset.core.theme

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModelsTest {

    @Test
    fun `preset colors match approved values`() {
        assertEquals("#D87C5F", ThemePreset.MAPLE.colors?.primary)
        assertEquals("#FFFFFF", ThemePreset.MAPLE.colors?.onPrimary)
        assertEquals("#FFDBCF", ThemePreset.MAPLE.colors?.primaryContainer)
        assertEquals("#5C1300", ThemePreset.MAPLE.colors?.onPrimaryContainer)
        assertEquals("#7D5C4F", ThemePreset.MAPLE.colors?.secondary)
        assertEquals("#FFDBCF", ThemePreset.MAPLE.colors?.secondaryContainer)
        assertEquals("#FFF8F5", ThemePreset.MAPLE.colors?.background)
        assertEquals("#241915", ThemePreset.MAPLE.colors?.onBackground)
        assertEquals("#FFF8F5", ThemePreset.MAPLE.colors?.surface)
        assertEquals("#241915", ThemePreset.MAPLE.colors?.onSurface)

        assertEquals("#F5C2D7", ThemePreset.CHERRY.colors?.primary)
        assertEquals("#3E1A2C", ThemePreset.CHERRY.colors?.onPrimary)
        assertEquals("#FFD9E8", ThemePreset.CHERRY.colors?.primaryContainer)
        assertEquals("#3E1A2C", ThemePreset.CHERRY.colors?.onPrimaryContainer)
        assertEquals("#7A5A68", ThemePreset.CHERRY.colors?.secondary)
        assertEquals("#FFD9E8", ThemePreset.CHERRY.colors?.secondaryContainer)
        assertEquals("#FFF8FA", ThemePreset.CHERRY.colors?.background)
        assertEquals("#2A1A20", ThemePreset.CHERRY.colors?.onBackground)

        assertEquals("#009999", ThemePreset.JADE.colors?.primary)
        assertEquals("#FFFFFF", ThemePreset.JADE.colors?.onPrimary)
        assertEquals("#003737", ThemePreset.JADE.colors?.primaryContainer)
        assertEquals("#B2FFFF", ThemePreset.JADE.colors?.onPrimaryContainer)
        assertEquals("#F5C2D7", ThemePreset.JADE.colors?.secondary)
        assertEquals("#F4FBFA", ThemePreset.JADE.colors?.background)
        assertEquals("#14201E", ThemePreset.JADE.colors?.onBackground)

        assertEquals("#009999", ThemePreset.JADE_ORANGE.colors?.primary)
        assertEquals("#FFFFFF", ThemePreset.JADE_ORANGE.colors?.onPrimary)
        assertEquals("#FFDBCF", ThemePreset.JADE_ORANGE.colors?.primaryContainer)
        assertEquals("#5C1300", ThemePreset.JADE_ORANGE.colors?.onPrimaryContainer)
        assertEquals("#D87C5F", ThemePreset.JADE_ORANGE.colors?.secondary)
        assertEquals("#F5C2D7", ThemePreset.JADE_ORANGE.colors?.tertiary)
        assertEquals("#F7F5F2", ThemePreset.JADE_ORANGE.colors?.background)
        assertEquals("#1F1A17", ThemePreset.JADE_ORANGE.colors?.onBackground)
    }

    @Test
    fun `color parser accepts only rgb hex`() {
        assertEquals("#D87C5F", ThemeColorParser.normalize("#d87c5f"))
        assertEquals(null, ThemeColorParser.normalize("D87C5F"))
        assertEquals(null, ThemeColorParser.normalize("#D87C5F0"))
        assertEquals(0xFFD87C5F.toInt(), ThemeColorParser.parseArgb("#D87C5F"))
    }

    @Test
    fun `color overrides take precedence over preset`() {
        val settings = UserThemeSettings(
            presetId = ThemePreset.CHERRY.id,
            colorOverrides = mapOf(ThemeColorFields.PRIMARY to "#123456"),
        )

        assertEquals("#123456", settings.resolvedColors()[ThemeColorFields.PRIMARY])
        assertEquals("#3E1A2C", settings.resolvedColors()[ThemeColorFields.ON_PRIMARY])
    }

    @Test
    fun `theme settings serialize and deserialize`() {
        val settings = UserThemeSettings(
            presetId = ThemePreset.JADE.id,
            materialYou = true,
            colorOverrides = mapOf(ThemeColorFields.PRIMARY to "#123456"),
        )
        val serialized = Json.encodeToString(UserThemeSettings.serializer(), settings)

        assertEquals(settings, Json.decodeFromString(UserThemeSettings.serializer(), serialized))
    }
}
