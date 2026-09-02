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
        presetId = ThemePreset.DEFAULT_DARK.id,
        materialYou = appConfig.theme.materialYou,
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

    suspend fun selectPreset(presetId: String) {
        update { it.copy(presetId = presetId) }
    }

    suspend fun setMaterialYou(enabled: Boolean) {
        update { it.copy(materialYou = enabled) }
    }

    suspend fun setColorOverrides(overrides: Map<String, String>) {
        update { it.copy(colorOverrides = overrides) }
    }

    suspend fun clearColorOverrides() {
        update { it.copy(colorOverrides = emptyMap()) }
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
