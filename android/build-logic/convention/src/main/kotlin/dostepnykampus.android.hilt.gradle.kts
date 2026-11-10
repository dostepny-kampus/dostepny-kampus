/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

import java.io.File

plugins {
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

dependencies {
    "implementation"(libs.findLibrary("hilt-android").get())
    "ksp"(libs.findLibrary("hilt-compiler").get())
}

val kspSerial = gradle.sharedServices.registerIfAbsent("kspSerial", KspSerialService::class.java) {
    maxParallelUsages.set(1)
}
val kspTask = Regex("ksp(\\w+)Kotlin")
val moduleDir = layout.projectDirectory.asFile
val moduleBuildDir = layout.buildDirectory.get().asFile

tasks.configureEach {
    val variant = kspTask.matchEntire(name)?.groupValues?.get(1) ?: return@configureEach
    val taskName = name
    usesService(kspSerial)
    val sourceSets = when {
        variant.endsWith("UnitTest") -> listOf("test", "test" + variant.removeSuffix("UnitTest"))
        variant.endsWith("AndroidTest") -> listOf("androidTest", "androidTest" + variant.removeSuffix("AndroidTest"))
        else -> listOf("main", variant.replaceFirstChar { it.lowercase() })
    }
    val sourceDirs = sourceSets.map { File(moduleDir, "src/$it") }
    val generated = File(moduleBuildDir, "generated/ksp/" + variant.replaceFirstChar { it.lowercase() })
    doLast { KspHiltGuard.check(taskName, sourceDirs, generated) }
}
