/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.TopLevel
import io.github.dostepnykampus.app.navigation.hasBottomBar
import io.github.dostepnykampus.app.navigation.selectedItem
import io.github.dostepnykampus.app.navigation.startBackStack
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.designsystem.BottomBar
import io.github.dostepnykampus.core.designsystem.BottomBarItem
import io.github.dostepnykampus.core.designsystem.BottomBarItems
import io.github.dostepnykampus.core.designsystem.NavigationIcons
import io.github.dostepnykampus.core.model.hasStaffRole

/**
 * Oboczka aplikacji: ekran z wierzchu historii i pasek dolny na ekranach, które go mają (wymagania wspólne).
 * Historia zaczyna się od V5 przy [firstLaunch] (US5.1), inaczej od V1, i przetrwa odtworzenie aktywności
 * (NFR-22). [roles] wybiera cele Gościa i napis czwartej pozycji, a [onFirstHelpLeft] dostaje znać, gdy
 * instrukcja z pierwszego uruchomienia zniknie z historii. Ekrany dojdą w kolejnych częściach, na razie są puste.
 */
@Composable
fun AppShell(firstLaunch: Boolean, roles: RoleProvider, onFirstHelpLeft: () -> Unit, modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(elements = startBackStack(firstLaunch).toTypedArray())
    val currentOnFirstHelpLeft by rememberUpdatedState(onFirstHelpLeft)
    val navigator = remember(backStack, roles) { AppNavigator(backStack, roles) { currentOnFirstHelpLeft() } }
    val items = bottomBarItems(hasWork = roles.currentAccount().hasStaffRole)
    Scaffold(
        modifier = modifier,
        bottomBar = {
            val current = backStack.last()
            if (hasBottomBar(current)) {
                BottomBar(
                    items = items,
                    selectedIndex = selectedItem(current)?.ordinal ?: -1,
                    onSelect = { navigator.open(TopLevel.entries[it]) },
                )
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(padding),
            onBack = { navigator.back() },
            entryProvider = { route -> NavEntry(route) {} },
        )
    }
}

@Composable
private fun bottomBarItems(hasWork: Boolean): BottomBarItems {
    val map = stringResource(R.string.nav_mapa)
    val report = stringResource(R.string.nav_zglos_bariere)
    val help = stringResource(R.string.nav_pomoc)
    val account = stringResource(if (hasWork) R.string.nav_konto_i_praca else R.string.nav_konto)
    return remember(map, report, help, account) {
        BottomBarItems(
            listOf(
                BottomBarItem(label = map, icon = NavigationIcons.Map),
                BottomBarItem(label = report, icon = NavigationIcons.Camera),
                BottomBarItem(label = help, icon = NavigationIcons.Help),
                BottomBarItem(label = account, icon = NavigationIcons.Account),
            ),
        )
    }
}
