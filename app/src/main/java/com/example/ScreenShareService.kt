package com.example

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.communication.PermissionManager
import com.example.communication.ScreenShareEngine
import com.example.communication.SpeechRecognitionManager
import com.example.ui.screens.saveBitmapToTempFile
import com.example.ui.viewmodel.IntelligenceViewModel
import java.util.concurrent.atomic.AtomicReference

class ScreenShareService : Service() {
    companion object {
        private const val TAG = "ScreenShareService"
        
        val latestBitmap = ScreenShareEngine.latestBitmap
        
        @Volatile
        var isServiceRunning = false

        @Volatile
        var isCaptureActive = false
    }

    private val channelId = "screenshare_service_channel"
    private val notificationId = 88272

    private var screenShareEngine: ScreenShareEngine? = null
    private var speechRecognitionManager: SpeechRecognitionManager? = null

    // Overlay properties
    private var windowManager: WindowManager? = null
    private var overlayView: FrameLayout? = null
    private val hideHandler = Handler(Looper.getMainLooper())
    private var isExpanded = true
    private var isAppInForeground = true

    private val hideRunnable = Runnable {
        minimizeOverlay()
    }

    private var isBackgroundListening = false

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        screenShareEngine = ScreenShareEngine(this)
        speechRecognitionManager = SpeechRecognitionManager(this)
        
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")
        
        val action = intent?.action
        if (action == "STOP_SERVICE") {
            stopScreenCapture()
            stopSelf()
            return START_NOT_STICKY
        }
        if (action == "APP_FOREGROUND_CHANGED") {
            val isFg = intent.getBooleanExtra("IS_FOREGROUND", true)
            onAppForegroundChanged(isFg)
            return START_STICKY
        }

