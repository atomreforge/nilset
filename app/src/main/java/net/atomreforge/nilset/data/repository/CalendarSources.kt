package net.atomreforge.nilset.data.repository

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalCalendarSource

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RemoteCalendarSource
