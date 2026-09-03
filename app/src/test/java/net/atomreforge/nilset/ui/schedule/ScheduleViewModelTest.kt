package net.atomreforge.nilset.ui.schedule

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.atomreforge.nilset.data.calendar.CalendarItem
import net.atomreforge.nilset.data.calendar.UserCalendar
import net.atomreforge.nilset.data.remote.interceptor.FakeSessionRepository
import net.atomreforge.nilset.data.repository.CalendarRepository
import net.atomreforge.nilset.data.repository.ScheduleViewRepository
import net.atomreforge.nilset.data.session.SessionState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads self schedule with today selected and next course`() = runTest {
        val viewModel = ScheduleViewModel(
            sessionRepository = FakeSessionRepository(
                SessionState(isLoggedIn = true, username = "alice"),
            ),
            calendarRepository = FakeCalendarRepository(
                UserCalendar(
                    calendarId = 1,
                    records = listOf(
                        schedule(weekday = 4, start = 1020),
                        schedule(weekday = 1, start = 480),
                        schedule(weekday = 4, start = 900),
                    ),
                ),
            ),
            scheduleViewRepository = FakeScheduleViewRepository(),
            clock = fixedClock(),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("alice", state.currentUsername)
        assertEquals("alice", state.selectedUsername)
        assertEquals(listOf(ScheduleMember("alice", isSelf = true)), state.members)
        assertEquals(4, state.selectedWeekday)
        assertEquals(listOf(900, 1020), state.selectedCourses.map { it.startMin })
        assertEquals(ScheduleNextCourseKind.TODAY, state.nextCourse.kind)
        assertEquals("17:00", state.nextCourse.startTime)
    }

    @Test
    fun `refresh preserves selected weekday`() = runTest {
        val viewModel = ScheduleViewModel(
            sessionRepository = FakeSessionRepository(
                SessionState(isLoggedIn = true, username = "alice"),
            ),
            calendarRepository = FakeCalendarRepository(
                UserCalendar(
                    calendarId = 1,
                    records = listOf(schedule(weekday = 1, start = 480)),
                ),
            ),
            scheduleViewRepository = FakeScheduleViewRepository(),
            clock = fixedClock(),
        )
        advanceUntilIdle()

        viewModel.selectWeekday(1)
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.selectedWeekday)
        assertEquals(false, state.isRefreshing)
        assertTrue(state.selectedCourses.isNotEmpty())
    }

    private fun fixedClock(): Clock = Clock.fixed(
        Instant.parse("2026-09-03T08:30:00Z"),
        ZoneId.of("Asia/Shanghai"),
    )

    private fun schedule(
        weekday: Int,
        start: Int,
    ) = CalendarItem(
        weekday = weekday,
        startMin = start,
        endMin = start + 60,
        title = if (start == 1020) "数学" else "体育",
    )
}

private class FakeCalendarRepository(
    private val calendar: UserCalendar,
) : CalendarRepository {
    override suspend fun getCalendar(username: String): Result<UserCalendar> =
        Result.success(calendar)

    override suspend fun saveCalendar(
        username: String,
        records: List<CalendarItem>,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun deleteCalendar(username: String): Result<Unit> = Result.success(Unit)
}

private class FakeScheduleViewRepository : ScheduleViewRepository {
    private val values = mutableMapOf<String, String>()

    override suspend fun lastViewedUsername(ownerUsername: String): String? =
        values[ownerUsername]

    override suspend fun setLastViewedUsername(
        ownerUsername: String,
        username: String,
    ) {
        values[ownerUsername] = username
    }
}
