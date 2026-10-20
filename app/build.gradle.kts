plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.playcube.urlrouter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.playcube.urlrouter"
        minSdk = 26
        targetSdk = 28 // Keeps generic web-link chooser behavior on Android TV; not for Play Store release.
        versionCode = 2
        versionName = "1.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
