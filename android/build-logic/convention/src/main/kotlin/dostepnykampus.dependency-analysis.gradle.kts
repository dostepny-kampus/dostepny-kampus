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
 * tylko maleć.
 */
plugins {
    id("com.autonomousapps.dependency-analysis")
}

dependencyAnalysis {
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
