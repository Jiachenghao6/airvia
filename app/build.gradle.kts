plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    // Kotlin package stays com.opus.airvia; the installed app id is our own,
    // so this build can sit next to the upstream APK (different signing key).
    namespace = "com.opus.airvia"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.jiachenghao6.airvia"
        minSdk = 29
        targetSdk = 34
        versionCode = 9
        versionName = "1.2.4-fork.1"
    }

    buildTypes {
        release {
            // No release keystore yet: sign with the debug key so the APK installs.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Same versions upstream's scripts/download_deps.py pins.
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.2")
    // cast-framework drags in an old fragment; ActivityResult APIs need >= 1.3.0.
    implementation("androidx.fragment:fragment-ktx:1.8.3")
    implementation("com.google.android.gms:play-services-cast-framework:21.4.0")
}
