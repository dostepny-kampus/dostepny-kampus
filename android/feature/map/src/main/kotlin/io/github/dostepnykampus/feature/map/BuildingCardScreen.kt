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
import androidx.compose.ui.platform.LocalDensity
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
import io.github.dostepnykampus.core.designsystem.PinnedActionBar
import io.github.dostepnykampus.core.designsystem.PinnedButton
import io.github.dostepnykampus.core.designsystem.PinnedButtonKind
import io.github.dostepnykampus.core.designsystem.Typografia

private const val LARGE_FONT_SCALE = 1.5f

/**
 * V2 Karta budynku (US2, BR-25): symbol, nazwa, wejścia z oznaczeniem tekstem i ikoną, zalecane pierwsze.
 * Karta nic nie mówi o barierach, bo aplikacja nie ma jeszcze ich źródła (US2.7). Czynności: „Sprawdź,
 * którędy wejść” i „Pokaż na mapie”. Przy skali czcionki co najmniej 1,5 czynności stoją w treści (reguła 10).
 * Nieznany symbol: „budynku nie ma w telefonie” (US2.7). Bez internetu nad treścią stoi [OfflineLine] jak na V1,
 * bez daty (NFR-10, US2.8).
 *
 * @param symbol symbol z trasy, widoczny w nagłówku także w trakcie wczytywania
 * @param onCheckEntrances „Sprawdź, którędy wejść” otwiera V3
 * @param onShowOnMap „Pokaż na mapie” zaznacza budynek na V1
 * @param onBack „Wstecz”
 * @param modifier wymiary nadane przez arkusz
 * @param viewModel stan karty; w aplikacji z grafu Hilt, w testach z podróbką katalogu
 */
@Composable
fun BuildingCardScreen(
    symbol: String,
    onCheckEntrances: () -> Unit,
    onShowOnMap: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BuildingCardViewModel =
        hiltViewModel<BuildingCardViewModel, BuildingCardViewModel.Factory>(
            creationCallback = { factory -> factory.create(symbol) },
        ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val card = state
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isLargeFont = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    val actions = @Composable { inContent: Boolean ->
        CardActions(onCheckEntrances = onCheckEntrances, onShowOnMap = onShowOnMap, inContent = inContent)
    }
    Column(modifier.fillMaxSize()) {
        BackHeader(title = symbol, onBack = onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m),
            verticalArrangement = Arrangement.spacedBy(Odstepy.s),
        ) {
            OfflineWhen(isOnline = isOnline)
            if (card is BuildingCardUiState.Ready) BuildingName(symbol = symbol, name = card.name)
            if (isLargeFont && card is BuildingCardUiState.Ready) actions(true)
            when (card) {
                BuildingCardUiState.Loading -> Unit
                BuildingCardUiState.Missing -> MissingBuilding(onRetry = viewModel::retry)
                is BuildingCardUiState.Ready -> BuildingFacts(card)
            }
        }
        if (!isLargeFont && card is BuildingCardUiState.Ready) actions(false)
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
private fun CardActions(onCheckEntrances: () -> Unit, onShowOnMap: () -> Unit, inContent: Boolean) {
    val check = stringResource(R.string.karta_sprawdz)
    val show = stringResource(R.string.karta_pokaz)
    val buttons: @Composable (Modifier) -> Unit = { width ->
        PinnedButton(text = check, onClick = onCheckEntrances, modifier = width)
        PinnedButton(text = show, onClick = onShowOnMap, modifier = width, kind = PinnedButtonKind.Tonal)
    }
    if (inContent) {
        Column(verticalArrangement = Arrangement.spacedBy(CeleDotyku.odstep)) { buttons(Modifier.fillMaxWidth()) }
    } else {
        PinnedActionBar { buttons(Modifier.fillMaxWidth()) }
    }
}
