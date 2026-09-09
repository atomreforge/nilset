package net.atomreforge.nilset.ui.schedule

import net.atomreforge.nilset.data.calendar.CalendarItem

data class ScheduleMember(
    val username: String,
    val isSelf: Boolean,
)

data class ScheduleCourseDraft(
    val title: String,
    val weekday: Int,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val teacher: String,
    val classroom: String,
    val note: String,
)

enum class ScheduleNextCourseKind {
    EMPTY,
    TODAY,
    TOMORROW_FIRST,
    TODAY_FINISHED,
}

data class ScheduleNextCourse(
    val kind: ScheduleNextCourseKind = ScheduleNextCourseKind.EMPTY,
    val title: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
)

data class ScheduleUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val currentUsername: String? = null,
    val selectedUsername: String? = null,
    val members: List<ScheduleMember> = emptyList(),
    val records: List<CalendarItem> = emptyList(),
    val selectedWeekday: Int = 1,
    val selectedCourses: List<CalendarItem> = emptyList(),
    val nextCourse: ScheduleNextCourse = ScheduleNextCourse(),
    val greetingHour: Int = 12,
    val isLocalSchedule: Boolean = false,
    val isCourseEditorVisible: Boolean = false,
    val editingCourse: CalendarItem? = null,
    val isSavingCourse: Boolean = false,
    val courseEditorError: String? = null,
    val errorMessage: String? = null,
)
