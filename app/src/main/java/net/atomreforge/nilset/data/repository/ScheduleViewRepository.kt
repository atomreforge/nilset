package net.atomreforge.nilset.data.repository

interface ScheduleViewRepository {
    suspend fun lastViewedUsername(ownerUsername: String): String?

    suspend fun setLastViewedUsername(
        ownerUsername: String,
        username: String,
    )
}
