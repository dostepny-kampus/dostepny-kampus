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
    implementation(libs.spatialk.geojson)
    implementation(libs.spatialk.turf)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    // Test na prawdziwym pliku konturów z katalogu data/ w repozytorium.
    val buildings = layout.projectDirectory.file("../../../data/buildings/pwr-buildings.geojson")
    inputs.file(buildings).withPathSensitivity(PathSensitivity.NONE)
    systemProperty("dostepnykampus.buildings", buildings.asFile.path)
}
