/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

import com.android.build.api.dsl.Lint
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.versionInt(alias: String): Int = findVersion(alias).get().requiredVersion.toInt()

/** Każde ostrzeżenie Lint to błąd; poza kontrolami świeżości wersji: zależą od daty i sieci, robi to Dependabot. */
internal fun Lint.configureStrict() {
    checkAllWarnings = true
    warningsAsErrors = true
    abortOnError = true
    disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
}
