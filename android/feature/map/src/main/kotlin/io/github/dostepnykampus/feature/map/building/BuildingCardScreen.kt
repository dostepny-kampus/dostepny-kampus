/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.GroupContainer
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.OfflinePill
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.designsystem.bar.PinnedButtonKind
import io.github.dostepnykampus.core.designsystem.marking.MarkingSign
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.map.R

/**
 * V2 Karta budynku: nagłówek z symbolem i nazwą z danych (budynek bez nazwy ma sam symbol), który TalkBack czyta jako
 * jeden, np. „Budynek A-1, Gmach Główny”, z fokusem po otwarciu; poza [CardMode.Direct] obok niego „Zamknij”. Pod
 * nim wiersz o wejściach, a bez danych o wejściach cichy napis, i wejścia z oznaczeniem, zalecane pierwsze. Czynności
 * stoją w przewijanej treści: „Sprawdź, którędy wejść” zawsze, „Pokaż budynek na mapie” w [CardMode.isWhole]; grupy
 * „Wejścia” i „Bariery w budynku” ma pełny opis, a pasek „Wstecz” tylko [CardMode.Direct].
 * Nieznany symbol: „budynku nie ma w telefonie”. Bez internetu nad nagłówkiem stoi [OfflinePill] bez daty.
 *
 * Wymagania: US2, BR-25, US2.8, NFR-18, US2.7, NFR-10
 *
 * @param symbol symbol z trasy, widoczny w nagłówku także w trakcie wczytywania
 * @param mode mała karta nad mapą, karta rozwinięta z niej, karta na cały ekran nad mapą albo pełny ekran od razu
 * @param onCheckEntrances „Sprawdź, którędy wejść” otwiera V3
 * @param onShowOnMap „Pokaż budynek na mapie” zaznacza budynek na V1
 * @param onBack „Wstecz” w [CardMode.Direct]
 * @param onClose „Zamknij” w nagłówku karty nad mapą
 * @param modifier wymiary nadane przez arkusz; mała karta ma wysokość treści, pełny ekran wypełnia całe miejsce
 * @param onReportEntrance „Zgłoś barierę przy wejściu” dla identyfikatora wejścia
 * @param onShowEntrance „Pokaż wejście na mapie” dla identyfikatora wejścia zalecanego
 * @param onReportRemoval „Zgłoś, że bariery nie ma” dla identyfikatora zgłoszenia; `null` ukrywa przycisk
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
    onReportRemoval: ((String) -> Unit)? = null,
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
        onReportRemoval = onReportRemoval,
    )
}

/**
 * V2 dla gotowego stanu [card] i [isOnline]; „Spróbuj ponownie” to [onRetry], „Pokaż na planie” to [onShowPlan]
 * z identyfikatorem piętra, „Oznacz obejrzane piętra” pod grupą „Piętra” to [onMarkFloors] (`null` ukrywa przycisk),
 * reszta jak w [BuildingCardScreen].
 */
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
    onReportRemoval: ((String) -> Unit)? = null,
    onShowPlan: (String) -> Unit = {},
    onMarkFloors: (() -> Unit)? = null,
) {
    val isFullScreen = mode != CardMode.Compact
    Column(if (isFullScreen) modifier.fillMaxSize() else modifier) {
        if (mode == CardMode.Direct) BackHeader(title = symbol, onBack = onBack)
        Column(
            modifier = Modifier.weight(1f, isFullScreen).verticalScroll(rememberScrollState()).padding(Odstepy.m),
            verticalArrangement = Arrangement.spacedBy(Odstepy.s),
        ) {
            if (!isOnline) OfflinePill()
            CardHeader(symbol = symbol, card = card, mode = mode, onClose = onClose)
            if (card is BuildingCardUiState.Ready || card.isWaiting(isFullScreen)) {
                CardActions(mode = mode, onCheckEntrances = onCheckEntrances, onShowOnMap = onShowOnMap)
            }
            FactsOrMissing(
                card = card,
                isFullScreen = isFullScreen,
                onReportEntrance = onReportEntrance,
                onShowEntrance = onShowEntrance,
                onReportRemoval = onReportRemoval,
                onRetry = onRetry,
                onShowPlan = onShowPlan,
                onMarkFloors = onMarkFloors,
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
    onReportRemoval: ((String) -> Unit)?,
    onRetry: () -> Unit,
    onShowPlan: (String) -> Unit,
    onMarkFloors: (() -> Unit)?,
) {
    if (card is BuildingCardUiState.Ready && isFullScreen) {
        BuildingFacts(
            card = card,
            onReportEntrance = onReportEntrance,
            onShowEntrance = onShowEntrance,
            onReportRemoval = onReportRemoval,
            onShowPlan = onShowPlan,
            onMarkFloors = onMarkFloors,
        )
    }
    if (card == BuildingCardUiState.Missing) MissingBuilding(onRetry = onRetry)
}

/**
 * Wiersz o wejściach pod nagłówkiem: znak oznaczenia z mapy i słowo o wejściach budynku.
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
    MarkingSign(marking = marking, label = stringResource(word))
}

@Composable
private fun BuildingFacts(
    card: BuildingCardUiState.Ready,
    onReportEntrance: (String) -> Unit,
    onShowEntrance: (String) -> Unit,
    onReportRemoval: ((String) -> Unit)?,
    onShowPlan: (String) -> Unit,
    onMarkFloors: (() -> Unit)?,
) {
    val common = commonAssessment(card.entrances)
    common?.let { AssessmentLine(it) }
    GroupContainer(title = stringResource(R.string.karta_wejscia)) {
        if (card.entrances.isEmpty()) {
            Text(text = stringResource(R.string.karta_wejscia_brak), style = MaterialTheme.typography.bodyLarge)
        }
        card.entrances.forEach { entrance ->
            EntranceRow(
                listed = entrance,
                withAssessment = common == null,
                onShowEntrance = onShowEntrance.takeIf { entrance.isRecommended },
                onReportEntrance = onReportEntrance,
            )
        }
    }
    BarriersGroup(card, onReportRemoval)
    card.floors?.let { LiftToiletGroup(it) }
    FloorsGroup(facts = card.floors, onShowPlan = onShowPlan)
    onMarkFloors?.let { onMark ->
        OutlinedButton(onClick = onMark, modifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.minimum)) {
            Text(stringResource(R.string.karta_oznacz_pietra))
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
private fun CardActions(mode: CardMode, onCheckEntrances: () -> Unit, onShowOnMap: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CeleDotyku.odstep)) {
        CardButton(text = stringResource(R.string.karta_sprawdz), onClick = onCheckEntrances)
        if (mode.isWhole) {
            CardButton(stringResource(R.string.karta_pokaz), onShowOnMap, outlined = true)
        }
    }
}

@Composable
private fun CardButton(text: String, onClick: () -> Unit, outlined: Boolean = false) {
    val kind = if (outlined) PinnedButtonKind.Outlined else PinnedButtonKind.Filled
    PinnedButton(text = text, onClick = onClick, modifier = Modifier.fillMaxWidth(), kind = kind)
}

/** Tryb treści V2 Karta budynku. Wymagania: US2.8, US2.9 */
enum class CardMode {
    /** Mała karta nad mapą: nagłówek z „Zamknij”, wiersz o wejściach i „Sprawdź, którędy wejść”. */
    Compact,

    /** Karta rozwinięta nad mapą: nagłówek z „Zamknij” i pod nim cały opis budynku. */
    Expanded,

    /**
     * Pełny ekran otwarty od razu, bez małej karty: szerokie okno, poziomo albo wybór z listy przy TalkBack.
     *
     * Wymagania: NFR-19, NFR-51
     */
    Direct,

    /** Karta przy dużej czcionce na cały ekran nad mapą: nagłówek z „Zamknij” i cały opis. Wymagania: NFR-20, US2.9 */
    Large,
}

/** Czy karta stoi od razu na całym ekranie, bez małej karty po drodze. Wymagania: NFR-19, NFR-20, US2.9 */
val CardMode.isWhole: Boolean
    get() = this == CardMode.Direct || this == CardMode.Large
