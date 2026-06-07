package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
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

class MainActivity : ComponentActivity() {
  private var receivedSessionId by mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Check if launched via notification deep link
    val initialSessionId = intent?.getStringExtra("SESSION_ID")
    if (initialSessionId != null) {
      receivedSessionId = initialSessionId
    }

    // Initialize the DepthLens Theme and Software Update Systems
    com.example.ui.theme.ThemeManager.init(applicationContext)
    com.example.ui.theme.TypographyManager.init(applicationContext)
    GithubUpdateManager.init(applicationContext)
    GithubUpdateManager.checkForUpdates(applicationContext, force = false)

    setContent {
      MyApplicationTheme {
        var showSplash by remember { mutableStateOf(true) }
        
        if (showSplash) {
          SplashOpeningScreen(
            onAnimationComplete = { showSplash = false },
            modifier = Modifier.fillMaxSize()
          )
        } else {
          val viewModel: IntelligenceViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
          
          LaunchedEffect(receivedSessionId) {
            receivedSessionId?.let { sessionId ->
              viewModel.selectSession(sessionId)
              receivedSessionId = null
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

  override fun onResume() {
    super.onResume()
    GithubUpdateManager.checkAndResumeInstallation(this)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    val sessionId = intent.getStringExtra("SESSION_ID")
    if (sessionId != null) {
      receivedSessionId = sessionId
    }
  }
}
