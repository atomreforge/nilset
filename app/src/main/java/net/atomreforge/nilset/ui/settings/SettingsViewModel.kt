package net.atomreforge.nilset.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.atomreforge.nilset.core.theme.ThemeColorParser
import net.atomreforge.nilset.core.theme.UserThemeSettings
import net.atomreforge.nilset.data.repository.ThemeRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
) : ViewModel() {
    val themeSettings: StateFlow<UserThemeSettings> = themeRepository.settings

    fun selectPreset(presetId: String) {
        viewModelScope.launch { themeRepository.selectPreset(presetId) }
    }

    fun setMaterialYou(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setMaterialYou(enabled) }
    }

    fun saveColorOverrides(overrides: Map<String, String>) {
        viewModelScope.launch {
            themeRepository.setColorOverrides(overrides.filterValues { value ->
                ThemeColorParser.normalize(value) != null
            })
        }
    }

    fun clearColorOverrides() {
        viewModelScope.launch { themeRepository.clearColorOverrides() }
    }
}
