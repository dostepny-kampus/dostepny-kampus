/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.model.hasStaffRole
import io.github.dostepnykampus.core.model.isSignedIn
import io.github.dostepnykampus.feature.auth.navigation.AccountRoute
import io.github.dostepnykampus.feature.auth.navigation.SignInRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportStep
import io.github.dostepnykampus.feature.help.navigation.HelpRoute
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.EntrancesRoute
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.navigation.MapRoute

/** Pozycja paska dolnego (wymagania wspólne). */
enum class TopLevel {
    /** „Mapa”: V1. */
    MAP,

    /** „Zgłoś barierę”: V4, Gość L1. */
    REPORT,

    /** „Pomoc”: V5. */
    HELP,

    /** „Konto” albo „Konto i praca”: L2, Gość L1. */
    ACCOUNT,
}

/**
 * Historia „Wstecz” przy starcie (mapa nawigacji, tabela „start, pasek dolny i »Wstecz«”): przy pierwszym
 * uruchomieniu V5 nad V1 (US5.1), przy kolejnym samo V1. Na dole jest zawsze V1, ekran bez „Wstecz”.
 */
fun startBackStack(firstLaunch: Boolean): List<NavKey> =
    if (firstLaunch) listOf(MapRoute(), HelpRoute(isFirstLaunch = true)) else listOf(MapRoute())

/**
 * Czy ekran [route] ma pasek nawigacji, dolny albo z boku: V1, V2, V3, V5 i L2 tak, L1 i V4
 * nie (wymagania wspólne, wiersz „Pasek dolny na V1, V2, V3, V5 i L2”).
 */
fun hasNavigationBar(route: NavKey): Boolean = selectedItem(route) != null

/** Pozycja paska zaznaczona na ekranie [route] albo `null`, gdy żadna nie jest zaznaczona. */
fun selectedItem(route: NavKey): TopLevel? =
    when (route) {
        is MapRoute, is BuildingRoute, is EntrancesRoute -> TopLevel.MAP
        is HelpRoute -> TopLevel.HELP
        AccountRoute -> TopLevel.ACCOUNT
        else -> null
    }

/**
 * Historia „Wstecz” [backStack], na dole zawsze V1. [onFirstHelpLeft] dostaje znać, gdy instrukcja z pierwszego
 * uruchomienia przestanie być na wierzchu historii dowolną drogą, także paskiem, żeby przy kolejnym starcie
 * otworzyło się V1 (US5.1, decyzja 73); powtórne wywołanie nie szkodzi. [restarts] liczy powroty do pierwszego
 * ekranu sekcji na każdej pozycji historii; trzeba go zapisywać razem z historią. Każda zmiana historii idzie
 * przez [leaving], dostępne tylko dla przejść w [AppNavigator].
 */
sealed class NavigationHistory(
    private val backStack: MutableList<NavKey>,
    private val onFirstHelpLeft: () -> Unit,
    private val restarts: MutableList<Int>,
) {
    /** Ekran na wierzchu historii, czyli widoczny. */
    val current: NavKey
        get() = backStack.last()

    /**
     * Klucz treści wpisu na pozycji [index] historii: pozycja, liczba powrotów na niej i trasa. Ten sam ekran dwa
     * razy w historii ma dwa klucze, a powrót do pierwszego ekranu sekcji daje nowy klucz, czyli świeży stan ekranu.
     * Zaznaczenie V1 nie wchodzi do klucza: V1 żyje pod arkuszem V2 i po „Wstecz” wraca w swoim stanie (decyzja 83).
     */
    fun contentKey(index: Int): String {
        val route = backStack[index].let { if (it is MapRoute) MapRoute() else it }
        return "$index:${restarts.getOrElse(index) { 0 }}:$route"
    }

    /** „Wstecz” w nagłówku i wstecz telefonu: poprzedni ekran (NFR-15). Na V1 fałsz, wtedy aplikacja się zamyka. */
    fun back(): Boolean {
        if (backStack.size == 1) return false
        leaving { backStack.removeAt(backStack.lastIndex) }
        return true
    }

    /** V1 z wyczyszczoną historią, np. „Wróć do mapy”, „Pomiń instrukcję”, „Zacznij”, „Nie teraz”. */
    fun backToMap() {
        leaving { backStack.subList(fromIndex = 1, toIndex = backStack.size).clear() }
    }

    /** Powrót do pierwszego ekranu sekcji na wierzchu: nowy klucz treści, czyli świeży stan (decyzja 72). */
    protected fun restartTop() {
        while (restarts.size < backStack.size) restarts += 0
        restarts[backStack.lastIndex]++
    }

    /** Jedyna droga zmiany historii: pilnuje wyjścia z instrukcji i liczników powrotów (decyzje 72 i 73). */
    protected fun leaving(change: MutableList<NavKey>.() -> Unit) {
        val isOnFirstHelp = current == FirstHelp
        backStack.change()
        while (restarts.size > backStack.size) restarts.removeAt(restarts.lastIndex)
        if (isOnFirstHelp && current != FirstHelp) onFirstHelpLeft()
    }

    private companion object {
        val FirstHelp = HelpRoute(isFirstLaunch = true)
    }
}

