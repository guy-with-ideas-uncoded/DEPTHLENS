package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MessageEntity
import com.example.data.model.SessionEntity
import com.example.data.model.MemoryInsight
import com.example.data.repository.IntelligenceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IntelligenceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = IntelligenceRepository(application)

    // Active session selection
    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    // Loading indicator
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Attached media asset
    private val _attachedImageUri = MutableStateFlow<String?>(null)
    val attachedImageUri: StateFlow<String?> = _attachedImageUri.asStateFlow()

    // Live list of session histories
    val sessions: StateFlow<List<SessionEntity>> = repository.allSessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live list of long-term memory logs
    val memoryInsights: StateFlow<List<MemoryInsight>> = repository.allMemoryInsightsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic message list for currently active session
    val activeMessages: StateFlow<List<MessageEntity>> = _activeSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessagesFlow(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initialize with default session if none exists
        viewModelScope.launch {
            val existing = repository.allSessionsFlow.firstOrNull() ?: emptyList()
            if (existing.isNotEmpty()) {
                _activeSessionId.value = existing.first().id
            } else {
                val newSession = repository.createNewSession("Global Intelligence Feed")
                _activeSessionId.value = newSession.id
            }
        }
    }

    fun selectSession(sessionId: String?) {
        _activeSessionId.value = sessionId
        clearAttachment()
    }

    fun createSession(title: String) {
        viewModelScope.launch {
            val newSession = repository.createNewSession(title.ifBlank { "Intel Thread ${System.currentTimeMillis() % 1000}" })
            _activeSessionId.value = newSession.id
            clearAttachment()
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_activeSessionId.value == sessionId) {
                val rem = repository.allSessionsFlow.firstOrNull() ?: emptyList()
                _activeSessionId.value = rem.firstOrNull()?.id
            }
        }
    }

    fun togglePinSession(sessionId: String) {
        viewModelScope.launch {
            repository.togglePinSession(sessionId)
        }
    }

    fun setAttachment(uriString: String?) {
        _attachedImageUri.value = uriString
    }

    fun clearAttachment() {
        _attachedImageUri.value = null
    }

    fun sendQuery(text: String) {
        val sessionId = _activeSessionId.value ?: return
        val cleanQuery = text.trim()
        if (cleanQuery.isEmpty() && _attachedImageUri.value == null) return

        val attachedUri = _attachedImageUri.value
        clearAttachment()

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Determine if this is the first user query in this conversation
                val existingHistory = repository.getMessagesFlow(sessionId).firstOrNull() ?: emptyList()
                val isFirstQuery = existingHistory.none { it.role == "user" }

                // 1. Insert user message to initiate continuity UI rendering
                repository.insertUserMessage(sessionId, cleanQuery, attachedUri)
                
                // 2. Perform intelligence analysis call to external models
                repository.generateAnalysis(sessionId)

                // 3. Asynchronously generate an aesthetic title if first query
                if (isFirstQuery && cleanQuery.isNotEmpty()) {
                    launch {
                        repository.generateTitleForSession(sessionId, cleanQuery)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Memory management options
    private val _isMemoryEnabled = MutableStateFlow(true)
    val isMemoryEnabled: StateFlow<Boolean> = _isMemoryEnabled.asStateFlow()

    private val _isCollectiveIntelligenceOptIn = MutableStateFlow(true)
    val isCollectiveIntelligenceOptIn: StateFlow<Boolean> = _isCollectiveIntelligenceOptIn.asStateFlow()

    fun setMemoryEnabled(enabled: Boolean) {
        _isMemoryEnabled.value = enabled
    }

    fun setCollectiveIntelligenceOptIn(optIn: Boolean) {
        _isCollectiveIntelligenceOptIn.value = optIn
    }

    fun clearAllMemoryInsights() {
        viewModelScope.launch {
            repository.clearAllMemoryInsights()
        }
    }

    fun clearAllUserData() {
        viewModelScope.launch {
            repository.clearAllData()
            _activeSessionId.value = null
            createSession("New Reality Intel")
        }
    }
}
