package com.example.gittracker.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.gittracker.data.local.SettingsManager
import com.example.gittracker.domain.usecase.MarkAsReadUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadReceiver : BroadcastReceiver() {

    @Inject
    lateinit var markAsReadUseCase: MarkAsReadUseCase

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        if (action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            handleDownloadComplete(context, intent)
        } else {
            handleDownloadClick(context, intent)
        }
    }

    private fun handleDownloadClick(context: Context, intent: Intent) {
        val url = intent.getStringExtra("EXTRA_DOWNLOAD_URL")
        val fileName = intent.getStringExtra("EXTRA_FILE_NAME")
        val repoId = intent.getLongExtra("EXTRA_REPO_ID", -1L)
        
        android.util.Log.d("DownloadReceiver", "Download clicked for repo: $repoId, file: $fileName")

        if (url != null && fileName != null) {
            val downloadId = DownloadUtils.downloadFile(context, url, fileName)
            Toast.makeText(context, "Starting download: $fileName", Toast.LENGTH_SHORT).show()

            if (repoId != -1L) {
                scope.launch {
                    settingsManager.addDownloadMapping(downloadId, repoId)
                    android.util.Log.d("DownloadReceiver", "Mapped download $downloadId to repo $repoId")
                }
            }
        }
    }

    private fun handleDownloadComplete(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) return

        android.util.Log.d("DownloadReceiver", "Download complete: $downloadId")

        val pendingResult = goAsync()
        scope.launch {
            try {
                val repoId = settingsManager.getRepoIdForDownload(downloadId)
                if (repoId != null) {
                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (statusIndex != -1) {
                            val status = cursor.getInt(statusIndex)
                            android.util.Log.d("DownloadReceiver", "Download $downloadId status: $status")
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                android.util.Log.i("DownloadReceiver", "Download successful, marking repo $repoId as read and dismissing notification")
                                markAsReadUseCase(repoId)
                                notificationHelper.dismissUpdateNotification(repoId)
                            } else {
                                android.util.Log.w("DownloadReceiver", "Download $downloadId failed or cancelled")
                            }
                        }
                    }
                    cursor.close()
                    settingsManager.removeDownloadMapping(downloadId)
                }
            } catch (e: Exception) {
                android.util.Log.e("DownloadReceiver", "Error handling download completion", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
