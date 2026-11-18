plugins {
    `kotlin-dsl`
}

java {
    // Matches this machine's installed JDK (21). The modules' own bytecode target is pinned to 17 in AndroidCommon.kt.
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    // compileOnly: the real plugin versions are put on the classpath by the root build.
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.dressup.android.application.get().pluginId
            implementationClass = "com.dressup.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.dressup.android.library.get().pluginId
            implementationClass = "com.dressup.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.dressup.android.compose.get().pluginId
            implementationClass = "com.dressup.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = libs.plugins.dressup.android.feature.get().pluginId
            implementationClass = "com.dressup.buildlogic.AndroidFeatureConventionPlugin"
        }
    }
}
