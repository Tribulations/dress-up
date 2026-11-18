package com.dressup.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project

/** Java/Kotlin bytecode level used by every module. */
internal val JAVA_VERSION = JavaVersion.VERSION_17

/** Settings shared by the application module and all Android library modules. */
internal fun Project.configureAndroidCommon(android: CommonExtension) {
    android.compileSdk = libs.versionInt("compileSdk")
    android.defaultConfig.minSdk = libs.versionInt("minSdk")
    android.compileOptions.sourceCompatibility = JAVA_VERSION
    android.compileOptions.targetCompatibility = JAVA_VERSION
}
