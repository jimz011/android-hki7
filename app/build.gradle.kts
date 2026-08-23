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
        minSdk = 31
        targetSdk = 37
        // 8 was consumed by an upload that was never released (Play reserves version codes
        // permanently, even for bundles left inactive).
        versionCode = 33
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        debug {
            // Installs alongside a release build instead of colliding with it. The signatures
            // differ, so without this a debug build cannot be installed on a device that already
            // has HKI 7 — including when the release copy lives in a Samsung Secure Folder, where
            // adb cannot reach it to uninstall.
            //
            // Safe for sign-in: the Home Assistant OAuth client_id and redirect_uri are fixed
            // constants, not derived from the application id. Google Drive sign-in is already
            // unavailable to debug builds (different signing certificate), so nothing regresses.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
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
    bundle {
        // Keep every supported translation in the installed app so switching languages from the
        // in-app picker never depends on Play downloading a language split.
        language {
            enableSplit = false
        }
    }
}

configurations.all {
    resolutionStrategy {
        // Newer libraries constrain kotlin-stdlib to 2.4.0, whose metadata AGP's built-in
        // Kotlin compiler (2.2.x, reads metadata <= 2.3.0) cannot parse. 2.3.0 is API-compatible
        // for everything on this classpath. Drop this once AGP's embedded Kotlin reaches 2.4.
        force("org.jetbrains.kotlin:kotlin-stdlib:2.3.0")
        // The Compose "group mapping" tasks request this at AGP's embedded Kotlin version
        // (2.2.10), which was never published — the artifact only exists from 2.3.0 onward, so
        // resolution fails and the release mapping file never gets written. Pin it to the Kotlin
        // version this project actually compiles with; without it :app:packageReleaseBundle fails
        // with "Metadata file .../mapping/release/mapping.txt does not exist".
        force("org.jetbrains.kotlin:compose-group-mapping:${libs.versions.kotlin.get()}")
    }
}

dependencies {
    // Home Assistant models and quick-action semantics, shared with the Wear OS app so the
    // same shortcut cannot mean two different things depending on the device.
    implementation(project(":core"))
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
    // Phone-to-watch handover: the watch is set up from here rather than signing in itself.
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.play.app.update.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    // Android Auto. The templates in androidx.car.app:app are the only UI a projected car head
    // unit will draw, so none of the Compose dashboard applies there. app-projected carries the
    // projected-specific host bits, and has to be paired with app explicitly: its POM declares
    // app at runtime scope, so on its own the templates are missing from the compile classpath.
    implementation(libs.androidx.car.app)
    implementation(libs.androidx.car.app.projected)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Compose UI tests, so gesture behaviour can be driven and asserted directly instead of being
    // reasoned about — the tab-swipe rules are exactly the kind of thing that reads correct and
    // behaves otherwise.
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
