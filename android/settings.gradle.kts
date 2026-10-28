pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
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
    ":feature:auth",
    ":feature:map",
    ":feature:capture",
    ":feature:review",
    ":feature:progress",
)
