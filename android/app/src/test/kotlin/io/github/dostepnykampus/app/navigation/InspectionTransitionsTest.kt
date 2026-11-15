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
import io.github.dostepnykampus.feature.inspection.navigation.MarkersRoute
import io.github.dostepnykampus.feature.inspection.navigation.ProgressRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Przejścia Koordynatora z postępu inwentaryzacji do znaczników budynku i z powrotem.
 *
 * Wymagania: US11.1, US11.3
 */
class InspectionTransitionsTest {
    private val stack = mutableListOf<NavKey>(MapRoute(), AccountRoute, ProgressRoute)
    private val coordinator = Account.SignedIn(setOf(StaffRole.COORDINATOR))

    private fun navigator() = AppNavigator(backStack = stack, roles = TestRoles(coordinator))

    /**
     * „Otwórz” przy budynku w K1 prowadzi do C5 tego budynku.
     *
     * Wiersz: K1, „Otwórz”, C5, RO-KOO.
     *
     * Wymagania: US11.3
     */
    @Test
    fun openLeadsToMarkers() {
        navigator().openMarkers(SYMBOL)
        assertEquals(listOf(MapRoute(), AccountRoute, ProgressRoute, MarkersRoute(SYMBOL)), stack, "stos po „Otwórz”")
    }

    /**
     * „Wstecz” z C5 wraca do K1.
     *
     * Wiersz: C5, „Wstecz”, K1, RO-KOO.
     *
     * Wymagania: US11.3
     */
    @Test
    fun backFromMarkersReturnsToProgress() {
        navigator().run {
            openMarkers(SYMBOL)
            back()
        }
        assertEquals(listOf(MapRoute(), AccountRoute, ProgressRoute), stack, "stos po „Wstecz”")
    }

    /**
     * „Otwórz” poza K1 niczego nie zmienia.
     *
     * Wymagania: US11.3
     */
    @Test
    fun openOutsideProgressChangesNothing() {
        stack.removeAt(stack.lastIndex)
        navigator().openMarkers(SYMBOL)
        assertEquals(listOf(MapRoute(), AccountRoute), stack, "stos bez K1")
    }

    private companion object {
        const val SYMBOL = "A-1"
    }
}
