pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "dress-up"

include(":app")
include(":core:model")
include(":core:data")
include(":core:ui")
include(":feature:wardrobe")
include(":feature:garment")
include(":feature:categories")
include(":feature:settings")
include(":feature:profiles")
include(":feature:outfits")
include(":feature:diary")
include(":feature:backup")
// Optional adapters, each behind one switch in gradle.properties (GUIDELINES.md §1 point 2, §2.2).
if (providers.gradleProperty("dressup.cutout").orNull != "false") include(":adapter:cutout-mlkit")
