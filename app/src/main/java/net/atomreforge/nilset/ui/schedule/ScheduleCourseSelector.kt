package net.atomreforge.nilset.ui.schedule

import java.time.LocalDate
import net.atomreforge.nilset.data.calendar.CalendarItem

object ScheduleCourseSelector {

    fun greetingFor(hour: Int): String = when (hour) {
        in 0..4 -> "凌晨好"
        in 5..10 -> "早上好"
        in 11..13 -> "中午好"
        in 14..18 -> "下午好"
        else -> "晚上好"
    }

    fun coursesFor(
        records: List<CalendarItem>,
        weekday: Int,
    ): List<CalendarItem> = records
        .filter { it.weekday == weekday }
        .sortedBy { it.startMin }

    fun nextCourse(
        records: List<CalendarItem>,
        today: LocalDate,
        minuteOfDay: Int,
    ): ScheduleNextCourse {
        if (records.isEmpty()) return ScheduleNextCourse(kind = ScheduleNextCourseKind.EMPTY)

        val todayCourse = coursesFor(records, today.dayOfWeek.value)
        todayCourse.firstOrNull { it.startMin > minuteOfDay }?.let { course ->
            return ScheduleNextCourse(
                kind = ScheduleNextCourseKind.TODAY,
                title = course.title,
                startTime = formatTime(course.startMin),
                endTime = formatTime(course.endMin),
            )
        }

        val tomorrowWeekday = today.dayOfWeek.value.rem(7) + 1
        val tomorrowFirst = coursesFor(records, tomorrowWeekday).firstOrNull()
        return if (tomorrowFirst == null) {
            ScheduleNextCourse(kind = ScheduleNextCourseKind.TODAY_FINISHED)
        } else {
            ScheduleNextCourse(
                kind = ScheduleNextCourseKind.TOMORROW_FIRST,
                title = tomorrowFirst.title,
                startTime = formatTime(tomorrowFirst.startMin),
                endTime = formatTime(tomorrowFirst.endMin),
            )
        }
    }

    fun formatTime(minuteOfDay: Int): String {
        val normalized = (minuteOfDay % (24 * 60) + 24 * 60) % (24 * 60)
        return "%02d:%02d".format(normalized / 60, normalized % 60)
    }
}
