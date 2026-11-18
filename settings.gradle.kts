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
