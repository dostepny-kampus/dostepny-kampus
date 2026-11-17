/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        /* Lustro Maven Central od Google przed samym Central: chwilowe 429 z repo.maven.apache.org nie psuje CI. */
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        /* Lustro Maven Central od Google przed samym Central: chwilowe 429 z repo.maven.apache.org nie psuje CI. */
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
    }
}

rootProject.name = "dostepny-kampus"

include(
    ":app",
    ":core:model",
    ":core:geo",
    ":core:location",
    ":core:database",
    ":core:network",
    ":core:data",
    ":core:designsystem",
    ":core:testing",
    ":feature:map",
    ":feature:capture",
    ":feature:help",
    ":feature:inspection",
    ":feature:auth",
    ":detekt-rules",
    ":systemtest",
    ":macrobenchmark",
)
