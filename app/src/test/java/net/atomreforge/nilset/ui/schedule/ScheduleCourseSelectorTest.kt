package net.atomreforge.nilset.ui.schedule

import java.time.LocalDate
import net.atomreforge.nilset.data.calendar.CalendarItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleCourseSelectorTest {

    @Test
    fun `greeting uses confirmed inclusive hour boundaries`() {
        assertEquals("凌晨好", ScheduleCourseSelector.greetingFor(0))
        assertEquals("凌晨好", ScheduleCourseSelector.greetingFor(4))
        assertEquals("早上好", ScheduleCourseSelector.greetingFor(5))
        assertEquals("早上好", ScheduleCourseSelector.greetingFor(10))
        assertEquals("中午好", ScheduleCourseSelector.greetingFor(11))
        assertEquals("中午好", ScheduleCourseSelector.greetingFor(13))
        assertEquals("下午好", ScheduleCourseSelector.greetingFor(14))
        assertEquals("下午好", ScheduleCourseSelector.greetingFor(18))
        assertEquals("晚上好", ScheduleCourseSelector.greetingFor(19))
        assertEquals("晚上好", ScheduleCourseSelector.greetingFor(23))
    }

    @Test
    fun `courses are filtered by weekday and sorted by start time`() {
        val courses = ScheduleCourseSelector.coursesFor(
            records = listOf(
                schedule(weekday = 4, start = 1020),
                schedule(weekday = 1, start = 480),
                schedule(weekday = 4, start = 900),
            ),
            weekday = 4,
        )

        assertEquals(listOf(900, 1020), courses.map { it.startMin })
    }

    @Test
    fun `next course is today's first unstarted lesson`() {
        val next = ScheduleCourseSelector.nextCourse(
            records = listOf(
                schedule(weekday = 4, start = 900),
                schedule(weekday = 4, start = 1020),
            ),
            today = LocalDate.of(2026, 9, 3),
            minuteOfDay = 990,
        )

        assertEquals(ScheduleNextCourseKind.TODAY, next.kind)
        assertEquals("数学", next.title)
        assertEquals("17:00", next.startTime)
        assertEquals("18:00", next.endTime)
    }

    @Test
    fun `finished today shows tomorrow first course`() {
        val next = ScheduleCourseSelector.nextCourse(
            records = listOf(
                schedule(weekday = 4, start = 480),
                schedule(weekday = 5, start = 480),
            ),
            today = LocalDate.of(2026, 9, 3),
            minuteOfDay = 990,
        )

        assertEquals(ScheduleNextCourseKind.TOMORROW_FIRST, next.kind)
        assertEquals("体育", next.title)
        assertEquals("08:00", next.startTime)
    }

    @Test
    fun `no records or no tomorrow course uses empty states`() {
        assertEquals(
            ScheduleNextCourseKind.EMPTY,
            ScheduleCourseSelector.nextCourse(emptyList(), LocalDate.of(2026, 9, 3), 480).kind,
        )
        assertEquals(
            ScheduleNextCourseKind.TODAY_FINISHED,
            ScheduleCourseSelector.nextCourse(
                records = listOf(schedule(weekday = 4, start = 480)),
                today = LocalDate.of(2026, 9, 3),
                minuteOfDay = 990,
            ).kind,
        )
    }

    private fun schedule(
        weekday: Int,
        start: Int,
        title: String = if (start == 1020) "数学" else "体育",
    ): CalendarItem = CalendarItem(
        weekday = weekday,
        startMin = start,
        endMin = start + 60,
        title = title,
    )
}
