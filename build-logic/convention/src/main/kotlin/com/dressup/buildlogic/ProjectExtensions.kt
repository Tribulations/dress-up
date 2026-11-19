package com.dressup.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/** The `libs` version catalog (gradle/libs.versions.toml), usable from convention plugins. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { IllegalArgumentException("No library '$alias' in libs.versions.toml") }

internal fun VersionCatalog.versionInt(alias: String): Int =
    findVersion(alias).orElseThrow { IllegalArgumentException("No version '$alias' in libs.versions.toml") }
        .requiredVersion
        .toInt()

/**
 * Namespace derived from the module path, so modules don't have to repeat it:
 * `:core:data` → `com.dressup.core.data`. A dash, invalid in a package name, is dropped:
 * `:adapter:cutout-mlkit` → `com.dressup.adapter.cutoutmlkit`.
 */
internal val Project.derivedNamespace: String
    get() = "com.dressup" + path.replace(':', '.').replace("-", "")
