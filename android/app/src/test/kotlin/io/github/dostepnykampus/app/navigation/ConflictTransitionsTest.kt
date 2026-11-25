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
import io.github.dostepnykampus.feature.capture.navigation.MySubmissionsRoute
import io.github.dostepnykampus.feature.capture.navigation.VersionConflictRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Przejścia Zbierającego z listy własnych zgłoszeń do wyboru wersji i z powrotem.
 *
 * Wymagania: US9.3, BR-10
 */
class ConflictTransitionsTest {
    private val stack = mutableListOf<NavKey>(MapRoute(), AccountRoute, MySubmissionsRoute)
    private val collector = Account.SignedIn(setOf(StaffRole.COLLECTOR))

    private fun navigator() = AppNavigator(backStack = stack, roles = TestRoles(collector))

    /**
     * Znak „Dwie wersje: wybierz jedną” na C3 prowadzi do C4 tego zgłoszenia, a wyjście z C4 wraca do C3.
     *
     * Wiersze: C3, znak „Dwie wersje: wybierz jedną”, C4; C4, konflikt, wybór, C3.
     *
     * Wymagania: US9.3, BR-10, RO-ZBI
     */
    @Test
    fun conflictOpensAndGoesBack() {
        val navigator = navigator()
        navigator.openConflict(ID)
        assertEquals(listOf(MapRoute(), AccountRoute, MySubmissionsRoute, VersionConflictRoute(ID)), stack, "stos C4")
        navigator.back()
        assertEquals(listOf(MapRoute(), AccountRoute, MySubmissionsRoute), stack, "powrót do C3")
    }

    /** Poza C3 wybór wersji się nie otwiera. Wymagania: US9.3 */
    @Test
    fun conflictOnlyFromOwnList() {
        stack.removeAt(stack.lastIndex)
        navigator().openConflict(ID)
        assertEquals(listOf(MapRoute(), AccountRoute), stack, "stos bez C3")
    }

    private companion object {
        const val ID = "s-dwie-wersje"
    }
}
