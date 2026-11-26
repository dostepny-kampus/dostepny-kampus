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
import io.github.dostepnykampus.feature.inspection.navigation.ExportRoute
import io.github.dostepnykampus.feature.inspection.navigation.InvitationRoute
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
     * Wiersz: K1, „Otwórz”, C5.
     *
     * Wymagania: US11.3, RO-KOO
     */
    @Test
    fun openLeadsToMarkers() {
        navigator().openMarkers(SYMBOL)
        assertEquals(listOf(MapRoute(), AccountRoute, ProgressRoute, MarkersRoute(SYMBOL)), stack, "stos po „Otwórz”")
    }

    /**
     * „Zaproś osobę” na L2 prowadzi do K2, a „Wstecz” z K2 wraca do L2.
     *
     * Wymagania: US12.1, RO-KOO
     */
    @Test
    fun invitationOpensAndGoesBack() = opensAndGoesBack(InvitationRoute, AppNavigator::inspectionInvitation)

    /**
     * „Pobierz eksport” na L2 prowadzi do K3, a „Wstecz” z K3 wraca do L2.
     *
     * Wymagania: US13, RO-KOO
     */
    @Test
    fun exportOpensAndGoesBack() = opensAndGoesBack(ExportRoute, AppNavigator::inspectionExport)

    /**
     * „Wstecz” z C5 wraca do K1.
     *
     * Wiersz: C5, „Wstecz”, K1.
     *
     * Wymagania: US11.3, RO-KOO
     */
    @Test
    fun backFromMarkersToProgress() {
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
    fun openOutsideProgressIgnored() {
        stack.removeAt(stack.lastIndex)
        navigator().openMarkers(SYMBOL)
        assertEquals(listOf(MapRoute(), AccountRoute), stack, "stos bez K1")
    }

    private fun opensAndGoesBack(route: NavKey, open: (AppNavigator) -> Unit) {
        val navigator = AppNavigator(backStack = stack.apply { removeAt(lastIndex) }, roles = TestRoles(coordinator))
        open(navigator)
        assertEquals(listOf(MapRoute(), AccountRoute, route), stack, "trasa czynności Koordynatora")
        navigator.back()
        assertEquals(listOf(MapRoute(), AccountRoute), stack, "powrót na konto")
    }

    private companion object {
        const val SYMBOL = "A-1"
    }
}
