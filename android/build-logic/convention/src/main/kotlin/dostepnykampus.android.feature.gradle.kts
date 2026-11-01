/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.library")
    id("dostepnykampus.android.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    "implementation"(libs.findLibrary("androidx-navigation3-runtime").get())
}
