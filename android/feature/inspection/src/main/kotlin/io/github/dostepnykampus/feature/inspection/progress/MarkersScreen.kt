/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.model.inspection.InspectionCoverage
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import io.github.dostepnykampus.core.model.inspection.ProgressMarker
import io.github.dostepnykampus.feature.inspection.R

/**
 * C5 „Obejrzane piętra” w wariancie Koordynatora budynku [symbol].
 *
 * Wymagania: US11.3, BR-20
 *
 * @param symbol symbol budynku z trasy C5
 * @param onBack „Wstecz” do K1
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel stan znaczników; w aplikacji z grafu Hilt
 */
@Composable
fun MarkersScreen(
    symbol: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MarkersViewModel =
        hiltViewModel<MarkersViewModel, MarkersViewModel.Factory>(
            key = symbol,
            creationCallback = { factory -> factory.create(symbol) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions =
        MarkerActions(onAsk = viewModel::ask, onKeep = viewModel::keep, onConfirm = viewModel::confirm)
    MarkersContent(state = state, actions = actions, onBack = onBack, modifier = modifier)
}

/**
 * Czynności przy znaczniku w C5 Koordynatora.
 *
 * Wymagania: US11.3
 *
 * @property onAsk „Cofnij znacznik”: identyfikator znacznika
 * @property onKeep „Zostaw znacznik”
 * @property onConfirm „Cofnij znacznik” w potwierdzeniu
 */
data class MarkerActions(val onAsk: (markerId: String) -> Unit, val onKeep: () -> Unit, val onConfirm: () -> Unit)

/**
 * Widok C5 Koordynatora bez modelu widoku: znaczniki z autorami, potwierdzenie cofnięcia i stany wspólne.
 *
 * Wymagania: US11.3, BR-20
 * TODO(#2455): stany Bez internetu i Błąd serwera.
 * TODO(#2453): etykieta piętra z danych PWr zamiast `floorId`.
 *
 * @param state stan ekranu
 * @param actions czynności przy znaczniku
 * @param onBack „Wstecz” do K1
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun MarkersContent(state: MarkersUiState, actions: MarkerActions, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        BackHeader(title = stringResource(R.string.znaczniki_naglowek), onBack = onBack)
        when (state) {
            MarkersUiState.Loading -> Loading()
            MarkersUiState.Offline, MarkersUiState.ServerError -> Unit
            is MarkersUiState.Ready -> MarkerList(state, actions)
        }
    }
}

@Composable
private fun MarkerList(state: MarkersUiState.Ready, actions: MarkerActions) {
    LazyColumn(contentPadding = PaddingValues(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.m)) {
        item {
            Text(state.building.buildingSymbol, style = MaterialTheme.typography.titleMedium)
            Text(coverageLine(state.building.coverage))
        }
        items(state.building.markers, key = { it.marker.id }) { marker ->
            MarkerCard(marker, isAsking = state.asking == marker.marker.id, actions = actions)
        }
    }
}

@Composable
private fun MarkerCard(marker: ProgressMarker, isAsking: Boolean, actions: MarkerActions) {
    OutlinedCard {
        Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            Text(placeText(marker.marker.place), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.znaczniki_autor, marker.authorPseudonym))
            if (isAsking) {
                Text(questionText(marker.marker.place))
                Button(onClick = actions.onConfirm) { Text(stringResource(R.string.znaczniki_cofnij)) }
                OutlinedButton(onClick = actions.onKeep) { Text(stringResource(R.string.znaczniki_zostaw)) }
            } else {
                OutlinedButton(onClick = { actions.onAsk(marker.marker.id) }) {
                    Text(stringResource(R.string.znaczniki_cofnij))
                }
            }
        }
    }
}

@Composable
@ReadOnlyComposable
private fun placeText(place: MarkerPlace): String =
    when (place) {
        is MarkerPlace.Floor -> stringResource(R.string.znaczniki_pietro, place.floorId)
        MarkerPlace.WholeBuilding -> stringResource(R.string.znaczniki_caly_budynek)
    }

@Composable
@ReadOnlyComposable
private fun questionText(place: MarkerPlace): String =
    when (place) {
        is MarkerPlace.Floor -> stringResource(R.string.znaczniki_pytanie_pietra, place.floorId)
        MarkerPlace.WholeBuilding -> stringResource(R.string.znaczniki_pytanie_budynku)
    }

@Composable
@ReadOnlyComposable
private fun coverageLine(coverage: InspectionCoverage): String =
    when (coverage) {
        is InspectionCoverage.Floors -> {
            stringResource(R.string.znaczniki_postep, coverage.floorsInspected, coverage.floorsTotal)
        }

        is InspectionCoverage.Whole -> {
            stringResource(if (coverage.isInspected) R.string.znaczniki_obejrzany else R.string.znaczniki_nieobejrzany)
        }
    }
