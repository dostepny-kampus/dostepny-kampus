/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.dostepnykampus.feature.capture.measure.place
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import io.github.dostepnykampus.feature.capture.photo.rememberPhotoCamera

/**
 * C2 Opisz barierę z miejscem z [route]. „Wstecz” w nagłówku i wstecz telefonu bez zmian wychodzą od razu,
 * ze zmianami pytają „Wyjść bez zapisu?”. „Zmień miejsce” wraca do C1 bez pytania, a wpisane pola
 * czekają na nowe miejsce.
 *
 * Wymagania: US7, US7.4, NFR-5, NFR-22
 *
 * @param route miejsce z pomiaru C1
 * @param onLeave wyjście bez zapisu
 * @param onSave zgłoszenie zapisane w kolejce; dalej C3
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel formularz; w aplikacji z grafu Hilt, w testach z zapisem testowym
 * @param draft szkic współdzielony z C1 przez nawigację
 * @param photoTranscoder przekodowanie zdjęcia, które przetrwa obrót ekranu
 */
@Composable
fun ReportFormScreen(
    route: ReportFormRoute,
    onLeave: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportFormViewModel = hiltViewModel(),
    draft: ReportDraft = remember { ReportDraft() },
    photoTranscoder: PhotoTranscoder = hiltViewModel(),
) {
    SideEffect { viewModel.open(ReportForm(place = route.place()), draft) }
    val camera =
        rememberPhotoCamera(onPhoto = { viewModel.edit(ReportEdit.Photo(it)) }, transcoder = photoTranscoder)
    val exits = ReportEditExits(onLeave = onLeave, onSave = onSave, onChangePlace = onLeave)
    ReportFormHost(controls = viewModel.controls(), camera = camera, exits = exits, modifier = modifier)
}
