// Top-level plugins
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // --- CHANGE THIS LINE ---
    id("com.google.devtools.ksp") // Use the full ID instead of "kotlin-ksp"
    // ---
    id("androidx.navigation.safeargs.kotlin") // For Navigation Safe Args
    id("com.google.gms.google-services") // For Firebase
}

android {
    namespace = "com.example.flow"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.flow"
        minSdk = 26 // Min SDK 26 is needed for Biometrics
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Add this for KSP (Room)
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    // Enable View Binding (if you use it)
    buildFeatures {
        viewBinding = true
    }
}

// Define versions in one place
val lifecycleVersion = "2.8.3"
val roomVersion = "2.6.1"
val navVersion = "2.7.7"

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // --- Lifecycle (ViewModel, LiveData, Transformations) ---
    // FIX: ADDED lifecycle-livedata-ktx
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion") // <-- THIS LINE IS THE FIX
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    // ---

    // --- Navigation Component ---
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")
    // ---

    // --- Room Database ---
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    // ---

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // For Firebase .await()
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    // ---

    // --- Firebase ---
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    // ---

    // --- Biometrics ---
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    // ---

    // --- Image Loading (Glide) ---
    // Added this to support loading receipt images
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // ---

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}