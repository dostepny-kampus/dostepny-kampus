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
 */
@Composable
fun RemovedScreen(
    route: RemovedRoute,
    onLeave: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RemovedHost(open = { open(route.acceptedId) }, onLeave = onLeave, onFinish = onFinish, modifier = modifier)
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
 */
@Composable
fun RemovedEditScreen(
    route: RemovedEditRoute,
    onLeave: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RemovedHost(open = { openEdit(route.submissionId) }, onLeave = onLeave, onFinish = onFinish, modifier = modifier)
}

/**
 * Wspólny C7: [open] wczytuje zgłoszenie do [viewModel], po zapisie woła [onFinish], bez zgłoszenia [onLeave].
 *
 * @param viewModel model widoku; w aplikacji z grafu Hilt
 * @param photoTranscoder przekodowanie zdjęcia, które przetrwa obrót ekranu
 */
@Composable
private fun RemovedHost(
    open: RemovedViewModel.() -> Unit,
    onLeave: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RemovedViewModel = hiltViewModel(),
    photoTranscoder: PhotoTranscoder = hiltViewModel(),
) {
    SideEffect { viewModel.open() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val leave by rememberUpdatedState(onLeave)
    val finish by rememberUpdatedState(onFinish)
    LaunchedEffect(viewModel) { if (viewModel.awaitExit()) finish() else leave() }
    val camera = rememberPhotoCamera(onPhoto = viewModel::takePhoto, transcoder = photoTranscoder)
    RemovedContent(
        state = state,
        actions = RemovedActions(camera = camera, onSend = viewModel::send, onBack = onLeave),
        modifier = modifier,
    )
}
