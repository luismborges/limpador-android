package com.luisborges.limpadorandroid.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.luisborges.limpadorandroid.utils.FileUtils

class ScheduledCleanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val filesMap = FileUtils.scanStorage()
            val allJunk = filesMap.values.flatten()
            FileUtils.deleteFiles(allJunk)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
