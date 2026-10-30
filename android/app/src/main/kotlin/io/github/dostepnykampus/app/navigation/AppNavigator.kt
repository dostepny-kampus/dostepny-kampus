/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.model.isSignedIn
import io.github.dostepnykampus.feature.auth.navigation.AccountRoute
import io.github.dostepnykampus.feature.auth.navigation.SignInRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.help.navigation.HelpRoute
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
    if (firstLaunch) listOf(MapRoute, HelpRoute(isFirstLaunch = true)) else listOf(MapRoute)

/**
 * Czy ekran [route] ma pasek dolny: V1, V5 i L2 tak (V2 i V3 dojdą z ich trasami), L1 i V4 nie (wymagania
 * wspólne, wiersz „Pasek dolny na V1, V2, V3, V5 i L2”).
 */
fun hasBottomBar(route: NavKey): Boolean = selectedItem(route) != null

/** Pozycja paska zaznaczona na ekranie [route] albo `null`, gdy żadna nie jest zaznaczona. */
fun selectedItem(route: NavKey): TopLevel? =
    when (route) {
        MapRoute -> TopLevel.MAP
        is HelpRoute -> TopLevel.HELP
        AccountRoute -> TopLevel.ACCOUNT
        else -> null
    }

/**
 * Przejścia między ekranami na historii [backStack] (mapa nawigacji: tabele „start, pasek dolny i »Wstecz«”
 * oraz „Gość i Odwiedzający”). Na dole historii jest zawsze V1. Konto z [roles] wybiera cel: Gość trafia do L1
 * zamiast V4 i L2 (US14, US14.2). [onFirstHelpLeft] dostaje znać, gdy instrukcja z pierwszego uruchomienia
 * zniknie z historii dowolną drogą, żeby przy kolejnym starcie otworzyło się V1 (US5.1).
 */
class AppNavigator(
    private val backStack: MutableList<NavKey>,
    private val roles: RoleProvider,
    private val onFirstHelpLeft: () -> Unit = {},
) {
    /** Ekran na wierzchu historii, czyli widoczny. */
    val current: NavKey
        get() = backStack.last()

    /** Pozycja [item] paska dolnego: „Mapa” wraca do V1, inna otwiera swój ekran nad bieżącym, jeśli to nie on. */
    fun open(item: TopLevel) {
        if (item != TopLevel.MAP && selectedItem(current) == item) return
        val isSignedIn = roles.currentAccount().isSignedIn
        when (item) {
            TopLevel.MAP -> backToMap()
            TopLevel.REPORT -> backStack.add(if (isSignedIn) ReportRoute else SignInRoute(isForReport = true))
            TopLevel.HELP -> backStack.add(HelpRoute())
            TopLevel.ACCOUNT -> backStack.add(if (isSignedIn) AccountRoute else SignInRoute(isForReport = false))
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

    private fun leaving(change: () -> Unit) {
        val hasHadFirstHelp = FirstHelp in backStack
        change()
        if (hasHadFirstHelp && FirstHelp !in backStack) onFirstHelpLeft()
    }

    private companion object {
        val FirstHelp = HelpRoute(isFirstLaunch = true)
    }
}
