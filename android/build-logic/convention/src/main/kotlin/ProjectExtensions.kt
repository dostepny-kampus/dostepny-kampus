/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.Lint
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import java.io.File

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.versionInt(alias: String): Int = findVersion(alias).get().requiredVersion.toInt()

/**
 * Każde ostrzeżenie Lint to błąd; poza kontrolami świeżości wersji: zależą od daty i sieci, robi to Dependabot.
 * Wyjątki dla pojedynczych bibliotek są we wspólnym pliku [exceptions], każdy z własnym issue.
 */
internal fun Lint.configureStrict(exceptions: File) {
    lintConfig = exceptions
    checkAllWarnings = true
    warningsAsErrors = true
    abortOnError = true
    disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
}

/**
 * Podpis debug stałym kluczem [keystore] z repozytorium zamiast klucza tworzonego na każdej maszynie, więc `apk-debug`
 * z kolejnych przebiegów CI instaluje się na poprzedni bez kasowania danych. Klucz służy tylko do debug.
 */
internal fun ApkSigningConfig.useRepositoryDebugKey(keystore: File) {
    storeFile = keystore
    storePassword = "android"
    keyAlias = "androiddebugkey"
    keyPassword = "android"
}
