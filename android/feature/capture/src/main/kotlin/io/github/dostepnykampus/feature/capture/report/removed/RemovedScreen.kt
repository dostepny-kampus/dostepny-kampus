/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.removed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.feature.capture.navigation.RemovedRoute
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import io.github.dostepnykampus.feature.capture.photo.rememberPhotoCamera
import kotlinx.coroutines.flow.first

/**
 * C7 „Usunięcie bariery” dla zgłoszenia z [route]. Po zapisie woła [onFinish], a gdy zgłoszenia
 * przyjętego już nie ma, wraca przez [onLeave].
 *
 * Wymagania: US8.4, BR-19, NFR-54
 *
 * @param route zgłoszenie przyjęte
 * @param onLeave „Wstecz” albo brak zgłoszenia przyjętego
 * @param onFinish zgłoszenie zapisane w telefonie; dalej C3
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel model widoku; w aplikacji z grafu Hilt, w testach z atrapą danych
 * @param photoTranscoder przekodowanie zdjęcia, które przetrwa obrót ekranu
 */
@Composable
fun RemovedScreen(
    route: RemovedRoute,
    onLeave: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RemovedViewModel = hiltViewModel(),
    photoTranscoder: PhotoTranscoder = hiltViewModel(),
) {
    SideEffect { viewModel.open(route.acceptedId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val leave by rememberUpdatedState(onLeave)
    val finish by rememberUpdatedState(onFinish)
    LaunchedEffect(viewModel) {
        val done = viewModel.uiState.first { it.stage == RemovedStage.SAVED || it.stage == RemovedStage.MISSING }
        if (done.stage == RemovedStage.SAVED) finish() else leave()
    }
    val camera = rememberPhotoCamera(onPhoto = viewModel::takePhoto, transcoder = photoTranscoder)
    RemovedContent(
        state = state,
        actions = RemovedActions(camera = camera, onSend = viewModel::send, onBack = onLeave),
        modifier = modifier,
    )
}
