package com.example.gittracker.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.gittracker.domain.usecase.SyncRepositoriesUseCase
import com.example.gittracker.domain.usecase.SyncResult
import com.example.gittracker.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepositoriesUseCase: SyncRepositoriesUseCase,
    private val scheduler: WorkManagerScheduler,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        android.util.Log.d("UpdateCheckWorker", "Starting work...")
        
        try {
            setForeground(createForegroundInfo())
        } catch (e: Exception) {
            android.util.Log.e("UpdateCheckWorker", "Failed to set foreground", e)
        }

        return try {
            when (val result = syncRepositoriesUseCase()) {
                is SyncResult.Success -> {
                    android.util.Log.d("UpdateCheckWorker", "Work finished successfully")
                    Result.success()
                }
                is SyncResult.RateLimited -> {
                    android.util.Log.w("UpdateCheckWorker", "Rate limited. Scheduling detour for ${result.resetTimeMillis}")
                    scheduler.scheduleDetourSync(result.resetTimeMillis)
                    Result.success()
                }
                is SyncResult.Error -> {
                    android.util.Log.e("UpdateCheckWorker", "Work failed: ${result.message}")
                    if (runAttemptCount < 3) Result.retry() else Result.failure()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UpdateCheckWorker", "Work failed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = notificationHelper.getSyncNotification()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NotificationHelper.SYNC_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NotificationHelper.SYNC_NOTIFICATION_ID, notification)
        }
    }
}
