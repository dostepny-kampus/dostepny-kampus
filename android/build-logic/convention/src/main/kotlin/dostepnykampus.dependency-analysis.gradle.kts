/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Analiza zależności Gradle dla projektu głównego; moduły dostają wtyczkę z wtyczek bazowych. Zadanie
 * `buildHealth` kończy się błędem, gdy moduł używa w kodzie biblioteki, której nie deklaruje, i podaje linię do
 * dopisania w `dependencies`. Pozostałe porady są ostrzeżeniami w raporcie
 * `build/reports/dependency-analysis/build-health-report.txt`. Wyjątki od reguły są tylko tutaj, a ich lista może
 * tylko maleć. Artefakty jednej biblioteki tworzą pakiet: deklaracja jednego z nich wystarcza za pozostałe
 * (Dagger i Hilt z grupy `com.google.dagger`, rdzeń i dyspozytor Androida dla korutyn).
 */
plugins {
    id("com.autonomousapps.dependency-analysis")
}

dependencyAnalysis {
    structure {
        bundle("dagger") {
            includeGroup("com.google.dagger")
        }
        bundle("korutyny") {
            includeDependency("org.jetbrains.kotlinx:kotlinx-coroutines-core")
            includeDependency("org.jetbrains.kotlinx:kotlinx-coroutines-android")
        }
    }
    issues {
        all {
            onAny {
                severity("warn")
            }
            onUsedTransitiveDependencies {
                severity("fail")
            }
        }
    }
}
