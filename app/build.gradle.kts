import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)

    // ✅ 新增：啟用 Kotlin 序列化功能（讓 @Serializable、Json 可用）
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}

// 🔑 從 local.properties 讀取 API KEY
val localProperties = rootProject.file("local.properties")
val properties = Properties().apply {
    if (localProperties.exists()) {
        load(localProperties.inputStream())
    }
}
val openAiKey: String = properties.getProperty("OPENAI_API_KEY") ?: ""

android {
    namespace = "tw.edu.pu.csim.refrigerator"
    compileSdk = 35

    defaultConfig {
        applicationId = "tw.edu.pu.csim.refrigerator"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 🔑 將 API KEY 注入 BuildConfig
        buildConfigField("String", "OPENAI_API_KEY", "\"$openAiKey\"")
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // ✅ 使用 Compose BOM 統一版本
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.compose.ui:ui-text:1.6.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation:1.5.4")

    implementation("androidx.compose.runtime:runtime-saveable")
    implementation("androidx.fragment:fragment-ktx:1.8.2") // ⬅ 提供 activityViewModels()
    implementation("com.google.firebase:firebase-storage-ktx")

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.firestore.ktx) {
        exclude(group = "com.google.firebase", module = "firebase-common")
    }
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore)

    // UI 與工具
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // 測試
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // OpenAI API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.36.0")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.31.5-beta")

    // ✅ 新增：Kotlinx Serialization JSON 函式庫（支援 @Serializable 與 Json）
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-storage:21.0.0")




}
