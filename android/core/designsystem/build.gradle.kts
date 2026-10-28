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

dependencies {
    api(project(":core:model"))
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit4)
    testImplementation(libs.snakeyaml.engine)
    testImplementation(libs.kotlin.reflect)
    constraints {
        implementation(libs.androidx.lifecycle.runtime)
    }
}
