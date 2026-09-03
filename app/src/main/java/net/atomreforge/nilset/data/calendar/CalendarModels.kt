package net.atomreforge.nilset.data.calendar

data class CalendarItem(
    val weekday: Int,
    val startMin: Int,
    val endMin: Int,
    val title: String,
)

data class UserCalendar(
    val calendarId: Long,
    val records: List<CalendarItem>,
)
