package net.atomreforge.nilset.ui.session

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.atomreforge.nilset.data.repository.SessionRepository
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    sessionRepository: SessionRepository,
) : ViewModel() {
    val isSessionReady = sessionRepository.isSessionReady
    val sessionState = sessionRepository.sessionState
}
