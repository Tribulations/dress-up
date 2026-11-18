plugins {
    alias(libs.plugins.dressup.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

room {
    // Exported schemas are committed, so every migration can be checked against them (GUIDELINES.md §1: never drop data).
    schemaDirectory("$projectDir/schemas")
}

// The wardrobe's storage: the Room database, the photo files and the repositories in front of them (ARCHITECTURE.md §4).
dependencies {
    api(project(":core:model"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.kotlinx.coroutines.test)
}
