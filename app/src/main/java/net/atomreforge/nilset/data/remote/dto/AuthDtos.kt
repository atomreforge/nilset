package net.atomreforge.nilset.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    @SerialName("registerway") val registerWay: String = "legacy",
    val username: String,
    val nickname: String,
    val password: String,
    @SerialName("registercode") val registerCode: String,
)

@Serializable
data class LoginRequest(
    @SerialName("loginway") val loginWay: String = "legacy",
    val username: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class RegisterResponse(
    val message: String,
)

@Serializable
data class UserInfoResponse(
    val uid: Long,
    val username: String,
    val nickname: String,
    val email: String? = null,
    @SerialName("register_time") val registerTime: String? = null,
    val role: String = "user",
    @SerialName("github_id") val githubId: String? = null,
    @SerialName("github_login") val githubLogin: String? = null,
)
