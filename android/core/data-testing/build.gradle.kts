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
    implementation(project(":core:network"))
}
