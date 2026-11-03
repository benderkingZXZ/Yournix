plugins {
    id("com.android.application")
}

android {
    namespace = "com.yournix.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yournix.app"
        minSdk = 26
        targetSdk = 34
        // На GitHub номер сборки растёт сам, поэтому новый APK ставится поверх старого.
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.0"
    }

    // Постоянный ключ подписи: GitHub Actions собирает keystore/yournix.p12 из keystore/yournix.p12.b64,
    // чтобы обновления ставились поверх старой версии. Если файла нет, берётся отладочный ключ.
    val keystoreFile = rootProject.file("keystore/yournix.p12")

    signingConfigs {
        create("yournix") {
            storeFile = keystoreFile
            storePassword = "yournix"
            keyAlias = "yournix"
            keyPassword = "yournix"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keystoreFile.exists()) {
                signingConfigs.getByName("yournix")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
