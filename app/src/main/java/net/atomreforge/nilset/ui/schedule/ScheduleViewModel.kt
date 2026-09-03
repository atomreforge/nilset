package net.atomreforge.nilset.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.atomreforge.nilset.data.repository.CalendarRepository
import net.atomreforge.nilset.data.repository.ScheduleViewRepository
import net.atomreforge.nilset.data.repository.SessionRepository
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val calendarRepository: CalendarRepository,
    private val scheduleViewRepository: ScheduleViewRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionRepository.sessionState.first { it.isLoggedIn || it.isSpecialMode }
            val username = session.username ?: session.userInfo?.username
            if (username.isNullOrBlank()) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "请先登录后查看课表")
                }
            } else {
                initialize(username)
            }
        }
    }

    fun selectWeekday(weekday: Int) {
        if (weekday !in 1..7) return
        _uiState.update { state ->
            state.copy(
                selectedWeekday = weekday,
                selectedCourses = ScheduleCourseSelector.coursesFor(state.records, weekday),
            )
        }
    }

    fun selectMember(username: String) {
        val current = _uiState.value
        val owner = current.currentUsername ?: return
        if (username == current.selectedUsername || current.members.none { it.username == username }) return

        _uiState.update { it.copy(selectedUsername = username, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            scheduleViewRepository.setLastViewedUsername(owner, username)
            loadCalendar(
                ownerUsername = owner,
                selectedUsername = username,
                refreshing = false,
                resetSelection = true,
            )
        }
    }

    fun refresh() {
        val current = _uiState.value
        val owner = current.currentUsername ?: return
        val selected = current.selectedUsername ?: return
        viewModelScope.launch {
            loadCalendar(owner, selected, refreshing = true, resetSelection = false)
        }
    }

    private suspend fun initialize(ownerUsername: String) {
        // 临时占位：服务端成员列表 API 尚未提供，当前成员列表只包含登录用户。
        val members = listOf(ScheduleMember(username = ownerUsername, isSelf = true))
        val restored = scheduleViewRepository.lastViewedUsername(ownerUsername)
            .takeIf { username -> members.any { it.username == username } }
            ?: ownerUsername

        _uiState.update {
            it.copy(
                currentUsername = ownerUsername,
                selectedUsername = restored,
                members = members,
                errorMessage = null,
            )
        }
        loadCalendar(ownerUsername, restored, refreshing = false, resetSelection = true)
    }

    private suspend fun loadCalendar(
        ownerUsername: String,
        selectedUsername: String,
        refreshing: Boolean,
        resetSelection: Boolean,
    ) {
        _uiState.update { state ->
            state.copy(
                isRefreshing = refreshing,
                isLoading = !refreshing,
                errorMessage = null,
            )
        }

        val now = LocalDateTime.now(clock)
        val result = calendarRepository.getCalendar(selectedUsername)
        val records = result.fold(
            onSuccess = { calendar -> calendar.records },
            onFailure = { error ->
                if (error is HttpException && error.code() == 404) {
                    emptyList()
                } else {
                    null
                }
            },
        )

        _uiState.update { state ->
            if (records == null) {
                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = "课表加载失败，请下拉重试",
                )
            } else {
                val selectedWeekday = if (resetSelection) {
                    now.dayOfWeek.value
                } else {
                    state.selectedWeekday
                }
                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    records = records,
                    selectedWeekday = selectedWeekday,
                    selectedCourses = ScheduleCourseSelector.coursesFor(records, selectedWeekday),
                    nextCourse = ScheduleCourseSelector.nextCourse(records, now.toLocalDate(), toMinute(now)),
                    greetingHour = now.hour,
                    errorMessage = null,
                )
            }
        }
    }

    private fun toMinute(now: LocalDateTime): Int = now.hour * 60 + now.minute
}
