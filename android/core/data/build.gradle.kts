/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.library")
}

val buildingsFile: RegularFile = layout.projectDirectory.file("../../../data/buildings/pwr-buildings.geojson")

android {
    testOptions.unitTests.all {
        it.useJUnitPlatform()
        // Test repozytorium na prawdziwym pliku konturów z katalogu data/ w repozytorium.
        it.jvmArgumentProviders.add(
            objects.newInstance<BuildingsFile>().apply { file.set(buildingsFile) },
        )
    }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:geo"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
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

/** Kopiuje plik konturów z `data/buildings` do zasobów aplikacji przy budowie, bez kopii w repozytorium (US1). */
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
