/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute

/**
 * C2 Opisz barierę (US7) z miejscem z [route]. „Wstecz” w nagłówku i wstecz telefonu bez zmian wychodzą od razu,
 * ze zmianami pytają „Wyjść bez zapisu?” (US7.4). „Zmień miejsce” wraca do C1 tą samą drogą co „Wstecz”.
 *
 * @param route miejsce z pomiaru C1
 * @param onLeft wyjście bez zapisu
 * @param onSaved zgłoszenie zapisane w kolejce (NFR-5); dalej C3
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel formularz; w aplikacji z grafu Hilt, w testach z zapisem testowym
 */
@Composable
fun ReportFormScreen(
    route: ReportFormRoute,
    onLeft: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportFormViewModel = hiltViewModel(),
) {
    LaunchedEffect(route) { viewModel.open(ReportForm(place = route.place())) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val stage = state?.stage
    val left by rememberUpdatedState(onLeft)
    val saved by rememberUpdatedState(onSaved)
    LaunchedEffect(stage) {
        when (stage) {
            ReportStage.LEFT -> left()
            ReportStage.SAVED -> saved()
            else -> Unit
        }
    }
    BackHandler(onBack = viewModel::back)
    state?.let { current ->
        val actions =
            ReportActions(
                onEdit = viewModel::edit,
                onChangePlace = viewModel::back,
                onTakePhoto = {},
                onSave = viewModel::save,
                onBack = viewModel::back,
                onConfirmExit = viewModel::confirmExit,
                onStay = viewModel::stay,
            )
        ReportContent(state = current, actions = actions, modifier = modifier)
    }
}
