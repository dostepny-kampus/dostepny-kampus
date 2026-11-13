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
    defaultConfig.testInstrumentationRunner = "io.github.dostepnykampus.app.HiltTestRunner"
    defaultConfig.testInstrumentationRunnerArguments["clearPackageData"] = "true"
    testOptions.execution = "ANDROIDX_TEST_ORCHESTRATOR"
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
    implementation(project(":core:database"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.navigation.suite)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.geometry)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigationevent)
    implementation(libs.androidx.navigationevent.compose)
    implementation(libs.androidx.savedstate)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.dagger)
    implementation(libs.hilt.core)
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)
    runtimeOnly(libs.kotlinx.coroutines.android)
    testImplementation(libs.konsist)
    testImplementation(project(":core:location"))
    testImplementation(project(":core:network"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit4)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.guava)
    testImplementation(libs.okio)
    testImplementation(libs.roborazzi.core)
    testImplementation(libs.robolectric.annotations)
    testImplementation(libs.robolectric.shadows.framework)
    kspTest(libs.hilt.compiler)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestUtil(libs.androidx.test.orchestrator)
    androidTestUtil(libs.androidx.test.services)
    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.junit4)
    androidTestImplementation(project(":core:network"))
    androidTestImplementation(libs.okhttp)
    androidTestImplementation(libs.maplibre.android)
    androidTestImplementation(libs.kotlinx.serialization.json)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(project(":core:network"))
    debugImplementation(libs.okhttp)
    testDebugImplementation(libs.okhttp.mockwebserver)
    constraints {
        implementation(libs.androidx.core.ktx) {
            because("Core zawiera rozszerzenia Kotlin; starszy Core KTX dubluje klasy.")
        }
        implementation(libs.androidx.lifecycle.runtime)
    }
}
