/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.application")
    id("dostepnykampus.android.compose")
    id("dostepnykampus.android.roborazzi")
    id("dostepnykampus.android.hilt")
}

android {
    buildFeatures.buildConfig = true
    defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testOptions.unitTests.all { it.useJUnitPlatform() }
}

tasks.withType<Test>().configureEach {
    inputs.files(fileTree(rootDir) { include("**/src/**/*.kt").exclude("**/build/**") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("konsistSources")
}

dependencies {
    implementation(project(":feature:auth"))
    implementation(project(":feature:map"))
    implementation(project(":feature:capture"))
    implementation(project(":feature:review"))
    implementation(project(":feature:progress"))
    implementation(project(":feature:help"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.navigation.suite)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.startup.runtime)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.konsist)
    testImplementation(project(":core:network"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit4)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.androidx.work.testing)
    kspTest(libs.hilt.compiler)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    constraints {
        implementation(libs.androidx.lifecycle.runtime)
    }
}
