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
 * uruchomieniu V5 nad V1 (US5.1), przy kolejnym samo V1 (US1.2). Na dole jest zawsze V1, ekran bez „Wstecz”.
 */
fun startBackStack(firstLaunch: Boolean): List<NavKey> =
    if (firstLaunch) listOf(MapRoute, HelpRoute(isFirstLaunch = true)) else listOf(MapRoute)

/**
 * Pozycja paska dolnego wybrana na ekranie [route] albo `null`, gdy ekran nie ma paska. Pasek mają V1, V5 i L2
 * (V2 i V3 dojdą z ich trasami); L1 i V4 go nie mają (wymagania wspólne).
 */
fun topLevelOf(route: NavKey): TopLevel? =
    when (route) {
        MapRoute -> TopLevel.MAP
        is HelpRoute -> TopLevel.HELP
        AccountRoute -> TopLevel.ACCOUNT
        else -> null
    }

/**
 * Przejścia między ekranami na historii [backStack] (mapa nawigacji: tabele „start, pasek dolny i »Wstecz«”
 * oraz „Gość i Odwiedzający”). Na dole historii jest zawsze V1. Konto z [roles] wybiera cel: Gość trafia do L1
 * zamiast V4 i L2 (US14, US14.2).
 */
class AppNavigator(private val backStack: MutableList<NavKey>, private val roles: RoleProvider) {
    /** Ekran na wierzchu historii, czyli widoczny. */
    val current: NavKey
        get() = backStack.last()

    /**
     * Pozycja [item] paska dolnego. „Mapa” wraca do V1. Inna pozycja otwiera swój ekran nad bieżącym, więc „Wstecz”
     * wraca tam, skąd ją otwarto (V5, NFR-15); ten sam ekran wcześniej w historii przechodzi na wierzch.
     */
    fun open(item: TopLevel) {
        if (item != TopLevel.MAP && topLevelOf(current) == item) return
        val account = if (roles.currentAccount().isSignedIn) AccountRoute else SignInRoute(isForReport = false)
        when (item) {
            TopLevel.MAP -> backToMap()
            TopLevel.REPORT -> push(reportStart())
            TopLevel.HELP -> push(HelpRoute())
            TopLevel.ACCOUNT -> push(account)
        }
    }

    /** „Wstecz” w nagłówku i wstecz telefonu: poprzedni ekran (NFR-15). Na V1 fałsz, wtedy aplikacja się zamyka. */
    fun back(): Boolean {
        if (backStack.size == 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    /** V1 z wyczyszczoną historią, np. „Wróć do mapy”, „Pomiń instrukcję”, „Zacznij”, „Pokaż na mapie”. */
    fun backToMap() {
        backStack.subList(fromIndex = 1, toIndex = backStack.size).clear()
    }

    private fun reportStart(): NavKey =
        if (roles.currentAccount().isSignedIn) ReportRoute() else SignInRoute(isForReport = true)

    private fun push(route: NavKey) {
        backStack.removeAll { it::class == route::class }
        backStack.add(route)
    }
}
