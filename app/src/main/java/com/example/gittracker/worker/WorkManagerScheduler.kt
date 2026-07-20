package com.example.gittracker.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import com.example.gittracker.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class WorkStatus(
    val nextTime: Long,
    val state: WorkInfo.State,
    val isDetour: Boolean = false
)

@Singleton
class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    @param:ApplicationScope private val externalScope: CoroutineScope
) {
    val workStatus: Flow<WorkStatus?> = combine(
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow("UpdateCheckWork"),
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow("DetourSyncWork")
    ) { periodic, detour ->
        val detourActive = detour.find { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        if (detourActive != null) {
            WorkStatus(detourActive.nextScheduleTimeMillis, detourActive.state, isDetour = true)
        } else {
            periodic.find { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
                ?.let { WorkStatus(it.nextScheduleTimeMillis, it.state, isDetour = false) }
        }
    }

    fun scheduleUpdateCheck(
        policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        externalScope.launch {
            val workRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag("sync_worker")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "UpdateCheckWork",
                policy,
                workRequest
            )
        }
    }

    fun scheduleDetourSync(resetTimeMillis: Long) {
        val delay = (resetTimeMillis - System.currentTimeMillis()).coerceAtLeast(0)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
            .setInitialDelay(delay + 10000, TimeUnit.MILLISECONDS) // Add 10s buffer
            .setConstraints(constraints)
            .addTag("detour_sync")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "DetourSyncWork",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
