/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

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
import kotlinx.coroutines.flow.first

/**
 * C2 z polami zgłoszenia [route] z C3. „Zapisz” zastępuje to samo zgłoszenie, „Zmień miejsce” idzie
 * przez C1 z polami w [draft], a „Wstecz” wraca do C3. Gdy zgłoszenia nie ma już w telefonie,
 * ekran od razu wraca do C3.
 *
 * Wymagania: US8.3, US7.4, NFR-54, NFR-22
 *
 * @param route zgłoszenie do poprawki
 * @param exits wyjścia do nawigacji
 * @param modifier wymiary nadane przez aplikację
 * @param draft szkic współdzielony z C1 przez nawigację
 * @param loader wczytanie z kolejki; w aplikacji z grafu Hilt, w testach z atrapą
 * @param viewModel formularz; w aplikacji z grafu Hilt, w testach z zapisem testowym
 * @param photoTranscoder przekodowanie zdjęcia, które przetrwa obrót ekranu
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
    val leave by rememberUpdatedState(exits.onLeave)
    LaunchedEffect(loader) {
        loader.load.first { it == ReportEditLoad.Missing }
        leave()
    }
    val camera =
        rememberPhotoCamera(onPhoto = { viewModel.edit(ReportEdit.Photo(it)) }, transcoder = photoTranscoder)
    ReportFormHost(controls = viewModel.controls(), camera = camera, exits = exits, modifier = modifier)
}

/**
 * Wyjścia z formularza C2; w nowym zgłoszeniu „Zmień miejsce” wychodzi tak samo jak [onLeave].
 *
 * @property onLeave wyjście bez zapisu albo brak zgłoszenia; powrót do C3
 * @property onSave poprawka zapisana; powrót do C3
 * @property onChangePlace „Zmień miejsce”: pomiar C1 zamiast poprawki
 */
data class ReportEditExits(val onLeave: () -> Unit, val onSave: () -> Unit, val onChangePlace: () -> Unit)
