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
        val encodedPath = path.split("/").joinToString("/") { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
        val url = "$sUrl/storage/v1/object/sign/$bucket/$encodedPath"

        val json = JSONObject().apply { put("expiresIn", expiresIn) }
        val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $aKey")
            .header("apikey", aKey)
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    JSONObject(bodyString ?: "").getString("signedUrl")
                } else {
                    Log.e(TAG, "Error generating signed URL: ${response.code} ${response.body?.string()}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception generating signed URL", e)
            null
        }
    }

    /**
     * Generates a public URL for the given bucket and path.
     */
    fun getPublicUrl(bucket: String, path: String): String {
        val encodedPath = path.split("/").joinToString("/") { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
        return "$supabaseUrl/storage/v1/object/public/$bucket/$encodedPath"
    }
}
