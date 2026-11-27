/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.photo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.OfflinePill
import io.github.dostepnykampus.core.designsystem.R as DesignR
import io.github.dostepnykampus.core.designsystem.StaffProblem
import io.github.dostepnykampus.core.designsystem.StaffProblemMessage
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.bar.PinnedActionBar
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.designsystem.card.OutlinedAction
import io.github.dostepnykampus.core.designsystem.bar.AdaptiveScreen
import io.github.dostepnykampus.feature.review.R

/**
 * Zasłanianie twarzy i tablic na zdjęciu [photoId] zgłoszenia [submissionId] ze stanem z modelu widoku.
 *
 * Wymagania: BR-15, NFR-25, NFR-53
 *
 * @param submissionId identyfikator sprawdzanego zgłoszenia
 * @param photoId identyfikator zasłanianego zdjęcia
 * @param onBack „Wstecz”: powrót do R2 bez zmian
 * @param onSaveFinish powrót do R2 po zapisie zdjęcia
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel stan ekranu; w aplikacji z grafu Hilt
 */
@Composable
fun CoverScreen(
    submissionId: String,
    photoId: String,
    onBack: () -> Unit,
    onSaveFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CoverViewModel =
        hiltViewModel<CoverViewModel, CoverViewModel.Factory>(
            key = "$submissionId/$photoId",
            creationCallback = { factory -> factory.create(submissionId = submissionId, photoId = photoId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions =
        CoverActions(
            onBack = onBack,
            onTap = viewModel::tap,
            onBigger = viewModel::bigger,
            onSmaller = viewModel::smaller,
            onRemove = viewModel::remove,
            onSave = { viewModel.save(onSaveFinish) },
            onRetry = viewModel::refresh,
        )
    CoverContent(state = state, actions = actions, modifier = modifier)
}

/**
 * Widok zasłaniania bez modelu widoku: napis, zdjęcie z zasłonami, przyciski zaznaczonej zasłony, „Zapisz zdjęcie” w
 * przypiętym pasie, brak sieci przy zapisie, wczytywanie i stany wspólne.
 *
 * Wymagania: BR-15, NFR-17, NFR-25, NFR-53
 *
 * @param state stan ekranu
 * @param actions czynności ekranu
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
internal fun CoverContent(state: CoverUiState, actions: CoverActions, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        AdaptiveScreen(
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            header = { BackHeader(title = stringResource(R.string.zaslona_naglowek), onBack = actions.onBack) },
            actions = { if (state is CoverUiState.Ready) PinnedActionBar { SaveButton(state, actions) } },
        ) {
            when (state) {
                CoverUiState.Loading -> Loading()
                is CoverUiState.Ready -> Ready(state = state, actions = actions)
                CoverUiState.Offline -> StaffProblemMessage(StaffProblem.OFFLINE, actions.onRetry)
                CoverUiState.ServerError -> StaffProblemMessage(StaffProblem.SERVER, actions.onRetry)
                CoverUiState.Forbidden -> StaffProblemMessage(StaffProblem.FORBIDDEN, actions.onBack)
            }
        }
    }
}

@Composable
private fun Loading() {
    Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        CircularProgressIndicator()
        Text(stringResource(DesignR.string.zdjecie_wczytujemy), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Ready(state: CoverUiState.Ready, actions: CoverActions) {
    val isLarge = LocalDensity.current.fontScale >= LARGE_FONT_SCALE

    Column(
        modifier = Modifier.fillMaxWidth().padding(Odstepy.m),
        verticalArrangement = Arrangement.spacedBy(Odstepy.m),
    ) {
        if (state.isOffline) {
            OfflinePill()
            Text(
                text = stringResource(R.string.zaslona_bez_sieci),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(stringResource(R.string.zaslona_dotknij), style = MaterialTheme.typography.bodyLarge)
        CoverPhotoView(photo = state.photo, covers = state.covers, onTap = actions.onTap)
        SelectedCover(covers = state.covers, actions = actions, isLarge = isLarge)
    }
}

@Composable
private fun SaveButton(state: CoverUiState.Ready, actions: CoverActions) {
    PinnedButton(
        text = stringResource(if (state.isSaving) R.string.zaslona_zapisujemy else R.string.zaslona_zapisz),
        onClick = actions.onSave,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SelectedCover(covers: Covers, actions: CoverActions, isLarge: Boolean) {
    val selected = covers.selected ?: return
    val button = Modifier.heightIn(min = CeleDotyku.przycisk)
    val bigger: @Composable (Modifier) -> Unit = { width ->
        OutlinedAction(onClick = actions.onBigger, modifier = button.then(width)) {
            Text(stringResource(R.string.zaslona_wiekszy))
        }
    }
    val smaller: @Composable (Modifier) -> Unit = { width ->
        OutlinedAction(onClick = actions.onSmaller, modifier = button.then(width)) {
            Text(stringResource(R.string.zaslona_mniejszy))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(
            text = stringResource(R.string.zaslona_ktora_zaznaczona, selected + 1, covers.items.size),
            style = MaterialTheme.typography.titleMedium,
        )
        if (isLarge) {
            bigger(Modifier.fillMaxWidth())
            smaller(Modifier.fillMaxWidth())
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(CeleDotyku.odstep)) {
                bigger(Modifier.weight(1f))
                smaller(Modifier.weight(1f))
            }
        }
        OutlinedAction(onClick = actions.onRemove, modifier = button.fillMaxWidth()) {
            Text(stringResource(R.string.zaslona_usun))
        }
    }
}
