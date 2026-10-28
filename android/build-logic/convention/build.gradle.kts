plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.compose.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.moduleGraphAssert.gradlePlugin)
    implementation(libs.roborazzi.gradlePlugin)
    implementation(libs.snakeyaml.engine)
    implementation(libs.kotlinpoet)
}
