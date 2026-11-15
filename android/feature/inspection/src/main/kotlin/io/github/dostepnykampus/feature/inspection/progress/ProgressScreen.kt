/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import androidx.annotation.StringRes
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.feature.inspection.R

/**
 * K1 „Postęp inwentaryzacji” ze stanem z modelu widoku.
 *
 * Wymagania: US11.1, US11.2
 *
 * @param actions czynności ekranu
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel stan listy; w aplikacji z grafu Hilt
 */
@Composable
fun ProgressScreen(
    actions: ProgressActions,
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProgressContent(state = state, actions = actions, onRefresh = viewModel::refresh, modifier = modifier)
}

/**
 * Widok K1 bez modelu widoku: lista budynków, wczytywanie, bez internetu, błąd serwera i brak uprawnień.
 *
 * Wymagania: US11.1, US11.2, NFR-50
 *
 * @param state stan ekranu
 * @param actions czynności ekranu
 * @param onRefresh „Odśwież” i „Spróbuj ponownie”
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun ProgressContent(
    state: ProgressUiState,
    actions: ProgressActions,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        BackHeader(title = stringResource(R.string.postep_naglowek), onBack = actions.onBack)
        when (state) {
            ProgressUiState.Loading -> Loading()
            is ProgressUiState.Ready -> BuildingList(state = state, onOpen = actions.onOpen, onRefresh = onRefresh)
            ProgressUiState.Offline -> Failure(Problem.OFFLINE, onRefresh)
            ProgressUiState.ServerError -> Failure(Problem.SERVER, onRefresh)
            ProgressUiState.Forbidden -> Failure(Problem.FORBIDDEN, actions.onBack)
        }
    }
}

@Composable
internal fun Loading() {
    Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        CircularProgressIndicator()
        Text(stringResource(R.string.postep_wczytujemy), style = MaterialTheme.typography.bodyLarge)
    }
}

private enum class Problem(
    @param:StringRes val message: Int,
    @param:StringRes val action: Int,
    @param:StringRes val headline: Int? = null,
) {
    OFFLINE(R.string.postep_wymaga_internetu, R.string.postep_sprobuj_ponownie, R.string.postep_bez_internetu),
    SERVER(R.string.postep_blad_serwera, R.string.postep_sprobuj_ponownie),
    FORBIDDEN(R.string.postep_brak_uprawnien, R.string.postep_wroc_do_mapy),
}

@Composable
private fun Failure(problem: Problem, onAction: () -> Unit) {
    Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        problem.headline?.let { Text(stringResource(it), style = MaterialTheme.typography.titleMedium) }
        Text(stringResource(problem.message), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onAction) { Text(stringResource(problem.action)) }
    }
}

@Composable
private fun BuildingList(state: ProgressUiState.Ready, onOpen: (String) -> Unit, onRefresh: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.m)) {
        item { ReadLine(readAt = state.readAt, isRefreshing = state.isRefreshing, onRefresh = onRefresh) }
        items(state.buildings, key = { it.buildingSymbol }) { building ->
            BuildingCard(building = building, onOpen = { onOpen(building.buildingSymbol) })
        }
    }
}
