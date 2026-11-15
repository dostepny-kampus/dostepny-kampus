/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.jvm.library")
}

kotlin {
    explicitApi()
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
    /* Testy na prawdziwych plikach konturów i granic terenów z katalogu data/ w repozytorium. */
    jvmArgumentProviders.add(
        objects.newInstance<BuildingsFile>().apply {
            file.set(layout.projectDirectory.file("../../../data/buildings/pwr-buildings.geojson"))
        },
    )
    jvmArgumentProviders.add(
        objects.newInstance<BuildingsFile>().apply {
            file.set(layout.projectDirectory.file("../../../data/areas/pwr-areas.geojson"))
            property.set("dostepnykampus.areas")
        },
    )
}
