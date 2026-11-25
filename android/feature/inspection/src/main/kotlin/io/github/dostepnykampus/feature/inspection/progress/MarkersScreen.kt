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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.StaffProblem
import io.github.dostepnykampus.core.designsystem.StaffProblemMessage
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.model.inspection.InspectionCoverage
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import io.github.dostepnykampus.core.model.inspection.ProgressMarker
import io.github.dostepnykampus.feature.inspection.R
import io.github.dostepnykampus.core.designsystem.R as DesignR

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
        MarkerActions(
            onAsk = viewModel::ask,
            onKeep = viewModel::keep,
            onConfirm = viewModel::confirm,
            onRetry = viewModel::retry,
        )
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
 * @property onRetry „Spróbuj ponownie” po błędzie odczytu
 */
data class MarkerActions(
    val onAsk: (markerId: String) -> Unit,
    val onKeep: () -> Unit,
    val onConfirm: () -> Unit,
    val onRetry: () -> Unit,
)

/**
 * Widok C5 Koordynatora bez modelu widoku: znaczniki z autorami, potwierdzenie cofnięcia i stany wspólne.
 *
 * Wymagania: US11.3, BR-20, NFR-50
 *
 * @param state stan ekranu
 * @param actions czynności przy znaczniku
 * @param onBack „Wstecz” do K1
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun MarkersContent(state: MarkersUiState, actions: MarkerActions, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.znaczniki_naglowek), onBack = onBack)
            when (state) {
                MarkersUiState.Loading -> Loading()
                MarkersUiState.Offline -> StaffProblemMessage(StaffProblem.OFFLINE, actions.onRetry)
                MarkersUiState.ServerError -> StaffProblemMessage(StaffProblem.SERVER, actions.onRetry)
                is MarkersUiState.Ready -> MarkerList(state, actions)
            }
        }
    }
}

@Composable
private fun MarkerList(state: MarkersUiState.Ready, actions: MarkerActions) {
    LazyColumn(contentPadding = PaddingValues(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.m)) {
        item {
            Text(state.building.buildingSymbol, style = MaterialTheme.typography.titleMedium)
            Text(coverageLine(state.building.coverage))
            state.failure?.let { Text(stringResource(it.message)) }
        }
        items(state.building.markers, key = { it.marker.id }) { marker ->
            MarkerCard(
                marker = marker,
                floorLabel = marker.floorLabel(state.floorLabels),
                isAsking = state.asking == marker.marker.id,
                actions = actions,
            )
        }
    }
}

@Composable
private fun MarkerCard(marker: ProgressMarker, floorLabel: String?, isAsking: Boolean, actions: MarkerActions) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            Text(placeText(marker.marker.place, floorLabel), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.znaczniki_autor, marker.authorPseudonym))
            if (isAsking) {
                Text(questionText(marker.marker.place, floorLabel))
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
private fun placeText(place: MarkerPlace, floorLabel: String?): String =
    when (place) {
        is MarkerPlace.Floor -> floorLabel ?: stringResource(R.string.znaczniki_pietro, place.floorId)
        MarkerPlace.WholeBuilding -> stringResource(R.string.znaczniki_caly_budynek)
    }

@Composable
@ReadOnlyComposable
private fun questionText(place: MarkerPlace, floorLabel: String?): String =
    when (place) {
        is MarkerPlace.Floor -> floorQuestion(place, floorLabel)
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

@Composable
@ReadOnlyComposable
private fun floorQuestion(place: MarkerPlace.Floor, floorLabel: String?): String =
    if (floorLabel == null) {
        stringResource(R.string.znaczniki_pytanie_pietra, place.floorId)
    } else {
        stringResource(R.string.znaczniki_pytanie_pietra_z_etykieta, floorLabel)
    }

private fun ProgressMarker.floorLabel(floorLabels: Map<String, String>): String? =
    (marker.place as? MarkerPlace.Floor)?.let { floorLabels[it.floorId] }

private val RevokeFailure.message: Int
    get() = if (this == RevokeFailure.OFFLINE) DesignR.string.praca_wymaga_internetu else DesignR.string.praca_blad_serwera
