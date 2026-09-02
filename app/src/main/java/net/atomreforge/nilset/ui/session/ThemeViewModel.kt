package net.atomreforge.nilset.ui.session

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import net.atomreforge.nilset.core.theme.UserThemeSettings
import net.atomreforge.nilset.data.repository.ThemeRepository
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    themeRepository: ThemeRepository,
) : ViewModel() {
    val themeSettings: StateFlow<UserThemeSettings> = themeRepository.settings
    val isThemeReady: StateFlow<Boolean> = themeRepository.isReady
}
