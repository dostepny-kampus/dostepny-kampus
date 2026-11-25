/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

plugins {
    id("com.android.application")
    id("com.jraska.module.graph.assertion")
    id("dostepnykampus.android.base")
}

android {
    defaultConfig {
        applicationId = "io.github.dostepnykampus"
        targetSdk = libs.versionInt("androidTargetSdk")
        versionCode = 1
        versionName = "0.1.0"
    }
    signingConfigs.getByName("debug").useRepositoryDebugKey(rootDir.resolve("debug.keystore"))
}

/* Plik APK i pakiet release powstają tylko z adresami https:// z konfiguracji budowania (ServerAddress). */
val releaseAddresses =
    tasks.register<ReleaseAddressCheck>("checkReleaseAddresses") {
        addresses.set(ServerAddress.entries.associate { it.name to it.release(providers).orEmpty() })
    }
tasks.matching { it.name == "packageRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn(releaseAddresses)
}

moduleGraphAssert {
    maxHeight = 5
    allowed = arrayOf(
        ":app -> :(core|feature):.*",
        ":feature:.* -> :core:.*",
        ":core:.* -> :core:.*",
    )
    restricted = arrayOf(
        ":core:model -X> .*",
        ":feature:.* -X> :core:(database|network)",
    )
}
