/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.building

import java.time.LocalDate

/**
 * Dzień i źródło oznaczenia wejścia, zawsze razem.
 *
 * Wymagania: US17.3, BR-24
 *
 * @property date dzień importu albo dzień przyjęcia zmiany oznaczenia (pole `markingDate` w kontrakcie)
 * @property source skąd pochodzi oznaczenie (pole `markingSource` w kontrakcie)
 */
public data class EntranceAssessment(val date: LocalDate, val source: AssessmentSource)

/**
 * Źródło oznaczenia wejścia (`markingSource` z kontraktu API, przy imporcie razem z polem `source` wejścia).
 *
 * Wymagania: BR-24, BR-27
 */
public enum class AssessmentSource {
    /** Import z OpenStreetMap i opisów, bez warstwy wejść planów PWr. */
    IMPORT,

    /** Import z warstwy wejść planów PWr. */
    PWR_PLANS,

    /** Przyjęta zmiana oznaczenia z obchodu. */
    WALKTHROUGH,
}
