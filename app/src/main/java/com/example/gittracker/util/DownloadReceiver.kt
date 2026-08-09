package com.example.gittracker.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val url = intent.getStringExtra("EXTRA_DOWNLOAD_URL")
        val fileName = intent.getStringExtra("EXTRA_FILE_NAME")
        
        if (url != null && fileName != null) {
            DownloadUtils.downloadFile(context, url, fileName)
            Toast.makeText(context, "Starting download: $fileName", Toast.LENGTH_SHORT).show()
        }
    }
}
