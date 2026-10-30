/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.feature.auth.navigation.AccountRoute
import io.github.dostepnykampus.feature.auth.navigation.SignInRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.help.navigation.HelpRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Testy przejść; komentarz nad testem bez scenariusza podaje wiersz tabeli mapy nawigacji (ekran, czynność, AC/BR). */
class AppNavigatorTest {
    private val stack = mutableListOf<NavKey>(MapRoute)
    private val visitor = Account.SignedIn()
    private var helpLeft = 0

    private fun navigator(account: Account = Account.Guest) =
        AppNavigator(backStack = stack, roles = { account }, onFirstHelpLeft = { helpLeft++ })

    private fun stackOf(vararg routes: NavKey) = listOf<NavKey>(MapRoute, *routes)

    // Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcja przy pierwszym uruchomieniu
    @Test
    fun us5_1_first_launch_opens_v5() {
        assertEquals(stackOf(HelpRoute(isFirstLaunch = true)), startBackStack(firstLaunch = true))
    }

    // Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcję można otworzyć ponownie z pomocy
    @Test
    fun us5_1_next_launch_opens_v1() {
        assertEquals(stackOf(), startBackStack(firstLaunch = false))
    }

    // Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcja przy pierwszym uruchomieniu
    @Test
    fun us5_1_first_launch_back_v1() {
        stack += startBackStack(firstLaunch = true).drop(1)
        navigator().back()
        assertEquals(stackOf(), stack)
        assertEquals(1, helpLeft)
    }

    // Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcję można otworzyć ponownie z pomocy
    @Test
    fun us5_1_first_help_left_via_l1() {
        stack += startBackStack(firstLaunch = true).drop(1)
        navigator().run {
            open(TopLevel.ACCOUNT)
            assertEquals(0, helpLeft)
            backToMap()
        }
        assertEquals(1, helpLeft)
    }

    // Wiersz: pasek dolny, „Pomoc” → V5, US5.1: zamknięcie V5 otwartego z paska nie zmienia pierwszego startu.
    @Test
    fun us5_1_bar_help_left_keeps_flag() {
        navigator().run {
            open(TopLevel.HELP)
            back()
        }
        assertEquals(0, helpLeft)
    }

    // Scenariusz (częściowo): zgloszenie-szybkie.feature: Gość rezygnuje z logowania
    @Test
    fun uc7_1b_guest_not_now_v1() {
        navigator().run {
            open(TopLevel.REPORT)
            backToMap()
        }
        assertEquals(stackOf(), stack)
    }

    // Wiersz: pasek dolny (V1, V5, L2), „Mapa” → V1, US1.
    @Test
    fun us1_bar_map_returns_to_v1() {
        stack += listOf(HelpRoute(), AccountRoute)
        navigator().open(TopLevel.MAP)
        assertEquals(stackOf(), stack)
    }

    // Wiersz: pasek dolny, „Zgłoś barierę” → V4, zalogowani, US4.
    @Test
    fun us4_bar_report_signed_in_v4() {
        navigator(visitor).open(TopLevel.REPORT)
        assertEquals(stackOf(ReportRoute), stack)
    }

    // Scenariusz (częściowo): zgloszenie-szybkie.feature: Gość niezalogowany jest proszony o logowanie przed zdjęciem
    @Test
    fun us14_2_bar_report_guest_l1() {
        navigator().open(TopLevel.REPORT)
        assertEquals(stackOf(SignInRoute(isForReport = true)), stack)
    }

    // Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcję można otworzyć ponownie z pomocy
    @Test
    fun us5_1_bar_help_v5_back_returns() {
        navigator(visitor).run {
            open(TopLevel.ACCOUNT)
            repeat(times = 2) { open(TopLevel.HELP) }
            assertEquals(stackOf(AccountRoute, HelpRoute()), stack)
            back()
        }
        assertEquals(stackOf(AccountRoute), stack)
    }

    // Wiersz: pasek dolny, „Konto” → L1, RO-GOS, US14.
    @Test
    fun us14_bar_account_guest_l1() {
        navigator().open(TopLevel.ACCOUNT)
        assertEquals(stackOf(SignInRoute(isForReport = false)), stack)
    }

    // Wiersze: pasek dolny, „Konto” → L2, RO-ODW i „Konto i praca” → L2, role pracownicze, US14.1, P1.
    @Test
    fun us14_1_bar_account_l2() {
        listOf(visitor, Account.SignedIn(setOf(StaffRole.VERIFIER))).forEach { account ->
            navigator(account).run {
                backToMap()
                open(TopLevel.ACCOUNT)
            }
            assertEquals(stackOf(AccountRoute), stack)
        }
    }

    // Wiersz: każdy ekran poza V1, „Wstecz”, NFR-15: V1 jest na dole historii, wstecz zamyka aplikację.
    @Test
    fun nfr15_back_on_v1_closes_app() {
        assertFalse(navigator().back())
        assertEquals(stackOf(), stack)
    }

    // Wymagania wspólne, wiersz „Pasek dolny na V1, V2, V3, V5 i L2”: L1 i V4 bez paska.
    @Test
    fun us14_1_bar_only_on_bar_screens() {
        val expected = listOf(TopLevel.MAP, TopLevel.HELP, TopLevel.ACCOUNT)
        assertEquals(expected, listOf(MapRoute, HelpRoute(), AccountRoute).map(::selectedItem))
        assertTrue(hasBottomBar(AccountRoute))
        listOf(SignInRoute(isForReport = false), ReportRoute).forEach { assertFalse(hasBottomBar(it)) }
    }
}
