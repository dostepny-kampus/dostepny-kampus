/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.library")
}

dependencies {
    api(project(":core:data"))
    api(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    testImplementation(libs.kotlinx.coroutines.core)
}
