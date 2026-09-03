package net.atomreforge.nilset.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.atomreforge.nilset.const.ApiExpressions

@Serializable
data class CalendarResponse(
    @SerialName(ApiExpressions.Json.CALENDAR_ID) val calendarId: Long,
    @SerialName(ApiExpressions.Json.RECORDS) val records: List<CalendarItemResponse>,
)

@Serializable
data class CalendarItemResponse(
    @SerialName(ApiExpressions.Json.WEEKDAY) val weekday: Int,
    @SerialName(ApiExpressions.Json.START_MIN) val startMin: Int,
    @SerialName(ApiExpressions.Json.END_MIN) val endMin: Int,
    @SerialName(ApiExpressions.Json.TITLE) val title: String,
)

@Serializable
data class CalendarPutRequest(
    @SerialName(ApiExpressions.Json.RECORDS) val records: List<CalendarItemResponse>,
)
