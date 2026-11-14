/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/* Testy systemowe: APK testów aplikacji bez podmiany sieci z testów `app`, uruchamiany z prawdziwym serwerem. */
plugins {
    id("dostepnykampus.android.systemtest")
}

android {
    targetProjectPath = ":app"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:testing"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui.test)
    implementation(libs.androidx.compose.ui.test.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.junit4)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
}
