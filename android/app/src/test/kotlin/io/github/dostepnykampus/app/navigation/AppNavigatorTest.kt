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
import io.github.dostepnykampus.feature.capture.navigation.ReportStep
import io.github.dostepnykampus.feature.help.navigation.HelpRoute
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.EntrancesRoute
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Testy przejść; komentarz nad testem bez scenariusza podaje wiersz tabeli mapy nawigacji (ekran, czynność, AC/BR). */
class AppNavigatorTest {
    private val stack = mutableListOf<NavKey>(MapRoute())
    private val visitor = Account.SignedIn()
    private val restarts = mutableListOf<Int>()
    private var helpLeft = 0

    private fun navigator(account: Account = Account.Guest) =
        AppNavigator(backStack = stack, roles = { account }, onFirstHelpLeft = { helpLeft++ }, restarts = restarts)

    private fun stackOf(vararg routes: NavKey) = listOf<NavKey>(MapRoute(), *routes)

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

    // Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcję można otworzyć ponownie z pomocy
    @Test
    fun us5_1_first_launch_back_v1() {
        stack += startBackStack(firstLaunch = true).drop(1)
        navigator().back()
        assertEquals(stackOf(), stack)
        assertEquals(1, helpLeft)
    }

    // Wiersz: V5 przy pierwszym uruchomieniu, wyjście z instrukcji dowolną drogą, tu paskiem „Konto” → L1,
    // US5.1, decyzja 73.
    @Test
    fun us5_1_first_help_left_via_l1() {
        stack += startBackStack(firstLaunch = true).drop(1)
        navigator().run {
            open(TopLevel.ACCOUNT)
            assertEquals(1, helpLeft)
            backToMap()
        }
        assertEquals(1, helpLeft)
    }

