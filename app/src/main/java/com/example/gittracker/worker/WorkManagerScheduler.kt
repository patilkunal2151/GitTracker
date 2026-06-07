package com.example.gittracker.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gittracker.data.local.SettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {
    val nextCheckTime: Flow<Long?> = WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow("UpdateCheckWork")
        .map { workInfos ->
            workInfos.firstOrNull()?.nextScheduleTimeMillis
        }

    fun schedulePeriodicUpdateCheck(policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        MainScope().launch {
            val hours = settingsManager.syncFrequency.first()
            val workRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                hours.toLong(), TimeUnit.HOURS,
                15, TimeUnit.MINUTES // Smaller flex to accommodate 1h frequency
            ).setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "UpdateCheckWork",
                policy,
                workRequest
            )
        }
    }
}
