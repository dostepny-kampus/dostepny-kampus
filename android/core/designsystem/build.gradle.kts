/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.library")
    id("dostepnykampus.android.compose")
    id("dostepnykampus.android.roborazzi")
}

val tokenFile = rootProject.layout.projectDirectory.file("../docs/docs/ux/tokeny.yml")

val generateDesignTokens = tasks.register<GenerateDesignTokens>("generateDesignTokens") {
    tokensFile = tokenFile
    packageName = "io.github.dostepnykampus.core.designsystem"
    outputDirectory = layout.buildDirectory.dir("generated/source/tokens")
}

androidComponents.onVariants { variant ->
    variant.sources.kotlin?.addGeneratedSourceDirectory(generateDesignTokens, GenerateDesignTokens::outputDirectory)
}

tasks.withType<Test>().configureEach {
    inputs.file(tokenFile).withPathSensitivity(PathSensitivity.NONE).withPropertyName("tokensFile")
    systemProperty("tokens.file", tokenFile.asFile.relativeTo(projectDir).invariantSeparatorsPath)
}

kotlin {
    explicitApi()
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.compose.foundation.layout)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.text)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime.annotation)
    implementation(libs.androidx.compose.ui.geometry)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.unit)
    testImplementation(libs.junit4)
    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.core)
    testImplementation(libs.robolectric.annotations)
    testImplementation(libs.robolectric.shadows.framework)
    testImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.snakeyaml.engine)
    testImplementation(libs.kotlin.reflect)
    constraints {
        implementation(libs.androidx.lifecycle.runtime)
    }
}
