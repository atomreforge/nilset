package net.atomreforge.nilset.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.atomreforge.nilset.const.ApiExpressions

@Serializable
data class RegisterRequest(
    @SerialName(ApiExpressions.Json.REGISTER_WAY) val registerWay: String = "legacy",
    val username: String,
    val nickname: String,
    val password: String,
    @SerialName(ApiExpressions.Json.REGISTER_CODE) val registerCode: String,
)

@Serializable
data class LoginRequest(
    @SerialName(ApiExpressions.Json.LOGIN_WAY) val loginWay: String = "legacy",
    val username: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    @SerialName(ApiExpressions.Json.ACCESS_TOKEN) val accessToken: String,
    @SerialName(ApiExpressions.Json.REFRESH_TOKEN) val refreshToken: String,
)

@Serializable
data class RefreshTokenRequest(
    @SerialName(ApiExpressions.Json.REFRESH_TOKEN) val refreshToken: String,
)

@Serializable
data class RegisterResponse(
    val message: String,
)

@Serializable
data class UserInfoResponse(
    @SerialName(ApiExpressions.Json.USER_UID) val uid: Long,
    val username: String,
    val nickname: String,
    val email: String? = null,
    @SerialName(ApiExpressions.Json.USER_REGISTER_TIME) val registerTime: String? = null,
    @SerialName(ApiExpressions.Json.USER_ROLE) val role: String = "user",
    @SerialName(ApiExpressions.Json.USER_GITHUB_ID) val githubId: String? = null,
    @SerialName(ApiExpressions.Json.USER_GITHUB_LOGIN) val githubLogin: String? = null,
)

@Serializable
data class MessageResponse(
    @SerialName(ApiExpressions.Json.MESSAGE) val message: String,
)

@Serializable
data class SignOutRequest(
    @SerialName(ApiExpressions.Json.REFRESH_TOKEN) val refreshToken: String,
    @SerialName(ApiExpressions.Json.SESSION) val session: List<String> = emptyList(),
)
