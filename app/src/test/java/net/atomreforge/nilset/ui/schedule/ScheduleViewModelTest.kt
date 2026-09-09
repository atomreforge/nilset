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
import org.junit.Assert.assertNull
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
            localCalendarRepository = FakeCalendarRepository(
                UserCalendar(
                    calendarId = 1,
                    records = listOf(
                        schedule(weekday = 4, start = 1020),
                        schedule(weekday = 1, start = 480),
                        schedule(weekday = 4, start = 900),
                    ),
                ),
            ),
            remoteCalendarRepository = unreachableCalendarRepository(),
            scheduleViewRepository = FakeScheduleViewRepository(),
            clock = fixedClock(),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("alice", state.currentUsername)
        assertEquals("alice", state.selectedUsername)
        assertEquals(listOf(ScheduleMember("alice", isSelf = true)), state.members)
        assertEquals(true, state.isLocalSchedule)
        assertEquals(4, state.selectedWeekday)
        assertEquals(listOf(900, 1020), state.selectedCourses.map { it.startMin })
        assertEquals(ScheduleNextCourseKind.TODAY, state.nextCourse.kind)
        assertEquals("17:00", state.nextCourse.startTime)
    }

    @Test
    fun `missing local self schedule loads as empty`() = runTest {
        val viewModel = ScheduleViewModel(
            sessionRepository = FakeSessionRepository(
                SessionState(isLoggedIn = true, username = "alice"),
            ),
            localCalendarRepository = FakeCalendarRepository(
                UserCalendar(calendarId = 0L, records = emptyList()),
            ),
            remoteCalendarRepository = unreachableCalendarRepository(),
            scheduleViewRepository = FakeScheduleViewRepository(),
            clock = fixedClock(),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isLocalSchedule)
        assertNull(state.errorMessage)
        assertEquals(emptyList<CalendarItem>(), state.records)
    }

    @Test
    fun `create course saves locally and refreshes selected day`() = runTest {
        val localRepository = FakeCalendarRepository(
            UserCalendar(calendarId = 0L, records = emptyList()),
        )
        val remoteRepository = RecordingCalendarRepository()
        val viewModel = ScheduleViewModel(
            sessionRepository = FakeSessionRepository(
                SessionState(isLoggedIn = true, username = "alice"),
            ),
            localCalendarRepository = localRepository,
            remoteCalendarRepository = remoteRepository,
            scheduleViewRepository = FakeScheduleViewRepository(),
            clock = fixedClock(),
        )
        advanceUntilIdle()

        viewModel.showCourseEditor()
        viewModel.saveCourse(
            ScheduleCourseDraft(
                title = "数学",
                weekday = 4,
                startHour = 17,
                startMinute = 0,
                endHour = 18,
                endMinute = 0,
                teacher = "张老师",
                classroom = "A301",
                note = "带计算器",
            ),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isCourseEditorVisible)
        assertNull(state.courseEditorError)
        assertEquals(listOf(1020), state.selectedCourses.map { it.startMin })
        assertEquals("17:00", state.nextCourse.startTime)
        assertEquals(
            listOf(
                CalendarItem(
                    weekday = 4,
                    startMin = 1020,
                    endMin = 1080,
                    title = "数学",
                    teacher = "张老师",
                    classroom = "A301",
                    note = "带计算器",
                ),
            ),
            localRepository.savedRecords,
        )
        assertEquals("alice", remoteRepository.savedUsernames.single())
        assertEquals(localRepository.savedRecords, remoteRepository.savedRecords)
    }

    @Test
    fun `blank course title shows input error`() = runTest {
        val localRepository = FakeCalendarRepository(
            UserCalendar(calendarId = 0L, records = emptyList()),
        )
        val viewModel = ScheduleViewModel(
            sessionRepository = FakeSessionRepository(
                SessionState(isLoggedIn = true, username = "alice"),
            ),
            localCalendarRepository = localRepository,
            remoteCalendarRepository = unreachableCalendarRepository(),
            scheduleViewRepository = FakeScheduleViewRepository(),
            clock = fixedClock(),
        )
        advanceUntilIdle()

        viewModel.showCourseEditor()
        viewModel.saveCourse(
            ScheduleCourseDraft(
                title = " ",
                weekday = 4,
                startHour = 17,
                startMinute = 0,
                endHour = 18,
                endMinute = 0,
                teacher = "",
                classroom = "",
                note = "",
            ),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("请输入课程标题", state.courseEditorError)
        assertEquals(emptyList<CalendarItem>(), localRepository.savedRecords)
    }

    @Test
    fun `edit course replaces matching local record`() = runTest {
        val original = schedule(weekday = 4, start = 900)
        val localRepository = FakeCalendarRepository(
            UserCalendar(calendarId = 0L, records = listOf(original)),
        )
        val remoteRepository = RecordingCalendarRepository()
        val viewModel = ScheduleViewModel(
            sessionRepository = FakeSessionRepository(
                SessionState(isLoggedIn = true, username = "alice"),
            ),
            localCalendarRepository = localRepository,
            remoteCalendarRepository = remoteRepository,
            scheduleViewRepository = FakeScheduleViewRepository(),
            clock = fixedClock(),
        )
        advanceUntilIdle()

        viewModel.showEditCourse(original)
        viewModel.saveCourse(
            ScheduleCourseDraft(
                title = "物理",
                weekday = 4,
                startHour = 10,
                startMinute = 0,
                endHour = 11,
                endMinute = 0,
                teacher = "李老师",
                classroom = "B202",
                note = "",
            ),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("物理", state.selectedCourses.single().title)
        assertEquals(1, localRepository.savedRecords.count { it.title == "物理" })
        assertEquals(0, localRepository.savedRecords.count { it.title == "体育" })
        assertEquals("alice", remoteRepository.savedUsernames.single())
        assertEquals(localRepository.savedRecords, remoteRepository.savedRecords)
    }

    @Test
    fun `delete course removes matching local record`() = runTest {
        val original = schedule(weekday = 4, start = 900)
        val localRepository = FakeCalendarRepository(
            UserCalendar(calendarId = 0L, records = listOf(original)),
        )
        val remoteRepository = RecordingCalendarRepository()
        val viewModel = ScheduleViewModel(
            sessionRepository = FakeSessionRepository(
                SessionState(isLoggedIn = true, username = "alice"),
            ),
            localCalendarRepository = localRepository,
            remoteCalendarRepository = remoteRepository,
            scheduleViewRepository = FakeScheduleViewRepository(),
            clock = fixedClock(),
        )
        advanceUntilIdle()

        viewModel.deleteCourse(original)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(emptyList<CalendarItem>(), state.records)
        assertEquals(emptyList<CalendarItem>(), state.selectedCourses)
        assertEquals(emptyList<CalendarItem>(), localRepository.savedRecords)
        assertEquals("alice", remoteRepository.savedUsernames.single())
        assertEquals(emptyList<CalendarItem>(), remoteRepository.savedRecords)
    }

    @Test
    fun `refresh preserves selected weekday`() = runTest {
        val viewModel = ScheduleViewModel(
            sessionRepository = FakeSessionRepository(
                SessionState(isLoggedIn = true, username = "alice"),
            ),
            localCalendarRepository = FakeCalendarRepository(
                UserCalendar(
                    calendarId = 1,
                    records = listOf(schedule(weekday = 1, start = 480)),
                ),
            ),
            remoteCalendarRepository = unreachableCalendarRepository(),
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
    val savedRecords = mutableListOf<CalendarItem>()

    override suspend fun getCalendar(username: String): Result<UserCalendar> =
        Result.success(calendar)

    override suspend fun saveCalendar(
        username: String,
        records: List<CalendarItem>,
    ): Result<Unit> {
        savedRecords.clear()
        savedRecords.addAll(records)
        return Result.success(Unit)
    }

    override suspend fun deleteCalendar(username: String): Result<Unit> = Result.success(Unit)
}

private class RecordingCalendarRepository : CalendarRepository {
    val savedUsernames = mutableListOf<String>()
    val savedRecords = mutableListOf<CalendarItem>()

    override suspend fun getCalendar(username: String): Result<UserCalendar> =
        Result.success(UserCalendar(calendarId = 0L, records = emptyList()))

    override suspend fun saveCalendar(
        username: String,
        records: List<CalendarItem>,
    ): Result<Unit> {
        savedUsernames.add(username)
        savedRecords.clear()
        savedRecords.addAll(records)
        return Result.success(Unit)
    }

    override suspend fun deleteCalendar(username: String): Result<Unit> = Result.success(Unit)
}

private fun unreachableCalendarRepository(): CalendarRepository {
    return object : CalendarRepository {
        override suspend fun getCalendar(username: String): Result<UserCalendar> {
            throw AssertionError("remote calendar should not be loaded for self")
        }

        override suspend fun saveCalendar(
            username: String,
            records: List<CalendarItem>,
        ): Result<Unit> {
            throw AssertionError("remote calendar should not be saved for self")
        }

        override suspend fun deleteCalendar(username: String): Result<Unit> {
            throw AssertionError("remote calendar should not be deleted for self")
        }
    }
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
