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

/**
 * Treść pliku granic terenów jako stała `AREAS_GEOJSON`: mapa i zgłoszenia czytają granice bez zasobów Androida,
 * także w teście JVM.
 */
@CacheableTask
abstract class GenerateAreaBoundaries : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val source: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val text = source.get().asFile.readText()
        check('$' !in text && "\"\"\"" !in text) { "Pliku granic terenów nie da się wstawić do napisu Kotlina" }
        val output = outputDirectory.get().asFile.apply { deleteRecursively() }
        val file = output.resolve("io/github/dostepnykampus/core/geo/AreasGeoJson.kt")
        file.parentFile.mkdirs()
        val quotes = "\"\"\""
        file.writeText(
            "package io.github.dostepnykampus.core.geo\n\n" +
                "/** Treść pliku `data/areas/pwr-areas.geojson`. */\n" +
                "internal const val AREAS_GEOJSON: String = $quotes$text$quotes\n",
        )
    }
}

val generateAreaBoundaries =
    tasks.register<GenerateAreaBoundaries>("generateAreaBoundaries") {
        source.set(layout.projectDirectory.file("../../../data/areas/pwr-areas.geojson"))
        outputDirectory.set(layout.buildDirectory.dir("generated/source/areas/main"))
    }

kotlin.sourceSets.main {
    kotlin.srcDir(generateAreaBoundaries.flatMap { it.outputDirectory })
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.spatialk.geojson)
    implementation(libs.spatialk.turf)
    implementation(libs.spatialk.units)
    implementation(libs.jts.core)

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
