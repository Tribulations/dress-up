plugins {
    alias(libs.plugins.dressup.android.feature)
}

// Export and import of whole profiles as a zip of profile.yaml and images/ (ARCHITECTURE.md §7.8).

dependencies {
    implementation(libs.androidx.activity.compose) // the system's save and open file pickers
}
