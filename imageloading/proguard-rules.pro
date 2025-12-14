# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep ImageLoader public API
-keep class com.example.imageloading.ImageLoader { *; }
-keep class com.example.imageloading.RequestBuilder { *; }
-keep interface com.example.imageloading.ImageCallback { *; }
-keep interface com.example.imageloading.Transformation { *; }

