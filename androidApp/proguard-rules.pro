-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*,InnerClasses
-renamesourcefileattribute SourceFile
-dontwarn org.slf4j.**

# Room 3.x
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Koin 4.x
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Ktor 3.x
-keep class io.ktor.client.** { *; }
-keep class io.ktor.http.** { *; }
-dontwarn io.ktor.**

# kotlinx.serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * { *** Companion; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}

# kotlinx-datetime
-keep class kotlinx.datetime.** { *; }

# multiplatform-settings
-keep class com.russhwolf.settings.** { *; }
-dontwarn com.russhwolf.settings.**

# Alarmee
-keep class com.tweener.alarmee.** { *; }
-dontwarn com.tweener.alarmee.**
