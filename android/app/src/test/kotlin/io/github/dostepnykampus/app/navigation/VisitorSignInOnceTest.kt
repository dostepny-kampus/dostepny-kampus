/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.TestRoles
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportStep
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Przejścia zalogowanego Odwiedzającego do zgłoszenia: logowanie wraca dopiero po wylogowaniu. */
class VisitorSignInOnceTest {
    /**
     * Zalogowany Odwiedzający od razu otwiera aparat, a po wylogowaniu zgłoszenie prosi o logowanie.
     * Scenariusz (częściowo): uprawnienia.feature: Zalogowany Odwiedzający nie jest proszony o logowanie ponownie
     *
     * Wymagania: US14.4
     */
    @Test
    fun askSignInOnlyAfterLogout() {
        val roles = TestRoles(Account.SignedIn())
        val stack = startBackStack(firstLaunch = false).toMutableList()
        val navigator = AppNavigator(backStack = stack, roles = roles)
        navigator.report()
        assertEquals(
            ReportRoute(step = ReportStep.PHOTO),
            stack.last(),
            "zalogowany Odwiedzający od razu otwiera aparat",
        )
        navigator.backToMap()
        roles.account.value = Account.Guest
        navigator.report()
        assertEquals(
            SignInRoute(isForReport = true),
            stack.last(),
            "po wylogowaniu zgłoszenie prosi o logowanie",
        )
    }

    /**
     * „Wyloguj się” na L2 otwiera V1 Gościa, a zgłoszenie znów prosi o logowanie.
     * Scenariusz (częściowo): uprawnienia.feature: Zalogowany Odwiedzający nie jest proszony o logowanie ponownie
     *
     * Wymagania: US14.4
     */
    @Test
    fun signOutAsksSignInAgain() {
        val roles = TestRoles(Account.SignedIn())
        val stack = mutableListOf<NavKey>(MapRoute(), AccountRoute)
        val navigator = AppNavigator(backStack = stack, roles = roles)
        navigator.signOut()
        assertEquals(Account.Guest, roles.account.value, "wylogowanie zmienia konto na Gościa")
        assertEquals(listOf<NavKey>(MapRoute()), stack, "po wylogowaniu otwiera się V1")
        navigator.report()
        assertEquals(SignInRoute(isForReport = true), stack.last(), "po wylogowaniu zgłoszenie prosi o logowanie")
    }
}
