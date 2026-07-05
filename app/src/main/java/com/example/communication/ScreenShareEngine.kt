package com.example.communication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference

class ScreenShareEngine(private val context: Context) {
    companion object {
        private const val TAG = "ScreenShareEngine"
        val latestBitmap = AtomicReference<Bitmap?>(null)

        private val _hasFrames = MutableStateFlow(false)
        val hasFrames: StateFlow<Boolean> = _hasFrames.asStateFlow()
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var projectionCallback: MediaProjection.Callback? = null

    fun startCapture(
        resultCode: Int,
        intentData: android.content.Intent,
        onCaptureStarted: () -> Unit,
        onCaptureStopped: () -> Unit
    ) {
        stopCapture()
        Log.d(TAG, "[STEP 1] startCapture requested. resultCode: $resultCode, intentData: $intentData")

        try {
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            if (projectionManager == null) {
                Log.e(TAG, "[ERROR] MediaProjectionManager is null - cannot start screen capture")
                onCaptureStopped()
                return
            }

            Log.d(TAG, "[STEP 2] Acquiring MediaProjection from projectionManager")
            val token = projectionManager.getMediaProjection(resultCode, intentData)
            if (token == null) {
                Log.e(TAG, "[ERROR] MediaProjection token is null - permission might be denied or revoked")
                android.os.Handler(context.mainLooper).post {
                    android.widget.Toast.makeText(context, "Screen share permission was denied", android.widget.Toast.LENGTH_SHORT).show()
                }
                onCaptureStopped()
                return
            }
            mediaProjection = token
            com.example.MainActivity.activeMediaProjection = token
            Log.d(TAG, "[STEP 3] MediaProjection token acquired successfully: $mediaProjection")

            // Create background thread for registering callback and processing frames
            backgroundThread = HandlerThread("ScreenShareBg").apply { start() }
            val threadHandler = Handler(backgroundThread!!.looper)
            backgroundHandler = threadHandler

            // Create MediaProjection.Callback
            val callback = object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection.Callback: onStop received internally")
                    stopCapture()
                    onCaptureStopped()
                }
            }
            projectionCallback = callback

            // Register callback BEFORE creating ImageReader/VirtualDisplay
            try {
                mediaProjection?.registerCallback(callback, threadHandler)
                Log.d(TAG, "MediaProjection.Callback registered successfully")
            } catch (e: Exception) {
                Log.e(TAG, "[ERROR] Failed to register MediaProjection callback: ${e.message}", e)
                android.os.Handler(context.mainLooper).post {
                    android.widget.Toast.makeText(context, "Failed to register projection callback: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                }
                stopCapture()
                onCaptureStopped()
                return
            }

            // Correct display width/height/densityDpi for VirtualDisplay
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
            var width = 1080
            var height = 1920
            var density = 320

            if (wm != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    val windowMetrics = wm.currentWindowMetrics
                    val bounds = windowMetrics.bounds
                    width = bounds.width()
                    height = bounds.height()
                } else {
                    val display = wm.defaultDisplay
                    val point = android.graphics.Point()
                    @Suppress("DEPRECATION")
                    display.getRealSize(point)
                    width = point.x
                    height = point.y
                }
            } else {
                val metrics = context.resources.displayMetrics
                width = metrics.widthPixels
                height = metrics.heightPixels
            }

            // Ensure dimensions are valid positive values
            if (width <= 0) width = 1080
            if (height <= 0) height = 1920
            
            val metrics = context.resources.displayMetrics
            density = if (metrics.densityDpi > 0) metrics.densityDpi else 320

            Log.d(TAG, "[STEP 4] Initializing ImageReader at verified resolution: ${width}x${height}, density: $density")

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader = reader
            var frameCount = 0
            reader.setOnImageAvailableListener({ imageReaderListener ->
                try {
                    val image = imageReaderListener.acquireLatestImage() ?: return@setOnImageAvailableListener
                    try {
                        frameCount++
                        if (frameCount == 1) {
                            Log.d(TAG, "[STEP 6] First screen capture image frame received successfully!")
                        }
                        if (frameCount % 30 == 1) {
                            Log.d(TAG, "imagereader onImageAvailable firing. Frame count: $frameCount")
                        }
                        
                        val planes = image.planes
                        val buffer: ByteBuffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * width

                        val bmp = Bitmap.createBitmap(
                            width + rowPadding / pixelStride,
                            height,
                            Bitmap.Config.ARGB_8888
                        )
                        bmp.copyPixelsFromBuffer(buffer)

                        val finalBmp = if (rowPadding > 0) {
                            val cropped = Bitmap.createBitmap(bmp, 0, 0, width, height)
                            if (cropped != bmp) {
                                bmp.recycle()
                            }
                            cropped
                        } else {
                            bmp
                        }

                        val oldBmp = latestBitmap.getAndSet(finalBmp)
                        oldBmp?.recycle()
                        _hasFrames.value = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing image frame: ${e.message}", e)
                    } finally {
                        image.close()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in onImageAvailable: ${e.message}", e)
                }
            }, threadHandler)

            Log.d(TAG, "[STEP 5] Creating VirtualDisplay for MediaProjection")
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "DepthLensDisplay",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                threadHandler
            )
            
            if (virtualDisplay != null) {
                Log.d(TAG, "[STEP 5 - SUCCESS] VirtualDisplay created successfully: $virtualDisplay")
            } else {
                Log.e(TAG, "[ERROR] Failed to create VirtualDisplay - virtualDisplay returned null")
                android.os.Handler(context.mainLooper).post {
                    android.widget.Toast.makeText(context, "Failed to initialize virtual display", android.widget.Toast.LENGTH_SHORT).show()
                }
                stopCapture()
                onCaptureStopped()
                return
            }

            onCaptureStarted()
            Log.d(TAG, "Screen Capture VirtualDisplay created successfully and callback registered")
        } catch (e: Exception) {
            Log.e(TAG, "[ERROR] Failed to start capture: ${e.message}", e)
            android.os.Handler(context.mainLooper).post {
                android.widget.Toast.makeText(context, "Failed to start capture: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            }
            stopCapture()
            onCaptureStopped()
        }
    }

    fun stopCapture() {
        Log.d(TAG, "stopCapture requested - cleaning up resources")
        try {
            _hasFrames.value = false
            
            virtualDisplay?.release()
            virtualDisplay = null

            imageReader?.setOnImageAvailableListener(null, null)
            imageReader?.close()
            imageReader = null

            projectionCallback?.let { callback ->
                try {
                    mediaProjection?.unregisterCallback(callback)
                } catch (e: Exception) {
                    Log.e(TAG, "Error unregistering callback: ${e.message}")
                }
            }
            projectionCallback = null

            mediaProjection?.stop()
            mediaProjection = null
            com.example.MainActivity.activeMediaProjection = null

            backgroundThread?.quitSafely()
            backgroundThread = null
            backgroundHandler = null

            Log.d(TAG, "Screen capture cleaned up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up ScreenShareEngine: ${e.message}", e)
        }
    }
}
