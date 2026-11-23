/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import io.github.dostepnykampus.app.TestRoles
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportStep
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
}
