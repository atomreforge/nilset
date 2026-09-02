package net.atomreforge.nilset.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.atomreforge.nilset.core.theme.ThemeColorFields
import net.atomreforge.nilset.core.theme.ThemeColorParser
import net.atomreforge.nilset.core.theme.ThemeColors
import net.atomreforge.nilset.core.theme.ThemeMode
import net.atomreforge.nilset.core.theme.UserThemeSettings
import net.atomreforge.nilset.data.repository.ThemeRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
) : ViewModel() {
    val themeSettings: StateFlow<UserThemeSettings> = themeRepository.settings

    fun selectPalette(paletteId: String) {
        viewModelScope.launch { themeRepository.selectPalette(paletteId) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themeRepository.setThemeMode(mode) }
    }

    fun setCardBorders(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setCardBorders(enabled) }
    }

    fun setTextScaleEnabled(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setTextScaleEnabled(enabled) }
    }

    fun setTextScale(scale: Float) {
        viewModelScope.launch { themeRepository.setTextScale(scale) }
    }

    fun setUiScaleEnabled(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setUiScaleEnabled(enabled) }
    }

    fun setUiScale(scale: Float) {
        viewModelScope.launch { themeRepository.setUiScale(scale) }
    }

    fun saveCustomColors(values: Map<String, String>, useDark: Boolean) {
        viewModelScope.launch {
            val colors = ThemeColorFields.ALL.mapNotNull { field ->
                ThemeColorParser.normalize(values[field])?.let { normalized ->
                    field to normalized
                }
            }.toMap()

            themeRepository.setCustomColors(
                colors = ThemeColors(
                    primary = colors.getValue(ThemeColorFields.PRIMARY),
                    secondary = colors.getValue(ThemeColorFields.SECONDARY),
                    background = colors.getValue(ThemeColorFields.BACKGROUND),
                    surface = colors.getValue(ThemeColorFields.SURFACE),
                ),
                useDark = useDark,
            )
        }
    }

    fun resetCustomColors(useDark: Boolean) {
        viewModelScope.launch { themeRepository.resetCustomColors(useDark) }
    }

    fun saveCustomBackground(color: String, useDark: Boolean) {
        val normalizedColor = ThemeColorParser.normalize(color) ?: return
        viewModelScope.launch {
            themeRepository.setCustomBackground(normalizedColor, useDark)
        }
    }

    fun resetCustomBackground(useDark: Boolean) {
        viewModelScope.launch { themeRepository.resetCustomBackground(useDark) }
    }
}
