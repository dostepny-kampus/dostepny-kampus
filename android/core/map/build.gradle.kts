/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.library")
    id("dostepnykampus.android.compose")
    id("dostepnykampus.android.hilt")
}

kotlin {
    explicitApi()
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.compose.runtime)
    api(libs.dagger)
    api(libs.javax.inject)
    api(libs.kotlinx.serialization.json)
    api(libs.okhttp)
    implementation(project(":core:designsystem"))
    implementation(libs.hilt.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.maplibre.android)
    testImplementation(libs.okhttp.mockwebserver)
}
