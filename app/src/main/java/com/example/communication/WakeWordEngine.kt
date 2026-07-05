package com.example.communication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.ui.viewmodel.SharedSpeechRecognizerManager

class WakeWordEngine(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit
) {
    companion object {
        private const val TAG = "WakeWordEngine"
        private const val OWNER_ID = "WAKE_WORD"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var isListening = false

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isListening = true
            Log.d(TAG, "Passive detection ready for voice")
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            isListening = false
            Log.d(TAG, "Passive speech recognition error code: $error")
            
            // Re-schedule passive listening after error
            if (isRunning) {
                mainHandler.postDelayed({
                    if (isRunning && !isListening) {
                        startListening()
                    }
                }, 1500)
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val bestMatch = matches[0].lowercase()
                Log.d(TAG, "Passive query heard: '$bestMatch'")
                
                if (bestMatch.contains("lens") || bestMatch.contains("lance") || bestMatch.contains("lenses")) {
                    Log.i(TAG, "WAKE WORD TRIGGERED! Heard: '$bestMatch'")
                    onWakeWordDetected()
                    // Pause for a moment to prevent duplicate triggers
                    mainHandler.postDelayed({
                        if (isRunning) {
                            startListening()
                        }
                    }, 4000)
                    return
                }
            }
            
            if (isRunning) {
                startListening()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val partialMatch = matches[0].lowercase()
                if (partialMatch.contains("hey lens") || partialMatch.contains("hey lance") || partialMatch.contains("hey land")) {
                    Log.i(TAG, "WAKE WORD TRIGGERED via partial matching! Heard: '$partialMatch'")
                    isListening = false
                    try {
                        SharedSpeechRecognizerManager.cancel(OWNER_ID)
                    } catch (_: Exception) {}
                    onWakeWordDetected()
                    mainHandler.postDelayed({
                        if (isRunning) {
                            startListening()
                        }
                    }, 4000)
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startEngine() {
        if (!com.example.ui.viewmodel.ENABLE_WAKE_WORD) {
            Log.d(TAG, "Wake word is disabled by feature flag. Not starting engine.")
            return
        }
        Log.i(TAG, "Starting passive wake detection engine")
        isRunning = true
        startListening()
    }

    fun stopEngine() {
        Log.i(TAG, "Stopping passive wake detection engine")
        isRunning = false
        isListening = false
        mainHandler.removeCallbacksAndMessages(null)
        try {
            SharedSpeechRecognizerManager.stopListening(OWNER_ID)
            SharedSpeechRecognizerManager.cancel(OWNER_ID)
            SharedSpeechRecognizerManager.releaseOwnership(OWNER_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping SharedSpeechRecognizer for WAKE_WORD: ${e.message}")
        }
    }

    private fun startListening() {
        if (!isRunning) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            SharedSpeechRecognizerManager.startListening(context, intent, recognitionListener, OWNER_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting passive listener: ${e.message}", e)
        }
    }
}
