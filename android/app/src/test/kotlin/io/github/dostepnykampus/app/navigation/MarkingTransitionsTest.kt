/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.TestRoles
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.feature.capture.navigation.MarkingRoute
import io.github.dostepnykampus.feature.capture.navigation.MySubmissionsRoute
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Przejścia Zbierającego z karty budynku V2 do C6 „Zmiana oznaczenia wejścia” i dalej do C3. Wymagania: US17.2 */
class MarkingTransitionsTest {
    private val stack = mutableListOf<NavKey>(MapRoute(), BuildingRoute("B-8"))

    /**
     * Wiersz: V2 „Zmień oznaczenie” → C6 tego wejścia, „Wyślij do sprawdzenia” → C3 bez C6 w historii.
     *
     * Wymagania: US17.2, NFR-54
     */
    @Test
    fun savedMarkingOpensC3() {
        collector().run {
            openMarking(MarkingRoute(entranceId = "n/1", buildingSymbol = "B-8"))
            assertEquals(MarkingRoute(entranceId = "n/1", buildingSymbol = "B-8"), current, "V2 otwiera C6")
            reportSaved()
        }
        val expected = listOf(MapRoute(), BuildingRoute("B-8"), MySubmissionsRoute)
        assertEquals(expected, stack, "C6 znika z historii, „Wstecz” z C3 wraca do V2")
    }

    /**
     * Wiersz: C3 „Otwórz” przy zmianie oznaczenia wejścia → C6 tej zmiany, zapis → to samo C3.
     *
     * Wymagania: US8.3, US17.2, NFR-54
     */
    @Test
    fun editsMarkingFromList() {
        stack += MySubmissionsRoute
        val route = MarkingRoute(entranceId = "n/1", buildingSymbol = "B-8", submissionId = "m-1")
        collector().run {
            openMarking(route)
            assertEquals(route, current, "C3 otwiera C6 zmiany")
            reportSaved()
        }
        assertEquals(listOf(MapRoute(), BuildingRoute("B-8"), MySubmissionsRoute), stack, "C6 znika, zostaje C3")
    }

    /** Wiersz: C6 zmiany otwiera się tylko z C3. Wymagania: US8.3 */
    @Test
    fun listMarkingOnlyFromList() {
        collector().openMarking(MarkingRoute(entranceId = "n/1", buildingSymbol = "B-8", submissionId = "m-1"))
        assertEquals(listOf(MapRoute(), BuildingRoute("B-8")), stack, "poza C3 bez zmiany")
    }

    /** Wiersz: „Wstecz” z C6 wraca do V2. Wymagania: US17.2 */
    @Test
    fun backFromMarkingToCard() {
        collector().run {
            openMarking(MarkingRoute(entranceId = "n/1", buildingSymbol = "B-8"))
            back()
            assertEquals(BuildingRoute("B-8"), current, "„Wstecz” wraca do V2")
        }
    }

    /** „Zmień oznaczenie” poza V2 niczego nie zmienia. Wymagania: US17.2 */
    @Test
    fun ignoresMarkingOutsideCard() {
        stack.removeAt(stack.lastIndex)
        collector().openMarking(MarkingRoute(entranceId = "n/1", buildingSymbol = "B-8"))
        assertEquals(listOf<NavKey>(MapRoute()), stack, "stos bez zmian")
    }

    private fun collector() =
        AppNavigator(backStack = stack, roles = TestRoles(Account.SignedIn(setOf(StaffRole.COLLECTOR))))
}
