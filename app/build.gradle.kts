import java.util.Properties

plugins {
    alias(libs.plugins.dressup.android.application)
    alias(libs.plugins.dressup.android.compose)
}

android {
    namespace = "com.dressup.app"

    defaultConfig {
        applicationId = "com.dressup.app"
        versionCode = 1
        versionName = "0.1.0"
    }

    // One dedicated release key, kept outside the repo and referenced from local.properties
    // (signing.storeFile/storePassword/keyAlias/keyPassword). Every published release must use it: Android refuses to
    // update an app whose signature changed. Without these properties the release build falls back to the debug key,
    // with a warning, so a fresh checkout still builds, but never publish that APK (AGENTS.md "Releases").
    val releaseSigning = localProperties().takeIf { it.getProperty("signing.storeFile") != null }
    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = file(releaseSigning.getProperty("signing.storeFile"))
                storePassword = releaseSigning.getProperty("signing.storePassword")
                keyAlias = releaseSigning.getProperty("signing.keyAlias")
                keyPassword = releaseSigning.getProperty("signing.keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = if (releaseSigning != null) {
                signingConfigs.getByName("release")
            } else {
                logger.warn("No signing.* in local.properties: release APK signed with the DEBUG key. Do not publish it.")
                signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug" // debug and release can be installed side by side
        }
    }
}

// APKs come out as dress-up-<version>-<buildType>.apk, so the files attached to a GitHub release say what they are.
base {
    archivesName = "dress-up-${android.defaultConfig.versionName}"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:wardrobe"))
    implementation(project(":feature:garment"))
    implementation(project(":feature:categories"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:profiles"))
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
}

fun localProperties(): Properties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
