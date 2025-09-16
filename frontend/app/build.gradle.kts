import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    load(FileInputStream(rootProject.file("local.properties")))
}
val PICOVOICE_ACCESS_KEY = localProperties.getProperty("PICOVOICE_ACCESS_KEY")
    ?: error("PICOVOICE_ACCESS_KEY not found in local.properties")

val NAVER_CLIENT_ID: String =
    localProperties.getProperty("NAVER_CLIENT_ID")
        ?: error("NAVER_CLIENT_ID not found in local.properties")


android {
    namespace = "com.example.eeum"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.eeum"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["PICOVOICE_ACCESS_KEY"] = PICOVOICE_ACCESS_KEY
        manifestPlaceholders["NAVER_CLIENT_ID"] = NAVER_CLIENT_ID
        buildConfigField("String", "NAVER_CLIENT_ID", "\"$NAVER_CLIENT_ID\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    dataBinding {
        enable = true
    }
    viewBinding{
        enable = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // 음성 인식용
    implementation("ai.picovoice:picovoice-android:3.0.2")
    implementation("ai.picovoice:porcupine-android:3.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // XML 테마용 (필수)
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation(platform("androidx.compose:compose-bom:2025.06.00"))
    implementation("androidx.compose.material:material")

    // CameraX + MLKit (QR 스캔)
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // retrofit
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // okhttp3
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.0")

    // Glide
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")

    // yaml
    implementation("org.yaml:snakeyaml:2.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.6")

    // Naver Map SDK
    implementation("com.naver.maps:map-sdk:3.22.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // LiveData -> Compose state
    implementation("androidx.compose.runtime:runtime-livedata")

    // Compose에서 ViewModel/Lifecycle 사용
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // viewModelScope 등 ViewModel KTX
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("io.coil-kt:coil-compose:2.6.0")

}