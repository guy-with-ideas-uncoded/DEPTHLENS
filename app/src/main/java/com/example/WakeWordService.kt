package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.communication.PermissionManager
import com.example.communication.WakeWordEngine

class WakeWordService : Service() {

    private var wakeWordEngine: WakeWordEngine? = null
    private val channelId = "wakeword_service_channel"
    private val notificationId = 88271

    override fun onCreate() {
        super.onCreate()
        Log.d("WakeWordService", "WakeWordService onCreate")

        if (!com.example.ui.viewmodel.ENABLE_WAKE_WORD) {
            Log.d("WakeWordService", "Wake Word is disabled by feature flag. Stopping service immediately.")
            stopSelf()
            return
        }

        createNotificationChannel()

        val hasPermission = PermissionManager.hasMicrophonePermission(this)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (hasPermission) {
                    startForeground(
                        notificationId,
                        buildNotification(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    // Start standard generic foreground service to prevent the OS from throwing
                    // ForegroundServiceDidNotStartInTimeException before we stopSelf()
                    startForeground(notificationId, buildNotification())
                }
            } else {
                startForeground(notificationId, buildNotification())
            }
        } catch (e: Exception) {
            Log.e("WakeWordService", "Failed to start foreground service", e)
            stopSelf()
            return
        }

        if (!hasPermission) {
            Log.e("WakeWordService", "RECORD_AUDIO permission missing — stopping service")
            stopSelf()
            return
        }

        // Initialize and start the clean WakeWordEngine
        wakeWordEngine = WakeWordEngine(this) {
            Log.d("WakeWordService", "Wake word detected via WakeWordEngine!")
            
            // Launch main app in voice mode
            val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("START_VOICE_MODE", true)
            }
            startActivity(launchIntent)
        }
        wakeWordEngine?.startEngine()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("WakeWordService", "WakeWordService onDestroy")
        wakeWordEngine?.stopEngine()
        wakeWordEngine = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Hey Lens Wake Word Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("DepthLens Voice Detection Active")
            .setContentText("Say 'Hey Lens' to wake the assistant hands-free")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .build()
    }
}
