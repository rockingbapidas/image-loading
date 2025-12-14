// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Root build.gradle.kts
plugins {
    // You need to declare the plugins here with versions, but NOT apply them
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}
