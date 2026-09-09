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
import net.atomreforge.nilset.data.calendar.CalendarItem
import net.atomreforge.nilset.data.repository.CalendarRepository
import net.atomreforge.nilset.data.repository.LocalCalendarSource
import net.atomreforge.nilset.data.repository.RemoteCalendarSource
import net.atomreforge.nilset.data.repository.ScheduleViewRepository
import net.atomreforge.nilset.data.repository.SessionRepository
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    @param:LocalCalendarSource private val localCalendarRepository: CalendarRepository,
    @param:RemoteCalendarSource private val remoteCalendarRepository: CalendarRepository,
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
        if (weekday !in 0..6) return
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

    fun showCourseEditor() {
        _uiState.update {
            it.copy(
                isCourseEditorVisible = true,
                editingCourse = null,
                courseEditorError = null,
            )
        }
    }

    fun showEditCourse(course: CalendarItem) {
        val current = _uiState.value
        if (!current.isLocalSchedule || current.isSavingCourse) return
        _uiState.update {
            it.copy(
                isCourseEditorVisible = true,
                editingCourse = course,
                courseEditorError = null,
            )
        }
    }

    fun dismissCourseEditor() {
        if (_uiState.value.isSavingCourse) return
        _uiState.update {
            it.copy(isCourseEditorVisible = false, courseEditorError = null)
        }
    }

    fun saveCourse(
        draft: ScheduleCourseDraft,
    ) {
        val current = _uiState.value
        val owner = current.currentUsername ?: return
        val editingCourse = current.editingCourse
        if (current.selectedUsername != owner || !current.isLocalSchedule || current.isSavingCourse) return

        val normalizedTitle = draft.title.trim()
        val normalizedTeacher = draft.teacher.trim().takeIf { it.isNotEmpty() }
        val normalizedClassroom = draft.classroom.trim().takeIf { it.isNotEmpty() }
        val normalizedNote = draft.note.trim().takeIf { it.isNotEmpty() }
        val startMinute = draft.startHour * 60 + draft.startMinute
        val endMinute = draft.endHour * 60 + draft.endMinute
        val validationError = when {
            draft.startHour !in 0..23 || draft.startMinute !in 0..59 ||
                draft.endHour !in 0..23 || draft.endMinute !in 0..59 -> "请选择完整时间"
            draft.weekday !in 0..6 -> "请选择上课日"
            draft.title.isBlank() -> "请输入课程标题"
            normalizedTitle.length > 255 -> "课程标题需为 1-255 个字符"
            startMinute >= endMinute -> "开始时间必须早于结束时间"
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(courseEditorError = validationError) }
            return
        }
        if (current.records.size >= 200) {
            _uiState.update { it.copy(courseEditorError = "课程数量最多为 200 节") }
            return
        }

        val course = CalendarItem(
            weekday = draft.weekday,
            startMin = startMinute!!,
            endMin = endMinute!!,
            title = normalizedTitle,
            teacher = normalizedTeacher,
            classroom = normalizedClassroom,
            note = normalizedNote,
        )
        _uiState.update { it.copy(isSavingCourse = true, courseEditorError = null) }
        viewModelScope.launch {
            val records = if (editingCourse == null) {
                (current.records + course)
            } else {
                current.records.map { record ->
                    if (record == editingCourse) course else record
                }
            }.sortedWith(
                compareBy({ it.weekday }, { it.startMin }, { it.endMin }, { it.title }),
            )
            persistOwnerCalendar(owner, records).fold(
                onSuccess = {
                    val now = LocalDateTime.now(clock)
                    val selectedWeekday = _uiState.value.selectedWeekday
                    _uiState.update { state ->
                        state.copy(
                            isSavingCourse = false,
                            isCourseEditorVisible = false,
                            editingCourse = null,
                            courseEditorError = null,
                            records = records,
                            selectedCourses = ScheduleCourseSelector.coursesFor(records, selectedWeekday),
                            nextCourse = ScheduleCourseSelector.nextCourse(
                                records,
                                now.toLocalDate(),
                                toMinute(now),
                            ),
                            greetingHour = now.hour,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingCourse = false,
                            courseEditorError = error.message ?: "课程保存失败，请重试",
                        )
                    }
                },
            )
        }
    }

    fun deleteCourse(course: CalendarItem) {
        val current = _uiState.value
        val owner = current.currentUsername ?: return
        if (!current.isLocalSchedule || current.isSavingCourse) return

        viewModelScope.launch {
            val records = current.records - course
            persistOwnerCalendar(owner, records).fold(
                onSuccess = {
                    val now = LocalDateTime.now(clock)
                    val selectedWeekday = current.selectedWeekday
                    _uiState.update { state ->
                        state.copy(
                            records = records,
                            selectedCourses = ScheduleCourseSelector.coursesFor(records, selectedWeekday),
                            nextCourse = ScheduleCourseSelector.nextCourse(
                                records,
                                now.toLocalDate(),
                                toMinute(now),
                            ),
                            greetingHour = now.hour,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(errorMessage = error.message ?: "课程删除失败，请重试")
                    }
                },
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

    private suspend fun persistOwnerCalendar(
        ownerUsername: String,
        records: List<CalendarItem>,
    ): Result<Unit> {
        val remoteResult = remoteCalendarRepository.saveCalendar(ownerUsername, records)
        if (remoteResult.isFailure) {
            return remoteResult
        }

        return localCalendarRepository.saveCalendar(ownerUsername, records)
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
        val result = calendarSource(ownerUsername, selectedUsername)
            .getCalendar(selectedUsername)
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
                    now.dayOfWeek.value % 7
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
                    isLocalSchedule = selectedUsername == state.currentUsername,
                    errorMessage = null,
                )
            }
        }
    }

    private fun calendarSource(
        ownerUsername: String,
        selectedUsername: String,
    ): CalendarRepository {
        return if (selectedUsername == ownerUsername) {
            localCalendarRepository
        } else {
            remoteCalendarRepository
        }
    }

    private fun toMinute(now: LocalDateTime): Int = now.hour * 60 + now.minute
}
