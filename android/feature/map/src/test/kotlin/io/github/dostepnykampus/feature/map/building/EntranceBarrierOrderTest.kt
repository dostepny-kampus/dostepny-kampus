/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.Severity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Wejście zalecane i odpowiedź karty, gdy przy wejściu jest przyjęta bariera.
 *
 * Wymagania: BR-25, US2.2, US3.1
 */
class EntranceBarrierOrderTest {
    /**
     * Wiersz: bariera „Tylko z pomocą” przy wejściu zalecanym → wejście zostaje zalecane, a odpowiedź ma jej
     *   uciążliwość.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Bariera przy wejściu zalecanym jest na karcie przy tym
     *   wejściu
     */
    @Test
    fun keepsRecommendedAtMinorBarrier() {
        val ordered = orderedEntrances(TWO_ENTRANCES, listOf(entranceBarrier("b", Severity.ONLY_WITH_HELP)))
        assertEquals(listOf("b"), ordered.filter { it.isRecommended }.map { it.entrance.id })
        assertEquals(
            VisitAnswer.Accessible(id = "b", name = "Wejście B", doorWidthCm = 100, barrier = Severity.ONLY_WITH_HELP),
            visitAnswer(ordered),
        )
    }

    /** Wiersz: dwie bariery przy jednym wejściu → wejście ma tę o najwyższej uciążliwości. */
    @Test
    fun keepsWorstBarrierOfEntrance() {
        val barriers = listOf(entranceBarrier("c", Severity.HINDRANCE), entranceBarrier("c", Severity.ONLY_WITH_HELP))
        val ordered = orderedEntrances(TWO_ENTRANCES, barriers)
        assertEquals(Severity.ONLY_WITH_HELP, ordered.first { it.entrance.id == "c" }.barrier?.severity)
    }

    /**
     * Wiersz: blokada przy wejściu B, wejście C dostępne → zalecane jest C, a B zostaje „dostępne”
     *   z blokadą.
     * Scenariusz (częściowo, przykład: jest inne): przegladanie-mapy.feature: Wejście z blokadą nie jest
     *   zalecane, a jego oznaczenie zostaje
     */
    @Test
    fun recommendsNextWhenBlocked() {
        val ordered = orderedEntrances(TWO_ENTRANCES, listOf(entranceBarrier("b", Severity.BLOCKING)))
        val blocked = ordered.first { it.entrance.id == "b" }
        assertEquals(listOf("c"), ordered.filter { it.isRecommended }.map { it.entrance.id })
        assertEquals(listOf(EntranceMarking.ACCESSIBLE, true), listOf(blocked.entrance.marking, blocked.isBlocked))
    }

    /**
     * Wiersz: blokada przy jedynym wejściu dostępnym → brak wejścia zalecanego, odpowiedź o blokadzie i wiersz
     *   „Brak wejścia dostępnego”.
     * Scenariusz (częściowo, przykład: nie ma): przegladanie-mapy.feature: Wejście z blokadą nie jest
     *   zalecane, a jego oznaczenie zostaje
     */
    @Test
    fun answersOnlyAccessibleBlocked() {
        val entrances = listOf(sampleEntrance("b"), sampleEntrance("c").copy(marking = EntranceMarking.INACCESSIBLE))
        val ordered = orderedEntrances(entrances, listOf(entranceBarrier("b", Severity.BLOCKING)))
        assertEquals(
            listOf<Any>(VisitAnswer.Blocked(count = 1), EntranceSummary.NoAccessible),
            listOf(visitAnswer(ordered), entranceSummary(ordered)),
        )
    }
}
