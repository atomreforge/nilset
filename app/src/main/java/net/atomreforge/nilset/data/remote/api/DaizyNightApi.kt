package net.atomreforge.nilset.data.remote.api

import net.atomreforge.nilset.data.remote.dto.LoginRequest
import net.atomreforge.nilset.data.remote.dto.LoginResponse
import net.atomreforge.nilset.data.remote.dto.RegisterRequest
import net.atomreforge.nilset.data.remote.dto.RegisterResponse
import net.atomreforge.nilset.data.remote.dto.RefreshTokenRequest
import net.atomreforge.nilset.data.remote.dto.UserInfoResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface DaizyNightApi {

    @POST("api/v1/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @POST("api/v1/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/v1/refresh-access-token")
    suspend fun refreshAccessToken(@Body body: RefreshTokenRequest): LoginResponse

    @GET("api/v1/user/me")
    suspend fun getUserMe(): UserInfoResponse
}
