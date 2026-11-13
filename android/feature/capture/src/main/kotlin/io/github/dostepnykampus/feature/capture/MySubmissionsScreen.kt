/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.data.submissions.SendState
import io.github.dostepnykampus.core.designsystem.BackHeader
import io.github.dostepnykampus.core.designsystem.Odstepy

/**
 * C3 „Moje zgłoszenia” z listą z telefonu (US8.1, NFR-5).
 *
 * @param actions czynności ekranu
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel lista; w aplikacji z grafu Hilt
 */
@Composable
fun MySubmissionsScreen(
    actions: MySubmissionsActions,
    modifier: Modifier = Modifier,
    viewModel: MySubmissionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MySubmissionsContent(state = state, actions = actions, modifier = modifier, onRetry = viewModel::retry)
}

/**
 * Widok C3 bez modelu widoku: lista od najnowszego albo stan pusty (US8.1, PW-12), nad listą sprawdzanie
 * statusów z serwera (US8.2).
 *
 * @param state stan ekranu
 * @param actions czynności ekranu
 * @param modifier wymiary nadane przez aplikację
 * @param onRetry „Spróbuj ponownie” po błędzie sprawdzania statusów (NFR-50)
 */
@Composable
fun MySubmissionsContent(
    state: MySubmissionsUiState,
    actions: MySubmissionsActions,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        BackHeader(title = stringResource(R.string.moje_naglowek), onBack = actions.onBack)
        if (state is MySubmissionsUiState.Ready) {
            SubmissionList(state = state, actions = actions, onRetry = onRetry)
        }
    }
}

@Composable
private fun SubmissionList(state: MySubmissionsUiState.Ready, actions: MySubmissionsActions, onRetry: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(Odstepy.m),
        verticalArrangement = Arrangement.spacedBy(Odstepy.m),
    ) {
        item {
            ListTop(list = state.list, onMeasure = actions.onMeasure)
        }
        item {
            StatusCheckLine(check = state.check, isOnline = state.isOnline, onRetry = onRetry)
        }
        items(state.list.items, key = { it.id }) { item ->
            val open = actions.onOpen.takeIf { item.state != SendState.SENT }
            SubmissionCard(item = item, onOpen = open?.let { { it(item.id) } })
        }
    }
}

@Composable
private fun ListTop(list: MySubmissions, onMeasure: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        if (list.items.isEmpty()) {
            Text(stringResource(R.string.moje_pusty), style = MaterialTheme.typography.bodyLarge)
        }
        if (list.waitingCount > 0) {
            Text(stringResource(R.string.moje_czeka, list.waitingCount), style = MaterialTheme.typography.titleSmall)
        }
        Button(onClick = onMeasure) {
            Text(stringResource(R.string.pomiar_naglowek))
        }
    }
}

@Composable
private fun StatusCheckLine(check: StatusCheck, isOnline: Boolean, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(Odstepy.s),
    ) {
        if (!isOnline) {
            Text(stringResource(R.string.moje_bez_internetu), style = MaterialTheme.typography.bodyMedium)
        }
        when (check) {
            StatusCheck.CHECKING -> {
                Text(stringResource(R.string.moje_sprawdzamy), style = MaterialTheme.typography.bodyMedium)
            }

            StatusCheck.FAILED -> {
                Text(stringResource(R.string.moje_blad_statusow), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.formularz_sprobuj_ponownie)) }
            }

            StatusCheck.DONE -> {}
        }
    }
}
