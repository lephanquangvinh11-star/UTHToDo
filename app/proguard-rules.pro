# Keep data models & Firebase serialization classes
-keep class com.uthtodo.dpgh.data.model.** { *; }
-keepclassmembers class com.uthtodo.dpgh.data.model.** { *; }

# OkHttp & org.json warnings
-dontwarn okhttp3.**
-dontwarn okio.**
