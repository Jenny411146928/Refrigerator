import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

// 🔑 讀取 local.properties 的 API KEY
val localProperties = File(rootDir, "local.properties")
val properties = Properties().apply {
    if (localProperties.exists()) {
        load(FileInputStream(localProperties))
    }
}
val openAiKey = properties["OPENAI_API_KEY"] as String? ?: ""

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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.runtime:runtime-saveable:1.5.4")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.23")
    implementation("androidx.compose.material:material-icons-extended:<compose_version>")

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
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)

    // 測試
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // OpenAI API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
}
