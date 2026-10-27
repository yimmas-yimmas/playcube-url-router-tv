plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.playcube.urlrouter"
    compileSdk = 35
    // Stable APK signing when GitHub Actions provides the secure keystore.
    // Local developers may still build with the normal Android debug key.
    val signingKeystoreFile = System.getenv("TVBOX_KEYSTORE_FILE")
    val signingKeystorePassword = System.getenv("TVBOX_SIGNING_PASSWORD")?.trim()
    if (!signingKeystoreFile.isNullOrBlank() && !signingKeystorePassword.isNullOrBlank()) {
        signingConfigs {
            create("tvboxPersistent") {
                storeFile = file(signingKeystoreFile)
                storePassword = signingKeystorePassword
                keyAlias = "tvbox-signing"
                keyPassword = signingKeystorePassword
                storeType = "pkcs12"
            }
        }
        buildTypes {
            getByName("debug") {
                signingConfig = signingConfigs.getByName("tvboxPersistent")
            }
        }
    }


    defaultConfig {
        applicationId = "com.playcube.urlrouter"
        minSdk = 26
        targetSdk = 28 // Keeps generic web-link chooser behavior on Android TV; not for Play Store release.
        versionCode = 8
        versionName = "1.4.3"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.google.zxing:core:3.5.3")
}
