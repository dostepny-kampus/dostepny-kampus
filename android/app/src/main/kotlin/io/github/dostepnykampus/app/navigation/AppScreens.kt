/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.quick.QuickReportActions
import io.github.dostepnykampus.feature.capture.quick.QuickReportScreen
import io.github.dostepnykampus.feature.help.HelpScreen
import io.github.dostepnykampus.feature.help.navigation.HelpRoute
import io.github.dostepnykampus.feature.inspection.navigation.CollectorMarkersRoute
import io.github.dostepnykampus.feature.inspection.navigation.ExportRoute
import io.github.dostepnykampus.feature.inspection.navigation.MarkersRoute
import io.github.dostepnykampus.feature.inspection.navigation.ProgressRoute
import io.github.dostepnykampus.feature.map.building.BuildingCardContent
import io.github.dostepnykampus.feature.map.building.BuildingCardUiState
import io.github.dostepnykampus.feature.map.building.BuildingCardViewModel
import io.github.dostepnykampus.feature.map.building.CardMode
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import io.github.dostepnykampus.feature.review.navigation.ReviewQueueRoute
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/** Szerokość okna w dp, od której pasek nawigacji stoi z boku, a V2 otwiera się od razu na pełnym ekranie. */
internal const val WIDE_WINDOW_DP = 600

/** Tryb karty z trasy: `Large` od skali czcionki 1,5, inaczej `Expanded`. Wymagania: US2.9, NFR-20 */
internal fun cardModeFor(fontScale: Float): CardMode =
    if (fontScale >= LARGE_FONT_SCALE) CardMode.Large else CardMode.Expanded

/** Bieżące okno aplikacji dla trybu karty V2: skala czcionki [fontScale], szerokość [widthDp] i [isLandscape]. */
internal data class CardWindow(val fontScale: Float, val widthDp: Int, val isLandscape: Boolean)

/**
 * Tryb karty V2 w oknie: `Direct` w oknie od 600 dp albo poziomo, inaczej jak [cardModeFor].
 *
 * Wymagania: NFR-51, NFR-22
 */
internal fun CardWindow.cardMode(): CardMode = if (isWideOrLandscape) CardMode.Direct else cardModeFor(fontScale)

/** Czy okno ma od 600 dp szerokości albo leży poziomo. Wymagania: NFR-51, NFR-22 */
internal val CardWindow.isWideOrLandscape: Boolean
    get() = widthDp >= WIDE_WINDOW_DP || isLandscape

/** Bieżące okno z kompozycji, więc tryb karty idzie za obrotem i zmianą czcionki w trakcie (UI-05). */
@Composable
internal fun currentCardWindow(): CardWindow {
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    val widthDp = with(density) { window.width.toDp() }.value.roundToInt()
    return CardWindow(density.fontScale, widthDp, isLandscape = window.width > window.height)
}

/**
 * Karta V2 w arkuszu: tryb [mode], „Wstecz” w górnym pasku pełnego ekranu [onBack], „Zamknij” w nagłówku [onClose],
 * przewijanie treści [scroll] i rozwinięcie niskiej karty [onExpand]. [isRising]: niska karta jedzie w górę albo stoi
 * wyżej, a [mode] to już tryb rozwiniętej.
 *
 * Wymagania: US2.8
 */
internal data class SheetCard(
    val mode: CardMode,
    val onBack: () -> Unit,
    val onClose: () -> Unit,
    val scroll: ScrollState,
    val onExpand: () -> Unit = {},
    val isRising: Boolean = false,
)

/**
 * Karta V2: `Direct`, gdy okno go wymaga albo V2 otwarto od razu na pełnym ekranie, inaczej tryb z rozwinięcia
 * arkusza; „Wstecz” jak wstecz telefonu, „Zamknij” zamyka V2. Przewijanie treści startuje od
 * [BuildingRoute.scroll], także gdy treść dochodzi po pierwszej klatce, i wraca na początek, gdy arkusz stanie niżej
 * niż na całym oknie. Karta schodząca
 * z ekranu zostawia nawigatorowi swoją wysokość i przewinięcie ([keepCard]).
 *
 * Wymagania: NFR-19, US2.8, US2.9, NFR-15
 */
@Composable
internal fun AppNavigator.sheetCard(): SheetCard {
    val window = currentCardWindow()
    val layout = LocalSheetPadding.current
    val sheet = SheetState(isExpanded = layout.isExpanded || layout.isRising)
    val route = current as? BuildingRoute
    val isDirect = window.cardMode() == CardMode.Direct || route?.isDirect == true
    val mode = if (isDirect) CardMode.Direct else sheet.cardMode(window.fontScale)
    val back = checkNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
    val start = route?.scroll ?: 0
    val scroll = rememberScrollState(initial = start)
    LaunchedEffect(scroll) { scroll.scrollToOnceContentReaches(target = start) }
    LaunchedEffect(layout.isFull) { if (!layout.isFull) scroll.animateScrollTo(0) }
    KeepCardOnLeave(route = route, isExpanded = layout.isExpanded, scroll = scroll)
    return SheetCard(
        mode = mode,
        onBack = back::onBackPressed,
        onClose = { back() },
        scroll = scroll,
        onExpand = layout.onExpand,
        isRising = layout.isRising && !layout.isExpanded,
    )
}

