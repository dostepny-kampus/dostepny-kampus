/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.jvm.library")
}

kotlin {
    explicitApi()
}

dependencies {
    testImplementation(libs.kotlinx.coroutines.test)
}
