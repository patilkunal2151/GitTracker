# Git Tracker ProGuard/R8 Rules

# R8 Full Mode is enabled in gradle.properties, which is more aggressive.

# Gson: Keep models as they are serialized/deserialized
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.example.gittracker.data.model.** { *; }
-keep class com.example.gittracker.domain.model.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Retrofit
-dontwarn retrofit2.**
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.Converter$Factory { *; }
-keep class retrofit2.CallAdapter$Factory { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Hilt / Dagger
-dontwarn dagger.hilt.internal.aggregateddeps.AggregatedDeps
-keep class dagger.hilt.** { *; }
-keep class * {
    @dagger.hilt.android.EntryPoint *;
}

# Kotlin Serialization (if used in future or by dependencies)
-keepattributes *Annotation*, InnerClasses
-keepnames class kotlinx.serialization.json.JsonNames

# General Optimizations
-repackageclasses ''
-allowaccessmodification
