plugins {
    alias(libs.plugins.dressup.android.library)
    alias(libs.plugins.dressup.android.compose)
}

// The design system: theme, photo tile, rating bar and other components shared by the features (GUIDELINES.md §2.1).
dependencies {
    api(project(":core:model"))
    api(libs.coil.compose)
    implementation(libs.androidx.activity.compose) // BackHandler in the garment gallery
}
