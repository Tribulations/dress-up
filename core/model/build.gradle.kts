plugins {
    alias(libs.plugins.dressup.android.library)
    alias(libs.plugins.kotlin.serialization)
}

// Plain Kotlin data types shared by every other module (GUIDELINES.md §2.1). No Android or Compose code here.

dependencies {
    // Only the annotations: some types are @Serializable, so an editor can keep its draft as one value (core:ui SavedDraft).
    api(libs.kotlinx.serialization.core)
}
