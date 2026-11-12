/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.feature")
}

dependencies {
    api(libs.kotlinx.serialization.core)
    implementation(libs.androidx.compose.runtime)
}
