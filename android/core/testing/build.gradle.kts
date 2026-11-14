/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.library")
    id("dostepnykampus.android.compose")
    id("dostepnykampus.android.robolectric")
}

kotlin {
    explicitApi()
}

dependencies {
    api(libs.androidx.compose.ui.test)
    api(libs.androidx.compose.ui.test.junit4)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.test.espresso.core)
    testImplementation(libs.junit4)
    testImplementation(libs.androidx.activity)
    testImplementation(libs.androidx.activity.compose)
    testImplementation(libs.androidx.compose.foundation)
    testImplementation(libs.androidx.compose.foundation.layout)
    testImplementation(libs.androidx.compose.runtime)
    testImplementation(libs.androidx.compose.ui.graphics)
    testImplementation(libs.androidx.test.ext.junit)
    testRuntimeOnly(libs.androidx.compose.ui.test.manifest)
}
