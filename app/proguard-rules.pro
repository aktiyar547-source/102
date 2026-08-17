# ============================================================================
# MECRC R8 / ProGuard keep rules
# Release builds are minified + resource-shrunk. These rules protect the
# reflection / code-generation surfaces from being stripped or renamed.
# ============================================================================

# --- Kotlin metadata & coroutines ---
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod
-dontwarn kotlinx.coroutines.**

# --- kotlinx.serialization ---
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * { @kotlinx.serialization.Serializable <fields>; }
-keep,includedescriptorclasses class com.middleeastcontainer.**$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.middleeastcontainer.** { *; }
-dontnote kotlinx.serialization.**

# --- Retrofit / OkHttp ---
# Retrofit reflects on interface methods + generic Response types.
-keep,allowobfuscation interface com.middleeastcontainer.data.network.MecrcApi { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class kotlin.coroutines.Continuation
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Network DTOs (kept for any future serialized bodies).
-keep class com.middleeastcontainer.data.network.dto.** { *; }

# --- Room ---
# Room generates *_Impl classes; keep entities' members (referenced by generated code).
-keep class com.middleeastcontainer.data.database.entity.** { *; }
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.**

# --- Hilt / Dagger ---
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }

# --- WorkManager (Hilt workers instantiated via factory + reflection) ---
-keep class com.middleeastcontainer.data.sync.** { *; }

# --- ML Kit text recognition ---
-dontwarn com.google.mlkit.**
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# --- Timber ---
-dontwarn org.jetbrains.annotations.**

# --- Domain enums referenced by wire strings (defensive) ---
-keepclassmembers enum com.middleeastcontainer.domain.model.** { *; }
