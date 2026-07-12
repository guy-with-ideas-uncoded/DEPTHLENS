import java.util.Base64
import java.util.Random

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  id("com.google.gms.google-services")
}

// Ensure Google Services JSON exists before the plugin is evaluated to avoid build failure
val googleServicesFile = File(projectDir, "google-services.json")
if (!googleServicesFile.exists()) {
    try {
        val dummyContent = """
        {
          "project_info": {
            "project_number": "123456789012",
            "project_id": "dummy-project-id",
            "storage_bucket": "dummy-project-id.appspot.com"
          },
          "client": [
            {
              "client_info": {
                "mobilesdk_app_id": "1:123456789012:android:0123456789abcdef012345",
                "android_client_info": {
                  "package_name": "com.aistudio.depthlens.v6.final"
                }
              },
              "oauth_client": [],
              "api_key": [
                {
                  "current_key": "dummy-api-key"
                }
              ],
              "services": {
                "appinvite_service": {
                  "other_platform_oauth_client": []
                }
              }
            }
          ],
          "configuration_version": "1"
        }
        """.trimIndent()
        googleServicesFile.writeText(dummyContent)
        println("Generated dummy google-services.json at ${googleServicesFile.absolutePath}")
    } catch (e: Exception) {
        println("Warning: failed to generate dummy google-services.json: ${e.message}")
    }
}

// Decode debug.keystore.base64 to debug.keystore if missing
val rootDirFile = project.rootDir
val debugKeystoreFile = File(rootDirFile, "debug.keystore")
val debugKeystoreBase64File = File(rootDirFile, "debug.keystore.base64")
if (!debugKeystoreFile.exists() && debugKeystoreBase64File.exists()) {
    try {
        val base64Text = debugKeystoreBase64File.readText().trim()
        if (base64Text.isNotEmpty()) {
            val decodedBytes = Base64.getDecoder().decode(base64Text)
            debugKeystoreFile.writeBytes(decodedBytes)
            println("Successfully restored debug.keystore from debug.keystore.base64 during configuration!")
        }
    } catch (e: Exception) {
        println("Warning: failed to restore debug.keystore: ${e.message}")
    }
}

android {
  namespace = "com.example"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.aistudio.depthlens.v6.final"
    minSdk = 24
    targetSdk = 35
    versionCode = 6000
    versionName = "6.0.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: ""
      val storePwd = System.getenv("STORE_PASSWORD") ?: ""
      if (keystorePath.isNotEmpty() && storePwd.isNotEmpty()) {
        storeFile = file(keystorePath)
        storePassword = storePwd
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      } else {
        // Fallback to debug signature so that builds in AI Studio are always validly signed and installable
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isDebuggable = false
      isCrunchPngs = false
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }

  bundle {
    language { enableSplit = false }
    density { enableSplit = false }
    abi { enableSplit = false }
  }

  splits {
    abi {
      isEnable = false
      isUniversalApk = true
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    jniLibs {
      useLegacyPackaging = true
    }
  }

  androidResources {
    noCompress.add("tflite")
  }
  sourceSets {
    getByName("main") {
      assets.directories.add("src/main/assets")
    }
  }
}

// Build-time helper: automatically generate .env file from host environment variables
// prior to secrets-gradle-plugin evaluation so that injected secrets are correctly compiled.
val rootEnvFileSetting = rootProject.file(".env")
if (!rootEnvFileSetting.exists()) {
    val geminiKey = System.getenv("GEMINI_API_KEY") ?: ""
    val firebaseKey = System.getenv("FIREBASE_API_KEY") ?: ""
    val firebaseProjectId = System.getenv("FIREBASE_PROJECT_ID") ?: ""
    val firebaseAppId = System.getenv("FIREBASE_APP_ID") ?: ""
    
    if (geminiKey.isNotEmpty() || firebaseKey.isNotEmpty() || firebaseProjectId.isNotEmpty()) {
        val content = """
            GEMINI_API_KEY=${if (geminiKey.isNotEmpty()) geminiKey else "MY_GEMINI_API_KEY"}
            FIREBASE_API_KEY=${if (firebaseKey.isNotEmpty()) firebaseKey else "PLACEHOLDER_FIREBASE_API_KEY"}
            FIREBASE_PROJECT_ID=${if (firebaseProjectId.isNotEmpty()) firebaseProjectId else "PLACEHOLDER_FIREBASE_PROJECT_ID"}
            FIREBASE_APP_ID=${if (firebaseAppId.isNotEmpty()) firebaseAppId else "PLACEHOLDER_FIREBASE_APP_ID"}
        """.trimIndent()
        rootEnvFileSetting.writeText(content)
        println("Generated root .env file from System environment variables successfully.")
    }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

tasks.register("generateFirebaseConfigs") {
    val rootEnvFile = rootProject.file(".env")
    val targetJsonFile = file("google-services.json")

    doFirst {
        var envApiKey = ""
        var envProjectId = ""
        var envAppId = ""
        
        if (rootEnvFile.exists()) {
            val lines = rootEnvFile.readLines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("FIREBASE_API_KEY=")) {
                    envApiKey = trimmed.substringAfter("FIREBASE_API_KEY=").trim().removeSurrounding("\"").removeSurrounding("'")
                }
                if (trimmed.startsWith("FIREBASE_PROJECT_ID=")) {
                    envProjectId = trimmed.substringAfter("FIREBASE_PROJECT_ID=").trim().removeSurrounding("\"").removeSurrounding("'")
                }
                if (trimmed.startsWith("FIREBASE_APP_ID=")) {
                    envAppId = trimmed.substringAfter("FIREBASE_APP_ID=").trim().removeSurrounding("\"").removeSurrounding("'")
                }
            }
        }
        
        if (envApiKey.isEmpty()) envApiKey = System.getenv("FIREBASE_API_KEY") ?: ""
        if (envProjectId.isEmpty()) envProjectId = System.getenv("FIREBASE_PROJECT_ID") ?: ""
        if (envAppId.isEmpty()) envAppId = System.getenv("FIREBASE_APP_ID") ?: ""

        val targetFile = targetJsonFile
        var shouldOverwrite = !targetFile.exists()

        if (targetFile.exists()) {
            val content = targetFile.readText()
            if (content.contains("dummy-project-id") || content.contains("dummy-api-key") || content.contains("PLACEHOLDER_FIREBASE_API_KEY") || content.trim().isEmpty()) {
                if (envApiKey.isNotEmpty()) {
                    shouldOverwrite = true
                }
            }
        }

        if (shouldOverwrite) {
            val finalApiKey = if (envApiKey.isNotEmpty()) envApiKey else "PLACEHOLDER_FIREBASE_API_KEY"
            val finalProjectId = if (envProjectId.isNotEmpty()) envProjectId else "com-aistudio-depthlens-uqmzkx"
            val finalAppId = if (envAppId.isNotEmpty()) envAppId else "1:123456789012:android:abcdef1234567890"

            println("generateFirebaseConfigs: finalApiKey length = ${finalApiKey.length}, starts with = ${if (finalApiKey.length > 5) finalApiKey.substring(0, 5) else "N/A"}")
            println("generateFirebaseConfigs: finalProjectId = $finalProjectId")
            println("generateFirebaseConfigs: finalAppId = $finalAppId")

            val jsonTemplate = """
            {
              "project_info": {
                "project_number": "123456789012",
                "project_id": "$finalProjectId",
                "storage_bucket": "$finalProjectId.appspot.com"
              },
              "client": [
                {
                  "client_info": {
                    "mobilesdk_app_id": "$finalAppId",
                    "android_client_info": {
                      "package_name": "com.aistudio.depthlens.v6.final"
                    }
                  },
                  "oauth_client": [
                    {
                      "client_id": "123456789012-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com",
                      "client_type": 3
                    }
                  ],
                  "api_key": [
                    {
                      "current_key": "$finalApiKey"
                    }
                  ],
                  "services": {
                    "appinvite_service": {
                      "other_platform_oauth_client": []
                    }
                  }
                }
              ],
              "configuration_version": "1"
            }
            """.trimIndent()
            targetFile.writeText(jsonTemplate)
            println("Dynamically resolved /app/google-services.json contents successfully.")
        }
    }
}

