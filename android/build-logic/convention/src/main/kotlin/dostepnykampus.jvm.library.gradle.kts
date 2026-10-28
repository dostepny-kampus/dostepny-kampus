/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.android.lint")
    id("dostepnykampus.detekt")
}

val jvmTargetVersion = libs.versionInt("jvmTarget")

java {
    sourceCompatibility = JavaVersion.toVersion(jvmTargetVersion)
    targetCompatibility = JavaVersion.toVersion(jvmTargetVersion)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(jvmTargetVersion.toString())
        allWarningsAsErrors = true
        freeCompilerArgs.add("-Xjdk-release=$jvmTargetVersion")
    }
}

lint {
    configureStrict()
}
