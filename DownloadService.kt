package com.snapdl.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Menjaga unduhan tetap berjalan saat aplikasi di latar belakang. */
class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel("dl", "Unduhan", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this, 1, build("Menyiapkan unduhan...", 0, true),
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
        )
        job?.cancel()
        job = scope.launch {
            Engine.items.collect { list ->
                val active = list.filter { it.status == St.RUNNING || it.status == St.QUEUED }
                if (active.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    val running = active.filter { it.status == St.RUNNING }
                    val pct = if (running.isEmpty()) 0 else (running.map { it.progress }.average() * 100).toInt()
                    val text = "${running.size} berjalan, ${active.size - running.size} antre"
                    getSystemService(NotificationManager::class.java).notify(1, build(text, pct, running.isEmpty()))
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun build(text: String, pct: Int, indeterminate: Boolean): Notification =
        NotificationCompat.Builder(this, "dl")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Nextube")
            .setContentText(text)
            .setProgress(100, pct, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
