package com.example.communication

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.ui.viewmodel.IntelligenceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

object CommunicationOrchestrator {
    private const val TAG = "CommunicationOrch"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // Orchestrator properties
    private var context: Context? = null
    private var isMuted = false
    private var currentOwnerId: String = "DEFAULT"

    // Engine instances
    private var speechRecognizer: SpeechRecognitionManager? = null
    private var ttsManager: TextToSpeechManager? = null
    private var audioManager: AudioManager? = null

    // Callbacks from UI
    private var onQueryReadyCallback: ((String) -> Unit)? = null
    private var onPartialTranscriptCallback: ((String) -> Unit)? = null
    private var onStateChangeCallback: ((CommunicationState) -> Unit)? = null

    // Smart Silence Timers
    private var noSpeechTimer: Runnable? = null
    private var speechCompleteTimer: Runnable? = null
    private const val NO_SPEECH_TIMEOUT_MS = 4000L      // Case 1: 4 seconds
    private const val SPEECH_COMPLETE_TIMEOUT_MS = 3000L  // Case 2: 3 seconds

    // State Tracking
    private var accumulatedSpeechText = ""
    private var lastObservedChunk = ""
    private var activeSpeechDetected = false
    private var isSpeakingResponse = false

    // Audio Focus
    private var audioFocusRequest: AudioFocusRequest? = null

    fun configure(
        context: Context,
        isMuted: Boolean,
        ownerId: String,
        onQueryReady: (String) -> Unit,
        onPartialTranscript: (String) -> Unit,
        onStateChange: (CommunicationState) -> Unit
    ) {
        Log.i(TAG, "Configuring orchestrator for $ownerId (muted=$isMuted)")
        this.context = context.applicationContext
        this.isMuted = isMuted
        this.currentOwnerId = ownerId
        this.onQueryReadyCallback = onQueryReady
        this.onPartialTranscriptCallback = onPartialTranscript
        this.onStateChangeCallback = onStateChange

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognitionManager(context)
        }
        if (ttsManager == null) {
            ttsManager = TextToSpeechManager(context)
            setupTtsCallbacks()
        }
        if (audioManager == null) {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        }

