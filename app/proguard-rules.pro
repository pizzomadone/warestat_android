# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# iText PDF
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Kotlin Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
