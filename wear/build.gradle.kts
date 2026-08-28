plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.jimz011apps.hki7.wear"
    compileSdk = 37

    defaultConfig {
        // Same applicationId as the phone app: Play then delivers this bundle to watches and the
        // phone bundle to phones, under one listing, using the watch uses-feature below.
        applicationId = "com.jimz011apps.hki7"
        // Wear OS 3. Lower than the phone app's 31 on purpose â€” watches sit on older API levels
        // far longer than phones do.
        minSdk = 30
        targetSdk = 37
        // Must differ from the phone app's. Convention here: phone versionCode + 1000, so the two
        // stay legible next to each other in Play Console.
        versionCode = 1033
        versionName = "1.2.0"
    }
    buildTypes {
        debug {
            // Must match :app's suffix â€” the Data Layer only pairs a phone and watch app that
            // share an application id, so a suffixed phone build needs a suffixed watch build.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    buildFeatures {
        compose = true
        // The mobile_app registration reports the watch app's version to Home Assistant.
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

configurations.all {
    resolutionStrategy {
        // Same pin as :app â€” AGP's embedded Kotlin cannot read 2.4.0 metadata.
        force("org.jetbrains.kotlin:kotlin-stdlib:2.3.0")
        force("org.jetbrains.kotlin:compose-group-mapping:${libs.versions.kotlin.get()}")
    }
}

dependencies {
    // The Home Assistant models and quick-action semantics the phone uses. Deliberately the only
    // shared code: the phone's Ktor client, websocket session and Compose dashboard stay off the
    // watch, where they would cost megabytes and battery for no benefit.
    implementation(project(":core"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.navigation)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    // Periodic battery reporting, batched with the system's other deferred work.
    implementation(libs.androidx.work.runtime.ktx)

    // Phone-to-watch credential and favourites handover.
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.wear.phone.interactions)
    implementation(libs.okhttp)

    // A tile is one swipe from the watch face, with no app launch â€” the surface a watch user
    // actually reaches for. Complications put a single entity on the face itself.
    implementation(libs.androidx.wear.tiles)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.androidx.protolayout)
    implementation(libs.androidx.protolayout.expression)
    implementation(libs.androidx.protolayout.material3)
    implementation(libs.androidx.watchface.complications.data.source.ktx)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
}
