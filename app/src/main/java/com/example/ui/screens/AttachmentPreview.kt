package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.AttachmentEntity
import com.example.ui.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// Modern attachment preview (ChatGPT/Claude-style): real thumbnails + in-app
// viewers for image/video/audio/pdf/text. Never punts images/video to external
// apps. Docs/unsupported fall back to an "Open With" chooser.
// ─────────────────────────────────────────────────────────────────────────────

private enum class AttachKind { IMAGE, VIDEO, AUDIO, PDF, TEXT, DOC, OTHER }

private fun kindOf(mime: String, fileName: String): AttachKind {
    val m = mime.lowercase()
    val n = fileName.lowercase()
    return when {
        m.startsWith("image/") -> AttachKind.IMAGE
        m.startsWith("video/") -> AttachKind.VIDEO
        m.startsWith("audio/") -> AttachKind.AUDIO
        m == "application/pdf" || n.endsWith(".pdf") -> AttachKind.PDF
        m.startsWith("text/") || n.endsWith(".txt") || n.endsWith(".json") ||
            n.endsWith(".kt") || n.endsWith(".java") || n.endsWith(".py") ||
            n.endsWith(".js") || n.endsWith(".ts") || n.endsWith(".md") ||
            n.endsWith(".xml") || n.endsWith(".csv") || n.endsWith(".log") -> AttachKind.TEXT
        n.endsWith(".doc") || n.endsWith(".docx") || n.endsWith(".xls") || n.endsWith(".xlsx") ||
            n.endsWith(".ppt") || n.endsWith(".pptx") -> AttachKind.DOC
        else -> AttachKind.OTHER
    }
}

// In-memory bitmap cache for generated thumbnails (video frame, pdf page).
private val thumbCache = android.util.LruCache<String, android.graphics.Bitmap>(48)

private fun normalizeUri(context: android.content.Context, uriString: String): android.net.Uri {
    val raw = android.net.Uri.parse(uriString)
    if (raw.scheme == "file") {
        val path = raw.path
        if (path != null) {
            return try {
                androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", java.io.File(path)
                )
            } catch (e: Exception) { raw }
        }
    }
    return raw
}

private suspend fun generateThumb(
    context: android.content.Context, uriString: String, kind: AttachKind
): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
    thumbCache.get(uriString)?.let { return@withContext it }
    val uri = android.net.Uri.parse(uriString)
    val bmp = try {
        when (kind) {
            AttachKind.VIDEO -> {
                val retriever = android.media.MediaMetadataRetriever()
                try {
                    if (uri.scheme == "file") {
                        retriever.setDataSource(uri.path)
                    } else {
                        retriever.setDataSource(context, uri)
                    }
                    retriever.getFrameAtTime(1_000_000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } finally {
                    try { retriever.release() } catch (_: Exception) {}
                }
            }
            AttachKind.PDF -> {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val renderer = android.graphics.pdf.PdfRenderer(pfd)
                    renderer.use { r ->
                        if (r.pageCount < 1) return@use null
                        val page = r.openPage(0)
                        val scale = 2
                        val out = android.graphics.Bitmap.createBitmap(
                            page.width * scale, page.height * scale, android.graphics.Bitmap.Config.ARGB_8888
                        )
                        out.eraseColor(android.graphics.Color.WHITE)
                        page.render(out, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        out
                    }
                }
            }
            else -> null
        }
    } catch (e: Exception) { null }
    bmp?.let { thumbCache.put(uriString, it) }
    bmp
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> ""
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

private fun querySize(context: android.content.Context, uriString: String): Long = try {
    val uri = android.net.Uri.parse(uriString)
    if (uri.scheme == "file") java.io.File(uri.path ?: "").length()
    else context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
        if (idx >= 0 && c.moveToFirst()) c.getLong(idx) else 0L
    } ?: 0L
} catch (e: Exception) { 0L }

