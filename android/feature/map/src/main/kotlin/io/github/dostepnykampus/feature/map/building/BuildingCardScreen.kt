/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.BackHeader
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.GroupContainer
import io.github.dostepnykampus.core.designsystem.MarkingChip
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.PinnedButton
import io.github.dostepnykampus.core.designsystem.PinnedButtonKind
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.controls.OfflinePill

/**
 * V2 Karta budynku: nagłówek z symbolem i nazwą z danych (budynek bez nazwy ma sam symbol), który TalkBack czyta jako
 * jeden, np. „Budynek A-1, Gmach Główny”, z fokusem po otwarciu. Pod nim wiersz o wejściach i wejścia z oznaczeniem
 * tekstem i ikoną, zalecane pierwsze. Czynności stoją w przewijanej treści: „Sprawdź, którędy wejść” zawsze, „Zamknij”
 * w [CardMode.Compact], „Pokaż budynek na mapie” w [CardMode.Direct]; pasek „Wstecz” i grupę „Wejścia” ma pełny ekran.
 * Nieznany symbol: „budynku nie ma w telefonie”. Bez internetu nad nagłówkiem stoi [OfflinePill] bez daty.
 *
 * Wymagania: US2, BR-25, US2.8, NFR-18, US2.7, NFR-10
 *
 * @param symbol symbol z trasy, widoczny w nagłówku także w trakcie wczytywania
 * @param mode mała karta nad mapą, pełny ekran rozwinięty z niej albo pełny ekran otwarty od razu
 * @param onCheckEntrances „Sprawdź, którędy wejść” otwiera V3
 * @param onShowOnMap „Pokaż budynek na mapie” zaznacza budynek na V1
 * @param onBack „Wstecz”
 * @param onClose „Zamknij” małej karty
 * @param modifier wymiary nadane przez arkusz; mała karta ma wysokość treści, pełny ekran wypełnia całe miejsce
 * @param onReportEntrance „Zgłoś barierę przy wejściu” dla identyfikatora wejścia
 * @param onShowEntrance „Pokaż wejście na mapie” dla identyfikatora wejścia zalecanego
 * @param viewModel stan karty; w aplikacji z grafu Hilt, w testach z podróbką katalogu
 */
