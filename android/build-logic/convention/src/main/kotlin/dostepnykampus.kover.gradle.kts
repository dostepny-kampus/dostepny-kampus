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

/**
 * Klasy poza progiem pokrycia: widok MapLibre (`feature:map`, decyzja 86) uruchamia natywną bibliotekę mapy na ARM,
 * której Robolectric nie wczytuje, więc testy JVM go nie wykonają. Widok sprawdza zrzut na emulatorze, a jego logika
 * (kamera, styl, dane, trafienie dotknięcia, nagłówek HTTP) jest w funkcjach JVM z testami i zostaje pod progiem.
 */
val coverageExcludedClasses =
    arrayOf(
        "io.github.dostepnykampus.feature.map.MapLibreMapView",
        "io.github.dostepnykampus.feature.map.MapLibreMapView$*",
    )

kover {
    reports {
        filters {
            excludes {
                classes("*ComposableSingletons*", "*.BuildConfig", "*.R", "*.R$*")
                classes(*coverageExcludedClasses)
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
