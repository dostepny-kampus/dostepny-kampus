/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.OfflinePill
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.designsystem.marking.MarkingSign
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.map.R

/** Znacznik kreski między górą karty a pełnym opisem. */
internal const val DESCRIPTION_LINE_TAG = "kreska-opisu"

/**
 * V2 Karta budynku: nagłówek z symbolem i nazwą z danych (budynek bez nazwy ma sam symbol), który TalkBack czyta jako
 * jeden, np. „Budynek A-1, Gmach Główny”, z fokusem po otwarciu; poza [CardMode.Direct] obok niego „Zamknij”. Pod
 * nim wiersz o wejściach z odpowiedzią, a bez danych o wejściach cichy napis. Czynności stoją w przewijanej treści:
 * „Pokaż wejście na mapie” dla wejścia zalecanego, „Pokaż budynek na mapie” w [CardMode.isWhole]; grupy
 * „Wejścia” i „Bariery w budynku” ma pełny opis, a pasek „Wstecz” tylko [CardMode.Direct].
 * Nieznany symbol: „budynku nie ma w telefonie”. Bez internetu nad nagłówkiem stoi [OfflinePill] bez daty.
 *
 * Wymagania: US2, BR-25, US2.8, US3.2, NFR-18, US2.7, NFR-10
 *
 * @param symbol symbol z trasy, widoczny w nagłówku także w trakcie wczytywania
 * @param mode mała karta nad mapą, karta rozwinięta z niej, karta na cały ekran nad mapą albo pełny ekran od razu
 * @param onShowOnMap „Pokaż budynek na mapie” zaznacza budynek na V1
 * @param onBack „Wstecz” w [CardMode.Direct]
 * @param onClose „Zamknij” w nagłówku karty nad mapą
 * @param modifier wymiary nadane przez arkusz; mała karta ma wysokość treści, pełny ekran wypełnia całe miejsce
 * @param onReport „Zgłoś barierę przy wejściu” bez wybranego wejścia, przy kilku wejściach bez danych
 * @param onShowEntrance „Pokaż wejście na mapie” pod wierszem o wejściach i dotknięcie wiersza na liście wejść,
 *   z identyfikatorem wejścia
 * @param onReportRemoval „Zgłoś, że bariery nie ma” dla identyfikatora zgłoszenia; `null` ukrywa przycisk
 * @param viewModel stan karty; w aplikacji z grafu Hilt, w testach z podróbką katalogu
 */
@Composable
fun BuildingCardScreen(
    symbol: String,
    mode: CardMode,
    onShowOnMap: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onReport: () -> Unit = {},
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
        onShowOnMap = onShowOnMap,
        onBack = onBack,
        onClose = onClose,
        onReport = onReport,
        onRetry = viewModel::retry,
        modifier = modifier,
        onShowEntrance = onShowEntrance,
        onReportRemoval = onReportRemoval,
    )
}

/**
 * V2 dla gotowego stanu [card] i [isOnline]; „Spróbuj ponownie” to [onRetry], „Pokaż na planie” to [onShowPlan]
 * z identyfikatorem piętra, „Oznacz obejrzane piętra” pod grupą „Piętra” to [onMarkFloors] (`null` ukrywa przycisk),
 * dotknięcie wystającego nagłówka „Wejścia” niskiej karty to [onExpand], reszta jak w [BuildingCardScreen].
 */
