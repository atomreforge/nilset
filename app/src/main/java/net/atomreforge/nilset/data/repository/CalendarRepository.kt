package net.atomreforge.nilset.data.repository

import net.atomreforge.nilset.data.calendar.CalendarItem
import net.atomreforge.nilset.data.calendar.UserCalendar

interface CalendarRepository {
    suspend fun getCalendar(username: String): Result<UserCalendar>

    suspend fun saveCalendar(
        username: String,
        records: List<CalendarItem>,
    ): Result<Unit>

    suspend fun deleteCalendar(username: String): Result<Unit>
}
