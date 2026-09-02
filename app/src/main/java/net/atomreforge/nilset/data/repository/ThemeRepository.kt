package net.atomreforge.nilset.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import dagger.hilt.android.qualifiers.ApplicationContext
import net.atomreforge.nilset.const.ThemeStoreKeys
import net.atomreforge.nilset.core.theme.ThemeColors
import net.atomreforge.nilset.core.theme.ThemeMode
import net.atomreforge.nilset.core.theme.ThemePreset
import net.atomreforge.nilset.core.theme.UserThemeSettings
import net.atomreforge.nilset.data.config.AppConfig
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = ThemeStoreKeys.STORE_NAME,
)

@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json,
    appConfig: AppConfig,
) {
    private val dataStore = context.themeDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val defaultSettings = UserThemeSettings(
        mode = if (appConfig.theme.materialYou) ThemeMode.DYNAMIC else ThemeMode.DARK,
        paletteId = ThemePreset.DEFAULT.id,
    )

    val settings: StateFlow<UserThemeSettings> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            val serialized = preferences[SETTINGS_KEY]
            if (serialized.isNullOrBlank()) {
                defaultSettings
            } else {
                try {
                    json.decodeFromString(UserThemeSettings.serializer(), serialized)
                } catch (exception: SerializationException) {
                    defaultSettings
                } catch (exception: IllegalArgumentException) {
                    defaultSettings
                }
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = defaultSettings,
        )

    val isReady: StateFlow<Boolean> = settings
        .map { true }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    suspend fun selectPalette(paletteId: String) {
        update { current ->
            val inheritedLight = current.customLightColors
                ?: current.palette.lightColors
                ?: UserThemeSettings.DEFAULT_LIGHT_COLORS
            val inheritedDark = current.customDarkColors
                ?: current.palette.darkColors
                ?: UserThemeSettings.DEFAULT_DARK_COLORS
            current.copy(
                paletteId = paletteId,
                customLightColors = if (paletteId == ThemePreset.CUSTOM.id) inheritedLight else current.customLightColors,
                customDarkColors = if (paletteId == ThemePreset.CUSTOM.id) inheritedDark else current.customDarkColors,
            )
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        update { it.copy(mode = mode) }
    }

    suspend fun setCustomColors(colors: ThemeColors, useDark: Boolean) {
        update { current ->
            current.copy(
                paletteId = ThemePreset.CUSTOM.id,
                customLightColors = if (useDark) current.customLightColors else colors,
                customDarkColors = if (useDark) colors else current.customDarkColors,
            )
        }
    }

    suspend fun resetCustomColors(useDark: Boolean) {
        update { current ->
            current.copy(
                paletteId = ThemePreset.CUSTOM.id,
                customLightColors = if (useDark) current.customLightColors else UserThemeSettings.DEFAULT_LIGHT_COLORS,
                customDarkColors = if (useDark) UserThemeSettings.DEFAULT_DARK_COLORS else current.customDarkColors,
            )
        }
    }

    private suspend fun update(transform: (UserThemeSettings) -> UserThemeSettings) {
        val next = transform(settings.value)
        dataStore.edit { preferences ->
            preferences[SETTINGS_KEY] = json.encodeToString(
                UserThemeSettings.serializer(),
                next,
            )
        }
    }

    private companion object {
        val SETTINGS_KEY = stringPreferencesKey(ThemeStoreKeys.SETTINGS)
    }
}
