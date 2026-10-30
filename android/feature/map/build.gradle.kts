/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    testOptions.unitTests.all { it.useJUnitPlatform() }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.spatialk.geojson)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
