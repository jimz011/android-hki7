plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.jimz011apps.hki7.core"
    compileSdk = 37

    defaultConfig {
        // Lower than the phone app's 31 so the Wear OS module, which reaches back to Wear OS 3,
        // can consume the same models.
        minSdk = 30
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // @Immutable on the entity models is a Compose hint read by whichever module compiles the
    // composables. Only the annotation is needed here, so this module does not apply the Compose
    // compiler plugin and pulls in no Compose UI.
    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