    // Wiersz: pasek dolny, „Pomoc” → V5, US5.1: zamknięcie V5 otwartego z paska nie zmienia pierwszego startu.
    @Test
    fun us5_1_bar_help_left_keeps_flag() {
        navigator().run {
            open(TopLevel.HELP)
            open(TopLevel.ACCOUNT)
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

    // Wiersz: pasek dolny, ponowne dotknięcie wybranej pozycji → pierwszy ekran sekcji, NFR-15, decyzja 72.
    @Test
    fun nfr15_reselect_restarts_v5() {
        navigator(visitor).run {
            open(TopLevel.ACCOUNT)
            open(TopLevel.HELP)
            val keys = stack.indices.map(::contentKey)
            open(TopLevel.HELP)
            assertEquals(stackOf(AccountRoute, HelpRoute()), stack)
            assertEquals(keys.dropLast(1), stack.indices.map(::contentKey).dropLast(1))
            assertNotEquals(keys.last(), contentKey(index = 2))
            back()
        }
        assertEquals(stack.size, restarts.size)
    }

    // Wiersz: pasek dolny, ponowne dotknięcie „Mapa” → V1 bez arkusza i wyniku wyszukiwania, NFR-15, decyzja 72.
    @Test
    fun nfr15_reselect_map_restarts_v1() {
        navigator().run {
            val key = contentKey(index = 0)
            open(TopLevel.HELP)
            open(TopLevel.MAP)
            assertEquals(key, contentKey(index = 0))
            openBuilding(symbol = "C-13")
            open(TopLevel.MAP)
            assertNotEquals(key, contentKey(index = 0))
        }
        assertEquals(stackOf(), stack)
    }

    // Wiersz: każdy ekran poza V1, „Wstecz”, NFR-15: ten sam ekran dwa razy w historii ma dwa klucze treści.
    @Test
    fun nfr15_same_screen_twice_keys() {
        navigator(visitor).run {
            open(TopLevel.ACCOUNT)
            open(TopLevel.HELP)
            open(TopLevel.ACCOUNT)
            assertEquals(stackOf(AccountRoute, HelpRoute(), AccountRoute), stack)
            val keys = stack.indices.map(::contentKey)
            assertEquals(keys.distinct(), keys)
        }
    }

    // Wiersz: pasek dolny, „Zgłoś barierę” → V4, zalogowani, US4.
    @Test
    fun us4_bar_report_signed_in_v4() {
        navigator(visitor).open(TopLevel.REPORT)
        assertEquals(stackOf(ReportRoute()), stack)
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
        val expected = listOf(TopLevel.MAP, TopLevel.MAP, TopLevel.MAP, TopLevel.HELP, TopLevel.ACCOUNT)
        val routes = listOf(MapRoute(), BuildingRoute("A-1"), EntrancesRoute("A-1"), HelpRoute(), AccountRoute)
        assertEquals(expected, routes.map(::selectedItem))
        assertTrue(hasNavigationBar(AccountRoute))
        val noBar = listOf(SignInRoute(isForReport = false), ReportRoute(), ReportRoute(step = ReportStep.SENT))
        noBar.forEach { assertFalse(hasNavigationBar(it)) }
    }

    // Wiersz: V2, „Wstecz” → V1 z zaznaczonym budynkiem, US1.1.
    @Test
    fun us1_1_v2_back_v1_selected() {
        navigator().run {
            openBuilding(symbol = "C-13")
            back()
        }
        assertEquals(listOf(MapRoute(MapFocus.Building("C-13"))), stack)
    }

    // Wiersz: V1 z arkuszem V2, dotknięcie konturu innego budynku → V2 tego budynku zamiast poprzedniego,
    // „Wstecz” → V1, US2, decyzja 48.
    @Test
    fun us2_other_building_replaces_v2() {
        navigator().run {
            openBuilding(symbol = "C-13")
            openBuilding(symbol = "A-1")
            assertEquals(listOf(MapRoute(MapFocus.Building("A-1")), BuildingRoute("A-1")), stack)
            back()
        }
        assertEquals(listOf(MapRoute(MapFocus.Building("A-1"))), stack)
    }

    // Wiersze: V3, „Wstecz” → V2 i „Pokaż wejście na mapie” → V1 z zaznaczonym wejściem, US2.1, US3.
    // Scenariusz (częściowo): przegladanie-mapy.feature: Wejście dostępne najwyżej 3 dotknięcia od mapy
    @Test
    fun us3_2_v1_v2_v3_back_v2() {
        navigator().run {
            openBuilding(symbol = "A-1")
            openEntrances(symbol = "A-1")
            assertEquals(3, stack.size)
            back()
            assertEquals(BuildingRoute("A-1"), current)
            openEntrances(symbol = "A-1")
            showOnMap(MapFocus.Entrance("A-1-glowne"))
        }
        assertEquals(listOf(MapRoute(MapFocus.Entrance("A-1-glowne"))), stack)
    }

    // Wiersz: V4, „Tak, zrezygnuj” po „Wstecz” z kroku 1 → ekran sprzed V4, UC7 1b; poza V4 kroki nic nie robią.
    // Scenariusz (częściowo): zgloszenie-szybkie.feature: Zgłoszenie przy wejściu podpowiada wejście, a miejsce
    //   bierze z bieżącej pozycji
    @Test
    fun us4_6_report_at_entrance() {
        navigator(visitor).run {
            openBuilding(symbol = "A-1")
            report(entranceId = "A-1-glowne")
            assertEquals(ReportRoute(entranceId = "A-1-glowne"), current)
            leaveReport()
            nextReportStep()
            reportSent()
        }
        assertEquals(BuildingRoute("A-1"), stack.last())
    }

    // Scenariusz (częściowo): zgloszenie-szybkie.feature: Zgłoszenie szybkie w 3 krokach z pozycją dołączoną
    //   automatycznie
    @Test
    fun us4_1_three_steps_sent_p2() {
        navigator(visitor).run {
            openBuilding(symbol = "D-2")
            report()
            repeat(times = 3) { nextReportStep() }
            assertEquals(ReportStep.entries.dropLast(1), stack.drop(2).map { (it as ReportRoute).step })
            reportSent()
            assertEquals(listOf(BuildingRoute("D-2"), ReportRoute(step = ReportStep.SENT)), stack.drop(1))
            back()
        }
        assertEquals(BuildingRoute("D-2"), stack.last())
    }

    // Scenariusz (częściowo): zgloszenie-szybkie.feature: Gość po zalogowaniu wraca do zgłoszenia szybkiego
    @Test
    fun us14_2_guest_signed_in_v4() {
        navigator().run {
            openBuilding(symbol = "A-1")
            report(entranceId = "A-1-glowne")
            assertEquals(SignInRoute(isForReport = true, entranceId = "A-1-glowne"), current)
        }
        repeat(times = 2) { navigator(visitor).signedIn() }
        assertEquals(listOf(BuildingRoute("A-1"), ReportRoute(entranceId = "A-1-glowne")), stack.drop(1))
    }

    // Wiersz: strona logowania, wejście z „Konto”, zalogowanie → L2, UC6 krok 3, P3.
    @Test
    fun uc6_signed_in_from_account_l2() {
        navigator().open(TopLevel.ACCOUNT)
        navigator(visitor).signedIn()
        assertEquals(stackOf(AccountRoute), stack)
    }
}
