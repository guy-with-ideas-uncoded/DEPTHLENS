package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import com.example.communication.CommunicationOrchestrator
import com.example.communication.CommunicationState

enum class AudioState {
    IDLE,
    LISTENING,
    PROCESSING,
    AI_SPEAKING,
    WAIT_FOR_TTS_FINISH
}

object AudioConversationManager {
    private const val TAG = "AudioConvManager"

    var currentState = AudioState.IDLE
        private set

    var isAISpeaking = false
        private set

    var responseGenerating = false
        private set

    private var stateChangeCallback: ((AudioState) -> Unit)? = null

    var ttsStop: (() -> Unit)? = null

    fun configure(
        context: Context,
        isMuted: Boolean = false,
        ownerId: String = "DEFAULT",
        onQueryReady: (String) -> Unit,
        onPartialTranscript: (String) -> Unit = {},
        onStateChange: (AudioState) -> Unit = {}
    ) {
        this.stateChangeCallback = onStateChange
        this.ttsStop = {
            try {
                CommunicationOrchestrator.interrupt()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping TTS via orchestrator", e)
            }
        }

        CommunicationOrchestrator.configure(
            context = context,
            isMuted = isMuted,
            ownerId = ownerId,
            onQueryReady = onQueryReady,
            onPartialTranscript = onPartialTranscript,
            onStateChange = { comState ->
                val audioState = when (comState) {
                    CommunicationState.Idle -> AudioState.IDLE
                    CommunicationState.Listening -> AudioState.LISTENING
                    CommunicationState.SpeechDetected -> AudioState.LISTENING
                    CommunicationState.SpeechEnded -> AudioState.LISTENING
                    CommunicationState.Processing -> AudioState.PROCESSING
                    CommunicationState.SendingRequest -> AudioState.PROCESSING
                    CommunicationState.WaitingForResponse -> AudioState.PROCESSING
                    CommunicationState.StreamingResponse -> AudioState.PROCESSING
                    CommunicationState.Speaking -> AudioState.AI_SPEAKING
                    CommunicationState.WaitingForNextTurn -> AudioState.WAIT_FOR_TTS_FINISH
                    else -> AudioState.IDLE
                }

                currentState = audioState
                isAISpeaking = (audioState == AudioState.AI_SPEAKING)
                responseGenerating = (audioState == AudioState.PROCESSING)

                Log.d(TAG, "Mapped State transition: $comState -> $audioState")
                onStateChange(audioState)
            }
        )
    }

    fun setMuted(muted: Boolean) {
        CommunicationOrchestrator.setMuted(muted)
    }

    fun interrupt() {
        CommunicationOrchestrator.interrupt()
    }

    fun startListening() {
        CommunicationOrchestrator.startListening()
    }

    fun stopListeningAndCancel() {
        CommunicationOrchestrator.stopListening()
    }

    fun reset(ownerId: String) {
        CommunicationOrchestrator.reset(ownerId)
    }

    fun noteAiSpokenText(text: String) {
        // Logged or ignored in orchestration layer
    }

    fun onTtsStarted() {
        isAISpeaking = true
        responseGenerating = false
        currentState = AudioState.AI_SPEAKING
        stateChangeCallback?.invoke(AudioState.AI_SPEAKING)
    }

    fun onTtsFinished() {
        isAISpeaking = false
        responseGenerating = false
        currentState = AudioState.WAIT_FOR_TTS_FINISH
        stateChangeCallback?.invoke(AudioState.WAIT_FOR_TTS_FINISH)
    }
}
