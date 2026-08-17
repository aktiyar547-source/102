package com.middleeastcontainer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import java.util.concurrent.Executors
import com.middleeastcontainer.data.sync.UploadScheduler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/** Application entry point: logging, Hilt-aware WorkManager, housekeeping schedule. */
@HiltAndroidApp
class MecrcApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var uploadScheduler: UploadScheduler

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        // Q7: ensure the daily purge of old, uploaded inspections is scheduled.
        uploadScheduler.ensureHousekeepingScheduled()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            // Uploads are memory-heavy (decoded bitmap + Base64 strings + request
            // buffer ~13 MB each). WorkManager would otherwise run 3-4 at once and
            // risk OutOfMemory on budget devices, so we run them one at a time.
            .setExecutor(Executors.newFixedThreadPool(1))
            .build()
}
