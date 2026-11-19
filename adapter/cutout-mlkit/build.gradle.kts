plugins {
    alias(libs.plugins.dressup.android.library)
}

// Optional adapter: removes the background from garment photos with ML Kit subject segmentation. It runs on the
// device; Google Play services downloads the model once. Switched off with dressup.cutout=false (gradle.properties).
android {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro") // applied when the app is minified
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(libs.mlkit.subject.segmentation)
    implementation(libs.play.services.base)
    implementation(libs.kotlinx.coroutines.core)
}
