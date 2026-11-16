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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

private val InspectedIcon: ImageVector =
    pathIcon(
        "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14z" +
            "M17.99 9l-1.41-1.42-6.59 6.59-2.58-2.57-1.42 1.41 4 3.99z",
    )

/**
 * C5 „Obejrzane piętra” w wariancie Zbierającego budynku [symbol].
 *
 * Wymagania: US15.1, US15.3, BR-20, NFR-7
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
    CollectorMarkersContent(
        symbol = symbol,
        state = state,
        onMark = viewModel::mark,
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * Widok C5 Zbierającego bez modelu widoku: piętra w osobnych kontenerach ze stanem i „Oznacz jako obejrzane”.
 *
 * Wymagania: US15.1, US15.3, PW-11, NFR-7
 *
 * @param symbol symbol budynku
 * @param state stan ekranu
 * @param onMark „Oznacz jako obejrzane” i „Spróbuj ponownie” po błędzie zapisu: miejsce znacznika
 * @param onBack „Wstecz” do karty budynku
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun CollectorMarkersContent(
    symbol: String,
    state: CollectorMarkersUiState,
    onMark: (MarkerPlace) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.znaczniki_naglowek), onBack = onBack)
            when (state) {
                CollectorMarkersUiState.Loading -> Loading()
                is CollectorMarkersUiState.Ready -> FloorRows(symbol, state, onMark)
            }
        }
    }
}

@Composable
private fun FloorRows(symbol: String, state: CollectorMarkersUiState.Ready, onMark: (MarkerPlace) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.m)) {
        item {
            Text(stringResource(R.string.zbierajacy_budynek, symbol), style = MaterialTheme.typography.titleMedium)
        }
        items(state.rows, key = { it.place.toString() }) { row ->
            FloorCard(row = row, isUnsaved = state.unsaved == row.place, onMark = { onMark(row.place) })
        }
    }
}

@Composable
private fun FloorCard(row: FloorRow, isUnsaved: Boolean, onMark: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Zaokraglenia.duze),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            val name = row.label ?: stringResource(R.string.znaczniki_caly_budynek)
            Text(name, style = MaterialTheme.typography.titleMedium)
            if (row.markerId != null) {
                Inspected(isPending = row.isPending)
            } else {
                Text(stringResource(R.string.zbierajacy_nieobejrzane), style = MaterialTheme.typography.titleMedium)
                if (isUnsaved) Text(stringResource(R.string.zbierajacy_blad_zapisu))
                val action = if (isUnsaved) R.string.postep_sprobuj_ponownie else R.string.zbierajacy_oznacz
                FilledTonalButton(
                    onClick = onMark,
                    modifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.minimum),
                ) {
                    Text(stringResource(action))
                }
            }
        }
    }
}

@Composable
private fun Inspected(isPending: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.s), verticalAlignment = Alignment.CenterVertically) {
            Icon(InspectedIcon, contentDescription = null)
            Text(stringResource(R.string.zbierajacy_obejrzane), style = MaterialTheme.typography.titleMedium)
        }
        Text(stringResource(R.string.zbierajacy_twoj_znacznik))
        if (isPending) Text(stringResource(R.string.zbierajacy_czeka), fontWeight = FontWeight.Bold)
    }
}
