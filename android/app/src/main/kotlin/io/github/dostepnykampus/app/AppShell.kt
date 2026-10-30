/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.TopLevel
import io.github.dostepnykampus.app.navigation.hasNavigationBar
import io.github.dostepnykampus.app.navigation.selectedItem
import io.github.dostepnykampus.app.navigation.startBackStack
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.designsystem.BottomBar
import io.github.dostepnykampus.core.designsystem.BottomBarItem
import io.github.dostepnykampus.core.designsystem.BottomBarItems
import io.github.dostepnykampus.core.designsystem.NavigationIcons
import io.github.dostepnykampus.core.designsystem.SideBar
import io.github.dostepnykampus.core.model.hasStaffRole

private val WideWindow = 600.dp

private val RestartsSaver =
    listSaver<SnapshotStateList<Int>, Int>(save = { it.toList() }, restore = { it.toMutableStateList() })

/**
 * Oboczka aplikacji: ekran z wierzchu historii i pasek nawigacji na ekranach, które go mają (wymagania wspólne).
 * W oknie o szerokości co najmniej 600 dp pasek stoi z boku, w węższym na dole (decyzja 69). Historia zaczyna się
 * od V5 przy [firstLaunch] (US5.1), inaczej od V1, i przetrwa odtworzenie aktywności (NFR-22). [roles] wybiera
 * cele Gościa i napis czwartej pozycji, a [onFirstHelpLeft] dostaje znać, gdy instrukcja z pierwszego
 * uruchomienia przestanie być na wierzchu. [content] rysuje ekran trasy i podpina jego czynności do nawigatora.
 */
@Composable
fun AppShell(
    firstLaunch: Boolean,
    roles: RoleProvider,
    onFirstHelpLeft: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable AppNavigator.(NavKey) -> Unit = {},
) {
    val backStack = rememberNavBackStack(elements = startBackStack(firstLaunch).toTypedArray())
    val restarts = rememberSaveable(saver = RestartsSaver) { SnapshotStateList() }
    val currentOnFirstHelpLeft by rememberUpdatedState(onFirstHelpLeft)
    val navigator =
        remember(backStack, roles, restarts) {
            AppNavigator(
                backStack = backStack,
                roles = roles,
                onFirstHelpLeft = { currentOnFirstHelpLeft() },
                restarts = restarts,
            )
        }
    val items = bottomBarItems(hasWork = roles.currentAccount().hasStaffRole)
    val entries =
        rememberDecoratedNavEntries(
            entries =
                backStack.indices.map { index ->
                    NavEntry(backStack[index], contentKey = navigator.contentKey(index)) { navigator.content(it) }
                },
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        )
    BoxWithConstraints(modifier = modifier) {
        val isWide = maxWidth >= WideWindow
        val current = backStack.last()
        NavigationSuiteScaffoldLayout(
            navigationSuite = {
                if (hasNavigationBar(current)) {
                    val selected = selectedItem(current)?.ordinal ?: -1
                    val onSelect = { index: Int -> navigator.open(TopLevel.entries[index]) }
                    if (isWide) SideBar(items, selected, onSelect) else BottomBar(items, selected, onSelect)
                }
            },
            navigationSuiteType = if (isWide) NavigationSuiteType.NavigationRail else NavigationSuiteType.NavigationBar,
            content = { NavDisplay(entries = entries, onBack = { navigator.back() }) },
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
