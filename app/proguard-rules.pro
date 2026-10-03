# Optimization & Reflection Attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Room Database & DAOs
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.Entity
-dontwarn androidx.room.paging.**

# Firebase Auth, Firestore, App Check
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# AndroidX Credentials & Google ID Token Credential
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**
-dontwarn com.google.android.libraries.identity.googleid.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Domain & Data Models (prevent reflection / serialization breakages)
-keep class com.example.domain.model.** { *; }
-keep class com.example.data.local.room.** { *; }
-keep class com.example.data.local.datastore.** { *; }
-keep class com.example.domain.workout.** { *; }
-keep class com.example.service.update.** { *; }
