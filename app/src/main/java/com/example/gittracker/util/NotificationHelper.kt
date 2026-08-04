package com.example.gittracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gittracker.MainActivity
import com.example.gittracker.R
import com.example.gittracker.domain.model.TrackedRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "repo_updates"
        private const val CHANNEL_NAME = "Repository Updates"
        private const val SYNC_CHANNEL_ID = "background_sync"
        private const val SYNC_CHANNEL_NAME = "Background Sync"
        private const val GROUP_KEY = "com.example.gittracker.UPDATES"
        private const val SUMMARY_ID = 0
        const val SYNC_NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val updateChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for tracked repository updates"
            }
            
            val syncChannel = NotificationChannel(
                SYNC_CHANNEL_ID,
                SYNC_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status of background update checks"
            }
            
            notificationManager.createNotificationChannels(listOf(updateChannel, syncChannel))
        }
    }

    fun getSyncNotification(): android.app.Notification {
        return NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle("Checking for updates")
            .setContentText("Refreshing repository data...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    fun showUpdateNotification(repo: TrackedRepo) {
        Log.d("NotificationHelper", "Showing notification for ${repo.repoName}")
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_REPO_ID", repo.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, repo.id.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle("Update for ${repo.repoName}")
            .setContentText("New version: ${repo.latestVersionTag}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setGroup(GROUP_KEY)
            .setAutoCancel(true)
            .build()

        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle("Repository Updates")
            .setContentText("New updates found")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(repo.id.toInt(), notification)
        notificationManager.notify(SUMMARY_ID, summaryNotification)
    }
}
