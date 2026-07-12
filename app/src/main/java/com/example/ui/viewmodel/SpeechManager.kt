package com.example.ui.viewmodel

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    
    private val _currentPlayingMessageId = MutableStateFlow<String?>(null)
    val currentPlayingMessageId = _currentPlayingMessageId.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentSpokenText = MutableStateFlow("")
    val currentSpokenText = _currentSpokenText.asStateFlow()
    
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()
    
    private val prefs = context.getSharedPreferences("speech_prefs", Context.MODE_PRIVATE)
    
    private var isInitialized = false
    private var pendingText: String? = null
    private var pendingMsgId: String? = null
    private var pendingLocale: Locale? = null

    // Audio focus & Recovery variables
    private var lastMessageRawText: String? = null
    private var lastMessageCleanText: String? = null
    private var wasInterrupted = false
    private var interruptedMessageId: String? = null
    private var interruptedText: String? = null
    private var interruptedIsFinished = false
    private var interruptedIsStream = false
    private var focusRequest: android.media.AudioFocusRequest? = null

    private val afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d("SpeechManager", "Audio focus loss transient/duck: pausing TTS playback")
                if (_isPlaying.value) {
                    wasInterrupted = true
                    interruptedMessageId = _currentPlayingMessageId.value ?: streamingMessageId
                    interruptedText = lastMessageRawText ?: lastMessageCleanText
                    interruptedIsFinished = streamingFinished
                    interruptedIsStream = streamingMessageId != null

                    // Pause/Stop actual TTS
                    tts?.stop()
                    _isPlaying.value = false
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d("SpeechManager", "Audio focus gained: keeping user interruption respected")
                wasInterrupted = false
                interruptedMessageId = null
                interruptedText = null
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d("SpeechManager", "Audio focus lost permanently: stopping playback")
                wasInterrupted = false
                stop()
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val result = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val attr = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val request = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attr)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(afChangeListener)
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                afChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(afChangeListener)
        }
    }

    // Streaming tracking variables
    private var streamingMessageId: String? = null
    private var highestSubmittedIndex = -1
    private var highestFinishedIndex = -1
    private var streamingFinished = false
    private var lastSpokenCharIndex = 0

    init {
        _playbackSpeed.value = prefs.getFloat("speed", 1.0f)
        tts = TextToSpeech(context.applicationContext, this)
        // Allow tap-to-interrupt (barge-in) to stop ongoing speech immediately.
        AudioConversationManager.ttsStop = { stop() }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isPlaying.value = true
                }

                override fun onDone(utteranceId: String?) {
                    handleUtteranceDone(utteranceId)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    handleUtteranceDone(utteranceId)
                }
                
                override fun onError(utteranceId: String?, errorCode: Int) {
                    handleUtteranceDone(utteranceId)
                }
            })
            // If we had a pending speak request, perform it now
            val text = pendingText
            val msgId = pendingMsgId
            val loc = pendingLocale
            if (text != null && msgId != null && loc != null) {
                speakNow(msgId, text, loc)
                pendingText = null
                pendingMsgId = null
                pendingLocale = null
            }
        } else {
            Log.e("SpeechManager", "TTS initialization failed")
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        prefs.edit().putFloat("speed", speed).apply()
        if (isInitialized) {
            tts?.setSpeechRate(speed)
        }
    }

    fun speak(messageId: String, text: String) {
        val cleanText = stripMarkdown(text)
        val locale = detectLanguageAndSelectLocale(cleanText)
        
        lastMessageCleanText = cleanText
        lastMessageRawText = null
        interruptedIsStream = false

        if (isSpeakingMessage(messageId)) {
            stop()
            return
        }

        stop() // stop current speech if any
        
        _currentPlayingMessageId.value = messageId
        requestAudioFocus()
        
        if (isInitialized) {
            speakNow(messageId, cleanText, locale)
        } else {
            pendingText = cleanText
            pendingMsgId = messageId
            pendingLocale = locale
        }
    }

    private fun speakNow(messageId: String, cleanText: String, locale: Locale) {
        val ttsInstance = tts ?: return
        
        // Apply fallback logic and verify language availability
        val resolvedLocale = resolveAvailableVoice(ttsInstance, locale)
        
        val speed = prefs.getFloat("speed", 1.0f)
        _playbackSpeed.value = speed
        ttsInstance.setSpeechRate(speed)
        
        val params = android.os.Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, messageId)
        }
        
        _isPlaying.value = true
        _currentSpokenText.value = cleanText
        try {
            AudioConversationManager.noteAiSpokenText(cleanText)
            AudioConversationManager.onTtsStarted()
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error calling AudioConversationManager.onTtsStarted", e)
        }
        ttsInstance.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, messageId)
    }

    fun stop() {
        tts?.stop()
        abandonAudioFocus()
        _isPlaying.value = false
        _currentPlayingMessageId.value = null
        _currentSpokenText.value = ""
        streamingMessageId = null
        highestSubmittedIndex = -1
        highestFinishedIndex = -1
        streamingFinished = false
        lastSpokenCharIndex = 0
        lastMessageRawText = null
        lastMessageCleanText = null
        wasInterrupted = false
        try {
            AudioConversationManager.onTtsFinished()
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error calling AudioConversationManager.onTtsFinished", e)
        }
    }

    fun resetState() {
        lastUserSpeechLocale = null
    }

    fun isSpeakingMessage(messageId: String): Boolean {
        return _isPlaying.value && _currentPlayingMessageId.value == messageId
    }

    fun playStreamProgress(messageId: String, text: String, isFinished: Boolean) {
        lastMessageRawText = text
        lastMessageCleanText = null
        interruptedIsStream = true

        if (streamingMessageId != messageId) {
            // Stop any ongoing standard/other playback
            stop()
            // New stream initiated! Reset trackers
            streamingMessageId = messageId
            _currentPlayingMessageId.value = messageId
            requestAudioFocus()
        }

        val cleanText = stripMarkdown(text)
        // Extract complete sentences from the buffer
        val newText = if (lastSpokenCharIndex < cleanText.length) {
            cleanText.substring(lastSpokenCharIndex)
        } else {
            ""
        }

        if (newText.isEmpty()) {
            if (isFinished && !streamingFinished) {
                markStreamFinished()
            }
            return
        }

        // Search for sentence boundaries in 'newText'
        // We look for '.', '?', '!', '\n', ':', ';', or closing XML tags like '>'
        var searchIndex = 0
        while (searchIndex < newText.length) {
            val char = newText[searchIndex]
            val isPunctuation = char == '.' || char == '?' || char == '!' || char == '\n' || char == ':' || char == ';'
            if (isPunctuation) {
                // We found a complete sentence boundary from lastSpokenCharIndex to this punctuation mark!
                val segment = newText.substring(0, searchIndex + 1).trim()
                if (segment.isNotEmpty()) {
                    // Submit this segment to queue!
                    enqueueSegment(segment)
                }
                // Advance lastSpokenCharIndex
                lastSpokenCharIndex += searchIndex + 1
                playStreamProgress(messageId, cleanText, isFinished) // Recurse to process the remaining part of newText
                return
            }
            searchIndex++
        }

        // If the API generation is fully finished, submit any remaining left-over text
        if (isFinished && !streamingFinished) {
            val remainingSegment = cleanText.substring(lastSpokenCharIndex).trim()
            if (remainingSegment.isNotEmpty()) {
                enqueueSegment(remainingSegment)
                lastSpokenCharIndex = cleanText.length
            }
            markStreamFinished()
        }
    }

    private fun enqueueSegment(segment: String) {
        val cleanSegment = stripMarkdown(segment)
        if (cleanSegment.isBlank() || cleanSegment.replace("[^a-zA-Z0-9]".toRegex(), "").isBlank()) {
            // Skip pure formatting or punctuation-only segments
            return
        }

        val locale = detectLanguageAndSelectLocale(cleanSegment)
        val ttsInstance = tts ?: return
        
        highestSubmittedIndex++
        val chunkUtteranceId = "$streamingMessageId-chunk-$highestSubmittedIndex"
        
        val resolvedLocale = resolveAvailableVoice(ttsInstance, locale)
        ttsInstance.language = resolvedLocale
        
        val speed = prefs.getFloat("speed", 1.0f)
        _playbackSpeed.value = speed
        ttsInstance.setSpeechRate(speed)
        
        val params = android.os.Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, chunkUtteranceId)
        }
        
        _isPlaying.value = true
        
        // For the absolute first segment, flush queue for zero latency!
        val queueMode = if (highestSubmittedIndex == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        
        _currentSpokenText.value = cleanSegment
        try {
            AudioConversationManager.noteAiSpokenText(cleanSegment)
            AudioConversationManager.onTtsStarted()
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error calling AudioConversationManager.onTtsStarted", e)
        }
        ttsInstance.speak(cleanSegment, queueMode, params, chunkUtteranceId)
        Log.d("SpeechManager", "Queued segment #$highestSubmittedIndex: '$cleanSegment' with mode $queueMode")
    }

    private fun markStreamFinished() {
        streamingFinished = true
        if (highestFinishedIndex >= highestSubmittedIndex) {
            _isPlaying.value = false
            _currentPlayingMessageId.value = null
            abandonAudioFocus()
        }
    }

    private fun handleUtteranceDone(utteranceId: String?) {
        if (utteranceId != null && utteranceId.contains("-chunk-")) {
            val parts = utteranceId.split("-chunk-")
            val msgId = parts[0]
            val index = parts.getOrNull(1)?.toIntOrNull() ?: -1
            if (msgId == streamingMessageId) {
                if (index > highestFinishedIndex) {
                    highestFinishedIndex = index
                }
                if (streamingFinished && highestFinishedIndex >= highestSubmittedIndex) {
                    _isPlaying.value = false
                    _currentPlayingMessageId.value = null
                    abandonAudioFocus()
                    try {
                        AudioConversationManager.onTtsFinished()
                    } catch (e: Exception) {
                        Log.e("SpeechManager", "Error calling AudioConversationManager.onTtsFinished", e)
                    }
                }
            }
        } else {
            _isPlaying.value = false
            _currentPlayingMessageId.value = null
            abandonAudioFocus()
            try {
                AudioConversationManager.onTtsFinished()
            } catch (e: Exception) {
                Log.e("SpeechManager", "Error calling AudioConversationManager.onTtsFinished", e)
            }
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }

    private fun stripMarkdown(text: String): String {
        return text
            // Strip XML tags like <summary>, <depth>, etc.
            .replace(Regex("<[^>]*>"), "")
            // Strip markdown block-quotes, asterisks, brackets, hashes, etc.
            .replace(Regex("[*#_`~>•✓]"), " ")
            .replace(Regex("\\[(.*?)\\]"), "$1")
            .replace(Regex("\\((.*?)\\)"), "$1")
            .split("\n")
            .filter { it.trim().isNotEmpty() }
            .joinToString(". ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun detectLanguageAndSelectLocale(text: String): Locale {
        val autoLang = prefs.getBoolean("auto_language", true)
        if (!autoLang) {
            return getSelectedLocale()
        }
        
        try {
            val depthlensPrefs = context.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
            val activeSessionId = depthlensPrefs.getString("last_active_session_id", null)
            if (activeSessionId != null) {
                val sessionLang = depthlensPrefs.getString("language_session_$activeSessionId", null)
                if (sessionLang != null) {
                    when (sessionLang) {
                        "GUJARATI_SCRIPT", "GUJLISH" -> return Locale("gu", "IN")
                        "HINDI_DEVANAGARI", "HINGLISH" -> return Locale("hi", "IN")
                        "ENGLISH" -> return Locale.US
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error reading session language preference", e)
        }

        var countHi = 0 // Devanagari (\u0900 - \u097F)
        var countGu = 0 // Gujarati (\u0A80 - \u0AFF)
        var countJa = 0 // Japanese Kana (\u3040 - \u30FF)
        var countKo = 0 // Korean Hangul (\uAC00 - \uD7AF)
        var countZh = 0 // CJK Unified Ideographs (\u4E00 - \u9FFF)
        var countAr = 0 // Arabic (\u0600 - \u06FF)
        var countRu = 0 // Cyrillic (\u0400 - \u04FF)
        var countLatin = 0 // Latin/English

        for (char in text) {
            val code = char.code
            when {
                code in 0x0900..0x097F -> countHi++
                code in 0x0A80..0x0AFF -> countGu++
                code in 0x3040..0x30FF -> countJa++
                code in 0xAC00..0xD7AF -> countKo++
                code in 0x4E00..0x9FFF -> countZh++
                code in 0x0600..0x06FF -> countAr++
                code in 0x0400..0x04FF -> countRu++
                char.isLetter() && char.toString().matches(Regex("[a-zA-Z]")) -> countLatin++
            }
        }

        // Determine dominant non-Latin languages
        val maxMap = mapOf(
            "hi" to countHi,
            "gu" to countGu,
            "ja" to countJa,
            "ko" to countKo,
            "zh" to countZh,
            "ar" to countAr,
            "ru" to countRu
        )

        val dominantNonLatin = maxMap.maxByOrNull { it.value }
        if (dominantNonLatin != null && dominantNonLatin.value > 2) {
            return when (dominantNonLatin.key) {
                "hi" -> Locale("hi", "IN")
                "gu" -> Locale("gu", "IN")
                "ja" -> Locale.JAPAN
                "ko" -> Locale.KOREAN
                "zh" -> Locale.SIMPLIFIED_CHINESE
                "ar" -> Locale("ar")
                "ru" -> Locale("ru", "RU")
                else -> Locale.US
            }
        }

        // If it uses primarily Latin script, check for Spanish/French/German/English/Hinglish/Gujlish
        if (countLatin > 0) {
            val lower = " $text ".lowercase().replace(Regex("[.,?!()\\-]"), " ")
            
            // 1. Check for Gujlish (Romanized Gujarati) keywords
            val gujlishKeywords = listOf(
                " kem ", " tame ", " karo ", " maza ", " maru ", " chale ", " nathi ", " thayu ", 
                " chhe ", " shu ", " shum ", " aavjo ", " jo ", " badhu ", " barabar ", " nava ", 
                " khabar ", " tamaru ", " amaru ", " bapu ", " motabhai ", " patel ", " su ",
                " che ", " cho ", " sathe ", " ane ", " pan "
            )
            val gujlishCount = gujlishKeywords.count { lower.contains(it) }
            if (gujlishCount >= 1 || lower.contains(" kem cho ") || lower.contains(" maza ma ")) {
                return Locale("gu", "IN") // Return Gujarati for Gujlish so it reads with a native Indian accent!
            }

            // 2. Check for Hinglish keywords written in English alphabet
            val hinglishKeywords = listOf(
                " hai ", " hain ", " aap ", " kaise ", " tum ", " kya ", " kar ", " rahe ", " mai ", " mera ", 
                " apka ", " achha ", " accha ", " shukriya", " bhai ", " namaste", " bahut ", " theek ", 
                " thik ", " kiya ", " diya ", " hoga ", " yaar ", " chal ", " gaya ", " kuch ", " samjhe ", 
                " samajh ", " aaya ", " hua ", " mujhe ", " tujhe ", " apne ", " liye ", " karke ", 
                " sath ", " saath ", " log ", " rha ", " rhi ", " rhey ", " rhen ", " didi ", " bhaiya ", " dost ",
                " ko ", " ki ", " ke ", " ka ", " se ", " aur ", " bhi ", " ek ", " ye ", " toh ", " kiske ", " banaya ",
                " banayi ", " bada ", " dosto "
            )
            val hinglishCount = hinglishKeywords.count { lower.contains(it) }
            val hasIndianEnglishIndicators = lower.contains("india") || lower.contains("indian") || lower.contains("rupee") || lower.contains("lakh") || lower.contains("crore")
            
            if (hinglishCount >= 1 || lower.startsWith(" haan ") || lower.contains(" haan ") || lower.endsWith(" haan ")) {
                return Locale("hi", "IN") // Return Hindi for Hinglish so it reads with a native Indian Hindi/Hinglish accent!
            }
            if (hasIndianEnglishIndicators) {
                return Locale("en", "IN")
            }

            // Check for German indicators
            val germanKeywords = listOf(" und ", " der ", " die ", " das ", " ist ", " mit ", " von ", " eine ", " ein ")
            val hasGermanChars = lower.contains("ä") || lower.contains("ö") || lower.contains("ü") || lower.contains("ß")
            var countDe = germanKeywords.count { lower.contains(it) } * 3 + (if (hasGermanChars) 5 else 0)

            // Check for Spanish indicators
            val spanishKeywords = listOf(" el ", " la ", " en ", " de ", " los ", " con ", " para ", " una ", " que ", " por ")
            val hasSpanishChars = lower.contains("í") || lower.contains("ó") || lower.contains("á") || lower.contains("ú") || lower.contains("ñ")
            var countEs = spanishKeywords.count { lower.contains(it) } * 3 + (if (hasSpanishChars) 5 else 0)

            // Check for French indicators
            val frenchKeywords = listOf(" et ", " le ", " la ", " en ", " les ", " pour ", " dans ", " est ", " une ", " pour ")
            val hasFrenchChars = lower.contains("é") || lower.contains("è") || lower.contains("à") || lower.contains("ù") || lower.contains("ç")
            var countFr = frenchKeywords.count { lower.contains(it) } * 3 + (if (hasFrenchChars) 5 else 0)

            // Check for Italian indicators
            val italianKeywords = listOf(" il ", " la ", " di ", " che ", " in ", " per ", " con ", " una ", " un ")
            var countIt = italianKeywords.count { lower.contains(it) } * 3

            // Check for Portuguese indicators
            val portugueseKeywords = listOf(" o ", " a ", " de ", " que ", " em ", " para ", " com ", " uma ", " um ")
            var countPt = portugueseKeywords.count { lower.contains(it) } * 3

            val maxLatinLang = mapOf(
                Locale.GERMANY to countDe,
                Locale("es", "ES") to countEs,
                Locale.FRANCE to countFr,
                Locale.ITALY to countIt,
                Locale("pt", "PT") to countPt
            ).maxByOrNull { it.value }

            if (maxLatinLang != null && maxLatinLang.value > 4) {
                return maxLatinLang.key
            }
        }

        return getSelectedLocale()
    }

    private var lastUserSpeechLocale: Locale? = null

    fun trackUserSpeech(text: String) {
        val cleanText = " $text ".lowercase().replace(Regex("[.,?!()\\-]"), " ")
        var countHi = 0
        var countGu = 0
        for (char in text) {
            val code = char.code
            if (code in 0x0900..0x097F) {
                countHi++
            }
            if (code in 0x0A80..0x0AFF) {
                countGu++
            }
        }
        
        if (countGu > 1) {
            lastUserSpeechLocale = Locale("gu", "IN")
            Log.d("SpeechManager", "User speech detected as Gujarati (Gujarati script)")
            return
        }

        if (countHi > 1) {
            lastUserSpeechLocale = Locale("hi", "IN")
            Log.d("SpeechManager", "User speech detected as Hindi (Devanagari)")
            return
        }

        // Gujlish (Romanized Gujarati) check
        val gujlishKeywords = listOf(
            " kem ", " tame ", " karo ", " maza ", " maru ", " chale ", " nathi ", " thayu ", 
            " chhe ", " shu ", " shum ", " aavjo ", " jo ", " badhu ", " barabar ", " nava ", 
            " khabar ", " tamaru ", " amaru ", " bapu ", " motabhai ", " patel ", " su ",
            " che ", " cho ", " sathe ", " ane ", " pan "
        )
        val gujlishCount = gujlishKeywords.count { cleanText.contains(it) }
        if (gujlishCount >= 1 || cleanText.contains(" kem cho ") || cleanText.contains(" maza ma ")) {
            lastUserSpeechLocale = Locale("gu", "IN")
            Log.d("SpeechManager", "User speech detected as Romanized Gujarati (Gujlish)")
            return
        }

        // Hinglish / Romanized Hindi check
        val hinglishKeywords = listOf(
            " hai ", " hain ", " aap ", " kaise ", " tum ", " kya ", " kar ", " rahe ", " mai ", " mera ", 
            " apka ", " achha ", " accha ", " shukriya", " bhai ", " namaste", " bahut ", " theek ", 
            " thik ", " kiya ", " diya ", " hoga ", " yaar ", " chal ", " gaya ", " kuch ", " samjhe ", 
            " samajh ", " aaya ", " hua ", " mujhe ", " tujhe ", " apne ", " liye ", " karke ", 
            " sath ", " saath ", " log ", " rha ", " rhi ", " rhey ", " rhen ", " didi ", " bhaiya ", " dost ",
            " haan ", " na ", " yaar ", " chal ", " bolo ", " baat ", " suno ", " batao ", " kyu ", " kyun ",
            " ko ", " ki ", " ke ", " ka ", " se ", " aur ", " bhi ", " ek ", " ye ", " toh ", " kiske ", " banaya ",
            " banayi ", " bada ", " dosto "
        )
        val hinglishCount = hinglishKeywords.count { cleanText.contains(it) }
        if (hinglishCount >= 1 || cleanText.startsWith(" haan ") || cleanText.contains(" haan ") || cleanText.endsWith(" haan ")) {
            lastUserSpeechLocale = Locale("hi", "IN") // Hindi voice for Hinglish so it reads with a native Indian Hindi accent!
            Log.d("SpeechManager", "User speech detected as Hinglish")
            return
        }

        val indianEnglishKeywords = listOf(
            " india ", " indian ", " rupee ", " rupees ", " lakh ", " lakhs ", " crore ", " crores ", 
            " gst ", " aadhar ", " pan card ", " delhi ", " mumbai ", " bangalore ", " ahmedabad ", " gujarat "
        )
        val isIndianEnglish = indianEnglishKeywords.any { cleanText.contains(it) }
        if (isIndianEnglish || Locale.getDefault().country.equals("IN", ignoreCase = true)) {
            lastUserSpeechLocale = Locale("en", "IN")
            Log.d("SpeechManager", "User speech detected as Indian English")
            return
        }

        val defaultLocale = Locale.getDefault()
        if (defaultLocale.language.equals("en", ignoreCase = true)) {
            lastUserSpeechLocale = defaultLocale
            Log.d("SpeechManager", "User speech detected as regional English: $defaultLocale")
            return
        }

        lastUserSpeechLocale = Locale.US
        Log.d("SpeechManager", "User speech detected as US English")
    }

    fun getSelectedLocale(): Locale {
        val accentStr = prefs.getString("voice_accent", "en_US") ?: "en_US"
        return when (accentStr) {
            "en_US" -> Locale.US
            "en_GB" -> Locale.UK
            "en_AU" -> Locale("en", "AU")
            "en_IN" -> Locale("en", "IN")
            "hi_IN" -> Locale("hi", "IN")
            "gu_IN" -> Locale("gu", "IN")
            "es_ES" -> Locale("es", "ES")
            "fr_FR" -> Locale("fr", "FR")
            else -> Locale.US
        }
    }

    private fun accentFallbackCountries(locale: Locale): List<String> {
        return when (locale.language.lowercase()) {
            "gu", "hi", "bn", "ta", "te", "mr", "pa", "kn", "ml", "ur" -> listOf("IN")
            "en" -> when (locale.country.uppercase()) {
                "IN" -> listOf("IN", "GB", "AU")
                "GB" -> listOf("GB", "IE", "AU")
                "AU" -> listOf("AU", "GB")
                else -> listOf(locale.country.uppercase()).filter { it.isNotBlank() }
            }
            "es" -> listOf(locale.country.uppercase(), "ES", "MX", "US").filter { it.isNotBlank() }.distinct()
            "fr" -> listOf(locale.country.uppercase(), "FR", "CA").filter { it.isNotBlank() }.distinct()
            "pt" -> listOf(locale.country.uppercase(), "PT", "BR").filter { it.isNotBlank() }.distinct()
            "ar" -> listOf(locale.country.uppercase(), "SA", "AE", "EG").filter { it.isNotBlank() }.distinct()
            else -> listOf(locale.country.uppercase()).filter { it.isNotBlank() }
        }
    }

    private fun resolveAvailableVoice(ttsInstance: TextToSpeech, requestedLocale: Locale): Locale {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val voices = ttsInstance.voices
                if (!voices.isNullOrEmpty()) {
                    // 1. Try exact language and country matching
                    var bestVoice = voices.find { v ->
                        v.locale.language.equals(requestedLocale.language, ignoreCase = true) &&
                        v.locale.country.equals(requestedLocale.country, ignoreCase = true)
                    }
                    
                    // 2. Try language and country matched via language tag or display country
                    if (bestVoice == null && requestedLocale.country.isNotEmpty()) {
                        bestVoice = voices.find { v ->
                            v.locale.language.equals(requestedLocale.language, ignoreCase = true) &&
                            (v.locale.toLanguageTag().contains(requestedLocale.country, ignoreCase = true) ||
                             v.locale.displayCountry.equals(requestedLocale.displayCountry, ignoreCase = true))
                        }
                    }

                    // 3. Prefer same language with a nearby regional accent
                    if (bestVoice == null) {
                        val preferredCountries = accentFallbackCountries(requestedLocale)
                        bestVoice = preferredCountries.asSequence()
                            .mapNotNull { country ->
                                voices.find { v ->
                                    v.locale.language.equals(requestedLocale.language, ignoreCase = true) &&
                                    (v.locale.country.equals(country, ignoreCase = true) ||
                                     v.locale.toLanguageTag().contains(country, ignoreCase = true))
                                }
                            }
                            .firstOrNull()
                    }

                    if (bestVoice != null) {
                        Log.d("SpeechManager", "Successfully matched voice: ${bestVoice.name} for requested locale: $requestedLocale")
                        ttsInstance.language = bestVoice.locale // Set language first to avoid voice resetting!
                        // Avoid setting ttsInstance.voice directly as it often causes silent failures
                        // when the high-quality network/offline voice package is not fully downloaded.
                        // Setting language is the industry standard and most robust across all devices.
                        return bestVoice.locale
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error querying tts voices", e)
        }

        // IMPORTANT: If we didn't find any voice matching the country/accent, do NOT fall back to a generic
        // voice of the same language using ttsInstance.voice = bestVoice (which often forces en_US / american accent).
        // Instead, we just set the language directly, which lets the Android TTS engine resolve the native accent dynamically!
        val result = ttsInstance.isLanguageAvailable(requestedLocale)
        if (result >= TextToSpeech.LANG_AVAILABLE) {
            ttsInstance.language = requestedLocale
            return requestedLocale
        }
        
        // Fallback Logic:
        val lang = requestedLocale.language
        val fallbackLocale = when {
            lang == "gu" -> {
                // Gujarati fallback path: gu_IN -> hi_IN -> en_IN -> en_US
                val fallbackHi = Locale("hi", "IN")
                val fallbackEnIn = Locale("en", "IN")
                if (ttsInstance.isLanguageAvailable(requestedLocale) >= TextToSpeech.LANG_AVAILABLE) {
                    requestedLocale
                } else if (ttsInstance.isLanguageAvailable(fallbackHi) >= TextToSpeech.LANG_AVAILABLE) {
                    fallbackHi
                } else if (ttsInstance.isLanguageAvailable(fallbackEnIn) >= TextToSpeech.LANG_AVAILABLE) {
                    fallbackEnIn
                } else {
                    Locale.US
                }
            }
            lang == "hi" -> {
                // Hindi fallback path: hi_IN -> en_IN -> en_US
                val fallbackEnIn = Locale("en", "IN")
                if (ttsInstance.isLanguageAvailable(requestedLocale) >= TextToSpeech.LANG_AVAILABLE) {
                    requestedLocale
                } else if (ttsInstance.isLanguageAvailable(fallbackEnIn) >= TextToSpeech.LANG_AVAILABLE) {
                    fallbackEnIn
                } else {
                    Locale.US
                }
            }
            lang == "en" && requestedLocale.country == "IN" -> {
                // English India fallback path: en_IN -> en_US
                if (ttsInstance.isLanguageAvailable(requestedLocale) >= TextToSpeech.LANG_AVAILABLE) {
                    requestedLocale
                } else {
                    Locale.US
                }
            }
            else -> {
                // Return default/system language or English US as absolute backstops
                val defaultLoc = Locale.getDefault()
                if (ttsInstance.isLanguageAvailable(requestedLocale) >= TextToSpeech.LANG_AVAILABLE) {
                    requestedLocale
                } else if (ttsInstance.isLanguageAvailable(defaultLoc) >= TextToSpeech.LANG_AVAILABLE) {
                    defaultLoc
                } else {
                    Locale.US
                }
            }
        }
        ttsInstance.language = fallbackLocale
        return fallbackLocale
    }
}
