/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

import com.android.build.api.dsl.TestExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/* Osobny APK testów aplikacji (`com.android.test`) z tymi samymi wersjami SDK, Javy i analizą co reszta modułów. */
plugins {
    id("com.android.test")
    id("dostepnykampus.detekt")
    id("dostepnykampus.kover")
}

extensions.configure<TestExtension> {
    namespace = "io.github.dostepnykampus" + path.replace(':', '.')
    compileSdk = libs.versionInt("androidCompileSdk")
    defaultConfig.minSdk = libs.versionInt("androidMinSdk")
    defaultConfig.targetSdk = libs.versionInt("androidTargetSdk")
    defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    compileOptions.sourceCompatibility = JavaVersion.toVersion(libs.versionInt("jvmTarget"))
    compileOptions.targetCompatibility = JavaVersion.toVersion(libs.versionInt("jvmTarget"))
    lint.configureStrict()
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.allWarningsAsErrors = true
}