        val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_OK) ?: Activity.RESULT_OK
        val intentData = intent?.getParcelableExtra<Intent>("INTENT_DATA")

        // Start foreground with appropriate types safely in onStartCommand
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val hasMicPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.RECORD_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                val serviceType = if (intentData != null) {
                    if (hasMicPerm) {
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    } else {
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    }
                } else {
                    if (hasMicPerm) {
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    } else {
                        0
                    }
                }

                if (serviceType != 0) {
                    startForeground(notificationId, buildNotification(), serviceType)
                } else {
                    startForeground(notificationId, buildNotification())
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val serviceType = if (intentData != null) {
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                } else {
                    0
                }
                if (serviceType != 0) {
                    startForeground(notificationId, buildNotification(), serviceType)
                } else {
                    startForeground(notificationId, buildNotification())
                }
            } else {
                startForeground(notificationId, buildNotification())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call startForeground safely in onStartCommand", e)
        }

        if (intentData != null) {
            screenShareEngine?.startCapture(
                resultCode,
                intentData,
                onCaptureStarted = {
                    isCaptureActive = true
                    Log.d(TAG, "Screen Capture Started Successfully")
                },
                onCaptureStopped = {
                    isCaptureActive = false
                    Log.d(TAG, "Screen Capture Stopped")
                }
            )
        } else {
            Log.w(TAG, "No intent data provided in onStartCommand")
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun onAppForegroundChanged(isFg: Boolean) {
        isAppInForeground = isFg
        Log.d(TAG, "Foreground state changed: isAppInForeground = $isFg")
        
        Handler(Looper.getMainLooper()).post {
            if (isFg) {
                hideFloatingOverlay()
                stopBackgroundListening()
            } else {
                if (isCaptureActive) {
                    showFloatingOverlay()
                    startBackgroundListening()
                }
            }
        }
    }

    private fun startBackgroundListening() {
        if (!PermissionManager.hasMicrophonePermission(this)) {
            Log.w(TAG, "Cannot start background listening: No microphone permission")
            return
        }

        isBackgroundListening = true
        try {
            speechRecognitionManager?.startListening(
                ownerId = "SCREEN_SHARE",
                languageCode = "en-US",
                onPartial = { },
                onFinal = { spoken ->
                    processBackgroundQuery(spoken)
                },
                onError = { errorCode ->
                    Log.w(TAG, "Background listening error: $errorCode")
                    // Auto-restart passive listening
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isBackgroundListening && !isAppInForeground) {
                            startBackgroundListening()
                        }
                    }, 2000)
                }
            )
            Log.d(TAG, "Background speech listening started successfully")
        } catch (e: Exception) {
            isBackgroundListening = false
            Log.e(TAG, "Error starting background listening: ${e.message}", e)
        }
    }

    private fun stopBackgroundListening() {
        isBackgroundListening = false
        speechRecognitionManager?.stopListening()
        speechRecognitionManager?.cancel()
    }

    private fun processBackgroundQuery(spoken: String) {
        val vm = IntelligenceViewModel.activeInstance ?: return
        
        val frame = latestBitmap.get()
        val tempFileUri = if (frame != null && !frame.isRecycled) {
            try {
                val copied = frame.copy(Bitmap.Config.ARGB_8888, false)
                saveBitmapToTempFile(this, copied)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy background frame", e)
                null
            }
        } else {
            null
        }

        Handler(Looper.getMainLooper()).post {
            try {
                if (tempFileUri != null) {
                    vm.setAttachment(tempFileUri)
                }
                vm.sendQuery(spoken)
                Log.d(TAG, "Background query submitted: '$spoken' with frame URI: $tempFileUri")
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting background query to ViewModel", e)
            }
        }
    }

    private fun stopScreenCapture() {
        screenShareEngine?.stopCapture()
        isCaptureActive = false
    }

    private fun showFloatingOverlay() {
        if (!PermissionManager.hasOverlayPermission(this)) {
            Log.w(TAG, "Cannot show overlay: Permission not granted")
            return
        }

        if (overlayView != null) return // Already showing

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val container = FrameLayout(this)
        overlayView = container

        val pill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val pad8 = dpToPx(8)
            val pad12 = dpToPx(12)
            setPadding(pad12, pad8, pad12, pad8)
            
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#E6070811"))
                cornerRadius = dpToPx(24).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#338B5CF6"))
            }
        }

        val dot = View(this).apply {
            val size = dpToPx(8)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                rightMargin = dpToPx(8)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FF10B981"))
            }
        }

        val pulseAnim = android.view.animation.AlphaAnimation(0.3f, 1.0f).apply {
            duration = 800
            repeatMode = android.view.animation.Animation.REVERSE
            repeatCount = android.view.animation.Animation.INFINITE
        }
        dot.startAnimation(pulseAnim)

        val label = TextView(this).apply {
            text = "DepthLens Active"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = dpToPx(12)
            }
        }

        val returnBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_revert)
            setColorFilter(Color.parseColor("#FF8B5CF6"))
            val size = dpToPx(24)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                rightMargin = dpToPx(12)
            }
            setOnClickListener {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(launchIntent)
            }
        }

        val endBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.parseColor("#FFEF4444"))
            val size = dpToPx(24)
            layoutParams = LinearLayout.LayoutParams(size, size)
            setOnClickListener {
                Log.d(TAG, "Overlay end button clicked")
                stopScreenCapture()
                hideFloatingOverlay()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }

        pill.addView(dot)
        pill.addView(label)
        pill.addView(returnBtn)
        pill.addView(endBtn)

        container.addView(pill)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dpToPx(80)
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true

        pill.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    hideHandler.removeCallbacks(hideRunnable)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isClick = false
                    }
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    windowManager?.updateViewLayout(container, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        v.performClick()
                        if (!isExpanded) {
                            expandOverlay(label, returnBtn, endBtn)
                        } else {
                            resetAutoHideTimer()
                        }
                    } else {
                        resetAutoHideTimer()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(container, params)
            resetAutoHideTimer()
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlay: ${e.message}", e)
        }
    }

    private fun resetAutoHideTimer() {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, 4000)
    }

    private fun expandOverlay(label: View, returnBtn: View, endBtn: View) {
        isExpanded = true
        label.visibility = View.VISIBLE
        returnBtn.visibility = View.VISIBLE
        endBtn.visibility = View.VISIBLE
        resetAutoHideTimer()
    }

    private fun minimizeOverlay() {
        isExpanded = false
        val container = overlayView ?: return
        val pill = container.getChildAt(0) as? LinearLayout ?: return
        
        val label = pill.getChildAt(1)
        val returnBtn = pill.getChildAt(2)
        val endBtn = pill.getChildAt(3)

        label.visibility = View.GONE
        returnBtn.visibility = View.GONE
        endBtn.visibility = View.GONE
    }

    private fun hideFloatingOverlay() {
        hideHandler.removeCallbacks(hideRunnable)
        val view = overlayView
        if (view != null) {
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {}
            overlayView = null
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        com.example.MainActivity.mediaProjectionIntentData = null
        com.example.MainActivity.activeMediaProjection = null
        isServiceRunning = false
        isCaptureActive = false
        hideFloatingOverlay()
        stopBackgroundListening()
        stopScreenCapture()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping foreground: ${e.message}")
        }

        val oldBitmap = latestBitmap.getAndSet(null)
        oldBitmap?.recycle()

        super.onDestroy()
    }

    private fun getNotificationImportance(): Int {
        val prefs = getSharedPreferences("depthlens_prefs", android.content.Context.MODE_PRIVATE)
        return if (prefs.getBoolean("screen_share_notification", true)) {
            NotificationManager.IMPORTANCE_LOW
        } else {
            NotificationManager.IMPORTANCE_MIN
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = getNotificationImportance()
            val dynamicChannelId = "${channelId}_$importance"
            val channel = NotificationChannel(
                dynamicChannelId,
                "Screen Share Service",
                importance
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val importance = getNotificationImportance()
        val dynamicChannelId = "${channelId}_$importance"
        val title = if (importance == NotificationManager.IMPORTANCE_MIN) "Sharing Screen" else "DepthLens Screen Sharing"
        val text = if (importance == NotificationManager.IMPORTANCE_MIN) "" else "Screen share is currently active. Tap to return to DepthLens."
        return NotificationCompat.Builder(this, dynamicChannelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
