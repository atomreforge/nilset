package net.atomreforge.nilset.data.config

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val main: MainConfig = MainConfig(),
    val api: ApiConfig,
    val auth: AuthConfig = AuthConfig(),
    val log: LogConfig = LogConfig(),
    val theme: ThemeConfig = ThemeConfig(),
)

@Serializable
data class MainConfig(
    val isDebugMode: Boolean = true,
)

@Serializable
data class ApiConfig(
    val baseUrl: String,
    val apiPrefix: String = "/api/v1",
    val timeouts: TimeoutConfig = TimeoutConfig(),
)

@Serializable
data class TimeoutConfig(
    val connect: String = "10s",
    val read: String = "15s",
)

@Serializable
data class AuthConfig(
    val autoRefresh: Boolean = false,
)

@Serializable
data class LogConfig(
    val isHttpLoggingEnabled: Boolean = true,
)

@Serializable
data class ThemeConfig(
    val materialYou: Boolean = false,
)
