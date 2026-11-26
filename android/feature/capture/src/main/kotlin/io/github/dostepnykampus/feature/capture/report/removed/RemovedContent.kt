/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.removed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.bar.PinnedActionBar
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.designsystem.campusDay
import io.github.dostepnykampus.core.designsystem.marking.BarrierPhoto
import io.github.dostepnykampus.core.designsystem.marking.barrierPlace
import io.github.dostepnykampus.core.designsystem.marking.nameRes
import io.github.dostepnykampus.core.designsystem.marking.nearEntrance
import io.github.dostepnykampus.feature.capture.R
import io.github.dostepnykampus.feature.capture.photo.PhotoPaths
import io.github.dostepnykampus.feature.capture.report.form.PhotoSection
import io.github.dostepnykampus.feature.capture.report.form.SendFailure

/**
 * Treść C7 „Usunięcie bariery” dla stanu [state], bez ViewModelu: dane zgłoszenia przyjętego bez edycji, nowe
 * zdjęcie miejsca, błąd walidacji albo zapisu i przypięte „Wyślij do sprawdzenia”. Dzień sprawdzenia liczy się
 * w strefie czasu kampusu, nie telefonu.
 *
 * Wymagania: US8.4, BR-19, NFR-7, NFR-25, NFR-52
 *
 * @param state stan ekranu
 * @param actions czynności ekranu
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun RemovedContent(state: RemovedUiState, actions: RemovedActions, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.usuniecie_naglowek), onBack = actions.onBack)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m),
                verticalArrangement = Arrangement.spacedBy(Odstepy.m),
            ) {
                val isRejected = state.stage == RemovedStage.SAVE_REJECTED
                if (isRejected || state.stage == RemovedStage.SAVE_FAILED) SendFailure(isRejected, actions.onSend)
                state.previous?.let { Accepted(barrier = it, state = state) }
                PhotoSection(
                    photos = PhotoPaths(listOfNotNull(state.photo?.path)),
                    camera = actions.camera,
                    title = stringResource(R.string.usuniecie_nowe_zdjecie),
                    error = stringResource(R.string.usuniecie_brak_zdjecia).takeIf { state.isPhotoMissing },
                )
                Text(stringResource(R.string.usuniecie_zostaje))
            }
            PinnedActionBar {
                PinnedButton(text = stringResource(R.string.usuniecie_wyslij), onClick = actions.onSend)
            }
        }
    }
}

@Composable
private fun Accepted(barrier: AcceptedBarrier, state: RemovedUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(
            text = stringResource(barrier.type?.nameRes() ?: R.string.moje_typ_nieznany),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = Typografia.wyroznienieWaga,
        )
        val place =
            barrierPlace(
                symbol = barrier.buildingSymbol,
                name = state.buildingName,
                entrance = state.entranceName,
                floor = barrier.floor,
            )
        Text(place)
        state.nearest?.let { Text(nearEntrance(entrance = it.entrance, meters = it.meters)) }
        barrier.verifiedAt?.let { verified ->
            val text = stringResource(R.string.usuniecie_sprawdzone, campusDay(verified))
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BarrierPhoto(url = barrier.photoUrl, type = barrier.type)
    }
}
