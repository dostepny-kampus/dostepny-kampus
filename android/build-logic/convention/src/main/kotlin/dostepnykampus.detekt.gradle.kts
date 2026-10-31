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
}

tasks.named("check") {
    dependsOn(tasks.withType<Detekt>())
}
