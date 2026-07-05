package com.example.communication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.util.Log
import com.example.ui.viewmodel.SharedSpeechRecognizerManager

class SpeechRecognitionManager(private val context: Context) {
    companion object {
        private const val TAG = "SpeechRecognitionMgr"
    }

    private var currentOwnerId: String = "DEFAULT"

    fun startListening(
        ownerId: String,
        languageCode: String = "en-US",
        onPartial: (String) -> Unit = {},
        onFinal: (String) -> Unit = {},
        onError: (Int) -> Unit = {}
    ) {
        currentOwnerId = ownerId
        Log.d(TAG, "startListening requested by: $ownerId")

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        val speechListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "[$ownerId] onReadyForSpeech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "[$ownerId] onBeginningOfSpeech")
                CommunicationStateManager.transitionTo(CommunicationState.SpeechDetected)
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "[$ownerId] onEndOfSpeech")
                CommunicationStateManager.transitionTo(CommunicationState.SpeechEnded)
            }

            override fun onError(error: Int) {
                Log.w(TAG, "[$ownerId] onError: $error")
                onError(error)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer_RESULTS_KEY())
                val finalSpoken = matches?.firstOrNull() ?: ""
                Log.d(TAG, "[$ownerId] onResults: '$finalSpoken'")
                onFinal(finalSpoken)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer_RESULTS_KEY())
                val partialSpoken = matches?.firstOrNull() ?: ""
                onPartial(partialSpoken)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        try {
            SharedSpeechRecognizerManager.startListening(context, intent, speechListener, ownerId)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting SharedSpeechRecognizer: ${e.message}", e)
            onError(-1)
        }
    }

    fun stopListening() {
        Log.d(TAG, "stopListening requested for: $currentOwnerId")
        try {
            SharedSpeechRecognizerManager.stopListening(currentOwnerId)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping SpeechRecognizer: ${e.message}", e)
        }
    }

    fun cancel() {
        Log.d(TAG, "cancel and release requested for: $currentOwnerId")
        try {
            SharedSpeechRecognizerManager.cancel(currentOwnerId)
            SharedSpeechRecognizerManager.releaseOwnership(currentOwnerId)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling SpeechRecognizer: ${e.message}", e)
        }
    }

    private fun SpeechRecognizer_RESULTS_KEY(): String {
        return android.speech.SpeechRecognizer.RESULTS_RECOGNITION
    }
}
