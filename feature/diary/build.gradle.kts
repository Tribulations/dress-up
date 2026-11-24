plugins {
    alias(libs.plugins.dressup.android.feature)
    alias(libs.plugins.kotlin.serialization) // the editor's draft (core:ui SavedDraft)
}

// The wear diary: a list of what was worn when, an entry's page and the editor (ARCHITECTURE.md §7.7).

dependencies {
    implementation(libs.androidx.activity.compose) // BackHandler in the pickers
}
