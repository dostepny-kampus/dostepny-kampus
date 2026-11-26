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
import io.github.dostepnykampus.feature.capture.navigation.SyncRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** C4 bez konfliktu: wejście z C3, powrót i ponowne logowanie. Wymagania: US9.1, US9.4, RO-ZBI */
class SyncTransitionsTest {
    private val stack = mutableListOf<NavKey>(MapRoute(), AccountRoute, MySubmissionsRoute)
    private val collector = Account.SignedIn(setOf(StaffRole.COLLECTOR))

    private fun navigator() = AppNavigator(backStack = stack, roles = TestRoles(collector))

    /** Wiersz: C3 „Czeka na wysłanie: N” → C4, „Wstecz” → to samo C3. Wymagania: US9.1 */
    @Test
    fun syncOpensAndGoesBack() {
        val navigator = navigator()
        navigator.openSync()
        assertEquals(listOf(MapRoute(), AccountRoute, MySubmissionsRoute, SyncRoute), stack, "stos C4")
        navigator.back()
        assertEquals(listOf(MapRoute(), AccountRoute, MySubmissionsRoute), stack, "powrót do C3")
    }

    /** Poza C3 C4 się nie otwiera. Wymagania: US9.1 */
    @Test
    fun syncOnlyFromOwnList() {
        stack.removeAt(stack.lastIndex)
        navigator().openSync()
        assertEquals(listOf(MapRoute(), AccountRoute), stack, "stos bez C3")
    }

    /** Wiersz: C4 przy wygasłym zalogowaniu, „Zaloguj się” → L1. Wymagania: US9.4, US14.4 */
    @Test
    fun signsInAgainFromSync() {
        val navigator = navigator()
        navigator.openSync()
        navigator.signInAgain()
        assertEquals(SignInRoute(isForReport = false), stack.last(), "L1 po „Zaloguj się”")
    }
}
