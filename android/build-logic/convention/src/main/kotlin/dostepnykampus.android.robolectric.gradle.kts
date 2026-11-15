/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

import com.android.build.api.dsl.CommonExtension

/*
 * Testy Robolectric modułu Androida: zasoby w testach JVM i flagi JVM, bez których Robolectric
 * nie sięga do wewnętrznych klas JDK ani kodu natywnego. Jedyne miejsce tych flag.
 */
extensions.configure<CommonExtension> {
    testOptions.unitTests.isIncludeAndroidResources = true
}

tasks.withType<Test>().configureEach {
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--enable-native-access=ALL-UNNAMED")
}

dependencies {
    "testImplementation"(libs.findLibrary("robolectric").get())
}
