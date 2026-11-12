/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

import dev.detekt.gradle.Detekt

plugins {
    id("dostepnykampus.jvm.library")
}

dependencies {
    compileOnly(libs.detekt.api)
    testImplementation(libs.detekt.api)
    testImplementation(libs.detekt.test)
    testImplementation(libs.detekt.test.utils)
}

/**
 * Pliki poza źródłami modułów, których nie widzą zadania detekt modułów: skrypty `*.kts`, `build-logic` i ten moduł
 * (sam siebie nie wczytuje jako wtyczki). Zadanie uruchamia tylko reguły projektu, więc zakres zakazu `//` jest ten
 * sam co w dawnym teście `NoLineCommentsTest`: każdy plik `*.kt` i `*.kts` w `android/` (#873).
 */
val detektScripts by tasks.registering(Detekt::class) {
    description = "Reguły projektu na skryptach Gradle, build-logic i module reguł."
    setSource(rootDir)
    include("**/*.kts", "build-logic/**/*.kt", "detekt-rules/**/*.kt")
    exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**")
    pluginClasspath.setFrom(tasks.jar)
    disableDefaultRuleSets = true
    allRules = true
    buildUponDefaultConfig = false
    config.setFrom()
}

tasks.check {
    dependsOn(detektScripts)
}
