# Mahjong Slash ProGuard Rules

# Keep source info for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Compose: keep Composable metadata
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Navigation Compose: keep argument types
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable

# DataStore: keep preferences keys
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences$Key { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep game model classes (used reflectively by data class copy())
-keep class com.mahjongslash.game.engine.GameState { *; }
-keep class com.mahjongslash.game.model.TileType { *; }
