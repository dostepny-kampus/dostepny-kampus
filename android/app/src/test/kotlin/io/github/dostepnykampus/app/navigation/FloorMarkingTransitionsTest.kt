/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.TestRoles
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.feature.inspection.navigation.CollectorMarkersRoute
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Przejścia Zbierającego z karty budynku V2 do C5 „Obejrzane piętra” i z powrotem. Wymagania: US15.1 */
class FloorMarkingTransitionsTest {
    /** Wiersz: V2, „Oznacz obejrzane piętra”, C5; „Wstecz” z C5 wraca do V2. Wymagania: US15.1, RO-ZBI */
    @Test
    fun opensMarkingFromCard() {
        val stack = mutableListOf<NavKey>(MapRoute(), BuildingRoute("A-1"))
        collector(stack).run {
            openFloorMarking("A-1")
            assertEquals(CollectorMarkersRoute("A-1"), current, "V2 otwiera C5")
            back()
            assertEquals(BuildingRoute("A-1"), current, "„Wstecz” wraca do V2")
        }
    }

    /** „Oznacz obejrzane piętra” poza V2 niczego nie zmienia. Wymagania: US15.1 */
    @Test
    fun ignoresMarkingOutsideCard() {
        val stack = mutableListOf<NavKey>(MapRoute())
        collector(stack).openFloorMarking("A-1")
        assertEquals(listOf<NavKey>(MapRoute()), stack, "stos bez zmian")
    }

    private fun collector(stack: MutableList<NavKey>) =
        AppNavigator(backStack = stack, roles = TestRoles(Account.SignedIn(setOf(StaffRole.COLLECTOR))))
}
