import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "razek.fuchoquiniela"
    compileSdk = 35

    defaultConfig {
        applicationId = "razek.fuchoquiniela"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Same backend as the iOS app.
        buildConfigField("String", "API_BASE_URL", "\"https://www.fuchoquiniela.com/api/v1\"")
    }

    buildTypes {
        debug {
            // Debug builds can talk to the local fuchito Docker setup: put
            // fucho.apiBaseUrl=http://10.0.2.2:50800/api/v1 in local.properties
            // (10.0.2.2 is the emulator's alias for the host's localhost).
            val localUrl = rootProject.file("local.properties").takeIf { it.exists() }
                ?.let { file -> Properties().apply { file.inputStream().use { load(it) } } }
                ?.getProperty("fucho.apiBaseUrl")
            if (localUrl != null) buildConfigField("String", "API_BASE_URL", "\"$localUrl\"")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        buildConfig = true
    }
    androidResources {
        // Spanish is the default (values/), English the translation (values-en/). This lists both
        // in the system's per-app language settings on Android 13+.
        generateLocaleConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.browser)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
