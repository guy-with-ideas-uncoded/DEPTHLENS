package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

object SharedSpeechRecognizerManager {
    private var speechRecognizer: SpeechRecognizer? = null
    private var activeListener: RecognitionListener? = null
    private var currentOwner: String? = null

    private fun getOwnerPriority(ownerId: String?): Int {
        return when (ownerId) {
            "FOREGROUND_CHAT", "HOME_SCREEN_MIC", "DASHBOARD_SCREEN_MIC" -> 3
            "SCREEN_SHARE" -> 2
            "WAKE_WORD" -> 1
            else -> 0
        }
    }
    
    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            Handler(Looper.getMainLooper()).post(action)
        }
    }

    fun init(context: Context) {
        runOnMainThread {
            if (speechRecognizer == null) {
                try {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                    speechRecognizer?.setRecognitionListener(delegatingListener)
                    Log.d("SharedSpeechRecognizer", "SpeechRecognizer singleton initialized successfully")
                } catch (e: Exception) {
                    Log.e("SharedSpeechRecognizer", "Failed to create SpeechRecognizer", e)
                }
            }
        }
    }

    fun setListener(listener: RecognitionListener?, ownerId: String = "DEFAULT") {
        if (currentOwner != null && currentOwner != ownerId) {
            val currentPriority = getOwnerPriority(currentOwner)
            val requestedPriority = getOwnerPriority(ownerId)
            if (requestedPriority <= currentPriority) {
                Log.d("SharedSpeechRecognizer", "setListener ignored: current owner is '$currentOwner' (priority $currentPriority), requested by '$ownerId' (priority $requestedPriority)")
                return
            }
        }
        activeListener = listener
    }

    fun getActiveListener(): RecognitionListener? {
        return activeListener
    }

    fun startListening(context: Context, intent: Intent, listener: RecognitionListener, ownerId: String = "DEFAULT") {
        runOnMainThread {
            val currentPriority = getOwnerPriority(currentOwner)
            val requestedPriority = getOwnerPriority(ownerId)
            
            if (currentOwner != null && currentOwner != ownerId) {
                if (requestedPriority > currentPriority) {
                    Log.d("SharedSpeechRecognizer", "Preempting current owner '$currentOwner' (priority $currentPriority) for new owner '$ownerId' (priority $requestedPriority)")
                    try {
                        speechRecognizer?.cancel()
                    } catch (e: Exception) {}
                    currentOwner = ownerId
                } else {
                    Log.d("SharedSpeechRecognizer", "startListening ignored: current owner '$currentOwner' (priority $currentPriority) has higher/equal priority than '$ownerId' (priority $requestedPriority)")
                    return@runOnMainThread
                }
            } else {
                currentOwner = ownerId
            }

            init(context)
            setListener(listener, ownerId)
            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("SharedSpeechRecognizer", "startListening error", e)
            }
        }
    }

    fun stopListening(ownerId: String = "DEFAULT") {
        runOnMainThread {
            if (currentOwner != null && currentOwner != ownerId) {
                val currentPriority = getOwnerPriority(currentOwner)
                val requestedPriority = getOwnerPriority(ownerId)
                if (requestedPriority <= currentPriority) {
                    Log.d("SharedSpeechRecognizer", "stopListening ignored: current owner is '$currentOwner' (priority $currentPriority), requested by '$ownerId' (priority $requestedPriority)")
                    return@runOnMainThread
                }
            }
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {}
        }
    }

    fun cancel(ownerId: String = "DEFAULT") {
        runOnMainThread {
            if (currentOwner != null && currentOwner != ownerId) {
                val currentPriority = getOwnerPriority(currentOwner)
                val requestedPriority = getOwnerPriority(ownerId)
                if (requestedPriority <= currentPriority) {
                    Log.d("SharedSpeechRecognizer", "cancel ignored: current owner is '$currentOwner' (priority $currentPriority), requested by '$ownerId' (priority $requestedPriority)")
                    return@runOnMainThread
                }
            }
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {}
        }
    }

    fun releaseOwnership(ownerId: String) {
        runOnMainThread {
            if (currentOwner == ownerId) {
                Log.d("SharedSpeechRecognizer", "Ownership released by '$ownerId'")
                currentOwner = null
                activeListener = null
            }
        }
    }

    fun destroy() {
        // Do not destroy the singleton to allow reuse across screens
    }

    private val delegatingListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            activeListener?.onReadyForSpeech(params)
        }
        override fun onBeginningOfSpeech() {
            activeListener?.onBeginningOfSpeech()
        }
        override fun onRmsChanged(rmsdB: Float) {
            activeListener?.onRmsChanged(rmsdB)
        }
        override fun onBufferReceived(buffer: ByteArray?) {
            activeListener?.onBufferReceived(buffer)
        }
        override fun onEndOfSpeech() {
            activeListener?.onEndOfSpeech()
        }
        override fun onError(error: Int) {
            activeListener?.onError(error)
        }
        override fun onResults(results: Bundle?) {
            activeListener?.onResults(results)
        }
        override fun onPartialResults(partialResults: Bundle?) {
            activeListener?.onPartialResults(partialResults)
        }
        override fun onEvent(eventType: Int, params: Bundle?) {
            activeListener?.onEvent(eventType, params)
        }
    }
}
