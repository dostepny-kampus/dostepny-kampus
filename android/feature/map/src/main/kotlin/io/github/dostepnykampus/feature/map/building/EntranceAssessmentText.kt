/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import io.github.dostepnykampus.core.model.building.EntranceAssessment

/**
 * Wspólne oznaczenie wejść [entrances]: dzień i źródło, gdy każde wejście ma te same.
 * `null`, gdy lista jest pusta, któreś wejście nie ma oznaczenia (z pliku) albo oznaczenia się różnią — wtedy
 * napis stoi przy każdym wejściu.
 *
 * Wymagania: US17.3, BR-24
 */
internal fun commonAssessment(entrances: List<OrderedEntrance>): EntranceAssessment? =
    entrances.map { it.entrance.assessment }.distinct().singleOrNull()
