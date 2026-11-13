/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.jvm.library")
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.spatialk.geojson)
    implementation(libs.spatialk.turf)
    implementation(libs.spatialk.units)

    testImplementation(libs.junit.jupiter.params)
}

tasks.test {
    /* Test na prawdziwym pliku konturów z katalogu data/ w repozytorium. */
    jvmArgumentProviders.add(
        objects.newInstance<BuildingsFile>().apply {
            file.set(layout.projectDirectory.file("../../../data/buildings/pwr-buildings.geojson"))
        },
    )
}