@Composable
fun BuildingCardContent(
    symbol: String,
    mode: CardMode,
    card: BuildingCardUiState,
    isOnline: Boolean,
    onShowOnMap: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onReport: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onShowEntrance: (String) -> Unit = {},
    onReportRemoval: ((String) -> Unit)? = null,
    onShowPlan: (String) -> Unit = {},
    onMarkFloors: (() -> Unit)? = null,
    onExpand: () -> Unit = {},
) {
    val isFullScreen = mode != CardMode.Compact
    val isHeaderFixed = mode == CardMode.Expanded
    val top = @Composable { CardTop(symbol = symbol, card = card, mode = mode, isOnline = isOnline, onClose = onClose) }
    Column(if (isFullScreen) modifier.fillMaxSize() else modifier) {
        if (mode == CardMode.Direct) BackHeader(title = symbol, onBack = onBack)
        if (isHeaderFixed) Box(Modifier.fixedTopPadding()) { top() }
        Column(
            modifier =
                Modifier
                    .weight(1f, isFullScreen)
                    .verticalScroll(rememberScrollState())
                    .cardPadding(isPeek = card.hasEntrancesPeek(isFullScreen), hasTop = !isHeaderFixed),
            verticalArrangement = Arrangement.spacedBy(Odstepy.s),
        ) {
            if (!isHeaderFixed) top()
            if (card is BuildingCardUiState.Ready || card.isWaiting(isFullScreen)) {
                CardActions(
                    mode = mode,
                    recommended = card.recommendedEntrance(),
                    onShowEntrance = onShowEntrance,
                    onShowOnMap = onShowOnMap,
                )
            }
            FactsOrMissing(
                card = card,
                isFullScreen = isFullScreen,
                onReport = onReport,
                onShowEntrance = onShowEntrance,
                onReportRemoval = onReportRemoval,
                onRetry = onRetry,
                onShowPlan = onShowPlan,
                onMarkFloors = onMarkFloors,
                onExpand = onExpand,
            )
        }
    }
}

@Composable
private fun CardTop(
    symbol: String,
    card: BuildingCardUiState,
    mode: CardMode,
    isOnline: Boolean,
    onClose: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        if (!isOnline) OfflinePill()
        CardHeader(symbol = symbol, card = card, mode = mode, onClose = onClose)
    }
}

private fun Modifier.fixedTopPadding(): Modifier =
    padding(start = Odstepy.m, top = Odstepy.m, end = Odstepy.m, bottom = Odstepy.s)

@Composable
private fun FactsOrMissing(
    card: BuildingCardUiState,
    isFullScreen: Boolean,
    onReport: () -> Unit,
    onShowEntrance: (String) -> Unit,
    onReportRemoval: ((String) -> Unit)?,
    onRetry: () -> Unit,
    onShowPlan: (String) -> Unit,
    onMarkFloors: (() -> Unit)?,
    onExpand: () -> Unit,
) {
    if (card.hasEntrancesPeek(isFullScreen)) {
        Column {
            DescriptionLine()
            EntrancesPeek(card = card, onExpand = onExpand)
        }
    }
    if (card is BuildingCardUiState.Ready && isFullScreen) {
        DescriptionLine()
        BuildingFacts(
            card = card,
            onReport = onReport,
            onShowEntrance = onShowEntrance,
            onReportRemoval = onReportRemoval,
            onShowPlan = onShowPlan,
            onMarkFloors = onMarkFloors,
        )
    }
    if (card == BuildingCardUiState.Missing) MissingBuilding(onRetry = onRetry)
}

@Composable
private fun DescriptionLine() =
    HorizontalDivider(Modifier.testTag(DESCRIPTION_LINE_TAG), color = MaterialTheme.colorScheme.outlineVariant)

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
    onReport: () -> Unit,
    onShowEntrance: (String) -> Unit,
    onReportRemoval: ((String) -> Unit)?,
    onShowPlan: (String) -> Unit,
    onMarkFloors: (() -> Unit)?,
) {
    val common = commonAssessment(card.entrances)
    common?.let { AssessmentLine(it) }
    EntrancesGroup(card = card, withAssessment = common == null, onShowEntrance = onShowEntrance, onReport = onReport)
    EvacuationExits(card.evacuationExits)
    BarriersGroup(card, onReportRemoval)
    card.floors?.let { LiftToiletGroup(it) }
    FloorsGroup(facts = card.floors, isBasic = card.isBasic, onShowPlan = onShowPlan)
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

/** Tryb treści V2 Karta budynku. Wymagania: US2.8, US2.9 */
enum class CardMode {
    /** Mała karta nad mapą: nagłówek z „Zamknij”, wiersz o wejściach z odpowiedzią i „Pokaż wejście na mapie”. */
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
