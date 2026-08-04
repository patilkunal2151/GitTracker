package com.example.gittracker.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gittracker.worker.WorkManagerScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduler: WorkManagerScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduler.scheduleUpdateCheck()
        }
    }
}
