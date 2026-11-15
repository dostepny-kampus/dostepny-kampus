/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.robolectric")
    id("io.github.takahirom.roborazzi")
}

roborazzi {
    outputDir = layout.projectDirectory.dir("src/screenshots")
}

tasks.named("check") {
    dependsOn("verifyRoborazziDebug")
}

dependencies {
    "testImplementation"(libs.findLibrary("roborazzi").get())
    constraints {
        "testImplementation"(libs.findLibrary("androidx-test-espresso-core").get())
    }
}
