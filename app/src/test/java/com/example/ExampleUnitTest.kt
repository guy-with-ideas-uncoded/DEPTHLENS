package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.ByteArrayInputStream
import com.google.firebase.FirebaseApp

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class ExampleUnitTest {
    @Test
    fun testFirebaseStorageUpload() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var testError: Throwable? = null
        var responseInfo: String? = null
        
        val thread = Thread {
            try {
                var apiKey = ""
                var projectId = ""
                var appId = ""
                
                // Search for .env file
                val searchPaths = listOf(File(".env"), File("../.env"), File("../../.env"), File("/.env"), File("app/.env"))
                val finalEnvFile = searchPaths.find { it.exists() }
                if (finalEnvFile != null) {
                    finalEnvFile.readLines().forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("FIREBASE_API_KEY=")) {
                            apiKey = trimmed.substringAfter("FIREBASE_API_KEY=").trim().removeSurrounding("\"").removeSurrounding("'")
                        }
                        if (trimmed.startsWith("FIREBASE_PROJECT_ID=")) {
                            projectId = trimmed.substringAfter("FIREBASE_PROJECT_ID=").trim().removeSurrounding("\"").removeSurrounding("'")
                        }
                        if (trimmed.startsWith("FIREBASE_APP_ID=")) {
                            appId = trimmed.substringAfter("FIREBASE_APP_ID=").trim().removeSurrounding("\"").removeSurrounding("'")
                        }
                    }
                }
                
                if (apiKey.isEmpty() || projectId.isEmpty() || appId.isEmpty()) {
                    val gsPaths = listOf(File("google-services.json"), File("app/google-services.json"), File("../app/google-services.json"))
                    val gsFile = gsPaths.find { it.exists() }
                    if (gsFile != null) {
                        val content = gsFile.readText()
                        val pidRegex = """(?s)"project_id":\s*"([^"]+)"""".toRegex()
                        projectId = pidRegex.find(content)?.groupValues?.getOrNull(1) ?: ""
                        val apiKeyRegex = """(?s)"current_key":\s*"([^"]+)"""".toRegex()
                        apiKey = apiKeyRegex.find(content)?.groupValues?.getOrNull(1) ?: ""
                        val appIdRegex = """(?s)"mobilesdk_app_id":\s*"([^"]+)"""".toRegex()
                        appId = appIdRegex.find(content)?.groupValues?.getOrNull(1) ?: ""
                    }
                }

                val client = okhttp3.OkHttpClient()
                val requestBody = okhttp3.RequestBody.create(null, ByteArray(0))
                
                val urlLegacy = "https://firebasestorage.googleapis.com/v0/b/$projectId.appspot.com/o?uploadType=resumable&name=test_${System.currentTimeMillis()}.txt"
                val requestLegacy = okhttp3.Request.Builder()
                    .url(urlLegacy)
                    .header("X-Goog-Upload-Protocol", "resumable")
                    .header("X-Goog-Upload-Command", "start")
                    .header("X-Goog-Upload-Header-Content-Length", "11")
                    .header("X-Goog-Upload-Header-Content-Type", "text/plain")
                    .post(requestBody)
                    .build()
                
                var resLegacy = ""
                try {
                    val response = client.newCall(requestLegacy).execute()
                    resLegacy = "Legacy Bucket ($projectId.appspot.com) -> Code: ${response.code}, Body: ${response.body?.string()}"
                } catch (err: Exception) {
                    resLegacy = "Legacy Bucket ($projectId.appspot.com) -> Exception: ${err.message}"
                }

                val urlNew = "https://firebasestorage.googleapis.com/v0/b/$projectId.firebasestorage.app/o?uploadType=resumable&name=test_${System.currentTimeMillis()}.txt"
                val requestNew = okhttp3.Request.Builder()
                    .url(urlNew)
                    .header("X-Goog-Upload-Protocol", "resumable")
                    .header("X-Goog-Upload-Command", "start")
                    .header("X-Goog-Upload-Header-Content-Length", "11")
                    .header("X-Goog-Upload-Header-Content-Type", "text/plain")
                    .post(requestBody)
                    .build()
                
                var resNew = ""
                try {
                    val response = client.newCall(requestNew).execute()
                    resNew = "New Bucket ($projectId.firebasestorage.app) -> Code: ${response.code}, Body: ${response.body?.string()}"
                } catch (err: Exception) {
                    resNew = "New Bucket ($projectId.firebasestorage.app) -> Exception: ${err.message}"
                }

                responseInfo = "$resLegacy\n$resNew"
            } catch (e: Throwable) {
                testError = e
            }
        }
        
        thread.start()
        thread.join()
        
        if (testError != null) {
            val e = testError!!
            val writer = java.io.StringWriter()
            e.printStackTrace(java.io.PrintWriter(writer))
            val stackTrace = writer.toString()
            fail("OkHttp test threw exception:\nType: ${e::class.java.name}\nMessage: ${e.message}\nStackTrace:\n$stackTrace")
        } else {
            println("OkHttp Request completed successfully!\nResponse:\n$responseInfo")
        }
    }
}




