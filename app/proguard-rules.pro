# Bruce Controller ProGuard Rules

# Keep BLE related
-keep class com.bruce.controller.ble.** { *; }

# Keep data models
-keep class com.bruce.controller.data.model.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# NimBLE / BLE
-dontwarn com.bruce.controller.ble.**
