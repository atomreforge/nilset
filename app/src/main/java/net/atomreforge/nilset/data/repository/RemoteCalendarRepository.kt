package net.atomreforge.nilset.data.repository

import kotlinx.coroutines.CancellationException
import net.atomreforge.nilset.data.calendar.CalendarItem
import net.atomreforge.nilset.data.calendar.UserCalendar
import net.atomreforge.nilset.data.remote.api.DaizyNightApi
import net.atomreforge.nilset.data.remote.dto.CalendarPutRequest
import javax.inject.Inject
import javax.inject.Singleton

open class RemoteCalendarRepository @Inject constructor(
    protected val api: DaizyNightApi,
) : CalendarRepository {

    final override suspend fun getCalendar(username: String): Result<UserCalendar> =
        fetchCalendar(username)

    protected open suspend fun fetchCalendar(username: String): Result<UserCalendar> =
        runCatching {
            val calendar = api.getAnyCalendar(username)
            UserCalendar(
                calendarId = calendar.calendarId,
                records = calendar.records.map(CalendarRoamingMapper::toPublicModel),
            )
        }.recoverCancellation()

    override suspend fun saveCalendar(
        username: String,
        records: List<CalendarItem>,
    ): Result<Unit> = runCatching {
        api.putCalendar(
            username = username,
            body = CalendarPutRequest(records = records.map(CalendarRoamingMapper::toRequest)),
        )
        Unit
    }.recoverCancellation()

    override suspend fun deleteCalendar(username: String): Result<Unit> = runCatching {
        api.deleteCalendar(username)
        Unit
    }.recoverCancellation()

    protected fun <T> Result<T>.recoverCancellation(): Result<T> {
        exceptionOrNull()?.let { exception ->
            if (exception is CancellationException) throw exception
        }
        return this
    }
}

@Singleton
class PrivateRemoteCalendarRepository @Inject constructor(
    api: DaizyNightApi,
) : RemoteCalendarRepository(api) {

    override suspend fun fetchCalendar(username: String): Result<UserCalendar> =
        runCatching {
            val calendar = api.getUserCalendar(username)
            UserCalendar(
                calendarId = calendar.calendarId,
                records = calendar.records.map(CalendarRoamingMapper::toPrivateModel),
            )
        }.recoverCancellation()
}
