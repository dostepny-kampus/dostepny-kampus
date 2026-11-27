/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
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
import io.github.dostepnykampus.app.navigation.AppScreen
import io.github.dostepnykampus.app.navigation.SheetSceneStrategy
import io.github.dostepnykampus.app.navigation.TopLevel
import io.github.dostepnykampus.app.navigation.WIDE_WINDOW_DP
import io.github.dostepnykampus.app.navigation.currentCardWindow
import io.github.dostepnykampus.app.navigation.isCaptureRoute
import io.github.dostepnykampus.app.navigation.sceneStrategies
import io.github.dostepnykampus.app.navigation.selectedItem
import io.github.dostepnykampus.app.navigation.startBackStack
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.designsystem.bar.BottomBar
import io.github.dostepnykampus.core.designsystem.bar.BottomBarItem
import io.github.dostepnykampus.core.designsystem.bar.BottomBarItems
import io.github.dostepnykampus.core.designsystem.bar.NavigationIcons
import io.github.dostepnykampus.core.designsystem.bar.SideBar
import io.github.dostepnykampus.core.designsystem.readableWidth
import io.github.dostepnykampus.core.model.hasStaffRole
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.report.ReportDraft
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute

private val WideWindow = WIDE_WINDOW_DP.dp

private val RestartsSaver =
    listSaver<SnapshotStateList<Int>, Int>(save = { it.toList() }, restore = { it.toMutableStateList() })

/**
 * Oboczka aplikacji: ekran z wierzchu historii i pasek nawigacji na ekranach, które go mają; w oknie od 600 dp pasek
 * stoi z boku, inaczej na dole. Historia zaczyna się od V5 przy [firstLaunch], inaczej od V1, i przetrwa odtworzenie
 * aktywności. [roles] wybiera cele Gościa i napis czwartej pozycji; [onFirstHelpLeft] dostaje znać, gdy instrukcja
 * pierwszego uruchomienia zejdzie z wierzchu, a [onChooseMap] zapisuje wybór „Mapa” po „Pokaż budynek na mapie”.
 * [content] rysuje ekran trasy; cały pasek jest dla treści zużytym wcięciem. V2 jest arkuszem nad V1
 * ([SheetSceneStrategy]) albo osobnym ekranem karty `Direct`; [onRestore] dostaje zdjęcia odtworzonych szkiców.
 *
 * Wymagania: US5.1, NFR-22, US2.9, NFR-51
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
                    ) { route -> RouteContent(route) { navigator.content(route) } }
                },
            entryDecorators =
                listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
        )
    BoxWithConstraints(modifier = modifier) {
        val isWide = maxWidth >= WideWindow
        val selected = selectedItem(navigator.current)?.ordinal
        var bar by remember { mutableStateOf(IntSize.Zero) }
        NavigationSuiteScaffoldLayout(
            navigationSuite = {
                if (selected != null) {
                    ShellBar(items = items, selected = selected, isWide = isWide, navigator = navigator) { bar = it }
                }
            },
            navigationSuiteType = if (isWide) NavigationSuiteType.NavigationRail else NavigationSuiteType.NavigationBar,
            content = {
                NavDisplay(
                    entries = entries,
                    modifier =
                        Modifier
                            .consumeWindowInsets(insetsTakenByBar(bar, selected != null, isWide))
                            .windowInsetsPadding(wideNavigationInsets(isWide)),
                    sceneStrategies = currentCardWindow().sceneStrategies(navigator.rememberSheetStrategy()),
                    onBack = { navigator.back() },
                )
            },
        )
    }
}

@Composable
private fun RouteContent(
    route: NavKey,
    content: @Composable () -> Unit,
) {
    val frame =
        if (route is MapRoute || route is BuildingRoute) {
            Modifier
        } else {
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer).readableWidth()
        }
    Box(modifier = frame, propagateMinConstraints = true) { content() }
}

@Composable
private fun ShellBar(
    items: BottomBarItems,
    selected: Int,
    isWide: Boolean,
    navigator: AppNavigator,
    onSize: (IntSize) -> Unit,
) {
    val onSelect = { index: Int -> navigator.open(TopLevel.entries[index]) }
    val measured = Modifier.onSizeChanged(onSize)
    if (isWide) {
        SideBar(items = items, selectedIndex = selected, onSelect = onSelect, modifier = measured)
    } else {
        BottomBar(items = items, selectedIndex = selected, onSelect = onSelect, modifier = measured)
    }
}

@Composable
private fun insetsTakenByBar(
    bar: IntSize,
    isShown: Boolean,
    isWide: Boolean,
): WindowInsets {
    val rail = WindowInsets(left = bar.width, right = bar.width).only(WindowInsetsSides.Start)
    val bottomBar = WindowInsets(bottom = bar.height)
    return when {
        !isShown -> WindowInsets(0)
        isWide -> WindowInsets.safeDrawing.only(WindowInsetsSides.Start).union(rail)
        else -> NavigationBarDefaults.windowInsets.only(WindowInsetsSides.Bottom).union(bottomBar)
    }
}

@Composable
private fun wideNavigationInsets(isWide: Boolean): WindowInsets =
    if (isWide) {
        WindowInsets.navigationBars.union(WindowInsets.mandatorySystemGestures).only(WindowInsetsSides.Bottom)
    } else {
        WindowInsets(0)
    }

/** Pliki zdjęć odtworzonych szkiców: zgłoszenia szybkiego w [routes] i C2 w [draft]. */
internal fun keptPhotos(
    routes: List<NavKey>,
    draft: ReportDraft,
): Set<String> = (routes.mapNotNull { (it as? ReportRoute)?.photoPath } + draft.photoPaths()).toSet()

@Composable
private fun rememberCaptureDraft(
    routes: NavBackStack<NavKey>,
    onRestore: (Set<String>) -> Unit,
): ReportDraft {
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

@Composable
private fun AppNavigator.rememberSheetStrategy(): SheetSceneStrategy = remember(this) { SheetSceneStrategy(mapMotion) }
