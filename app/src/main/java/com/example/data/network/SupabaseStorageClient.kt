package com.example.data.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

object SupabaseStorageClient {
    private const val TAG = "SupabaseStorageClient"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val supabaseUrl: String
        get() {
            var url = BuildConfig.SUPABASE_URL.trim().removeSuffix("/")
            if (url.endsWith("/rest/v1")) {
                url = url.removeSuffix("/rest/v1")
            }
            return url
        }

    private val anonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY.trim()

    /**
     * Uploads bytes to a Supabase Storage bucket at a specific path.
     * Returns the public URL of the uploaded file on success, or null on failure.
     */
    fun uploadBytes(bucket: String, path: String, bytes: ByteArray, mimeType: String, upsert: Boolean = false): String? {
        val sUrl = supabaseUrl
        val aKey = anonKey
        
        if (sUrl.isBlank() || aKey.isBlank() || sUrl.contains("PLACEHOLDER")) {
            Log.e(TAG, "Supabase credentials are not configured properly.")
            return null
        }

        // URL encode the path segments except slashes to handle spaces/special characters
        val encodedPath = path.split("/").joinToString("/") { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
        val url = "$sUrl/storage/v1/object/$bucket/$encodedPath"
        
        Log.d(TAG, "Uploading to: $url, size: ${bytes.size} bytes, mimeType: $mimeType, upsert: $upsert")

        val mediaType = mimeType.toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()
        val requestBody = bytes.toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $aKey")
            .header("apikey", aKey)
            .post(requestBody)
        
        if (upsert) {
            requestBuilder.header("x-upsert", "true")
        }

        val request = requestBuilder.build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseCode = response.code
                val bodyString = response.body?.string()
                
                Log.d(TAG, "Upload response code: $responseCode, body: $bodyString")
                
                if (response.isSuccessful) {
                    getPublicUrl(bucket, path)
                } else {
                    Log.e(TAG, "Upload failed with code $responseCode: $bodyString")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during upload", e)
            null
        }
    }

    /**
     * Helper to read bytes from an Input Stream and upload them.
     */
    fun uploadInputStream(context: Context, bucket: String, path: String, uri: Uri, mimeType: String, upsert: Boolean = false): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.use { it.readBytes() }
            if (bytes != null) {
                uploadBytes(bucket, path, bytes, mimeType, upsert)
            } else {
                Log.e(TAG, "Failed to read bytes from Uri: $uri")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception reading Uri stream", e)
            null
        }
    }

    /**
     * Helper to upload a local file.
     */
    fun uploadFile(bucket: String, path: String, file: File, mimeType: String, upsert: Boolean = false): String? {
        return try {
            if (file.exists()) {
                val bytes = file.readBytes()
                uploadBytes(bucket, path, bytes, mimeType, upsert)
            } else {
                Log.e(TAG, "Local file does not exist: ${file.absolutePath}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception reading local file", e)
            null
        }
    }

    /**
     * Deletes a single file from the bucket.
     */
    fun deleteFile(bucket: String, path: String): Boolean {
        return deleteFiles(bucket, listOf(path))
    }

    /**
     * Deletes multiple files from the bucket.
     */
    fun deleteFiles(bucket: String, paths: List<String>): Boolean {
        if (supabaseUrl.isBlank() || anonKey.isBlank() || supabaseUrl.contains("PLACEHOLDER") || paths.isEmpty()) {
            return false
        }

        val url = "$supabaseUrl/storage/v1/object/$bucket"
        Log.d(TAG, "Deleting files from bucket $bucket: $paths")

        val json = JSONObject()
        val jsonArray = JSONArray()
        paths.forEach { jsonArray.put(it) }
        json.put("prefixes", jsonArray)

        val mediaType = "application/json".toMediaTypeOrNull()
        val requestBody = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $anonKey")
            .header("apikey", anonKey)
            .delete(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                Log.d(TAG, "Delete response code: ${response.code}, body: $bodyString")
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during deletion", e)
            false
        }
    }

    /**
     * Generates a signed URL for a private object in Supabase Storage.
     */
    fun getSignedUrl(bucket: String, path: String, expiresIn: Int = 3600): String? {
        val sUrl = supabaseUrl
        val aKey = anonKey
        if (sUrl.isBlank() || aKey.isBlank() || path.isBlank()) {
            Log.e(TAG, "Cannot sign object: Supabase configuration or storage path is empty")
            return null
        }

        val encodedPath = encodeStoragePath(path)
        val endpoint = "$sUrl/storage/v1/object/sign/$bucket/$encodedPath"
        val requestBody = JSONObject()
            .apply { put("expiresIn", expiresIn) }
            .toString()
            .toRequestBody("application/json".toMediaTypeOrNull())

        return try {
            val request = Request.Builder()
                .url(endpoint)
                .header("Authorization", "Bearer $aKey")
                .header("apikey", aKey)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Signed URL request failed status=${response.code} path=$path body=$responseBody")
                    return@use null
                }

                val json = JSONObject(responseBody)
                // Storage API versions have returned both spellings. Accept either.
                val rawSignedUrl = json.optString("signedURL")
                    .takeIf { it.isNotBlank() }
                    ?: json.optString("signedUrl").takeIf { it.isNotBlank() }
                    ?: json.optString("signed_url").takeIf { it.isNotBlank() }

                if (rawSignedUrl == null) {
                    Log.e(TAG, "Signed URL response did not contain a recognized URL field")
                    return@use null
                }

                absoluteStorageUrl(rawSignedUrl)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception generating signed URL for path=$path", e)
            null
        }
    }

