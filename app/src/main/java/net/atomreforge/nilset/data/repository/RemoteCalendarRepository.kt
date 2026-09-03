package net.atomreforge.nilset.data.repository

import kotlinx.coroutines.CancellationException
import net.atomreforge.nilset.data.calendar.CalendarItem
import net.atomreforge.nilset.data.calendar.UserCalendar
import net.atomreforge.nilset.data.remote.api.DaizyNightApi
import net.atomreforge.nilset.data.remote.dto.CalendarItemResponse
import net.atomreforge.nilset.data.remote.dto.CalendarPutRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteCalendarRepository @Inject constructor(
    private val api: DaizyNightApi,
) : CalendarRepository {

    override suspend fun getCalendar(username: String): Result<UserCalendar> = runCatching {
        val calendar = api.getCalendar(username)
        UserCalendar(
            calendarId = calendar.calendarId,
            records = calendar.records.map { it.toModel() },
        )
    }.recoverCancellation()

    override suspend fun saveCalendar(
        username: String,
        records: List<CalendarItem>,
    ): Result<Unit> = runCatching {
        api.putCalendar(
            username = username,
            body = CalendarPutRequest(records = records.map { it.toResponse() }),
        )
        Unit
    }.recoverCancellation()

    override suspend fun deleteCalendar(username: String): Result<Unit> = runCatching {
        api.deleteCalendar(username)
        Unit
    }.recoverCancellation()

    private fun CalendarItemResponse.toModel() = CalendarItem(
        weekday = weekday,
        startMin = startMin,
        endMin = endMin,
        title = title,
    )

    private fun CalendarItem.toResponse() = CalendarItemResponse(
        weekday = weekday,
        startMin = startMin,
        endMin = endMin,
        title = title,
    )

    private fun <T> Result<T>.recoverCancellation(): Result<T> {
        exceptionOrNull()?.let { exception ->
            if (exception is CancellationException) throw exception
        }
        return this
    }
}
