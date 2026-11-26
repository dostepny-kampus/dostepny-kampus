/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.OfflinePill
import io.github.dostepnykampus.core.designsystem.StaffProblem
import io.github.dostepnykampus.core.designsystem.StaffProblemMessage
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.feature.review.R

/**
 * R2 „Sprawdzenie zgłoszenia” [submissionId] ze stanem z modelu widoku.
 *
 * Wymagania: US10, US10.2, US10.8
 *
 * @param submissionId identyfikator sprawdzanego zgłoszenia
 * @param actions czynności ekranu
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel stan ekranu; w aplikacji z grafu Hilt
 */
@Composable
fun ReviewCheckScreen(
    submissionId: String,
    actions: ReviewCheckActions,
    modifier: Modifier = Modifier,
    viewModel: ReviewCheckViewModel =
        hiltViewModel<ReviewCheckViewModel, ReviewCheckViewModel.Factory>(
            key = submissionId,
            creationCallback = { factory -> factory.create(submissionId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val decision =
        DecisionActions(
            onAccept = { viewModel.accept(actions.onDecided) },
            onReject = { choice ->
                viewModel.reject(reason = choice.reason, duplicateOf = choice.duplicateOf, onDone = actions.onDecided)
            },
        )
    ReviewCheckContent(
        state = state,
        actions = actions,
        decision = decision,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

/**
 * Widok R2 bez modelu widoku: dane zgłoszenia, decyzja, komunikaty, wczytywanie i stany wspólne.
 *
 * Wymagania: US10, US10.2, US10.8, BR-3, BR-11, NFR-50
 *
 * @param state stan ekranu
 * @param actions czynności ekranu
 * @param decision „Przyjmij” i „Odrzuć”
 * @param onRetry „Spróbuj ponownie”
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
internal fun ReviewCheckContent(
    state: CheckUiState,
    actions: ReviewCheckActions,
    decision: DecisionActions,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.sprawdzenie_naglowek), onBack = actions.onBack)
            when (state) {
                CheckUiState.Loading -> Loading()
                is CheckUiState.Ready -> Check(state = state, decision = decision)
                CheckUiState.Offline -> StaffProblemMessage(StaffProblem.OFFLINE, onRetry)
                CheckUiState.ServerError -> StaffProblemMessage(StaffProblem.SERVER, onRetry)
                CheckUiState.Forbidden -> StaffProblemMessage(StaffProblem.FORBIDDEN, actions.onBack)
            }
        }
    }
}

@Composable
private fun Loading() {
    Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        CircularProgressIndicator()
        Text(stringResource(R.string.sprawdzenie_wczytujemy), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Check(state: CheckUiState.Ready, decision: DecisionActions) {
    val similar = remember(state.similar, state.similarPhotos) { SimilarReports(state.similar, state.similarPhotos) }
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(Odstepy.m),
        verticalArrangement = Arrangement.spacedBy(Odstepy.m),
    ) {
        if (state.notice == CheckNotice.OFFLINE) OfflinePill()
        if (state.notice == CheckNotice.CHANGED) {
            Notice(title = R.string.sprawdzenie_zmienione, body = R.string.sprawdzenie_zmienione_sprawdz)
        }
        SubmissionDetails(state)
        SimilarList(similar)
        HorizontalDivider(thickness = Obramowania.ramka, color = MaterialTheme.colorScheme.outlineVariant)
        Decision(
            similar = similar,
            allowsDuplicate = state.submission.content.kind != SubmissionKind.ENTRANCE_MARKING,
            notice = state.notice,
            actions = decision,
        )
    }
}
