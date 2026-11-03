/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.model.EntranceMarking

/**
 * V2 Karta budynku (US2, BR-25): symbol, nazwa, wiersz o wejściach (US2.8), wejścia z oznaczeniem tekstem
 * i ikoną, zalecane pierwsze.
 * Karta nic nie mówi o barierach, bo aplikacja nie ma jeszcze ich źródła (US2.7). Czynności stoją w przewijanej
 * treści pod wierszem o wejściach, bez przypiętego pasa (reguła 10): „Sprawdź, którędy wejść” w każdym trybie,
 * „Zamknij” w [CardMode.Compact], „Pokaż budynek na mapie” w [CardMode.Direct]. Górny pasek „Wstecz” ma tylko
 * pełny ekran, a grupę „Wejścia” tylko pełny ekran.
 * Nieznany symbol: „budynku nie ma w telefonie” (US2.7). Bez internetu nad treścią stoi [OfflineLine] jak na V1,
 * bez daty (NFR-10, US2.8).
 *
 * @param symbol symbol z trasy, widoczny w nagłówku także w trakcie wczytywania
 * @param mode mała karta nad mapą, pełny ekran rozwinięty z niej albo pełny ekran otwarty od razu
 * @param onCheckEntrances „Sprawdź, którędy wejść” otwiera V3
 * @param onShowOnMap „Pokaż budynek na mapie” zaznacza budynek na V1
 * @param onBack „Wstecz”
 * @param onClose „Zamknij” małej karty
 * @param modifier wymiary nadane przez arkusz
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
    viewModel: BuildingCardViewModel =
        hiltViewModel<BuildingCardViewModel, BuildingCardViewModel.Factory>(
            creationCallback = { factory -> factory.create(symbol) },
        ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val card = state
    val isOnline by viewModel.online.collectAsStateWithLifecycle()
    val isFullScreen = mode != CardMode.Compact
    Column(modifier.fillMaxSize()) {
        if (isFullScreen) BackHeader(title = symbol, onBack = onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m),
            verticalArrangement = Arrangement.spacedBy(Odstepy.s),
        ) {
            OfflineWhen(isOnline = isOnline)
            if (card is BuildingCardUiState.Ready) {
                BuildingName(symbol = symbol, name = card.name)
                EntranceSummaryRow(entranceSummary(card.entrances))
                CardActions(
                    mode = mode,
                    onCheckEntrances = onCheckEntrances,
                    onShowOnMap = onShowOnMap,
                    onClose = onClose,
                )
                if (isFullScreen) BuildingFacts(card)
            }
            if (card == BuildingCardUiState.Missing) MissingBuilding(onRetry = viewModel::retry)
        }
    }
}

@Composable
private fun OfflineWhen(isOnline: Boolean) {
    if (!isOnline) OfflineLine()
}

@Composable
private fun BuildingName(symbol: String, name: String?) {
    val visible = name ?: stringResource(R.string.mapa_budynek, symbol)
    val spoken = if (name == null) visible else stringResource(R.string.karta_czytaj, symbol, name)
    Text(
        text = visible,
        modifier = Modifier.semantics { contentDescription = spoken },
        style = MaterialTheme.typography.titleLarge,
    )
}

/**
 * Wiersz o wejściach pod nagłówkiem (US2.8, NFR-21): słowo i ikona oznaczenia jak w [MarkingChip] (kształt, nie
 * para zielony/czerwony).
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
private fun BuildingFacts(card: BuildingCardUiState.Ready) {
    GroupContainer(title = stringResource(R.string.karta_wejscia)) {
        card.entrances.forEach { EntranceLine(it) }
    }
}

@Composable
private fun EntranceLine(listed: OrderedEntrance) {
    val title = listed.entrance.name ?: stringResource(R.string.karta_bez_nazwy)
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.xs)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
            verticalArrangement = Arrangement.spacedBy(Odstepy.xs),
        ) {
            MarkingChip(marking = listed.entrance.marking)
            if (listed.isRecommended) {
                Text(
                    text = stringResource(R.string.karta_zalecane),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = Typografia.wyroznienieWaga,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Text(text = title, fontWeight = Typografia.wyroznienieWaga, style = MaterialTheme.typography.titleMedium)
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
        PinnedButton(
            text = stringResource(R.string.karta_sprawdz),
            onClick = onCheckEntrances,
            modifier = Modifier.fillMaxWidth(),
        )
        when (mode) {
            CardMode.Compact -> OutlinedAction(text = stringResource(R.string.mapa_zamknij), onClick = onClose)
            CardMode.Expanded -> Unit
            CardMode.Direct -> OutlinedAction(text = stringResource(R.string.karta_pokaz), onClick = onShowOnMap)
        }
    }
}

@Composable
private fun OutlinedAction(text: String, onClick: () -> Unit) {
    PinnedButton(text = text, onClick = onClick, modifier = Modifier.fillMaxWidth(), kind = PinnedButtonKind.Outlined)
}
