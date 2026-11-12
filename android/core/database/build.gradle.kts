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
    testOptions.unitTests.isIncludeAndroidResources = true

    /* Schematy Room też do testów jednostkowych (Robolectric), żeby MigrationTestHelper je czytał. */
    sourceSets.getByName("test") {
        assets.srcDir("$projectDir/generated/schemas")
    }
}

room {
    /* Katalog `generated` jest wyłączony z kontroli CI, bo migawka schematu powstaje w budowie. */
    schemaDirectory("$projectDir/generated/schemas")
}

dependencies {
    api(libs.dagger)
    api(libs.javax.inject)
    implementation(libs.androidx.collection)
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite)
    implementation(libs.hilt.core)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.junit4)
    testImplementation(libs.androidx.test.monitor)
    testImplementation(libs.robolectric)
    testImplementation(libs.robolectric.shadows.framework)
    testImplementation(libs.kotlinx.coroutines.test)
}
