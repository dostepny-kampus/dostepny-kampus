/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.feature")
    id("dostepnykampus.android.hilt")
    id("dostepnykampus.android.roborazzi")
}

/**
 * Treść pliku granic terenów jako stała `AREAS_GEOJSON`: dane mapy czytają granice bez zasobów Androida, także
 * w teście JVM.
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
        val file = output.resolve("io/github/dostepnykampus/feature/map/campus/AreasGeoJson.kt")
        file.parentFile.mkdirs()
        val quotes = "\"\"\""
        file.writeText(
            "package io.github.dostepnykampus.feature.map.campus\n\n" +
                "/** Treść pliku `data/areas/pwr-areas.geojson`. */\n" +
                "internal const val AREAS_GEOJSON: String = $quotes$text$quotes\n",
        )
    }
}

val generateAreaBoundaries =
    tasks.register<GenerateAreaBoundaries>("generateAreaBoundaries") {
        source.set(layout.projectDirectory.file("../../../data/areas/pwr-areas.geojson"))
    }

androidComponents {
    onVariants { variant ->
        variant.sources.kotlin?.addGeneratedSourceDirectory(
            generateAreaBoundaries,
            GenerateAreaBoundaries::outputDirectory,
        )
    }
}

dependencies {
    api(libs.androidx.compose.foundation.layout)
    api(libs.coil.compose.core)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    api(libs.androidx.lifecycle.viewmodel)
    api(libs.androidx.navigation3.runtime)
    api(libs.dagger)
    api(libs.javax.inject)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.core)
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:geo"))
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime.annotation)
    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.androidx.compose.ui.geometry)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core)
    implementation(libs.coil.compose)
    implementation(libs.hilt.core)
    api(libs.kotlinx.serialization.json)
    implementation(libs.maplibre.android)
    implementation(libs.maplibre.geojson)
    implementation(libs.okhttp)
    implementation(libs.spatialk.geojson)
    implementation(libs.spatialk.turf)
    implementation(libs.spatialk.units)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.coil.core)
    testImplementation(libs.coil.singleton)
    testImplementation(libs.coil.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit4)
    testImplementation(libs.androidx.activity)
    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.roborazzi.core)
    testImplementation(libs.robolectric.annotations)
    testImplementation(libs.robolectric.shadows.framework)
    testImplementation(libs.androidx.compose.ui.test.manifest)
    constraints {
        implementation(libs.androidx.core.ktx) {
            because("Core zawiera rozszerzenia Kotlin; starszy Core KTX dubluje klasy.")
        }
    }
}
