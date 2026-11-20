/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    id("dostepnykampus.android.library")
    id("dostepnykampus.android.hilt")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.openapi.generator)
}

android {
    buildFeatures.buildConfig = true
    /* Z emulatora 10.0.2.2 to komputer gospodarza. Release zostaje przy tym samym placeholderze. */
    val apiBaseUrl = "\"http://10.0.2.2:8080/\""
    buildTypes {
        getByName("debug") { buildConfigField("String", "API_BASE_URL", apiBaseUrl) }
        getByName("release") { buildConfigField("String", "API_BASE_URL", apiBaseUrl) }
    }
}

val openApiFile = rootProject.layout.projectDirectory.file("../api/openapi.yaml")

/*
 * Modele JSON klienta z kontraktu: schematy, które moduł czyta i wysyła, razem z zależnymi. Stała bez typu (`const`)
 * jest tekstem, a kontur GeoJSON zostaje obiektem JSON dla czytnika geometrii z `:core:geo`.
 */
val generateContractModels =
    tasks.named<GenerateTask>("openApiGenerate") {
        generatorName = "kotlin"
        /* Ścieżka jako URI pliku: generator nie czyta zwykłej ścieżki ze spacją. */
        remoteInputSpec = openApiFile.asFile.toURI().toString()
        inputs.file(openApiFile).withPathSensitivity(PathSensitivity.NONE).withPropertyName("openApiFile")
        outputDir = layout.buildDirectory.dir("generated/source/openapi")
        modelPackage = "io.github.dostepnykampus.core.network.json"
        cleanupOutput = true
        modelFilesConstrainedTo =
            listOf(
                "BuildingList", "BuildingDetails", "LocateResult", "FloorList", "RoomList", "RoomSearchResults",
                "SubmissionContent", "Submission", "VersionConflictProblem", "BuildingProgress", "SyncPullResponse",
                "AccessiblePathList",
            )
        generateRecursiveDependentModels = true
        generateModelDocumentation = false
        generateModelTests = false
        configOptions =
            mapOf(
                "serializationLibrary" to "kotlinx_serialization",
                "dateLibrary" to "string",
                "nonPublicApi" to "true",
                "enumPropertyNaming" to "UPPERCASE",
            )
        typeMappings = mapOf("UUID" to "kotlin.String", "URI" to "kotlin.String", "AnyType" to "kotlin.String")
        schemaMappings = mapOf("GeoArea" to "kotlinx.serialization.json.JsonObject")
    }

androidComponents.onVariants { variant ->
    variant.sources.kotlin?.addGeneratedSourceDirectory(generateContractModels, GenerateTask::outputDir)
}

tasks.withType<Test>().configureEach {
    inputs.file(openApiFile).withPathSensitivity(PathSensitivity.NONE).withPropertyName("openApiFile")
    systemProperty("openapi.file", openApiFile.asFile.relativeTo(projectDir).invariantSeparatorsPath)
}

kotlin {
    explicitApi()
}

dependencies {
    api(project(":core:model"))
    api(libs.dagger)
    api(libs.javax.inject)
    api(libs.kotlinx.serialization.core)
    implementation(project(":core:geo"))
    implementation(libs.hilt.core)
    implementation(libs.okhttp)
    implementation(libs.timber)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.json.schema.validator)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.junit.platform.commons)
    testImplementation(libs.okio)
    testImplementation(libs.opentest4j)
}
