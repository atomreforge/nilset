package net.atomreforge.nilset.const

object ScheduleStoreKeys {
    const val STORE_NAME = "nilset_schedule"
    private const val LAST_VIEWED_USERNAME_PREFIX = "last_viewed_username_"
    private const val LOCAL_CALENDAR_PREFIX = "local_calendar_"

    fun lastViewedUsername(ownerUsername: String): String =
        LAST_VIEWED_USERNAME_PREFIX + ownerUsername

    fun localCalendar(ownerUsername: String): String =
        LOCAL_CALENDAR_PREFIX + ownerUsername
}
