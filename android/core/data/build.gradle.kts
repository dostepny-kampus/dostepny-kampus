/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.library")
}

android {
    testOptions.unitTests.all { it.useJUnitPlatform() }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:geo"))
    implementation(libs.androidx.datastore.preferences)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
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
        source.set(layout.projectDirectory.file("../../../data/buildings/pwr-buildings.geojson"))
    }

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyBuildingsAsset, CopyBuildingsAsset::outputDirectory)
    }
}