// Ensure processGoogleServices depends on generateFirebaseConfigs
tasks.matching { it.name.contains("GoogleServices") }.configureEach {
    dependsOn("generateFirebaseConfigs")
}

androidComponents {
    onVariants { variant ->
        val vName = variant.name
        val variantName = variant.name.replaceFirstChar { it.uppercaseChar() }
        
        // Resolve file paths at configure time to comply with Configuration Cache requirements
        val buildDirFile = layout.buildDirectory.get().asFile
        val apkDirResolved = File(buildDirFile, "outputs/apk/$vName")
        val originalApkResolved = File(apkDirResolved, "app-$vName.apk")
        
        val bundleDirResolved = File(buildDirFile, "outputs/bundle/$vName")
        val originalAabResolved = File(bundleDirResolved, "app-$vName.aab")
        
        val rootDirResolved = rootProject.rootDir
        val buildOutputDirResolved = File(rootDirResolved, "build-outputs")
        val dotBuildOutputDirResolved = File(rootDirResolved, ".build-outputs")

        tasks.matching { it.name == "package$variantName" }.configureEach {
            doLast {
                if (originalApkResolved.exists()) {
                    if (!buildOutputDirResolved.exists()) buildOutputDirResolved.mkdirs()
                    if (!dotBuildOutputDirResolved.exists()) dotBuildOutputDirResolved.mkdirs()
                    
                    val versionedApkNames = if (vName == "release") listOf("DepthLens_v6.0.0.apk") else listOf("DepthLens_v6.0.0-debug.apk")
                    
                    versionedApkNames.forEach { apkName ->
                        // Copy to build-outputs
                        originalApkResolved.copyTo(File(buildOutputDirResolved, apkName), overwrite = true)
                        
                        // Copy to .build-outputs
                        originalApkResolved.copyTo(File(dotBuildOutputDirResolved, apkName), overwrite = true)
                    }
                    
                    val sizeMB = originalApkResolved.length() / (1024.0 * 1024.0)
                    println("Successfully copied $vName APK to /build-outputs and /.build-outputs as DepthLens_v6.0.0.apk (${String.format("%.2f", sizeMB)} MB)")
                }
            }
        }
    }
}

dependencies {
  // Core Android / Ktx
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.activity.compose)

  // Compose Bom & UI
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.ui.text.google.fonts)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)

  // Navigation
  // implementation(libs.androidx.navigation.compose)

  // Room
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // Retrofit / OkHttp / Moshi
  implementation(libs.retrofit)
  implementation(libs.converter.moshi)
  implementation(libs.okhttp)
  // implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  ksp(libs.moshi.kotlin.codegen)

  // Coil (Image Loading)
  implementation(libs.coil.compose)

  // Datastore
  // implementation(libs.androidx.datastore.preferences)

  // Biometric
  implementation("androidx.biometric:biometric:1.1.0")

  // CameraX
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)

  // Firebase Code & Google SDKs
  implementation("com.google.android.gms:play-services-auth:21.2.0")
  implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
  implementation("com.google.firebase:firebase-firestore")
  implementation("com.google.firebase:firebase-auth")
  implementation("com.google.firebase:firebase-storage")

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)

  // Testing
  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.runner)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}

