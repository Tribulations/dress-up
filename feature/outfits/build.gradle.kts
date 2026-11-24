plugins {
    alias(libs.plugins.dressup.android.feature)
    alias(libs.plugins.kotlin.serialization) // the editor's draft (core:ui SavedDraft)
}

// Outfits: garments put together on a collage board, picked from a gallery of all garment pictures (ARCHITECTURE.md §7.6).

dependencies {
    implementation(libs.androidx.activity.compose) // BackHandler in the gallery
}
