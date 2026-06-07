# Gson rules
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.example.gittracker.data.model.** { *; }

# Room rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Retrofit rules
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Hilt rules
-keep class dagger.hilt.** { *; }
-keep class * {
    @dagger.hilt.android.EntryPoint *;
}
