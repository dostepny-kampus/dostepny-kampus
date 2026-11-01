/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.library")
    id("dostepnykampus.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    buildFeatures.buildConfig = true
    // Z emulatora 10.0.2.2 to komputer gospodarza. Release zostaje przy tym samym placeholderze.
    val apiBaseUrl = "\"http://10.0.2.2:8080/\""
    buildTypes {
        getByName("debug") { buildConfigField("String", "API_BASE_URL", apiBaseUrl) }
        getByName("release") { buildConfigField("String", "API_BASE_URL", apiBaseUrl) }
    }
    testOptions.unitTests.all { it.useJUnitPlatform() }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:geo"))
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.okhttp.mockwebserver)
    testRuntimeOnly(libs.junit.platform.launcher)
}
