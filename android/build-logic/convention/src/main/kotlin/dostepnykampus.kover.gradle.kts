/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    id("org.jetbrains.kotlinx.kover")
}

/**
 * Moduły poza progiem pokrycia: kod `core:designsystem` uruchamiają tylko testy Robolectric (zrzuty Roborazzi),
 * a ich wykonania Kover nie rejestruje, więc moduł sprawdzają zrzuty.
 */
val coverageExcludedProjects = setOf(":core:designsystem")

kover {
    reports {
        filters {
            excludes {
                classes("*ComposableSingletons*", "*.BuildConfig", "*.R", "*.R$*")
            }
        }
    }
}

pluginManager.withPlugin("com.android.application") {
    val modules = rootProject.subprojects.filter { it != project && it.buildFile.exists() }
    dependencies {
        modules.filter { it.path !in coverageExcludedProjects }.forEach { "kover"(project(it.path)) }
    }
    gradle.projectsEvaluated {
        val missing = modules.filterNot {
            it.pluginManager.hasPlugin("org.jetbrains.kotlinx.kover") && it.pluginManager.hasPlugin("dostepnykampus.detekt")
        }
        check(missing.isEmpty()) {
            "Moduły bez convention plugins (Kover, detekt): ${missing.joinToString { it.path }}"
        }
    }
    kover.reports.verify.rule {
        minBound(80, CoverageUnit.LINE)
        minBound(70, CoverageUnit.BRANCH)
    }
}
