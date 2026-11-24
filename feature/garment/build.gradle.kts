plugins {
    alias(libs.plugins.dressup.android.feature)
    alias(libs.plugins.kotlin.serialization) // the editor's draft (core:ui SavedDraft)
}

// One garment: its page (photo, details, rating, like) and the editor with the in-app camera (ARCHITECTURE.md §7.1).
dependencies {
    implementation(libs.androidx.activity.compose) // camera and photo-picker launchers
}
