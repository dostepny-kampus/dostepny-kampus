/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

import java.time.LocalDate

/**
 * Dzień i źródło oznaczenia wejścia, zawsze razem.
 *
 * Wymagania: US17.3, BR-24
 *
 * @property date dzień importu albo dzień przyjęcia zmiany oznaczenia (pole `markingDate` w kontrakcie)
 * @property source skąd pochodzi oznaczenie (pole `markingSource` w kontrakcie)
 */
data class EntranceAssessment(val date: LocalDate, val source: AssessmentSource)

/** Źródło oznaczenia wejścia (`markingSource` z kontraktu API). Wymagania: BR-24 */
enum class AssessmentSource {
    /** Import danych PWr albo OpenStreetMap. */
    IMPORT,

    /** Przyjęta zmiana oznaczenia z obchodu. */
    WALKTHROUGH,
}
