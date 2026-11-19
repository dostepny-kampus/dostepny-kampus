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
     * Wiersz: bariera „Tylko z pomocą” przy wejściu zalecanym, innego wejścia dostępnego nie ma → wejście zostaje
     *   zalecane, a odpowiedź ma jej uciążliwość.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Bariera przy wejściu zalecanym jest na karcie przy tym
     *   wejściu
     */
    @Test
    fun keepsRecommendedAtMinorBarrier() {
        val entrances = listOf(sampleEntrance("b"), sampleEntrance("c").copy(marking = EntranceMarking.INACCESSIBLE))
        val ordered = orderedEntrances(entrances, listOf(entranceBarrier("b", Severity.ONLY_WITH_HELP)))
        assertEquals(
            listOf("b"),
            ordered.filter { it.isRecommended }.map { it.entrance.id },
            "Bariera „Tylko z pomocą” odebrała wejściu zalecenie",
        )
        assertEquals(
            VisitAnswer.Accessible(id = "b", name = "Wejście B", doorWidthCm = 100, barrier = Severity.ONLY_WITH_HELP),
            visitAnswer(ordered),
            "Odpowiedź karty nie ma uciążliwości bariery przy wejściu zalecanym",
        )
    }

    /** Wiersz: dwie bariery przy jednym wejściu → wejście ma tę o najwyższej uciążliwości. */
    @Test
    fun keepsWorstBarrierOfEntrance() {
        val barriers = listOf(entranceBarrier("c", Severity.HINDRANCE), entranceBarrier("c", Severity.ONLY_WITH_HELP))
        val ordered = orderedEntrances(TWO_ENTRANCES, barriers)
        assertEquals(
            Severity.ONLY_WITH_HELP,
            ordered.first { it.entrance.id == "c" }.barrier?.severity,
            "Wejście nie ma bariery o najwyższej uciążliwości",
        )
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
        assertEquals(
            listOf("c"),
            ordered.filter { it.isRecommended }.map { it.entrance.id },
            "Zalecane nie jest następne wejście dostępne",
        )
        assertEquals(
            listOf(EntranceMarking.ACCESSIBLE, true),
            listOf(blocked.entrance.marking, blocked.isBlocked),
            "Wejście z blokadą zmieniło oznaczenie albo nie jest zablokowane",
        )
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
            "Karta nie mówi o blokadzie jedynego wejścia dostępnego",
        )
    }

    /**
     * Wiersz: „Utrudnienie” przy głównym, boczne bez barier → zalecane jest boczne, główne zostaje na liście.
     * Scenariusz (częściowo, przykład: Utrudnienie | żadnej | wejście boczne): przegladanie-mapy.feature: Wejście
     *   bez barier jest zalecane przed wejściem z barierą
     */
    @Test
    fun prefersEntranceWithoutBarrier() {
        val ordered = orderedEntrances(MAIN_AND_SIDE, listOf(entranceBarrier("b", Severity.HINDRANCE)))
        assertEquals(
            listOf("c" to true, "b" to false),
            ordered.map { it.entrance.id to it.isRecommended },
            "Zalecane nie jest wejście bez barier",
        )
    }

    /**
     * Wiersz: „Tylko z pomocą” przy głównym, „Utrudnienie” przy bocznym → zalecane jest boczne, z najlżejszą
     *   barierą.
     * Scenariusz (częściowo, przykład: Tylko z pomocą | przyjęta bariera „Utrudnienie” | wejście boczne, bo ma
     *   najlżejszą barierę): przegladanie-mapy.feature: Wejście bez barier jest zalecane przed wejściem z barierą
     */
    @Test
    fun prefersLightestBarrier() {
        val barriers = listOf(entranceBarrier("b", Severity.ONLY_WITH_HELP), entranceBarrier("c", Severity.HINDRANCE))
        assertEquals(
            listOf("c"),
            orderedEntrances(MAIN_AND_SIDE, barriers).filter { it.isRecommended }.map { it.entrance.id },
            "Zalecane nie jest wejście z najlżejszą barierą",
        )
    }

    /**
     * Wiersz: „Utrudnienie” przy obu → zalecane zostaje główne.
     * Scenariusz (częściowo, przykład: Utrudnienie | przyjęta bariera „Utrudnienie” | wejście główne):
     *   przegladanie-mapy.feature: Wejście bez barier jest zalecane przed wejściem z barierą
     */
    @Test
    fun keepsMainAtEqualBarriers() {
        val barriers = listOf(entranceBarrier("b", Severity.HINDRANCE), entranceBarrier("c", Severity.HINDRANCE))
        assertEquals(
            listOf("b"),
            orderedEntrances(MAIN_AND_SIDE, barriers).filter { it.isRecommended }.map { it.entrance.id },
            "Przy równych barierach zalecane nie jest wejście główne",
        )
    }
}

private val MAIN_AND_SIDE = listOf(sampleEntrance("b").copy(isMain = true), sampleEntrance("c"))
