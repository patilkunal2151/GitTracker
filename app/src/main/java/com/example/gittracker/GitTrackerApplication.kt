package com.example.gittracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.gittracker.worker.WorkManagerScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GitTrackerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var scheduler: WorkManagerScheduler

    override fun onCreate() {
        super.onCreate()
        // Use KEEP to ensure we don't reset the timer if it's already running
        scheduler.scheduleUpdateCheck(policy = ExistingPeriodicWorkPolicy.KEEP)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
