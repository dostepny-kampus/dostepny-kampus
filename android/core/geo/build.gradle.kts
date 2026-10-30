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

/** Ścieżka pliku konturów dla testu; wejściem zadania jest treść pliku, a nie jego położenie na dysku. */
abstract class BuildingsFile : CommandLineArgumentProvider {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val file: RegularFileProperty

    override fun asArguments(): List<String> = listOf("-Ddostepnykampus.buildings=${file.get().asFile.path}")
}

tasks.test {
    useJUnitPlatform()
    // Test na prawdziwym pliku konturów z katalogu data/ w repozytorium.
    jvmArgumentProviders.add(
        objects.newInstance<BuildingsFile>().apply {
            file.set(layout.projectDirectory.file("../../../data/buildings/pwr-buildings.geojson"))
        },
    )
}
