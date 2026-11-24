plugins {
    alias(libs.plugins.dressup.android.library)
    alias(libs.plugins.dressup.android.compose)
    alias(libs.plugins.kotlin.serialization) // only for SavedDraftTest
}

// The design system: theme, photo tile, rating bar and other components shared by the features (GUIDELINES.md §2.1).
dependencies {
    api(project(":core:model"))
    api(libs.coil.compose)
    implementation(libs.androidx.activity.compose) // BackHandler in the garment gallery
    api(libs.androidx.lifecycle.viewmodel.savedstate) // SavedDraft
    implementation(libs.kotlinx.serialization.json)
}
