// Top-level build file where you can add configuration options common to all sub-projects.
// Every plugin a module uses is declared here with `apply false` so its version is resolved once:
// applying the same plugin with a version in two modules fails once it is already on the classpath.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
}
