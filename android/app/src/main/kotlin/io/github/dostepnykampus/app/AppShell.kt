/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.TopLevel
import io.github.dostepnykampus.app.navigation.isCaptureRoute
import io.github.dostepnykampus.app.navigation.selectedItem
import io.github.dostepnykampus.app.navigation.startBackStack
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.designsystem.BottomBar
import io.github.dostepnykampus.core.designsystem.BottomBarItem
import io.github.dostepnykampus.core.designsystem.BottomBarItems
import io.github.dostepnykampus.core.designsystem.NavigationIcons
import io.github.dostepnykampus.core.designsystem.SideBar
import io.github.dostepnykampus.core.model.hasStaffRole
import io.github.dostepnykampus.feature.capture.ReportDraft
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute

private val WideWindow = WIDE_WINDOW_DP.dp

private val RestartsSaver =
    listSaver<SnapshotStateList<Int>, Int>(save = { it.toList() }, restore = { it.toMutableStateList() })

/**
 * Oboczka aplikacji: ekran z wierzchu historii i pasek nawigacji na ekranach, które go mają (wymagania wspólne).
 * W oknie o szerokości co najmniej 600 dp pasek stoi z boku, w węższym na dole (decyzja 69). Historia zaczyna się
 * od V5 przy [firstLaunch] (US5.1), inaczej od V1, i przetrwa odtworzenie aktywności (NFR-22). [roles] wybiera
 * cele Gościa i napis czwartej pozycji, a [onFirstHelpLeft] dostaje znać, gdy instrukcja z pierwszego
 * uruchomienia przestanie być na wierzchu, a [onChooseMap] zapisuje wybór „Mapa” po „Pokaż budynek na mapie”.
 * [content] rysuje ekran trasy i podpina jego czynności do nawigatora, domyślnie [AppScreen]. Wcięcia systemowe,
 * które zajmuje pasek, są dla treści zużyte: ekran dopełnia resztę przez `WindowInsets.safeDrawing` albo rysuje pod
 * nią, jak mapa. V2 jest arkuszem nad V1 ([SheetSceneStrategy]),
 * a gdy okno daje kartę `Direct` (duża czcionka, szerokie okno, poziomo), osobnym ekranem bez mapy (US2.9, NFR-51).
 * Po odtworzeniu historii [onRestore] dostaje pliki zdjęć odtworzonych szkiców, których sprzątanie nie usuwa (#927).
 */
@Composable
fun AppShell(
    firstLaunch: Boolean,
    roles: RoleProvider,
    onFirstHelpLeft: () -> Unit,
    modifier: Modifier = Modifier,
    onChooseMap: () -> Unit = {},
    onRestore: (keptPhotos: Set<String>) -> Unit = {},
    content: @Composable AppNavigator.(NavKey) -> Unit = { AppScreen(it) },
) {
    val backStack = rememberNavBackStack(elements = startBackStack(firstLaunch).toTypedArray())
    val restarts = rememberSaveable(saver = RestartsSaver) { SnapshotStateList() }
    val draft = rememberCaptureDraft(routes = backStack, onRestore = onRestore)
    val currentOnFirstHelpLeft by rememberUpdatedState(onFirstHelpLeft)
    val currentOnChooseMap by rememberUpdatedState(onChooseMap)
    val navigator =
        remember(backStack, roles, restarts, draft) {
            AppNavigator(
                backStack = backStack,
                roles = roles,
                onFirstHelpLeft = { currentOnFirstHelpLeft() },
                restarts = restarts,
                onChooseMap = { currentOnChooseMap() },
                reportDraft = draft,
            )
        }
    val items = bottomBarItems(hasWork = roles.hasWork())
    val entries =
        rememberDecoratedNavEntries(
            entries =
                backStack.indices.map { index ->
                    val route = backStack[index]
                    NavEntry(
                        key = route,
                        contentKey = navigator.contentKey(index),
                        metadata = SheetSceneStrategy.metadata(route),
                    ) { navigator.content(it) }
                },
            entryDecorators =
                listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
        )
    BoxWithConstraints(modifier = modifier) {
        val isWide = maxWidth >= WideWindow
        val selected = selectedItem(navigator.current)?.ordinal
        NavigationSuiteScaffoldLayout(
            navigationSuite = {
                if (selected != null) {
                    val onSelect = { index: Int -> navigator.open(TopLevel.entries[index]) }
                    if (isWide) SideBar(items, selected, onSelect) else BottomBar(items, selected, onSelect)
                }
            },
            navigationSuiteType = if (isWide) NavigationSuiteType.NavigationRail else NavigationSuiteType.NavigationBar,
            content = {
                val barInsets =
                    when {
                        selected == null -> WindowInsets(0)
                        isWide -> NavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start)
                        else -> NavigationBarDefaults.windowInsets.only(WindowInsetsSides.Bottom)
                    }
                NavDisplay(
                    entries = entries,
                    modifier = Modifier.consumeWindowInsets(barInsets),
                    sceneStrategies = currentCardWindow().sceneStrategies(),
                    onBack = { navigator.back() },
                )
            },
        )
    }
}

/** Pliki zdjęć odtworzonych szkiców: zgłoszenia szybkiego w [routes] i C2 w [draft] (#927). */
internal fun keptPhotos(routes: List<NavKey>, draft: ReportDraft): Set<String> =
    (routes.mapNotNull { (it as? ReportRoute)?.photoPath } + draft.photoPaths()).toSet()

@Composable
private fun rememberCaptureDraft(routes: NavBackStack<NavKey>, onRestore: (Set<String>) -> Unit): ReportDraft {
    val draft = rememberSerializable(serializer = ReportDraft.serializer()) { ReportDraft() }
    if (routes.none { it.isCaptureRoute() }) SideEffect { draft.clear() }
    val currentOnRestore by rememberUpdatedState(onRestore)
    DisposableEffect(Unit) {
        currentOnRestore(keptPhotos(routes = routes, draft = draft))
        onDispose {}
    }
    return draft
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

@Composable
private fun RoleProvider.hasWork(): Boolean {
    val account by account.collectAsStateWithLifecycle()
    return account.hasStaffRole
}
