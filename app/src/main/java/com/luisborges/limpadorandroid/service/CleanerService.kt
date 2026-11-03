package com.luisborges.limpadorandroid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.luisborges.limpadorandroid.R
import com.luisborges.limpadorandroid.utils.FileUtils
import kotlinx.coroutines.*

class CleanerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val CHANNEL_ID = "cleaner_service_channel"
    private val NOTIF_ID = 1001

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Limpando arquivos..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            try {
                val filesMap = FileUtils.scanStorage { msg ->
                    updateNotification(msg)
                }
                val allJunk = filesMap.values.flatten()
                val (deleted, freed) = FileUtils.deleteFiles(allJunk)
                updateNotification("Limpeza concluída: ${freed / 1024 / 1024}MB liberados ($deleted arquivos)")
            } catch (e: Exception) {
                updateNotification("Erro na limpeza: ${e.message}")
            } finally {
                delay(3000)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Limpador Android",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Serviço de limpeza em background"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Limpador Android")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_clean)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        val notifManager = getSystemService(NotificationManager::class.java)
        notifManager.notify(NOTIF_ID, buildNotification(text))
    }
}
