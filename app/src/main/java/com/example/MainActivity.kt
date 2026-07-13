package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SplashOpeningScreen
import com.example.ui.screens.GithubUpdateManager
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.IntelligenceViewModel
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class MainActivity : FragmentActivity() {
  companion object {
    var mediaProjectionIntentData: Intent? = null
    var activeMediaProjection: android.media.projection.MediaProjection? = null
    @Volatile var isAppInForeground: Boolean = false
    @Volatile var currentContext: android.content.Context? = null
  }

  private val screenShareLauncher = registerForActivityResult(
      androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
  ) { result ->
      if (result.resultCode == RESULT_OK && result.data != null) {
          mediaProjectionIntentData = result.data
          activeMediaProjection = null
          android.util.Log.d("MainActivity", "MediaProjection token acquired successfully")
          
          try {
              val serviceIntent = Intent(this, ScreenShareService::class.java).apply {
                  putExtra("RESULT_CODE", RESULT_OK)
                  putExtra("INTENT_DATA", result.data)
              }
              if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                  startForegroundService(serviceIntent)
              } else {
                  startService(serviceIntent)
              }
          } catch (e: Exception) {
              android.util.Log.e("MainActivity", "Failed to start ScreenShareService after token acquisition", e)
          }
      } else {
          mediaProjectionIntentData = null
          activeMediaProjection = null
          android.util.Log.d("MainActivity", "MediaProjection permission was not granted")
      }
  }

  fun requestMediaProjection() {
      launchMediaProjectionDirectly()
  }

  fun launchMediaProjectionDirectly() {
      try {
          val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
          screenShareLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
      } catch (e: Exception) {
          android.util.Log.e("MainActivity", "Failed to launch media projection permission intent", e)
      }
  }

  private var receivedSessionId by mutableStateOf<String?>(null)
  private var startVoiceModeTrigger by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    currentContext = applicationContext
    try {
        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleTracker)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    enableEdgeToEdge()

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }
    
    // Check if launched via notification deep link
    val initialSessionId = intent?.getStringExtra("SESSION_ID")
    if (initialSessionId != null) {
      receivedSessionId = initialSessionId
    }
    val startVoice = intent?.getBooleanExtra("START_VOICE_MODE", false) ?: false
    if (startVoice) {
      startVoiceModeTrigger = true
    }

    // Initialize the DepthLens Theme and Software Update Systems safely to prevent startup crashes
    try {
        com.google.firebase.FirebaseApp.initializeApp(applicationContext)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    try {
        com.example.ui.theme.ThemeManager.init(applicationContext)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    try {
        com.example.ui.theme.TypographyManager.init(applicationContext)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    try {
        GithubUpdateManager.init(applicationContext)
        GithubUpdateManager.checkForUpdates(applicationContext, force = false)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Start background wake word service if enabled
    try {
        val prefs = getSharedPreferences("depthlens_prefs", MODE_PRIVATE)
        val wakeEnabled = prefs.getBoolean("wake_word_enabled", false) && com.example.ui.viewmodel.ENABLE_WAKE_WORD
        if (wakeEnabled) {
            val serviceIntent = Intent(this, WakeWordService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Auto privacy cleanup
    try {
        val prefs = getSharedPreferences("depthlens_prefs", MODE_PRIVATE)
        val autoCleanup = prefs.getBoolean("auto_cleanup", false)
        if (autoCleanup) {
            // Keep user files in filesDir/attachments safe. Only clean general temporary system cache if necessary.
            android.util.Log.d("Privacy", "Auto privacy cleanup skipped user attachments to prevent data loss")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    setContent {
      MyApplicationTheme {
        var showSplash by remember { mutableStateOf(true) }
        
        if (showSplash) {
          SplashOpeningScreen(
            onAnimationComplete = { showSplash = false },
            modifier = Modifier.fillMaxSize()
          )
        } else {
          val prefs = getSharedPreferences("depthlens_prefs", MODE_PRIVATE)
          val isBiometricEnabled = prefs.getBoolean("biometric_lock_enabled", false)
          var biometricAuthenticated by remember { mutableStateOf(!isBiometricEnabled) }

          if (!biometricAuthenticated) {
            Box(
              modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF0C0B1C)),
              contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
              androidx.compose.material3.Button(
                onClick = {
                    val executor = ContextCompat.getMainExecutor(this@MainActivity)
                    val biometricPrompt = BiometricPrompt(this@MainActivity, executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                biometricAuthenticated = true
                            }
                        })
                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Biometric login for DepthLens")
                        .setSubtitle("Log in using your biometric credential")
                        .setNegativeButtonText("Cancel")
                        .build()
                    biometricPrompt.authenticate(promptInfo)
                }
              ) {
                  androidx.compose.material3.Text("Unlock DepthLens")
              }
            }
            
            LaunchedEffect(Unit) {
                val executor = ContextCompat.getMainExecutor(this@MainActivity)
                val biometricPrompt = BiometricPrompt(this@MainActivity, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            biometricAuthenticated = true
                        }
                    })
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric login for DepthLens")
                    .setSubtitle("Log in using your biometric credential")
                    .setNegativeButtonText("Cancel")
                    .build()
                biometricPrompt.authenticate(promptInfo)
            }
          } else {
            val viewModel: IntelligenceViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            
            LaunchedEffect(receivedSessionId) {
              receivedSessionId?.let { sessionId ->
                viewModel.selectSession(sessionId)
                receivedSessionId = null
              }
            }

            LaunchedEffect(startVoiceModeTrigger) {
              if (startVoiceModeTrigger) {
                viewModel.triggerVoiceMode(true)
                startVoiceModeTrigger = false
              }
            }

            DashboardScreen(
              viewModel = viewModel,
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    GithubUpdateManager.checkAndResumeInstallation(this)
  }

  override fun onStart() {
    super.onStart()
    isAppInForeground = true
    notifyForegroundState(true)
  }

  override fun onStop() {
    super.onStop()
    isAppInForeground = false
    notifyForegroundState(false)
  }

  private fun notifyForegroundState(isForeground: Boolean) {
    if (ScreenShareService.isServiceRunning) {
        try {
            val intent = Intent(this, ScreenShareService::class.java).apply {
                action = "APP_FOREGROUND_CHANGED"
                putExtra("IS_FOREGROUND", isForeground)
            }
            startService(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to notify ScreenShareService of foreground state", e)
        }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    val sessionId = intent.getStringExtra("SESSION_ID")
    if (sessionId != null) {
      receivedSessionId = sessionId
    }
    val startVoice = intent.getBooleanExtra("START_VOICE_MODE", false)
    if (startVoice) {
      startVoiceModeTrigger = true
    }
  }
}

object AppLifecycleTracker : androidx.lifecycle.DefaultLifecycleObserver {
    override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
        MainActivity.isAppInForeground = true
    }

    override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
        MainActivity.isAppInForeground = false
    }

    override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
        MainActivity.isAppInForeground = true
        cancelCompletedNotification()
    }

    override fun onPause(owner: androidx.lifecycle.LifecycleOwner) {
        // App is pausing
    }

    private fun cancelCompletedNotification() {
        try {
            val ctx = MainActivity.currentContext
            if (ctx != null) {
                val notificationManager = ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.cancel(1002)
                android.util.Log.d("AppLifecycleTracker", "Cancelled completed notification (ID 1002) as user returned to app.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
