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

# ── Room ──────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Entity class * { *; }

# ── Lombok ────────────────────────────────────────────────────────────────
-keep @lombok.* class *
-keepclassmembers @lombok.* class * { *; }

# ── APRS parser (herencia AVRS, usa reflection en algunos parsers) ─────────
-keep class ar.motorfar.app.aprs.** { *; }

# ── Concentus codec (JNI internals) ───────────────────────────────────────
-keep class concentus.** { *; }

# ── ESP32 flash lib ───────────────────────────────────────────────────────
-keep class io.github.dkaukov.** { *; }

# ── Stack traces legibles en Crashlytics / logcat ─────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile