package net.atomreforge.nilset

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import net.atomreforge.nilset.core.logging.LogFileWriter
import javax.inject.Inject

@HiltAndroidApp
class NilSetApplication : Application() {
    @Inject
    lateinit var logFileWriter: LogFileWriter

    override fun onCreate() {
        super.onCreate()
        logFileWriter.markSessionStarted()
    }
}
