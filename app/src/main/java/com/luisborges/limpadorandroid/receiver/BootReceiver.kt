package com.luisborges.limpadorandroid.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.luisborges.limpadorandroid.worker.ScheduledCleanWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            schedulePeriodicCleaning(context)
        }
    }

    companion object {
        fun schedulePeriodicCleaning(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val work = PeriodicWorkRequestBuilder<ScheduledCleanWorker>(
                3, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "scheduled_clean",
                ExistingPeriodicWorkPolicy.KEEP,
                work
            )
        }
    }
}
