plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.amlet.callblocker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.amlet.callblocker"
        minSdk = 29
        targetSdk = 34
        versionCode = 3
        versionName = "1.3.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // --- Jetpack Compose BOM ---
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // --- Activity + Navigation ---
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // --- Room (ORM / SQLite) ---
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // --- ViewModel + Lifecycle ---
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // --- Serializzazione JSON (backup) ---
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // --- DataStore (preferenze persistenti) ---
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // --- WorkManager (backup automatico periodico) ---
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // --- DocumentFile (accesso cartelle SAF nel Worker) ---
    implementation("androidx.documentfile:documentfile:1.0.1")

    implementation("androidx.appcompat:appcompat:1.6.1")
}
