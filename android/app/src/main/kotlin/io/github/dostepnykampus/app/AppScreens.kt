/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.measure
import io.github.dostepnykampus.app.navigation.openReportForm
import io.github.dostepnykampus.app.navigation.reportSaved
import io.github.dostepnykampus.core.designsystem.BackHeader
import io.github.dostepnykampus.feature.auth.navigation.AccountRoute
import io.github.dostepnykampus.feature.auth.navigation.SignInRoute
import io.github.dostepnykampus.feature.capture.MeasureScreen
import io.github.dostepnykampus.feature.capture.QuickReportActions
import io.github.dostepnykampus.feature.capture.QuickReportScreen
import io.github.dostepnykampus.feature.capture.ReportFormScreen
import io.github.dostepnykampus.feature.capture.navigation.MeasureRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.help.HelpScreen
import io.github.dostepnykampus.feature.help.navigation.HelpRoute
import io.github.dostepnykampus.feature.map.BuildingCardContent
import io.github.dostepnykampus.feature.map.BuildingCardUiState
import io.github.dostepnykampus.feature.map.BuildingCardViewModel
import io.github.dostepnykampus.feature.map.CardMode
import io.github.dostepnykampus.feature.map.EntrancesScreen
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.EntrancesRoute
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import kotlin.math.roundToInt

private const val LARGE_FONT_SCALE = 1.5f

/** Szerokość okna w dp, od której pasek nawigacji stoi z boku, a V2 otwiera się od razu na pełnym ekranie. */
internal const val WIDE_WINDOW_DP = 600

/** Tryb karty z trasy (US2.9, NFR-20): `Direct` od skali czcionki 1,5, inaczej `Expanded`. */
internal fun cardModeFor(fontScale: Float): CardMode =
    if (fontScale >= LARGE_FONT_SCALE) CardMode.Direct else CardMode.Expanded

/** Bieżące okno aplikacji dla trybu karty V2: skala czcionki [fontScale], szerokość [widthDp] i [isLandscape]. */
internal data class CardWindow(val fontScale: Float, val widthDp: Int, val isLandscape: Boolean)

/** Tryb karty V2 w oknie: `Direct` w oknie od 600 dp albo poziomo (NFR-51, NFR-22), inaczej jak [cardModeFor]. */
internal fun CardWindow.cardMode(): CardMode =
    if (widthDp >= WIDE_WINDOW_DP || isLandscape) CardMode.Direct else cardModeFor(fontScale)

/** Bieżące okno z kompozycji, więc tryb karty idzie za obrotem i zmianą czcionki w trakcie (UI-05). */
@Composable
internal fun currentCardWindow(): CardWindow {
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    val widthDp = with(density) { window.width.toDp() }.value.roundToInt()
    return CardWindow(density.fontScale, widthDp, isLandscape = window.width > window.height)
}

/** Karta V2 w arkuszu: tryb [mode], „Wstecz” w górnym pasku [onBack] i „Zamknij” małej karty [onClose] (US2.8). */
internal data class SheetCard(val mode: CardMode, val onBack: () -> Unit, val onClose: () -> Unit)

/**
 * Karta V2: `Direct`, gdy okno go wymaga albo V2 otwarto od razu na pełnym ekranie (NFR-19), inaczej tryb z rozwinięcia
 * arkusza; „Wstecz” jak wstecz telefonu, „Zamknij” zamyka V2 (US2.8, US2.9, NFR-15).
 */
@Composable
internal fun AppNavigator.sheetCard(): SheetCard {
    val window = currentCardWindow()
    val sheet = SheetState(isExpanded = LocalSheetPadding.current.isExpanded)
    val isDirect = window.cardMode() == CardMode.Direct || (current as? BuildingRoute)?.isDirect == true
    val mode = if (isDirect) CardMode.Direct else sheet.cardMode(window.fontScale)
    val back = checkNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
    return SheetCard(mode, onBack = back::onBackPressed, onClose = { back() })
}

private typealias Action = Pair<String, () -> Unit>

/**
 * Ekran trasy [route] z czynnościami podpiętymi do nawigatora. V1 to mapa z `feature:map` ze stanem w ViewModel
 * z grafu Hilt, V2 karta budynku, V3 „Którędy wejść”, C1 i C2 z `feature:capture` i V5 z `feature:help`, reszta
 * to zaślepki: nagłówek „Wstecz” z nazwą z listy ekranów i przyciski czynności bez danych. Ekran z `feature:*`
 * zastępuje swoją gałąź i dostaje czynności jako lambdy. Zaślepki nie zapamiętują stanu, więc nie muszą się
 * odświeżać osobno od nawigatora. Gdy konto staje się zalogowane (przełącznik ról w debug, potem logowanie),
 * strona logowania przechodzi do V4 krok 1 albo L2 (US14.2).
 */
