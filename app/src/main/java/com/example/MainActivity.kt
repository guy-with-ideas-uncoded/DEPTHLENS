package com.example

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
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SplashOpeningScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.IntelligenceViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
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
          DashboardScreen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }
}
