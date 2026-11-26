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
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.feature.capture.navigation.RemovedEditRoute
import io.github.dostepnykampus.feature.capture.navigation.RemovedRoute
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import io.github.dostepnykampus.feature.capture.photo.rememberPhotoCamera

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
    RemovedHost(
        state = state,
        actions =
            RemovedHostActions(
                onPhoto = viewModel::takePhoto,
                onSend = viewModel::send,
                onLeave = onLeave,
                onFinish = onFinish,
            ),
        photoTranscoder = photoTranscoder,
        modifier = modifier,
    )
}

/**
 * C7 z C3 dla własnego zgłoszenia z [route]: zapisane zdjęcie na start, zapis zastępuje to zgłoszenie. Gdy
 * zgłoszenia nie ma już w telefonie, wraca przez [onLeave].
 *
 * Wymagania: US8.3, US8.4, BR-19, NFR-54
 *
 * @param route własne zgłoszenie z kolejki telefonu
 * @param onLeave „Wstecz” albo brak zgłoszenia
 * @param onFinish poprawka zapisana w telefonie; dalej C3
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel model widoku; w aplikacji z grafu Hilt, w testach z atrapą danych
 * @param photoTranscoder przekodowanie zdjęcia, które przetrwa obrót ekranu
 */
@Composable
fun RemovedEditScreen(
    route: RemovedEditRoute,
    onLeave: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RemovedViewModel = hiltViewModel(),
    photoTranscoder: PhotoTranscoder = hiltViewModel(),
) {
    SideEffect { viewModel.openEdit(route.submissionId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RemovedHost(
        state = state,
        actions =
            RemovedHostActions(
                onPhoto = viewModel::takePhoto,
                onSend = viewModel::send,
                onLeave = onLeave,
                onFinish = onFinish,
            ),
        photoTranscoder = photoTranscoder,
        modifier = modifier,
    )
}

/** Czynności C7 niezależne od tego, skąd ekran otwarto. */
private data class RemovedHostActions(
    val onPhoto: (PendingPhotoDraft) -> Unit,
    val onSend: () -> Unit,
    val onLeave: () -> Unit,
    val onFinish: () -> Unit,
)

@Composable
private fun RemovedHost(
    state: RemovedUiState,
    actions: RemovedHostActions,
    photoTranscoder: PhotoTranscoder,
    modifier: Modifier = Modifier,
) {
    val leave by rememberUpdatedState(actions.onLeave)
    val finish by rememberUpdatedState(actions.onFinish)
    LaunchedEffect(state.stage) {
        when (state.stage) {
            RemovedStage.SAVED -> finish()
            RemovedStage.MISSING -> leave()
            else -> Unit
        }
    }
    val camera = rememberPhotoCamera(onPhoto = actions.onPhoto, transcoder = photoTranscoder)
    RemovedContent(
        state = state,
        actions = RemovedActions(camera = camera, onSend = actions.onSend, onBack = actions.onLeave),
        modifier = modifier,
    )
}
