package com.dressup.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Everything a `:feature:*` module needs: Android library + Compose + the shared core modules.
 *
 * Features may depend on `:core:*` only, never on another feature (GUIDELINES.md §2.2).
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("dressup.android.library")
        pluginManager.apply("dressup.android.compose")

        dependencies {
            add("implementation", project(":core:model"))
            add("implementation", project(":core:data"))
            add("implementation", project(":core:ui"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.library("kotlinx-coroutines-core"))
            add("testImplementation", libs.library("kotlinx-coroutines-test"))
        }
    }
}
