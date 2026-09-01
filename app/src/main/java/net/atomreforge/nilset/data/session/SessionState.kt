package net.atomreforge.nilset.data.session

data class SessionState(
    val isLoggedIn: Boolean = false,
    val isSpecialMode: Boolean = false,
    val username: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userInfo: UserInfo? = null,
)

data class UserInfo(
    val uid: Long,
    val username: String,
    val nickname: String,
    val email: String? = null,
    val registerTime: String? = null,
    val role: String = "user",
)