private suspend fun resolveAttachmentLocalUri(
    context: android.content.Context,
    attachment: AttachmentEntity,
    onProgress: (Boolean) -> Unit = {}
): String = withContext(Dispatchers.IO) {
    val currentUri = attachment.localUri
    if (currentUri.isNotBlank()) {
        try {
            val uri = android.net.Uri.parse(currentUri)
            if (uri.scheme == "content") {
                var readable = false
                try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use {
                        readable = true
                    }
                } catch (e: Exception) {}
                if (readable) return@withContext currentUri
            } else {
                val path = uri.path ?: currentUri
                val file = java.io.File(path)
                if (file.exists()) return@withContext currentUri
            }
        } catch (e: Exception) {}
    }

    val remote = attachment.remoteUrl
    if (remote.isNullOrBlank()) return@withContext currentUri

    try {
        onProgress(true)
        val cacheDir = java.io.File(context.cacheDir, "attachments")
        cacheDir.mkdirs()
        val destFile = java.io.File(cacheDir, "${attachment.attachmentId}_${attachment.fileName}")
        
        if (!destFile.exists()) {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val request = okhttp3.Request.Builder().url(remote).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { input ->
                        java.io.FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }

        if (destFile.exists()) {
            val newLocalUri = "file://${destFile.absolutePath}"
            val updated = attachment.copy(localUri = newLocalUri)
            try {
                val db = com.example.data.database.DepthDatabase.getDatabase(context)
                db.attachmentDao().insertAttachment(updated)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onProgress(false)
            return@withContext newLocalUri
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    onProgress(false)
    return@withContext currentUri
}

/**
 * Compact glass thumbnail card for one attachment. Tapping calls [onClick].
 */
@Composable
fun AttachmentThumb(attachment: AttachmentEntity, onClick: () -> Unit) {
    val context = LocalContext.current
    var resolvedUri by remember(attachment.attachmentId, attachment.localUri, attachment.remoteUrl) {
        mutableStateOf(attachment.localUri)
    }
    var isDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(attachment.attachmentId, attachment.localUri, attachment.remoteUrl) {
        resolvedUri = resolveAttachmentLocalUri(context, attachment) { isDownloading = it }
    }

    val model = resolvedUri
    val kind = remember(attachment.attachmentId) { kindOf(attachment.mimeType, attachment.fileName) }
    val accent = ThemeManager.accentColor

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val cardShape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .graphicsLayer {
                val s = if (appeared) 1f else 0.9f
                scaleX = s; scaleY = s; alpha = if (appeared) 1f else 0f
            }
            .clip(cardShape)
            .background(Color.White.copy(alpha = if (ThemeManager.isDarkTheme) 0.06f else 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), cardShape)
            .clickable(enabled = !isDownloading) { onClick() }
    ) {
        when (kind) {
            AttachKind.IMAGE -> {
                AsyncImage(
                    model = model,
                    contentDescription = attachment.fileName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .widthIn(min = 80.dp, max = 240.dp)
                        .heightIn(min = 80.dp, max = 240.dp)
                        .clip(cardShape)
                )
            }
            AttachKind.VIDEO, AttachKind.PDF -> {
                var thumb by remember(attachment.attachmentId) { mutableStateOf<android.graphics.Bitmap?>(null) }
                LaunchedEffect(attachment.attachmentId, model) { thumb = generateThumb(context, model, kind) }
                val bmp = thumb
                if (bmp != null) {
                    val aspectRatio = bmp.width.toFloat() / bmp.height.toFloat()
                    Box(
                        modifier = Modifier
                            .widthIn(min = 80.dp, max = 240.dp)
                            .heightIn(min = 80.dp, max = 240.dp)
                            .aspectRatio(aspectRatio)
                            .clip(cardShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = attachment.fileName,
                            modifier = Modifier.matchParentSize().clip(cardShape),
                            contentScale = ContentScale.Crop
                        )
                        if (kind == AttachKind.VIDEO) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(24.dp)) }
                        }
                    }
                } else {
                    Box(
                        Modifier.size(120.dp, 120.dp).clip(cardShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (kind == AttachKind.PDF) Icons.Default.PictureAsPdf else Icons.Default.PlayArrow,
                            null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
            else -> {
                // Audio / Doc / Text / Other → icon + name + size card
                Row(
                    Modifier.width(210.dp).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(11.dp))
                            .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.6f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (kind) {
                            AttachKind.AUDIO -> Icons.Default.Headset
                            AttachKind.TEXT -> Icons.Default.Description
                            else -> Icons.Default.InsertDriveFile
                        }
                        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            attachment.fileName.ifBlank { "attachment" },
                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        val sz = remember(attachment.attachmentId, model) { formatSize(querySize(context, model)) }
                        Text(
                            if (sz.isNotBlank()) sz else "Tap to open",
                            color = Color(0xFF9D98C9), fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Downloading Overlay
        if (isDownloading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = accent,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

/**
 * Full-screen in-app viewer. Routes by type; docs/unsupported immediately fall
 * back to the system "Open With" chooser and dismiss.
 */
@Composable
fun InAppAttachmentViewer(attachment: AttachmentEntity, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var resolvedUri by remember(attachment.attachmentId, attachment.localUri, attachment.remoteUrl) {
        mutableStateOf(attachment.localUri)
    }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadFailed by remember { mutableStateOf(false) }
    var retryCount by remember { mutableStateOf(0) }

    LaunchedEffect(attachment.attachmentId, attachment.localUri, attachment.remoteUrl, retryCount) {
        downloadFailed = false
        val uri = resolveAttachmentLocalUri(context, attachment) { isDownloading = it }
        resolvedUri = uri
        
        // Double-check if the file is readable
        val readable = if (uri.isNotBlank()) {
            try {
                val parsed = android.net.Uri.parse(uri)
                if (parsed.scheme == "content") {
                    var ok = false
                    context.contentResolver.openFileDescriptor(parsed, "r")?.use { ok = true }
                    ok
                } else {
                    val path = parsed.path ?: uri
                    java.io.File(path).exists()
                }
            } catch (e: Exception) { false }
        } else false

        if (!readable && !attachment.remoteUrl.isNullOrBlank()) {
            downloadFailed = true
        }
    }

    val model = resolvedUri
    val kind = remember(attachment.attachmentId) { kindOf(attachment.mimeType, attachment.fileName) }

    if (kind == AttachKind.DOC || kind == AttachKind.OTHER) {
        LaunchedEffect(attachment.attachmentId, model) {
            if (!isDownloading && !downloadFailed) {
                openAttachment(context, model, attachment.mimeType)
                onDismiss()
            }
        }
        if (isDownloading) {
            Dialog(onDismissRequest = onDismiss) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.material3.CircularProgressIndicator(color = ThemeManager.accentColor)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading…", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
        return
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.94f))) {
            AnimatedVisibility(shown, enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.92f, animationSpec = tween(200)),
                exit = fadeOut() + scaleOut()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isDownloading) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.material3.CircularProgressIndicator(color = ThemeManager.accentColor)
                            Spacer(Modifier.height(16.dp))
                            Text("Downloading attachment…", color = Color.White, fontSize = 14.sp)
                        }
                    } else if (downloadFailed) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Failed to download attachment", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Please check your internet connection and try again.", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                androidx.compose.material3.Button(
                                    onClick = { onDismiss() },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                                ) {
                                    Text("Cancel", color = Color.White)
                                }
                                androidx.compose.material3.Button(
                                    onClick = { retryCount++ },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ThemeManager.accentColor)
                                ) {
                                    Text("Retry", color = Color.White)
                                }
                            }
                        }
                    } else {
                        when (kind) {
                            AttachKind.IMAGE -> ZoomableImage(model, onDismiss)
                            AttachKind.VIDEO -> VideoPlayerBox(android.net.Uri.parse(model))
                            AttachKind.AUDIO -> AudioPlayerBox(android.net.Uri.parse(model), attachment.fileName)
                            AttachKind.PDF -> PdfViewerBox(context, android.net.Uri.parse(model))
                            AttachKind.TEXT -> TextViewerBox(context, android.net.Uri.parse(model), attachment.fileName)
                            else -> {}
                        }
                    }
                }
            }
            // Close button
            Box(
                Modifier.align(Alignment.TopEnd).padding(16.dp).size(42.dp)
                    .clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.15f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(22.dp)) }
        }
    }
}

