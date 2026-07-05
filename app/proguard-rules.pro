# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- DepthLens Proguard Keep Rules ---

# Keep Room databases and entities
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.data.model.** { *; }

# Keep Moshi and Retrofit network requests, responses, and updater models
-keep class com.example.data.network.** { *; }
-keep class com.example.ui.screens.GitHubRelease { *; }

# Keep Moshi annotations and generated code
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Prevent Retrofit from warning on missing OkHttp platforms or optional extensions
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Fast R8 tree-shaking (shrinking) without slow optimization or obfuscation passes
-dontobfuscate
-dontoptimize

