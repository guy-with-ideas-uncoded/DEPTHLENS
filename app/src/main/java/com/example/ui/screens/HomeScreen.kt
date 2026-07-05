package com.example.ui.screens

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.interaction.collectIsPressedAsState


import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Download
import androidx.core.content.FileProvider
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.ManageSearch
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.ResponseParser
import com.example.ui.theme.*
import com.example.ui.viewmodel.SpeechManager
import com.example.ui.components.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.content.Intent
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.util.Calendar
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow

@OptIn(ExperimentalLayoutApi::class)
// Strips raw markdown symbols that Compose Text() cannot render  
private fun stripMarkdown(text: String): String {
    return cleanResponseText(text)
}

// Cleans raw response text from XML/HTML tags, markdown rule symbols, stray internal system labels, and excessive empty lines
private fun cleanResponseText(text: String): String {
    if (text.isEmpty()) return text

    // First do XML tags removal if any remain
    var cleaned = text
        .replace(Regex("""```[\s\S]*?```"""), " ")
        .replace(Regex("""<[^>]+>"""), "")
        .replace(Regex("""(?i)\b(?:file|content)://\S+"""), " ")
        .replace(Regex("""(?i)/(?:root|data|storage|sdcard|tmp|var|home)/\S+"""), " ")
        .replace(Regex("""[A-Za-z]:\\[^\s]+"""), " ")

    // Normalize markdown and stray formatting symbols
    cleaned = cleaned
        // Replace bold **text** with just text
        .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
        // Replace italic *text* or _text_ with just text
        .replace(Regex("""\*(.*?)\*"""), "$1")
        .replace(Regex("""__(.*?)__"""), "$1")
        .replace(Regex("""_(.*?)_"""), "$1")
        // Remove markdown headers ##, ### etc.
        .replace(Regex("""^#{1,6}\s+""", RegexOption.MULTILINE), "")
        // Remove quote markers
        .replace(Regex("""^>\s+""", RegexOption.MULTILINE), "")
        // Replace list bullet points
        .replace(Regex("""^[-*+]\s""", RegexOption.MULTILINE), "• ")
        // Remove markdown line rules
        .replace("---", "")
        .replace("***", "")
        .replace("___", "")
        .replace(Regex("""["“”]{2,}"""), " ")
        .replace(Regex("""[`\\{}\[\]|]"""), " ")

    // Split into lines to process line-by-line:
    val lines = cleaned.split("\n")
    val processedLines = mutableListOf<String>()

    // Set of lowercase exact matches or prefix/sub-string matches for unwanted system lines
    val unwantedSubstrings = listOf(
        "emphasis:",
        "depth:",
        "analysis level",
        "reasoning allocation",
        "thinking mode active",
        "depthlens high thinking",
        "depthlens medium thinking",
        "depthlens low thinking",
        "stacktrace",
        "exception:",
        "at com.",
        "at java.",
        "/root/"
    )
    val unwantedExactMatches = setOf(
        "high",
        "medium",
        "low",
        "emphasis",
        "depth",
        "analysis",
        "thinking",
        "emphasis: high",
        "emphasis: medium",
        "emphasis: low",
        "depth: high",
        "depth: medium",
        "depth: low"
    )

    for (line in lines) {
        val trimmedLine = line.trim()
        val lowerLine = trimmedLine.lowercase()

        // 1. Skip system label lines
        if (unwantedExactMatches.contains(lowerLine)) {
            continue
        }
        var shouldSkip = false
        for (pattern in unwantedSubstrings) {
            if (lowerLine.contains(pattern)) {
                shouldSkip = true
                break
            }
        }
        if (shouldSkip) {
            continue
        }

        // 2. Filter empty lines or only whitespace, keeping them as empty strings
        if (trimmedLine.isEmpty()) {
            processedLines.add("")
        } else {
            processedLines.add(trimmedLine)
        }
    }

    // Now, join lines while keeping at most ONE consecutive blank line
    val finalLines = mutableListOf<String>()
    var consecutiveBlankCount = 0

    for (line in processedLines) {
        if (line.isEmpty()) {
            consecutiveBlankCount++
            if (consecutiveBlankCount <= 1) {
                finalLines.add("")
            }
        } else {
            consecutiveBlankCount = 0
            finalLines.add(line)
        }
    }

    return finalLines.joinToString("\n").trim()
}

private fun compactLiveCaption(text: String, maxWords: Int = 14): String {
    val cleaned = cleanResponseText(text).replace(Regex("\\s+"), " ").trim()
    if (cleaned.isEmpty()) return ""
    val words = cleaned.split(" ").filter { it.isNotBlank() }
    return if (words.size <= maxWords) cleaned else words.takeLast(maxWords).joinToString(" ")
}

private fun isStopSpeechCommand(text: String): Boolean {
    val cleaned = text.trim().lowercase()
    if (cleaned.isEmpty()) return false
    val normalized = cleaned.replace(Regex("[.,!?;:\"'`*_\\-]+"), " ").replace(Regex("\\s+"), " ").trim()
    val exact = setOf(
        "stop", "stop it", "stop now", "ruk", "ruko", "ruk jao", "bas", "band karo",
        "chup", "hold on", "wait", "cancel", "abort", "enough",
        "રોકો", "બંધ કરો", "बस", "रुको", "बंद करो"
    )
    return normalized in exact || normalized.startsWith("stop ") || normalized.startsWith("ruk ") || normalized.startsWith("ruko ")
}

private fun isSelfEcho(spokenText: String, ttsText: String): Boolean {
    if (spokenText.isBlank() || ttsText.isBlank()) return false
    
    val cleanSpoken = spokenText.lowercase().replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()
    val cleanTts = ttsText.lowercase().replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()
    
    if (cleanSpoken.isEmpty() || cleanTts.isEmpty()) return false
    
    // If the spoken text is a substring of the TTS text, it's definitely an echo!
    if (cleanTts.contains(cleanSpoken)) return true
    
    // Also check if a large percentage of words in cleanSpoken are present in cleanTts
    val spokenWords = cleanSpoken.split(" ").filter { it.isNotBlank() }
    if (spokenWords.isNotEmpty()) {
        var matchCount = 0
        for (word in spokenWords) {
            if (cleanTts.contains(word)) {
                matchCount++
            }
        }
        // If more than 75% of the spoken words are found in the TTS text, consider it an echo
        if (matchCount.toFloat() / spokenWords.size > 0.75f) {
            return true
        }
    }
    
    return false
}

// Parses simple markdown bold markers and section headers into proper visual styling (FontWeight/FontSize/Color) in AnnotatedString
private fun parseMarkdownToAnnotatedString(rawText: String): AnnotatedString {
    val cleanedText = cleanResponseText(rawText)
    
    return buildAnnotatedString {
        val lines = cleanedText.split("\n")
        lines.forEachIndexed { index, line ->
            // Check for heading lines
            val headingMatch = Regex("""^(#{1,6})\s+(.+)$""").find(line)
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].length
                val title = headingMatch.groupValues[2].trim()
                
                val fontSize = when (level) {
                    1 -> 18.sp
                    2 -> 16.sp
                    else -> 15.sp
                }
                
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        color = ElectricViolet
                    )
                ) {
                    append(title)
                }
            } else {
                // Parse inline bolding: **bold**
                var currentIndex = 0
                val length = line.length
                while (currentIndex < length) {
                    val nextBoldStart = line.indexOf("**", currentIndex)
                    if (nextBoldStart == -1) {
                        append(line.substring(currentIndex))
                        break
                    } else {
                        if (nextBoldStart > currentIndex) {
                            append(line.substring(currentIndex, nextBoldStart))
                        }
                        
                        val nextBoldEnd = line.indexOf("**", nextBoldStart + 2)
                        if (nextBoldEnd == -1) {
                            append(line.substring(nextBoldStart))
                            break
                        } else {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(line.substring(nextBoldStart + 2, nextBoldEnd))
                            }
                            currentIndex = nextBoldEnd + 2
                        }
                    }
                }
            }
            
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

data class SearchMatch(
    val messageId: String,
    val occurrenceIndex: Int
)

fun exportChatToPdf(
    context: android.content.Context,
    sessionTitle: String,
    messages: List<com.example.data.model.MessageEntity>,
    onComplete: (String) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        
        var pageNumber = 1
        var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        val paint = android.graphics.Paint()
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
            isAntiAlias = true
        }
        
        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(106, 27, 154) // Purple tint
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val metaPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 9f
            isAntiAlias = true
        }

        var y = 50f
        
        canvas.drawText("DepthLens Conversation Analysis", 40f, y, titlePaint)
        y += 22f
        canvas.drawText("Session: $sessionTitle", 40f, y, textPaint)
        y += 14f
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val dateStr = sdf.format(java.util.Date())
        canvas.drawText("Export Date: $dateStr", 40f, y, metaPaint)
        y += 25f
        
        paint.color = android.graphics.Color.LTGRAY
        paint.strokeWidth = 1f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 25f
        
        for (msg in messages) {
            if (y > pageHeight - 90) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
                
                canvas.drawText("DepthLens Analysis - Session: $sessionTitle (Page $pageNumber)", 40f, y, metaPaint)
                y += 14f
                canvas.drawLine(40f, y, 555f, y, paint)
                y += 22f
            }
            
            val isUser = msg.role == "user"
            val rolePaint = android.graphics.Paint().apply {
                color = if (isUser) android.graphics.Color.rgb(0, 150, 136) else android.graphics.Color.rgb(103, 58, 183)
                textSize = 10f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val roleLabel = if (isUser) "USER" else "DEPTHLENS AI"
            canvas.drawText(roleLabel, 40f, y, rolePaint)
            y += 14f
            
            val textLines = wrapText(msg.text, textPaint, 515)
            for (line in textLines) {
                if (y > pageHeight - 55) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                    
                    canvas.drawText("DepthLens Analysis - Session: $sessionTitle (Page $pageNumber)", 40f, y, metaPaint)
                    y += 14f
                    canvas.drawLine(40f, y, 555f, y, paint)
                    y += 22f
                }
                
                canvas.drawText(line, 40f, y, textPaint)
                y += 13f
            }
            
            y += 15f
        }
        
        pdfDocument.finishPage(page)
        
        val fileName = "DepthLens_Chat_${java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.getDefault()).format(java.util.Date())}.pdf"
        var file: java.io.File
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            file = java.io.File(downloadsDir, fileName)
            java.io.FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
        } catch (e: Exception) {
            val fallbackDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            file = java.io.File(fallbackDir ?: context.cacheDir, fileName)
            java.io.FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
        }
        
        pdfDocument.close()
        onComplete(file.absolutePath)
    } catch (e: Exception) {
        e.printStackTrace()
        onError(e.message ?: "Unknown PDF Export error")
    }
}

fun wrapText(text: String, paint: android.graphics.Paint, maxWidth: Int): List<String> {
    val lines = mutableListOf<String>()
    val paragraphs = text.split("\n")
    for (paragraph in paragraphs) {
        val words = paragraph.split(" ")
        var currentLine = StringBuilder()
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else currentLine.toString() + " " + word
            val width = paint.measureText(testLine)
            if (width > maxWidth) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            } else {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
    }
    return lines
}