    private fun encodeStoragePath(path: String): String =
        path.trim().trimStart('/').split("/")
            .filter { it.isNotEmpty() }
            .joinToString("/") {
                java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20")
            }

    /** Converts relative Storage API responses into URLs accepted by OkHttp. */
    private fun absoluteStorageUrl(url: String): String {
        val value = url.trim()
        return when {
            value.startsWith("https://") || value.startsWith("http://") -> value
            value.startsWith("/storage/v1/") -> "$supabaseUrl$value"
            value.startsWith("/object/") -> "$supabaseUrl/storage/v1$value"
            value.startsWith("storage/v1/") -> "$supabaseUrl/$value"
            value.startsWith("object/") -> "$supabaseUrl/storage/v1/$value"
            value.startsWith("/") -> "$supabaseUrl$value"
            else -> "$supabaseUrl/$value"
        }
    }

    private fun safeUrlForLog(url: String): String = url.substringBefore('?')

    /**
     * Generates a public URL for the given bucket and path.
     */
    fun getPublicUrl(bucket: String, path: String): String {
        val encodedPath = path.split("/").joinToString("/") { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
        return "$supabaseUrl/storage/v1/object/public/$bucket/$encodedPath"
    }

    /**
     * Result of a low-level storage download attempt. [httpStatus] is the HTTP code,
     * or -1 if the request threw before a response was received.
     */
    data class DownloadOutcome(
        val success: Boolean,
        val httpStatus: Int,
        val bytesWritten: Long,
        val errorBody: String? = null,
        val exception: String? = null
    )

    /**
     * Extracts the storage object path (the part AFTER the bucket name) from any
     * Supabase Storage URL, whether public (`/object/public/<bucket>/...`),
     * signed (`/object/sign/<bucket>/...?token=...`) or authenticated
     * (`/object/<bucket>/...`). Returns null if the URL is not a storage URL for
     * the given bucket. The returned path is URL-decoded and query-stripped so it
     * matches the exact key used at upload time.
     */
    fun storagePathFromUrl(url: String?, bucket: String = "attachments"): String? {
        if (url.isNullOrBlank() || !url.startsWith("http")) return null
        val markers = listOf(
            "/object/public/$bucket/",
            "/object/sign/$bucket/",
            "/object/authenticated/$bucket/",
            "/object/$bucket/"
        )
        for (marker in markers) {
            val idx = url.indexOf(marker)
            if (idx >= 0) {
                var path = url.substring(idx + marker.length)
                // Strip any query string (signed-URL token, cache-busters, etc.)
                path = path.substringBefore("?").substringBefore("#")
                if (path.isBlank()) return null
                return try {
                    // Decode each segment; keep slashes intact.
                    path.split("/").joinToString("/") { java.net.URLDecoder.decode(it, "UTF-8") }
                } catch (e: Exception) {
                    path
                }
            }
        }
        return null
    }

    /**
     * Downloads [url] into [dest], writing full diagnostics to logcat on failure.
     * Sends the anon apikey/Authorization headers so the call works for public
     * buckets, signed URLs, and RLS-protected buckets alike. Never throws.
     */
    fun downloadUrlToFile(url: String, dest: File): DownloadOutcome {
        val normalizedUrl = absoluteStorageUrl(url)
        return try {
            val aKey = anonKey
            val builder = Request.Builder().url(normalizedUrl).get()
            if (aKey.isNotBlank()) {
                builder.header("apikey", aKey)
                builder.header("Authorization", "Bearer $aKey")
            }

            client.newCall(builder.build()).execute().use { response ->
                val status = response.code
                if (response.isSuccessful) {
                    dest.parentFile?.mkdirs()
                    val tempFile = File(dest.parentFile, "${dest.name}.part")
                    if (tempFile.exists()) tempFile.delete()
                    val written = response.body?.byteStream()?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: 0L

                    if (written > 0L && tempFile.renameTo(dest)) {
                        Log.d(TAG, "Download succeeded status=$status bytes=$written file=${dest.name}")
                        DownloadOutcome(true, status, written)
                    } else {
                        if (tempFile.exists()) tempFile.delete()
                        if (dest.exists() && dest.length() == 0L) dest.delete()
                        Log.e(TAG, "Download produced no committed data status=$status url=${safeUrlForLog(normalizedUrl)}")
                        DownloadOutcome(false, status, written, errorBody = "empty or uncommitted body")
                    }
                } else {
                    val body = response.body?.string()
                    Log.e(TAG, "Download failed status=$status url=${safeUrlForLog(normalizedUrl)} body=$body")
                    DownloadOutcome(false, status, 0L, errorBody = body)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download exception url=${safeUrlForLog(normalizedUrl)}", e)
            if (dest.exists() && dest.length() == 0L) dest.delete()
            DownloadOutcome(false, -1, 0L, exception = e.toString())
        }
    }
}
