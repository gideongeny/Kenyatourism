# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\mukht\AppData\Local\Android\Sdk\tools\proguard\proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any custom keep rules here that are specific to your project.

# AdMob rules
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }
-keep class com.gideongeng.kenyatourism.ads.AdsManager { *; }

# Coil rules
-keep class coil.** { *; }
-keep interface coil.** { *; }

# JSON/JSONObject rules (used for Weather)
-keep class org.json.** { *; }

# Coroutines rules
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext$HandlerPost {
    *** run();
}

# Keep Compose rules
-keep class androidx.compose.ui.platform.** { *; }
-keep interface androidx.compose.ui.platform.** { *; }
