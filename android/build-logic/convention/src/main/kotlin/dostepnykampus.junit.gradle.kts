/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    "testImplementation"(platform(libs.findLibrary("junit-bom").get()))
    "testImplementation"(libs.findLibrary("junit-jupiter-api").get())
    "testRuntimeOnly"(libs.findLibrary("junit-jupiter-engine").get())
    "testRuntimeOnly"(libs.findLibrary("junit-vintage-engine").get())
    "testRuntimeOnly"(libs.findLibrary("junit-platform-launcher").get())
}
