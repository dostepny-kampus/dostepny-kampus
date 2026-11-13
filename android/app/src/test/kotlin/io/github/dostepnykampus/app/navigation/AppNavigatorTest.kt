/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.TestRoles
import io.github.dostepnykampus.app.closeCardOnEmptyMap
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.feature.capture.navigation.MeasureRoute
import io.github.dostepnykampus.feature.capture.navigation.MySubmissionsRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportEditRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute
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
    private val collector = Account.SignedIn(setOf(StaffRole.COLLECTOR))
    private val restarts = mutableListOf<Int>()
    private var helpLeft = 0
    private var mapChosen = 0

    private fun navigator(account: Account = Account.Guest) =
        AppNavigator(
            backStack = stack,
            roles = TestRoles(account),
            onFirstHelpLeft = { helpLeft++ },
            restarts = restarts,
        )

    private fun mapChoiceNavigator() =
        AppNavigator(backStack = stack, roles = TestRoles(), restarts = restarts, onChooseMap = { mapChosen++ })

    private fun stackOf(vararg routes: NavKey) = listOf<NavKey>(MapRoute(), *routes)

    /** Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcja przy pierwszym uruchomieniu. */
    @Test
    fun us5_1_first_launch_opens_v5() {
        assertEquals(stackOf(HelpRoute(isFirstLaunch = true)), startBackStack(firstLaunch = true))
    }

    /** Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcję można otworzyć ponownie z pomocy. */
    @Test
    fun us5_1_next_launch_opens_v1() {
        assertEquals(stackOf(), startBackStack(firstLaunch = false))
    }

    /** Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcję można otworzyć ponownie z pomocy. */
    @Test
    fun us5_1_first_launch_back_v1() {
        stack += startBackStack(firstLaunch = true).drop(1)
        navigator().back()
        assertEquals(stackOf(), stack)
        assertEquals(1, helpLeft)
    }

    /**
     * Wiersz: V5 przy pierwszym uruchomieniu, wyjście z instrukcji dowolną drogą, tu paskiem „Konto” → L1.
     *
     * Wymagania: US5.1
     */
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

    /**
     * Wiersz: pasek dolny, „Pomoc” → V5: zamknięcie V5 otwartego z paska nie zmienia pierwszego startu.
     *
     * Wymagania: US5.1
     */
    @Test
    fun us5_1_bar_help_left_keeps_flag() {
        navigator().run {
            open(TopLevel.HELP)
            open(TopLevel.ACCOUNT)
            back()
        }
        assertEquals(0, helpLeft)
    }

    /** Scenariusz (częściowo): zgloszenie-szybkie.feature: Gość rezygnuje z logowania. */
    @Test
    fun uc7_1b_guest_not_now_v1() {
        navigator().run {
            open(TopLevel.REPORT)
            backToMap()
        }
        assertEquals(stackOf(), stack)
    }

    /** Wiersz: pasek dolny (V1, V5, L2), „Mapa” → V1. Wymagania: US1 */
    @Test
    fun us1_bar_map_returns_to_v1() {
        stack += listOf(HelpRoute(), AccountRoute)
        navigator().open(TopLevel.MAP)
        assertEquals(stackOf(), stack)
    }

    /** Wiersz: pasek dolny, ponowne dotknięcie wybranej pozycji → pierwszy ekran sekcji. Wymagania: NFR-15 */
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

    /** Wiersz: pasek dolny, ponowne dotknięcie „Mapa” → V1 bez arkusza i wyniku wyszukiwania. Wymagania: NFR-15 */
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

    /**
     * Wiersz: każdy ekran poza V1, „Wstecz”: ten sam ekran dwa razy w historii ma dwa klucze treści.
     *
     * Wymagania: NFR-15
     */
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

    /** Wiersz: pasek dolny, „Zgłoś barierę” → V4, zalogowani. Wymagania: US4 */
    @Test
    fun us4_bar_report_signed_in_v4() {
        navigator(visitor).open(TopLevel.REPORT)
        assertEquals(stackOf(ReportRoute()), stack)
    }

    /**
     * Scenariusz (częściowo): zgloszenie-szybkie.feature: Gość niezalogowany jest proszony o logowanie przed zdjęciem.
     */
    @Test
    fun us14_2_bar_report_guest_l1() {
        navigator().open(TopLevel.REPORT)
        assertEquals(stackOf(SignInRoute(isForReport = true)), stack)
    }

    /** Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcję można otworzyć ponownie z pomocy. */
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

    /** Wiersz: pasek dolny, „Konto” → L1. Wymagania: RO-GOS, US14 */
    @Test
    fun us14_bar_account_guest_l1() {
        navigator().open(TopLevel.ACCOUNT)
        assertEquals(stackOf(SignInRoute(isForReport = false)), stack)
    }

    /** Wiersze: pasek dolny, „Konto” → L2, RO-ODW i „Konto i praca” → L2, role pracownicze, P1. Wymagania: US14.1 */
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

    /** Wiersz: każdy ekran poza V1, „Wstecz”: V1 jest na dole historii, wstecz zamyka aplikację. Wymagania: NFR-15 */
    @Test
    fun nfr15_back_on_v1_closes_app() {
        assertFalse(navigator().back())
        assertEquals(stackOf(), stack)
    }

    /** Wymagania wspólne, wiersz „Pasek dolny na V1, V2, V3, V5 i L2”: L1 i V4 bez paska. */
    @Test
    fun us14_1_bar_only_on_bar_screens() {
        val expected = listOf(TopLevel.MAP, TopLevel.MAP, TopLevel.MAP, TopLevel.HELP, TopLevel.ACCOUNT)
        val routes = listOf(MapRoute(), BuildingRoute("A-1"), EntrancesRoute("A-1"), HelpRoute(), AccountRoute)
        assertEquals(expected, routes.map(::selectedItem))
        assertTrue(hasNavigationBar(AccountRoute))
        val noBar = listOf(SignInRoute(isForReport = false), ReportRoute(), ReportRoute(step = ReportStep.SENT))
        noBar.forEach { assertFalse(hasNavigationBar(it)) }
    }

    /** Wiersz: mała karta V2, „Wstecz” → V1 bez zaznaczenia budynku. Wymagania: US2.8 */
    @Test
    fun us2_8_v2_back_v1_unselected() {
        navigator().run {
            openBuilding(symbol = "C-13")
            back()
        }
        assertEquals(listOf(MapRoute()), stack)
    }

    /**
     * Wiersz: V1 z arkuszem V2, dotknięcie konturu innego budynku → V2 tego budynku zamiast poprzedniego,
     * „Wstecz” → V1 bez zaznaczenia budynku.
     *
     * Wymagania: US2.8
     */
    @Test
    fun us2_other_building_replaces_v2() {
        navigator().run {
            openBuilding(symbol = "C-13")
            openBuilding(symbol = "A-1")
            assertEquals(listOf(MapRoute(MapFocus.Building("A-1")), BuildingRoute("A-1")), stack)
            back()
        }
        assertEquals(listOf(MapRoute()), stack)
    }

    /**
     * Wiersz: mała karta V2 nad V1, dotknięcie pustej mapy → V1 bez zaznaczenia budynku;
     * samo V1, także z budynkiem zaznaczonym przez „Pokaż na mapie”, i V3 nad V2 zostają.
     *
     * Wymagania: US2.8, NFR-17
     */
    @Test
    fun us2_8_empty_map_closes_v2() {
        val selected = MapRoute(MapFocus.Building("A-1"))
        navigator().run {
            showOnMap(MapFocus.Building("A-1"))
            closeCardOnEmptyMap()
            assertEquals(listOf(selected), stack)
            openBuilding(symbol = "A-1")
            openEntrances(symbol = "A-1")
            closeCardOnEmptyMap()
            assertEquals(listOf(selected, BuildingRoute("A-1"), EntrancesRoute("A-1")), stack)
            back()
            closeCardOnEmptyMap()
        }
        assertEquals(listOf(MapRoute()), stack)
    }

    /**
     * Wiersz: V3, „Wstecz” → pełny ekran V2, a pod nim V1 dalej z zaznaczonym budynkiem; zaznaczenie znika dopiero
     * z zamknięciem karty.
     *
     * Wymagania: US2.8
     */
    @Test
    fun us2_8_v3_back_v1_selected() {
        navigator().run {
            openBuilding(symbol = "A-1")
            openEntrances(symbol = "A-1")
            back()
        }
        assertEquals(listOf(MapRoute(MapFocus.Building("A-1")), BuildingRoute("A-1")), stack)
    }

    /**
     * Wiersze: V3, „Wstecz” → V2 i „Pokaż wejście na mapie” → V1 z zaznaczonym wejściem.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Wejście dostępne najwyżej 3 dotknięcia od mapy
     *
     * Wymagania: US2.1, US3
     */
    @Test
    fun us3_2_v1_v2_v3_back_v2() {
        navigator().run {
            openBuilding(symbol = "A-1")
            openEntrances(symbol = "A-1")
            assertEquals(3, stack.size)
            back()
            assertEquals(BuildingRoute("A-1"), current)
            openEntrances(symbol = "A-1")
            showOnMap(MapFocus.Entrance(id = "A-1-glowne", buildingSymbol = "A-1"))
        }
        assertEquals(listOf(MapRoute(MapFocus.Entrance(id = "A-1-glowne", buildingSymbol = "A-1"))), stack)
    }

    /**
     * Wiersze: V1, dotknięcie budynku na liście przy zwykłej czcionce → V1 w widoku mapy z małą kartą V2; mała karta,
     * „Wstecz” albo dotknięcie pustej mapy → V1 bez zaznaczenia, zapamiętany wybór „Lista budynków” bez zmian.
     *
     * Wymagania: US2.8, NFR-19
     */
    @Test
    fun nfr19_list_compact_keeps_list() {
        mapChoiceNavigator().run {
            openBuildingFromList("A-1", false)
            assertEquals(listOf(MapRoute(MapFocus.Building("A-1")), BuildingRoute("A-1")), stack)
            back()
            assertEquals(listOf(MapRoute()), stack)
            openBuildingFromList("A-1", false)
            closeCardOnEmptyMap()
        }
        assertEquals(listOf(MapRoute()), stack)
        assertEquals(0, mapChosen)
    }

    /**
     * Wiersze: V1, dotknięcie budynku na liście przy TalkBack → V2, pełny ekran od razu;
     * V2 otwarte od razu, „Wstecz” → lista V1 jednym krokiem, wybór bez zmian.
     *
     * Wymagania: US3.2, NFR-19, NFR-15, US2.9
     */
    @Test
    fun nfr19_talkback_list_tap_direct() {
        mapChoiceNavigator().run {
            openBuildingFromList("A-1", true)
            assertEquals(BuildingRoute("A-1", isDirect = true), current)
            assertEquals(2, stack.size)
            assertTrue(back())
        }
        assertEquals(listOf(MapRoute()), stack)
        assertEquals(0, mapChosen)
    }

    /**
     * Wiersz: V2 otwarte od razu, „Pokaż budynek na mapie” → V1 w widoku mapy z zaznaczonym budynkiem, bez karty,
     * zapamiętany wybór „Mapa”.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Z listy przy dużej czcionce opis budynku prowadzi do mapy
     *
     * Wymagania: US2.9, NFR-19
     */
    @Test
    fun us2_9_show_on_map_chooses_map() {
        mapChoiceNavigator().run {
            openBuildingFromList("A-1", true)
            showOnMap(MapFocus.Building("A-1"))
        }
        assertEquals(listOf(MapRoute(MapFocus.Building("A-1"))), stack)
        assertEquals(1, mapChosen)
    }

    /**
     * Wiersze: V3, „Pokaż wejście na mapie” → V1 z zaznaczonym wejściem w widoku „Mapa”;
     * pasek, ponowne „Mapa” → V1, nie zmienia wyboru drugi raz.
     *
     * Wymagania: US2.1, NFR-15
     */
    @Test
    fun us2_1_entrance_chooses_map() {
        mapChoiceNavigator().run {
            showOnMap(MapFocus.Entrance(id = "A-1-glowne", buildingSymbol = "A-1"))
            open(TopLevel.MAP)
        }
        assertEquals(1, mapChosen)
    }

    /**
     * Wiersz: V4, „Tak, zrezygnuj” po „Wstecz” z kroku 1 → ekran sprzed V4; poza V4 kroki nic nie robią.
     * Scenariusz (częściowo): zgloszenie-szybkie.feature: Zgłoszenie przy wejściu podpowiada wejście, a miejsce
     *   bierze z bieżącej pozycji
     *
     * Wymagania: UC7
     */
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

    /**
     * Scenariusz (częściowo): zgloszenie-szybkie.feature: Zgłoszenie szybkie w 3 krokach z pozycją dołączoną
     *   automatycznie.
     */
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

    /** Wysłanie bez internetu: potwierdzenie mówi, że zdjęcie wyśle się samo. Wymagania: US4.2 */
    @Test
    fun us4_2_sent_offline() {
        navigator(visitor).run {
            report()
            reportSent(isSavedOffline = true)
        }
        assertEquals(ReportRoute(step = ReportStep.SENT, isSavedOffline = true), stack.last())
    }

    /** Scenariusz (częściowo): zgloszenie-szybkie.feature: Gość po zalogowaniu wraca do zgłoszenia szybkiego. */
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

    /** Wiersz: strona logowania, wejście z „Konto”, zalogowanie → L2, P3. Wymagania: UC6 */
    @Test
    fun uc6_signed_in_from_account_l2() {
        navigator().open(TopLevel.ACCOUNT)
        navigator(visitor).signedIn()
        assertEquals(stackOf(AccountRoute), stack)
    }

    /** Wiersz: L2, „Zmierz i zgłoś barierę” → C1, potem C1 „Dalej” → C2. Wymagania: US6 */
    @Test
    fun us6_l2_to_c1_to_c2() {
        val form = ReportFormRoute(longitude = 17.06, latitude = 51.11, accuracyMeters = 0.8, buildingSymbol = "A-1")
        stack += AccountRoute
        navigator(collector).run {
            openReportForm(form)
            assertEquals(stackOf(AccountRoute), stack)
            measure()
            openReportForm(form)
        }
        assertEquals(stackOf(AccountRoute, MeasureRoute, form), stack)
    }

    /**
     * Wiersz: C2, „Zapisz” → C1 i C2 znikają z historii, otwiera się C3, „Wstecz” wraca do L2, P2.
     *
     * Wymagania: NFR-54
     */
    @Test
    fun nfr54_saved_opens_c3() {
        val form = ReportFormRoute(longitude = 17.06, latitude = 51.11, accuracyMeters = 0.8, buildingSymbol = null)
        stack += listOf(AccountRoute, MeasureRoute, form)
        navigator(collector).reportSaved()
        assertEquals(stackOf(AccountRoute, MySubmissionsRoute), stack)
    }

    /**
     * Wiersz: L2 „Moje zgłoszenia” → C3, C3 „Zmierz i zgłoś barierę” → C1, „Zapisz” → ten sam C3.
     *
     * Wymagania: US8.1, NFR-54
     */
    @Test
    fun us8_1_c3_measure_same_c3() {
        val form = ReportFormRoute(longitude = 17.06, latitude = 51.11, accuracyMeters = 0.8, buildingSymbol = "A-1")
        stack += AccountRoute
        navigator(collector).run {
            mySubmissions()
            measure()
            openReportForm(form)
            reportSaved()
        }
        assertEquals(stackOf(AccountRoute, MySubmissionsRoute), stack)
    }

    /** Wiersz: C3 „Otwórz” → poprawka C2, „Zapisz” → to samo C3. Wymagania: US8.3, NFR-54 */
    @Test
    fun us8_3_open_edit_saved_same_c3() {
        stack += listOf(AccountRoute, MySubmissionsRoute)
        navigator(collector).run {
            openEdit("s1")
            assertEquals(ReportEditRoute("s1"), current)
            reportSaved()
        }
        assertEquals(stackOf(AccountRoute, MySubmissionsRoute), stack)
    }

    /** Wiersz: poprawka C2 „Zmień miejsce” → C1, „Dalej” → C2, „Zapisz” → to samo C3. Wymagania: US7.4, US8.3 */
    @Test
    fun us8_3_edit_change_place() {
        val form = ReportFormRoute(longitude = 17.06, latitude = 51.11, accuracyMeters = 0.8, buildingSymbol = "A-1")
        stack += listOf(AccountRoute, MySubmissionsRoute, ReportEditRoute("s1"))
        navigator(collector).run {
            changeEditPlace()
            assertEquals(MeasureRoute, current)
            openReportForm(form)
            reportSaved()
        }
        assertEquals(stackOf(AccountRoute, MySubmissionsRoute), stack)
    }

    /** Wiersz: „Otwórz” i „Zmień miejsce” poza C3 i poprawką nic nie robią. Wymagania: US8.3 */
    @Test
    fun us8_3_edit_only_from_c3() {
        stack += AccountRoute
        navigator(collector).run {
            openEdit("s1")
            changeEditPlace()
        }
        assertEquals(stackOf(AccountRoute), stack)
    }

    /** Wiersz: V4, „Wstecz” między krokami nie usuwa zdjęcia ani grupy. Wymagania: US4.8 */
    @Test
    fun us4_8_steps_keep_photo() {
        navigator(visitor).run {
            report()
            nextReportStep(ReportRoute(photoId = "z-1", photoPath = "/z-1.jpg"))
            nextReportStep((current as ReportRoute).copy(group = BarrierGroup.TOILETS))
            back()
        }
        val step = stack.last() as ReportRoute
        assertEquals(ReportStep.GROUP, step.step)
        assertEquals(BarrierGroup.TOILETS, step.group)
        assertEquals("z-1", (stack[1] as ReportRoute).photoId)
    }
}
