package net.atomreforge.nilset.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.atomreforge.nilset.data.remote.api.DaizyNightApi
import net.atomreforge.nilset.di.ApplicationScope
import java.time.Clock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

enum class ServerConnectionStatus {
    UNKNOWN,
    CHECKING,
    CONNECTED,
    CONNECTED_UNAUTHENTICATED,
    DISCONNECTED,
}

data class ServerConnectionUiState(
    val status: ServerConnectionStatus = ServerConnectionStatus.UNKNOWN,
    val canRetry: Boolean = false,
)

@Singleton
class ServerConnectionManager @Inject constructor(
    private val api: DaizyNightApi,
    @param:ApplicationScope private val scope: CoroutineScope,
    private val clock: Clock,
) {
    private val _uiState = MutableStateFlow(ServerConnectionUiState())
    val uiState: StateFlow<ServerConnectionUiState> = _uiState.asStateFlow()

    private val initialCheckStarted = AtomicBoolean(false)
    private val checkInFlight = AtomicBoolean(false)
    private var cooldownJob: Job? = null

    fun startInitialCheckIfNeeded() {
        if (!initialCheckStarted.compareAndSet(false, true)) {
            return
        }
        startCheck()
    }

    fun requestManualRetry() {
        val state = _uiState.value
        if (state.status != ServerConnectionStatus.DISCONNECTED || !state.canRetry) {
            return
        }
        startCheck()
    }

    private fun startCheck() {
        if (!checkInFlight.compareAndSet(false, true)) {
            return
        }
        cooldownJob?.cancel()
        val retryAllowedAtMillis = clock.millis() + RETRY_COOLDOWN_MILLIS
        _uiState.update {
            it.copy(status = ServerConnectionStatus.CHECKING, canRetry = false)
        }

        scope.launch {
            try {
                api.healthDb()
                _uiState.update {
                    it.copy(status = ServerConnectionStatus.CONNECTED, canRetry = false)
                }
            } catch (exception: HttpException) {
                _uiState.update {
                    if (exception.code() == 401) {
                        it.copy(
                            status = ServerConnectionStatus.CONNECTED_UNAUTHENTICATED,
                            canRetry = false,
                        )
                    } else {
                        it.copy(status = ServerConnectionStatus.DISCONNECTED, canRetry = false)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(status = ServerConnectionStatus.DISCONNECTED, canRetry = false)
                }
            } finally {
                checkInFlight.set(false)
            }

            scheduleRetryUnlock(retryAllowedAtMillis)
        }
    }

    private fun scheduleRetryUnlock(retryAllowedAtMillis: Long) {
        cooldownJob?.cancel()
        cooldownJob = scope.launch {
            val remainingMillis = retryAllowedAtMillis - clock.millis()
            if (remainingMillis > 0) {
                delay(remainingMillis)
            }
            _uiState.update {
                if (it.status == ServerConnectionStatus.DISCONNECTED) {
                    it.copy(canRetry = true)
                } else {
                    it
                }
            }
        }
    }

    companion object {
        const val RETRY_COOLDOWN_MILLIS = 10_000L
    }
}
