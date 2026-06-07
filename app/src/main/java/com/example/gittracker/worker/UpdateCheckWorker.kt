package com.example.gittracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.gittracker.data.repository.AppRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: AppRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        android.util.Log.d("UpdateCheckWorker", "Starting work...")
        return try {
            repository.checkForUpdates()
            android.util.Log.d("UpdateCheckWorker", "Work finished successfully")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("UpdateCheckWorker", "Work failed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
