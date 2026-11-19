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
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.feature.capture.navigation.MeasureRoute
import io.github.dostepnykampus.feature.capture.navigation.MySubmissionsRoute
import io.github.dostepnykampus.feature.capture.navigation.RemovedRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportEditRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportPlace
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportStep
import io.github.dostepnykampus.feature.help.navigation.HelpRoute
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Testy przejść; komentarz nad testem bez scenariusza podaje wiersz tabeli mapy nawigacji (ekran, czynność, AC/BR).
 *
 * Wymagania: US14.2, US2, US4.6, US4.1
 */
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
    fun opensHelpOnFirstLaunch() {
        assertEquals(stackOf(HelpRoute(isFirstLaunch = true)), startBackStack(firstLaunch = true))
    }

    /** Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcję można otworzyć ponownie z pomocy. */
    @Test
    fun opensMapOnNextLaunch() {
        assertEquals(stackOf(), startBackStack(firstLaunch = false))
    }

    /** Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcję można otworzyć ponownie z pomocy. */
    @Test
    fun goesBackToMapOnFirstLaunch() {
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
    fun leavesFirstHelpViaSignIn() {
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
    fun keepsFlagOnBarHelpLeave() {
        navigator().run {
            open(TopLevel.HELP)
            open(TopLevel.ACCOUNT)
            back()
        }
        assertEquals(0, helpLeft)
    }

    /** Scenariusz (częściowo): zgloszenie-szybkie.feature: Gość rezygnuje z logowania. */
    @Test
    fun returnsGuestToMap() {
        navigator().run {
            open(TopLevel.REPORT)
            backToMap()
        }
        assertEquals(stackOf(), stack)
    }

    /** Wiersz: pasek dolny (V1, V5, L2), „Mapa” → V1. Wymagania: US1 */
    @Test
    fun returnsToMapFromBar() {
        stack += listOf(HelpRoute(), AccountRoute)
        navigator().open(TopLevel.MAP)
        assertEquals(stackOf(), stack)
    }

    /** Wiersz: pasek dolny, ponowne dotknięcie wybranej pozycji → pierwszy ekran sekcji. Wymagania: NFR-15 */
    @Test
    fun restartsHelpOnReselect() {
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

    /**
     * Wiersz: pasek dolny, ponowne dotknięcie „Mapa” niczego nie zamyka: karta i V1 pod nią zostają.
     * Scenariusz: przegladanie-mapy.feature: Ponowne dotknięcie „Mapa” w pasku dolnym nie zamyka karty
     *
     * Wymagania: NFR-15, US2.8
     */
    @Test
    fun keepsCardOnMapReselect() {
        navigator().run {
            val key = contentKey(index = 0)
            open(TopLevel.HELP)
            open(TopLevel.MAP)
            assertEquals(key, contentKey(index = 0))
            openBuilding(symbol = "C-13")
            val keys = stack.indices.map(::contentKey)
            val routes = stack.toList()
            open(TopLevel.MAP)
            assertEquals(keys, stack.indices.map(::contentKey))
            assertEquals(routes, stack)
        }
        assertEquals(BuildingRoute("C-13"), stack.last())
    }

    /**
     * Wiersz: każdy ekran poza V1, „Wstecz”: ten sam ekran dwa razy w historii ma dwa klucze treści.
     *
     * Wymagania: NFR-15
     */
    @Test
    fun keysSameScreenTwice() {
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
    fun opensReportWhenSignedIn() {
        navigator(visitor).open(TopLevel.REPORT)
        assertEquals(stackOf(ReportRoute()), stack)
    }

    /**
     * Scenariusz (częściowo): zgloszenie-szybkie.feature: Gość niezalogowany jest proszony o logowanie przed zdjęciem.
     */
    @Test
    fun opensSignInForGuestReport() {
        navigator().open(TopLevel.REPORT)
        assertEquals(stackOf(SignInRoute(isForReport = true)), stack)
    }

    /** Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcję można otworzyć ponownie z pomocy. */
    @Test
    fun returnsFromBarHelp() {
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
    fun opensSignInForGuestAccount() {
        navigator().open(TopLevel.ACCOUNT)
        assertEquals(stackOf(SignInRoute(isForReport = false)), stack)
    }

    /**
     * Wiersze: pasek dolny, „Konto” → L2, Odwiedzający i „Konto i praca” → L2, role pracownicze, P1.
     *
     * Wymagania: US14.1, RO-ODW
     */
    @Test
    fun opensAccount() {
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
    fun closesAppOnMapBack() {
        assertFalse(navigator().back())
        assertEquals(stackOf(), stack)
    }

    /** Wymagania wspólne, wiersz „Pasek dolny na V1, V2, V5 i L2”: L1 i V4 bez paska. */
    @Test
    fun showsBarOnBarScreensOnly() {
        val expected = listOf(TopLevel.MAP, TopLevel.MAP, TopLevel.HELP, TopLevel.ACCOUNT)
        val routes = listOf(MapRoute(), BuildingRoute("A-1"), HelpRoute(), AccountRoute)
        assertEquals(expected, routes.map(::selectedItem))
        assertTrue(hasNavigationBar(AccountRoute))
        val noBar = listOf(SignInRoute(isForReport = false), ReportRoute(), ReportRoute(step = ReportStep.SENT))
        noBar.forEach { assertFalse(hasNavigationBar(it)) }
    }

    /** Wiersz: mała karta V2, „Wstecz” → V1 bez zaznaczenia budynku. Wymagania: US2.8 */
    @Test
    fun unselectsOnCardBack() {
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
    fun replacesCardWithOther() {
        navigator().run {
            openBuilding(symbol = "C-13")
            openBuilding(symbol = "A-1")
            assertEquals(listOf(MapRoute(MapFocus.Building("A-1")), BuildingRoute("A-1")), stack)
            back()
        }
        assertEquals(listOf(MapRoute()), stack)
    }

    /**
     * Wiersz: karta zastąpiona arkuszem znaku, powrót do karty tego budynku → V2 z zapamiętaną wysokością
     *   i przewinięciem, a do karty innego budynku albo po dotknięciu konturu → V2 od nowa.
     *
     * Wymagania: US2.8, US3.3
     */
    @Test
    fun returnsToKeptCard() {
        val kept = BuildingRoute("A-1", isExpanded = true, scroll = 300)
        navigator().run {
            openBuilding(symbol = "A-1")
            keepCard(kept)
            showOnMap(MapFocus.Entrance(id = "node/1", buildingSymbol = "A-1"))
            returnToCard("A-1")
            assertEquals(listOf(MapRoute(MapFocus.Building("A-1")), kept), stack)
            returnToCard("C-13")
            assertEquals(BuildingRoute("C-13"), current, "karta innego budynku od nowa")
            keepCard(kept)
            openBuilding(symbol = "A-1")
            assertEquals(BuildingRoute("A-1"), current, "dotknięcie konturu od nowa")
        }
    }

    /**
     * Wiersz: mała karta V2 nad V1, dotknięcie pustej mapy → V1 bez zaznaczenia budynku;
     * samo V1, także z budynkiem zaznaczonym przez „Pokaż na mapie”, i V4 nad V2 zostają.
     *
     * Wymagania: US2.8, NFR-17
     */
    @Test
    fun closesCardOnEmptyMap() {
        val selected = MapRoute(MapFocus.Building("A-1"))
        navigator().run {
            showOnMap(MapFocus.Building("A-1"))
            closeCardOnEmptyMap()
            assertEquals(listOf(selected), stack)
            openBuilding(symbol = "A-1")
            report()
            closeCardOnEmptyMap()
            assertEquals(listOf(selected, BuildingRoute("A-1"), SignInRoute(isForReport = true)), stack)
            back()
            closeCardOnEmptyMap()
        }
        assertEquals(listOf(MapRoute()), stack)
    }

    /**
     * Wiersze: V1, dotknięcie budynku na liście przy zwykłej czcionce → V1 w widoku mapy z małą kartą V2; mała karta,
     * „Wstecz” albo dotknięcie pustej mapy → V1 bez zaznaczenia, zapamiętany wybór „Lista budynków” bez zmian.
     *
     * Wymagania: US2.8, NFR-19
     */
    @Test
    fun keepsListWhenCompact() {
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
    fun opensDirectFromTalkBackList() {
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
    fun choosesMapOnShow() {
        mapChoiceNavigator().run {
            openBuildingFromList("A-1", true)
            showOnMap(MapFocus.Building("A-1"))
        }
        assertEquals(listOf(MapRoute(MapFocus.Building("A-1"))), stack)
        assertEquals(1, mapChosen)
    }

    /**
     * Wiersze: V2, „Pokaż wejście na mapie” → V1 z zaznaczonym wejściem w widoku „Mapa”;
     * pasek, ponowne „Mapa” → V1, nie zmienia wyboru drugi raz.
     *
     * Wymagania: US2.1, NFR-15
     */
    @Test
    fun choosesMapForEntrance() {
        mapChoiceNavigator().run {
            showOnMap(MapFocus.Entrance(id = "A-1-glowne", buildingSymbol = "A-1"))
            open(TopLevel.MAP)
        }
        assertEquals(1, mapChosen)
    }

    /**
     * Wiersz: V4, „Tak, zrezygnuj” po „Wstecz” z kroku 1 → ekran sprzed V4; poza V4 kroki nic nie robią.
     * Scenariusz (częściowo): zgloszenie-szybkie.feature: Zgłoszenie przy wejściu ma miejsce w punkcie wybranego
     *   wejścia
     *
     * Wymagania: UC7
     */
    @Test
    fun reportsAtEntrance() {
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
    fun sendsAfterThreeSteps() {
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
    fun sendsOffline() {
        navigator(visitor).run {
            report()
            reportSent(isSavedOffline = true)
        }
        assertEquals(ReportRoute(step = ReportStep.SENT, isSavedOffline = true), stack.last())
    }

    /** Scenariusz (częściowo): zgloszenie-szybkie.feature: Gość po zalogowaniu wraca do zgłoszenia szybkiego. */
    @Test
    fun opensReportAfterGuestSignIn() {
        navigator().run {
            openBuilding(symbol = "A-1")
            report(entranceId = "A-1-glowne")
            assertEquals(SignInRoute(isForReport = true, entranceId = "A-1-glowne"), current)
        }
        repeat(times = 2) { navigator(visitor).signedIn() }
        assertEquals(listOf(BuildingRoute("A-1"), ReportRoute(entranceId = "A-1-glowne")), stack.drop(1))
    }

    /**
     * Wiersz: arkusz toalety, schodów albo windy, przycisk zgłoszenia → V4 z obiektem; Gość przez L1 wraca do V4
     *   z tym samym obiektem.
     * Scenariusz (częściowo, przykład: arkusz windy): zgloszenie-szybkie.feature: Zgłoszenie przy obiekcie
     *   planu ma miejsce w punkcie obiektu i piętro planu
     *
     * Wymagania: US4.6, US14.2
     */
    @Test
    fun reportsAtPlanObjectAfterSignIn() {
        val place =
            ReportPlace(
                category = RoomCategory.ELEVATOR,
                buildingSymbol = "A-1",
                floorId = "A-1/p2",
                floorLabel = "2 piętro",
                latitude = 51.11,
                longitude = 17.06,
            )
        navigator().run {
            report(place = place)
            assertEquals(SignInRoute(isForReport = true, spot = place), current)
        }
        navigator(visitor).signedIn()
        assertEquals(ReportRoute(place = place), stack.last())
    }

    /** Wiersz: strona logowania, wejście z „Konto”, zalogowanie → L2, P3. Wymagania: UC6 */
    @Test
    fun signsInFromAccount() {
        navigator().open(TopLevel.ACCOUNT)
        navigator(visitor).signedIn()
        assertEquals(stackOf(AccountRoute), stack)
    }

    /** Wiersz: L2, „Zmierz i zgłoś barierę” → C1, potem C1 „Dalej” → C2. Wymagania: US6 */
    @Test
    fun goesFromAccountToForm() {
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
    fun opensOwnListAfterSave() {
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
    fun returnsToSameOwnList() {
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
    fun returnsToOwnListAfterEdit() {
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
    fun changesPlaceOnEdit() {
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

    /** Wiersz: V2 „Zgłoś, że bariery nie ma” → C7, „Wyślij do sprawdzenia” → C3 bez C7. Wymagania: US8.4, NFR-54 */
    @Test
    fun removedSavedOpensSameC3() {
        stack += BuildingRoute("A-1")
        navigator(collector).run {
            openRemoved("tb9")
            assertEquals(RemovedRoute("tb9"), current, "V2 otwiera C7")
            reportSaved()
        }
        assertEquals(stackOf(BuildingRoute("A-1"), MySubmissionsRoute), stack, "C7 znika z historii, C3 po V2")
    }

    /** Wiersz: „Otwórz” i „Zmień miejsce” poza C3 i poprawką nic nie robią. Wymagania: US8.3 */
    @Test
    fun editsOnlyFromOwnList() {
        stack += AccountRoute
        navigator(collector).run {
            openEdit("s1")
            changeEditPlace()
        }
        assertEquals(stackOf(AccountRoute), stack)
    }

    /** Wiersz: V4, „Wstecz” między krokami nie usuwa zdjęcia ani grupy. Wymagania: US4.8 */
    @Test
    fun keepsPhotoAcrossSteps() {
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
