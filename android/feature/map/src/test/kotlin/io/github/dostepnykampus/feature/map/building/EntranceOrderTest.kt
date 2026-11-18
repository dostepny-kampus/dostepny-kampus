/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Kolejność wejść po wejściu zalecanym: grupy oznaczeń, w grupie nazwy z danych alfabetycznie po polsku z liczbami
 * według wartości, a na końcu grupy wejścia z nazwą z importu w kolejności z danych.
 *
 * Wymagania: BR-25, US2.1
 */
class EntranceOrderTest {
    /**
     * Wiersz: V2, grupa „Wejścia”: wejścia „niedostępne” z nazwą alfabetycznie, wejście bez nazwy w danych po nich.
     * Scenariusz: przegladanie-mapy.feature: Wejścia z jednym oznaczeniem stoją na karcie alfabetycznie
     */
    @Test
    fun sortsNamesInsideMarking() {
        val ordered =
            orderedEntrances(
                listOf(
                    entrance(name = "Wejście \"E\"", marking = EntranceMarking.INACCESSIBLE),
                    entrance(name = "Wejście od wschodu", marking = EntranceMarking.INACCESSIBLE, isGenerated = true),
                    entrance(name = "Wejście \"A\"", marking = EntranceMarking.INACCESSIBLE),
                    entrance(name = "Wejście \"C\"", marking = EntranceMarking.INACCESSIBLE),
                ),
            )
        assertEquals(
            listOf("Wejście \"A\"", "Wejście \"C\"", "Wejście \"E\"", "Wejście od wschodu"),
            ordered.map { it.entrance.name },
            "nazwy alfabetycznie, wejście z nazwą z importu na końcu",
        )
    }

    /** Wiersz: V2, grupa „Wejścia”: grupy oznaczeń zostają, a w każdej kolejność według nazw. */
    @Test
    fun keepsMarkingGroups() {
        val ordered =
            orderedEntrances(
                listOf(
                    entrance(name = "Brama", marking = EntranceMarking.NO_DATA),
                    entrance(name = "Tył", marking = EntranceMarking.INACCESSIBLE),
                    entrance(name = "Schody", marking = EntranceMarking.INACCESSIBLE),
                    entrance(name = "Arkady", marking = EntranceMarking.NO_DATA),
                ),
            )
        assertEquals(listOf("Schody", "Tył", "Arkady", "Brama"), ordered.map { it.entrance.name }, "grupy oznaczeń")
    }

    /** Wiersz: liczby w nazwie według wartości, polskie litery na swoim miejscu alfabetu. */
    @Test
    fun sortsNumbersAndPolish() {
        val names = listOf("Wejście 10", "Wejście Ł", "Wejście 2", "Wejście M", "Wejście L")
        assertEquals(
            listOf("Wejście 2", "Wejście 10", "Wejście L", "Wejście Ł", "Wejście M"),
            names.sortedWith(EntranceNameOrder),
            "liczby według wartości, Ł między L i M",
        )
    }

    /** Wiersz: dwa wejścia bez nazwy w danych zostają w kolejności z danych, za wejściami z nazwą. */
    @Test
    fun keepsDataOrderWithoutNames() {
        val ordered =
            orderedEntrances(
                listOf(
                    entrance(name = "Wejście od zachodu", marking = EntranceMarking.NO_DATA, isGenerated = true),
                    entrance(name = null, marking = EntranceMarking.NO_DATA),
                    entrance(name = "Brama", marking = EntranceMarking.NO_DATA),
                ),
            )
        val names = ordered.map { it.entrance.name }
        assertEquals(listOf("Brama", "Wejście od zachodu", null), names, "bez nazw w kolejności danych")
    }

    private fun entrance(name: String?, marking: EntranceMarking, isGenerated: Boolean = false) =
        Entrance(
            id = name.orEmpty(),
            buildingSymbol = "C-13",
            name = name,
            isMain = false,
            type = "yes",
            location = GeoPoint(longitude = 0.0, latitude = 0.0),
            marking = marking,
            isNameGenerated = isGenerated,
        )
}
