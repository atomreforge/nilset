package net.atomreforge.nilset.data.remote.api

import net.atomreforge.nilset.data.remote.dto.LoginRequest
import net.atomreforge.nilset.data.remote.dto.LoginResponse
import net.atomreforge.nilset.data.remote.dto.MessageResponse
import net.atomreforge.nilset.data.remote.dto.RegisterRequest
import net.atomreforge.nilset.data.remote.dto.RegisterResponse
import net.atomreforge.nilset.data.remote.dto.RefreshTokenRequest
import net.atomreforge.nilset.data.remote.dto.SignOutRequest
import net.atomreforge.nilset.data.remote.dto.UserInfoResponse
import net.atomreforge.nilset.const.ApiExpressions
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST

interface DaizyNightApi {

    @POST(ApiExpressions.Endpoint.REGISTER)
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @POST(ApiExpressions.Endpoint.LOGIN)
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST(ApiExpressions.Endpoint.REFRESH_ACCESS_TOKEN)
    suspend fun refreshAccessToken(@Body body: RefreshTokenRequest): LoginResponse

    @GET(ApiExpressions.Endpoint.USER_ME)
    suspend fun getUserMe(@Path("username") username: String): UserInfoResponse

    @POST(ApiExpressions.Endpoint.SIGN_OUT)
    suspend fun signOut(@Body body: SignOutRequest): MessageResponse
}
