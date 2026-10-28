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
}

moduleGraphAssert {
    allowed = arrayOf(
        ":app -> :(core|feature):.*",
        ":feature:.* -> :core:.*",
        ":core:.* -> :core:.*",
    )
    restricted = arrayOf(
        ":core:model -X> .*",
    )
}