/**
 * Przejścia między ekranami (mapa nawigacji: tabele „start, pasek dolny i »Wstecz«”, „Gość i Odwiedzający” oraz
 * „logowanie i konto”). Konto z [roles] wybiera cel: Gość trafia do L1 zamiast V4 i L2 (US14, US14.2).
 */
class AppNavigator(
    backStack: MutableList<NavKey>,
    private val roles: RoleProvider,
    onFirstHelpLeft: () -> Unit = {},
    restarts: MutableList<Int> = mutableListOf(),
) : NavigationHistory(backStack, onFirstHelpLeft, restarts) {
    private val isSignedIn: Boolean
        get() = roles.currentAccount().isSignedIn

    internal val hasWork: Boolean
        get() = roles.currentAccount().hasStaffRole

    /**
     * Pozycja [item] paska: „Mapa” wraca do V1, inna otwiera swój ekran nad bieżącym. Ponowne dotknięcie wybranej
     * pozycji wraca do pierwszego ekranu sekcji w postaci jak po wejściu (NFR-15, decyzja 72).
     */
    fun open(item: TopLevel) {
        if (selectedItem(current) == item) {
            restartSection(item)
            return
        }
        when (item) {
            TopLevel.MAP -> backToMap()
            TopLevel.REPORT -> report()
            TopLevel.HELP -> leaving { add(HelpRoute()) }
            TopLevel.ACCOUNT -> leaving { add(if (isSignedIn) AccountRoute else SignInRoute(isForReport = false)) }
        }
    }

    private fun restartSection(item: TopLevel) {
        if (item == TopLevel.MAP) showOnMap(focus = null)
        restartTop()
    }

    /** V1 → V2 budynku [symbol]; „Wstecz” z V2 wraca do V1 z zaznaczonym budynkiem (US1.1, US2, US3.2). */
    fun openBuilding(symbol: String) {
        leaving {
            this[0] = MapRoute(MapFocus.Building(symbol))
            add(BuildingRoute(symbol))
        }
    }

    /** V2 → V3 budynku [symbol]: „Sprawdź, którędy wejść” (US3, US3.2). */
    fun openEntrances(symbol: String) {
        leaving { add(EntrancesRoute(symbol)) }
    }

    /** V1 z zaznaczonym [focus] i wyczyszczoną historią: „Pokaż na mapie”, „Pokaż wejście na mapie” (US2.1, BR-25). */
    fun showOnMap(focus: MapFocus?) {
        leaving {
            subList(fromIndex = 1, toIndex = size).clear()
            this[0] = MapRoute(focus)
        }
    }

    /** „Zgłoś barierę”, także przy wejściu [entranceId]: V4 krok 1, a Gość najpierw L1 (US4, US4.6, US14.2). */
    fun report(entranceId: String? = null) {
        val route =
            if (isSignedIn) {
                ReportRoute(entranceId = entranceId)
            } else {
                SignInRoute(isForReport = true, entranceId = entranceId)
            }
        leaving { add(route) }
    }

    /** Następny krok V4: „Dalej” po zdjęciu, wybór grupy albo piętra (US4.1, US4.3, US4.5). */
    fun nextReportStep() {
        val route = (current as? ReportRoute)?.takeIf { it.step < ReportStep.SEND } ?: return
        leaving { add(route.copy(step = ReportStep.entries[route.step.ordinal + 1])) }
    }

    /** „Wyślij”: potwierdzenie zamiast kroków V4, więc „Wstecz” prowadzi na ekran sprzed V4 (US4.4, P2). */
    fun reportSent() {
        if (current !is ReportRoute) return
        leaving {
            dropReport()
            add(ReportRoute(step = ReportStep.SENT))
        }
    }

    /** „Tak, zrezygnuj” po „Wstecz” z kroku 1: ekran sprzed V4, zgłoszenie nie powstaje (UC7 1b). */
    fun leaveReport() {
        leaving { dropReport() }
    }

    /** Po zalogowaniu na L1: V4 krok 1 z wejściem wybranym przed L1 albo L2 (US14.2, UC7 1a, P3). */
    fun signedIn() {
        val route = current as? SignInRoute ?: return
        leaving {
            removeAt(lastIndex)
            add(if (route.isForReport) ReportRoute(entranceId = route.entranceId) else AccountRoute)
        }
    }

    private fun MutableList<NavKey>.dropReport() {
        while (last() is ReportRoute) removeAt(lastIndex)
    }
}
