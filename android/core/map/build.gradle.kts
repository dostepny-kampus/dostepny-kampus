/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.library")
    id("dostepnykampus.android.compose")
}

kotlin {
    explicitApi()
}

dependencies {
    api(libs.androidx.compose.runtime)
    api(libs.kotlinx.serialization.json)
    implementation(project(":core:designsystem"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.maplibre.android)
}
