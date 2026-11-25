/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.pathIcon
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import io.github.dostepnykampus.feature.inspection.R
import io.github.dostepnykampus.core.designsystem.R as DesignR

private val InspectedIcon: ImageVector =
    pathIcon(
        "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14z " +
            "M17.99 9l-1.41-1.42-6.59 6.59-2.58-2.57-1.42 1.41 4 3.99z",
    )

/**
 * Czynności przy wierszu piętra w C5 Zbierającego.
 *
 * Wymagania: US15.1, US15.2
 *
 * @property onMark „Oznacz jako obejrzane” i „Spróbuj ponownie” po błędzie zapisu: miejsce znacznika
 * @property onAsk „Cofnij znacznik”: identyfikator własnego znacznika
 * @property onKeep „Zostaw znacznik”
 * @property onConfirm „Cofnij znacznik” w potwierdzeniu
 */
data class CollectorMarkerActions(
    val onMark: (MarkerPlace) -> Unit = {},
    val onAsk: (markerId: String) -> Unit = {},
    val onKeep: () -> Unit = {},
    val onConfirm: () -> Unit = {},
)

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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Zaokraglenia.duze),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            val name = row.label ?: stringResource(R.string.znaczniki_caly_budynek)
            Text(name, style = MaterialTheme.typography.titleMedium)
            if (row.markerId != null) {
                Inspected(row = row, isAsking = isAsking, isUnsaved = isUnsaved, actions = actions)
            } else {
                Text(stringResource(R.string.zbierajacy_nieobejrzane), style = MaterialTheme.typography.titleMedium)
                if (isUnsaved) Text(stringResource(R.string.zbierajacy_blad_zapisu))
                val action = if (isUnsaved) DesignR.string.sprobuj_ponownie else R.string.zbierajacy_oznacz
                FilledTonalButton(
                    onClick = { actions.onMark(row.place) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.minimum),
                ) {
                    Text(stringResource(action))
                }
            }
        }
    }
}

@Composable
private fun Inspected(row: FloorRow, isAsking: Boolean, isUnsaved: Boolean, actions: CollectorMarkerActions) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.s), verticalAlignment = Alignment.CenterVertically) {
            Icon(InspectedIcon, contentDescription = null)
            Text(stringResource(R.string.zbierajacy_obejrzane), style = MaterialTheme.typography.titleMedium)
        }
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
            OutlinedButton(
                onClick = actions.onKeep,
                modifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.minimum),
            ) {
                Text(stringResource(R.string.znaczniki_zostaw))
            }
        } else {
            val markerId = row.markerId ?: return
            OutlinedButton(
                onClick = { actions.onAsk(markerId) },
                modifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.minimum),
            ) {
                Text(stringResource(R.string.znaczniki_cofnij))
            }
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
