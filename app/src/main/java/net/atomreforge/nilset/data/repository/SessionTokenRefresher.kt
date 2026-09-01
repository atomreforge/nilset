package net.atomreforge.nilset.data.repository

interface SessionTokenRefresher {
    fun refreshAccessTokenBlocking(): String?
}
