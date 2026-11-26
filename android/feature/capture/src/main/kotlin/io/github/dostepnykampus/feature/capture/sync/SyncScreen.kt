/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.sync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// TODO(#3993): trasa C4 bez konfliktu w `app` otwiera ten ekran z „Czeka na wysłanie” na C3.

/**
 * C4 bez konfliktu ze stanem z modelu widoku; „Synchronizuj teraz” rusza wysyłkę.
 *
 * Wymagania: US9.1, US9.2, US9.4
 *
 * @param onBack „Wstecz”
 * @param onLogin „Zaloguj się” przy wygasłym zalogowaniu
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel model widoku; w aplikacji z grafu Hilt
 */
@Composable
fun SyncScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SyncViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SyncContent(
        state = state,
        actions = SyncActions(onBack = onBack, onSyncNow = viewModel::syncNow, onLogin = onLogin),
        modifier = modifier,
    )
}
