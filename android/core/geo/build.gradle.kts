plugins {
    id("dostepnykampus.jvm.library")
}

dependencies {
    api(project(":core:model"))
    implementation(libs.spatialk.turf)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
