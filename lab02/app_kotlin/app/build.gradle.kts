plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.danielgregorini.sensorappdaniel"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.danielgregorini.sensorappdaniel"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }
}

dependencies {
    // --- ViewModel / Services extras que não estão no libs.versions.toml ---
    // Para usar viewModels(), etc
    implementation("androidx.activity:activity-ktx:1.8.0")

    // Para collectAsStateWithLifecycle em Compose
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    // Para Service com Lifecycle (SensorService)
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")

    // --- Dependências gerenciadas pelo version catalog (libs.*) ---

    // Core e lifecycle base
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Activity + Compose
    implementation(libs.androidx.activity.compose)

    // Compose BOM + UI + Material3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
