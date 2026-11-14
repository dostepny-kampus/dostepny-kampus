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
