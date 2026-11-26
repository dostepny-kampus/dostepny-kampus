/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.library")
}

kotlin {
    explicitApi()
}

dependencies {
    api(libs.androidx.compose.runtime)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    implementation(project(":core:designsystem"))
    implementation(libs.maplibre.android)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
