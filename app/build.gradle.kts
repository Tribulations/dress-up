import java.util.Properties

plugins {
    alias(libs.plugins.dressup.android.application)
    alias(libs.plugins.dressup.android.compose)
}

android {
    namespace = "com.dressup.app"

    defaultConfig {
        applicationId = "com.dressup.app"
        versionCode = 6
        versionName = "0.2.2"
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

// Optional adapters (GUIDELINES.md §1 point 2, §2.2), switched in gradle.properties. Each switch picks the source folder that wires
// the adapter in, or the one that wires in "not available", so the app builds either way.
val cutoutEnabled = providers.gradleProperty("dressup.cutout").orNull != "false"
android.sourceSets.getByName("main").kotlin.srcDir(if (cutoutEnabled) "src/cutout/kotlin" else "src/noCutout/kotlin")

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
    implementation(project(":feature:outfits"))
    implementation(project(":feature:diary"))
    implementation(project(":feature:backup"))
    if (cutoutEnabled) implementation(project(":adapter:cutout-mlkit"))
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
