package net.atomreforge.nilset.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.atomreforge.nilset.data.calendar.CalendarItem
import net.atomreforge.nilset.data.calendar.UserCalendar
import net.atomreforge.nilset.data.remote.ServerConnectionManager
import net.atomreforge.nilset.data.remote.ServerConnectionStatus
import net.atomreforge.nilset.di.ApplicationScope
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

interface CalendarSyncManager {

    suspend fun syncIfConnected(ownerUsername: String): Result<Unit>
}

@Singleton
class LocalFirstCalendarSyncManager @Inject constructor(
    @param:LocalCalendarSource private val localCalendarRepository: CalendarRepository,
    @param:PrivateCalendarSource private val remoteCalendarRepository: CalendarRepository,
    private val sessionRepository: SessionRepository,
    private val serverConnectionManager: ServerConnectionManager,
    @param:ApplicationScope private val scope: CoroutineScope,
) : CalendarSyncManager {

    private val syncMutex = Mutex()

    init {
        scope.launch {
            combine(
                serverConnectionManager.uiState,
                sessionRepository.sessionState,
            ) { connection, session ->
                connection.status to session
            }.distinctUntilChanged().collect { (status, session) ->
                val username = session.username ?: session.userInfo?.username
                if (!isConnectionUsable(status, session.isLoggedIn) || username.isNullOrBlank()) {
                    return@collect
                }
                syncIfConnected(username)
            }
        }
    }

    override suspend fun syncIfConnected(ownerUsername: String): Result<Unit> = syncMutex.withLock {
        val session = sessionRepository.sessionState.value
        val username = session.username ?: session.userInfo?.username
        val status = serverConnectionManager.uiState.value.status
        if (!session.isLoggedIn || username != ownerUsername ||
            !isConnectionUsable(status, session.isLoggedIn)
        ) {
            return Result.success(Unit)
        }

        val localResult = localCalendarRepository.getCalendar(ownerUsername)
        val localCalendar = localResult.getOrElse { return Result.failure(it) }
        val remoteCalendar = remoteCalendarRepository.getCalendar(ownerUsername).fold(
            onSuccess = { calendar -> calendar },
            onFailure = { error ->
                if (error is HttpException && error.code() == 404) {
                    UserCalendar(calendarId = 0L, records = emptyList())
                } else {
                    return Result.failure(error)
                }
            },
        )

        if (remoteSignature(localCalendar.records) == remoteSignature(remoteCalendar.records)) {
            return Result.success(Unit)
        }

        remoteCalendarRepository.saveCalendar(ownerUsername, localCalendar.records)
    }

    private fun remoteSignature(records: List<CalendarItem>): List<CalendarItem> =
        records
            .map { record ->
                record.copy(
                    teacher = null,
                    classroom = null,
                    note = null,
                )
            }
            .sortedWith(
                compareBy({ it.weekday }, { it.startMin }, { it.endMin }, { it.title }),
            )
}

private fun isConnectionUsable(
    status: ServerConnectionStatus,
    isLoggedIn: Boolean,
): Boolean = status == ServerConnectionStatus.CONNECTED ||
    (status == ServerConnectionStatus.CONNECTED_UNAUTHENTICATED && isLoggedIn)
