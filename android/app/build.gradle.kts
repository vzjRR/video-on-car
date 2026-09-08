import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Optional local-only quickstart seed: android/app/seed.properties is
// gitignored and never committed (see docs/PLATFORM_LIMITATIONS.md /
// Phase 3 — credentials must never land in source control). When present,
// its values are baked into this build only, so the app can seed one
// provider on first launch instead of requiring manual Settings entry.
val seedProperties = Properties().apply {
    val seedFile = rootProject.file("app/seed.properties")
    if (seedFile.exists()) {
        seedFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.iptvcar.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.iptvcar.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "SEED_XTREAM_URL", "\"${seedProperties.getProperty("xtreamUrl", "")}\"")
        buildConfigField("String", "SEED_XTREAM_USER", "\"${seedProperties.getProperty("xtreamUser", "")}\"")
        buildConfigField("String", "SEED_XTREAM_PASS", "\"${seedProperties.getProperty("xtreamPass", "")}\"")
        buildConfigField("String", "SEED_DISPLAY_NAME", "\"${seedProperties.getProperty("displayName", "")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":core"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Native playback: Media3/ExoPlayer with HLS + built-in codecs for MP4/MPEG-TS.
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")

    // Android for Cars App Library: officially supported browsing templates
    // for Android Auto / Android Automotive OS.
    implementation("androidx.car.app:app:1.4.0")
    implementation("androidx.car.app:app-projected:1.4.0")

    // Encrypted credential storage backed by Android Keystore.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}
