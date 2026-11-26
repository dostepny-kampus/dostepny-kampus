/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.marking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.feature.capture.navigation.MarkingRoute
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import io.github.dostepnykampus.feature.capture.photo.rememberPhotoCamera
import kotlinx.coroutines.flow.first

/**
 * C6 „Zmiana oznaczenia wejścia” dla wejścia z [route]. Po zapisie woła [onFinish], a gdy wejścia nie ma w katalogu,
 * wraca przez [onLeave].
 *
 * Wymagania: US17.2, BR-24, NFR-54
 *
 * @param route wejście i jego budynek
 * @param onLeave „Wstecz” albo brak wejścia
 * @param onFinish zmiana zapisana w telefonie; dalej C3
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel model widoku; w aplikacji z grafu Hilt, w testach z atrapą danych
 * @param photoTranscoder przekodowanie zdjęcia, które przetrwa obrót ekranu
 */
@Composable
fun MarkingScreen(
    route: MarkingRoute,
    onLeave: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MarkingViewModel = hiltViewModel(),
    photoTranscoder: PhotoTranscoder = hiltViewModel(),
) {
    SideEffect { viewModel.open(route) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val leave by rememberUpdatedState(onLeave)
    val finish by rememberUpdatedState(onFinish)
    LaunchedEffect(viewModel) {
        val done = viewModel.uiState.first { it.stage == MarkingStage.SAVED || it.stage == MarkingStage.MISSING }
        if (done.stage == MarkingStage.SAVED) finish() else leave()
    }
    val camera = rememberPhotoCamera(onPhoto = viewModel::takePhoto, transcoder = photoTranscoder)
    val actions =
        MarkingActions(camera = camera, onChoose = viewModel::choose, onSend = viewModel::send, onBack = onLeave)
    MarkingContent(state = state, actions = actions, modifier = modifier)
}
