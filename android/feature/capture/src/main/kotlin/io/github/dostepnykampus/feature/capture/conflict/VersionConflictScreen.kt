/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.conflict

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.feature.capture.navigation.VersionConflictRoute
import kotlinx.coroutines.flow.first

/**
 * C4 przy konflikcie wersji zgłoszenia z [route] ze stanem z modelu widoku. [onClose] jest wywoływane po wyborze
 * albo odłożeniu wyboru, przy wyjściu bez wyboru i gdy zgłoszenie nie czeka już na wybór.
 *
 * Wymagania: US9.3, BR-10
 *
 * @param route zgłoszenie z konfliktem
 * @param onClose powrót do listy własnych zgłoszeń
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel stan wyboru; w aplikacji z grafu Hilt
 */
@Composable
fun VersionConflictScreen(
    route: VersionConflictRoute,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VersionConflictViewModel = hiltViewModel(),
) {
    SideEffect { viewModel.open(route.submissionId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val close by rememberUpdatedState(onClose)
    LaunchedEffect(viewModel) {
        viewModel.state.first { it == ConflictUiState.Closed || it == ConflictUiState.Missing }
        close()
    }
    VersionConflictContent(
        state = state,
        actions =
            ConflictActions(
                onBack = onClose,
                onKeepMine = viewModel::keepMine,
                onTakeServer = viewModel::takeServer,
                onLater = viewModel::later,
            ),
        modifier = modifier,
    )
}
