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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    MySubmissionsContent(state = state, actions = actions, modifier = modifier)
}

/**
 * Widok C3 bez modelu widoku: lista od najnowszego albo stan pusty (US8.1, PW-12).
 *
 * @param state stan ekranu
 * @param actions czynności ekranu
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun MySubmissionsContent(state: MySubmissionsUiState, actions: MySubmissionsActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        BackHeader(title = stringResource(R.string.moje_naglowek), onBack = actions.onBack)
        if (state is MySubmissionsUiState.Ready) {
            SubmissionList(list = state.list, actions = actions)
        }
    }
}

@Composable
private fun SubmissionList(list: MySubmissions, actions: MySubmissionsActions) {
    LazyColumn(
        contentPadding = PaddingValues(Odstepy.m),
        verticalArrangement = Arrangement.spacedBy(Odstepy.m),
    ) {
        item {
            ListTop(list = list, onMeasure = actions.onMeasure)
        }
        items(list.items, key = { it.id }) { item ->
            SubmissionCard(item = item, onOpen = { actions.onOpen(item.id) })
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
