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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.StaffProblem
import io.github.dostepnykampus.core.designsystem.StaffProblemMessage
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.feature.inspection.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val READ_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy, HH:mm", Locale.ROOT)

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
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.postep_naglowek), onBack = actions.onBack)
            when (state) {
                ProgressUiState.Loading -> Loading()
                is ProgressUiState.Ready -> BuildingList(state = state, onOpen = actions.onOpen, onRefresh = onRefresh)
                ProgressUiState.Offline -> StaffProblemMessage(StaffProblem.OFFLINE, onRefresh)
                ProgressUiState.ServerError -> StaffProblemMessage(StaffProblem.SERVER, onRefresh)
                ProgressUiState.Forbidden -> StaffProblemMessage(StaffProblem.FORBIDDEN, actions.onBack)
            }
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

@Composable
private fun BuildingList(state: ProgressUiState.Ready, onOpen: (String) -> Unit, onRefresh: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.m)) {
        item { ReadLine(readAt = state.readAt, isRefreshing = state.isRefreshing, onRefresh = onRefresh) }
        items(state.buildings, key = { it.buildingSymbol }) { building ->
            BuildingCard(
                building = building,
                name = state.names[building.buildingSymbol],
                onOpen = { onOpen(building.buildingSymbol) },
            )
        }
    }
}

/**
 * Chwila odczytu postępu i „Odśwież”; postęp nie odświeża się sam.
 *
 * Wymagania: US11.2
 *
 * @param readAt chwila odczytu
 * @param isRefreshing czy trwa odświeżanie; wyłącza przycisk
 * @param onRefresh „Odśwież”
 */
@Composable
internal fun ReadLine(readAt: Instant, isRefreshing: Boolean, onRefresh: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(stringResource(R.string.postep_stan_z, READ_FORMAT.format(readAt.atZone(ZoneId.systemDefault()))))
        OutlinedButton(onClick = onRefresh, enabled = !isRefreshing) { Text(stringResource(R.string.postep_odswiez)) }
    }
}
