// This block tells Gradle where to look for plugins
pluginManagement {
    repositories {
        google()
        mavenCentral()
        // This is the official "store" where Gradle finds plugins like KSP
        gradlePluginPortal()
    }

    // --- THIS IS THE CORRECT, COMPATIBLE SET OF VERSIONS ---
    // These are the latest STABLE versions that are known to exist
    // and are compatible with each other.
    plugins {
        // Latest stable Android Gradle Plugin
        id("com.android.application") version "8.4.2" apply false
        // Latest stable Kotlin
        id("org.jetbrains.kotlin.android") version "2.0.0" apply false
        // The KSP version that matches Kotlin 2.0.0
        id("com.google.devtools.ksp") version "2.0.0-1.0.21" apply false

        // Standard, stable versions for other plugins
        id("com.google.gms.google-services") version "4.4.2" apply false
        id("androidx.navigation.safeargs.kotlin") version "2.7.7" apply false
    }
    // ---
}

// This block tells your app where to look for library dependencies
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FLOW"
include(":app")