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
import io.github.dostepnykampus.feature.help.navigation.HelpRoute
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.EntrancesRoute
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
 * Przejścia między ekranami na historii [backStack] (mapa nawigacji: tabele „start, pasek dolny i »Wstecz«”
 * oraz „Gość i Odwiedzający”). Na dole historii jest zawsze V1. Konto z [roles] wybiera cel: Gość trafia do L1
 * zamiast V4 i L2 (US14, US14.2). [onFirstHelpLeft] dostaje znać, gdy instrukcja z pierwszego uruchomienia
 * przestanie być na wierzchu historii dowolną drogą, także paskiem, żeby przy kolejnym starcie otworzyło się V1
 * (US5.1, decyzja 73); powtórne wywołanie nie szkodzi. Każda zmiana historii idzie przez `leaving`. [restarts]
 * liczy powroty do pierwszego ekranu sekcji na każdej pozycji historii; trzeba go zapisywać razem z historią.
 */
class AppNavigator(
    private val backStack: MutableList<NavKey>,
    private val roles: RoleProvider,
    private val onFirstHelpLeft: () -> Unit = {},
    private val restarts: MutableList<Int> = mutableListOf(),
) {
    /** Ekran na wierzchu historii, czyli widoczny. */
    val current: NavKey
        get() = backStack.last()

    internal val isSignedIn: Boolean
        get() = roles.currentAccount().isSignedIn

    internal val hasWork: Boolean
        get() = roles.currentAccount().hasStaffRole

    /**
     * Klucz treści wpisu na pozycji [index] historii: pozycja, liczba powrotów na niej i trasa. Ten sam ekran dwa
     * razy w historii ma dwa klucze, a powrót do pierwszego ekranu sekcji daje nowy klucz, czyli świeży stan ekranu.
     * Zaznaczenie V1 nie wchodzi do klucza: V1 żyje pod arkuszem V2 i po „Wstecz” wraca w swoim stanie (decyzja 83).
     */
    fun contentKey(index: Int): String {
        val route = backStack[index].let { if (it is MapRoute) MapRoute() else it }
        return "$index:${restarts.getOrElse(index) { 0 }}:$route"
    }

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
            TopLevel.HELP -> push(HelpRoute())
            TopLevel.ACCOUNT -> push(if (isSignedIn) AccountRoute else SignInRoute(isForReport = false))
        }
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

    private fun restartSection(item: TopLevel) {
        if (item == TopLevel.MAP) showOnMap(focus = null)
        while (restarts.size < backStack.size) restarts += 0
        restarts[backStack.lastIndex]++
    }

    private fun push(route: NavKey) {
        leaving { backStack.add(route) }
    }

    /** Jedyna droga zmiany historii: pilnuje wyjścia z instrukcji i liczników powrotów (decyzje 72 i 73). */
    internal fun leaving(change: MutableList<NavKey>.() -> Unit) {
        val isOnFirstHelp = current == FirstHelp
        backStack.change()
        while (restarts.size > backStack.size) restarts.removeAt(restarts.lastIndex)
        if (isOnFirstHelp && current != FirstHelp) onFirstHelpLeft()
    }

    private companion object {
        val FirstHelp = HelpRoute(isFirstLaunch = true)
    }
}
