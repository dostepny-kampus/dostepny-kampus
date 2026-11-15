/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Tekst pięter z windą i toaletą dostosowaną w grupie „Winda i toaleta”. Wymagania: US16.3 */
class LiftToiletTextTest {
    @Test
    fun namesRangeOnEveryFloor() {
        val presence = FloorPresence(floors = listOf(one("Piwnica"), one("Parter"), one("3 piętro")), emptyList())
        assertEquals(
            "Na każdym piętrze: Piwnica – 3 piętro.",
            presenceText(presence, WORDS),
            "Winda na każdym piętrze nie dała zakresu od najniższego do najwyższego",
        )
    }

    @Test
    fun listsFloorsWithCountAndMissing() {
        val presence =
            FloorPresence(
                floors = listOf(one("Piwnica"), Counted("1 piętro", 2), one("2 piętro")),
                missing = listOf("Parter", "3 piętro"),
            )
        assertEquals(
            "Na piętrach: Piwnica, 1 piętro (2) i 2 piętro. Nie ma na piętrach: Parter i 3 piętro.",
            presenceText(presence, WORDS),
            "Tekst nie podał pięter z liczbą i pięter bez toalety",
        )
    }

    private fun one(name: String) = Counted(name = name, count = 1)

    private companion object {
        val WORDS =
            PresenceWords(
                onFloors = "Na piętrach: %1\$s.",
                everywhere = "Na każdym piętrze: %1\$s – %2\$s.",
                missing = "Nie ma na piętrach: %1\$s.",
                and = "i",
            )
    }
}
