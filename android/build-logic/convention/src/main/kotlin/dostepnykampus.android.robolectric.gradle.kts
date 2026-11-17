/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

import com.android.build.api.dsl.CommonExtension

/*
 * Testy Robolectric modułu Androida: zasoby w testach JVM i flagi JVM, bez których Robolectric
 * nie sięga do wewnętrznych klas JDK ani kodu natywnego. Jedyne miejsce tych flag. Obrazy `android-all`
 * Robolectric pobiera z lustra Maven Central od Google, jak Gradle w `settings.gradle.kts`.
 */
extensions.configure<CommonExtension> {
    testOptions.unitTests.isIncludeAndroidResources = true
}

tasks.withType<Test>().configureEach {
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--enable-native-access=ALL-UNNAMED")
    systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2")
}

dependencies {
    "testImplementation"(libs.findLibrary("robolectric").get())
}