fun highlightAnnotatedString(
    annotated: AnnotatedString,
    query: String,
    isActiveIndexFunc: (Int) -> Boolean = { false }
): AnnotatedString {
    if (query.isEmpty()) return annotated
    val builder = AnnotatedString.Builder(annotated)
    val text = annotated.text
    var index = text.indexOf(query, ignoreCase = true)
    var matchCounter = 0
    while (index != -1) {
        val isActive = isActiveIndexFunc(matchCounter)
        val style = if (isActive) {
            androidx.compose.ui.text.SpanStyle(
                background = androidx.compose.ui.graphics.Color(0xFFFF9800),
                color = androidx.compose.ui.graphics.Color.Black,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        } else {
            androidx.compose.ui.text.SpanStyle(
                background = androidx.compose.ui.graphics.Color(0xFFFFF176),
                color = androidx.compose.ui.graphics.Color.Black
            )
        }
        builder.addStyle(style, index, index + query.length)
        matchCounter++
        index = text.indexOf(query, index + query.length, ignoreCase = true)
    }
    return builder.toAnnotatedString()
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    sessions: List<SessionEntity>,
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    selectedDepth: String = "Standard Analysis",
    onDepthSelected: (String) -> Unit = {},
    isDeepThoughtEnabled: Boolean = false,
    onDeepThoughtToggle: (Boolean) -> Unit = {},
    onSessionSelected: (String) -> Unit,
    onSubmitQuery: (String) -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onAddAttachment: (String) -> Unit = {},
    onRemoveAttachment: () -> Unit = {},
    onRemoveAttachmentUri: (String) -> Unit = {},
    attachedImageUri: String? = null,
    archivedInsights: List<com.example.data.model.ArchivedInsightEntity> = emptyList(),
    onDeleteArchivedInsight: (String) -> Unit = {},
    activeMessages: List<com.example.data.model.MessageEntity> = emptyList(),
    isLoading: Boolean = false,
    onStopGeneration: () -> Unit = {},
    onRetryLastAnalysis: (String) -> Unit = {},
    onRegenerateLastAnalysis: (String) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onCreateNewSession: () -> Unit = {},
    onDeleteMessage: (String) -> Unit = {},
    isPrivacyModeEnabled: Boolean = false,
    onGetAttachmentsFlow: (String, String) -> Flow<List<com.example.data.model.AttachmentEntity>> = { _, _ -> kotlinx.coroutines.flow.flowOf(emptyList()) },
    speechManager: com.example.ui.viewmodel.SpeechManager? = null,
    onDigDeeper: (String, String) -> Unit = { _, _ -> },
    onNavigateToVoiceMode: () -> Unit = {},
    selectedMessageId: String? = null,
    selectedText: String? = null,
    replyMessageId: String? = null,
    replySelectedText: String? = null,
    onEnterSelectionMode: (String, String) -> Unit = { _, _ -> },
    onClearSelectionMode: () -> Unit = {},
    onSetReplyState: (String, String) -> Unit = { _, _ -> },
    onClearReplyState: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("depthlens_prefs", android.content.Context.MODE_PRIVATE) }
    var hasCompletedOnboarding by remember { mutableStateOf(prefs.getBoolean("has_completed_permission_onboarding", false)) }
    var showPermissionOnboardingDialog by remember { mutableStateOf(false) }
    var showSystemSettingsPrompt by remember { mutableStateOf(false) }
    var showUploadSecurityNotice by remember { mutableStateOf(false) }
    var pendingPermissionAction by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showUploadSecurityNotice) {
        if (showUploadSecurityNotice) {
            kotlinx.coroutines.delay(2500)
            showUploadSecurityNotice = false
        }
    }

    var rawText by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var currentMatchIndex by rememberSaveable { mutableStateOf(0) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var showModePopup by remember { mutableStateOf(false) }
    var activeSubmenu by remember { mutableStateOf<String?>(null) }

    val searchMatches = remember(activeMessages, searchQuery) {
        val list = mutableListOf<SearchMatch>()
        if (searchQuery.isNotEmpty()) {
            activeMessages.forEach { msg ->
                val text = msg.text
                var index = text.indexOf(searchQuery, ignoreCase = true)
                var occurrence = 0
                while (index != -1) {
                    list.add(SearchMatch(msg.id, occurrence))
                    occurrence++
                    index = text.indexOf(searchQuery, index + searchQuery.length, ignoreCase = true)
                }
            }
        }
        list
    }

    val safeMatchIndex = if (searchMatches.isEmpty()) 0 else currentMatchIndex.coerceIn(0, searchMatches.lastIndex)
    var isListening by remember { mutableStateOf(false) }

    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spoken = matches[0]
                    if (!spoken.isNullOrBlank()) {
                        rawText = if (rawText.text.isEmpty()) TextFieldValue(spoken) else TextFieldValue("${rawText.text} $spoken")
                        try {
                            speechManager?.trackUserSpeech(spoken)
                        } catch (e: Exception) {
                            android.util.Log.e("HomeScreen", "Error tracking user speech", e)
                        }
                    }
                }
                isListening = false
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            com.example.ui.viewmodel.SharedSpeechRecognizerManager.setListener(null, "HOME_SCREEN_MIC")
            com.example.ui.viewmodel.SharedSpeechRecognizerManager.releaseOwnership("HOME_SCREEN_MIC")
        }
    }

    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isListening = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            }
            com.example.ui.viewmodel.SharedSpeechRecognizerManager.startListening(context, intent, recognitionListener, "HOME_SCREEN_MIC")
        } else {
            showSystemSettingsPrompt = true
        }
    }

    var showAttachBottomSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var messageToExport by remember { mutableStateOf<com.example.data.model.MessageEntity?>(null) }

    // Mode selector popup state
    var showModeMenu by remember { mutableStateOf(false) }
    var openSubMenu by remember { mutableStateOf<String?>(null) } // "standard" or "depth"

    val attachPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showAttachBottomSheet = true
        } else {
            showSystemSettingsPrompt = true
        }
    }
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    // Reshuffle the empty-state suggestion chips every time the chat becomes empty
    // (new chat / cleared), so the three prompts aren't always the same.
    var chatSuggestionSeed by remember { mutableStateOf(0) }
    val isChatEmpty = activeMessages.isEmpty()
    LaunchedEffect(isChatEmpty) {
        if (isChatEmpty) chatSuggestionSeed++
    }

    val greeting = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "GOOD MORNING"
            in 12..16 -> "GOOD AFTERNOON"
            in 17..20 -> "GOOD EVENING"
            else -> "GOOD NIGHT"
        }
    }

    var animateEntry by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateEntry = true
    }
    val slideOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (animateEntry) 0.dp else 16.dp,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800, easing = androidx.compose.animation.core.EaseOutCubic),
        label = "slide"
    )
    val opacity by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animateEntry) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800),
        label = "opacity"
    )

    var editingMessageId by remember { mutableStateOf<String?>(null) }

    var replyQuoteText by remember { mutableStateOf<String?>(null) }
    var showReplyDialog by remember { mutableStateOf(false) }
    var replyDialogMessageText by remember { mutableStateOf("") }

    // ── ChatGPT/Claude-style "Reply" on text selection ───────────────────────────
    // The AI body is a read-only text field; when the user selects part of it we capture
    // the exact substring + its on-screen anchor and show a floating Reply/Copy bar.
    val clipboardForReply = androidx.compose.ui.platform.LocalClipboardManager.current
    var replySelText by remember { mutableStateOf<String?>(null) }
    var replySelAnchor by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Background is drawn once by the DashboardScreen root; drawing it again here
            // created a second, independently-animated layer whose seam showed as a faint
            // "box/band" behind the nav & bars. Stay transparent over the single root bg.
            .statusBarsPadding()
            .imePadding()
    ) {
        val scrollState = rememberScrollState()
        // Hoisted here so the scroll arrow button can hide when keyboard is open
        var inputFocused by remember { mutableStateOf(false) }

        // Stream / load states do not force scroll-to-top to preserve reading flow, especially for Dig Deeper.

        // Track whether the user is "stuck" to the bottom of the chat. Only when the
        // user is already at (or very near) the bottom do we auto-scroll as new content
        // streams in. If the user has scrolled up to read an earlier reply, we leave
        // their scroll position alone instead of yanking them back down.
        var stickToBottom by remember { mutableStateOf(true) }
        LaunchedEffect(scrollState.value, scrollState.maxValue) {
            val distanceFromBottom = scrollState.maxValue - scrollState.value
            stickToBottom = distanceFromBottom <= 120
        }

        // When a brand new user message is sent, snap to bottom and re-engage stick mode.
        val lastMessageId = remember(activeMessages) { activeMessages.lastOrNull()?.id }
        val lastMessageRole = remember(activeMessages) { activeMessages.lastOrNull()?.role }
        LaunchedEffect(lastMessageId) {
            if (lastMessageRole == "user") {
                stickToBottom = true
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }

        // While content streams in (new tokens / cards expanding), only follow the bottom
        // if the user hasn't scrolled away to read something.
        LaunchedEffect(scrollState.maxValue) {
            if (activeMessages.isNotEmpty() && stickToBottom) {
                scrollState.scrollTo(scrollState.maxValue)
            }
        }

        val showScrollButtonState by remember {
            derivedStateOf {
                val max = scrollState.maxValue
                val curr = scrollState.value
                val threshold = 150
                when {
                    max <= threshold -> "none"
                    // Near the bottom (latest reply visible) — nothing to jump to, hide.
                    curr >= max - threshold -> "none"
                    else -> "bottom"
                }
            }
        }

        // Only reveal the floating button while the user is actively scrolling
        // (and briefly after), and never while the keyboard is visible.
        // Note: we check actual IME visibility (not text-field focus state) because
        // a BasicTextField can remain "focused" even after the soft keyboard is
        // dismissed (e.g. back button / swipe-down), which previously left the
        // button permanently hidden until the activity was recreated.
        val imeVisible = WindowInsets.isImeVisible
        var isScrolling by remember { mutableStateOf(false) }
        LaunchedEffect(scrollState.isScrollInProgress) {
            if (scrollState.isScrollInProgress) {
                isScrolling = true
            } else {
                kotlinx.coroutines.delay(200)
                isScrolling = false
            }
        }
        val showFloatingScrollButton = showScrollButtonState != "none" && isScrolling && !imeVisible



        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Glass pinned top bar — clean floating glass over the themed background
            // (no heavy drop-shadow band; that dark halo read as an "old" strip).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(top = 4.dp)
                    .height(58.dp)
                    .premiumGlassBg(
                        shape = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp),
                        borderWidth = 1.dp
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Left slot: Menu/Hamburger Button
                val menuInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val menuPressed by menuInteraction.collectIsPressedAsState()
                val menuScale by animateFloatAsState(
                    targetValue = if (menuPressed) 0.88f else 1f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
                    label = "menu_scale"
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(38.dp)
                        .scale(menuScale)
                        .background(
                            color = Color.White.copy(alpha = 0.07f),
                            shape = RoundedCornerShape(13.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0x33FFFFFF),
                            shape = RoundedCornerShape(13.dp)
                        )
                        .clickable(interactionSource = menuInteraction, indication = null) { onOpenDrawer() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = TextPrimaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Center slot: Title and Caption
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "DepthLens",
                        color = TextPrimaryColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InstrumentSansFontFamily
                    )
                    Text(
                        text = "SEE BEYOND SURFACE",
                        color = PremiumCyan,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontFamily = InstrumentSansFontFamily
                    )
                }

                // Right slot: More options/⋮ Button
                androidx.compose.animation.AnimatedVisibility(
                    visible = activeMessages.isNotEmpty(),
                    enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(durationMillis = 250)),
                    exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(durationMillis = 200)),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    val editInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val editPressed by editInteraction.collectIsPressedAsState()
                    val editScale by animateFloatAsState(
                        targetValue = if (editPressed) 0.88f else 1f,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
                        label = "edit_scale"
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .scale(editScale)
                            .background(
                                color = Color.White.copy(alpha = 0.07f),
                                shape = RoundedCornerShape(13.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0x33FFFFFF),
                                shape = RoundedCornerShape(13.dp)
                            )
                            .clickable(interactionSource = editInteraction, indication = null) { isMenuExpanded = !isMenuExpanded }
                            .testTag("three_dot_menu_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = TextPrimaryColor,
                            modifier = Modifier.size(20.dp)
                        )

                        if (isMenuExpanded) {
                            val density = androidx.compose.ui.platform.LocalDensity.current
                            androidx.compose.ui.window.Popup(
                                alignment = Alignment.TopEnd,
                                offset = androidx.compose.ui.unit.IntOffset(0, with(density) { 44.dp.roundToPx() }),
                                onDismissRequest = { isMenuExpanded = false },
                                properties = androidx.compose.ui.window.PopupProperties(
                                    focusable = true,
                                    dismissOnBackPress = true,
                                    dismissOnClickOutside = true
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(188.dp)
                                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(18.dp))
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            color = SurfaceCardColor.copy(alpha = 0.98f),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = GlassBorder,
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .padding(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                isMenuExpanded = false
                                                isSearchActive = false
                                                searchQuery = ""
                                                onCreateNewSession()
                                                rawText = TextFieldValue("")
                                                focusRequester.requestFocus()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                            .testTag("menu_new_chat"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "＋  New chat",
                                            color = TextPrimaryColor,
                                            fontSize = 13.sp,
                                            fontFamily = InstrumentSansFontFamily
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                isMenuExpanded = false
                                                isSearchActive = true
                                                searchQuery = ""
                                                currentMatchIndex = 0
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                            .testTag("menu_search_chat"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "⌕  Search this chat",
                                            color = TextPrimaryColor,
                                            fontSize = 13.sp,
                                            fontFamily = InstrumentSansFontFamily
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                isMenuExpanded = false
                                                val sessionTitle = activeMessages.firstOrNull { it.role == "user" }?.text?.take(30) ?: "DepthLens Chat"
                                                exportChatToPdf(
                                                    context = context,
                                                    sessionTitle = sessionTitle,
                                                    messages = activeMessages,
                                                    onComplete = { path ->
                                                        android.widget.Toast.makeText(context, "Chat exported to: $path", android.widget.Toast.LENGTH_LONG).show()
                                                    },
                                                    onError = { err ->
                                                        android.widget.Toast.makeText(context, "Export failed: $err", android.widget.Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                            .testTag("menu_export_chat"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "⤓  Export chat (PDF)",
                                            color = TextPrimaryColor,
                                            fontSize = 13.sp,
                                            fontFamily = InstrumentSansFontFamily
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.Top
            ) {

            if (isSearchActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .premiumGlassBg(cornerRadius = 12.dp, borderAlpha = 0.4f)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ElectricViolet,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            currentMatchIndex = 0
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_input_field"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = InstrumentSansFontFamily,
                            fontSize = 13.sp,
                            color = TextPrimaryColor
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search inside this chat...",
                                    fontFamily = InstrumentSansFontFamily,
                                    fontSize = 13.sp,
                                    color = TextMutedColor
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (searchMatches.isNotEmpty()) {
                        Text(
                            text = "${safeMatchIndex + 1}/${searchMatches.size}",
                            fontFamily = DMMonoFontFamily,
                            fontSize = 11.sp,
                            color = ElectricViolet,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                if (searchMatches.isNotEmpty()) {
                                    currentMatchIndex = if (safeMatchIndex - 1 < 0) searchMatches.lastIndex else safeMatchIndex - 1
                                    val match = searchMatches[currentMatchIndex]
                                    val messageIndex = activeMessages.indexOfFirst { it.id == match.messageId }
                                    if (messageIndex != -1) {
                                        coroutineScope.launch {
                                            val targetScrollValue = (scrollState.maxValue * (messageIndex.toFloat() / activeMessages.size.coerceAtLeast(1).toFloat())).toInt()
                                            scrollState.animateScrollTo(targetScrollValue)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(24.dp).testTag("search_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Previous Match",
                                tint = ElectricViolet,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (searchMatches.isNotEmpty()) {
                                    currentMatchIndex = (safeMatchIndex + 1) % searchMatches.size
                                    val match = searchMatches[currentMatchIndex]
                                    val messageIndex = activeMessages.indexOfFirst { it.id == match.messageId }
                                    if (messageIndex != -1) {
                                        coroutineScope.launch {
                                            val targetScrollValue = (scrollState.maxValue * (messageIndex.toFloat() / activeMessages.size.coerceAtLeast(1).toFloat())).toInt()
                                            scrollState.animateScrollTo(targetScrollValue)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(24.dp).testTag("search_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Next Match",
                                tint = ElectricViolet,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        },
                        modifier = Modifier.size(24.dp).testTag("search_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Search",
                            tint = TextMutedColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Tagline section (only shown if Chat Feed is empty) with smooth auto-collapse
            androidx.compose.animation.AnimatedVisibility(
                visible = activeMessages.isEmpty(),
                enter = fadeIn(animationSpec = tween(500)) + expandVertically(animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
            ) {
                val infiniteBreatheTransition = rememberInfiniteTransition(label = "orb_breathe")
                val breatheScale by infiniteBreatheTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1750, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "orb_scale"
                )

                val timeGreeting = remember(currentHour) {
                    val base = when (currentHour) {
                        in 5..11 -> "Good morning"
                        in 12..16 -> "Good afternoon"
                        in 17..20 -> "Good evening"
                        else -> "Good night"
                    }
                    "$base, Abhay 👋"
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp)
                        .offset(y = slideOffset)
                        .alpha(opacity),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 1. Brand logo — using the single shared component
                    DepthLensLogo(
                        size = 96.dp,
                        showGlow = true
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 2. Heading: How can I help?
                    Text(
                        text = "How can I help?",
                        fontFamily = InstrumentSansFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimaryColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 3. Muted sub-line
                    Text(
                        text = "See beneath the surface. DepthLens reveals hidden patterns, root causes and probable futures.",
                        fontFamily = InstrumentSansFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextMutedColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 4. Rotating suggestion chips — a fresh trio each time the empty
                    // state appears, drawn from DepthLens' core use-cases.
                    val suggestionPool = remember {
                        listOf(
                            "Analyze my startup idea",
                            "Why do I keep avoiding this?",
                            "Should I switch careers?",
                            "Break down this decision",
                            "What's my blind spot here?",
                            "Read between the lines",
                            "Map the root cause",
                            "Forecast how this plays out",
                            "Is this fear or logic?",
                            "Spot the pattern in my choices",
                            "Stress-test my plan",
                            "What am I not seeing?"
                        )
                    }
                    val rotatingChips = remember(chatSuggestionSeed) {
                        suggestionPool.shuffled().take(3)
                    }
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3
                    ) {
                        rotatingChips.forEach { chipText ->
                            val chipInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            val chipPressed by chipInteraction.collectIsPressedAsState()
                            val chipScale by animateFloatAsState(
                                targetValue = if (chipPressed) 0.93f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                label = "chip_scale"
                            )

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .scale(chipScale)
                                    .background(
                                        color = Color.White.copy(alpha = 0.07f),
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .clickable(
                                        interactionSource = chipInteraction,
                                        indication = null
                                    ) {
                                        rawText = TextFieldValue("")
                                        onSubmitQuery(chipText)
                                    }
                                    .padding(horizontal = 13.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = chipText,
                                    fontSize = 11.sp,
                                    fontFamily = InstrumentSansFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimaryColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Mode selector state (replaces old horizontal pill row) ──────
            // showModeMenu: main popup open/closed
            // openSubMenu: which sub-popup is open ("standard" / "depth" / null)

            // ── Inline Analysis Results ──────────────────────────────────────
            if (activeMessages.isNotEmpty() || isLoading) {
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ANALYSIS",
                        fontSize = 8.sp,
                        letterSpacing = 1.2.sp,
                        fontFamily = DMMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = TextMutedColor
                    )
                    
                    if (isPrivacyModeEnabled) {
                         Row(
                             horizontalArrangement = Arrangement.spacedBy(4.dp),
                             verticalAlignment = Alignment.CenterVertically,
                             modifier = Modifier
                                 .background(Color(0xFF2C1010), RoundedCornerShape(4.dp))
                                 .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                 .padding(horizontal = 6.dp, vertical = 2.dp)
                         ) {
                             Icon(
                                 imageVector = Icons.Default.Lock,
                                 contentDescription = "Privacy Mode Active",
                                 tint = Color(0xFFFF5252),
                                 modifier = Modifier.size(10.dp)
                             )
                             Text(
                                 text = "PRIVACY ACTIVE",
                                 fontSize = 8.sp,
                                 fontFamily = DMMonoFontFamily,
                                 fontWeight = FontWeight.Bold,
                                 color = Color(0xFFFF5252)
                             )
                         }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    activeMessages.forEach { message ->
                        key(message.id) {
                            if (message.role == "user") {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    val attachmentsState = remember(message.id, message.imageUri) {
                                        onGetAttachmentsFlow(message.id, message.imageUri ?: "")
                                    }.collectAsState(initial = emptyList())
                                    val attachments = attachmentsState.value

                                    if (attachments.isNotEmpty()) {
                                        // Modern preview: real thumbnails, horizontally scrollable,
                                        // tap opens the in-app viewer (image/video/audio/pdf/text) or
                                        // an "Open With" chooser for docs/unsupported.
                                        var viewerAttachment by remember { mutableStateOf<com.example.data.model.AttachmentEntity?>(null) }
                                        androidx.compose.foundation.layout.BoxWithConstraints(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .widthIn(max = maxWidth * 0.82f)
                                                    .horizontalScroll(rememberScrollState())
                                                    .padding(bottom = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                for (attachment in attachments) {
                                                    AttachmentThumb(attachment) { viewerAttachment = attachment }
                                                }
                                            }
                                        }
                                        viewerAttachment?.let { att ->
                                            InAppAttachmentViewer(att) { viewerAttachment = null }
                                        }
                                    }

                                    if (message.text.isNotBlank()) {
                                        androidx.compose.foundation.layout.BoxWithConstraints(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            val bubbleScale = remember { Animatable(0.9f) }
                                            val bubbleAlpha = remember { Animatable(0f) }
                                            LaunchedEffect(Unit) {
                                                launch {
                                                    bubbleScale.animateTo(
                                                        targetValue = 1f,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessLow
                                                        )
                                                    )
                                                }
                                                launch {
                                                    bubbleAlpha.animateTo(
                                                        targetValue = 1f,
                                                        animationSpec = tween(durationMillis = 300)
                                                    )
                                                }
                                            }

                                            val isMsgSelected = selectedMessageId == message.id
                                            Box(
                                                modifier = Modifier
                                                    .widthIn(max = maxWidth * 0.78f)
                                                    .graphicsLayer {
                                                        scaleX = bubbleScale.value
                                                        scaleY = bubbleScale.value
                                                        alpha = bubbleAlpha.value
                                                    }
                                                    .shadow(
                                                        elevation = 8.dp,
                                                        shape = RoundedCornerShape(
                                                            topStart = 20.dp,
                                                            topEnd = 20.dp,
                                                            bottomStart = 20.dp,
                                                            bottomEnd = 6.dp
                                                        ),
                                                        clip = false,
                                                        ambientColor = if (isMsgSelected) ElectricViolet else ElectricViolet.copy(alpha = 0.4f),
                                                        spotColor = if (isMsgSelected) ElectricViolet else ElectricViolet.copy(alpha = 0.4f)
                                                    )
                                                    .border(
                                                        width = if (isMsgSelected) 2.dp else 0.dp,
                                                        color = if (isMsgSelected) Color.White else Color.Transparent,
                                                        shape = RoundedCornerShape(
                                                            topStart = 20.dp,
                                                            topEnd = 20.dp,
                                                            bottomStart = 20.dp,
                                                            bottomEnd = 6.dp
                                                        )
                                                    )
                                                    .background(
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(ElectricViolet, GradientEnd)
                                                        ),
                                                        shape = RoundedCornerShape(
                                                            topStart = 20.dp,
                                                            topEnd = 20.dp,
                                                            bottomStart = 20.dp,
                                                            bottomEnd = 6.dp
                                                        )
                                                    )
                                                    // No parent long-press handler: the inner SelectionContainer
                                                    // owns long-press so Android's native selection handles +
                                                    // highlight render, instead of selecting the whole message.
                                                    .padding(horizontal = 15.dp, vertical = 12.dp)
                                            ) {
                                                Column {
                                                    if (message.replyToMessageId != null && message.selectedText != null) {
                                                        ReplyHeaderBlock(
                                                            replyToMessageId = message.replyToMessageId,
                                                            selectedText = message.selectedText,
                                                            allMessages = activeMessages,
                                                            isUserMessage = true,
                                                            onRepliedBoxClick = { targetId ->
                                                                val idx = activeMessages.indexOfFirst { it.id == targetId }
                                                                if (idx >= 0) {
                                                                    coroutineScope.launch {
                                                                        val targetVal = (scrollState.maxValue * (idx.toFloat() / activeMessages.size.coerceAtLeast(1).toFloat())).toInt()
                                                                        scrollState.animateScrollTo(targetVal)
                                                                    }
                                                                }
                                                            }
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                    }
                                                    val highlightedUserText = remember(message.text, searchQuery, isSearchActive, safeMatchIndex, searchMatches, selectedMessageId, selectedText) {
                                                        if (selectedMessageId == message.id && selectedText != null) {
                                                            val builder = AnnotatedString.Builder(message.text)
                                                            val index = message.text.indexOf(selectedText)
                                                            if (index >= 0) {
                                                                builder.addStyle(
                                                                    style = androidx.compose.ui.text.SpanStyle(
                                                                        background = Color.White.copy(alpha = 0.35f),
                                                                        color = Color.White
                                                                    ),
                                                                    start = index,
                                                                    end = index + selectedText.length
                                                                )
                                                            }
                                                            builder.toAnnotatedString()
                                                        } else if (isSearchActive && searchQuery.isNotEmpty()) {
                                                            val occurrenceIndices = searchMatches.filter { it.messageId == message.id }
                                                            highlightAnnotatedString(
                                                                annotated = AnnotatedString(message.text),
                                                                query = searchQuery,
                                                                isActiveIndexFunc = { localIndex ->
                                                                    val match = searchMatches.getOrNull(safeMatchIndex)
                                                                    match != null && match.messageId == message.id && occurrenceIndices.getOrNull(localIndex) == match
                                                                }
                                                            )
                                                        } else {
                                                            AnnotatedString(message.text)
                                                        }
                                                    }
                                                    CustomSelectionProvider(
                                                        onCopy = { text ->
                                                            clipboardForReply.setText(AnnotatedString(text))
                                                        },
                                                        onReply = { text ->
                                                            replyQuoteText = text.trim().take(500)
                                                            onSetReplyState(message.id, text)
                                                            focusRequester.requestFocus()
                                                        },
                                                        onQuote = { text ->
                                                            val quotePrefix = "> ${text}\n\n"
                                                            rawText = TextFieldValue(
                                                                text = quotePrefix + rawText.text,
                                                                selection = androidx.compose.ui.text.TextRange(quotePrefix.length + rawText.text.length)
                                                            )
                                                            focusRequester.requestFocus()
                                                        },
                                                        onShare = { text ->
                                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                                type = "text/plain"
                                                                putExtra(android.content.Intent.EXTRA_TEXT, text)
                                                            }
                                                            val chooser = android.content.Intent.createChooser(shareIntent, "Share text")
                                                            context.startActivity(chooser)
                                                        },
                                                        onSelectAll = {
                                                            // Handled natively by SelectionContainer
                                                        }
                                                    ) { selectionKey ->
                                                        androidx.compose.runtime.key(selectionKey) {
                                                            androidx.compose.foundation.text.selection.SelectionContainer {
                                                                Text(
                                                                    text = highlightedUserText,
                                                                    fontSize = 13.sp,
                                                                    lineHeight = 20.sp,
                                                                    color = Color.White,
                                                                    fontFamily = InstrumentSansFontFamily,
                                                                    modifier = Modifier
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Subtle micro action buttons row
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, end = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val uContext = LocalContext.current
                                        val uClipboard = LocalClipboardManager.current
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .clickable {
                                                    uClipboard.setText(AnnotatedString(message.text))
                                                    android.widget.Toast.makeText(uContext, "Copied question", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ContentCopy,
                                                contentDescription = "Copy message",
                                                tint = Color(0xFF9D98C9),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .clickable {
                                                    rawText = TextFieldValue(message.text)
                                                    editingMessageId = message.id
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit message",
                                                tint = Color(0xFF9D98C9),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                        } else {
                            val parsedResponse = remember(message.text) {
                                try { ResponseParser.parse(message.text) } catch (e: Exception) { null }
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (message.text.startsWith("Error:") || message.text.contains("Error invoking DepthLens")) {
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0A0A)),
                                        border = BorderStroke(1.dp, Color(0xFFF44336).copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Analysis failed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF44336), fontFamily = InstrumentSansFontFamily)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(message.text, fontSize = 10.sp, color = TextSecondaryColor, fontFamily = InstrumentSansFontFamily, lineHeight = 14.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            TextButton(onClick = { onRetryLastAnalysis(message.id) }) {
                                                Text("Retry", fontSize = 11.sp, color = ElectricViolet)
                                            }
                                        }
                                    }
                                } else {
                                    val plainCleanedResponse = remember(message.text) {
                                        try {
                                            val parsed = ResponseParser.parse(message.text)
                                            ResponseParser.getCopyableText(message.text)
                                        } catch (e: Exception) {
                                            cleanResponseText(message.text)
                                        }
                                    }
                                    val annotatedPlainDisplayText = remember(plainCleanedResponse, searchQuery, isSearchActive, safeMatchIndex, searchMatches, selectedMessageId, selectedText) {
                                        val base = parseMarkdownToAnnotatedString(plainCleanedResponse)
                                        if (selectedMessageId == message.id && selectedText != null) {
                                            val builder = AnnotatedString.Builder(base)
                                            val index = plainCleanedResponse.indexOf(selectedText)
                                            if (index >= 0) {
                                                builder.addStyle(
                                                    style = androidx.compose.ui.text.SpanStyle(
                                                        background = ElectricViolet.copy(alpha = 0.35f),
                                                        color = Color.White
                                                    ),
                                                    start = index,
                                                    end = index + selectedText.length
                                                )
                                            }
                                            builder.toAnnotatedString()
                                        } else if (isSearchActive && searchQuery.isNotEmpty()) {
                                            val occurrenceIndices = searchMatches.filter { it.messageId == message.id }
                                            highlightAnnotatedString(
                                                annotated = base,
                                                query = searchQuery,
                                                isActiveIndexFunc = { localIndex ->
                                                    val match = searchMatches.getOrNull(safeMatchIndex)
                                                    match != null && match.messageId == message.id && occurrenceIndices.getOrNull(localIndex) == match
                                                }
                                            )
                                        } else {
                                            base
                                        }
                                    }
                                    val currentlySpeakingId by (speechManager?.currentPlayingMessageId?.collectAsState() ?: remember { mutableStateOf(null) })
                                    val isThisSpeaking = currentlySpeakingId == message.id
                                    val associatedUserQuery = remember(message.id, activeMessages) {
                                        activeMessages
                                            .subList(0, activeMessages.indexOfFirst { it.id == message.id }.coerceAtLeast(0))
                                            .findLast { it.role == "user" }?.text ?: ""
                                    }
                                    val tagLabel = remember(message.text) {
                                        val labels = listOf("KEY INSIGHT", "ROOT CAUSE", "STRATEGIC ASSESSMENT")
                                        labels[message.id.hashCode().coerceAtLeast(0) % labels.size]
                                    }

                                    val aiBubbleScale = remember { Animatable(0.9f) }
                                    val aiBubbleAlpha = remember { Animatable(0f) }
                                    LaunchedEffect(Unit) {
                                        launch {
                                            aiBubbleScale.animateTo(
                                                targetValue = 1f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                        launch {
                                            aiBubbleAlpha.animateTo(
                                                targetValue = 1f,
                                                animationSpec = tween(durationMillis = 300)
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        val isAIsgSelected = selectedMessageId == message.id
                                        Box(
                                            modifier = Modifier
                                                .widthIn(max = 280.dp)
                                                .graphicsLayer {
                                                    scaleX = aiBubbleScale.value
                                                    scaleY = aiBubbleScale.value
                                                    alpha = aiBubbleAlpha.value
                                                }
                                                .border(
                                                    width = if (isAIsgSelected) 2.dp else 0.dp,
                                                    color = if (isAIsgSelected) ElectricViolet.copy(alpha = 0.8f) else Color.Transparent,
                                                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 20.dp)
                                                )
                                                .premiumGlassBg(
                                                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 20.dp),
                                                    borderWidth = 1.dp,
                                                    elevation = 0.dp
                                                )
                                                // No parent long-press handler: inner SelectionContainer owns
                                                // long-press so native selection handles + highlight render.
                                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                        ) {
                                            Column {
                                                if (message.replyToMessageId != null && message.selectedText != null) {
                                                    ReplyHeaderBlock(
                                                        replyToMessageId = message.replyToMessageId,
                                                        selectedText = message.selectedText,
                                                        allMessages = activeMessages,
                                                        isUserMessage = false,
                                                        onRepliedBoxClick = { targetId ->
                                                            val idx = activeMessages.indexOfFirst { it.id == targetId }
                                                            if (idx >= 0) {
                                                                coroutineScope.launch {
                                                                    val targetVal = (scrollState.maxValue * (idx.toFloat() / activeMessages.size.coerceAtLeast(1).toFloat())).toInt()
                                                                    scrollState.animateScrollTo(targetVal)
                                                                 }
                                                            }
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                }
                                                // TAG label
                                                Text(
                                                    text = tagLabel,
                                                    fontFamily = DMMonoFontFamily,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SectionLabelColor,
                                                    letterSpacing = 1.2.sp,
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                )

                                                // Body text — wrapped in our native CustomSelectionProvider
                                                // which supports Copy, Reply, Quote, Share, and Select All in a custom floating context menu.
                                                CustomSelectionProvider(
                                                    onCopy = { text ->
                                                        clipboardForReply.setText(AnnotatedString(text))
                                                    },
                                                    onReply = { text ->
                                                        replyQuoteText = text.trim().take(500)
                                                        onSetReplyState(message.id, text)
                                                        focusRequester.requestFocus()
                                                    },
                                                    onQuote = { text ->
                                                        val quotePrefix = "> ${text}\n\n"
                                                        rawText = TextFieldValue(
                                                            text = quotePrefix + rawText.text,
                                                            selection = androidx.compose.ui.text.TextRange(quotePrefix.length + rawText.text.length)
                                                        )
                                                        focusRequester.requestFocus()
                                                    },
                                                    onShare = { text ->
                                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                            type = "text/plain"
                                                            putExtra(android.content.Intent.EXTRA_TEXT, text)
                                                        }
                                                        val chooser = android.content.Intent.createChooser(shareIntent, "Share text")
                                                        context.startActivity(chooser)
                                                    },
                                                    onSelectAll = {
                                                        // Handled natively by SelectionContainer
                                                    }
                                                ) { selectionKey ->
                                                    androidx.compose.runtime.key(selectionKey) {
                                                        androidx.compose.foundation.text.selection.SelectionContainer {
                                                            Text(
                                                                text = annotatedPlainDisplayText,
                                                                fontSize = 13.sp,
                                                                color = TextPrimaryColor,
                                                                lineHeight = 20.sp,
                                                                fontFamily = InstrumentSansFontFamily,
                                                                modifier = Modifier
                                                            )
                                                        }
                                                    }
                                                }

                                                // Action icons row
                                                ResponseActionRow(
                                                    message = message,
                                                    displayText = plainCleanedResponse,
                                                    associatedUserQuery = associatedUserQuery,
                                                    speechManager = speechManager,
                                                    onDigDeeper = onDigDeeper,
                                                    onRetry = { onRegenerateLastAnalysis(message.id) },
                                                    onBranchNewChat = {
                                                        onCreateNewSession()
                                                        if (associatedUserQuery.isNotBlank()) onSubmitQuery(associatedUserQuery)
                                                    },
                                                    onSearchWeb = {
                                                         val q = associatedUserQuery.ifBlank { message.text }
                                                         onSubmitQuery("[web] $q")
                                                     }
                                                 )

                                                 Spacer(modifier = Modifier.height(10.dp))

                                                 // Follow-up chips row
                                                 androidx.compose.foundation.layout.FlowRow(
                                                     modifier = Modifier.fillMaxWidth(),
                                                     horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                     verticalArrangement = Arrangement.spacedBy(6.dp)
                                                 ) {
                                                     // Chip 1: Go Deeper
                                                     Box(
                                                         modifier = Modifier
                                                             .premiumGlassBg(cornerRadius = 999.dp, borderWidth = 1.dp)
                                                             .clickable { onSubmitQuery("Go Deeper") }
                                                             .padding(horizontal = 10.dp, vertical = 5.dp)
                                                     ) {
                                                         Text("Go Deeper", color = TextPrimaryColor, fontSize = 10.sp, fontFamily = InstrumentSansFontFamily, fontWeight = FontWeight.Medium)
                                                     }

                                                     // Chip 2: Challenge it
                                                     Box(
                                                         modifier = Modifier
                                                             .premiumGlassBg(cornerRadius = 999.dp, borderWidth = 1.dp)
                                                             .clickable { onSubmitQuery("Challenge it") }
                                                             .padding(horizontal = 10.dp, vertical = 5.dp)
                                                     ) {
                                                         Text("Challenge it", color = TextPrimaryColor, fontSize = 10.sp, fontFamily = InstrumentSansFontFamily, fontWeight = FontWeight.Medium)
                                                     }
                                                 }
                                             }
                                         }
                                     }
                                 }
                             }
                         }
                     }
                 }

                 Spacer(modifier = Modifier.height(16.dp))
                 
                 if (isLoading && activeMessages.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            ThreeDotThinkingIndicator()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ── ChatGPT-style unified input bar + floating popups wrapper ──────
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Input bar wrapper — transparent so only the app background shows behind the
            // floating glass pill (matches preview). No full-width opaque band / divider line.
            // Bottom padding leaves a clear gap between the typing bar and the bottom nav.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 6.dp,
                        bottom = if (imeVisible) 0.dp else 10.dp
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                // Security notice toast
                if (showUploadSecurityNotice) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(Color(0xFF0F252C), RoundedCornerShape(12.dp))
                            .border(1.2.dp, PremiumCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🛡️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Isolated local sandbox active. Secure transit guaranteed.",
                            fontSize = 14.sp,
                            color = PremiumCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InstrumentSansFontFamily
                        )
                    }
                }

                // Edit mode banner
                if (editingMessageId != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(Surface2, RoundedCornerShape(8.dp))
                            .border(1.dp, ElectricViolet.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(ElectricViolet, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Editing Question",
                                fontSize = 14.sp,
                                color = ElectricViolet,
                                fontWeight = FontWeight.Bold,
                                fontFamily = DMMonoFontFamily
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel edit",
                            tint = TextMutedColor,
                            modifier = Modifier.size(14.dp).clickable {
                                editingMessageId = null
                                rawText = TextFieldValue("")
                            }
                        )
                    }
                }

                // Attachment Preview before sending
                attachedImageUri?.let { uri ->
                    AttachmentPreviewItem(
                        uri = uri,
                        onRemove = onRemoveAttachment,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // ── Premium Glass Pill Input Bar (DepthLens Redesign) ───────────────────
                if (isSearchActive) {
                    // Glass Search Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .premiumGlassBg(
                                cornerRadius = 22.dp,
                                borderWidth = 1.dp
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = ElectricViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    currentMatchIndex = 0
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("search_input_field"),
                                textStyle = TextStyle(
                                    fontFamily = InstrumentSansFontFamily,
                                    fontSize = 14.sp,
                                    color = if (ThemeManager.isDarkTheme) Color(0xFFEFEDFF) else Color(0xFF161334)
                                ),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search inside this chat...",
                                                fontFamily = InstrumentSansFontFamily,
                                                fontSize = 14.sp,
                                                color = Color(0xFF9D98C9)
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            
                            if (searchMatches.isNotEmpty()) {
                                Text(
                                    text = "${safeMatchIndex + 1}/${searchMatches.size}",
                                    fontFamily = DMMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = ElectricViolet,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                
                                IconButton(
                                    onClick = {
                                        if (searchMatches.isNotEmpty()) {
                                            currentMatchIndex = if (safeMatchIndex - 1 < 0) searchMatches.lastIndex else safeMatchIndex - 1
                                            val match = searchMatches[currentMatchIndex]
                                            val messageIndex = activeMessages.indexOfFirst { it.id == match.messageId }
                                            if (messageIndex != -1) {
                                                coroutineScope.launch {
                                                    val targetScrollValue = (scrollState.maxValue * (messageIndex.toFloat() / activeMessages.size.coerceAtLeast(1).toFloat())).toInt()
                                                    scrollState.animateScrollTo(targetScrollValue)
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(28.dp).testTag("search_prev_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Previous Match",
                                        tint = ElectricViolet,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                
                                IconButton(
                                    onClick = {
                                        if (searchMatches.isNotEmpty()) {
                                            currentMatchIndex = (safeMatchIndex + 1) % searchMatches.size
                                            val match = searchMatches[currentMatchIndex]
                                            val messageIndex = activeMessages.indexOfFirst { it.id == match.messageId }
                                            if (messageIndex != -1) {
                                                coroutineScope.launch {
                                                    val targetScrollValue = (scrollState.maxValue * (messageIndex.toFloat() / activeMessages.size.coerceAtLeast(1).toFloat())).toInt()
                                                    scrollState.animateScrollTo(targetScrollValue)
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(28.dp).testTag("search_next_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Next Match",
                                        tint = ElectricViolet,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = {
                                    isSearchActive = false
                                    searchQuery = ""
                                },
                                modifier = Modifier.size(28.dp).testTag("search_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Search",
                                    tint = Color(0xFF9D98C9),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Reply quote preview bar (shown when replying to selected text)
                    replyQuoteText?.let { quote ->
                        val targetReplyId = selectedMessageId ?: replyMessageId
                        val repliedMsg = activeMessages.find { it.id == targetReplyId }
                        val senderLabel = if (repliedMsg?.role == "user") "You" else "DepthLens"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceCardColor.copy(alpha = 0.5f))
                                .border(0.8.dp, ElectricViolet.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .clickable {
                                    val targetId = targetReplyId
                                    if (targetId != null) {
                                        val idx = activeMessages.indexOfFirst { it.id == targetId }
                                        if (idx >= 0) {
                                            coroutineScope.launch {
                                                val targetVal = (scrollState.maxValue * (idx.toFloat() / activeMessages.size.coerceAtLeast(1).toFloat())).toInt()
                                                scrollState.animateScrollTo(targetVal)
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Small vertical colored reply bar
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(32.dp)
                                    .background(ElectricViolet, RoundedCornerShape(1.5.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Replying to $senderLabel",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricViolet,
                                    fontFamily = InstrumentSansFontFamily
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = quote,
                                    fontSize = 10.sp,
                                    color = TextSecondaryColor,
                                    fontFamily = InstrumentSansFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Surface2)
                                    .clickable { 
                                        replyQuoteText = null 
                                        onClearReplyState()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel reply",
                                    tint = TextMutedColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    // Glass Pill Input Bar (Normal mode) - TWO ROW layout (shorter, compact rounded pill)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .premiumGlassBg(
                                cornerRadius = 22.dp,
                                borderWidth = 1.dp
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // ROW 1: Large Multi-line text field - Shorter and more compact
                        BasicTextField(
                            value = rawText,
                            onValueChange = { rawText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 28.dp, max = 120.dp)
                                .focusRequester(focusRequester)
                                .onFocusChanged { inputFocused = it.isFocused },
                            cursorBrush = SolidColor(ElectricViolet),
                            textStyle = TextStyle(
                                fontFamily = InstrumentSansFontFamily,
                                fontSize = 14.sp,
                                color = TextPrimaryColor
                            ),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                    if (rawText.text.isEmpty()) {
                                        Text(
                                            text = "Ask DepthLens…",
                                            fontFamily = InstrumentSansFontFamily,
                                            fontSize = 14.sp,
                                            color = TextMutedColor
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Theme-aware circle-button styling so the round buttons stay
                        // visible on BOTH dark and white (Dawn) themes.
                        val isLightTheme = !ThemeManager.isDarkTheme
                        val circleButtonBg = if (isLightTheme) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.08f)
                        val circleButtonBorder = if (isLightTheme) TextMutedColor.copy(alpha = 0.22f) else Color.Transparent

                        // ROW 2: Slim action row under the text field
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // LEFT: "+" round button — sized to match the Multi-Layer pill height
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(circleButtonBg)
                                    .border(0.8.dp, circleButtonBorder, CircleShape)
                                    .clickable { showAttachBottomSheet = !showAttachBottomSheet },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Attach",
                                    tint = TextPrimaryColor,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // COMPACT "Multi-Layer ▾" glass pill Box wrapper
                            Box(modifier = Modifier.wrapContentSize()) {
                                Row(
                                    modifier = Modifier
                                        .premiumGlassBg(
                                            cornerRadius = 16.dp,
                                            borderWidth = 1.dp
                                        )
                                        .clickable { showModePopup = !showModePopup }
                                        .padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedMode,
                                        color = TextPrimaryColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = InstrumentSansFontFamily
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "▾",
                                        color = TextMutedColor,
                                        fontSize = 10.sp
                                    )
                                }

                                if (showModePopup) {
                                    val density = androidx.compose.ui.platform.LocalDensity.current
                                    val yOffsetPx = with(density) { -45.dp.roundToPx() }
                                    Popup(
                                        alignment = Alignment.BottomStart,
                                        offset = androidx.compose.ui.unit.IntOffset(0, yOffsetPx),
                                        onDismissRequest = {
                                            showModePopup = false
                                            activeSubmenu = null
                                        },
                                        properties = PopupProperties(
                                            focusable = true,
                                            dismissOnBackPress = true,
                                            dismissOnClickOutside = true
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .wrapContentSize(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            // 1. MAIN POPUP CARD
                                            GlassPopupCard(
                                                modifier = Modifier.width(200.dp)
                                            ) {
                                                GlassPopupItem(
                                                    title = "Multi-Layer",
                                                    isSelected = selectedMode == "Multi-Layer",
                                                    isExpanded = false,
                                                    onClick = {
                                                        onModeSelected("Multi-Layer")
                                                        showModePopup = false
                                                        activeSubmenu = null
                                                    }
                                                )
                                                GlassPopupItem(
                                                    title = "Quick Insight",
                                                    subtitle = "Surface Read · Root Cause",
                                                    isSelected = selectedMode == "Quick Insight",
                                                    isExpanded = false,
                                                    onClick = {
                                                        onModeSelected("Quick Insight")
                                                        showModePopup = false
                                                        activeSubmenu = null
                                                    }
                                                )
                                                GlassPopupItem(
                                                    title = "Standard Analysis",
                                                    isSelected = selectedMode in listOf("Pattern Map", "Psychology", "Systems", "Probability", "Business", "Relationships", "Spiritual"),
                                                    isExpanded = activeSubmenu == "Standard",
                                                    hasSubmenu = true,
                                                    onClick = {
                                                        activeSubmenu = if (activeSubmenu == "Standard") null else "Standard"
                                                    }
                                                )
                                                GlassPopupItem(
                                                    title = "Depth Analysis",
                                                    isSelected = selectedMode in listOf("Full Investigation", "Deep Thought", "Deep Scan", "Deep Synthesis"),
                                                    isExpanded = activeSubmenu == "Depth",
                                                    hasSubmenu = true,
                                                    onClick = {
                                                        activeSubmenu = if (activeSubmenu == "Depth") null else "Depth"
                                                    }
                                                )
                                            }

                                            // 2. SECONDARY POPUP CARD (SUBMENU)
                                            if (activeSubmenu != null) {
                                                val submenuTitle = if (activeSubmenu == "Standard") "STANDARD ANALYSIS" else "DEPTH ANALYSIS"
                                                val submenuOptions = if (activeSubmenu == "Standard") {
                                                    listOf("Pattern Map", "Psychology", "Systems", "Probability", "Business", "Relationships", "Spiritual")
                                                } else {
                                                    listOf("Full Investigation", "Deep Thought", "Deep Scan", "Deep Synthesis")
                                                }

                                                GlassPopupCard(
                                                    modifier = Modifier.width(200.dp)
                                                ) {
                                                    // Title
                                                    Text(
                                                        text = submenuTitle,
                                                        fontFamily = InstrumentSansFontFamily,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF9D98C9),
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    
                                                    submenuOptions.forEach { option ->
                                                        GlassPopupItem(
                                                            title = option,
                                                            isSelected = selectedMode == option,
                                                            isExpanded = false,
                                                            onClick = {
                                                                onModeSelected(option)
                                                                showModePopup = false
                                                                activeSubmenu = null
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Dictate / Mic button (Visible only when text is empty)
                            AnimatedVisibility(
                                visible = rawText.text.isEmpty(),
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
                                Row {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isListening) Color(0x33FF3B30) else circleButtonBg
                                            )
                                            .border(0.8.dp, circleButtonBorder, CircleShape)
                                            .clickable {
                                                val hasPerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                                if (!hasPerm) {
                                                    audioPermLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                                } else {
                                                    isListening = true
                                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                    }
                                                    com.example.ui.viewmodel.SharedSpeechRecognizerManager.startListening(context, intent, recognitionListener, "HOME_SCREEN_MIC")
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                            contentDescription = "Dictate",
                                            tint = if (isListening) Color(0xFFFF3B30) else TextPrimaryColor,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                }
                            }

                            // RIGHT: PRIMARY round button (SEND/Waveform)
                            val isFieldEmpty = rawText.text.trim().isEmpty() && attachedImageUri.isNullOrEmpty()
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .shadow(
                                        elevation = if (isFieldEmpty) 0.dp else 6.dp,
                                        shape = CircleShape,
                                        clip = false
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        brush = if (isFieldEmpty) {
                                            SolidColor(circleButtonBg)
                                        } else {
                                            Brush.linearGradient(listOf(ElectricViolet, GradientEnd))
                                        },
                                        shape = CircleShape
                                    )
                                    .border(0.8.dp, if (isFieldEmpty) circleButtonBorder else Color.Transparent, CircleShape)
                                    .clickable {
                                        if (isFieldEmpty) {
                                            onNavigateToVoiceMode()
                                        } else {
                                            if (rawText.text.isNotBlank() || !attachedImageUri.isNullOrEmpty()) {
                                                val userText = rawText.text
                                                val textToSend = if (replyQuoteText != null) {
                                                    "Regarding this part of your previous response:\n\"\"\"\n" + replyQuoteText + "\n\"\"\"\n\nMy question: " + userText
                                                } else {
                                                    userText
                                                }
                                                rawText = TextFieldValue("")
                                                replyQuoteText = null
                                                if (editingMessageId != null) {
                                                    val editedId = editingMessageId!!
                                                    val currentEdited = activeMessages.find { m -> m.id == editedId }
                                                    if (currentEdited != null) {
                                                        val toDelete = activeMessages.filter { it.timestamp >= currentEdited.timestamp }
                                                        toDelete.forEach { msg -> onDeleteMessage(msg.id) }
                                                    }
                                                    editingMessageId = null
                                                }
                                                onSubmitQuery(textToSend)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFieldEmpty) {
                                    val waveformColor = if (isLightTheme) ElectricViolet else Color(0xFFEFEDFF)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(2.dp, 6.dp).background(waveformColor, RoundedCornerShape(1.dp)))
                                        Box(modifier = Modifier.size(2.dp, 11.dp).background(waveformColor, RoundedCornerShape(1.dp)))
                                        Box(modifier = Modifier.size(2.dp, 14.dp).background(waveformColor, RoundedCornerShape(1.dp)))
                                        Box(modifier = Modifier.size(2.dp, 9.dp).background(waveformColor, RoundedCornerShape(1.dp)))
                                        Box(modifier = Modifier.size(2.dp, 5.dp).background(waveformColor, RoundedCornerShape(1.dp)))
                                    }
                                } else {
                                    Text(
                                        text = "➤",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    // Floating scroll arrow — smart auto show/hide: appears only while the user is
    // actively scrolling away from the bottom, hides shortly after scrolling stops,
    // and stays hidden whenever the keyboard/input is focused.
    AnimatedVisibility(
        visible = showFloatingScrollButton,
        enter = scaleIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
        exit = scaleOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 130.dp)
    ) {
        val icon = Icons.Default.KeyboardArrowDown
        val desc = "Scroll to Bottom"
        
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .premiumGlassBg(
                    cornerRadius = 24.dp,
                    borderAlpha = 0.8f
                )
                .clickable {
                    coroutineScope.launch {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = desc,
                tint = PremiumCyan,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

    // ── Attachment file picker launcher ──────────────────────────────────────
    val attachPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                // Persist read access so the attachment can still be opened later
                // (even after the app is reopened). With the old GetContent picker the
                // content:// permission was temporary, so tapping the file later failed
                // to open it. OpenDocument + takePersistableUriPermission fixes that.
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    android.util.Log.w("AttachmentSystem", "Could not persist URI permission: ${e.message}")
                }
                android.util.Log.d("AttachmentSystem", "Successfully selected attachment URI: $uri")
                onAddAttachment(uri.toString())
            } catch (e: Exception) {
                android.util.Log.e("AttachmentSystem", "Failed to add attachment: ${e.message}", e)
            }
        } else {
            android.util.Log.w("AttachmentSystem", "File selection cancelled or returned null URI")
        }
    }

    var tempCameraImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val cameraCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraImageUri?.let { uri ->
                android.util.Log.d("AttachmentSystem", "Camera capture success: $uri")
                onAddAttachment(uri.toString())
            }
        } else {
            android.util.Log.w("AttachmentSystem", "Camera capture cancelled or failed")
        }
    }

    val cameraPermissionLauncherForCapture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val directory = java.io.File(context.cacheDir, "camera")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val file = java.io.File(directory, "depthlens_capture_${System.currentTimeMillis()}.jpg")
                val authority = "${context.packageName}.fileprovider"
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    authority,
                    file
                )
                tempCameraImageUri = uri
                cameraCaptureLauncher.launch(uri)
            } catch (e: Exception) {
                android.util.Log.e("AttachmentSystem", "Error preparing camera: ${e.message}", e)
                android.widget.Toast.makeText(
                    context,
                    "Could not initialize camera: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            android.widget.Toast.makeText(
                context,
                "Camera permission is required to take a photo.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun launchCamera() {
        try {
            val directory = java.io.File(context.cacheDir, "camera")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = java.io.File(directory, "depthlens_capture_${System.currentTimeMillis()}.jpg")
            val authority = "${context.packageName}.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                authority,
                file
            )
            tempCameraImageUri = uri
            cameraCaptureLauncher.launch(uri)
        } catch (e: Exception) {
            android.util.Log.e("AttachmentSystem", "Error preparing camera: ${e.message}", e)
            android.widget.Toast.makeText(
                context,
                "Could not initialize camera: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ── Compact floating attachment popup ─────────────────────────────────────
    // Renders as a small pill-menu that floats above the "+" button.
    // Scrim is subtle (25% black) so the chat is still visible behind it.
    // No bottom sheet — screen usage is ~25% instead of ~80%.

    // Subtle scrim — dismiss on tap outside
    androidx.compose.animation.AnimatedVisibility(
        visible = showAttachBottomSheet,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = androidx.compose.animation.core.tween(160)
        ),
        exit = androidx.compose.animation.fadeOut(
            animationSpec = androidx.compose.animation.core.tween(120)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { showAttachBottomSheet = false }
        )
    }

    // Floating popup anchored to bottom-start (above the "+" button)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
        androidx.compose.animation.AnimatedVisibility(
            visible = showAttachBottomSheet,
            enter = androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(180)
            ) + androidx.compose.animation.scaleIn(
                initialScale = 0.88f,
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f),
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                )
            ),
            exit = androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(120)
            ) + androidx.compose.animation.scaleOut(
                targetScale = 0.90f,
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f),
                animationSpec = androidx.compose.animation.core.tween(120)
            )
        ) {
            // Popup card
            Column(
                modifier = Modifier
                    .padding(start = 14.dp, bottom = 80.dp)
                    .width(220.dp)
                    .background(
                        color = if (ThemeManager.isDarkTheme) Color(0xEE16132E) else Color(0xEEFFFFFF),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val items = listOf(
                    Triple("Photo / Image", Icons.Filled.Image, "image/*"),
                    Triple("Video", Icons.Filled.Videocam, "video/*"),
                    Triple("Document / PDF", Icons.Filled.PictureAsPdf, "application/pdf"),
                    Triple("Camera", Icons.Filled.Videocam, "camera")
                )

                items.forEach { (label, icon, type) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                showAttachBottomSheet = false
                                if (type == "camera") {
                                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.CAMERA
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        launchCamera()
                                    } else {
                                        cameraPermissionLauncherForCapture.launch(android.Manifest.permission.CAMERA)
                                    }
                                } else {
                                    attachPickerLauncher.launch(arrayOf(type))
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (type == "camera") {
                            // Custom high-quality, minimalistic vector camera box
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .border(1.5.dp, ElectricViolet, RoundedCornerShape(4.dp))
                                    .padding(top = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(ElectricViolet, CircleShape)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = ElectricViolet,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontFamily = InstrumentSansFontFamily,
                            color = if (ThemeManager.isDarkTheme) Color(0xFFEFEDFF) else Color(0xFF161334),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // ── Scan Setup permission dialog (compact redesign) ───────────────────────
    if (showPermissionOnboardingDialog) {
        Dialog(onDismissRequest = { showPermissionOnboardingDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        // Top specular shimmer line
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    ElectricViolet.copy(alpha = 0.55f),
                                    PremiumCyan.copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            ),
                            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                            size = androidx.compose.ui.geometry.Size(size.width, 1.2f)
                        )
                    }
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        if (ThemeManager.isDarkTheme) Color(0xF70F0C22) else Color(0xF7FFFFFF)
                    )
                    .border(
                        width = 0.8.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                ElectricViolet.copy(alpha = 0.32f),
                                PremiumCyan.copy(alpha = 0.20f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // Shield icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ElectricViolet.copy(alpha = 0.15f))
                            .border(0.7.dp, ElectricViolet.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = ElectricViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Title
                    Text(
                        text = "Scan Setup",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryColor,
                        fontFamily = InstrumentSansFontFamily,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.2.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Description — compact
                    Text(
                        text = "DepthLens requires standard permissions to analyze documents & enable voice queries. Everything runs in an isolated sandbox.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = TextSecondaryColor,
                        fontFamily = InstrumentSansFontFamily,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Permission card 1 — Storage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ElectricViolet.copy(alpha = 0.09f))
                            .border(0.7.dp, ElectricViolet.copy(alpha = 0.24f), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElectricViolet.copy(alpha = 0.17f))
                                .border(0.6.dp, ElectricViolet.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderOpen,
                                contentDescription = null,
                                tint = ElectricViolet,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Secure Storage Scan",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryColor,
                                fontFamily = InstrumentSansFontFamily
                            )
                            Text(
                                "Attach images, PDFs & voice memos",
                                fontSize = 11.sp,
                                color = TextMutedColor,
                                fontFamily = InstrumentSansFontFamily
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    // Permission card 2 — Mic
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PremiumCyan.copy(alpha = 0.07f))
                            .border(0.7.dp, PremiumCyan.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PremiumCyan.copy(alpha = 0.14f))
                                .border(0.6.dp, PremiumCyan.copy(alpha = 0.32f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = PremiumCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Mic & Voice Analytics",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryColor,
                                fontFamily = InstrumentSansFontFamily
                            )
                            Text(
                                "Dictate statements directly to AI",
                                fontSize = 11.sp,
                                color = TextMutedColor,
                                fontFamily = InstrumentSansFontFamily
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Not Now — solid red
                        Button(
                            onClick = {
                                showPermissionOnboardingDialog = false
                                pendingPermissionAction = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB02020),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
                        ) {
                            Text(
                                "Not Now",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = InstrumentSansFontFamily
                            )
                        }

                        // Allow Access — solid white
                        Button(
                            onClick = {
                                prefs.edit().putBoolean("has_completed_permission_onboarding", true).apply()
                                hasCompletedOnboarding = true
                                showPermissionOnboardingDialog = false
                                if (pendingPermissionAction == "mic") {
                                    audioPermLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                } else {
                                    val mediaPerm = if (android.os.Build.VERSION.SDK_INT >= 33) {
                                        android.Manifest.permission.READ_MEDIA_IMAGES
                                    } else {
                                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                                    }
                                    attachPermLauncher.launch(mediaPerm)
                                }
                            },
                            modifier = Modifier
                                .weight(1.6f)
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1A0A3A)
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
                        ) {
                            Text(
                                "Allow Access",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A0A3A),
                                fontFamily = InstrumentSansFontFamily,
                                letterSpacing = 0.2.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sandbox note
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = TextMutedColor.copy(alpha = 0.45f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Files processed in a secure sandbox",
                            fontSize = 10.sp,
                            color = TextMutedColor.copy(alpha = 0.45f),
                            fontFamily = InstrumentSansFontFamily
                        )
                    }
                }
            }
        }
    }

    if (showSystemSettingsPrompt) {
        Dialog(onDismissRequest = { showSystemSettingsPrompt = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumGlassBg(
                        cornerRadius = 24.dp,
                        borderAlpha = 0.8f,
                        borderWidth = 1.dp,
                        showSpecularHighlight = true
                    )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔒 Permissions Required",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorColor,
                        fontFamily = InstrumentSansFontFamily,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Device storage or audio recording permissions have been restricted. To proceed with scanning or dictating context, please enable them in Android Settings.",
                        fontSize = 12.sp,
                        color = TextSecondaryColor,
                        fontFamily = InstrumentSansFontFamily,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { showSystemSettingsPrompt = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = TextSecondaryColor)
                        }
                        
                        Button(
                            onClick = {
                                showSystemSettingsPrompt = false
                                try {
                                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open Settings", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showExportDialog) {
            messageToExport?.let { message ->
                ExportOptionsDialog(
                    onDismiss = { showExportDialog = false },
                    messageText = message.text,
                    context = context
                )
            }
        }

        // Old floating reply bar replaced by CustomSelectionProvider context menu.

        if (showReplyDialog) {
            var replySelectionValue by remember(replyDialogMessageText) {
                mutableStateOf(TextFieldValue(text = replyDialogMessageText))
            }
            val hasSelection = !replySelectionValue.selection.collapsed

            Dialog(onDismissRequest = { showReplyDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(
                            cornerRadius = 24.dp,
                            borderAlpha = 0.8f,
                            borderWidth = 1.dp
                        )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Select text to reply to",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryColor,
                            fontFamily = InstrumentSansFontFamily
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Long-press and drag to select the specific part, then tap Reply.",
                            fontSize = 11.sp,
                            color = TextMutedColor,
                            fontFamily = InstrumentSansFontFamily
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        val dialogScrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Surface2.copy(alpha = 0.5f))
                                .border(0.8.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                .verticalScroll(dialogScrollState)
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = replySelectionValue,
                                onValueChange = { newValue ->
                                    if (newValue.text == replyDialogMessageText) {
                                        replySelectionValue = newValue
                                    } else {
                                        replySelectionValue = TextFieldValue(
                                            text = replyDialogMessageText,
                                            selection = newValue.selection
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(
                                    fontFamily = InstrumentSansFontFamily,
                                    fontSize = 13.sp,
                                    color = TextPrimaryColor,
                                    lineHeight = 20.sp
                                ),
                                cursorBrush = SolidColor(ElectricViolet)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Surface2)
                                    .border(0.8.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                    .clickable { showReplyDialog = false }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Cancel", color = TextMutedColor, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            }

                            val replyBtnBg = if (hasSelection)
                                Brush.linearGradient(listOf(ElectricViolet, GradientEnd))
                            else
                                Brush.linearGradient(listOf(Surface2, Surface2))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(replyBtnBg)
                                    .border(
                                        0.8.dp,
                                        if (hasSelection) Color.Transparent else BorderSubtle,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        val selectedText = if (hasSelection) {
                                            val start = minOf(replySelectionValue.selection.start, replySelectionValue.selection.end)
                                            val end = maxOf(replySelectionValue.selection.start, replySelectionValue.selection.end)
                                            replyDialogMessageText.substring(
                                                start.coerceAtLeast(0),
                                                end.coerceAtMost(replyDialogMessageText.length)
                                            )
                                        } else {
                                            replyDialogMessageText
                                        }
                                        replyQuoteText = selectedText.trim().take(500)
                                        showReplyDialog = false
                                        focusRequester.requestFocus()
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (hasSelection) "Reply with selected" else "Reply to all",
                                    color = if (hasSelection) Color.White else TextMutedColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InstrumentSansFontFamily
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Custom Floating Toolbar (Claude/ChatGPT style) ──────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.animation.AnimatedVisibility(
                visible = selectedMessageId != null && selectedText != null,
                enter = androidx.compose.animation.fadeIn(animationSpec = tween(180)) + androidx.compose.animation.slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(180)),
                exit = androidx.compose.animation.fadeOut(animationSpec = tween(150)) + androidx.compose.animation.slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(150)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 110.dp) // Float perfectly above chat input bar
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCardColor.copy(alpha = 0.95f))
                    .border(1.2.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
            val uClipboard = androidx.compose.ui.platform.LocalClipboardManager.current
            val context = androidx.compose.ui.platform.LocalContext.current
            val selectedMsgText = remember(selectedMessageId, activeMessages) {
                activeMessages.find { it.id == selectedMessageId }?.text ?: ""
            }

            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // REPLY
                TextButton(
                    onClick = {
                        if (selectedMessageId != null && selectedText != null) {
                            replyQuoteText = selectedText.trim().take(500)
                            onSetReplyState(selectedMessageId, selectedText)
                            onClearSelectionMode()
                            focusRequester.requestFocus()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ElectricViolet)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "Reply",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reply", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InstrumentSansFontFamily)
                }

                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.15f)))

                // COPY
                TextButton(
                    onClick = {
                        if (selectedText != null) {
                            uClipboard.setText(AnnotatedString(selectedText))
                            android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                            onClearSelectionMode()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextPrimaryColor)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy", fontSize = 12.sp, fontFamily = InstrumentSansFontFamily)
                }

                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.15f)))

                // SELECT ALL
                TextButton(
                    onClick = {
                        if (selectedMessageId != null) {
                            onEnterSelectionMode(selectedMessageId, selectedMsgText)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextPrimaryColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = "Select All",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select All", fontSize = 12.sp, fontFamily = InstrumentSansFontFamily)
                }
            }
        }
        }
    }
}

// ───────────────────────────────────────────
// EXPORTERS & EXPORT OPTION DIALOG FOR V4.1.5
// ───────────────────────────────────────────

private fun saveToDownloads(context: android.content.Context, fileName: String, mimeType: String, contentBytes: ByteArray): Boolean {
    return try {
        val resolver = context.contentResolver
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(contentBytes)
                }
                true
            } else {
                false
            }
        } else {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, fileName)
            file.writeBytes(contentBytes)
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun generatePdfReport(titleText: String, textContent: String): ByteArray {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 pt
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas
    val paint = android.graphics.Paint()
    
    val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
    
    fun drawPageDecorations(canvas: android.graphics.Canvas, pageNum: Int, paint: android.graphics.Paint, timestamp: String) {
        // Draw elegant high-contrast decorative top band in Electric Violet
        paint.color = android.graphics.Color.rgb(126, 101, 255) // #7E65FF Electric Violet
        canvas.drawRect(0f, 0f, 595f, 15f, paint)
        
        // Draw elegant accent strip in Premium Cyan
        paint.color = android.graphics.Color.rgb(0, 229, 255) // Premium Cyan
        canvas.drawRect(0f, 15f, 595f, 18f, paint)
        
        // Header Brand Text
        paint.textSize = 14f
        paint.isFakeBoldText = true
        paint.color = android.graphics.Color.rgb(126, 101, 255)
        canvas.drawText("DEPTHLENS", 40f, 42f, paint)
        
        paint.textSize = 7.5f
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.rgb(0, 229, 255)
        canvas.drawText("INTELLIGENCE ENGINE", 125f, 38f, paint)
        
        // Header thin divider
        paint.color = android.graphics.Color.rgb(220, 225, 235)
        paint.strokeWidth = 1f
        canvas.drawLine(40f, 53f, 555f, 53f, paint)
        
        // Footer thin divider
        canvas.drawLine(40f, 795f, 555f, 795f, paint)
        
        // Footer details
        paint.textSize = 8f
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.GRAY
        canvas.drawText("Generated by DepthLens Intelligence Engine  |  $timestamp", 40f, 812f, paint)
        
        // Page Number to the right
        canvas.drawText("Page $pageNum", 520f, 812f, paint)
    }

    var pageNum = 1
    // Draw decorations of the first page
    drawPageDecorations(canvas, pageNum, paint, timestamp)
    
    // Render text with pages pagination
    var currentY = 85f
    val margin = 45f
    val maxWidth = 505f
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 10.5f
    
    val sections = textContent.replace("\r", "").split("\n")
    for (section in sections) {
        if (section.trim().isEmpty()) {
            currentY += 10f
            continue
        }
        
        if (section.startsWith("=== DEPTHLENS") || section.startsWith("===") || section.startsWith("==================")) {
            // Skips raw separator lines to keep look premium
            continue
        }
        
        if (section.startsWith("###") || section.startsWith("##") || section.startsWith("#") || 
            section.equals("INTRODUCTION") || section.equals("EXECUTIVE SUMMARY") || section.equals("DEEP SYNTHESIS (INTEGRATED VISIONS)") || 
            section.startsWith("ROOT CAUSE REPORT") || section.startsWith("HUMAN DRIVERS") || section.startsWith("FUTURE SCENARIOS")) {
            paint.isFakeBoldText = true
            paint.textSize = 11.5f
            paint.color = android.graphics.Color.rgb(88, 28, 135) // Deep purple section header
            val cleanSec = section.replace("#", "").trim()
            if (currentY > 775f) {
                pdfDocument.finishPage(page)
                pageNum++
                page = pdfDocument.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNum).create())
                canvas = page.canvas
                drawPageDecorations(canvas, pageNum, paint, timestamp)
                currentY = 85f
            }
            canvas.drawText(cleanSec, margin, currentY, paint)
            currentY += 22f
            continue
        } else {
            paint.isFakeBoldText = false
            paint.textSize = 9.5f
            paint.color = android.graphics.Color.BLACK
        }
        
        val words = section.split(" ")
        val line = StringBuilder()
        for (word in words) {
            val spaceText = if (line.isNotEmpty()) " " + word else word
            if (paint.measureText(line.toString() + spaceText) < maxWidth) {
                line.append(spaceText)
            } else {
                if (currentY > 775f) {
                    pdfDocument.finishPage(page)
                    pageNum++
                    page = pdfDocument.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNum).create())
                    canvas = page.canvas
                    drawPageDecorations(canvas, pageNum, paint, timestamp)
                    currentY = 85f
                }
                
                // If it is a list item or field-label, color the starting tag beautifully
                val lineStr = line.toString()
                if (lineStr.trim().startsWith("-") || lineStr.trim().startsWith("*")) {
                    paint.color = android.graphics.Color.rgb(126, 101, 255)
                    canvas.drawText(lineStr.take(1), margin, currentY, paint)
                    paint.color = android.graphics.Color.BLACK
                    canvas.drawText(lineStr.drop(1), margin + 12f, currentY, paint)
                } else if (lineStr.contains(":")) {
                    val label = lineStr.substringBefore(":") + ":"
                    val value = lineStr.substringAfter(":")
                    paint.isFakeBoldText = true
                    paint.color = android.graphics.Color.rgb(126, 101, 255) // Electric Violet label
                    canvas.drawText(label, margin, currentY, paint)
                    
                    paint.isFakeBoldText = false
                    paint.color = android.graphics.Color.BLACK
                    val offset = paint.measureText(label) + 4f
                    canvas.drawText(value, margin + offset, currentY, paint)
                } else {
                    canvas.drawText(lineStr, margin, currentY, paint)
                }
                
                currentY += 16f
                line.setLength(0)
                line.append(word)
            }
        }
        if (line.isNotEmpty()) {
            if (currentY > 775f) {
                pdfDocument.finishPage(page)
                pageNum++
                page = pdfDocument.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNum).create())
                canvas = page.canvas
                drawPageDecorations(canvas, pageNum, paint, timestamp)
                currentY = 85f
            }
            
            val lineStr = line.toString()
            if (lineStr.contains(":")) {
                val label = lineStr.substringBefore(":") + ":"
                val value = lineStr.substringAfter(":")
                paint.isFakeBoldText = true
                paint.color = android.graphics.Color.rgb(126, 101, 255) // Electric Violet label
                canvas.drawText(label, margin, currentY, paint)
                
                paint.isFakeBoldText = false
                paint.color = android.graphics.Color.BLACK
                val offset = paint.measureText(label) + 4f
                canvas.drawText(value, margin + offset, currentY, paint)
            } else {
                canvas.drawText(lineStr, margin, currentY, paint)
            }
            currentY += 18f
        }
    }
    
    pdfDocument.finishPage(page)
    val outputStream = java.io.ByteArrayOutputStream()
    pdfDocument.writeTo(outputStream)
    pdfDocument.close()
    return outputStream.toByteArray()
}

@Composable
private fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    messageText: String,
    context: android.content.Context
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "EXPORT REPORT",
                fontSize = 14.sp,
                fontFamily = DMMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = PremiumCyan,
                letterSpacing = 1.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Choose an output format to save to your local Downloads folder:",
                    fontSize = 11.sp,
                    color = TextSecondaryColor,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val options = listOf(
                    Triple("Plain Text Document", "depthlens_report.txt", "text/plain"),
                    Triple("Structured JSON Document", "depthlens_report.json", "application/json"),
                    Triple("Portable Document Format (PDF)", "depthlens_report.pdf", "application/pdf")
                )
                
                options.forEach { (label, fileNm, mime) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface2, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .clickable {
                                val cleanText = ResponseParser.getCopyableText(messageText)
                                val finalBytes = when (mime) {
                                    "application/json" -> {
                                        try {
                                            val jsonObject = org.json.JSONObject().apply {
                                                put("platform", "DepthLens")
                                                put("version", "v4.1.5")
                                                put("timestamp", System.currentTimeMillis())
                                                put("analysis_report", cleanText)
                                            }
                                            jsonObject.toString(4).toByteArray()
                                        } catch (e: Exception) {
                                            cleanText.toByteArray()
                                        }
                                    }
                                    "application/pdf" -> {
                                        generatePdfReport("DepthLens Analysis", cleanText)
                                    }
                                    else -> cleanText.toByteArray()
                                }
                                val ok = saveToDownloads(context, fileNm, mime, finalBytes)
                                if (ok) {
                                    android.widget.Toast.makeText(context, "Exported Successfully", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Export failed: unable to write document.", android.widget.Toast.LENGTH_LONG).show()
                                }
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryColor,
                                fontFamily = InstrumentSansFontFamily
                            )
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download icons",
                                tint = ElectricViolet,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = PremiumCyan, fontWeight = FontWeight.Bold, fontFamily = DMMonoFontFamily, fontSize = 11.sp)
            }
        },
        containerColor = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.depthGlass(cornerRadius = 16.dp, borderWidth = 1.2.dp)
    )
}


// Helper to parse archived JSON — kept for any legacy calls
private data class ParsedArchivedDetails(val introduction: String = "", val depthLayers: List<com.example.data.model.DepthLayerInsight> = emptyList())

private fun parseArchivedJson(jsonContent: String): ParsedArchivedDetails {
    return try {
        val parsed = ResponseParser.parse(jsonContent)
        ParsedArchivedDetails(introduction = parsed.introduction, depthLayers = parsed.depthLayers)
    } catch (e: Exception) {
        ParsedArchivedDetails()
    }
}

// ── Mode selector helper composables ─────────────────────────────────────────

@Composable
private fun ModeMenuItem(
    label: String,
    subtitle: String?,
    isSelected: Boolean,
    hasArrow: Boolean,
    isSubOpen: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected || isSubOpen) ElectricViolet.copy(alpha = 0.10f) else Color.Transparent
            )
            .clickable(indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InstrumentSansFontFamily,
                color = TextPrimaryColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    fontFamily = InstrumentSansFontFamily,
                    color = TextMutedColor,
                    letterSpacing = 0.2.sp
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Text("✓", fontSize = 13.sp, color = ElectricViolet)
            }
            if (hasArrow) {
                Text("›", fontSize = 16.sp, color = TextMutedColor)
            }
        }
    }
}

@Composable
private fun SubModeMenuItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) ElectricViolet.copy(alpha = 0.10f) else Color.Transparent
            )
            .clickable(indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = InstrumentSansFontFamily,
            color = TextPrimaryColor
        )
        if (isSelected) {
            Text("✓", fontSize = 13.sp, color = ElectricViolet)
        }
    }
}

@Composable
fun GlassTypingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    @Composable
    fun rememberBlinkState(delayMillis: Int): State<Float> {
        return infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1200
                    0.3f at delayMillis with LinearEasing
                    1f at (delayMillis + 300) with LinearEasing
                    0.3f at (delayMillis + 600) with LinearEasing
                    0.3f at 1200 with LinearEasing
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "blink"
        )
    }

    @Composable
    fun rememberBounceState(delayMillis: Int): State<Float> {
        return infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -4f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1200
                    0f at delayMillis with LinearEasing
                    -4f at (delayMillis + 300) with LinearEasing
                    0f at (delayMillis + 600) with LinearEasing
                    0f at 1200 with LinearEasing
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "bounce"
        )
    }

    val alpha1 by rememberBlinkState(0)
    val alpha2 by rememberBlinkState(200)
    val alpha3 by rememberBlinkState(400)

    val bounce1 by rememberBounceState(0)
    val bounce2 by rememberBounceState(200)
    val bounce3 by rememberBounceState(400)

    Row(
        modifier = modifier
            .wrapContentSize()
            .background(
                color = Color.White.copy(alpha = 0.07f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            Pair(alpha1, bounce1),
            Pair(alpha2, bounce2),
            Pair(alpha3, bounce3)
        ).forEach { (alpha, bounce) ->
            Box(
                modifier = Modifier
                    .offset(y = bounce.dp)
                    .size(7.dp)
                    .background(
                        color = Color(0xFFA78BFA).copy(alpha = alpha),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun ResponseActionRow(
    message: MessageEntity,
    displayText: String,
    associatedUserQuery: String,
    speechManager: SpeechManager?,
    onDigDeeper: ((String, String) -> Unit)?,
    onRetry: (() -> Unit)? = null,
    onBranchNewChat: (() -> Unit)? = null,
    onSearchWeb: (() -> Unit)? = null,
    onReply: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showMore by remember { mutableStateOf(false) }

    // Voice state
    val currentlySpeakingId by (speechManager?.currentPlayingMessageId?.collectAsState() ?: remember { mutableStateOf(null) })
    val isSpeaking = currentlySpeakingId == message.id

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .drawBehind {
                drawLine(
                    color = CardBorderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 0.8.dp.toPx()
                )
            }
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // [ Copy ] action button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Surface2, RoundedCornerShape(100.dp))
                    .border(0.8.dp, BorderSubtle, RoundedCornerShape(100.dp))
                    .clickable {
                        clipboardManager.setText(AnnotatedString(displayText))
                        android.widget.Toast.makeText(context, "Response copied", android.widget.Toast.LENGTH_SHORT).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Copy",
                    tint = TextMutedColor,
                    modifier = Modifier.size(13.dp)
                )
            }

            // [ Reply ] action button — reply to selected text
            if (onReply != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(ElectricViolet.copy(alpha = 0.08f), RoundedCornerShape(100.dp))
                        .border(0.8.dp, ElectricViolet.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                        .clickable { onReply?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "Reply",
                        tint = ElectricViolet,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // [ Voice ] action button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isSpeaking) ElectricViolet.copy(alpha = 0.15f) else Surface2,
                        RoundedCornerShape(100.dp)
                    )
                    .border(
                        0.8.dp,
                        if (isSpeaking) ElectricViolet else BorderSubtle,
                        RoundedCornerShape(100.dp)
                    )
                    .clickable {
                        speechManager?.speak(message.id, displayText)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                    contentDescription = if (isSpeaking) "Stop" else "Voice",
                    tint = if (isSpeaking) ElectricViolet else TextMutedColor,
                    modifier = Modifier.size(13.dp)
                )
            }

            // [ Retry ] action button — regenerate this analysis
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Surface2, RoundedCornerShape(100.dp))
                    .border(0.8.dp, BorderSubtle, RoundedCornerShape(100.dp))
                    .clickable {
                        if (onRetry != null) {
                            onRetry()
                        } else {
                            android.widget.Toast.makeText(context, "Retry not configured", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Retry",
                    tint = TextMutedColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            // [ More ⋯ ] action button — Branch in a new chat / Search the Web
            Box {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Surface2, RoundedCornerShape(100.dp))
                        .border(0.8.dp, BorderSubtle, RoundedCornerShape(100.dp))
                        .clickable { showMore = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = "More",
                        tint = TextMutedColor,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Custom rounded popup (M3 DropdownMenu draws a square surface behind the
                // rounded background → square corners bled out; this fixes that).
                if (showMore) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    androidx.compose.ui.window.Popup(
                        alignment = Alignment.TopStart,
                        offset = androidx.compose.ui.unit.IntOffset(0, with(density) { 34.dp.roundToPx() }),
                        onDismissRequest = { showMore = false },
                        properties = androidx.compose.ui.window.PopupProperties(
                            focusable = true,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .width(216.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceCardColor.copy(alpha = 0.98f))
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                .padding(5.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showMore = false
                                        if (onBranchNewChat != null) onBranchNewChat()
                                        else android.widget.Toast.makeText(context, "Branch not configured", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 11.dp)
                            ) {
                                Text("⑂  Branch in a new chat", color = TextPrimaryColor, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showMore = false
                                        if (onSearchWeb != null) onSearchWeb()
                                        else android.widget.Toast.makeText(context, "Web search not configured", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 11.dp)
                            ) {
                                Text("⌕  Search the Web", color = TextPrimaryColor, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + fraction * (stop - start)

@Composable
fun EyeMark(state: VoiceOverlayState, modifier: Modifier = Modifier) {
    val merging = state == VoiceOverlayState.SPEAKING
    val mergeT by animateFloatAsState(
        targetValue = if (merging) 1f else 0f,
        animationSpec = tween(500, easing = EaseOutCubic),
        label = "mergeT"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val isTalking = state == VoiceOverlayState.LISTENING || state == VoiceOverlayState.SPEAKING
    val breathScale by if (isTalking) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.045f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathScale"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val glowAlpha by animateFloatAsState(
        targetValue = if (isTalking) 0.35f else 0.12f,
        animationSpec = tween(300),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = breathScale
            scaleY = breathScale
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8B5CF6).copy(alpha = glowAlpha), Color.Transparent),
                    center = center,
                    radius = size.minDimension * 0.75f
                ),
                radius = size.minDimension * 0.75f,
                center = center
            )

            val baseWidth = 79f
            val baseHeight = 59f
            val scale = minOf(size.width / baseWidth, size.height / baseHeight)
            val dx = (size.width - baseWidth * scale) / 2f - 14f * scale
            val dy = (size.height - baseHeight * scale) / 2f - 24f * scale

            withTransform({
                translate(dx, dy)
                scale(scale, scale, Offset(0f, 0f))
            }) {
                val eyePath = Path().apply {
                    moveTo(18f, 53.5f)
                    quadraticBezierTo(53.5f, 28f, 89f, 53.5f)
                    quadraticBezierTo(53.5f, 79f, 18f, 53.5f)
                    close()
                }
                drawPath(path = eyePath, color = Color.White)

                val specPath = Path().apply {
                    moveTo(18f, 53.5f)
                    quadraticBezierTo(53.5f, 28f, 89f, 53.5f)
                    quadraticBezierTo(53.5f, 44f, 18f, 53.5f)
                    close()
                }
                drawPath(
                    path = specPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                        startY = 28f,
                        endY = 53.5f
                    )
                )

                drawPath(
                    path = eyePath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF22D3EE), Color(0xFF8B5CF6)),
                        start = Offset(18f, 28f),
                        end = Offset(89f, 79f)
                    ),
                    style = Stroke(width = 2.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                drawCircle(
                    color = Color(0xFF22D3EE),
                    radius = 11.5f,
                    center = Offset(53.5f, 53.5f),
                    style = Stroke(width = 2.4f),
                    alpha = 0.9f
                )

                val ax = lerp(58.7f, 53.5f, mergeT)
                val ay = lerp(48.3f, 53.5f, mergeT)
                val bx = lerp(48.3f, 53.5f, mergeT)
                val by = lerp(58.7f, 53.5f, mergeT)

                val isMerged = mergeT > 0.85f
                val fadeSeparate = maxOf(0f, 1f - mergeT / 0.85f)
                val fadeMerged = maxOf(0f, (mergeT - 0.7f) / 0.3f)

                val ringR = lerp(3.2f, 4.8f, minOf(1f, mergeT))
                val coreR = lerp(1.8f, 3.0f, minOf(1f, mergeT))

                if (fadeSeparate > 0.02f) {
                    drawLine(
                        color = Color(0xFF8B5CF6),
                        start = Offset(ax, ay),
                        end = Offset(bx, by),
                        strokeWidth = 2.2f,
                        cap = StrokeCap.Round,
                        alpha = fadeSeparate * 0.85f
                    )
                }

                if (!isMerged) {
                    listOf(ax to ay, bx to by).forEach { (px, py) ->
                        drawCircle(
                            color = Color(0xFF22D3EE),
                            radius = ringR,
                            center = Offset(px, py),
                            style = Stroke(width = 1.8f),
                            alpha = 0.9f
                        )
                        drawCircle(
                            color = Color(0xFF0B1320),
                            radius = coreR,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 0.7f,
                            center = Offset(px - 0.7f, py - 0.7f),
                            alpha = 0.85f
                        )
                    }
                } else {
                    drawCircle(
                        color = Color(0xFF22D3EE),
                        radius = 4.8f,
                        center = Offset(53.5f, 53.5f),
                        style = Stroke(width = 1.9f),
                        alpha = 0.9f
                    )
                    drawCircle(
                        color = Color(0xFF0B1320),
                        radius = 3.0f,
                        center = Offset(53.5f, 53.5f),
                        alpha = minOf(1f, fadeMerged + 0.3f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 1.1f,
                        center = Offset(53.5f - 1.1f, 53.5f - 1.1f),
                        alpha = 0.85f
                    )
                }
            }
        }
    }
}

enum class VoiceOverlayState {
    LISTENING, THINKING, SPEAKING
}

@Composable
fun VoiceConversationOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSubmitQuery: (String, String?) -> Unit = { _, _ -> },
    onUserInterrupt: () -> Unit = {},
    isLoading: Boolean = false,
    speechManager: com.example.ui.viewmodel.SpeechManager? = null,
    activeMessages: List<com.example.data.model.MessageEntity> = emptyList(),
    voiceOutputEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val VoiceOverlayStateSaver = remember {
        androidx.compose.runtime.saveable.Saver<MutableState<VoiceOverlayState>, String>(
            save = { it.value.name },
            restore = { mutableStateOf(enumValueOf<VoiceOverlayState>(it)) }
        )
    }

    var selectedTab by rememberSaveable { mutableStateOf("Voice") } // "Voice", "Video", "Text"
    var overlayState by rememberSaveable(saver = VoiceOverlayStateSaver) { mutableStateOf(VoiceOverlayState.LISTENING) }
    val context = LocalContext.current

    var feedSource by rememberSaveable { mutableStateOf("Camera") } // "Camera", "Screen"
    var isScreenSharingActive by rememberSaveable { mutableStateOf(false) }

    fun handleDismiss() {
        if (isScreenSharingActive) {
            isScreenSharingActive = false
            try {
                com.example.MainActivity.mediaProjectionIntentData = null
                com.example.MainActivity.activeMediaProjection?.stop()
                com.example.MainActivity.activeMediaProjection = null
            } catch (_: Exception) {}
            try {
                val serviceIntent = android.content.Intent(context, com.example.ScreenShareService::class.java).apply {
                    action = "STOP_SERVICE"
                }
                context.startService(serviceIntent)
            } catch (_: Exception) {}
        }
        onDismiss()
    }

    // Only post the ongoing "assistant active" notification while the voice/video overlay
    // is actually visible. Previously this fired whenever the overlay composable was in the
    // tree (it's always composed, just hidden), so the notification kept reappearing.
    DisposableEffect(visible, selectedTab) {
        if (visible) {
            val title = when (selectedTab) {
                "Voice" -> "DepthLens Voice Assistant Active"
                "Video" -> "DepthLens Video Assistant Active"
                else -> "DepthLens Assistant Connected"
            }
            val text = "Tap to open the assistant panel."
            ActiveSessionNotificationManager.showNotification(context, title, text)
        } else {
            ActiveSessionNotificationManager.dismissNotification(context)
        }
        onDispose {
            ActiveSessionNotificationManager.dismissNotification(context)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(200)
            val serviceRunning = com.example.ScreenShareService.isServiceRunning
            if (isScreenSharingActive != serviceRunning) {
                isScreenSharingActive = serviceRunning
            }
        }
    }

    var selectedAnalysisTarget by rememberSaveable { mutableStateOf("Object") }
    var activePreviewView by remember { mutableStateOf<androidx.camera.view.PreviewView?>(null) }
    val latestCameraFrame = remember { java.util.concurrent.atomic.AtomicReference<android.graphics.Bitmap?>(null) }

    fun captureLiveFrameUriForQuery(): String? {
        val bitmap = try {
            when (feedSource) {
                "Camera" -> {
                    val cached = latestCameraFrame.get()
                    if (cached != null && !cached.isRecycled) {
                        cached.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                    } else {
                        activePreviewView?.bitmap
                    }
                }
                "Screen" -> if (isScreenSharingActive) drawScreenShareBitmap(context) else null
                else -> null
            }
        } catch (e: Exception) {
            android.util.Log.e("VoiceOverlay", "Unable to capture live visual frame", e)
            null
        }
        return bitmap?.let { frame ->
            try {
                saveBitmapToTempFile(context, frame)
            } finally {
                try { frame.recycle() } catch (_: Exception) {}
            }
        }
    }

    var liveTranscription by rememberSaveable { mutableStateOf("") }
    var isListeningState by remember { mutableStateOf(false) }

    var isMuted by rememberSaveable { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }

    val currentOnSubmitQuery by androidx.compose.runtime.rememberUpdatedState(onSubmitQuery)
    val currentOnUserInterrupt by androidx.compose.runtime.rememberUpdatedState(onUserInterrupt)

    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isListeningState = true
            com.example.ui.viewmodel.AudioConversationManager.startListening()
        }
    }

    fun startListeningFlow() {
        if (isMuted) return
        val hasMicPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasMicPermission) {
            isListeningState = true
            com.example.ui.viewmodel.AudioConversationManager.startListening()
        } else {
            audioPermLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(lastInteractionTime) {
        delay(3200)
        controlsVisible = false
    }

    // Configure and bind AudioConversationManager
    LaunchedEffect(visible, selectedTab, isMuted) {
        if (visible && (selectedTab == "Voice" || selectedTab == "Video")) {
            com.example.ui.viewmodel.AudioConversationManager.configure(
                context = context,
                isMuted = isMuted,
                ownerId = "FOREGROUND_CHAT",
                onQueryReady = { spoken ->
                    val visualFrameUri = if (selectedTab == "Video") captureLiveFrameUriForQuery() else null
                    currentOnSubmitQuery(spoken, visualFrameUri)
                    overlayState = VoiceOverlayState.THINKING
                },
                onPartialTranscript = { text ->
                    liveTranscription = text
                },
                onStateChange = { state ->
                    overlayState = when (state) {
                        com.example.ui.viewmodel.AudioState.IDLE -> VoiceOverlayState.LISTENING
                        com.example.ui.viewmodel.AudioState.LISTENING -> VoiceOverlayState.LISTENING
                        com.example.ui.viewmodel.AudioState.PROCESSING -> VoiceOverlayState.THINKING
                        com.example.ui.viewmodel.AudioState.AI_SPEAKING -> VoiceOverlayState.SPEAKING
                        com.example.ui.viewmodel.AudioState.WAIT_FOR_TTS_FINISH -> VoiceOverlayState.SPEAKING
                    }
                    isListeningState = (state == com.example.ui.viewmodel.AudioState.LISTENING)
                }
            )
            startListeningFlow()
        } else {
            com.example.ui.viewmodel.AudioConversationManager.reset("FOREGROUND_CHAT")
        }
    }

    var lensFacing by rememberSaveable { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == "Video" && !hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val currentlySpeakingId by (speechManager?.currentPlayingMessageId?.collectAsState() ?: remember { mutableStateOf(null) })
    val spokenCaption by (speechManager?.currentSpokenText?.collectAsState() ?: remember { mutableStateOf("") })

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    val keepScreenShareAlive = selectedTab == "Video" && feedSource == "Screen" && isScreenSharingActive
                    if (!keepScreenShareAlive) {
                        com.example.ui.viewmodel.AudioConversationManager.reset("FOREGROUND_CHAT")
                        isListeningState = false
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    if (visible && (selectedTab == "Voice" || selectedTab == "Video")) {
                        startListeningFlow()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(voiceOutputEnabled) {
        if (!voiceOutputEnabled) {
            speechManager?.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { latestCameraFrame.getAndSet(null)?.recycle() } catch (_: Exception) {}
        }
    }

    // Keep the freshest camera frame ready so Video Chat answers the user's question
    // against what is currently visible, without waiting for a capture round-trip.
    LaunchedEffect(selectedTab, visible, hasCameraPermission, feedSource, activePreviewView) {
        while (visible && selectedTab == "Video" && feedSource == "Camera" && hasCameraPermission) {
            val view = activePreviewView
            val frame = try { view?.bitmap } catch (e: Exception) { null }
            if (frame != null) {
                val prepared = copyBitmapForAnalysis(frame)
                try { frame.recycle() } catch (_: Exception) {}
                if (prepared != null) {
                    val old = latestCameraFrame.getAndSet(prepared)
                    try { old?.recycle() } catch (_: Exception) {}
                }
            }
            delay(650)
        }
    }

    val lastModelMsg = remember(activeMessages) { activeMessages.lastOrNull { it.role == "model" } }

    LaunchedEffect(lastModelMsg?.text, isLoading) {
        if (visible && (selectedTab == "Voice" || selectedTab == "Video") && lastModelMsg != null && voiceOutputEnabled) {
            if (lastModelMsg.text.isNotEmpty()) {
                if (overlayState == VoiceOverlayState.THINKING) {
                    overlayState = VoiceOverlayState.SPEAKING
                }
                speechManager?.playStreamProgress(
                    messageId = lastModelMsg.id,
                    text = lastModelMsg.text,
                    isFinished = !isLoading
                )
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(500, easing = EaseOutCubic)
        ),
        exit = fadeOut(animationSpec = tween(400)) + slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(400, easing = EaseInCubic)
        )
    ) {
        val lastModelMsg = remember(activeMessages) { activeMessages.lastOrNull { it.role == "model" } }
        val cleanModelText = remember(lastModelMsg?.text) { cleanResponseText(lastModelMsg?.text ?: "") }
        val cleanSpokenCaption = remember(spokenCaption) { compactLiveCaption(spokenCaption) }
        val displayText = when {
            isMuted -> "Muted. Tap the mic button to speak."
            overlayState == VoiceOverlayState.LISTENING -> if (liveTranscription.isNotEmpty()) liveTranscription else "Listening..."
            overlayState == VoiceOverlayState.THINKING -> if (liveTranscription.isNotEmpty()) liveTranscription else "Thinking..."
            cleanSpokenCaption.isNotEmpty() -> cleanSpokenCaption
            cleanModelText.isNotEmpty() -> compactLiveCaption(cleanModelText)
            selectedTab == "Video" && feedSource == "Screen" -> "Live screen ready - ask anything"
            selectedTab == "Video" -> "Camera lens ready - ask anything"
            else -> "Speaking..."
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0C0B1C).copy(alpha = 0.95f))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Radial drift glows
            val infiniteTransition = rememberInfiniteTransition(label = "drifting_glows")
            val glowAnim1 by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 20.dp.value,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowAnim1"
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Top-right cyan glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF38E1D8).copy(alpha = 0.15f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.15f + glowAnim1),
                        radius = size.minDimension * 0.6f
                    ),
                    radius = size.minDimension * 0.6f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.15f + glowAnim1)
                )
                // Middle-left violet glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF7C5CFF).copy(alpha = 0.20f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.12f, size.height * 0.4f - glowAnim1),
                        radius = size.minDimension * 0.7f
                    ),
                    radius = size.minDimension * 0.7f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.12f, size.height * 0.4f - glowAnim1)
                )
            }

            // MAIN CONTENT AREA (placed first so top/bottom controls float over it)
            val isCameraFull = selectedTab == "Video" && feedSource == "Camera" && hasCameraPermission
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = if (isCameraFull) 0.dp else 150.dp,
                        bottom = if (isCameraFull) 0.dp else 120.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selectedTab == "Video" && feedSource == "Camera") {
                    // CAMERA MODE
                    if (hasCameraPermission) {
                        VideoModePreview(
                            lensFacing = lensFacing,
                            onPreviewViewCreated = { activePreviewView = it },
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Switch camera switcher on top of the preview, top right
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                                    .clickable {
                                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                            CameraSelector.LENS_FACING_FRONT
                                        } else {
                                            CameraSelector.LENS_FACING_BACK
                                        }
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Switch Camera",
                                    tint = Color(0xFF38E1D8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        // CAMERA PERMISSION UI
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .premiumGlassBg(cornerRadius = 24.dp, borderAlpha = 0.3f)
                                    .padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideocamOff,
                                    contentDescription = "Camera Permission Needed",
                                    tint = Color(0xFF38E1D8),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "CAMERA FEED ACCESS",
                                    fontSize = 12.sp,
                                    fontFamily = DMMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Access is required for multimodal analysis and environmental focus.",
                                    fontSize = 10.sp,
                                    fontFamily = InstrumentSansFontFamily,
                                    color = Color(0xFF9D98C9),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C5CFF)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "GRANT ACCESS",
                                        fontFamily = DMMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedTab == "Video" && feedSource == "Screen") {
                    // SCREEN MODE
                    if (isScreenSharingActive) {
                        // Cast Grid Background + Screen Image Frame
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val gridStep = 48.dp.toPx()
                                val w = size.width
                                val h = size.height
                                
                                for (x in 0..w.toInt() step gridStep.toInt()) {
                                    drawLine(
                                        color = Color(0xFF161829).copy(alpha = 0.4f),
                                        start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f),
                                        end = androidx.compose.ui.geometry.Offset(x.toFloat(), h),
                                        strokeWidth = 1f
                                    )
                                }
                                for (y in 0..h.toInt() step gridStep.toInt()) {
                                    drawLine(
                                        color = Color(0xFF161829).copy(alpha = 0.4f),
                                        start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()),
                                        end = androidx.compose.ui.geometry.Offset(w, y.toFloat()),
                                        strokeWidth = 1f
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Live red pill
                                Row(
                                    modifier = Modifier
                                        .background(Color(0xFFFF4D6D).copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                                        .border(1.dp, Color(0xFFFF4D6D).copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val liveDotAlpha by infiniteTransition.animateFloat(
                                        initialValue = 0.3f,
                                        targetValue = 1.0f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1000, easing = EaseInOutSine),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "liveDotAlpha"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .graphicsLayer { alpha = liveDotAlpha }
                                            .background(Color(0xFFFF4D6D), CircleShape)
                                    )
                                    Text(
                                        text = "LIVE SHARING",
                                        color = Color(0xFFFF8AA0),
                                        fontSize = 11.sp,
                                        fontFamily = InstrumentSansFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.Black)
                                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(18.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    var latestScreenFrame by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                                    val screenContext = LocalContext.current
                                    LaunchedEffect(isScreenSharingActive) {
                                        while (isScreenSharingActive) {
                                            val frame = try { captureScreenProjection(screenContext) } catch (_: Exception) { null }
                                            if (frame != null) {
                                                val old = latestScreenFrame
                                                latestScreenFrame = frame
                                                try { old?.recycle() } catch (_: Exception) {}
                                            }
                                            delay(220)
                                        }
                                        latestScreenFrame = null
                                    }
                                    DisposableEffect(Unit) {
                                        onDispose {
                                            try { latestScreenFrame?.recycle() } catch (_: Exception) {}
                                        }
                                    }

                                    val frame = latestScreenFrame
                                    if (frame != null && !frame.isRecycled) {
                                        Image(
                                            bitmap = frame.asImageBitmap(),
                                            contentDescription = "Live shared screen",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tv,
                                                contentDescription = "Screen Share Casting",
                                                tint = Color(0xFF38E1D8),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "WAITING FOR LIVE SCREEN FRAME",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontFamily = DMMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // SCREEN SHARE INACTIVE UI
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .premiumGlassBg(cornerRadius = 24.dp, borderAlpha = 0.3f)
                                    .padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenShare,
                                    contentDescription = "Screen Share Inactive",
                                    tint = Color(0xFF7C5CFF),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "SCREEN SHARE INACTIVE",
                                    fontSize = 12.sp,
                                    fontFamily = DMMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Initiate visual screen sharing using the button below.",
                                    fontSize = 10.sp,
                                    fontFamily = InstrumentSansFontFamily,
                                    color = Color(0xFF9D98C9),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = {
                                        val nextActive = true
                                        isScreenSharingActive = nextActive
                                        lastInteractionTime = System.currentTimeMillis()
                                        if (nextActive) {
                                            val activity = findActivity(context)
                                            com.example.MainActivity.mediaProjectionIntentData = null
                                            if (activity is com.example.MainActivity) {
                                                activity.requestMediaProjection()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C5CFF)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "START SCREEN SHARE",
                                        fontFamily = DMMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // VOICE MODE
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Pulse rings animation variables
                        val pulse1 by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "pulse1"
                        )
                        val pulse2 by infiniteTransition.animateFloat(
                            initialValue = 0.5f,
                            targetValue = 1.5f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "pulse2"
                        )

                        Box(
                            modifier = Modifier
                                .size(170.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Pulse Rings Canvas
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (!isMuted && overlayState == VoiceOverlayState.LISTENING) {
                                    // Ring 1
                                    drawCircle(
                                        color = Color(0xFF7C5CFF).copy(alpha = (1f - pulse1).coerceIn(0f, 1f) * 0.4f),
                                        radius = size.minDimension / 2 * (1f + pulse1 * 0.5f),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                    )
                                    // Ring 2
                                    val p2Normalized = if (pulse2 > 1f) pulse2 - 1f else pulse2
                                    drawCircle(
                                        color = Color(0xFF7C5CFF).copy(alpha = (1f - p2Normalized).coerceIn(0f, 1f) * 0.4f),
                                        radius = size.minDimension / 2 * (1f + p2Normalized * 0.5f),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                    )
                                }
                            }

                            // Centered Orb
                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(Color(0xFF1A2A4A), Color(0xFF0A0F1F)),
                                            center = androidx.compose.ui.geometry.Offset.Unspecified
                                        )
                                    )
                                    .border(1.5.dp, Color(0xFF7C5CFF).copy(alpha = 0.4f), CircleShape)
                                    .clickable {
                                        // Cycle state manually
                                        overlayState = when (overlayState) {
                                            VoiceOverlayState.LISTENING -> VoiceOverlayState.THINKING
                                            VoiceOverlayState.THINKING -> VoiceOverlayState.SPEAKING
                                            VoiceOverlayState.SPEAKING -> VoiceOverlayState.LISTENING
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Inner glow
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color(0xFF7C5CFF).copy(alpha = 0.6f), Color.Transparent),
                                            center = center,
                                            radius = size.minDimension * 0.6f
                                        ),
                                        radius = size.minDimension * 0.6f,
                                        center = center
                                    )
                                }

                                EyeMark(
                                    state = overlayState,
                                    modifier = Modifier.size(100.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Waveform Bars
                        Row(
                            modifier = Modifier
                                .height(40.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val waveHeights = (0..6).map { index ->
                                infiniteTransition.animateFloat(
                                    initialValue = 8f,
                                    targetValue = 34f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(
                                            durationMillis = 600 + (index * 120) % 400,
                                            easing = EaseInOutSine
                                        ),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "wave_height_$index"
                                )
                            }
                            waveHeights.forEach { heightState ->
                                val height = if (!isMuted && overlayState != VoiceOverlayState.THINKING) heightState.value.dp else 8.dp
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .height(height)
                                        .background(Color(0xFFA78BFA), RoundedCornerShape(3.dp))
                                )
                            }
                        }

                        // State label (Listening / Speaking / Muted)
                        val stateText = when {
                            isMuted -> "Muted"
                            overlayState == VoiceOverlayState.LISTENING -> "Listening…"
                            overlayState == VoiceOverlayState.THINKING -> "Thinking…"
                            else -> "Speaking…"
                        }
                        Text(
                            text = stateText,
                            color = Color(0xFFEFEDFF),
                            fontSize = 15.sp,
                            fontFamily = InstrumentSansFontFamily,
                            fontWeight = FontWeight.Bold
                        )

                        // Live transcript line
                        Text(
                            text = displayText,
                            color = Color(0xFF9D98C9),
                            fontSize = 12.sp,
                            fontFamily = InstrumentSansFontFamily,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = 32.dp)
                                .widthIn(max = 240.dp)
                        )
                    }
                }
            }

            // Top segment control (overlaid on top of the content)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Voice", "Video", "Screen").forEach { mode ->
                    val isSelected = when (mode) {
                        "Voice" -> selectedTab == "Voice"
                        "Video" -> selectedTab == "Video" && feedSource == "Camera"
                        "Screen" -> selectedTab == "Video" && feedSource == "Screen"
                        else -> false
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Brush.linearGradient(listOf(Color(0xFF7C5CFF), Color(0xFF5B3FD6)))
                                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable {
                                if (mode == "Voice") {
                                    selectedTab = "Voice"
                                } else if (mode == "Video") {
                                    selectedTab = "Video"
                                    feedSource = "Camera"
                                } else {
                                    selectedTab = "Video"
                                    feedSource = "Screen"
                                }
                                lastInteractionTime = System.currentTimeMillis()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode,
                            color = if (isSelected) Color(0xFFEFEDFF) else Color(0xFF9D98C9),
                            fontSize = 11.sp,
                            fontFamily = InstrumentSansFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Top-left custom exit button
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .premiumGlassBg(cornerRadius = 100.dp, borderAlpha = 0.5f)
                        .clickable { handleDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit overlay",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Bottom controls
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 46.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Mute control
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(
                            if (isMuted) Color(0xFFFF4D6D).copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.07f)
                        )
                        .border(
                            1.dp,
                            if (isMuted) Color(0xFFFF4D6D).copy(alpha = 0.4f)
                            else Color.White.copy(alpha = 0.20f),
                            CircleShape
                        )
                        .clickable {
                            isMuted = !isMuted
                            lastInteractionTime = System.currentTimeMillis()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                        contentDescription = "Mute",
                        tint = if (isMuted) Color(0xFFFF4D6D) else Color(0xFFEFEDFF),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 2. End Call control
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFF4D6D), Color(0xFFD61F44))
                            )
                        )
                        .clickable { handleDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "End call",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 3. Speak/Play Response control
                val isSpeaking = currentlySpeakingId != null
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSpeaking) Color(0xFF7C5CFF).copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.07f)
                        )
                        .border(
                            1.dp,
                            if (isSpeaking) Color(0xFF7C5CFF).copy(alpha = 0.4f)
                            else Color.White.copy(alpha = 0.20f),
                            CircleShape
                        )
                        .clickable {
                            lastInteractionTime = System.currentTimeMillis()
                            if (isSpeaking) {
                                speechManager?.stop()
                            } else {
                                lastModelMsg?.let { msg ->
                                    speechManager?.playStreamProgress(
                                        messageId = msg.id,
                                        text = msg.text,
                                        isFinished = !isLoading
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = "Speak/Play Response",
                        tint = if (isSpeaking) Color(0xFF7C5CFF) else Color(0xFFEFEDFF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedOrb(
    state: VoiceOverlayState,
    onClickNextState: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale2"
    )

    val rotatingAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_spin"
    )

    val waveFactor1 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaking_wave1"
    )

    val waveFactor2 by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaking_wave2"
    )

    val orbMainColor by animateColorAsState(
        targetValue = when (state) {
            VoiceOverlayState.LISTENING -> PremiumCyan
            VoiceOverlayState.THINKING -> ElectricViolet
            VoiceOverlayState.SPEAKING -> Color(0xFF00FFD8)
        },
        animationSpec = tween(500),
        label = "orbColor"
    )

    val orbSecondColor by animateColorAsState(
        targetValue = when (state) {
            VoiceOverlayState.LISTENING -> ElectricViolet
            VoiceOverlayState.THINKING -> Color(0xFF9E00FF)
            VoiceOverlayState.SPEAKING -> Color(0xFF0055FF)
        },
        animationSpec = tween(500),
        label = "orbSecondColor"
    )

    // Smooth state transitions via animated alphas
    val listeningAlpha by animateFloatAsState(
        targetValue = if (state == VoiceOverlayState.LISTENING) 1f else 0f,
        animationSpec = tween(600, easing = EaseInOutCubic),
        label = "listeningAlpha"
    )
    val thinkingAlpha by animateFloatAsState(
        targetValue = if (state == VoiceOverlayState.THINKING) 1f else 0f,
        animationSpec = tween(600, easing = EaseInOutCubic),
        label = "thinkingAlpha"
    )
    val speakingAlpha by animateFloatAsState(
        targetValue = if (state == VoiceOverlayState.SPEAKING) 1f else 0f,
        animationSpec = tween(600, easing = EaseInOutCubic),
        label = "speakingAlpha"
    )

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClickNextState() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = center
            val baseRadius = size.minDimension * 0.35f
            
            // --- 1. LISTENING EFFECT (Subtle Pulsing) ---
            if (listeningAlpha > 0.01f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orbMainColor.copy(alpha = 0.15f * listeningAlpha), Color.Transparent),
                        center = centerOffset,
                        radius = baseRadius * 1.8f * pulseScale2
                    ),
                    radius = baseRadius * 1.8f * pulseScale2,
                    center = centerOffset
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orbSecondColor.copy(alpha = 0.25f * listeningAlpha), Color.Transparent),
                        center = centerOffset,
                        radius = baseRadius * 1.4f * pulseScale1
                    ),
                    radius = baseRadius * 1.4f * pulseScale1,
                    center = centerOffset
                )
                
                val gradient = Brush.radialGradient(
                    colors = listOf(orbMainColor.copy(alpha = listeningAlpha), orbSecondColor.copy(alpha = listeningAlpha)),
                    center = centerOffset,
                    radius = baseRadius * pulseScale1
                )
                drawCircle(
                    brush = gradient,
                    radius = baseRadius * 0.95f * pulseScale1,
                    center = centerOffset
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.15f * listeningAlpha),
                    radius = baseRadius * 0.5f,
                    center = Offset(centerOffset.x - baseRadius * 0.2f, centerOffset.y - baseRadius * 0.2f)
                )
            }
            
            // --- 2. THINKING EFFECT (Rapid Shifting Gradients & Orbit Ring) ---
            if (thinkingAlpha > 0.01f) {
                val ringRadius1 = baseRadius * 1.3f
                val ringRadius2 = baseRadius * 1.0f
                
                drawCircle(
                    color = orbSecondColor.copy(alpha = 0.12f * thinkingAlpha),
                    radius = ringRadius1,
                    center = centerOffset,
                    style = Stroke(width = 8.dp.toPx())
                )

                val satelliteCount = 3
                for (i in 0 until satelliteCount) {
                    val angleRad = Math.toRadians((rotatingAngle + (i * (360 / satelliteCount))).toDouble())
                    val satelliteCenterX = (centerOffset.x + ringRadius1 * Math.cos(angleRad)).toFloat()
                    val satelliteCenterY = (centerOffset.y + ringRadius1 * Math.sin(angleRad)).toFloat()
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(orbMainColor.copy(alpha = thinkingAlpha), Color.Transparent),
                            center = Offset(satelliteCenterX, satelliteCenterY),
                            radius = 24.dp.toPx()
                        ),
                        radius = 24.dp.toPx(),
                        center = Offset(satelliteCenterX, satelliteCenterY)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = thinkingAlpha),
                        radius = 4.dp.toPx(),
                        center = Offset(satelliteCenterX, satelliteCenterY)
                    )
                }

                // Rapidly shifting sweep gradient effect, rotating like a Radar/CSS scanner
                rotate(degrees = rotatingAngle * 2.5f, pivot = centerOffset) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                orbMainColor.copy(alpha = thinkingAlpha),
                                orbSecondColor.copy(alpha = thinkingAlpha),
                                Color.Transparent,
                                orbMainColor.copy(alpha = thinkingAlpha)
                            ),
                            center = centerOffset
                        ),
                        radius = ringRadius2,
                        center = centerOffset,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orbSecondColor.copy(alpha = thinkingAlpha), Color(0xFF03040A).copy(alpha = thinkingAlpha)),
                        center = centerOffset,
                        radius = baseRadius * 0.75f * pulseScale1
                    ),
                    radius = baseRadius * 0.75f * pulseScale1,
                    center = centerOffset
                )
            }
            
            // --- 3. SPEAKING EFFECT (Amplitude Expansion Waves) ---
            if (speakingAlpha > 0.01f) {
                drawCircle(
                    color = orbMainColor.copy(alpha = 0.08f * (1.5f - waveFactor2 * 0.9f) * speakingAlpha),
                    radius = baseRadius * waveFactor2 * 1.5f,
                    center = centerOffset,
                    style = Stroke(width = 2.dp.toPx())
                )

                drawCircle(
                    color = orbMainColor.copy(alpha = 0.15f * (1.3f - waveFactor1 * 0.9f) * speakingAlpha),
                    radius = baseRadius * waveFactor1 * 1.25f,
                    center = centerOffset,
                    style = Stroke(width = 3.dp.toPx())
                )

                drawCircle(
                    color = orbSecondColor.copy(alpha = 0.25f * speakingAlpha),
                    radius = baseRadius * (1f + (waveFactor1 - 1f) * 0.3f),
                    center = centerOffset,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                val coreRadius = baseRadius * (0.85f + (waveFactor1 - 1f) * 0.25f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orbMainColor.copy(alpha = speakingAlpha), orbSecondColor.copy(alpha = speakingAlpha)),
                        center = centerOffset,
                        radius = coreRadius
                    ),
                    radius = coreRadius,
                    center = centerOffset
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.3f * speakingAlpha),
                    radius = coreRadius * 0.8f,
                    center = centerOffset,
                    style = Stroke(width = 0.8.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun VideoModePreview(
    lensFacing: Int,
    onPreviewViewCreated: (PreviewView) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                onPreviewViewCreated(this)
            }
        },
        modifier = modifier,
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    android.util.Log.e("VideoModePreview", "Error binding CameraX on update", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                } catch (e: Exception) {
                    android.util.Log.e("VideoModePreview", "Error unbinding CameraX on dispose", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }
}

fun copyBitmapForAnalysis(bitmap: android.graphics.Bitmap, maxSide: Int = 960): android.graphics.Bitmap? {
    return try {
        if (bitmap.width <= 0 || bitmap.height <= 0 || bitmap.isRecycled) return null
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxSide) {
            bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
        } else {
            val scale = maxSide.toFloat() / longest.toFloat()
            val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
            android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true)
        }
    } catch (e: Exception) {
        android.util.Log.e("VideoModeCapture", "Error preparing bitmap for analysis", e)
        null
    }
}

fun saveBitmapToTempFile(context: android.content.Context, bitmap: android.graphics.Bitmap): String? {
    return try {
        val prepared = copyBitmapForAnalysis(bitmap, 960) ?: bitmap
        val file = java.io.File(context.cacheDir, "depthlens_live_frame_${System.currentTimeMillis()}.jpg")
        java.io.FileOutputStream(file).use { out ->
            prepared.compress(android.graphics.Bitmap.CompressFormat.JPEG, 72, out)
        }
        if (prepared !== bitmap) {
            try { prepared.recycle() } catch (_: Exception) {}
        }
        android.net.Uri.fromFile(file).toString()
    } catch (e: Exception) {
        android.util.Log.e("VideoModeCapture", "Error saving capture bitmap to file", e)
        null
    }
}

fun findActivity(context: android.content.Context): android.app.Activity? {
    var currentContext = context
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is android.app.Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

fun openAttachment(context: android.content.Context, uriString: String, mimeType: String) {
    try {
        val rawUri = android.net.Uri.parse(uriString)
        val uri = if (rawUri.scheme == "file") {
            val path = rawUri.path
            if (path != null) {
                try {
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        java.io.File(path)
                    )
                } catch (e: Exception) {
                    android.util.Log.e("openAttachment", "FileProvider resolution failed: ${e.message}")
                    rawUri
                }
            } else {
                rawUri
            }
        } else {
            rawUri
        }

        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("openAttachment", "Failed opening via MIME type, trying generic", e)
        try {
            val rawUri = android.net.Uri.parse(uriString)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, rawUri).apply {
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (ex: Exception) {
            android.util.Log.e("openAttachment", "Failed generic file open", ex)
            android.widget.Toast.makeText(context, "Cannot open this file type directly", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

object ActiveSessionNotificationManager {
    private const val CHANNEL_ID = "active_session_channel"
    private const val CHANNEL_NAME = "Active Sessions"
    private const val NOTIFICATION_ID = 9982

    fun showNotification(context: android.content.Context, title: String, text: String) {
        val prefs = context.getSharedPreferences("depthlens_prefs", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("live_session_enabled", true)) {
            return
        }
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(CHANNEL_ID, CHANNEL_NAME, android.app.NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun dismissNotification(context: android.content.Context) {
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
        notificationManager.cancel(NOTIFICATION_ID)
    }
}

fun captureScreenProjection(context: android.content.Context): android.graphics.Bitmap? {
    val serviceBitmap = com.example.ScreenShareService.latestBitmap.get()
    if (serviceBitmap != null && !serviceBitmap.isRecycled) {
        return try {
            serviceBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
        } catch (e: Exception) {
            null
        }
    }
    return null
}

@androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.O)
fun captureWindow(activity: android.app.Activity): android.graphics.Bitmap? {
    try {
        val window = activity.window ?: return null
        val view = window.decorView ?: return null
        val width = view.width
        val height = view.height
        if (width <= 0 || height <= 0) return null
        
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val latch = java.util.concurrent.CountDownLatch(1)
        val handlerThread = android.os.HandlerThread("PixelCopyThread")
        handlerThread.start()
        
        android.view.PixelCopy.request(
            window,
            bitmap,
            { copyResult ->
                latch.countDown()
                handlerThread.quitSafely()
            },
            android.os.Handler(handlerThread.looper)
        )
        
        val success = latch.await(2000, java.util.concurrent.TimeUnit.MILLISECONDS)
        return if (success) bitmap else null
    } catch (e: Exception) {
        android.util.Log.e("ScreenCapture", "PixelCopy capture failed, falling back: ${e.message}", e)
        return null
    }
}

fun captureWindowLegacy(activity: android.app.Activity): android.graphics.Bitmap? {
    return try {
        val view = activity.window.decorView
        val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        view.draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}

fun drawScreenShareBitmap(context: android.content.Context): android.graphics.Bitmap {
    val projectionBitmap = try { captureScreenProjection(context) } catch (e: Exception) { null }
    if (projectionBitmap != null) {
        return projectionBitmap
    }

    val width = 800
    val height = 600
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()

    paint.color = android.graphics.Color.parseColor("#070811")
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    paint.color = android.graphics.Color.parseColor("#161829")
    paint.strokeWidth = 1f
    val gridSize = 48
    for (x in 0 until width step gridSize) {
        canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), paint)
    }
    for (y in 0 until height step gridSize) {
        canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
    }

    paint.color = android.graphics.Color.WHITE
    paint.textSize = 22f
    paint.isAntiAlias = true
    paint.textAlign = android.graphics.Paint.Align.CENTER
    canvas.drawText("WAITING FOR LIVE SCREEN FRAME", width / 2f, height / 2f, paint)

    return bitmap
}

@Composable
fun SharedScreenLiveStreamPreview(
    isMuted: Boolean,
    onMuteToggle: () -> Unit,
    onStopScreenShare: () -> Unit,
    onMinimize: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var elapsedSeconds by remember { mutableStateOf(0) }
    var latestFrame by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    val previewContext = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            val frame = try { captureScreenProjection(previewContext) } catch (_: Exception) { null }
            if (frame != null) {
                val old = latestFrame
                latestFrame = frame
                try { old?.recycle() } catch (_: Exception) {}
            }
            delay(220)
        }
    }

    DisposableEffect(Unit) {
        onDispose { try { latestFrame?.recycle() } catch (_: Exception) {} }
    }
    
    // Timer Effect
    LaunchedEffect(Unit) {
        elapsedSeconds = 0
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }
    
    // Pulsating indicator animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val indicatorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "indicatorAlpha"
    )

    Box(
        modifier = modifier
            .background(Color(0xFF070811))
            .padding(16.dp)
    ) {
        // Futuristic grid background
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val gridStep = 48.dp.toPx()
            val w = size.width
            val h = size.height
            
            for (x in 0..w.toInt() step gridStep.toInt()) {
                drawLine(
                    color = Color(0xFF161829).copy(alpha = 0.4f),
                    start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f),
                    end = androidx.compose.ui.geometry.Offset(x.toFloat(), h),
                    strokeWidth = 1f
                )
            }
            for (y in 0..h.toInt() step gridStep.toInt()) {
                drawLine(
                    color = Color(0xFF161829).copy(alpha = 0.4f),
                    start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()),
                    end = androidx.compose.ui.geometry.Offset(w, y.toFloat()),
                    strokeWidth = 1f
                )
            }
        }

        // Main Layout
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar: Indicator & Timer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .premiumGlassBg(cornerRadius = 16.dp, borderAlpha = 0.15f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsating red dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .graphicsLayer { alpha = indicatorAlpha }
                            .background(Color(0xFFFE3B30), CircleShape)
                    )
                    Text(
                        text = "LIVE SHARING",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = DMMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Timer text
                val mins = elapsedSeconds / 60
                val secs = elapsedSeconds % 60
                val timerStr = String.format("%02d:%02d", mins, secs)
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Session duration",
                        tint = PremiumCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = timerStr,
                        color = PremiumCyan,
                        fontSize = 12.sp,
                        fontFamily = DMMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 2. Central Visual: live screen frame
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                val frame = latestFrame
                if (frame != null && !frame.isRecycled) {
                    Image(
                        bitmap = frame.asImageBitmap(),
                        contentDescription = "Live shared screen",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "Screen Share Casting",
                            tint = PremiumCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "WAITING FOR LIVE SCREEN FRAME",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = DMMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isLoading) "Analyzing captured screen content..." else "Keep screen sharing active and ask anything",
                            color = if (isLoading) PremiumCyan else TextMutedColor,
                            fontSize = 11.sp,
                            fontFamily = InstrumentSansFontFamily,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 3. Bottom Controls Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                    .premiumGlassBg(cornerRadius = 32.dp, borderAlpha = 0.3f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Mute button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) Color.Red.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                            .border(1.dp, if (isMuted) Color.Red else Color.White.copy(alpha = 0.15f), CircleShape)
                            .clickable { onMuteToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute/Unmute Mic",
                            tint = if (isMuted) Color.Red else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = if (isMuted) "UNMUTE" else "MUTE",
                        color = if (isMuted) Color.Red else TextMutedColor,
                        fontSize = 8.sp,
                        fontFamily = DMMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Stop sharing button (Large red action)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFE3B30))
                            .clickable { onStopScreenShare() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Screen Share",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "STOP SHARE",
                        color = Color(0xFFFE3B30),
                        fontSize = 8.sp,
                        fontFamily = DMMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Minimize / Return to chat button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .clickable { onMinimize() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Minimize to Chat",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "CHAT VIEW",
                        color = TextMutedColor,
                        fontSize = 8.sp,
                        fontFamily = DMMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun GlassPopupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    // Popups must stay readable — a translucent 62% fill let the chat bleed through.
    // Use the theme card colour at near-opaque alpha, tuned per glass material:
    // Liquid Crystal reads as slightly lighter glass, Frost Aurora as a solid frost.
    val isLiquid = ThemeManager.glassStyle == "Liquid Crystal"
    val popupBg = SurfaceCardColor.copy(alpha = if (isLiquid) 0.93f else 0.98f)
    Column(
        modifier = modifier
            .background(
                color = popupBg,
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

@Composable
fun GlassPopupItem(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean = false,
    hasSubmenu: Boolean = false,
    isExpanded: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor = if (isPressed) Color.White.copy(alpha = 0.12f)
                  else if (isSelected) ElectricViolet.copy(alpha = 0.20f)
                  else if (isExpanded) Color.White.copy(alpha = 0.08f)
                  else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = InstrumentSansFontFamily,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) ElectricViolet else TextPrimaryColor,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontFamily = InstrumentSansFontFamily,
                    fontSize = 9.sp,
                    color = TextMutedColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
        if (isSelected) {
            Text(
                text = "✓",
                fontFamily = InstrumentSansFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricViolet,
                modifier = Modifier.padding(start = 4.dp)
            )
        } else if (hasSubmenu) {
            Text(
                text = "›",
                fontFamily = InstrumentSansFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextMutedColor,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun ReplyHeaderBlock(
    replyToMessageId: String,
    selectedText: String,
    allMessages: List<com.example.data.model.MessageEntity>,
    isUserMessage: Boolean,
    onRepliedBoxClick: (String) -> Unit
) {
    val repliedMsg = allMessages.find { it.id == replyToMessageId }
    val senderLabel = if (repliedMsg?.role == "user") "You" else "DepthLens"
    val barColor = if (isUserMessage) Color.White.copy(alpha = 0.6f) else ElectricViolet
    val bgColor = if (isUserMessage) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f)
    val textColor = if (isUserMessage) Color.White.copy(alpha = 0.85f) else TextPrimaryColor
    val labelColor = if (isUserMessage) Color.White.copy(alpha = 0.7f) else SectionLabelColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable { onRepliedBoxClick(replyToMessageId) }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical reply bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(28.dp)
                .background(barColor, RoundedCornerShape(1.5.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Replying to $senderLabel",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                fontFamily = InstrumentSansFontFamily
            )
            Text(
                text = selectedText,
                fontSize = 10.sp,
                color = textColor,
                fontFamily = InstrumentSansFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CustomSelectionProvider(
    onCopy: (String) -> Unit,
    onReply: (String) -> Unit,
    onQuote: (String) -> Unit,
    onShare: (String) -> Unit,
    onSelectAll: () -> Unit,
    content: @Composable (selectionKey: Int) -> Unit
) {
    val systemToolbar = androidx.compose.ui.platform.LocalTextToolbar.current
    val systemClipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    var lastCapturedText by remember { mutableStateOf("") }
    var selectionKey by remember { mutableStateOf(0) }
    var popupMenuState by remember { mutableStateOf<PopupMenuState?>(null) }
    
    val customClipboardManager = remember {
        object : androidx.compose.ui.platform.ClipboardManager {
            override fun setText(annotatedString: androidx.compose.ui.text.AnnotatedString) {
                lastCapturedText = annotatedString.text
                systemClipboardManager.setText(annotatedString)
            }
            override fun getText(): androidx.compose.ui.text.AnnotatedString? {
                return systemClipboardManager.getText()
            }
        }
    }
    
    val customTextToolbar = remember {
        object : androidx.compose.ui.platform.TextToolbar {
            override val status: androidx.compose.ui.platform.TextToolbarStatus
                get() = if (popupMenuState != null) androidx.compose.ui.platform.TextToolbarStatus.Shown else androidx.compose.ui.platform.TextToolbarStatus.Hidden
                
            override fun showMenu(
                rect: androidx.compose.ui.geometry.Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?
            ) {
                // ROOT CAUSE FIX: previously we called onCopyRequested() here at
                // show-time to grab the selection string. That eager copy collapsed the
                // live selection on-device, so Android's drag handles + highlight vanished
                // the instant the toolbar appeared. Instead grab the text lazily, only when
                // the user actually taps an action — the selection + handles stay visible.
                fun grabSelectedText(): String {
                    onCopyRequested?.invoke()   // routes through customClipboardManager
                    return lastCapturedText
                }

                popupMenuState = PopupMenuState(
                    rect = rect,
                    selectedText = "",
                    onCopy = {
                        onCopy(grabSelectedText())
                        selectionKey++
                        popupMenuState = null
                    },
                    onReply = {
                        onReply(grabSelectedText())
                        selectionKey++
                        popupMenuState = null
                    },
                    onQuote = {
                        onQuote(grabSelectedText())
                        selectionKey++
                        popupMenuState = null
                    },
                    onShare = {
                        onShare(grabSelectedText())
                        selectionKey++
                        popupMenuState = null
                    },
                    onSelectAll = {
                        onSelectAllRequested?.invoke()
                        onSelectAll()
                    }
                )
            }
            
            override fun hide() {
                popupMenuState = null
            }
        }
    }
    
    val selectionColors = remember(ThemeManager.accentColor) {
        androidx.compose.foundation.text.selection.TextSelectionColors(
            handleColor = ThemeManager.accentColor,
            backgroundColor = ThemeManager.accentColor.copy(alpha = 0.35f)
        )
    }
    
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalTextToolbar provides customTextToolbar,
        androidx.compose.ui.platform.LocalClipboardManager provides customClipboardManager,
        androidx.compose.foundation.text.selection.LocalTextSelectionColors provides selectionColors
    ) {
        // wrapContentSize (no fillMaxWidth) so the message bubble shrinks to its
        // text — short messages like "hi" no longer stretch to a big rectangle.
        Box(modifier = Modifier.wrapContentWidth()) {
            content(selectionKey)

            popupMenuState?.let { state ->
                CustomSelectionMenu(
                    state = state,
                    onDismiss = { popupMenuState = null }
                )
            }
        }
    }
}

data class PopupMenuState(
    val rect: androidx.compose.ui.geometry.Rect,
    val selectedText: String,
    val onCopy: () -> Unit,
    val onReply: () -> Unit,
    val onQuote: () -> Unit,
    val onShare: () -> Unit,
    val onSelectAll: () -> Unit
)

@Composable
private fun CustomSelectionMenu(
    state: PopupMenuState,
    onDismiss: () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    // Smooth fade + scale animations for the custom selection floating toolbar
    val scale = remember { androidx.compose.animation.core.Animatable(0.85f) }
    val alpha = remember { androidx.compose.animation.core.Animatable(0f) }
    
    LaunchedEffect(Unit) {
        val scope = this
        scope.launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            )
        }
        scope.launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 180,
                    easing = androidx.compose.animation.core.LinearOutSlowInEasing
                )
            )
        }
    }
    
    // Anchor the toolbar exactly over the selected text. `state.rect` is the
    // selection bounds in window pixels, so we position off it directly instead
    // of a fixed alignment. Prefers just above the selection, flips below if
    // there's no room, and clamps inside the screen.
    val menuPositionProvider = remember(state.rect) {
        object : androidx.compose.ui.window.PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: androidx.compose.ui.unit.IntRect,
                windowSize: androidx.compose.ui.unit.IntSize,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                popupContentSize: androidx.compose.ui.unit.IntSize
            ): androidx.compose.ui.unit.IntOffset {
                val gapPx = with(density) { 12.dp.roundToPx() }
                val marginPx = with(density) { 8.dp.roundToPx() }
                val centerX = state.rect.center.x.toInt() - popupContentSize.width / 2
                val x = centerX.coerceIn(
                    marginPx,
                    (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
                )
                val aboveY = state.rect.top.toInt() - popupContentSize.height - gapPx
                val belowY = state.rect.bottom.toInt() + gapPx
                val y = if (aboveY >= marginPx) {
                    aboveY
                } else {
                    belowY.coerceAtMost(
                        (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx)
                    )
                }
                return androidx.compose.ui.unit.IntOffset(x, y)
            }
        }
    }

    androidx.compose.ui.window.Popup(
        popupPositionProvider = menuPositionProvider,
        onDismissRequest = onDismiss,
        // focusable = false: keeps the SelectionContainer focused so Android's
        // native start/end selection handles stay visible and draggable.
        properties = androidx.compose.ui.window.PopupProperties(focusable = false)
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(26.dp),
                    clip = false,
                    ambientColor = ThemeManager.accentColor.copy(alpha = 0.25f),
                    spotColor = ThemeManager.accentColor.copy(alpha = 0.35f)
                )
                .background(
                    // Opaque base tinted by the live accent so the toolbar clearly
                    // tracks the app theme (dark vs light) and accent color.
                    brush = Brush.verticalGradient(
                        colors = if (ThemeManager.isDarkTheme) listOf(
                            androidx.compose.ui.graphics.lerp(Color(0xF20E0B20), ThemeManager.accentColor, 0.20f),
                            androidx.compose.ui.graphics.lerp(Color(0xF716122F), ThemeManager.accentColor, 0.12f)
                        ) else listOf(
                            androidx.compose.ui.graphics.lerp(Color(0xFAFFFFFF), ThemeManager.accentColor, 0.12f),
                            androidx.compose.ui.graphics.lerp(Color(0xFAF2EEFF), ThemeManager.accentColor, 0.20f)
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .background(
                    // Diagonal specular sheen = same glass look as the rest of the app.
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (ThemeManager.isDarkTheme) 0.10f else 0.16f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .border(
                    width = 1.1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            ThemeManager.accentColor.copy(alpha = 0.45f),
                            ThemeManager.accentColor.copy(alpha = 0.15f)
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            val menuContentColor = if (ThemeManager.isDarkTheme) Color.White else Color(0xFF1A1730)
            val menuDividerColor = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f)
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val menuItems = listOf(
                    Triple("Copy", Icons.Default.ContentCopy, state.onCopy),
                    Triple("Reply", Icons.AutoMirrored.Filled.Reply, state.onReply),
                    Triple("Quote", Icons.Default.Chat, state.onQuote),
                    Triple("Select All", Icons.Default.SelectAll, state.onSelectAll)
                )
                
                menuItems.forEachIndexed { index, (label, icon, action) ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(menuDividerColor)
                        )
                    }
                    
                    val itemInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val rippleIndication = androidx.compose.material3.ripple(
                        color = ThemeManager.accentColor
                    )
                    
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(
                                interactionSource = itemInteractionSource,
                                indication = rippleIndication,
                                onClick = action
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = menuContentColor,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            fontSize = 9.5.sp,
                            color = menuContentColor,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InstrumentSansFontFamily
                        )
                    }
                }
            }
        }
    }
}


