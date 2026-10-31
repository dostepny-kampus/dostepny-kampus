/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

import com.android.build.api.dsl.CommonExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("dostepnykampus.detekt")
    id("dostepnykampus.kover")
}

extensions.configure<CommonExtension> {
    namespace = "io.github.dostepnykampus" + path.replace(':', '.')
    compileSdk = libs.versionInt("androidCompileSdk")
    defaultConfig.minSdk = libs.versionInt("androidMinSdk")
    compileOptions.sourceCompatibility = JavaVersion.toVersion(libs.versionInt("jvmTarget"))
    compileOptions.targetCompatibility = JavaVersion.toVersion(libs.versionInt("jvmTarget"))
    lint.configureStrict()
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.allWarningsAsErrors = true
}
