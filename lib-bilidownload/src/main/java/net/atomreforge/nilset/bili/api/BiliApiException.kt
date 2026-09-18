package net.atomreforge.nilset.bili.api

class BiliApiException(
    val code: Int,
    override val message: String,
) : Exception(message)