@Composable
fun BuildingCardScreen(
    symbol: String,
    mode: CardMode,
    onCheckEntrances: () -> Unit,
    onShowOnMap: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onReportEntrance: (String) -> Unit = {},
    onShowEntrance: (String) -> Unit = {},
    viewModel: BuildingCardViewModel =
        hiltViewModel<BuildingCardViewModel, BuildingCardViewModel.Factory>(
            creationCallback = { factory -> factory.create(symbol) },
        ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.online.collectAsStateWithLifecycle()
    BuildingCardContent(
        symbol = symbol,
        mode = mode,
        card = state,
        isOnline = isOnline,
        onCheckEntrances = onCheckEntrances,
        onShowOnMap = onShowOnMap,
        onBack = onBack,
        onClose = onClose,
        onReportEntrance = onReportEntrance,
        onRetry = viewModel::retry,
        modifier = modifier,
        onShowEntrance = onShowEntrance,
    )
}

/** V2 dla gotowego stanu [card] i [isOnline]; „Spróbuj ponownie” to [onRetry], reszta jak w [BuildingCardScreen]. */
@Composable
fun BuildingCardContent(
    symbol: String,
    mode: CardMode,
    card: BuildingCardUiState,
    isOnline: Boolean,
    onCheckEntrances: () -> Unit,
    onShowOnMap: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onReportEntrance: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onShowEntrance: (String) -> Unit = {},
) {
    val isFullScreen = mode != CardMode.Compact
    Column(if (isFullScreen) modifier.fillMaxSize() else modifier) {
        if (isFullScreen) BackHeader(title = symbol, onBack = onBack)
        Column(
            modifier = Modifier.weight(1f, isFullScreen).verticalScroll(rememberScrollState()).padding(Odstepy.m),
            verticalArrangement = Arrangement.spacedBy(Odstepy.s),
        ) {
            if (!isOnline) OfflinePill()
            if (card is BuildingCardUiState.Ready) {
                SymbolOverName(symbol = symbol, name = card.name)
                EntranceSummaryRow(entranceSummary(card.entrances))
                CardActions(
                    mode = mode,
                    onCheckEntrances = onCheckEntrances,
                    onShowOnMap = onShowOnMap,
                    onClose = onClose,
                )
            }
            FactsOrMissing(
                card = card,
                isFullScreen = isFullScreen,
                onReportEntrance = onReportEntrance,
                onShowEntrance = onShowEntrance,
                onRetry = onRetry,
            )
        }
    }
}

@Composable
private fun FactsOrMissing(
    card: BuildingCardUiState,
    isFullScreen: Boolean,
    onReportEntrance: (String) -> Unit,
    onShowEntrance: (String) -> Unit,
    onRetry: () -> Unit,
) {
    if (card is BuildingCardUiState.Ready && isFullScreen) BuildingFacts(card, onReportEntrance, onShowEntrance)
    if (card == BuildingCardUiState.Missing) MissingBuilding(onRetry = onRetry)
}

@Composable
private fun SymbolOverName(symbol: String, name: String?) {
    val spoken =
        name?.let { stringResource(R.string.karta_czytaj, symbol, it) } ?: stringResource(R.string.mapa_budynek, symbol)
    val focus = remember { FocusRequester() }
    Column(
        modifier =
            Modifier
                .semantics(mergeDescendants = true) { contentDescription = spoken }
                .focusRequester(focus)
                .focusable(),
    ) {
        Text(text = symbol, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        name?.let { Text(text = it, style = MaterialTheme.typography.headlineMedium) }
    }
    LaunchedEffect(focus) { withFrameMillis { focus.requestFocus() } }
}

/**
 * Wiersz o wejściach pod nagłówkiem: słowo i ikona oznaczenia jak w [MarkingChip] (kształt, nie
 * para zielony/czerwony).
 *
 * Wymagania: US2.8, NFR-21
 */
@Composable
internal fun EntranceSummaryRow(summary: EntranceSummary) {
    val (marking, word) =
        when (summary) {
            EntranceSummary.Accessible -> EntranceMarking.ACCESSIBLE to R.string.karta_wiersz_dostepne
            EntranceSummary.NoAccessible -> EntranceMarking.INACCESSIBLE to R.string.karta_wiersz_niedostepne
            EntranceSummary.NoData -> EntranceMarking.NO_DATA to R.string.karta_wiersz_brak_danych
        }
    MarkingChip(marking = marking, label = stringResource(word))
}

@Composable
private fun BuildingFacts(
    card: BuildingCardUiState.Ready,
    onReportEntrance: (String) -> Unit,
    onShowEntrance: (String) -> Unit,
) {
    val common = commonAssessment(card.entrances)
    common?.let { AssessmentLine(it) }
    GroupContainer(title = stringResource(R.string.karta_wejscia)) {
        card.entrances.forEach { entrance ->
            EntranceRow(
                listed = entrance,
                withAssessment = common == null,
                onShowEntrance = onShowEntrance.takeIf { entrance.isRecommended },
                onReportEntrance = onReportEntrance,
            )
        }
    }
}

@Composable
internal fun MissingBuilding(onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.m)) {
        Text(text = stringResource(R.string.karta_brak_w_telefonie), style = MaterialTheme.typography.bodyLarge)
        PinnedButton(
            text = stringResource(R.string.karta_sprobuj),
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CardActions(mode: CardMode, onCheckEntrances: () -> Unit, onShowOnMap: () -> Unit, onClose: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CeleDotyku.odstep)) {
        CardButton(text = stringResource(R.string.karta_sprawdz), onClick = onCheckEntrances)
        when (mode) {
            CardMode.Compact -> CardButton(stringResource(R.string.mapa_zamknij), onClose, outlined = true)
            CardMode.Expanded -> Unit
            CardMode.Direct -> CardButton(stringResource(R.string.karta_pokaz), onShowOnMap, outlined = true)
        }
    }
}

@Composable
private fun CardButton(text: String, onClick: () -> Unit, outlined: Boolean = false) {
    val kind = if (outlined) PinnedButtonKind.Outlined else PinnedButtonKind.Filled
    PinnedButton(text = text, onClick = onClick, modifier = Modifier.fillMaxWidth(), kind = kind)
}