@Composable
@NonRestartableComposable
internal fun AppNavigator.AppScreen(route: NavKey) {
    val save = { reportSaved() }
    when (route) {
        is BuildingRoute -> HiltBuildingCard(route.symbol)
        is EntrancesRoute -> Entrances(route.symbol)
        is ReportRoute -> QuickReport(route)
        MeasureRoute -> MeasureScreen(onBack = { back() }, onNext = { openReportForm(it) })
        is ReportFormRoute -> ReportFormScreen(route = route, onLeave = { back() }, onSave = save, draft = reportDraft)
        is HelpRoute -> HelpScreen(route.isFirstLaunch, onOpenMap = { backToMap() }, onLeave = { back() })
        is SignInRoute, AccountRoute -> Account(route)
        is MapRoute -> HiltMapScreen(focus = route.focus)
        else -> Unit
    }
}

@Composable
private fun AppNavigator.Entrances(symbol: String) {
    EntrancesScreen(
        symbol = symbol,
        onShowEntrance = { showOnMap(MapFocus.Entrance(id = it, buildingSymbol = symbol)) },
        onBack = { back() },
    )
}

@Composable
private fun AppNavigator.HiltBuildingCard(
    symbol: String,
    viewModel: BuildingCardViewModel =
        hiltViewModel<BuildingCardViewModel, BuildingCardViewModel.Factory>(
            creationCallback = { factory -> factory.create(symbol) },
        ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.online.collectAsStateWithLifecycle()
    BuildingCard(symbol = symbol, state = state, isOnline = isOnline, onRetry = viewModel::retry)
}

/**
 * V2 ze stanem [state] i czynnościami podpiętymi do nawigatora; „Zgłoś barierę przy wejściu” prowadzi do V4
 * z wybranym wejściem, a Gościa najpierw do L1 (US4.6, US14.2). „Pokaż wejście na mapie” prowadzi do V1
 * z zaznaczonym wejściem tego budynku (US3.3, US2.1). [onRetry] to „Spróbuj ponownie”.
 */
@Composable
internal fun AppNavigator.BuildingCard(
    symbol: String,
    state: BuildingCardUiState,
    isOnline: Boolean,
    onRetry: () -> Unit,
) {
    val card = sheetCard()
    BuildingCardContent(
        symbol = symbol,
        mode = card.mode,
        card = state,
        isOnline = isOnline,
        onCheckEntrances = { openEntrances(symbol) },
        onShowOnMap = { showOnMap(MapFocus.Building(symbol, command = System.nanoTime())) },
        onBack = card.onBack,
        onClose = card.onClose,
        onReportEntrance = { report(it) },
        onShowEntrance = { showOnMap(MapFocus.Entrance(id = it, buildingSymbol = symbol)) },
        onRetry = onRetry,
    )
}

@Composable
@NonRestartableComposable
private fun AppNavigator.QuickReport(route: ReportRoute) {
    val actions =
        QuickReportActions(
            onNext = { nextReportStep(it) },
            onSent = { reportSent() },
            onBack = { back() },
            onBackToMap = { backToMap() },
        )
    QuickReportScreen(route = route, actions = actions)
}

@Composable
@NonRestartableComposable
private fun AppNavigator.Account(route: NavKey) {
    val isWork = hasWork && route == AccountRoute
    val title = stringResource(if (isWork) R.string.nav_konto_i_praca else R.string.nav_konto)
    when {
        route is SignInRoute -> {
            val isNowSignedIn = isSignedIn
            SideEffect { if (isNowSignedIn) signedIn() }
            Stub(title, stringResource(R.string.nie_teraz) to { backToMap() })
        }

        isWork -> {
            Stub(title, stringResource(R.string.zmierz_i_zglos) to { measure() })
        }

        else -> {
            Stub(title)
        }
    }
}

@Composable
@NonRestartableComposable
private fun AppNavigator.Stub(title: String, vararg actions: Action) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = title, onBack = { back() })
            actions.forEach { (label, onClick) -> TextButton(onClick = onClick) { Text(text = label) } }
        }
    }
}
