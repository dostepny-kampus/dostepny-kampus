/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.feature")
    id("dostepnykampus.android.hilt")
}

dependencies {
    api(libs.androidx.lifecycle.viewmodel)
    api(libs.androidx.navigation3.runtime)
    api(libs.dagger)
    api(libs.javax.inject)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.core)
    api(project(":core:data"))
    api(project(":core:model"))
    implementation(libs.hilt.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
