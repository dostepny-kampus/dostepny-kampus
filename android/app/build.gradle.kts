plugins {
    id("dostepnykampus.android.application")
    id("dostepnykampus.android.compose")
}

android {
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
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.konsist)
    testRuntimeOnly(libs.junit.platform.launcher)
    constraints {
        implementation(libs.androidx.lifecycle.runtime)
    }
}
