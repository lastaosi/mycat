# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Koin
-keep class org.koin.** { *; }

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# SQLDelight
-keep class app.cash.sqldelight.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep class kotlinx.serialization.** { *; }

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Google Maps / Places
-keep class com.google.android.gms.** { *; }
-keep class com.google.android.libraries.places.** { *; }

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Domain 모델 보호
-keep class com.lastaosi.mycat.domain.model.** { *; }

# AlarmManager / BroadcastReceiver
-keep class com.lastaosi.mycat.worker.AlarmReceiver { *; }
-keep class com.lastaosi.mycat.worker.** { *; }

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**