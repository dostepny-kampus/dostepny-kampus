/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.feature")
    id("dostepnykampus.android.hilt")
}

android {
    testOptions.unitTests.all { it.useJUnitPlatform() }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:location"))
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
