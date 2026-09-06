package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Foreground service to keep the app alive and maintain network priority
 * during deep strategic analysis in the background.
 */
class AnalysisService : Service() {
    companion object {
        private const val CHANNEL_ID = "analysis_service_channel"
        private const val NOTIFICATION_ID = 99123
        
        @Volatile
        var isServiceRunning = false

        fun start(context: Context) {
            try {
                val intent = Intent(context, AnalysisService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                android.util.Log.w("AnalysisService", "Could not start foreground service: ${t.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, AnalysisService::class.java).apply {
                    action = "STOP_SERVICE"
                }
                context.startService(intent)
            } catch (t: Throwable) {
                android.util.Log.w("AnalysisService", "Could not stop foreground service: ${t.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP_SERVICE") {
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } catch (t: Throwable) {
                android.util.Log.w("AnalysisService", "Error stopping service: ${t.message}")
            }
            return START_NOT_STICKY
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Strategic Analysis in Progress")
            .setContentText("DepthLens is calculating core reality... Analyzing in background.")
            .setSmallIcon(R.drawable.ic_depthlens_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (t: Throwable) {
            android.util.Log.w("AnalysisService", "Failed startForeground: ${t.message}")
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isServiceRunning = false
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Analysis Service"
            val descriptionText = "Keep analysis active in background"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
