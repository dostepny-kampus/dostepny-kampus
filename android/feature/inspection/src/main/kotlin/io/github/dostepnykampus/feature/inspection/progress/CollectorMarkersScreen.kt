/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.card.CardAction
import io.github.dostepnykampus.core.designsystem.card.ListItemCard
import io.github.dostepnykampus.core.designsystem.card.OutlinedAction
import io.github.dostepnykampus.feature.inspection.R
import io.github.dostepnykampus.core.designsystem.R as DesignR

/**
 * C5 „Obejrzane piętra” w wariancie Zbierającego budynku [symbol].
 *
 * Wymagania: US15.1, US15.2, US15.3, BR-20, NFR-7
 *
 * @param symbol symbol budynku z trasy C5
 * @param onBack „Wstecz” do karty budynku
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel stan pięter; w aplikacji z grafu Hilt
 */
@Composable
fun CollectorMarkersScreen(
    symbol: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectorMarkersViewModel =
        hiltViewModel<CollectorMarkersViewModel, CollectorMarkersViewModel.Factory>(
            key = symbol,
            creationCallback = { factory -> factory.create(symbol) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions =
        CollectorMarkerActions(
            onMark = viewModel::mark,
            onAsk = viewModel::askRevoke,
            onKeep = viewModel::keepMarker,
            onConfirm = viewModel::confirmRevoke,
        )
    CollectorMarkersContent(
        symbol = symbol,
        state = state,
        actions = actions,
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * Widok C5 Zbierającego bez modelu widoku: piętra w osobnych kontenerach ze stanem, „Oznacz jako obejrzane”
 * i „Cofnij znacznik” z potwierdzeniem.
 *
 * Wymagania: US15.1, US15.2, US15.3, BR-20, NFR-7
 *
 * @param symbol symbol budynku
 * @param state stan ekranu
 * @param actions czynności przy wierszu piętra
 * @param onBack „Wstecz” do karty budynku
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun CollectorMarkersContent(
    symbol: String,
    state: CollectorMarkersUiState,
    actions: CollectorMarkerActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.znaczniki_naglowek), onBack = onBack)
            when (state) {
                CollectorMarkersUiState.Loading -> Loading()
                is CollectorMarkersUiState.Ready -> FloorRows(symbol, state, actions)
            }
        }
    }
}

@Composable
private fun FloorRows(symbol: String, state: CollectorMarkersUiState.Ready, actions: CollectorMarkerActions) {
    LazyColumn(contentPadding = PaddingValues(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.m)) {
        item {
            Text(stringResource(R.string.zbierajacy_budynek, symbol), style = MaterialTheme.typography.titleMedium)
        }
        items(state.rows, key = { it.place.toString() }) { row ->
            FloorCard(
                row = row,
                isAsking = row.markerId != null && state.asking == row.markerId,
                isUnsaved = state.unsaved == row.place,
                actions = actions,
            )
        }
    }
}

@Composable
private fun FloorCard(row: FloorRow, isAsking: Boolean, isUnsaved: Boolean, actions: CollectorMarkerActions) {
    ListItemCard {
        val name = row.label ?: stringResource(R.string.znaczniki_caly_budynek)
        Text(name, style = MaterialTheme.typography.titleMedium)
        if (row.markerId != null) {
            Inspected(row = row, isAsking = isAsking, isUnsaved = isUnsaved, actions = actions)
        } else {
            Text(stringResource(R.string.zbierajacy_nieobejrzane), style = MaterialTheme.typography.titleMedium)
            if (isUnsaved) Text(stringResource(R.string.zbierajacy_blad_zapisu))
            val action = if (isUnsaved) DesignR.string.sprobuj_ponownie else R.string.zbierajacy_oznacz
            CardAction(text = stringResource(action), onClick = { actions.onMark(row.place) })
        }
    }
}

@Composable
private fun Inspected(row: FloorRow, isAsking: Boolean, isUnsaved: Boolean, actions: CollectorMarkerActions) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        InspectedLabel(stringResource(R.string.zbierajacy_obejrzane), style = MaterialTheme.typography.titleMedium)
        val authorText =
            if (row.isMine) R.string.zbierajacy_twoj_znacznik else R.string.zbierajacy_znacznik_innej_osoby
        Text(stringResource(authorText))
        if (!row.isMine) return
        if (row.isPending) Text(stringResource(R.string.zbierajacy_czeka), fontWeight = FontWeight.Bold)
        if (isUnsaved) Text(stringResource(R.string.zbierajacy_blad_zapisu))
        if (isAsking) {
            HorizontalDivider()
            Text(collectorQuestionText(row.label), fontWeight = FontWeight.Bold)
            Button(
                onClick = actions.onConfirm,
                modifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.minimum),
            ) {
                Text(stringResource(R.string.znaczniki_cofnij))
            }
            OutlinedAction(onClick = actions.onKeep, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.znaczniki_zostaw))
            }
        } else {
            val markerId = row.markerId ?: return
            CardAction(text = stringResource(R.string.znaczniki_cofnij), onClick = { actions.onAsk(markerId) })
        }
    }
}

@Composable
@ReadOnlyComposable
private fun collectorQuestionText(label: String?): String =
    if (label != null) {
        stringResource(R.string.zbierajacy_pytanie_pietra, label)
    } else {
        stringResource(R.string.zbierajacy_pytanie_budynku)
    }
