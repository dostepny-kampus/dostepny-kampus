/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.queue

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.GroupFrame
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.StaffProblem
import io.github.dostepnykampus.core.designsystem.StaffProblemMessage
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.feature.review.R

/**
 * Czynności ekranu R1.
 *
 * Wymagania: US10.4, US10.7
 *
 * @property onBack „Wstecz” i „Wróć do mapy”
 * @property onOpen otwarcie zgłoszenia wziętego przez tego Weryfikatora; argument to identyfikator zgłoszenia
 */
data class ReviewQueueActions(val onBack: () -> Unit, val onOpen: (submissionId: String) -> Unit)

/**
 * R1 „Zgłoszenia do sprawdzenia” ze stanem z modelu widoku.
 *
 * Wymagania: US10.4, US10.7
 *
 * @param actions czynności ekranu
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel stan kolejki; w aplikacji z grafu Hilt
 */
@Composable
fun ReviewQueueScreen(
    actions: ReviewQueueActions,
    modifier: Modifier = Modifier,
    viewModel: ReviewQueueViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReviewQueueContent(
        state = state,
        actions = actions,
        onClaim = { id -> viewModel.claim(id, actions.onOpen) },
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

/**
 * Widok R1 bez modelu widoku: kolejka, wczytywanie, pusta kolejka i stany wspólne.
 *
 * Wymagania: US10.4, US10.7, BR-11, NFR-50
 *
 * @param state stan ekranu
 * @param actions czynności ekranu
 * @param onClaim „Weź do sprawdzenia”; argument to identyfikator zgłoszenia
 * @param onRetry „Spróbuj ponownie”
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun ReviewQueueContent(
    state: ReviewQueueUiState,
    actions: ReviewQueueActions,
    onClaim: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.kolejka_naglowek), onBack = actions.onBack)
            when (state) {
                ReviewQueueUiState.Loading -> Loading()
                is ReviewQueueUiState.Ready -> Queue(state = state, onOpen = actions.onOpen, onClaim = onClaim)
                ReviewQueueUiState.Offline -> StaffProblemMessage(StaffProblem.OFFLINE, onRetry)
                ReviewQueueUiState.ServerError -> StaffProblemMessage(StaffProblem.SERVER, onRetry)
                ReviewQueueUiState.Forbidden -> StaffProblemMessage(StaffProblem.FORBIDDEN, actions.onBack)
            }
        }
    }
}

@Composable
private fun Queue(state: ReviewQueueUiState.Ready, onOpen: (String) -> Unit, onClaim: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.m)) {
        if (state.isTaken) {
            item {
                GroupFrame {
                    Text(stringResource(R.string.kolejka_zajete), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.kolejka_zajete_wybierz), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        section(R.string.kolejka_wziete, state.mine) { mine -> QueueCard(mine, isMine = true) { onOpen(mine.id) } }
        if (state.waiting.isEmpty()) {
            item { Text(stringResource(R.string.kolejka_pusta), style = MaterialTheme.typography.bodyLarge) }
        } else {
            section(R.string.kolejka_do_sprawdzenia, state.waiting) { waiting ->
                QueueCard(waiting, isMine = false) { onClaim(waiting.id) }
            }
        }
    }
}

private fun LazyListScope.section(
    @StringRes title: Int,
    submissions: List<Submission>,
    card: @Composable (Submission) -> Unit,
) {
    if (submissions.isEmpty()) return
    item {
        Text(
            text = stringResource(title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
    }
    items(submissions, key = { it.id }) { card(it) }
}

@Composable
private fun Loading() {
    Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        CircularProgressIndicator()
        Text(stringResource(R.string.kolejka_wczytujemy), style = MaterialTheme.typography.bodyLarge)
    }
}
