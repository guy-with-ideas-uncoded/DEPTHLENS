package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.model.*
import com.example.data.repository.IntelligenceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IntelligenceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = IntelligenceRepository(application)
    private val prefs = application.getSharedPreferences("depthlens_prefs", android.content.Context.MODE_PRIVATE)

    // User account states
    val isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isGuest = MutableStateFlow(prefs.getBoolean("is_guest", false))
    val userId = MutableStateFlow(prefs.getString("user_id", "guest_local") ?: "guest_local")
    val userName = MutableStateFlow(prefs.getString("user_name", "Guest Explorer") ?: "Guest Explorer")
    val userEmail = MutableStateFlow(prefs.getString("user_email", "") ?: "")
    val githubToken = MutableStateFlow(prefs.getString("github_token", "") ?: "")
    val repoOwnerAndName = MutableStateFlow(prefs.getString("github_repo", "") ?: "")
    val onboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_completed", false))

    // Active session selection
    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    // Selected analysis mode (synced from UI)
    private val _selectedMode = MutableStateFlow("Root Cause")
    val selectedMode: StateFlow<String> = _selectedMode.asStateFlow()

    fun setSelectedMode(mode: String) { _selectedMode.value = mode }

    // Selected analysis depth (synced from UI)
    private val _selectedDepth = MutableStateFlow("Standard Analysis")
    val selectedDepth: StateFlow<String> = _selectedDepth.asStateFlow()

    fun setSelectedDepth(depth: String) { _selectedDepth.value = depth }

    // Loading indicator dynamically mapped from the repository's background analyses set
    val isLoading: StateFlow<Boolean> = combine(
        _activeSessionId,
        IntelligenceRepository.runningAnalyses
    ) { activeId: String?, runningSet: Set<String> ->
        activeId != null && runningSet.contains(activeId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Attached media asset
    private val _attachedImageUri = MutableStateFlow<String?>(null)
    val attachedImageUri: StateFlow<String?> = _attachedImageUri.asStateFlow()

    // Live list of session histories
    val sessions: StateFlow<List<SessionEntity>> = repository.allSessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live list of long-term memory logs
    val memoryInsights: StateFlow<List<MemoryInsight>> = repository.allMemoryInsightsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live list of archived deep-dive insights
    val archivedInsights: StateFlow<List<ArchivedInsightEntity>> = repository.allArchivedInsightsFlow
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

    // Deep-dive system reflections cache
    private val _deepDiveInsights = MutableStateFlow<Map<String, String>>(emptyMap())
    val deepDiveInsights: StateFlow<Map<String, String>> = _deepDiveInsights.asStateFlow()

    // Loading state for Deep-dive
    private val _isDeepDiveLoading = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isDeepDiveLoading: StateFlow<Map<String, Boolean>> = _isDeepDiveLoading.asStateFlow()

    init {
        // Always open the Home Screen on app launch / entry
        _activeSessionId.value = null

        // Load deep-dive insights from prefs
        try {
            val allPrefs = prefs.all
            val loadedDeepDives = mutableMapOf<String, String>()
            allPrefs.forEach { (key, value) ->
                if (key.startsWith("deep_dive_") && value is String) {
                    val sId = key.removePrefix("deep_dive_")
                    loadedDeepDives[sId] = value
                }
            }
            _deepDiveInsights.value = loadedDeepDives
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        
        viewModelScope.launch {
            val existing = repository.allSessionsFlow.firstOrNull() ?: emptyList()
            if (existing.isEmpty()) {
                // Pre-seed a default session silently so workspace is ready
                repository.createNewSession(generateUniqueSessionName("Root Cause"))
            }
        }
    }

    fun selectSession(sessionId: String?) {
        _activeSessionId.value = sessionId
        clearAttachment()
        clearContinuityBrief()
    }

    fun createSession(title: String) {
        viewModelScope.launch {
            val newSession = repository.createNewSession(title.ifBlank { generateUniqueSessionName("Root Cause") })
            _activeSessionId.value = newSession.id
            clearAttachment()
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_activeSessionId.value == sessionId) {
                _activeSessionId.value = null
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

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessageById(messageId)
        }
    }

    fun retryLastAnalysis(errorMessageId: String) {
        val sessionId = _activeSessionId.value ?: return
        viewModelScope.launch {
            try {
                // Delete the error message
                repository.deleteMessageById(errorMessageId)
                // Determine if there are still any messages
                val existingHistory = repository.getMessagesFlow(sessionId).firstOrNull() ?: emptyList()
                val lastUserMsg = existingHistory.lastOrNull { it.role == "user" }
                if (lastUserMsg != null) {
                    // Trigger analysis again in background
                    repository.startBackgroundAnalysis(sessionId, _selectedMode.value, _selectedDepth.value)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun regenerateLastAnalysis(aiMessageId: String) {
        val sessionId = _activeSessionId.value ?: return
        viewModelScope.launch {
            try {
                // Delete this AI response
                repository.deleteMessageById(aiMessageId)
                // Trigger background analysis again
                repository.startBackgroundAnalysis(sessionId, _selectedMode.value, _selectedDepth.value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendQuery(text: String) {
        val cleanQuery = text.trim()
        if (cleanQuery.isEmpty() && _attachedImageUri.value == null) return

        val attachedUri = _attachedImageUri.value
        clearAttachment()

        viewModelScope.launch {
            try {
                // Determine or create session if none active (e.g. from Home screen prompt)
                val sessionId = _activeSessionId.value ?: run {
                    val newSession = repository.createNewSession(generateUniqueSessionName(_selectedMode.value))
                    _activeSessionId.value = newSession.id
                    newSession.id
                }

                // Determine if this is the first user query in this conversation
                val existingHistory = repository.getMessagesFlow(sessionId).firstOrNull() ?: emptyList()
                val userMessages = existingHistory.filter { it.role == "user" }
                val isFirstQuery = userMessages.isEmpty()
                val isSecondQuery = userMessages.size == 1

                val activeSession = repository.allSessionsFlow.firstOrNull()?.find { it.id == sessionId }
                val currentTitle = activeSession?.title ?: ""
                val isCurrentTitleVague = currentTitle.isEmpty() || 
                        currentTitle.endsWith("Brief") || 
                        currentTitle.endsWith("Analysis") || 
                        currentTitle.endsWith("Study") || 
                        currentTitle.endsWith("Inquiry") || 
                        currentTitle.startsWith("Origin Pattern") || 
                        currentTitle.startsWith("Causal Chain") || 
                        currentTitle.startsWith("Source Mapping") || 
                        currentTitle.startsWith("Root Factor") || 
                        currentTitle.startsWith("Deep Cause") || 
                        currentTitle.startsWith("Foundation Analysis") || 
                        currentTitle.startsWith("Trigger Sequence") || 
                        currentTitle.startsWith("Core Driver") || 
                        currentTitle.startsWith("Underlying Force") ||
                        currentTitle.startsWith("Cognitive Pattern") ||
                        currentTitle.startsWith("Behavioral Motive") ||
                        currentTitle.startsWith("Mental Model") ||
                        currentTitle.startsWith("Psychological Driver") ||
                        currentTitle.startsWith("Belief System") ||
                        currentTitle.startsWith("Emotional Trigger") ||
                        currentTitle.startsWith("Bias Detection") ||
                        currentTitle.startsWith("Subconscious Pattern") ||
                        currentTitle.startsWith("Identity Lens") ||
                        currentTitle.startsWith("Feedback Loop") ||
                        currentTitle.startsWith("System Dynamics") ||
                        currentTitle.startsWith("Incentive Structure") ||
                        currentTitle.startsWith("Network Effect") ||
                        currentTitle.startsWith("Systemic Leverage") ||
                        currentTitle.startsWith("Loop Analysis") ||
                        currentTitle.startsWith("Equilibrium Pattern") ||
                        currentTitle.startsWith("Emergent Behavior") ||
                        currentTitle.startsWith("System Blind Spot") ||
                        currentTitle.contains("Reality Intel") ||
                        currentTitle.startsWith("New Session") ||
                        currentTitle.startsWith("Untitled")

                // 1. Insert user message to initiate continuity UI rendering
                repository.insertUserMessage(sessionId, cleanQuery, attachedUri)
                
                // 2. Perform intelligence analysis call to external models in background (non-blocking)
                repository.startBackgroundAnalysis(sessionId, _selectedMode.value, _selectedDepth.value) {
                    if (cleanQuery.isNotEmpty()) {
                        viewModelScope.launch {
                            if (isFirstQuery) {
                                if (isVagueOrShort(cleanQuery)) {
                                    val tempTitle = getTemporaryTitleForMode(_selectedMode.value)
                                    repository.updateSessionTitle(sessionId, tempTitle)
                                } else {
                                    repository.generateTitleForSession(sessionId, cleanQuery)
                                }
                            } else if (isSecondQuery && isCurrentTitleVague && !isVagueOrShort(cleanQuery)) {
                                repository.generateTitleForSession(sessionId, cleanQuery)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Memory management options
    private val _isMemoryEnabled = MutableStateFlow(true)
    val isMemoryEnabled: StateFlow<Boolean> = _isMemoryEnabled.asStateFlow()

    private val _isCollectiveIntelligenceOptIn = MutableStateFlow(true)
    val isCollectiveIntelligenceOptIn: StateFlow<Boolean> = _isCollectiveIntelligenceOptIn.asStateFlow()

    private val _isPrivacyModeEnabled = MutableStateFlow(prefs.getBoolean("privacy_mode_enabled", false))
    val isPrivacyModeEnabled: StateFlow<Boolean> = _isPrivacyModeEnabled.asStateFlow()

    fun setMemoryEnabled(enabled: Boolean) {
        _isMemoryEnabled.value = enabled
    }

    fun setCollectiveIntelligenceOptIn(optIn: Boolean) {
        _isCollectiveIntelligenceOptIn.value = optIn
    }

    fun setPrivacyModeEnabled(enabled: Boolean) {
        _isPrivacyModeEnabled.value = enabled
        prefs.edit().putBoolean("privacy_mode_enabled", enabled).apply()
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

    // Conversation Continuity States
    private val _continuityBrief = MutableStateFlow<String?>(null)
    val continuityBrief: StateFlow<String?> = _continuityBrief.asStateFlow()

    private val _continuityBriefStatus = MutableStateFlow("Idle")
    val continuityBriefStatus: StateFlow<String> = _continuityBriefStatus.asStateFlow()

    fun clearContinuityBrief() {
        _continuityBrief.value = null
        _continuityBriefStatus.value = "Idle"
    }

    fun reconnectConversationContext() {
        val sessionId = _activeSessionId.value ?: return
        _continuityBriefStatus.value = "Syncing"
        _continuityBrief.value = null
        viewModelScope.launch {
            try {
                val brief = repository.generateContinuityBrief(sessionId)
                _continuityBrief.value = brief
                _continuityBriefStatus.value = "Done"
            } catch (e: Exception) {
                e.printStackTrace()
                _continuityBriefStatus.value = "Error"
            }
        }
    }

    // Collapsible System Controls state saved in SharedPreferences
    private val _isSystemControlsExpanded = MutableStateFlow(prefs.getBoolean("system_controls_expanded", false))
    val isSystemControlsExpanded: StateFlow<Boolean> = _isSystemControlsExpanded.asStateFlow()

    fun setSystemControlsExpanded(expanded: Boolean) {
        _isSystemControlsExpanded.value = expanded
        prefs.edit().putBoolean("system_controls_expanded", expanded).apply()
    }

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    private val _darkModeEnabled = MutableStateFlow(prefs.getBoolean("dark_mode_enabled", true))
    val darkModeEnabled: StateFlow<Boolean> = _darkModeEnabled.asStateFlow()

    fun setDarkModeEnabled(enabled: Boolean) {
        _darkModeEnabled.value = enabled
        prefs.edit().putBoolean("dark_mode_enabled", enabled).apply()
    }

    fun setOnboardingCompleted(completed: Boolean) {
        onboardingCompleted.value = completed
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
    }

    fun generateDeepDive(sessionId: String, queryText: String) {
        if (_isDeepDiveLoading.value[sessionId] == true) return
        _isDeepDiveLoading.value = _isDeepDiveLoading.value + (sessionId to true)

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    val errMsg = "Error: Missing Gemini API Key in the Secrets panel."
                    _deepDiveInsights.value = _deepDiveInsights.value + (sessionId to errMsg)
                    return@launch
                }

                val request = com.example.data.network.GenerateContentRequest(
                    contents = listOf(
                        com.example.data.network.Content(
                            parts = listOf(com.example.data.network.Part(text = "Provide a systemic deep-dive reflection on the query: '$queryText' emphasizing 2nd and 3rd order consequences."))
                        )
                    ),
                    generationConfig = com.example.data.network.GenerationConfig(temperature = 0.72f),
                    systemInstruction = com.example.data.network.Content(
                        parts = listOf(
                            com.example.data.network.Part(
                                text = """
                                    You are DepthLens Deep-Dive AI. Provide a high-level system-oriented reflection.
                                    Meticulously structure your response into:
                                    - **Systemic Reflection**: A profound overview of the scenario's hidden causal gears.
                                    - **1st Order Impact**: The immediate, obvious results.
                                    - **2nd Order (System Cascade) Ripple**: The knock-on effects that occur once the system reacts.
                                    - **3rd Order (Evolutionary Loop) Shift**: The long-term behavioral, structural, and ontological adjustments.
                                    Be stark, analytical, and highly structured. Do not use generic chat style or fluff.
                                """.trimIndent()
                            )
                        )
                    )
                )

                var insightText: String? = null
                val modelsToTry = listOf("gemini-2.5-flash", "gemini-3.5-flash", "gemini-3.1-pro-preview")
                for (modelName in modelsToTry) {
                    try {
                        val response = com.example.data.network.RetrofitClient.service.generateContent(modelName, apiKey, request)
                        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (!text.isNullOrEmpty()) {
                            insightText = text
                            break
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val result = insightText ?: "Error generating deep-dive insight from the service. Please verify your connection and try again."
                _deepDiveInsights.value = _deepDiveInsights.value + (sessionId to result)
                prefs.edit().putString("deep_dive_$sessionId", result).apply()
            } catch (e: Exception) {
                _deepDiveInsights.value = _deepDiveInsights.value + (sessionId to "Error: ${e.message}")
            } finally {
                _isDeepDiveLoading.value = _isDeepDiveLoading.value + (sessionId to false)
            }
        }
    }

    fun loginWithGoogle(email: String, fullName: String) {
        val googleId = "google_" + java.util.UUID.randomUUID().toString().substring(0, 8)
        userId.value = googleId
        userName.value = fullName
        userEmail.value = email
        isLoggedIn.value = true
        isGuest.value = false
        
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putBoolean("is_guest", false)
            putString("user_id", googleId)
            putString("user_name", fullName)
            putString("user_email", email)
            apply()
        }

        // Merge original local Guest sessions to the cloud!
        viewModelScope.launch {
            try {
                val currentSessions = repository.allSessionsFlow.first()
                currentSessions.forEach { s ->
                    com.example.data.network.CloudSyncService.uploadSession(googleId, s.id, s.title, s.isPinned, s.createdAt, s.lastUpdatedAt)
                    val msgs = repository.getMessagesFlow(s.id).first()
                    msgs.forEach { m ->
                        com.example.data.network.CloudSyncService.uploadMessage(googleId, m.id, m.sessionId, m.role, m.text, m.imageUri, m.timestamp)
                    }
                }
                repository.allMemoryInsightsFlow.first().forEach { m ->
                    com.example.data.network.CloudSyncService.uploadMemoryInsight(googleId, m.id, m.category, m.content, m.timestamp)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loginAsGuest(fullName: String) {
        isLoggedIn.value = false
        isGuest.value = true
        userId.value = "guest_local"
        userName.value = fullName.ifBlank { "Guest Explorer" }
        userEmail.value = ""
        
        prefs.edit().apply {
            putBoolean("is_logged_in", false)
            putBoolean("is_guest", true)
            putString("user_id", "guest_local")
            putString("user_name", fullName.ifBlank { "Guest Explorer" })
            putString("user_email", "")
            apply()
        }
    }

    fun signOut() {
        isLoggedIn.value = false
        isGuest.value = false
        userId.value = "guest_local"
        userName.value = "Guest Explorer"
        userEmail.value = ""
        
        prefs.edit().apply {
            putBoolean("is_logged_in", false)
            putBoolean("is_guest", false)
            putString("user_id", "guest_local")
            putString("user_name", "Guest Explorer")
            putString("user_email", "")
            apply()
        }
    }

    fun saveGithubSettings(token: String, repo: String) {
        githubToken.value = token
        repoOwnerAndName.value = repo
        prefs.edit().apply {
            putString("github_token", token)
            putString("github_repo", repo)
            apply()
        }
    }

    fun archiveInsight(sessionId: String, query: String, introTitle: String, jsonContent: String) {
        viewModelScope.launch {
            val insight = ArchivedInsightEntity(
                id = java.util.UUID.randomUUID().toString(),
                sessionId = sessionId,
                query = query,
                introTitle = introTitle,
                jsonContent = jsonContent,
                timestamp = System.currentTimeMillis()
            )
            repository.insertArchivedInsight(insight)
        }
    }

    fun deleteArchivedInsight(id: String) {
        viewModelScope.launch {
            repository.deleteArchivedInsight(id)
        }
    }

    fun goDeeper(associatedUserMessageText: String) {
        val sessionId = _activeSessionId.value ?: return
        viewModelScope.launch {
            try {
                // Insert a special scan requested message
                repository.insertUserMessage(sessionId, "🔍 Deep-Lens scanning requested on: '$associatedUserMessageText'")
                
                // Prompt template for Go Deeper re-analysis
                val specialPrompt = """
                    The user has active interest to GO DEEPER on the following query: '$associatedUserMessageText'.
                    
                    Re-analyze this EXACT theme, meticulously deconstructing it across five levels of reality. Format your output strictly using these XML tags:
                    
                    <summary>
                    Deeper Reality Reconstruction successfully initiated for '$associatedUserMessageText'. Expanding spectrum layers...
                    </summary>
                    
                    <depth>
                    Level 1 - Surface Reality: [Visual/Explicit/Public observation layer, claims, outer postures, verbal symbols]
                    
                    Level 5 - Psychological Reality: [Underlying psychodynamics, unconscious protection layers, core fears, hidden need drivers]
                    
                    Level 7 - Systemic Reality: [Complex feedback structures, game-theory incentive constraints, underlying structural forces]
                    
                    Level 9 - Root Cause Reality: [Strategic root cause assessment, why this situation exists fundamentally]
                    
                    Level 10 - Probability Reality: [Futuristic timeline scenario trees, early warning indicator metrics]
                    </depth>
                    
                    <memory_insight>
                    • Deeper exploration requested: '$associatedUserMessageText'
                    • Analytical lens expansion applied to clarify structural assumptions
                    </memory_insight>
                    
                    <suggested>
                    ✓ What is the chief blindspot?
                    ✓ How can we disrupt this defensive loop?
                    </suggested>
                """.trimIndent()
                
                repository.generateAnalysis(sessionId, customInstructionOverride = specialPrompt)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun submitFeedback(category: String, message: String, email: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val pInfo = try { getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0) } catch (e: Exception) { null }
            val appVer = pInfo?.versionName ?: "3.0.2"
            
            // Send to Firestore
            val success = com.example.data.network.CloudSyncService.submitFeedback(
                userId = userId.value,
                userName = userName.value,
                email = email.ifBlank { userEmail.value },
                message = message,
                appVersion = appVer,
                category = category
            )
            
            // Optionally, create GitHub Issue
            val tokenVal = githubToken.value
            val repoVal = repoOwnerAndName.value
            if (tokenVal.isNotBlank() && repoVal.isNotBlank()) {
                val title = "[$category Feedback] from ${userName.value}"
                val body = """
                    Class: $category
                    User: ${userName.value} ($email)
                    User ID: ${userId.value}
                    App Version: $appVer
                    
                    Feedback Message:
                    $message
                """.trimIndent()
                com.example.data.network.CloudSyncService.submitGithubIssue(tokenVal, repoVal, title, body)
            }
            
            onComplete(success)
        }
    }

    fun submitBugReport(message: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val pInfo = try { getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0) } catch (e: Exception) { null }
            val appVer = pInfo?.versionName ?: "3.0.2"
            val deviceModel = android.os.Build.MODEL ?: "Unknown Device"
            val androidVer = android.os.Build.VERSION.RELEASE ?: "Unknown Android"
            val deviceInfo = "$deviceModel (Android $androidVer)"
            
            val success = com.example.data.network.CloudSyncService.submitBugReport(
                userId = userId.value,
                userName = userName.value,
                email = userEmail.value,
                description = message,
                deviceInfo = deviceInfo,
                androidVersion = androidVer,
                appVersion = appVer
            )
            
            // Optionally compile GitHub issue
            val tokenVal = githubToken.value
            val repoVal = repoOwnerAndName.value
            if (tokenVal.isNotBlank() && repoVal.isNotBlank()) {
                val title = "[Bug Report] System Fault Telemetry"
                val body = """
                    User: ${userName.value}
                    User Email: ${userEmail.value}
                    User ID: ${userId.value}
                    System Telemetry: $deviceInfo | App version: $appVer
                    
                    Details of incident:
                    $message
                """.trimIndent()
                com.example.data.network.CloudSyncService.submitGithubIssue(tokenVal, repoVal, title, body)
            }
            
            onComplete(success)
        }
    }

    private fun generateUniqueSessionName(mode: String): String {
        val topicsByMode = mapOf(
            "Root Cause" to listOf(
                "Origin Pattern Study", "Causal Chain Analysis", "Source Mapping Trace",
                "Root Factor Probe", "Deep Cause Inquiry", "Foundation Analysis",
                "Trigger Sequence Study", "Core Driver Audit", "Underlying Force Map"
            ),
            "Psychology" to listOf(
                "Cognitive Pattern Scan", "Behavioral Motive Audit", "Mental Model Probe",
                "Psychological Driver Study", "Belief System Map", "Emotional Trigger Trace",
                "Bias Detection Study", "Subconscious Pattern Audit", "Identity Lens Analysis"
            ),
            "Systems" to listOf(
                "Feedback Loop Scan", "System Dynamics Map", "Incentive Structure Audit",
                "Network Effect Probe", "Systemic Leverage Study", "Loop Analysis Trace",
                "Equilibrium Pattern Map", "Emergent Behavior Study", "System Blind Spot Audit"
            ),
            "Probability" to listOf(
                "Outcome Probability Map", "Timeline Likelihood Study", "Risk Scenario Probe",
                "Bayesian Path Analysis", "Probability Tree Audit", "Expected Value Trace",
                "Uncertainty Field Scan", "Decision Probability Study", "Scenario Weight Map"
            ),
            "Business" to listOf(
                "Strategic Position Audit", "Market Dynamic Study", "Growth Lever Map",
                "Competitive Moat Analysis", "Revenue Model Probe", "Value Chain Scan",
                "Business Model Trace", "Organizational Driver Study", "Opportunity Gap Map"
            ),
            "Relationships" to listOf(
                "Interpersonal Dynamic Audit", "Attachment Pattern Study", "Bond Structure Map",
                "Relationship Driver Probe", "Communication Pattern Scan", "Trust Fabric Analysis",
                "Social Dynamic Trace", "Conflict Pattern Study", "Connection Depth Map"
            ),
            "Spiritual" to listOf(
                "Purpose Alignment Probe", "Values Clarity Audit", "Inner Growth Map",
                "Meaning Pattern Study", "Higher Principle Trace", "Spiritual Lens Analysis",
                "Core Values Scan", "Life Purpose Map", "Growth Pathway Study"
            ),
            "Decision Making" to listOf(
                "Decision Framework Audit", "Risk-Benefit Map", "Choice Architecture Study",
                "Heuristic Bias Probe", "Trade-off Analysis Trace", "Strategic Choice Scan",
                "Decision Quality Map", "Option Evaluation Study", "Choice Driver Audit"
            ),
            "Multi-Layer" to listOf(
                "Reality Architecture Scan", "Multi-Dimensional Audit", "Full-Spectrum Analysis",
                "Deep-Layer Probe", "Reality Tunnel Trace", "Ontological Pattern Map",
                "Consciousness Layer Study", "Meta-Pattern Audit", "Invisible Architecture Scan"
            )
        )
        val topics = topicsByMode[mode] ?: topicsByMode["Root Cause"]!!
        val index = (System.currentTimeMillis() % topics.size).toInt()
        return topics[index]
    }

    fun isVagueOrShort(query: String): Boolean {
        val clean = query.trim().lowercase()
        if (clean.isEmpty()) return true
        if (clean.length <= 10) return true
        val stopWords = setOf("hello", "hi", "test", "hey", "help", "start", "go", "query", "please", "analyse", "analyze", "depthlens", "anyone there", "ok", "yes", "no", "thanks", "thank you", "diagnostic", "assessment")
        if (clean in stopWords) return true
        val alphabetChars = clean.count { it.isLetter() }
        if (alphabetChars < 4) return true
        return false
    }

    fun getTemporaryTitleForMode(mode: String): String {
        return when (mode) {
            "Root Cause" -> "Root Cause Analysis Brief"
            "Psychology" -> "Psychological Analysis Brief"
            "Systems" -> "Systems Dynamics Analysis"
            "Probability" -> "Probability Analysis Study"
            "Business" -> "Business Strategy Study"
            "Relationships" -> "Interpersonal Dynamic Inquiry"
            "Spiritual" -> "Alignment Analysis Study"
            else -> "Strategic Reality Analysis"
        }
    }
}