@Composable
private fun ZoomableImage(model: Any?, onDismiss: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    Box(
        Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x; offsetY += pan.y
                    } else {
                        offsetX = 0f; offsetY = 0f
                        // when not zoomed, a downward pan dismisses
                        if (pan.y > 40f) onDismiss()
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) { scale = 1f; offsetX = 0f; offsetY = 0f } else scale = 2.5f
                })
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model, contentDescription = "Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale; scaleY = scale; translationX = offsetX; translationY = offsetY
            }
        )
    }
}

@Composable
private fun VideoPlayerBox(uri: android.net.Uri) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            android.widget.VideoView(ctx).apply {
                if (uri.scheme == "file") {
                    setVideoPath(uri.path)
                } else {
                    setVideoURI(uri)
                }
                val mc = android.widget.MediaController(ctx)
                mc.setAnchorView(this)
                setMediaController(mc)
                setOnPreparedListener { it.isLooping = false; start() }
            }
        },
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
    )
}

@Composable
private fun AudioPlayerBox(uri: android.net.Uri, name: String) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    val player = remember { android.media.MediaPlayer() }
    DisposableEffect(Unit) {
        try {
            if (uri.scheme == "file") {
                val file = java.io.File(uri.path ?: "")
                java.io.FileInputStream(file).use { fis ->
                    player.setDataSource(fis.fd)
                }
            } else {
                player.setDataSource(context, uri)
            }
            player.prepare()
        } catch (_: Exception) {}
        player.setOnCompletionListener { playing = false }
        onDispose { try { player.release() } catch (_: Exception) {} }
    }
    Column(
        Modifier.padding(28.dp).clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.08f)).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Headset, null, tint = ThemeManager.accentColor, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(14.dp))
        Text(name.ifBlank { "Audio" }, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier.size(66.dp).clip(RoundedCornerShape(50))
                .background(ThemeManager.accentColor)
                .clickable {
                    try {
                        if (playing) { player.pause(); playing = false }
                        else { player.start(); playing = true }
                    } catch (_: Exception) {}
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun PdfViewerBox(context: android.content.Context, uri: android.net.Uri) {
    var pages by remember { mutableStateOf<List<android.graphics.Bitmap>>(emptyList()) }
    LaunchedEffect(uri) {
        pages = withContext(Dispatchers.IO) {
            val out = mutableListOf<android.graphics.Bitmap>()
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    android.graphics.pdf.PdfRenderer(pfd).use { r ->
                        val count = minOf(r.pageCount, 30)
                        for (i in 0 until count) {
                            val page = r.openPage(i)
                            val scale = 2
                            val bmp = android.graphics.Bitmap.createBitmap(
                                page.width * scale, page.height * scale, android.graphics.Bitmap.Config.ARGB_8888
                            )
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            out.add(bmp)
                        }
                    }
                }
            } catch (_: Exception) {}
            out
        }
    }
    if (pages.isEmpty()) {
        Icon(Icons.Default.PictureAsPdf, null, tint = Color.White, modifier = Modifier.size(60.dp))
    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 60.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            for (bmp in pages) {
                Image(bmp.asImageBitmap(), "PDF page", Modifier.fillMaxWidth().padding(vertical = 6.dp), contentScale = ContentScale.FillWidth)
            }
        }
    }
}

@Composable
private fun TextViewerBox(context: android.content.Context, uri: android.net.Uri, name: String) {
    var text by remember { mutableStateOf("Loading…") }
    LaunchedEffect(uri) {
        text = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    val buf = CharArray(200_000)
                    val n = it.read(buf)
                    if (n > 0) String(buf, 0, n) else "(empty)"
                } ?: "(cannot read)"
            } catch (e: Exception) { "(cannot read: ${e.message})" }
        }
    }
    Column(Modifier.fillMaxSize().padding(top = 64.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        Text(name.ifBlank { "Text" }, color = ThemeManager.accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(text, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
        }
    }
}
