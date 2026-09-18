package net.atomreforge.nilset.data.remote.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.atomreforge.nilset.const.ApiExpressions

@Serializable
data class CalendarResponse(
    @SerialName(ApiExpressions.Json.USER_UID) val uid: Long? = null,
    @SerialName(ApiExpressions.Json.CALENDAR_ID) val calendarId: Long,
    @SerialName(ApiExpressions.Json.RECORDS) val records: List<CalendarItemResponse>,
)

@Serializable
data class CalendarItemResponse(
    @SerialName(ApiExpressions.Json.CALENDAR_ID) val calendarId: Long? = null,
    @SerialName(ApiExpressions.Json.WEEKDAY) val weekday: Int,
    @SerialName(ApiExpressions.Json.START_MIN) val startMin: Int,
    @SerialName(ApiExpressions.Json.END_MIN) val endMin: Int,
    @SerialName(ApiExpressions.Json.TITLE) val title: String,
    @SerialName(ApiExpressions.Json.ROAMING) val roaming: CalendarRoamingResponse? = null,
)

@Serializable
data class CalendarRoamingResponse(
    @SerialName(ApiExpressions.Json.DESCRIPTION) val description: String = "",
    @SerialName(ApiExpressions.Json.ANNOTATION) val annotation: String = "",
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CalendarRoamingRequest(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName(ApiExpressions.Json.DESCRIPTION) val description: String = "",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName(ApiExpressions.Json.ANNOTATION) val annotation: String = "",
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CalendarItemRequest(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName(ApiExpressions.Json.ROAMING) val roaming: CalendarRoamingRequest = CalendarRoamingRequest(),
    @SerialName(ApiExpressions.Json.WEEKDAY) val weekday: Int,
    @SerialName(ApiExpressions.Json.START_MIN) val startMin: Int,
    @SerialName(ApiExpressions.Json.END_MIN) val endMin: Int,
    @SerialName(ApiExpressions.Json.TITLE) val title: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CalendarPutRequest(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName(ApiExpressions.Json.ROAMING) val roaming: CalendarRoamingRequest = CalendarRoamingRequest(),
    @SerialName(ApiExpressions.Json.RECORDS) val records: List<CalendarItemRequest>,
)
