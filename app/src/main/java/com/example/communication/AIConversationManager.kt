package com.example.communication

import android.util.Log
import com.example.ui.viewmodel.IntelligenceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object AIConversationManager {
    private const val TAG = "AIConversationManager"
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var observationJob: Job? = null

    fun submitUserQuery(text: String, attachmentUri: String? = null) {
        val viewModel = IntelligenceViewModel.activeInstance
        if (viewModel == null) {
            Log.e(TAG, "IntelligenceViewModel instance is null!")
            CommunicationStateManager.transitionTo(CommunicationState.Error)
            return
        }

        CommunicationStateManager.transitionTo(CommunicationState.Processing)
        if (attachmentUri != null) {
            viewModel.setAttachment(attachmentUri)
        }
        viewModel.sendQuery(text)
    }

    fun startObservingResponse(
        onResponseStarted: () -> Unit,
        onChunk: (String) -> Unit,
        onResponseFinished: (String) -> Unit
    ) {
        observationJob?.cancel()
        
        val viewModel = IntelligenceViewModel.activeInstance ?: return
        
        observationJob = scope.launch {
            var hasStarted = false
            var lastKnownText = ""

            // Combine activeMessages flow and isLoading flow to track the streaming content and generation state
            viewModel.activeMessages.collectLatest { messages ->
                val lastModelMessage = messages.lastOrNull { it.role == "model" }
                val isLoading = viewModel.isLoading.value

                if (lastModelMessage != null) {
                    val currentText = lastModelMessage.text
                    if (currentText.isNotEmpty()) {
                        if (!hasStarted) {
                            hasStarted = true
                            onResponseStarted()
                            CommunicationStateManager.transitionTo(CommunicationState.StreamingResponse)
                        }
                        
                        if (currentText != lastKnownText) {
                            lastKnownText = currentText
                            onChunk(currentText)
                        }
                    }
                }

                // If loading completed and we had already started, complete the response
                if (!isLoading && hasStarted) {
                    onResponseFinished(lastKnownText)
                    hasStarted = false
                    observationJob?.cancel()
                }
            }
        }
    }

    fun stopObserving() {
        observationJob?.cancel()
        observationJob = null
    }
}
