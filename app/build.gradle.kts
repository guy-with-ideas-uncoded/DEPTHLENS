plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  id("com.google.gms.google-services")
}

tasks.register("repairHomeScreen") {
    doLast {
        val homeScreenFile = file("src/main/java/com/example/ui/screens/HomeScreen.kt")
        if (homeScreenFile.exists()) {
            var text = homeScreenFile.readText()
            val nl = if (text.contains("\r\n")) "\r\n" else "\n"
            
            // 1. Strip comment block DEAD_CODE
            text = text.replace(Regex("/\\* DEAD_CODE.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            
            // 2. Remove the exact 5 mismatched braces block specifically (both Unix and CRLF)
            val bracesUnix = "                                                                                               }\n                                             }\n                                         }\n                                     }\n                                 }"
            val bracesCRLF = "                                                                                               }\r\n                                             }\r\n                                         }\r\n                                     }\r\n                                 }"
            text = text.replace(bracesUnix, "")
            text = text.replace(bracesCRLF, "")
            
            // 3. Update the fallback container with Card wrapper (both Unix and CRLF)
            val findUnix = "                                 } else {\n                                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp),\n                                         colors = CardDefaults.cardColors(containerColor = Color(0xFF141420)),\n                                         border = BorderStroke(1.dp, Color(0x0FFFFFFF)),\n                                         modifier = Modifier.fillMaxWidth()\n                                     ) {"
            val replUnix = "                                 } else {\n                                     Card(\n                                         shape = RoundedCornerShape(topStart = 3.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp),\n                                         colors = CardDefaults.cardColors(containerColor = Color(0xFF141420)),\n                                         border = BorderStroke(1.dp, Color(0x0FFFFFFF)),\n                                         modifier = Modifier.fillMaxWidth()\n                                     ) {"
            text = text.replace(findUnix, replUnix)
            
            val findCRLF = "                                 } else {\r\n                                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp),\r\n                                         colors = CardDefaults.cardColors(containerColor = Color(0xFF141420)),\r\n                                         border = BorderStroke(1.dp, Color(0x0FFFFFFF)),\r\n                                         modifier = Modifier.fillMaxWidth()\r\n                                     ) {"
            val replCRLF = "                                 } else {\r\n                                     Card(\r\n                                         shape = RoundedCornerShape(topStart = 3.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp),\r\n                                         colors = CardDefaults.cardColors(containerColor = Color(0xFF141420)),\r\n                                         border = BorderStroke(1.dp, Color(0x0FFFFFFF)),\r\n                                         modifier = Modifier.fillMaxWidth()\r\n                                     ) {"
            text = text.replace(findCRLF, replCRLF)

            // 4. Repair the three corrupted joined sections space-independently
            val pat1 = Regex("lineHeight = 16\\.sp\\s+\\)\\s+}\\s+}\\s+else\\s*\\{\\s+Spacer\\(modifier = Modifier\\.weight\\(1f\\)\\)")
            val pat2 = Regex("modifier = Modifier\\.size\\(10\\.dp\\)\\s+\\)\\s+}\\s+}\\s+}\\s+else\\s*\\{\\s+val parsedResponse = remember\\(message\\.text\\)")
            val pat3 = Regex("fontWeight = FontWeight\\.Bold\\s+\\)\\s+}\\s+else\\s*\\{\\s+if \\(rawText\\.isEmpty\\(\\)\\)")
            val pat4 = Regex("modifier = Modifier\\.weight\\(1f\\)\\s+\\)\\s+\\}\\s+else\\s*\\{\\s+Card\\(")
            
            println("PAT1 MATCHES: ${pat1.containsMatchIn(text)}")
            println("PAT2 MATCHES: ${pat2.containsMatchIn(text)}")
            println("PAT3 MATCHES: ${pat3.containsMatchIn(text)}")
            println("PAT4 MATCHES: ${pat4.containsMatchIn(text)}")

            text = text.replace(
                pat1,
                "lineHeight = 16.sp${nl}                                        )${nl}                                    }${nl}                                }${nl}                            } else {${nl}                                Spacer(modifier = Modifier.weight(1f))"
            )

            text = text.replace(
                pat2,
                "modifier = Modifier.size(10.dp)${nl}                                        )${nl}                                    }${nl}                                }${nl}                            } else {${nl}                            val parsedResponse = remember(message.text)"
            )

            text = text.replace(
                pat4,
                "modifier = Modifier.weight(1f)${nl}                                                              )${nl}                                                          }${nl}                                                      }${nl}                                                  }${nl}                                              }${nl}                                                }${nl}                                            }${nl}                                        }${nl}                                    }${nl}                                } else {${nl}                                    Card("
            )

            text = text.replace(
                pat3,
                "fontWeight = FontWeight.Bold${nl}                                )${nl}                            } else {${nl}                            if (rawText.isEmpty())"
            )
            
            text = text.replace(
                "// Helper to parse archived JSON — kept for any legacy calls",
                "}${nl}${nl}// Helper to parse archived JSON — kept for any legacy calls"
            )
            
            homeScreenFile.writeText(text)
        }
    }
}

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.depthlens.uqmzkx"
    minSdk = 24
    targetSdk = 36
    versionCode = 5892
    versionName = "5.8.9.x-1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
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
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
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
            if (content.contains("AIzaSyMockKeyForCompilationOnly123456") || content.contains("PLACEHOLDER_FIREBASE_API_KEY") || content.trim().isEmpty()) {
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
                      "package_name": "com.aistudio.depthlens.uqmzkx"
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
            println("Dynamically resolved /app/google-services.json contents")
        }
    }
}

// Ensure processGoogleServices depends on generateFirebaseConfigs
tasks.matching { it.name.contains("GoogleServices") }.configureEach {
    dependsOn("generateFirebaseConfigs")
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation("com.google.firebase:firebase-analytics")
  implementation("com.google.firebase:firebase-auth")
  implementation("com.google.firebase:firebase-firestore")
  implementation("com.google.android.gms:play-services-auth:21.2.0")
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.text.google.fonts)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation("androidx.navigation:navigation-compose")
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}