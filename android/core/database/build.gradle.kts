/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("dostepnykampus.android.library")
    id("dostepnykampus.android.hilt")
    alias(libs.plugins.androidx.room)
}

android {
    testOptions.unitTests.all { it.useJUnitPlatform() }
}

room {
    // `generated` входить у CI-виключення «**/generated/**», бо снапшот згенерований збіркою.
    schemaDirectory("$projectDir/generated/schemas")
}

dependencies {
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}