        // Initialize state
        CommunicationStateManager.transitionTo(CommunicationState.Idle)
        onStateChangeCallback?.invoke(CommunicationState.Idle)
    }

    private fun setupTtsCallbacks() {
        ttsManager?.setCallbacks(
            onStart = {
                Log.d(TAG, "TTS speaking started")
                isSpeakingResponse = true
                CommunicationStateManager.transitionTo(CommunicationState.Speaking)
                onStateChangeCallback?.invoke(CommunicationState.Speaking)
            },
            onDone = {
                Log.d(TAG, "TTS speaking done")
                isSpeakingResponse = false
                mainHandler.post {
                    resumeListeningAfterTts()
                }
            },
            onError = { utId, err ->
                Log.w(TAG, "TTS error on: $utId, code: $err")
                isSpeakingResponse = false
                mainHandler.post {
                    resumeListeningAfterTts()
                }
            }
        )
    }

    fun setMuted(muted: Boolean) {
        this.isMuted = muted
        if (muted) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun startListening() {
        if (isMuted) return
        val ctx = context ?: return

        cancelSilenceTimers()
        isSpeakingResponse = false
        activeSpeechDetected = false
        accumulatedSpeechText = ""

        CommunicationStateManager.transitionTo(CommunicationState.Listening)
        onStateChangeCallback?.invoke(CommunicationState.Listening)

        // Case 1 silence timer: if absolutely no speech is detected in 4 seconds, turn mic off
        noSpeechTimer = Runnable {
            Log.i(TAG, "Smart Silence Case 1: No speech detected in $NO_SPEECH_TIMEOUT_MS ms. Stopping.")
            stopListening()
            CommunicationStateManager.transitionTo(CommunicationState.Idle)
            onStateChangeCallback?.invoke(CommunicationState.Idle)
        }
        mainHandler.postDelayed(noSpeechTimer!!, NO_SPEECH_TIMEOUT_MS)

        try {
            speechRecognizer?.startListening(
                ownerId = currentOwnerId,
                onPartial = { partial ->
                    cancelSilenceTimers()
                    activeSpeechDetected = true
                    onPartialTranscriptCallback?.invoke(partial)
                },
                onFinal = { final ->
                    cancelSilenceTimers()
                    if (final.isNotBlank()) {
                        accumulatedSpeechText = final
                        // Case 2 silence timer: wait 3 seconds for additional speech before submitting
                        speechCompleteTimer = Runnable {
                            Log.i(TAG, "Smart Silence Case 2: Speech complete. Automatically submitting query: '$accumulatedSpeechText'")
                            submitQuery(accumulatedSpeechText)
                        }
                        mainHandler.postDelayed(speechCompleteTimer!!, SPEECH_COMPLETE_TIMEOUT_MS)
                    } else {
                        // Restart idle timer if speech ended with nothing
                        startListening()
                    }
                },
                onError = { err ->
                    Log.w(TAG, "Speech recognizer error: $err")
                    // Auto-recover or restart if appropriate
                    mainHandler.postDelayed({
                        if (CommunicationStateManager.getCurrentState() == CommunicationState.Listening) {
                            startListening()
                        }
                    }, 1000)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognition: ${e.message}", e)
            CommunicationStateManager.transitionTo(CommunicationState.Error)
            onStateChangeCallback?.invoke(CommunicationState.Error)
        }
    }

    fun stopListening() {
        cancelSilenceTimers()
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping listener", e)
        }
    }

    fun interrupt() {
        Log.i(TAG, "User tap-to-interrupt triggered")
        cancelSilenceTimers()
        isSpeakingResponse = false

        try {
            ttsManager?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS on interrupt", e)
        }

        abandonAudioFocus()

        // Transition back to listening immediately
        mainHandler.post {
            startListening()
        }
    }

    private fun submitQuery(text: String) {
        cancelSilenceTimers()
        stopListening()

        CommunicationStateManager.transitionTo(CommunicationState.SendingRequest)
        onStateChangeCallback?.invoke(CommunicationState.SendingRequest)

        try {
            // Callback to trigger capturing the current frame if in Video mode
            onQueryReadyCallback?.invoke(text)

            CommunicationStateManager.transitionTo(CommunicationState.WaitingForResponse)
            onStateChangeCallback?.invoke(CommunicationState.WaitingForResponse)

            // Start observing Gemini response chunks
            AIConversationManager.startObservingResponse(
                onResponseStarted = {
                    Log.d(TAG, "AI response streaming started")
                    CommunicationStateManager.transitionTo(CommunicationState.StreamingResponse)
                    onStateChangeCallback?.invoke(CommunicationState.StreamingResponse)
                },
                onChunk = { fullText ->
                    val chunk = fullText.substring(lastObservedChunk.length)
                    if (chunk.isNotBlank()) {
                        lastObservedChunk = fullText
                        speakChunk(chunk)
                    }
                },
                onResponseFinished = { finalResponse ->
                    Log.d(TAG, "AI response completed: $finalResponse")
                    lastObservedChunk = ""
                    // If no text was sent to speak, make sure to resume
                    if (!isSpeakingResponse) {
                        resumeListeningAfterTts()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during query submission: ${e.message}", e)
            CommunicationStateManager.transitionTo(CommunicationState.Error)
            onStateChangeCallback?.invoke(CommunicationState.Error)
        }
    }

    private fun speakChunk(text: String) {
        if (isMuted) return
        requestAudioFocus()
        try {
            ttsManager?.speak(text, "orch_${System.currentTimeMillis()}", Locale.US)
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text chunk: ${e.message}", e)
        }
    }

    private fun resumeListeningAfterTts() {
        abandonAudioFocus()
        CommunicationStateManager.transitionTo(CommunicationState.WaitingForNextTurn)
        onStateChangeCallback?.invoke(CommunicationState.WaitingForNextTurn)

        // Return to listening automatically
        mainHandler.postDelayed({
            if (context != null && !isMuted) {
                startListening()
            }
        }, 800)
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrib = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attrib)
                    .setOnAudioFocusChangeListener { }
                    .build()
                am.requestAudioFocus(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    { },
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio focus: ${e.message}", e)
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus { }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error abandoning audio focus: ${e.message}", e)
        }
    }

    private fun cancelSilenceTimers() {
        noSpeechTimer?.let {
            mainHandler.removeCallbacks(it)
            noSpeechTimer = null
        }
        speechCompleteTimer?.let {
            mainHandler.removeCallbacks(it)
            speechCompleteTimer = null
        }
    }

    fun reset(ownerId: String) {
        Log.i(TAG, "Resetting orchestrator requested by: $ownerId")
        cancelSilenceTimers()
        isSpeakingResponse = false
        lastObservedChunk = ""

        try {
            ttsManager?.stop()
        } catch (e: Exception) {}

        abandonAudioFocus()
        stopListening()

        CommunicationStateManager.transitionTo(CommunicationState.Idle)
        onStateChangeCallback?.invoke(CommunicationState.Idle)
    }

    fun shutdown() {
        Log.i(TAG, "Shutting down orchestrator")
        reset(currentOwnerId)
        try {
            ttsManager?.shutdown()
        } catch (e: Exception) {}
        ttsManager = null
        speechRecognizer = null
        context = null
    }
}
