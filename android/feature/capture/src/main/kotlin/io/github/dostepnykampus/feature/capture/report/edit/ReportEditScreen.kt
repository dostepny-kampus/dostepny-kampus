/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.edit

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
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import io.github.dostepnykampus.feature.capture.photo.rememberPhotoCamera
import io.github.dostepnykampus.feature.capture.report.ReportDraft
import io.github.dostepnykampus.feature.capture.report.ReportEditExits
import io.github.dostepnykampus.feature.capture.report.ReportFormHost
import io.github.dostepnykampus.feature.capture.report.ReportFormViewModel
import io.github.dostepnykampus.feature.capture.report.controls
import io.github.dostepnykampus.feature.capture.report.form.ReportEdit
import kotlinx.coroutines.flow.first

/**
 * C2 z polami zgłoszenia [route] z C3. „Zapisz” zastępuje to samo zgłoszenie, a przy ponownym wysłaniu
 * nieaktualnej zmiany tworzy nową zmianę bieżącego przyjętego zgłoszenia. „Zmień miejsce” idzie przez C1 z polami
 * w [draft], a „Wstecz” wraca do C3. Gdy zgłoszenia albo bieżącego przyjętego zgłoszenia nie ma w telefonie, ekran
 * od razu wraca do C3.
 *
 * Wymagania: US8.3, US7.4, NFR-54, NFR-22, BR-23
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
    SideEffect { loader.open(route) }
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
