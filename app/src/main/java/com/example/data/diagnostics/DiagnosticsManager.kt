package com.example.data.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DiagnosticsManager {
    data class DiagnosticSession(
        val apiStatus: String = "Unknown",
        val currentModel: String = "None",
        val promptTokens: Int = 0,
        val contextTokens: Int = 0,
        val remainingContextWindow: Int = 1048576,
        val activeRetries: Int = 0,
        val compressionStatus: String = "None",
        val lastHttpStatus: Int = 0,
        val lastGeminiError: String = "None",
        val lastFinishReason: String = "None",
        val safetyBlockInfo: String = "None",
        val rawUserInput: String = "",
        val normalizedInput: String = "",
        val systemPrompt: String = "",
        val injectedMemory: String = "",
        val conversationHistorySize: Int = 0,
        val estimatedResponseTokens: Int = 0,
        val totalTokens: Int = 0,
        val responseHeaders: String = "",
        val retryCount: Int = 0,
        val requestLatencyMs: Long = 0,
        val lastLogTime: Long = 0
    )

    private val _currentSession = MutableStateFlow(DiagnosticSession())
    val currentSession: StateFlow<DiagnosticSession> = _currentSession.asStateFlow()

    private val _logHistory = MutableStateFlow<List<DiagnosticSession>>(emptyList())
    val logHistory: StateFlow<List<DiagnosticSession>> = _logHistory.asStateFlow()

    fun updateSession(update: (DiagnosticSession) -> DiagnosticSession) {
        _currentSession.value = update(_currentSession.value)
    }

    fun commitSession() {
        val current = _currentSession.value.copy(lastLogTime = System.currentTimeMillis())
        _logHistory.value = (listOf(current) + _logHistory.value).take(50)
    }
}
