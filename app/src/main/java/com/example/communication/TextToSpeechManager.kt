package com.example.communication

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {
    companion object {
        private const val TAG = "TextToSpeechManager"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingSpeakText: String? = null
    private var pendingUtteranceId: String? = null
    private var pendingLocale: Locale? = null
    
    private var onStartCallback: ((String?) -> Unit)? = null
    private var onDoneCallback: ((String?) -> Unit)? = null
    private var onErrorCallback: ((String?, Int) -> Unit)? = null

    private var speedRate = 1.0f

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS onStart: $utteranceId")
                    onStartCallback?.invoke(utteranceId)
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS onDone: $utteranceId")
                    onDoneCallback?.invoke(utteranceId)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    Log.w(TAG, "TTS onError: $utteranceId")
                    onErrorCallback?.invoke(utteranceId, -1)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.w(TAG, "TTS onError: $utteranceId, error: $errorCode")
                    onErrorCallback?.invoke(utteranceId, errorCode)
                }
            })

            // Run pending if any
            val text = pendingSpeakText
            val uId = pendingUtteranceId
            val loc = pendingLocale
            if (text != null && uId != null && loc != null) {
                speakNow(text, uId, loc)
                pendingSpeakText = null
                pendingUtteranceId = null
                pendingLocale = null
            }
        } else {
            Log.e(TAG, "TTS initialization failed")
        }
    }

    fun setCallbacks(
        onStart: (String?) -> Unit,
        onDone: (String?) -> Unit,
        onError: (String?, Int) -> Unit
    ) {
        this.onStartCallback = onStart
        this.onDoneCallback = onDone
        this.onErrorCallback = onError
    }

    fun setSpeechRate(rate: Float) {
        this.speedRate = rate
        if (isInitialized) {
            tts?.setSpeechRate(rate)
        }
    }

    fun speak(text: String, utteranceId: String, locale: Locale = Locale.US) {
        if (isInitialized) {
            speakNow(text, utteranceId, locale)
        } else {
            pendingSpeakText = text
            pendingUtteranceId = utteranceId
            pendingLocale = locale
        }
    }

    private fun speakNow(text: String, utteranceId: String, locale: Locale) {
        val ttsInstance = tts ?: return
        
        try {
            val result = ttsInstance.isLanguageAvailable(locale)
            if (result >= TextToSpeech.LANG_AVAILABLE) {
                ttsInstance.language = locale
            } else {
                ttsInstance.language = Locale.US
            }
            
            ttsInstance.setSpeechRate(speedRate)
            
            val params = android.os.Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            
            ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during TTS speak: ${e.message}", e)
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS: ${e.message}", e)
        }
    }

    fun shutdown() {
        try {
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS: ${e.message}", e)
        }
    }
}
