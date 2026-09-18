package net.atomreforge.nilset.data.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.atomreforge.nilset.const.ApiExpressions
import net.atomreforge.nilset.const.CalendarRoamingKeys
import net.atomreforge.nilset.data.calendar.CalendarItem
import net.atomreforge.nilset.data.remote.dto.CalendarItemRequest
import net.atomreforge.nilset.data.remote.dto.CalendarItemResponse
import net.atomreforge.nilset.data.remote.dto.CalendarRoamingRequest
import net.atomreforge.nilset.data.remote.dto.CalendarRoamingResponse

@Serializable
private data class CalendarRoamingExtras(
    val teacher: String? = null,
    val classroom: String? = null,
    val note: String? = null,
)

internal object CalendarRoamingMapper {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toPublicModel(response: CalendarItemResponse): CalendarItem = response.toModel(
        CalendarRoamingExtras(),
    )

    fun toPrivateModel(response: CalendarItemResponse): CalendarItem = response.toModel(
        decode(response.roaming),
    )

    fun toRequest(item: CalendarItem): CalendarItemRequest {
        val teacherClassroom = buildJsonObject {
            item.teacher?.let { put(CalendarRoamingKeys.TEACHER, it) }
            item.classroom?.let { put(CalendarRoamingKeys.CLASSROOM, it) }
        }

        return CalendarItemRequest(
            roaming = CalendarRoamingRequest(
                description = if (teacherClassroom.isEmpty()) {
                    ""
                } else {
                    json.encodeToString(JsonObject.serializer(), teacherClassroom)
                },
                annotation = item.note ?: "",
            ),
            weekday = item.weekday,
            startMin = item.startMin,
            endMin = item.endMin,
            title = item.title,
        )
    }

    private fun CalendarItemResponse.toModel(extras: CalendarRoamingExtras) = CalendarItem(
        weekday = weekday,
        startMin = startMin,
        endMin = endMin,
        title = title,
        teacher = extras.teacher,
        classroom = extras.classroom,
        note = extras.note,
    )

    private fun decode(roaming: CalendarRoamingResponse?): CalendarRoamingExtras {
        if (roaming == null) return CalendarRoamingExtras()

        val description = runCatching {
            json.parseToJsonElement(roaming.description).jsonObject
        }.getOrNull()

        return CalendarRoamingExtras(
            teacher = description?.string(CalendarRoamingKeys.TEACHER),
            classroom = description?.string(CalendarRoamingKeys.CLASSROOM),
            note = roaming.annotation.takeIf { it.isNotBlank() },
        )
    }

    private fun JsonObject.string(key: String): String? = runCatching {
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
