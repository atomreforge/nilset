package net.atomreforge.nilset.const

object ScheduleStoreKeys {
    const val STORE_NAME = "nilset_schedule"
    private const val LAST_VIEWED_USERNAME_PREFIX = "last_viewed_username_"

    fun lastViewedUsername(ownerUsername: String): String =
        LAST_VIEWED_USERNAME_PREFIX + ownerUsername
}
