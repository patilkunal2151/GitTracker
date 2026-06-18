package com.example.gittracker.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import com.example.gittracker.data.local.SettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
            workInfos.find { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
                ?.nextScheduleTimeMillis
        }

    fun scheduleUpdateCheck(
        policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE,
        overrideHours: Int? = null
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            val hours = overrideHours ?: settingsManager.syncFrequency.first()
            val workRequest = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
                .setConstraints(constraints)
                .setInitialDelay(hours.toLong(), TimeUnit.HOURS)
                .addTag("sync_worker")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "UpdateCheckWork",
                policy,
                workRequest
            )
        }
    }
}
