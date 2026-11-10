/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.feature.capture.navigation.ReportEditRoute
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * C2 z polami zgłoszenia [route] z C3 (US8.3). „Zapisz” zastępuje to samo zgłoszenie, „Zmień miejsce” idzie
 * przez C1 z polami w [draft] (US7.4), a „Wstecz” wraca do C3 (NFR-54). Gdy zgłoszenia nie ma już w telefonie,
 * ekran od razu wraca do C3.
 *
 * @param route zgłoszenie do poprawki
 * @param exits wyjścia do nawigacji
 * @param modifier wymiary nadane przez aplikację
 * @param draft szkic współdzielony z C1 przez nawigację
 * @param loader wczytanie z kolejki; w aplikacji z grafu Hilt, w testach z atrapą
 * @param viewModel formularz; w aplikacji z grafu Hilt, w testach z zapisem testowym
 * @param photoTranscoder przekodowanie zdjęcia, które przetrwa obrót ekranu (NFR-22)
 */
@Composable
fun ReportEditScreen(
    route: ReportEditRoute,
    exits: ReportEditExits,
    modifier: Modifier = Modifier,
    draft: ReportDraft = remember { ReportDraft() },
    loader: ReportEditViewModel = hiltViewModel(),
    viewModel: ReportFormViewModel = hiltViewModel(),
    photoTranscoder: PhotoTranscoder = hiltViewModel(),
) {
    SideEffect { loader.open(route.submissionId) }
    val load by loader.load.collectAsStateWithLifecycle()
    val start = (load as? ReportEditLoad.Ready)?.start
    if (start != null) SideEffect { viewModel.open(start.form, draft, start.photos) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val leave by rememberUpdatedState(exits.onLeave)
    val save by rememberUpdatedState(exits.onSave)
    val changePlace by rememberUpdatedState(exits.onChangePlace)
    LaunchedEffect(loader) {
        loader.load.first { it == ReportEditLoad.Missing }
        leave()
    }
    LaunchedEffect(viewModel) {
        viewModel.uiState
            .map { it?.stage }
            .distinctUntilChanged()
            .collect { stage ->
                if (stage == ReportStage.LEFT) leave()
                if (stage == ReportStage.PLACE_CHANGED) changePlace()
                if (stage == ReportStage.SAVED) save()
            }
    }
    BackHandler(onBack = viewModel::back)
    val camera =
        rememberPhotoCamera(onPhoto = { viewModel.edit(ReportEdit.Photo(it)) }, transcoder = photoTranscoder)
    state?.let { current ->
        val actions =
            ReportActions(
                onEdit = viewModel::edit,
                onChangePlace = viewModel::changePlace,
                camera = camera,
                onSave = viewModel::save,
                onBack = viewModel::back,
                onConfirmExit = viewModel::confirmExit,
                onStay = viewModel::stay,
            )
        ReportContent(state = current, actions = actions, modifier = modifier)
    }
}

/**
 * Wyjścia z poprawki C2.
 *
 * @property onLeave wyjście bez zapisu albo brak zgłoszenia; powrót do C3
 * @property onSave poprawka zapisana; powrót do C3
 * @property onChangePlace „Zmień miejsce”: pomiar C1 zamiast poprawki
 */
data class ReportEditExits(val onLeave: () -> Unit, val onSave: () -> Unit, val onChangePlace: () -> Unit)
