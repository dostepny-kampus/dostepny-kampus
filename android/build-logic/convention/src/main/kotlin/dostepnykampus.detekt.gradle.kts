/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.FailOnSeverity

plugins {
    id("dev.detekt")
}

/** Moduł reguł projektu (#873); sam siebie nie wczytuje, a jego pliki sprawdza zadanie `detektScripts` w nim. */
val rulesModule = ":detekt-rules"

detekt {
    buildUponDefaultConfig = true
    allRules = true
    config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
    failOnSeverity = FailOnSeverity.Info
    basePath = rootProject.layout.projectDirectory
}

dependencies {
    detektPlugins(libs.findLibrary("detekt-rules-ktlint").get())
    detektPlugins(libs.findLibrary("compose-rules-detekt").get())
    if (path != rulesModule) {
        detektPlugins(project(rulesModule))
    }
}

tasks.named("check") {
    dependsOn(tasks.withType<Detekt>())
}
