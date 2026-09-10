package net.atomreforge.nilset

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import net.atomreforge.nilset.core.logging.LogFileWriter
import net.atomreforge.nilset.data.remote.ServerConnectionManager
import javax.inject.Inject

@HiltAndroidApp
class NilSetApplication : Application() {
    @Inject
    lateinit var logFileWriter: LogFileWriter

    @Inject
    lateinit var serverConnectionManager: ServerConnectionManager

    override fun onCreate() {
        super.onCreate()
        logFileWriter.markSessionStarted()
        serverConnectionManager.startInitialCheckIfNeeded()
    }
}
