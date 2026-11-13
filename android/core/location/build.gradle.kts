/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.library")
    id("dostepnykampus.android.hilt")
}

android {
    testOptions.unitTests.isIncludeAndroidResources = true
}

kotlin {
    explicitApi()
}

dependencies {
    api(project(":core:model"))
    api(libs.dagger)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.hilt.core)
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.robolectric.shadows.framework)
    testImplementation(libs.kotlinx.coroutines.test)
}