private suspend fun ScrollState.scrollToOnceContentReaches(target: Int) {
    if (target == 0) return
    snapshotFlow { isScrollInProgress || maxValue in target..<Int.MAX_VALUE }.first { it }
    if (!isScrollInProgress && value < target) scrollTo(target)
}

@Composable
private fun AppNavigator.KeepCardOnLeave(route: BuildingRoute?, isExpanded: Boolean, scroll: ScrollState) {
    val isExpandedNow by rememberUpdatedState(isExpanded)
    DisposableEffect(route) {
        onDispose { route?.let { keepCard(it.copy(isExpanded = isExpandedNow, scroll = scroll.value)) } }
    }
}

/**
 * Ekran trasy [route] z czynnościami podpiętymi do nawigatora. V1 to mapa z `feature:map` ze stanem w ViewModel
 * z grafu Hilt, V2 karta budynku, C1, C2 i C3 z `feature:capture` i V5 z `feature:help`, reszta
 * to zaślepki: nagłówek „Wstecz” z nazwą z listy ekranów i przyciski czynności bez danych. Ekran z `feature:*`
 * zastępuje swoją gałąź i dostaje czynności jako lambdy. Zaślepki nie zapamiętują stanu, więc nie muszą się
 * odświeżać osobno od nawigatora. Gdy konto staje się zalogowane (przełącznik ról w debug, potem logowanie),
 * strona logowania przechodzi do V4 krok 1 albo L2.
 *
 * Wymagania: US14.2
 */
@Composable
@NonRestartableComposable
internal fun AppNavigator.AppScreen(route: NavKey) {
    when (route) {
        is BuildingRoute -> HiltBuildingCard(route.symbol)
        is ReportRoute -> QuickReport(route)
        ProgressRoute, ExportRoute, is MarkersRoute, is CollectorMarkersRoute -> Inspection(route)
        ReviewQueueRoute -> ReviewQueue()
        is HelpRoute -> HelpScreen(route.isFirstLaunch, onOpenMap = { backToMap() }, onLeave = { back() })
        is SignInRoute, AccountRoute -> Account(route)
        is MapRoute -> HiltMapScreen(focus = route.focus)
        else -> Capture(route)
    }
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
 * bez wybranego wejścia, a Gościa najpierw do L1. „Pokaż wejście na mapie” i wiersz wejścia prowadzą do V1
 * z zaznaczonym wejściem tego budynku, a „Pokaż na planie” do V1 z planem tego piętra. „Zgłoś, że bariery nie ma”
 * widzi tylko Zbierający i prowadzi do C7, „Oznacz obejrzane piętra” do C5, a „Zmień oznaczenie” do C6.
 * [onRetry] to „Spróbuj ponownie”.
 *
 * Wymagania: US4.6, US14.2, US3.3, US2.1, US8.4, US15.1, US17.2
 */
@Composable
internal fun AppNavigator.BuildingCard(
    symbol: String,
    state: BuildingCardUiState,
    isOnline: Boolean,
    onRetry: () -> Unit,
) {
    val card = sheetCard()
    val isDirect = card.mode == CardMode.Direct
    val mode = if (card.isRising && state !is BuildingCardUiState.Ready) CardMode.Compact else card.mode
    val roles by account.collectAsStateWithLifecycle()
    val isCollector = StaffRole.COLLECTOR in (roles as? Account.SignedIn)?.staffRoles.orEmpty()
    BuildingCardContent(
        symbol = symbol,
        mode = mode,
        card = state,
        isOnline = isOnline,
        onShowOnMap = { showOnMap(MapFocus.Building(symbol, command = System.nanoTime())) },
        onBack = card.onBack,
        onClose = card.onClose,
        onReport = { report() },
        onShowEntrance = { showOnMap(MapFocus.Entrance(id = it, buildingSymbol = symbol, isFullSheet = isDirect)) },
        onReportRemoval = this::openRemoved.takeIf { isCollector },
        onReplaceBarrier = this::openReplace.takeIf { isCollector },
        onRetry = onRetry,
        onShowPlan = { showOnMap(MapFocus.Building(symbol, command = System.nanoTime(), floor = it)) },
        onMarkFloors = { openFloorMarking(symbol) }.takeIf { isCollector },
        onChangeMarking = { id: String -> openMarking(id, buildingSymbol = symbol) }.takeIf { isCollector },
        onExpand = card.onExpand,
        scroll = card.scroll,
    )
}

@Composable
@NonRestartableComposable
private fun AppNavigator.QuickReport(route: ReportRoute) {
    val actions =
        QuickReportActions(
            onNext = { nextReportStep(it) },
            onSent = { reportSent(isSavedOffline = it) },
            onBack = { back() },
            onBackToMap = { backToMap() },
        )
    QuickReportScreen(route = route, actions = actions)
}
