package net.atomreforge.nilset.data.remote.api

import net.atomreforge.nilset.data.remote.dto.LoginRequest
import net.atomreforge.nilset.data.remote.dto.LoginResponse
import net.atomreforge.nilset.data.remote.dto.MessageResponse
import net.atomreforge.nilset.data.remote.dto.CalendarPutRequest
import net.atomreforge.nilset.data.remote.dto.CalendarResponse
import net.atomreforge.nilset.data.remote.dto.PublicUserInfoResponse
import net.atomreforge.nilset.data.remote.dto.RegisterRequest
import net.atomreforge.nilset.data.remote.dto.RegisterResponse
import net.atomreforge.nilset.data.remote.dto.RefreshTokenRequest
import net.atomreforge.nilset.data.remote.dto.SignOutRequest
import net.atomreforge.nilset.data.remote.dto.UserInfoResponse
import net.atomreforge.nilset.const.ApiExpressions
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.POST

interface DaizyNightApi {

    @POST(ApiExpressions.Endpoint.REGISTER)
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @POST(ApiExpressions.Endpoint.LOGIN)
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST(ApiExpressions.Endpoint.REFRESH_ACCESS_TOKEN)
    suspend fun refreshAccessToken(@Body body: RefreshTokenRequest): LoginResponse

    @GET(ApiExpressions.Endpoint.USER_INFO)
    suspend fun getUserInfo(@Path("username") username: String): UserInfoResponse

    @GET(ApiExpressions.Endpoint.PUBLIC_USER_INFO)
    suspend fun getPublicUserInfo(@Path("username") username: String): PublicUserInfoResponse

    @GET(ApiExpressions.Endpoint.CALENDAR)
    suspend fun getUserCalendar(@Path("username") username: String): CalendarResponse

    @GET(ApiExpressions.Endpoint.PUBLIC_CALENDAR)
    suspend fun getAnyCalendar(@Path("username") username: String): CalendarResponse

    @PUT(ApiExpressions.Endpoint.CALENDAR)
    suspend fun putCalendar(
        @Path("username") username: String,
        @Body body: CalendarPutRequest,
    ): MessageResponse

    @GET(ApiExpressions.Endpoint.HEALTH_DB)
    suspend fun healthDb(): MessageResponse

    @DELETE(ApiExpressions.Endpoint.CALENDAR)
    suspend fun deleteCalendar(@Path("username") username: String): MessageResponse

    @POST(ApiExpressions.Endpoint.SIGN_OUT)
    suspend fun signOut(@Body body: SignOutRequest): MessageResponse
}
