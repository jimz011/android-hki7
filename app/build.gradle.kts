plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.jimz011apps.hki7"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jimz011apps.hki7"
        minSdk = 34
        targetSdk = 37
        versionCode = 8
        versionName = "1.0.0-beta.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            // R8 full-mode shrinking + obfuscation + resource shrinking. Keep rules that the
            // serialization models rely on live in proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Work around KT-83266: with AGP 9.2 + Kotlin 2.4 the Compose compiler plugin's release
// "group mapping" tasks fail (they try to resolve org.jetbrains.kotlin:compose-group-mapping at
// AGP's embedded 2.2.10, which isn't published). That mapping only helps deobfuscate Compose
// frames in profilers/Layout Inspector — it is not part of the shipped APK — so disabling it is
// safe. Revisit once the toolchain bug is fixed.
tasks.matching { it.name.contains("ComposeMapping") }.configureEach { enabled = false }

configurations.all {
    resolutionStrategy {
        // Newer libraries constrain kotlin-stdlib to 2.4.0, whose metadata AGP's built-in
        // Kotlin compiler (2.2.x, reads metadata <= 2.3.0) cannot parse. 2.3.0 is API-compatible
        // for everything on this classpath. Drop this once AGP's embedded Kotlin reaches 2.4.
        force("org.jetbrains.kotlin:kotlin-stdlib:2.3.0")
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.lottie.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.play.services.location)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
