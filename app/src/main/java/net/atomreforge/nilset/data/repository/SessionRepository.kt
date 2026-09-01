package net.atomreforge.nilset.data.repository

import kotlinx.coroutines.flow.StateFlow
import net.atomreforge.nilset.data.session.SessionState
import net.atomreforge.nilset.data.session.UserInfo

interface SessionRepository {

    val sessionState: StateFlow<SessionState>
    val isSessionReady: StateFlow<Boolean>

    suspend fun login(username: String, password: String): Result<Unit>

    suspend fun enterLocalSession(username: String)

    suspend fun register(
        username: String,
        nickname: String,
        password: String,
        registerCode: String,
    ): Result<Unit>

    suspend fun fetchUserInfo(): Result<UserInfo>

    suspend fun logout(): Result<Unit>

    fun enterSpecialMode()

    fun clear()
}
