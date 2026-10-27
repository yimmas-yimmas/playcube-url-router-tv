plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.playcube.sender"
    compileSdk = 35
    // Stable APK signing when GitHub Actions provides the secure keystore.
    // Local developers may still build with the normal Android debug key.
    val signingKeystoreFile = System.getenv("TVBOX_KEYSTORE_FILE")
    val signingKeystorePassword = System.getenv("TVBOX_SIGNING_PASSWORD")
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
        applicationId = "com.playcube.sender"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.2.2"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
}
