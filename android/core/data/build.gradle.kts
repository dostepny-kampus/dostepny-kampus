/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.library")
    id("dostepnykampus.android.hilt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val buildingsFile: RegularFile = layout.projectDirectory.file("../../../data/buildings/pwr-buildings.geojson")

android {
    testOptions.unitTests.all {
        /* Test repozytorium na prawdziwym pliku konturów z katalogu data/ w repozytorium. */
        it.jvmArgumentProviders.add(
            objects.newInstance<BuildingsFile>().apply { file.set(buildingsFile) },
        )
    }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:geo"))
    implementation(project(":core:network"))
    api(libs.androidx.datastore.core)
    api(libs.androidx.datastore.preferences.core)
    api(libs.dagger)
    api(libs.javax.inject)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)
    implementation(libs.hilt.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.okhttp)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.serialization.json)
}

/**
 * Kopiuje plik konturów z `data/buildings` do zasobów aplikacji przy budowie, bez kopii w repozytorium.
 *
 * Wymagania: US1
 */
abstract class CopyBuildingsAsset : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val source: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copy() {
        source.get().asFile.copyTo(outputDirectory.file("pwr-buildings.geojson").get().asFile, overwrite = true)
    }
}

val copyBuildingsAsset =
    tasks.register<CopyBuildingsAsset>("copyBuildingsAsset") {
        source.set(buildingsFile)
    }

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyBuildingsAsset, CopyBuildingsAsset::outputDirectory)
    }
}
