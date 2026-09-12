plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

fun localKey(name: String): String {
    val file = rootProject.file("local.properties")
    if (!file.exists()) return ""
    return file.readLines()
        .firstOrNull { it.startsWith("$name=") }
        ?.substringAfter("=")
        ?.trim()
        ?.trim('"')
        ?: ""
}

android {
    namespace = "com.eatda.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.eatda.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "EATDA_TTS_KEY",    "\"${localKey("eatda-android-tts-key")}\"")
        buildConfigField("String", "EATDA_VISION_KEY", "\"${localKey("eatda-android-vision-key")}\"")
    }

    buildTypes {
        release {
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)

    debugImplementation(libs.androidx.ui.tooling)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)

    implementation(libs.mlkit.image.labeling)
    implementation(libs.mlkit.ocr)
    implementation(libs.mlkit.ocr.korean)

//    implementation(libs.coil.compose)
//    implementation(libs.coil.network.okhttp)
}