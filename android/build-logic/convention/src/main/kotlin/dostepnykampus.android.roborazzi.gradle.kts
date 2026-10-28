import com.android.build.api.dsl.CommonExtension

plugins {
    id("io.github.takahirom.roborazzi")
}

extensions.configure<CommonExtension> {
    testOptions.unitTests.isIncludeAndroidResources = true
}

roborazzi {
    outputDir = layout.projectDirectory.dir("src/screenshots")
}

tasks.withType<Test>().configureEach {
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--enable-native-access=ALL-UNNAMED")
}

tasks.named("check") {
    dependsOn("verifyRoborazziDebug")
}

dependencies {
    "testImplementation"(libs.findLibrary("robolectric").get())
    "testImplementation"(libs.findLibrary("roborazzi").get())
    "testImplementation"(libs.findLibrary("roborazzi-compose").get())
    constraints {
        "testImplementation"(libs.findLibrary("androidx-test-espresso-core").get())
    }
}
